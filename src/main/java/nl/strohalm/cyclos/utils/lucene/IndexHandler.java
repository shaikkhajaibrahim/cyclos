/*
   This file is part of Cyclos.

   Cyclos is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 2 of the License, or
   (at your option) any later version.

   Cyclos is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with Cyclos; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

 */
package nl.strohalm.cyclos.utils.lucene;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.MessageResolver;
import nl.strohalm.cyclos.utils.CurrentTransactionData.Entry;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.util.Version;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles configuration and operation of Lucene indexes
 * @author luis
 */
public class IndexHandler implements InitializingBean, DisposableBean {

    /**
     * Bean for index parameters
     * @author luis
     */
    public static class IndexParameters {

        private Operation                  operation;
        private Class<? extends Indexable> type;
        private Collection<Long>           ids;

        /**
         * Constructs an IndexParameters for optimize
         */
        public IndexParameters(final Operation operation, final Class<? extends Indexable> type) {
            this.operation = operation;
            this.type = type;
        }

        public IndexParameters(final Operation operation, final Class<? extends Indexable> type, final Collection<Long> ids) {
            this(operation, type);
            this.ids = ids;
        }

        public Collection<Long> getIds() {
            return ids;
        }

        public Operation getOperation() {
            return operation;
        }

        public Class<? extends Indexable> getType() {
            return type;
        }

        @Override
        public String toString() {
            return operation.name() + (type == null ? "" : " " + type) + (ids == null ? "" : ids);
        }
    }

    /**
     * The index operation to run
     * @author luis
     */
    public static enum Operation {
        RECREATE, RECREATE_IF_CORRUPT, ADD, REMOVE, OPTIMIZE, NOTIFY_OPTIMIZED, NOTIFY_REBUILD_START, NOTIFY_REBUILD_END
    }

    /**
     * A thread that keeps reading the queue and then indexing the given entities
     * 
     * @author luis
     */
    private class IndexingThread extends Thread {
        private BlockingQueue<IndexParameters> indexingQueue = new LinkedBlockingQueue<IndexParameters>();

        @Override
        public void run() {
            try {
                while (true) {
                    IndexParameters indexParameters = null;
                    indexParameters = indexingQueue.take();
                    final Operation operation = indexParameters == null ? null : indexParameters.getOperation();
                    if (operation == null) {
                        continue;
                    }

                    // Perform the actual operation
                    final Class<? extends Indexable> type = indexParameters.getType();
                    final Collection<Long> ids = indexParameters.getIds();
                    switch (operation) {
                        case RECREATE:
                            doRecreate(type, true);
                            break;
                        case RECREATE_IF_CORRUPT:
                            doRecreate(type, false);
                            break;
                        case ADD:
                            // First remove the documents from the index, if any
                            doRemove(type, ids, true);
                            // Then add them back
                            doAdd(type, ids);
                            break;
                        case REMOVE:
                            doRemove(type, ids, false);
                            break;
                        case OPTIMIZE:
                            doOptimize(type);
                            break;
                        case NOTIFY_OPTIMIZED:
                            doNotifyOptimized(type);
                            break;
                        case NOTIFY_REBUILD_START:
                            doRebuildStart(type);
                            break;
                        case NOTIFY_REBUILD_END:
                            doRebuildEnd(type);
                            break;
                    }
                }
            } catch (final InterruptedException e) {
                // Interrupted. Just leave the loop
            }
        }

        /**
         * Adds the given entities to the index
         */
        private void doAdd(final Class<? extends Indexable> entityType, final Collection<Long> ids) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                @SuppressWarnings("unchecked")
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    try {
                        final DocumentMapper documentMapper = documentMappers.get(entityType);
                        final IndexWriter writer = getWriter(entityType);
                        final Session session = SessionFactoryUtils.getSession(sessionFactory, true);

                        // Get the entities to be indexed
                        String typeName = null;
                        if (LOG.isDebugEnabled()) {
                            typeName = ClassHelper.getClassName(entityType);
                        }
                        Iterator<? extends Indexable> iterator = null;
                        try {
                            iterator = session.createQuery("from " + entityType.getName() + " e where e.id in (:ids)").setParameterList("ids", ids).iterate();
                            while (iterator.hasNext()) {
                                final Indexable entity = iterator.next();
                                final Document document = documentMapper.map(entity);
                                writer.addDocument(document);
                                entity.setIndexed(true);
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Added to index " + typeName + "#" + entity.getId());
                                }
                            }
                        } finally {
                            if (iterator != null) {
                                Hibernate.close(iterator);
                            }
                        }

                        // Commit the index writer
                        writer.commit();
                    } catch (final Exception e) {
                        status.setRollbackOnly();
                        LOG.warn("Error indexing " + ClassHelper.getClassName(entityType) + " with ids " + ids, e);
                    } finally {
                    }
                }
            });
        }

        private void doNotifyOptimized(final Class<? extends Indexable> type) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus arg0) {
                    alertService.create(SystemAlert.Alerts.INDEX_OPTIMIZED, resolveAlertArgument(type));
                }
            });
        }

        /**
         * Optimizes the given index
         */
        private void doOptimize(final Class<? extends Indexable> entityType) {
            final IndexWriter writer = getWriter(entityType);
            try {
                writer.optimize();
                writer.commit();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Optimized index for " + ClassHelper.getClassName(entityType));
                }
            } catch (final Exception e) {
                LOG.warn("Error optimizing index for " + ClassHelper.getClassName(entityType), e);
            }
        }

        private void doRebuildEnd(final Class<? extends Indexable> type) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus arg0) {
                    alertService.create(SystemAlert.Alerts.INDEX_REBUILD_END, resolveAlertArgument(type));
                }
            });
        }

        private void doRebuildStart(final Class<? extends Indexable> type) {
            transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus arg0) {
                    alertService.create(SystemAlert.Alerts.INDEX_REBUILD_START, resolveAlertArgument(type));
                }
            });
        }

        /**
         * Recreates an index. If the force parameter is false, execute only if the index is corrupt or missing
         */
        private void doRecreate(final Class<? extends Indexable> type, final boolean force) {
            boolean execute = true;
            // When not forced, run only
            if (!force) {
                final IndexStatus status = getIndexStatus(type);
                execute = status != IndexStatus.CORRUPT && status != IndexStatus.MISSING;
            }
            if (!execute) {
                return;
            }
            final IndexWriter indexWriter = cachedWriters.get(type);
            if (indexWriter != null) {
                try {
                    indexWriter.close();
                } catch (final Exception e) {
                    // Silently ignore
                }
                cachedWriters.remove(type);
            }
            // Remove all files and recreate the directory
            final File dir = getIndexDir(type);
            try {
                FileUtils.deleteDirectory(dir);
            } catch (final IOException e) {
                // Silently ignore
            }
            dir.mkdirs();
        }

        /**
         * Removes the given entities from the index
         */
        private void doRemove(final Class<? extends Indexable> entityType, final Collection<Long> ids, final boolean beforeAdd) {
            final IndexWriter writer = getWriter(entityType);
            // Filter
            final Query query = Filters.filter(new MatchAllDocsQuery(), Filters.terms("id", ids));
            try {
                writer.deleteDocuments(query);
                writer.commit();
                if (LOG.isDebugEnabled() && !beforeAdd) {
                    LOG.debug("Removed from index: " + ClassHelper.getClassName(entityType) + " with ids " + ids);
                }
            } catch (final Exception e) {
                LOG.warn("Error removing from index " + ClassHelper.getClassName(entityType) + " the ids " + ids, e);
            }
        }

        private String resolveAlertArgument(final Class<? extends Indexable> type) {
            String suffix;
            if (type == null) {
                suffix = "all";
            } else {
                suffix = ClassHelper.getClassName(type);
            }
            return messageResolver.message("adminTasks.indexes.type." + suffix);
        }
    }

    public static final Version LUCENE_VERSION  = Version.LUCENE_30;

    private static final int    MAX_INDEX_BATCH = 30;
    private static final Log    LOG             = LogFactory.getLog(IndexHandler.class);

    public static void handleException(final Exception e) {
        handleException(e, (File) null);
    }

    /**
     * Handles the given exception
     */
    public static void handleException(final Exception e, final File dir) {
        if (e instanceof CorruptIndexException) {
            LOG.error("Search index corrupted: " + dir);
        } else if (e instanceof LockObtainFailedException) {
            LOG.error("Could not obtain lock in search index: " + dir);
        }
        throw new DaoException(e);
    }

    /**
     * Returns the root directory where indexes are stored
     */
    public static File resolveIndexRoot() {
        // Setup the Lucene index directory to WEB-INF/indexes directory
        final File bin = FileUtils.toFile(IndexHandler.class.getResource("/")); // WEB-INF/classes
        File root = bin.getParentFile(); // WEB-INF
        // When running on the standalone server, the bin is root/bin, not root/web/WEB-INF/classes
        if (!bin.getAbsolutePath().contains("WEB-INF") && new File(root, "web").exists()) {
            root = new File(root, "web/WEB-INF");
        }
        return new File(root, "indexes"); // WEB-INF/indexes
    }

    private File                                            indexRoot;
    private SettingsService                                 settingsService;
    private Map<Class<?>, IndexWriter>                      cachedWriters = new HashMap<Class<?>, IndexWriter>();
    private IndexingThread                                  indexingThread;
    private TransactionTemplate                             transactionTemplate;
    private SessionFactory                                  sessionFactory;
    private AlertService                                    alertService;
    private MessageResolver                                 messageResolver;
    private Map<Class<? extends Indexable>, DocumentMapper> documentMappers;

    /**
     * Adds the given entities to index
     */
    public void add(final Class<? extends Indexable> entityType, final List<Long> ids) {
        // Split the given list into parts, to allow small indexing batches
        final int size = ids.size();
        for (int i = 0; i < size; i += MAX_INDEX_BATCH) {
            final List<Long> part = ids.subList(i, Math.min(i + MAX_INDEX_BATCH, size));
            CurrentTransactionData.addIndexing(new IndexParameters(Operation.ADD, entityType, part));
        }
    }

    /**
     * Adds the given entity to index
     */
    public void add(final Class<? extends Indexable> entityType, final Long id) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.ADD, entityType, Collections.singleton(id)));
    }

    public void afterPropertiesSet() throws Exception {
        indexRoot = resolveIndexRoot();
        if (!indexRoot.exists()) {
            indexRoot.mkdirs();
        }
        if (indexRoot == null) {
            throw new IllegalStateException("No write access to indexes directory");
        }
    }

    public void destroy() throws Exception {
        // Stop the indexing thread
        if (indexingThread != null) {
            indexingThread.interrupt();
            indexingThread = null;
        }

        // Close all index writers
        for (final Map.Entry<Class<?>, IndexWriter> entry : cachedWriters.entrySet()) {
            try {
                final IndexWriter writer = entry.getValue();
                writer.close();
            } catch (final Exception e) {
                LOG.warn("Error closing index for " + ClassHelper.getClassName(entry.getKey()));
            }
        }
        cachedWriters.clear();
    }

    /**
     * Returns the root directory for all indexes
     */
    public File getIndexRoot() {
        return indexRoot;
    }

    /**
     * Returns the index status for the given entity type
     */
    public IndexStatus getIndexStatus(final Class<? extends Indexable> entityType) {
        IndexReader reader;
        try {
            reader = IndexReader.open(getDirectory(entityType), true);
        } catch (final FileNotFoundException e) {
            return IndexStatus.MISSING;
        } catch (final IOException e) {
            return IndexStatus.CORRUPT;
        }
        try {
            // The isCurrent call will force the check for corrupted indexes
            reader.isCurrent();

            if (reader.maxDoc() == 0) {
                // An index with no documents is always optimized
                return IndexStatus.OPTIMIZED;
            }

            // The index is not corrupt. Check whether it's optimized
            final boolean optimized = reader.isOptimized();
            return optimized ? IndexStatus.OPTIMIZED : IndexStatus.NOT_OPTIMIZED;
        } catch (final CorruptIndexException e) {
            return IndexStatus.CORRUPT;
        } catch (final IOException e) {
            handleException(e, entityType);
            return null;
        } finally {
            try {
                reader.close();
            } catch (final IOException e) {
                // Silently ignore
            }
        }
    }

    /**
     * Returns an {@link IndexReader} for the given entity type
     */
    public IndexReader getReader(final Class<? extends Indexable> entityType) {
        try {
            return IndexReader.open(getDirectory(entityType), true);
        } catch (final IOException e) {
            handleException(e, entityType);
            return null;
        }
    }

    /**
     * Returns whether index dir exists
     */
    public boolean indexesExists() {
        return indexRoot.exists() && indexRoot.list().length > 0;
    }

    /**
     * Notifies and index optimization complete
     */
    public void notifyOptimized(final Class<? extends Indexable> entityType) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.NOTIFY_OPTIMIZED, entityType));
    }

    /**
     * Notifies and index rebuilding end
     */
    public void notifyRebuildEnd(final Class<? extends Indexable> entityType) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.NOTIFY_REBUILD_END, entityType));
    }

    /**
     * Notifies and index rebuilding start
     */
    public void notifyRebuildStart(final Class<? extends Indexable> entityType) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.NOTIFY_REBUILD_START, entityType));
    }

    /**
     * Optimizes the index for the given entity type
     */
    public void optimize(final Class<? extends Indexable> entityType) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.OPTIMIZE, entityType));
    }

    /**
     * Process all the given indexing operations
     */
    public void process(final Collection<IndexParameters> parameters) {
        final IndexingThread thread = getIndexingThread();
        thread.indexingQueue.addAll(parameters);
    }

    /**
     * Processes the next indexing operations according to the {@link CurrentTransactionData}
     */
    public void processFromCurrentTransaction() {
        final Entry entry = CurrentTransactionData.getEntry();
        final Collection<IndexParameters> indexings = entry == null ? null : entry.getIndexings();
        if (CollectionUtils.isNotEmpty(indexings)) {
            process(indexings);
        }
    }

    /**
     * Recreates the index for the given entity type
     */
    public void recreate(final Class<? extends Indexable> entityType) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.RECREATE, entityType));
    }

    /**
     * Recreates the index for the given entity type if it is corrupt
     */
    public void recreateIfCorrupt(final Class<? extends Indexable> entityType) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.RECREATE_IF_CORRUPT, entityType));
    }

    /**
     * Removes the given entities from index
     */
    public void remove(final Class<? extends Indexable> entityType, final List<Long> ids) {
        CurrentTransactionData.addIndexing(new IndexParameters(Operation.REMOVE, entityType, ids));
    }

    /**
     * Removes the given entity from index
     */
    public void remove(final Class<? extends Indexable> entityType, final Long id) {
        remove(entityType, Collections.singletonList(id));
    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setDocumentMappers(final Map<Class<? extends Indexable>, DocumentMapper> documentMappers) {
        this.documentMappers = documentMappers;
    }

    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void setSessionFactory(final SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    private Analyzer getAnalyzer() {
        return settingsService.getLocalSettings().getLanguage().getAnalyzer();
    }

    /**
     * Returns the lucene directory for the given entity type
     */
    private FSDirectory getDirectory(final Class<? extends Indexable> entityType) {
        final File dir = getIndexDir(entityType);
        try {
            return FSDirectory.open(dir);
        } catch (final IOException e) {
            handleException(e, dir);
            return null;
        }
    }

    /**
     * Returns the directory where the index is stored
     */
    private File getIndexDir(final Class<? extends Indexable> entityType) {
        return new File(indexRoot, ClassHelper.getClassName(entityType));
    }

    private IndexingThread getIndexingThread() {
        if (indexingThread == null || !indexingThread.isAlive()) {
            // Start the indexing thread
            final LocalSettings localSettings = settingsService.getLocalSettings();
            indexingThread = new IndexingThread();
            indexingThread.setName("Indexing for " + localSettings.getApplicationName());
            indexingThread.start();
        }
        return indexingThread;
    }

    /**
     * Returns an {@link IndexWriter} for the given entity type
     */
    private synchronized IndexWriter getWriter(final Class<? extends Indexable> entityType) {
        IndexWriter writer = cachedWriters.get(entityType);
        if (writer == null) {
            final Analyzer analyzer = getAnalyzer();
            try {
                final FSDirectory directory = getDirectory(entityType);
                IndexWriter.unlock(directory);
                writer = new IndexWriter(directory, analyzer, MaxFieldLength.UNLIMITED);
                cachedWriters.put(entityType, writer);
            } catch (final Exception e) {
                handleException(e, entityType);
            }
        }
        return writer;
    }

    private void handleException(final Exception e, final Class<? extends Indexable> entityType) {
        handleException(e, entityType == null ? null : getIndexDir(entityType));
    }

}

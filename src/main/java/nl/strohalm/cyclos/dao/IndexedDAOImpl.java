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
package nl.strohalm.cyclos.dao;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.lucene.Filters;
import nl.strohalm.cyclos.utils.lucene.IndexHandler;
import nl.strohalm.cyclos.utils.lucene.LuceneQueryHandler;
import nl.strohalm.cyclos.utils.query.PageParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

/**
 * Base implementation for indexed DAOs
 * @author luis
 */
public abstract class IndexedDAOImpl<E extends Entity & Indexable> extends BaseDAOImpl<E> implements IndexedDAO<E> {

    private IndexHandler       indexHandler;
    private LuceneQueryHandler luceneQueryHandler;
    private SettingsService    settingsService;

    public IndexedDAOImpl(final Class<E> entityClass) {
        super(entityClass);
    }

    public void addMissingEntitiesToIndex() {
        for (final Class<? extends E> type : resolveAllIndexedTypes()) {
            final IndexStatus status = indexHandler.getIndexStatus(type);
            if (status == IndexStatus.CORRUPT || status == IndexStatus.MISSING) {
                // The index is missing or corrupt. Rebuild it
                rebuildIndex(type);
            } else {
                // The index is ok - just add missing entities
                indexHandler.add(type, resolveAllIndexedEntityIds(type));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void addToIndex(final E entity) {
        final Class<? extends Indexable> type = (Class<? extends Indexable>) EntityHelper.getRealClass(entity);
        indexHandler.add(type, entity.getId());
    }

    public IndexStatus getIndexStatus(final Class<? extends E> entityType) {
        return indexHandler.getIndexStatus(entityType);
    }

    public final void optimizeIndex(final Class<? extends E> type) {
        indexHandler.optimize(type);
    }

    public final void rebuildIndex(final Class<? extends E> type) {
        // Ensure all entities are marked as indexed = false
        getHibernateTemplate().bulkUpdate("update " + type.getName() + " set indexed = false");

        // Ensure the index is not corrupt
        indexHandler.recreate(type);

        // Add all entities to index
        indexHandler.add(type, resolveAllIndexedEntityIds(type));

        // Ensure the index is optimized after the rebuild
        indexHandler.optimize(type);
    }

    public void removeFromIndex(final Class<? extends E> entityType, final Long... ids) {
        if (ids != null && ids.length > 0) {
            indexHandler.remove(entityType, Arrays.asList(ids));
        }
    }

    @SuppressWarnings("unchecked")
    public void removeFromIndex(final E entity) {
        final Class<? extends Indexable> type = (Class<? extends Indexable>) EntityHelper.getRealClass(entity);
        indexHandler.remove(type, entity.getId());
    }

    public void setIndexHandler(final IndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    public void setLuceneQueryHandler(final LuceneQueryHandler luceneQueryHandler) {
        this.luceneQueryHandler = luceneQueryHandler;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * May be overridden in order to append extra conditions to the 'all indexed entities' query
     */
    protected void appendAllIndexedEntitiesQuery(final Class<? extends E> type, final StringBuilder hql, final String alias, final Map<String, Object> params) {
    }

    protected Analyzer getAnalyzer() {
        return settingsService.getLocalSettings().getLanguage().getAnalyzer();
    }

    /**
     * Executes a full-text query, applying the desired result type and page parameters
     */
    protected <T extends E> List<T> list(final Class<T> entityType, final QueryParameters queryParameters, final Query query, final Filters filters) {
        final ResultType resultType = queryParameters == null || queryParameters.getResultType() == null ? ResultType.LIST : queryParameters.getResultType();
        final PageParameters pageParameters = queryParameters == null ? null : queryParameters.getPageParameters();
        final Relationship[] fetch = queryParameters == null || queryParameters.getFetch() == null ? null : queryParameters.getFetch().toArray(new Relationship[queryParameters.getFetch().size()]);
        return luceneQueryHandler.executeQuery(entityType, query, filters, resultType, pageParameters, fetch);
    }

    /**
     * Returns a list containing all indexed ids for the given type
     */
    protected List<Long> resolveAllIndexedEntityIds(final Class<? extends E> type) {
        final Map<String, Object> params = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append(" select e.id");
        hql.append(" from " + type.getName() + " e");
        hql.append(" where e.indexed = false");
        appendAllIndexedEntitiesQuery(type, hql, "e", params);
        return list(hql.toString(), params);
    }

    /**
     * Returns all indexed entity types. By default, returns the entityType for this DAO, but DAOs that handle hierarchies should override this method
     */
    @SuppressWarnings("unchecked")
    protected Class<? extends E>[] resolveAllIndexedTypes() {
        return new Class[] { getEntityType() };
    }
}

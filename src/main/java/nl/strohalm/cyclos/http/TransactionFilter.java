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
package nl.strohalm.cyclos.http;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.aop.AdminNotificationAspect;
import nl.strohalm.cyclos.exceptions.ApplicationException;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.DataIteratorHelper;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.CurrentTransactionData.Entry;
import nl.strohalm.cyclos.utils.CurrentTransactionData.TransactionCommitListener;
import nl.strohalm.cyclos.utils.lucene.IndexHandler;
import nl.strohalm.cyclos.utils.lucene.IndexHandler.IndexParameters;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.envers.tools.MutableBoolean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Filter used to manage transactions
 * @author luis
 */
public class TransactionFilter extends OncePerRequestFilter {

    private static final String  LISTENERS_KEY = "transactionCommitListeners";
    private static final Log     LOG           = LogFactory.getLog(TransactionFilter.class);

    private FetchService         fetchService;
    private TransactionTemplate  transactionTemplate;
    private boolean              alreadyLoggedMailError;
    private AccountStatusHandler accountStatusHandler;
    private IndexHandler         indexHandler;

    @Inject
    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    @Inject
    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    @Inject
    public void setIndexHandler(final IndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    @Inject
    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    protected void execute(final HttpServletRequest request, final HttpServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {

        // As the CXF servlet manually flushes the response before we have the time to end the transaction, we must use a custom response, which never
        // flushes the buffer until we really want it
        final HttpServletResponse response;
        if (RequestHelper.isWebService(request)) {
            response = new ForcedBufferResponse(servletResponse);
        } else {
            response = servletResponse;
        }

        // Can't use a primitive because it must be final
        final MutableBoolean commit = new MutableBoolean(false);

        // Execute the filter in a transaction
        Throwable error;
        try {
            error = transactionTemplate.execute(new TransactionCallback<Throwable>() {
                public Throwable doInTransaction(final TransactionStatus status) {
                    final Throwable result = runInTransaction(status, request, response, chain);
                    // When no rollback, set the commit flag
                    if (!status.isRollbackOnly()) {
                        commit.set();
                    }
                    return result;
                }
            });
        } catch (final Exception e) {
            LOG.error("Error executing transaction", e);
            error = e;
        }

        try {
            // If there was a commit, notify the TransactionCommitListeners
            if (commit.isSet()) {
                notifyTransactionCommitListeners(request);
            }

            // The resulting error was not silenced (i.e, by the BaseAction's try / catch. Log and rethrow
            if (error != null) {
                ActionHelper.generateLog(request, config.getServletContext(), error);
                if (error instanceof RuntimeException) {
                    throw (RuntimeException) error;
                } else if (error instanceof ServletException) {
                    throw (ServletException) error;
                } else if (error instanceof IOException) {
                    throw (IOException) error;
                } else {
                    throw new RuntimeException(error);
                }
            }

            // Flush the response if it's a web service
            if (response instanceof ForcedBufferResponse) {
                ((ForcedBufferResponse) response).doFlushBuffer();
            }
        } finally {
            // Ensure that if any data was placed on the CurrentTransactionData, it is released
            CurrentTransactionData.cleanup();

        }
    }

    /**
     * Adds the given {@link TransactionCommitListener} for the given request
     */
    @SuppressWarnings("unchecked")
    private void addTransactionCommitListener(final HttpServletRequest request, final TransactionCommitListener listener) {
        List<TransactionCommitListener> listeners = (List<TransactionCommitListener>) request.getAttribute(LISTENERS_KEY);
        if (listeners == null) {
            listeners = new ArrayList<TransactionCommitListener>();
            request.setAttribute(LISTENERS_KEY, listeners);
        }
        listeners.add(listener);
    }

    /**
     * Notifies the {@link TransactionCommitListener}s for the given request
     */
    @SuppressWarnings("unchecked")
    private void notifyTransactionCommitListeners(final HttpServletRequest request) {
        final List<TransactionCommitListener> listeners = (List<TransactionCommitListener>) request.getAttribute(LISTENERS_KEY);
        if (listeners != null) {
            for (final TransactionCommitListener listener : listeners) {
                try {
                    listener.onTransactionCommit();
                } catch (final Exception e) {
                    ActionHelper.generateLog(request, config.getServletContext(), e);
                }
            }
        }
    }

    private Throwable runInTransaction(final TransactionStatus status, final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) {

        // Execute the chain
        Throwable error = null;
        boolean errorWasSilenced = false;
        boolean hasWrite = false;
        List<TransactionCommitListener> commitListeners = null;
        try {
            chain.doFilter(request, response);
            Entry entry = CurrentTransactionData.getEntry();
            if (entry == null || entry.isSafeToFlush()) {
                fetchService.clearCache();
                entry = CurrentTransactionData.getEntry();
            }
            hasWrite = entry != null && entry.isWrite();
            error = entry == null ? null : entry.getErrorIfThereWereWrites();
            errorWasSilenced = error != null;
            commitListeners = entry == null ? null : entry.getTransactionCommitListeners();

            // Process the pending account statuses after the commit
            final int pendingAccountStatuses = entry == null ? 0 : entry.getPendingAccountStatuses();
            if (pendingAccountStatuses > 0) {
                addTransactionCommitListener(request, new TransactionCommitListener() {
                    public void onTransactionCommit() {
                        accountStatusHandler.processNext(pendingAccountStatuses);
                    }
                });
            }

            // Add custom transaction commit listeners
            if (CollectionUtils.isNotEmpty(commitListeners)) {
                for (final TransactionCommitListener listener : commitListeners) {
                    addTransactionCommitListener(request, listener);
                }
            }

            // Process the full text updates after the commit
            final List<IndexParameters> indexings = entry == null ? null : entry.getIndexings();
            if (CollectionUtils.isNotEmpty(indexings)) {
                // As no commit is done when there was no write, we can process directly. When there were writes, add a TransactionCommitListener
                if (hasWrite) {
                    addTransactionCommitListener(request, new TransactionCommitListener() {
                        public void onTransactionCommit() {
                            indexHandler.process(indexings);
                        }
                    });
                } else {
                    // No write: a rollback will be performed. Ensure the indexings are processed anyway (example: index recreation / optimization)
                    indexHandler.process(indexings);
                }
            }
        } catch (final Throwable t) {
            error = t;
        } finally {
            // Clean other control ThreadLocals also
            CurrentTransactionData.cleanup();
            if (!alreadyLoggedMailError && MailHandler.hasException()) {
                alreadyLoggedMailError = true;
                LOG.warn("Error sending mail", MailHandler.getCurrentException());
            }
            MailHandler.cleanup();
            AdminNotificationAspect.cleanup();
        }

        // Set rollback
        if (!hasWrite) {
            // When there were no database writes, set rollback
            status.setRollbackOnly();
        } else if (error instanceof ApplicationException) {
            // On application exceptions, we can determine if transactions will be applied
            if (((ApplicationException) error).isShouldRollback()) {
                status.setRollbackOnly();
            }
        } else if (error != null) {
            status.setRollbackOnly();
        }

        // Ensure there are no open iterators
        DataIteratorHelper.closeOpenIterators();

        // Throw the original error if it was not silenced
        if (!errorWasSilenced && error != null) {
            return error;
        }

        return null;
    }
}

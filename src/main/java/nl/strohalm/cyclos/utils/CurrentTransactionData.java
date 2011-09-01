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
package nl.strohalm.cyclos.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.utils.lucene.IndexHandler.IndexParameters;

import org.apache.commons.collections.CollectionUtils;

/**
 * Holds data about the current transaction
 * @author luis
 */
public class CurrentTransactionData {

    /**
     * Contains the current transaction data
     * @author luis
     */
    public static class Entry {
        private Throwable                       error;
        private boolean                         write;
        private int                             pendingAccountStatuses;
        private List<IndexParameters>           indexings;
        private List<TransactionCommitListener> transactionCommitListeners;
        private Map<Long, Boolean>              lockedAccounts;

        public Throwable getError() {
            return error;
        }

        public Throwable getErrorIfThereWereWrites() {
            return write ? error : null;
        }

        public List<IndexParameters> getIndexings() {
            return indexings;
        }

        public Map<Long, Boolean> getLockedAccounts() {
            if (lockedAccounts == null) {
                lockedAccounts = new HashMap<Long, Boolean>();
            }
            return lockedAccounts;
        }

        public int getPendingAccountStatuses() {
            return pendingAccountStatuses;
        }

        public List<TransactionCommitListener> getTransactionCommitListeners() {
            return transactionCommitListeners;
        }

        public boolean isSafeToFlush() {
            return error == null;
        }

        public boolean isWrite() {
            return write;
        }
    }

    /**
     * Listener notified when a transaction is committed
     * 
     * @author luis
     */
    public static interface TransactionCommitListener {
        void onTransactionCommit();
    }

    private static ThreadLocal<Entry> HOLDER = new ThreadLocal<Entry>();

    /**
     * Adds an indexing operation to be processed after the current transaction commit
     */
    public static void addIndexing(final IndexParameters indexParameters) {
        final Entry entry = currentEntry();
        if (entry.indexings == null) {
            entry.indexings = new ArrayList<IndexParameters>();
        }
        entry.indexings.add(indexParameters);
    }

    /**
     * Registers the given pending status id for the current transaction
     */
    public static void addPendingAccountStatus() {
        currentEntry().pendingAccountStatuses++;
    }

    /**
     * Adds a transaction commit listener
     */
    public static void addTransactionCommitListener(final TransactionCommitListener listener) {
        final Entry entry = currentEntry();
        if (entry.transactionCommitListeners == null) {
            entry.transactionCommitListeners = new ArrayList<TransactionCommitListener>();
        }
        entry.transactionCommitListeners.add(listener);
    }

    /**
     * Removes any reference
     */
    public static void cleanup() {
        try {
            HOLDER.remove();
        } catch (final Throwable e) {
            // Ignored
        }
    }

    /**
     * Removes the current error, if any
     */
    public static void clearError() {
        final Entry entry = getEntry();
        if (entry != null) {
            entry.error = null;
        }
    }

    /**
     * Returns the current entry, or null if nothing set
     */
    public static Entry getEntry() {
        return HOLDER.get();
    }

    /**
     * Returns the the error in this transaction, or null if none
     */
    public static Throwable getError() {
        final Entry entry = getEntry();
        return entry == null ? null : entry.error;
    }

    public static Map<Long, Boolean> getLockedAccounts() {
        return currentEntry().getLockedAccounts();
    }

    /**
     * Checks if there's an error in this transaction
     */
    public static boolean hasError() {
        return getError() != null;
    }

    /**
     * Check if there were database writes in this transaction
     */
    public static boolean hasWrite() {
        final Entry entry = getEntry();
        return entry != null && entry.write;
    }

    /**
     * Notify all registered listeners that a transaction has been committed
     */
    public static void runCurrentTransactionCommitListeners() {
        final Entry entry = HOLDER.get();
        final List<TransactionCommitListener> listeners = entry == null ? null : entry.getTransactionCommitListeners();
        if (CollectionUtils.isEmpty(listeners)) {
            return;
        }
        for (final TransactionCommitListener listener : listeners) {
            listener.onTransactionCommit();
        }
    }

    /**
     * Sets the current throwable
     */
    public static void setError(final Throwable throwable) {
        final Entry entry = currentEntry();
        // The error is set only if there was no previous error already
        if (entry.error == null) {
            entry.error = throwable;
        }
    }

    /**
     * Mark the current transaction as having database writes
     */
    public static void setWrite() {
        currentEntry().write = true;
    }

    /**
     * Returns the current entry, initializing it when no previous entry
     */
    private static Entry currentEntry() {
        Entry entry = HOLDER.get();
        if (entry == null) {
            entry = new Entry();
            HOLDER.set(entry);
        }
        return entry;
    }
}

/*
    This file is part of Cyclos <http://project.cyclos.org>

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.utils;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * It traces all invalid attempts for doing something, for example: invalid credentials.<br>
 * It has two parameters: the maximum number of allowed attempts and the time period<br>
 * within which will fail any new attempt (used after the maximum allowed has been reached)
 * @author ameyer
 */
public class InvalidTriesTracer {
    public enum TryStatus {
        /**
         * No more tries are allowed because the max has already been reached before.
         */
        NO_MORE_TRIES,

        /**
         * No more tries are allowed because the max has just been reached in this try.
         */
        MAX_TRIES_REACHED,

        /**
         * There are allowed tries
         */
        TRIES_ALLOWED;
    }

    private class TraceEntry {
        private int      tries;
        private Calendar blockedUntil;

        /**
         * @param calendar the time to compare with
         * @return true if the blocked time has not expired yet
         */
        private boolean isBlocked(final Calendar calendar) {
            return blockedUntil != null && !blockedUntil.before(calendar);
        }
    };

    private final Map<String, TraceEntry> traces = new ConcurrentHashMap<String, TraceEntry>();

    public boolean isBlocked(final String key) {
        synchronized (key) {
            final TraceEntry entry = traces.get(key);
            if (entry != null && entry.isBlocked(Calendar.getInstance())) {
                return true;
            } else {
                return false;
            }
        }
    }

    public TryStatus trace(final String key, final int maxTries, final TimePeriod blockTimeAfterMaxTries) {
        synchronized (key) {
            if (isBlocked(key)) {
                return TryStatus.NO_MORE_TRIES;
            } else if (maxTries > 0) { // if max tries is less or equals zero then there is unlimited tries count
                final TraceEntry entry = ensureEntry(key);
                entry.tries++;
                if (entry.tries == maxTries) {
                    // verify if the blocked time has expired (in that case we must remove the failure traces)
                    final Calendar current = Calendar.getInstance();
                    entry.blockedUntil = blockTimeAfterMaxTries.add(current);
                    entry.tries = 0; // we need to reset
                    return TryStatus.MAX_TRIES_REACHED;

                    // final Calendar blockedUntil = entry.blockedUntil;
                    // if (blockedUntil != null && blockedUntil.before(current)) {
                    // entry.tries = 1; // we must take into account this fail
                    // if (maxTries == 1) { // in this case the max tries has been reached
                    // entry.blockedUntil = blockTimeAfterMaxTries.add(current);
                    // return TryStatus.MAX_TRIES_REACHED;
                    // } else {
                    // entry.blockedUntil = null;
                    // return TryStatus.TRIES_ALLOWED;
                    // }
                    // } else {
                    // if (blockedUntil == null) {
                    // // Block the user. Calculate when the user will be able to login again
                    // entry.blockedUntil = blockTimeAfterMaxTries.add(current);
                    // return TryStatus.MAX_TRIES_REACHED;
                    // } else {
                    // return TryStatus.NO_MORE_TRIES;
                    // }
                    // }
                } else { // the max tries hasn't been reached yet
                    return TryStatus.TRIES_ALLOWED;
                }
            } else { // max tries <= 0
                return TryStatus.TRIES_ALLOWED;
            }
        }
    }

    public void unblock(final String key) {
        synchronized (key) {
            traces.remove(key);
        }
    }

    private TraceEntry ensureEntry(final String key) {
        TraceEntry entry = traces.get(key);
        if (entry == null) {
            entry = new TraceEntry();
            traces.put(key, entry);
        }
        return entry;
    }
}

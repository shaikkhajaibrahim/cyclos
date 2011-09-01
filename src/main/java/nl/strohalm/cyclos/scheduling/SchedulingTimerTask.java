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
package nl.strohalm.cyclos.scheduling;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.TimerTask;

import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.scheduling.tasks.ScheduledTask;
import nl.strohalm.cyclos.utils.CurrentTransactionData;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

/**
 * Executes the required tasks
 * @author luis
 */
public class SchedulingTimerTask extends TimerTask {

    private SchedulingHandler handler;

    public SchedulingTimerTask(final SchedulingHandler handler) {
        this.handler = handler;
    }

    @Override
    public void run() {
        final LocalSettings localSettings = handler.getSettingsService().getLocalSettings();
        final TimeZone timeZone = localSettings.getTimeZone();
        final Calendar time = Calendar.getInstance();
        // Use a localized formatter to get the time in the instance's local time zone
        final SimpleDateFormat format = new SimpleDateFormat("HH");
        if (timeZone != null) {
            format.setTimeZone(timeZone);
        }
        final boolean runExtraTasks = localSettings.getSchedulingHour() == Integer.parseInt(format.format(time.getTime()));
        for (final ScheduledTask task : handler.getTasks()) {
            final boolean runTask = task.isEveryHour() || runExtraTasks;
            if (runTask) {
                CurrentTransactionData.cleanup();

                final Boolean success = handler.getTransactionTemplate().execute(new TransactionCallback<Boolean>() {
                    public Boolean doInTransaction(final TransactionStatus status) {
                        return runInTransaction(status, task, time);
                    }
                });

                if (success) {
                    // Ensure the generated pending account statuses and index updates are processed
                    handler.getAccountStatusHandler().processFromCurrentTransaction();
                    handler.getIndexHandler().processFromCurrentTransaction();
                    CurrentTransactionData.runCurrentTransactionCommitListeners();
                }

                CurrentTransactionData.cleanup();
            }
        }
        handler.getLoggingHandler().logSchedulingTrace(System.currentTimeMillis() - time.getTimeInMillis());
    }

    private boolean runInTransaction(final TransactionStatus status, final ScheduledTask task, final Calendar time) {
        final long taskStart = System.currentTimeMillis();
        try {
            task.run(time);
            handler.getLoggingHandler().logScheduledTaskTrace(task.getName(), System.currentTimeMillis() - taskStart);
            return true;
        } catch (final Exception e) {
            status.setRollbackOnly();
            handler.getLoggingHandler().logScheduledTaskError(task.getName(), e);
            return false;
        }
    }
}

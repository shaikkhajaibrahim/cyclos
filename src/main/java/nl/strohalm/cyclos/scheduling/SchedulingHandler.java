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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.scheduling.tasks.ScheduledTask;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;
import nl.strohalm.cyclos.utils.lucene.IndexHandler;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles execution of scheduled jobs
 * @author luis
 */
public class SchedulingHandler {
    private Timer                timer;
    private SettingsService      settingsService;
    private LoggingHandler       loggingHandler;
    private TransactionTemplate  transactionTemplate;
    private AccountStatusHandler accountStatusHandler;
    private IndexHandler         indexHandler;
    private List<ScheduledTask>  tasks;

    public AccountStatusHandler getAccountStatusHandler() {
        return accountStatusHandler;
    }

    public IndexHandler getIndexHandler() {
        return indexHandler;
    }

    public LoggingHandler getLoggingHandler() {
        return loggingHandler;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public List<ScheduledTask> getTasks() {
        return tasks;
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    /**
     * Starts running scheduled jobs
     */
    public void initialize() {
        initializeTimer();
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setIndexHandler(final IndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTasks(final List<ScheduledTask> tasks) {
        this.tasks = tasks;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Stops all running tasks
     */
    public synchronized void shutdown() {
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Should be called when the time the tasks run should be modified
     */
    public synchronized void updateTime() {
        shutdown();
        initializeTimer();
    }

    private synchronized void initializeTimer() {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        timer = new Timer("Scheduled tasks handler for " + localSettings.getApplicationName());
        final SchedulingTimerTask task = new SchedulingTimerTask(this);
        timer.schedule(task, startsTaskAt(), DateUtils.MILLIS_PER_HOUR);
    }

    /**
     * Returns the date where the task will start running. It should be on the next hour, settings the minute to the <code>minute</code> property
     */
    private Date startsTaskAt() {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final Calendar startAt = Calendar.getInstance();
        startAt.add(Calendar.HOUR_OF_DAY, 1);
        startAt.set(Calendar.MINUTE, localSettings.getSchedulingMinute());
        return startAt.getTime();
    }
}

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
package nl.strohalm.cyclos.services.application;

import java.util.Calendar;
import java.util.Map;

import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.Application.PasswordHash;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for application-wide (global) events like status, time and messages.
 * @author luis
 */
public interface ApplicationService extends Service {

    /**
     * Returns the date since when the account status is activated
     */
    @SystemAction
    Calendar getAccountStatusEnabledSince();

    /**
     * Return statistical data regarding the application
     */
    @AdminAction(@Permission(module = "systemStatus", operation = "view"))
    ApplicationStatusVO getApplicationStatus();

    /**
     * Returns the application version
     */
    @SystemAction
    String getCyclosVersion();

    /**
     * A map indicating each index status by entity type
     */
    @AdminAction(@Permission(module = "systemTasks", operation = "manageIndexes"))
    Map<Class<? extends Indexable>, IndexStatus> getFullTextIndexesStatus();

    /**
     * Returns the hash algorithm that should be used for passwords
     */
    @DontEnforcePermission(traceable = true)
    PasswordHash getPasswordHash();

    /**
     * Notifies the application initialization
     */
    @SystemAction
    void initialize();

    /**
     * Returns whether the system is online
     */
    @SystemAction
    @AdminAction(@Permission(module = "systemTasks", operation = "onlineState"))
    boolean isOnline();

    /**
     * Optimizes the given full-text index, or all if the param is null
     */
    @AdminAction(@Permission(module = "systemTasks", operation = "manageIndexes"))
    void optimizeIndexes(Class<? extends Indexable> entityType);

    /**
     * Rebuilds the given full-text index from scratch, or all if the param is null
     */
    @AdminAction(@Permission(module = "systemTasks", operation = "manageIndexes"))
    void rebuildIndexes(Class<? extends Indexable> entityType);

    /**
     * Sets the system offline status
     */
    @AdminAction(@Permission(module = "systemTasks", operation = "onlineState"))
    void setOnline(boolean online);

    /**
     * Notifies the application shutdown
     */
    @SystemAction
    void shutdown();

}

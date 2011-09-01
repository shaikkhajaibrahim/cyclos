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
package nl.strohalm.cyclos.services.alerts;

import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.alerts.ErrorLogEntry;
import nl.strohalm.cyclos.entities.alerts.ErrorLogEntryQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for error logs
 * @author luis
 */
public interface ErrorLogService extends Service {

    /**
     * Count the number of active (non-deleted) error logs
     */
    int getCount();

    /**
     * Insert a new error log
     */
    ErrorLogEntry insert(Throwable t, String path, Map<String, ?> parameters);

    /**
     * Loads an error log by id
     */
    ErrorLogEntry load(Long id, Relationship... fetch);

    /**
     * Removes the given error log entries. They are not physically removed, but marked as removed.
     */
    @AdminAction(@Permission(module = "systemErrorLog", operation = "manage"))
    int remove(Long... ids);

    /**
     * Searches for error logs
     */
    List<ErrorLogEntry> search(ErrorLogEntryQuery query);
}

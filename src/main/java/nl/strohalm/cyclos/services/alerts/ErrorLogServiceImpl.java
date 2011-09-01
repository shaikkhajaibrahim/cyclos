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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.alerts.ErrorLogEntryDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.alerts.ErrorLogEntry;
import nl.strohalm.cyclos.entities.alerts.ErrorLogEntryQuery;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation for error logs service
 * @author luis
 */
public class ErrorLogServiceImpl implements ErrorLogService {

    private ErrorLogEntryDAO errorLogEntryDao;

    public int getCount() {
        final ErrorLogEntryQuery query = new ErrorLogEntryQuery();
        query.setShowRemoved(false);
        query.setPageForCount();
        return PageHelper.getTotalCount(errorLogEntryDao.search(query));
    }

    public ErrorLogEntry insert(final Throwable t, final String path, final Map<String, ?> parameters) {
        if (t == null) {
            throw new ValidationException("exception", new RequiredError());
        }
        if (StringUtils.isEmpty(path)) {
            throw new ValidationException("path", new RequiredError());
        }
        final ErrorLogEntry entry = new ErrorLogEntry();
        // Basic attributes
        entry.setDate(Calendar.getInstance());
        entry.setPath(path);
        // Logged user
        if (LoggedUser.isValid()) {
            entry.setLoggedUser(LoggedUser.user());
        }
        // Exception
        final StringWriter stackTrace = new StringWriter();
        t.printStackTrace(new PrintWriter(stackTrace));
        entry.setStackTrace(stackTrace.toString());
        // Request parameters
        if (MapUtils.isNotEmpty(parameters)) {
            final Map<String, String> params = new HashMap<String, String>();
            for (final Map.Entry<String, ?> e : parameters.entrySet()) {
                final String name = e.getKey();
                if (StringUtils.isEmpty(name)) {
                    continue;
                }
                // Mask the value if it's a password
                String value;
                if (name.toLowerCase().contains("password")) {
                    value = "***";
                } else {
                    value = CoercionHelper.coerce(String.class, e.getValue());
                }
                params.put(name, value);
            }
            entry.setParameters(params);
        }
        return errorLogEntryDao.insert(entry);
    }

    public ErrorLogEntry load(final Long id, final Relationship... fetch) {
        return errorLogEntryDao.load(id, fetch);
    }

    public int remove(final Long... ids) {
        return errorLogEntryDao.delete(ids);
    }

    public List<ErrorLogEntry> search(final ErrorLogEntryQuery query) {
        return errorLogEntryDao.search(query);
    }

    public void setErrorLogEntryDao(final ErrorLogEntryDAO errorLogEntryDao) {
        this.errorLogEntryDao = errorLogEntryDao;
    }
}

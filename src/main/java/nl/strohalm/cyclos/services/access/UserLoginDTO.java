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
package nl.strohalm.cyclos.services.access;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * Class to store the user and the date of current login
 * @author luis
 */
public class UserLoginDTO extends DataObject {

    private static final long             serialVersionUID = 6704036990371722852L;
    private final User                    user;
    private final Map<String, SessionDTO> sessions         = new LinkedHashMap<String, SessionDTO>();

    public UserLoginDTO(final User user) {
        this.user = user;
    }

    public void addSession(final SessionDTO session) {
        sessions.put(session.getSessionId(), session);
        session.setLogin(this);
    }

    public SessionDTO getSession(final String sessionId) {
        return sessions.get(sessionId);
    }

    public Collection<SessionDTO> getSessions() {
        return sessions.values();
    }

    public User getUser() {
        return user;
    }

    public void removeSession(final String sessionId) {
        final SessionDTO session = sessions.remove(sessionId);
        if (session != null) {
            session.setLogin(null);
        }
    }
}

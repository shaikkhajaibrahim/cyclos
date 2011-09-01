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

import java.util.Calendar;

import nl.strohalm.cyclos.entities.access.User;

/**
 * Contains data for a login session
 * 
 * @author luis
 */
public class SessionDTO {
    private final String   sessionId;
    private final Calendar loginDate;
    private final String   remoteAddress;
    private UserLoginDTO   login;

    public SessionDTO(final String sessionId, final Calendar loginDate, final String remoteAddress) {
        this.sessionId = sessionId;
        this.loginDate = loginDate;
        this.remoteAddress = remoteAddress;
    }

    public UserLoginDTO getLogin() {
        return login;
    }

    public Calendar getLoginDate() {
        return loginDate;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public User getUser() {
        if (login == null) {
            return null;
        }
        return login.getUser();
    }

    public void setLogin(final UserLoginDTO login) {
        this.login = login;
    }
}

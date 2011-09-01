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
package nl.strohalm.cyclos.entities.accounts.transactions;

import java.util.Calendar;

import nl.strohalm.cyclos.entities.Entity;

/**
 * This class represents a request for a transfer reverse. <br>
 * Before insert a transfer the engine must the check if there is a reverse for it and if any <br>
 * the transfer must ignored.
 * @author ameyer
 */
public class Reverse extends Entity {
    private static final long serialVersionUID = 1L;

    private Calendar          date;
    private String            traceNumber;
    private Long              clientId;

    public Long getClientId() {
        return clientId;
    }

    public Calendar getDate() {
        return date;
    }

    public String getTraceNumber() {
        return traceNumber;
    }

    public void setClientId(final Long clientId) {
        this.clientId = clientId;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setTraceNumber(final String traceNumber) {
        this.traceNumber = traceNumber;
    }

    @Override
    public String toString() {
        return "Reverse [id=" + getId() + "clientId=" + clientId + ", traceNumber=" + traceNumber + "]";
    }

}

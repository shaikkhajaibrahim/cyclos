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
package nl.strohalm.cyclos.dao.accounts.transactions;

import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.accounts.transactions.Reverse;

public class ReverseDAOImpl extends BaseDAOImpl<Reverse> implements ReverseDAO {

    public ReverseDAOImpl() {
        super(Reverse.class);
    }

    public Reverse load(final Long clientId, final String traceNumber) throws EntityNotFoundException {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("clientId", clientId);
        parameters.put("traceNumber", traceNumber);
        final Reverse reverse = uniqueResult("from Reverse r where r.clientId = :clientId and r.traceNumber = :traceNumber", parameters);
        if (reverse == null) {
            throw new EntityNotFoundException();
        }
        return reverse;
    }
}

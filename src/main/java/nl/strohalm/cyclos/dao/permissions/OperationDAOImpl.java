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
package nl.strohalm.cyclos.dao.permissions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Operation;

/**
 * The default implementation for OperationDAO
 * @author luis
 */
public class OperationDAOImpl extends BaseDAOImpl<Operation> implements OperationDAO {

    public OperationDAOImpl() {
        super(Operation.class);
    }

    public List<Operation> listAll() {
        return list("from " + Operation.class.getName() + " o order by o.module.name, o.name", null);
    }

    public Operation loadByName(final String moduleName, final String operationName) throws EntityNotFoundException {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("module", moduleName);
        params.put("operation", operationName);
        final Operation operation = uniqueResult("from " + Operation.class.getName() + " o where o.module.name = :module and o.name = :operation", params);
        if (operation == null) {
            throw new EntityNotFoundException(Operation.class);
        }
        return operation;
    }
}

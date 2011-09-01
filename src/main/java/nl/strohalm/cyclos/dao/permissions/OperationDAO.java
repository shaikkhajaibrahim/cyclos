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

import java.util.List;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Operation;

/**
 * Data Access Object interface for permission operations
 * @author luis
 */
public interface OperationDAO extends BaseDAO<Operation>, InsertableDAO<Operation> {

    /**
     * Lists all operations, ordering by module name and operation name
     */
    List<Operation> listAll();

    /**
     * Loads an operation by module name and operation name
     * @throws EntityNotFoundException The given operation does not exist
     */
    Operation loadByName(String moduleName, String operationName) throws EntityNotFoundException;
}

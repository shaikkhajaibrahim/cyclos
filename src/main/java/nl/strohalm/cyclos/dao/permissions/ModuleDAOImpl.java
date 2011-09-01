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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Module;

/**
 * The default implementation for ModuleDAO
 * @author luis
 */
public class ModuleDAOImpl extends BaseDAOImpl<Module> implements ModuleDAO {

    public ModuleDAOImpl() {
        super(Module.class);
    }

    public List<Module> listAll() {
        return list("from " + Module.class.getName(), null);
    }

    public Module loadByName(final String name) throws EntityNotFoundException {
        final Map<String, ?> params = Collections.singletonMap("name", name);
        final Module module = uniqueResult("from " + Module.class.getName() + " m where m.name = :name", params);
        if (module == null) {
            throw new EntityNotFoundException(Module.class);
        }
        return module;
    }

}

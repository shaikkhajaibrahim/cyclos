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
package nl.strohalm.cyclos.entities.access;

import java.util.Collection;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.groups.Group;

/**
 * An operation is a possible operation over a module
 * @author luis
 */
public class Operation extends Entity implements Comparable<Operation> {

    public static enum Relationships implements Relationship {
        MODULE("module"), PERMISSIONS("permissions");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long serialVersionUID = 7876923613014537349L;
    private String            messageKey;
    private Module            module;
    private String            name;
    private Collection<Group> permissions;

    public int compareTo(final Operation o) {
        if (o == null) {
            return -1;
        }
        int comp = module.compareTo(o.module);
        if (comp == 0) {
            comp = name.compareTo(o.name);
        }
        return comp;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Module getModule() {
        return module;
    }

    public String getName() {
        return name;
    }

    public Collection<Group> getPermissions() {
        return permissions;
    }

    public void setMessageKey(final String messageKey) {
        this.messageKey = messageKey;
    }

    public void setModule(final Module module) {
        this.module = module;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPermissions(final Collection<Group> groups) {
        permissions = groups;
    }

    @Override
    public String toString() {
        return getId() + " - " + (module == null ? name : module.getName() + "." + name);
    }
}

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
import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * A module is an entity that represents a group of actions.
 * @author luis
 */
public class Module extends Entity implements Comparable<Module> {

    public static enum Relationships implements Relationship {
        OPERATIONS("operations");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static enum Type implements StringValuedEnum {
        BASIC("B", "basic", false), ADMIN_SYSTEM("AS", "system", false), ADMIN_MEMBER("AM", "adminMember", true), ADMIN_ADMIN("AA", "adminAdmin", false), MEMBER("M", "member", true), BROKER("BK", "broker", true), OPERATOR("O", "operator", true);

        /**
         * Returns a type for the given operation name
         */
        public static Type getByModuleName(final String operation) {
            if (operation != null) {
                for (final Type type : values()) {
                    if (operation.startsWith(type.getPrefix())) {
                        return type;
                    }
                }
            }
            throw new IllegalArgumentException(operation);
        }

        private final String  value;
        private final String  prefix;

        private final boolean relatedToMember;

        private Type(final String value, final String prefix, final boolean relatedToMember) {
            this.value = value;
            this.prefix = prefix;
            this.relatedToMember = relatedToMember;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getValue() {
            return value;
        }

        public boolean isRelatedToMember() {
            return relatedToMember;
        }
    }

    private static final long     serialVersionUID = -1828686608405198166L;

    private String                messageKey;
    private String                name;
    private Collection<Operation> operations;
    private Type                  type;

    public Module() {
    }

    public int compareTo(final Module o) {
        if (o == null) {
            return -1;
        }
        return name.compareTo(o.name);
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getName() {
        return name;
    }

    public Collection<Operation> getOperations() {
        return operations;
    }

    public Type getType() {
        return type;
    }

    public void setMessageKey(final String messageKey) {
        this.messageKey = messageKey;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setOperations(final Collection<Operation> operations) {
        this.operations = operations;
    }

    public void setType(final Type type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return getId() + " - " + name;
    }
}

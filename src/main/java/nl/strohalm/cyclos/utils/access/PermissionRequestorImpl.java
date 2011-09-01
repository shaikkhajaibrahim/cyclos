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
package nl.strohalm.cyclos.utils.access;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import nl.strohalm.cyclos.entities.groups.Group.Nature;
import nl.strohalm.cyclos.entities.members.Member;

/**
 * IPermissionRequestor implementation.
 * @see nl.strohalm.cyclos.utils.access.IPermissionRequestor
 * @author ameyer
 */
public class PermissionRequestorImpl implements IPermissionRequestor, Serializable {
    private static class PermissionImpl implements Permission {
        private String module;
        private String operation;

        private PermissionImpl(final String module, final String operation) {
            this.module = module;
            this.operation = operation;
        }

        public Class<? extends Annotation> annotationType() {
            return Permission.class;
        }

        public String module() {
            return module;
        }

        public String operation() {
            return operation;
        }

        @Override
        public String toString() {
            return "Perm[module: " + module + ", operation: " + operation + "]";
        }
    }

    /**
     * Container class with the required permissions and related members to be checked for a group's nature
     * @author ameyer
     */
    private static final class PermissionNatureInfo {
        private Collection<Permission>      permissionsToCheck;

        private Collection<Member>          relatedMembers;

        @SuppressWarnings("unchecked")
        private static PermissionNatureInfo EMPTY_INFO = new PermissionNatureInfo(Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        private PermissionNatureInfo(final Collection<Permission> permissions, final Collection<Member> related) {
            permissionsToCheck = permissions;
            relatedMembers = related;
        }

        @Override
        public String toString() {
            return "Permissions: " + permissionsToCheck.toString() + ", Related: " + relatedMembers;
        }

        private void add(final Member m) {
            relatedMembers.add(m);
        }

        private void add(final Permission perm) {
            permissionsToCheck.add(perm);
        }
    }

    private static final long                 serialVersionUID = 1L;

    private Map<Nature, PermissionNatureInfo> permissions;

    public PermissionRequestorImpl() {
        permissions = new EnumMap<Nature, PermissionNatureInfo>(Nature.class);
    }

    /**
     * Marks that an Admin is allowed to do the action (without any extra permission)
     */
    public PermissionRequestorImpl admin() {
        ensureEntryForNature(Nature.ADMIN);
        return this;
    }

    /**
     * Add the permissions required for an Admin
     */
    public PermissionRequestorImpl adminPermissions(final String module, final String... operations) {
        return addPermission(Nature.ADMIN, module, operations);
    }

    /**
     * Marks that a Broker is allowed to do the action (without any extra permission)
     */
    public PermissionRequestorImpl broker() {
        ensureEntryForNature(Nature.BROKER);
        return this;
    }

    /**
     * Add the permissions required for a Broker
     */
    public PermissionRequestorImpl brokerPermissions(final String module, final String... operations) {
        return addPermission(Nature.BROKER, module, operations);
    }

    public boolean isAllowed(final Nature groupNature) {
        return permissions.containsKey(groupNature);
    }

    public Collection<Member> managedMembers(final Nature groupNature) {
        return entryForNature(groupNature).relatedMembers;
    }

    /**
     * Add the required members that the user (Admin/Broker/Member/Opertor) must manages
     */
    public PermissionRequestorImpl manages(final Member... managed) {
        return adminManages(managed).memberManages(managed).brokerManages(managed).operatorManages(managed);
    }

    /**
     * Marks that a Member is allowed to do the action (without any extra permission)
     */
    public PermissionRequestorImpl member() {
        ensureEntryForNature(Nature.MEMBER);
        return this;
    }

    /**
     * Add the permissions required for a Member
     */
    public PermissionRequestorImpl memberPermissions(final String module, final String... operations) {
        return addPermission(Nature.MEMBER, module, operations);
    }

    /**
     * Marks that an Operator is allowed to do the action (without any extra permission)
     */
    public PermissionRequestorImpl operator() {
        ensureEntryForNature(Nature.OPERATOR);
        return this;
    }

    /**
     * Add the permissions required for a Operator
     */
    public PermissionRequestorImpl operatorPermissions(final String module, final String... operations) {
        return addPermission(Nature.OPERATOR, module, operations);
    }

    public Collection<Permission> requiredPermissions(final Nature groupNature) {
        return entryForNature(groupNature).permissionsToCheck;
    }

    @Override
    public String toString() {
        return permissions.toString();
    }

    private PermissionRequestorImpl addPermission(final Nature groupNature, final String module, final String... operations) {
        if (operations == null) {
            return this;
        }
        for (final String op : operations) {
            ensureEntryForNature(groupNature).add(new PermissionImpl(module, op));
        }
        return this;
    }

    private PermissionRequestorImpl addRelatedMember(final Nature groupNature, final Member... related) {
        if (related == null) {
            return this;
        }
        for (final Member rel : related) {
            ensureEntryForNature(groupNature).add(rel);
        }

        return this;
    }

    private PermissionRequestorImpl adminManages(final Member... managed) {
        return addRelatedMember(Nature.ADMIN, managed);
    }

    private PermissionRequestorImpl brokerManages(final Member... managed) {
        return addRelatedMember(Nature.BROKER, managed);
    }

    private PermissionNatureInfo ensureEntryForNature(final Nature nature) {
        PermissionNatureInfo info = permissions.get(nature);
        if (info == null) {
            info = new PermissionNatureInfo(new ArrayList<Permission>(), new ArrayList<Member>());
            permissions.put(nature, info);
        }
        return info;
    }

    private PermissionNatureInfo entryForNature(final Nature nature) {
        final PermissionNatureInfo info = permissions.get(nature);
        return info == null ? PermissionNatureInfo.EMPTY_INFO : info;
    }

    private PermissionRequestorImpl memberManages(final Member... managed) {
        return addRelatedMember(Nature.MEMBER, managed);
    }

    private PermissionRequestorImpl operatorManages(final Member... managed) {
        return addRelatedMember(Nature.OPERATOR, managed);
    }
}

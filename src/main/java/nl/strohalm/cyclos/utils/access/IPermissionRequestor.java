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

import java.util.Collection;

import nl.strohalm.cyclos.entities.groups.Group.Nature;
import nl.strohalm.cyclos.entities.members.Member;

/**
 * Interface used to programmatically control business logic execution.<br>
 * You can implement it and specify the required permissions, the allowed actions and the related members to be checked.
 * @see nl.strohalm.cyclos.utils.access.PermissionRequestorImpl
 * @see nl.strohalm.cyclos.services.permissions.PermissionService#checkPermissions(IPermissionRequestor)
 * @author ameyer
 */
public interface IPermissionRequestor {
    /**
     * @param groupNature the group to check if it is allowed to do the action.
     * @return true if the action is allowed to the specified group.
     */
    boolean isAllowed(Nature groupNature);

    /**
     * @param groupNature the group from which we will get the related member.
     * @return the required (related) members (or null/empty) to be checked for the specified group. <br>
     * See {@link nl.strohalm.cyclos.services.permissions.PermissionService#checkPermissions(IPermissionRequestor)} for a description of<br>
     * the management relationship.
     */
    Collection<Member> managedMembers(Nature groupNature);

    /**
     * @param groupNature the group from which we will get the required permissions.
     * @return the required permissions (or null/empty) for the specified group's nature.<br>
     */
    Collection<Permission> requiredPermissions(Nature groupNature);
}

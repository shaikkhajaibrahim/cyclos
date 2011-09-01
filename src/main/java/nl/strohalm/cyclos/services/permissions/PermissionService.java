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
package nl.strohalm.cyclos.services.permissions;

import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IPermissionRequestor;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for group permissions This service use to control permissions and to list the permissions by group.
 * @author rafael
 */
public interface PermissionService extends Service {

    /**
     * Checks if the group of the logged user is allowed to do an action according to the specified requestor. <br>
     * The following is checked:
     * <ul>
     * <li>User type (e.g. if the requestor only allows an admin action and the logged user is a member then the check will fails).</li>
     * <li>User permissions.</li>
     * <li>User management relationship.</li>
     * </ul>
     * Management relationship (related member):
     * <ul>
     * <li>An <b>admin</b> can manages his managed members (specified through a permission).</li>
     * <li>A <b>broker</b> can manages his brokered members (a member whose broker is equals to him).</li>
     * <li>A <b>member</b> can manages himself.</li>
     * <li>An <b>operator</b> can manages his owner member.</li>
     * </ul>
     * @see nl.strohalm.cyclos.utils.access.IPermissionRequestor
     * @return true if the logged user type is allowed, has at least one of the required permissions<br>
     * and can manage at least one of the specified member.
     */
    @DontEnforcePermission(traceable = false)
    public boolean checkPermissions(final IPermissionRequestor permissionRequestor);

    /**
     * Returns a collection containing the cached versions of each given operation
     */
    @SystemAction
    public Collection<Operation> getCachedOperations(Collection<Operation> operations);

    /**
     * Lists the modules by group nature
     */
    @AdminAction( {
            @Permission(module = "systemGroups", operation = "manageAdmin"),
            @Permission(module = "systemGroups", operation = "manageBroker"),
            @Permission(module = "systemGroups", operation = "manageMember") })
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @IgnoreMember
    public List<Module> listModules(Group.Nature nature);

    /**
     * Returns a module by name
     * @throws EntityNotFoundException The given module does not exists
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @IgnoreMember
    public Module loadModule(String name) throws EntityNotFoundException;

    /**
     * Returns an operation by module and operation name
     * @throws EntityNotFoundException The given module does not exists
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @IgnoreMember
    public Operation loadOperation(String moduleName, String operationName);

    /**
     * Checks if the specified group member has permission to the module and operation
     * @return The boolean result for permission check
     */
    @DontEnforcePermission(traceable = false)
    boolean checkPermission(Group group, String module, String operation);

    /**
     * Checks if the group of the logged member has permission to the module and operation
     * @return The boolean result for permission check
     */
    @DontEnforcePermission(traceable = false)
    boolean checkPermission(String module, String operation);

    /**
     * Imports new modules and operations
     */
    @SystemAction
    void importNew();

    /**
     * Refreshes cached list of permissions for all groups
     */
    @SystemAction
    void refreshCache();

    /**
     * Refreshes cached list of permissions for a given group
     */
    @SystemAction
    void refreshCache(Group group);
}

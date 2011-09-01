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
package nl.strohalm.cyclos.http.lifecycle;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.services.permissions.PermissionService;

/**
 * Permission initialization - check for new operations, inserting them
 * @author luis
 */
public class PermissionInitialization implements ContextInitialization {

    private PermissionService permissionService;

    public PermissionService getPermissionService() {
        return permissionService;
    }

    public void init(final ServletContext context) {
        context.setAttribute("permissionService", permissionService);
        permissionService.importNew();
    }

    @Inject
    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }
}

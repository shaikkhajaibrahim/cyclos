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
package nl.strohalm.cyclos.controls.access;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.Group.Nature;
import nl.strohalm.cyclos.services.access.UserLoginDTO;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to list the connected users
 * @author luis
 */
public class ListConnectedUsersAction extends BaseAction {

    private DataBinder<Collection<Group.Nature>> dataBinder;

    public DataBinder<Collection<Group.Nature>> getDataBinder() {
        if (dataBinder == null) {
            dataBinder = SimpleCollectionBinder.instance(Group.Nature.class, "nature");
        }
        return dataBinder;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final ListConnectedUsersForm form = context.getForm();
        final HttpServletRequest request = context.getRequest();
        final HttpSession session = request.getSession();
        List<UserLoginDTO> users;
        if (context.isAdmin()) {
            Collection<Nature> natures = getDataBinder().readFromString(form);
            if (RequestHelper.isFromMenu(request)) {
                session.removeAttribute("_natures");
            } else {
                if (CollectionUtils.isEmpty(natures)) {
                    natures = (Collection<Nature>) session.getAttribute("_natures");
                } else {
                    session.setAttribute("_natures", natures);
                }
            }
            request.setAttribute("selectedNatures", natures);
            users = getAccessService().listConnectedUsers(natures);

            final PermissionService permissionService = getPermissionService();
            final List<Group.Nature> groupNatures = new ArrayList<Nature>();
            if (permissionService.checkPermission("systemStatus", "viewConnectedAdmins")) {
                groupNatures.add(Group.Nature.ADMIN);
            }
            if (permissionService.checkPermission("systemStatus", "viewConnectedMembers")) {
                groupNatures.add(Group.Nature.MEMBER);
            }
            if (permissionService.checkPermission("systemStatus", "viewConnectedBrokers")) {
                groupNatures.add(Group.Nature.BROKER);
            }
            if (permissionService.checkPermission("systemStatus", "viewConnectedOperators")) {
                groupNatures.add(Group.Nature.OPERATOR);
            }
            request.setAttribute("groupNatures", groupNatures);
            request.setAttribute("canDisconnectAdmin", permissionService.checkPermission("adminAdminAccess", "disconnect"));
            request.setAttribute("canDisconnectMember", permissionService.checkPermission("adminMemberAccess", "disconnect"));
        } else {
            users = getAccessService().listConnectedOperators();
        }
        request.setAttribute("users", users);
        return context.getInputForward();
    }

}

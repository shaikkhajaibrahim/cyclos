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
package nl.strohalm.cyclos.controls.admins;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.elements.SearchElementsAction;
import nl.strohalm.cyclos.entities.customization.fields.AdminCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.members.FullTextAdminQuery;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.query.QueryParameters;

/**
 * Action to search administrators
 * @author luis
 */
public class SearchAdminsAction extends SearchElementsAction<FullTextAdminQuery> {

    private GroupService groupService;

    public GroupService getGroupService() {
        return groupService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected Class<? extends CustomFieldValue> getCustomFieldValueClass() {
        return AdminCustomFieldValue.class;
    }

    @Override
    protected Class<FullTextAdminQuery> getQueryClass() {
        return FullTextAdminQuery.class;
    }

    @Override
    protected QueryParameters prepareForm(final ActionContext context) {
        final FullTextAdminQuery query = (FullTextAdminQuery) super.prepareForm(context);

        final HttpServletRequest request = context.getRequest();

        // Store the groups
        final GroupQuery groupQuery = new GroupQuery();
        groupQuery.setNatures(Group.Nature.ADMIN);
        request.setAttribute("groups", groupService.search(groupQuery));

        // Store the possible groups for new admin
        if (getPermissionService().checkPermission("adminAdmins", "register")) {
            final GroupQuery possibleGroupQuery = new GroupQuery();
            possibleGroupQuery.setNatures(Group.Nature.ADMIN);
            possibleGroupQuery.setStatus(Group.Status.NORMAL);
            request.setAttribute("possibleNewGroups", groupService.search(possibleGroupQuery));
        }
        return query;
    }
}

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
package nl.strohalm.cyclos.controls.elements;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.remarks.GroupRemark;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.RemarkService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.StringUtils;

/**
 * Action used to change an element group
 * @author luis
 */
public class ChangeElementGroupAction extends BaseFormAction {

    private ElementService elementService;
    private GroupService   groupService;
    private RemarkService  remarkService;

    public ElementService getElementService() {
        return elementService;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    public RemarkService getRemarkService() {
        return remarkService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Inject
    public void setRemarkService(final RemarkService remarkService) {
        this.remarkService = remarkService;
    }

    @Override
    protected void formAction(final ActionContext context) throws Exception {
        final ChangeElementGroupForm form = context.getForm();
        final String comments = form.getComments();
        final Element element = elementService.load(form.getElementId());
        if (element instanceof Member) {
            final MemberGroup newGroup = EntityHelper.reference(MemberGroup.class, form.getNewGroupId());
            elementService.changeMemberGroup((Member) element, newGroup, comments);
        } else if (element instanceof Operator) {
            final OperatorGroup newGroup = EntityHelper.reference(OperatorGroup.class, form.getNewGroupId());
            elementService.changeOperatorGroup((Operator) element, newGroup, comments);
        } else {
            final AdminGroup newGroup = EntityHelper.reference(AdminGroup.class, form.getNewGroupId());
            elementService.changeAdminGroup((Administrator) element, newGroup, comments);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ChangeElementGroupForm form = context.getForm();
        Element element = null;
        try {
            element = elementService.load(form.getElementId(), Element.Relationships.GROUP);
            final Element loggedElement = context.getElement();
            if (loggedElement.equals(element)) {
                throw new Exception();
            }
        } catch (final Exception e) {
            element = null;
        }
        if (element == null) {
            throw new ValidationException();
        }

        // Retrieve the possible new groups
        final List<? extends Group> possible = groupService.getPossibleNewGroups(element.getGroup());
        final Group currentGroup = element.getGroup();
        form.setNewGroupId(currentGroup.getId());
        request.setAttribute("permanentlyRemoved", currentGroup.getStatus() == Group.Status.REMOVED);

        // Retrieve the history
        final List<GroupRemark> history = remarkService.listGroupRemarksFor(element);

        request.setAttribute("element", element);
        request.setAttribute("possibleGroups", possible);
        request.setAttribute("history", history);
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final ChangeElementGroupForm form = context.getForm();
        final ValidationException val = new ValidationException();
        val.setPropertyKey("elementId", "member.member");
        val.setPropertyKey("newGroupId", "changeGroup.new");
        val.setPropertyKey("comments", "remark.comments");
        if (form.getElementId() <= 0) {
            val.addPropertyError("elementId", new RequiredError());
        }
        if (form.getNewGroupId() <= 0) {
            val.addPropertyError("newGroupId", new RequiredError());
        }
        if (StringUtils.isEmpty(form.getComments())) {
            val.addPropertyError("comments", new RequiredError());
        }
        val.throwIfHasErrors();
    }

}
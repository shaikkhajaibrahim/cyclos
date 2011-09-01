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

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to manage an user's passwords: login and transaction
 * @author luis
 */
public class ManagePasswordsAction extends BaseAction {

    private ElementService elementService;

    public ElementService getElementService() {
        return elementService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ManagePasswordsForm form = context.getForm();
        final long userId = form.getUserId();
        User user = null;
        try {
            if (userId > 0L) {
                user = elementService.loadUser(userId, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
            }
            if (user == null) {
                throw new Exception();
            }
        } catch (final Exception e) {
            throw new ValidationException();
        }

        Element element = user.getElement();
        if (element instanceof Operator) {
            element = getFetchService().fetch(element, RelationshipHelper.nested(Operator.Relationships.MEMBER, Element.Relationships.GROUP));
        }
        final BasicGroupSettings groupSettings = element.getGroup().getBasicSettings();
        boolean sendPasswordByEmail = false;
        if (user instanceof MemberUser) {
            sendPasswordByEmail = ((MemberUser) user).getMember().getMemberGroup().getMemberSettings().isSendPasswordByEmail();
        }

        boolean canChangePassword = false;
        boolean canResetPassword = false;
        boolean canManageTransactionPassword = false;

        final boolean tpUsed = groupSettings.getTransactionPassword() != null && groupSettings.getTransactionPassword().isUsed();

        // Determine which the actions can be performed
        if (context.isAdmin()) {
            canChangePassword = getPermissionService().checkPermission("adminMemberAccess", "changePassword");
            // Only can reset if send password by mail is enabled
            canResetPassword = sendPasswordByEmail && getPermissionService().checkPermission("adminMemberAccess", "resetPassword");
            // Only can change TP if it is used
            canManageTransactionPassword = tpUsed && getPermissionService().checkPermission("adminMemberAccess", "transactionPassword");
        } else if (context.isMember()) {
            if (user instanceof OperatorUser) {
                // A member can manage it's operators passwords
                canChangePassword = true;
                canManageTransactionPassword = groupSettings.getTransactionPassword() != null && groupSettings.getTransactionPassword().isUsed();
                ;
            } else {
                // A member accessing as a broker
                if (!(user instanceof MemberUser) || !context.isBrokerOf((Member) element)) {
                    throw new ValidationException();
                }
                canChangePassword = getPermissionService().checkPermission("brokerMemberAccess", "changePassword");
                // Only can reset if send password by mail is enabled
                canResetPassword = sendPasswordByEmail && getPermissionService().checkPermission("brokerMemberAccess", "resetPassword");
                canManageTransactionPassword = tpUsed && getPermissionService().checkPermission("brokerMemberAccess", "transactionPassword");
            }
        }
        request.setAttribute("ofAdmin", user instanceof AdminUser);
        request.setAttribute("ofOperator", user instanceof OperatorUser);
        request.setAttribute("user", user);
        request.setAttribute("canChangePassword", canChangePassword);
        request.setAttribute("canResetPassword", canResetPassword);
        request.setAttribute("canManageTransactionPassword", canManageTransactionPassword);
        return context.getInputForward();
    }
}
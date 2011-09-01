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

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to reenable a user to login immediately if his login was blocked by wrong password tries
 * @author luis
 */
public class AllowUserLoginAction extends BaseAction {

    private ElementService elementService;

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final AllowUserLoginForm form = context.getForm();
        final long id = form.getUserId();
        if (id <= 0L) {
            throw new ValidationException();
        }
        String param;
        String forward;
        final User user = elementService.loadUser(id);
        if (user instanceof MemberUser) {
            getAccessService().reenableMemberLogin((MemberUser) user);
            param = "memberId";
            forward = "memberProfile";
        } else if (user instanceof AdminUser) {
            getAccessService().reenableAdminLogin((AdminUser) user);
            param = "adminId";
            forward = "adminProfile";
        } else { // user instanceof OperatorUser
            getAccessService().reenableOperatorLogin((OperatorUser) user);
            param = "operatorId";
            forward = "operatorProfile";
        }
        context.sendMessage("profile.userAllowedToLogin");
        return ActionHelper.redirectWithParam(context.getRequest(), context.findForward(forward), param, id);
    }

}
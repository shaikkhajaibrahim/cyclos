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
package nl.strohalm.cyclos.controls.posweb;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.access.LoginForm;
import nl.strohalm.cyclos.controls.posweb.PosWebHelper.Action;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.permissions.PermissionService;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used for posweb login
 * @author luis
 */
public class LoginAction extends nl.strohalm.cyclos.controls.access.LoginAction {

    private PermissionService permissionService;

    @Inject
    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Override
    protected ActionForward alreadyLoggedForward(final ActionMapping mapping, final HttpServletRequest request, final HttpServletResponse response, final LoginForm form, final User user) {
        return loginForward(mapping, request, response, form, user);
    }

    @Override
    protected ActionForward doLogin(final ActionMapping mapping, final HttpServletRequest request, final HttpServletResponse response, final LoginForm form) {
        final HttpSession session = request.getSession();
        session.setAttribute("isPosWeb", true);
        session.setAttribute("isWebShop", false);
        // Ensure the principal for login will always be user
        form.setPrincipalType(Principal.USER.name());
        return super.doLogin(mapping, request, response, form);
    }

    @Override
    protected ActionForward handleDisplay(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        request.setAttribute("isOperator", PosWebHelper.isOperator(request));
        return super.handleDisplay(mapping, actionForm, request, response);
    }

    @Override
    protected boolean isMemberRequired(final HttpServletRequest request) {
        return PosWebHelper.isOperator(request);
    }

    @Override
    protected ActionForward loginForward(final ActionMapping mapping, final HttpServletRequest request, final HttpServletResponse response, final LoginForm form, final User user) {
        final Group group = user.getElement().getGroup();
        boolean canPay;
        boolean canReceive;
        if (group instanceof MemberGroup) {
            // Members depend on the entry url
            final Action action = PosWebHelper.getAction(request);
            canPay = action.canPay();
            canReceive = action.canReceive();
        } else {
            // Operators depends on permissions
            canPay = permissionService.checkPermission(group, "operatorPayments", "externalMakePayment");
            canReceive = permissionService.checkPermission(group, "operatorPayments", "externalReceivePayment");
        }
        // Redirect the operator according to the permissions
        if (!canPay && !canReceive) {
            // An operator with no external permissions is logged!
            request.getSession().removeAttribute("loggedUser");
            throw new PermissionDeniedException();
        } else if (canReceive) {
            return mapping.findForward("receivePayment");
        } else {
            return mapping.findForward("makePayment");
        }
    }

    @Override
    protected String resolveErrorReturnTo(final ActionMapping mapping, final HttpServletRequest request, final HttpServletResponse response, final LoginForm form) {
        return PosWebHelper.loginUrl(request);
    }
}

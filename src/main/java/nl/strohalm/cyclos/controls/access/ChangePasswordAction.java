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
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.services.access.ChangeLoginPasswordDTO;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.CredentialsAlreadyUsedException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.conversion.CoercionConverter;

import org.apache.struts.action.ActionForward;

/**
 * Changes the password of a given element
 * @author luis
 */
public class ChangePasswordAction extends BaseFormAction {

    private DataBinder<ChangeLoginPasswordDTO> dataBinder;
    private ElementService                     elementService;

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final User user = ofUser(context);
        getAccessService().canChangePassword(user);
    }

    @Override
    protected ActionForward handleDisplay(final ActionContext context) throws Exception {
        final ChangePasswordForm form = context.getForm();
        final User ofUser = ofUser(context);
        final HttpServletRequest request = context.getRequest();
        request.setAttribute("user", ofUser);

        final BasicGroupSettings basicSettings = ofUser.getElement().getGroup().getBasicSettings();
        request.setAttribute("passwordLength", basicSettings.getPasswordLength());

        request.setAttribute("ofAdmin", ofUser instanceof AdminUser);
        final User loggedUser = context.getUser();
        request.setAttribute("myPassword", loggedUser.equals(ofUser));
        request.setAttribute("shouldRequestOldPassword", shouldRequestOldPassword(context, ofUser));

        if (form.isEmbed()) {
            return new ActionForward("/pages/access/changePassword.jsp");
        } else {
            return context.getInputForward();
        }
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ChangePasswordForm form = context.getForm();
        final ChangeLoginPasswordDTO params = getDataBinder().readFromString(form);
        final User user = getFetchService().fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        try {
            if (user == null || context.getUser().equals(user)) {
                final User updatedUser = getAccessService().changeMyPassword(params, request.getRemoteAddr());
                context.getSession().setAttribute("loggedUser", updatedUser);
            } else if (user instanceof AdminUser) {
                getAccessService().changeAdminPassword(params);
            } else if (user instanceof OperatorUser) {
                getAccessService().changeOperatorPassword(params);
            } else {
                getAccessService().changeMemberPassword(params);
            }
            context.sendMessage("changePassword.modified");
        } catch (final InvalidCredentialsException e) {
            final String key = "changePassword.error.incorrect";
            return context.sendError(key);
        } catch (final BlockedCredentialsException e) {
            request.getSession().invalidate();
            return context.sendError("changePassword.error.userBlocked");
        } catch (final CredentialsAlreadyUsedException e) {
            return context.sendError("changePassword.error.alreadyUsed");
        }

        ActionForward forward;
        if (form.isEmbed()) {
            forward = context.findForward("managePasswords");
        } else if (context.isAdmin()) {
            forward = context.findForward("adminHome");
        } else if (context.isMember()) {
            forward = context.findForward("memberHome");
        } else {
            forward = context.findForward("operatorHome");
        }
        return ActionHelper.redirectWithParam(context.getRequest(), forward, "userId", form.getUserId());
    }

    /**
     * Return the user we are changing the password
     */
    protected User ofUser(final ActionContext context) {
        final ChangePasswordForm form = context.getForm();
        final long userId = form.getUserId();
        if (userId == 0 || userId < 0) {
            return context.getUser();
        }
        return elementService.loadUser(userId, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
    }

    /**
     * Determines if the old password should be requested
     */
    protected boolean shouldRequestOldPassword(final ActionContext context, final User ofUser) {
        final User loggedUser = context.getUser();
        return loggedUser.equals(ofUser);
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final ChangePasswordForm form = context.getForm();
        final ChangeLoginPasswordDTO params = getDataBinder().readFromString(form);
        final User user = getFetchService().fetch(params.getUser());
        if (user == null || context.getUser().equals(user)) {
            getAccessService().validateChangeMyPassword(params);
        } else if (user instanceof AdminUser) {
            getAccessService().validateChangeAdminPassword(params);
        } else if (user instanceof OperatorUser) {
            getAccessService().validateChangeOperatorPassword(params);
        } else {
            getAccessService().validateChangeMemberPassword(params);
        }
    }

    private DataBinder<ChangeLoginPasswordDTO> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<ChangeLoginPasswordDTO> binder = BeanBinder.instance(ChangeLoginPasswordDTO.class);
            binder.registerBinder("user", PropertyBinder.instance(User.class, "userId"));
            binder.registerBinder("oldPassword", PropertyBinder.instance(String.class, "oldPassword", CoercionConverter.instance(String.class)));
            binder.registerBinder("newPassword", PropertyBinder.instance(String.class, "newPassword", CoercionConverter.instance(String.class)));
            binder.registerBinder("newPasswordConfirmation", PropertyBinder.instance(String.class, "newPasswordConfirmation", CoercionConverter.instance(String.class)));
            binder.registerBinder("forceChange", PropertyBinder.instance(boolean.class, "forceChange"));
            dataBinder = binder;
        }
        return dataBinder;
    }
}

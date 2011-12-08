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
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.services.access.ChangeLoginPasswordDTO;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.CredentialsAlreadyUsedException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.conversion.CoercionConverter;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import org.apache.struts.action.ActionForward;

/**
 * Action used to reset a member's password and send it by mail
 * @author luis
 */
public class ResetPasswordAction extends BaseAction {

    private ElementService elementService;
    private BeanBinder<ChangeLoginPasswordDTO> dataBinder;

    public ElementService getElementService() {
        return elementService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final ResetPasswordForm form = context.getForm();
        final long userId = form.getUserId();
        if (userId <= 0L) {
            throw new ValidationException();
        }
        final ChangeLoginPasswordDTO params = getDataBinder().readFromString(form);

        try{
            getAccessService().resetPasswordOnly(params);
            context.sendMessage("changePassword.resetPasswordDone");
        } catch (final Exception e) {
            return context.sendError("changePassword.error.internal");
        }

        return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "userId", userId);
    }


    private DataBinder<ChangeLoginPasswordDTO> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<ChangeLoginPasswordDTO> binder = BeanBinder.instance(ChangeLoginPasswordDTO.class);
            binder.registerBinder("user", PropertyBinder.instance(User.class, "userId"));
            dataBinder = binder;
        }
        return dataBinder;
    }

}

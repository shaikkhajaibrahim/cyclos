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
package nl.strohalm.cyclos.controls.general;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.exceptions.MailSendingException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to invite a person to join cyclos
 * @author luis
 */
public class InviteAction extends BaseFormAction {

    private ElementService elementService;

    public ElementService getElementService() {
        return elementService;
    }

    @Inject
    public void setElementService(final ElementService memberService) {
        elementService = memberService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final InviteForm form = context.getForm();
        final String to = form.getTo();
        try {
            elementService.invitePerson(to);
            context.sendMessage("invite.sent", to);
            final MailSendingException currentException = MailHandler.getCurrentException();
            if (currentException != null) {
                throw currentException;
            }
        } catch (final Exception e) {
            context.sendMessage("error.sendingMail", to);
        }
        return context.findForward("home");
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final InviteForm form = context.getForm();
        if (StringUtils.isEmpty(form.getTo())) {
            throw new ValidationException("to", "member.email", new RequiredError());
        }
    }
}

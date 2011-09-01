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
package nl.strohalm.cyclos.controls.webshop;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BasePublicFormAction;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.WebShopTicket;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.transactions.DoPaymentDTO;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TicketService;
import nl.strohalm.cyclos.services.transactions.exceptions.CreditsException;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used to confirm a
 * 
 * @author luis
 */
public class ConfirmWebShopPaymentAction extends BasePublicFormAction {

    private AccessService         accessService;
    private ChannelService        channelService;
    private PaymentService        paymentService;
    private TransactionFeeService transactionFeeService;
    private TicketService         ticketService;

    @Inject
    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    @Inject
    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Inject
    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Inject
    public void setTicketService(final TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Inject
    public void setTransactionFeeService(final TransactionFeeService transactionFeeService) {
        this.transactionFeeService = transactionFeeService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        final ConfirmWebShopPaymentForm form = (ConfirmWebShopPaymentForm) actionForm;
        final DoPaymentDTO paymentDTO = resolvePayment(request);
        final Member from = (Member) paymentDTO.getFrom();

        final HttpSession session = request.getSession();
        session.setAttribute("errorReturnTo", "/do/webshop/confirmPayment");

        try {
            // Check for transaction password
            if (shouldValidateTransactionPassword(mapping, actionForm, request, response, paymentDTO)) {
                accessService.checkTransactionPassword(null, from.getUsername(), form.getTransactionPassword(), request.getRemoteAddr());
            }
        } catch (final InvalidCredentialsException e) {
            throw new ValidationException("transactionPassword.error.invalid");
        } catch (final BlockedCredentialsException e) {
            cancelTicket(request, paymentDTO);
            throw new ValidationException("transactionPassword.error.blockedByTrials");
        }

        // Perform the actual payment
        Payment payment;
        try {
            // We must fool the model layer, pretending that there is a logged user
            LoggedUser.init(from.getUser(), request.getRemoteAddr());
            payment = paymentService.doPaymentFromMemberToMember(paymentDTO);

            // Store the payment on the session
            WebShopHelper.setPerformedPayment(session, payment);

            return mapping.findForward("success");
        } catch (final CreditsException e) {
            cancelTicket(request, paymentDTO);
            throw new ValidationException(ActionHelper.resolveErrorKey(e), ActionHelper.resolveParameters(e));
        } catch (final UnexpectedEntityException e) {
            cancelTicket(request, paymentDTO);
            throw new ValidationException("payment.error.invalidTransferType");
        } finally {
            LoggedUser.cleanup();
        }
    }

    @Override
    protected void prepareForm(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        final DoPaymentDTO payment = resolvePayment(request);
        request.setAttribute("payment", payment);
        request.setAttribute("requestTransactionPassword", shouldValidateTransactionPassword(mapping, actionForm, request, response, payment));

        final TransactionFeePreviewDTO fees = transactionFeeService.preview(payment.getFrom(), payment.getTo(), payment.getTransferType(), payment.getAmount());
        request.setAttribute("finalAmount", fees.getFinalAmount());
        request.setAttribute("fees", fees.getFees());
    }

    @Override
    protected void validateForm(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws ValidationException {
        final DoPaymentDTO payment = resolvePayment(request);
        if (shouldValidateTransactionPassword(mapping, actionForm, request, response, payment)) {
            final ConfirmWebShopPaymentForm form = (ConfirmWebShopPaymentForm) actionForm;
            if (StringUtils.isEmpty(form.getTransactionPassword())) {
                throw new ValidationException("_transactionPassword", "login.transactionPassword", new RequiredError());
            }
        }
    }

    private void cancelTicket(final HttpServletRequest request, final DoPaymentDTO payment) {
        final WebShopTicket ticket = (WebShopTicket) getFetchService().fetch(payment.getTicket());
        ticketService.cancelWebShopTicket(ticket, request.getRemoteAddr());

        final HttpSession session = request.getSession();
        session.removeAttribute("forceBack");
        session.setAttribute("errorReturnTo", ticket.getReturnUrl());
    }

    private DoPaymentDTO resolvePayment(final HttpServletRequest request) {
        final DoPaymentDTO payment = WebShopHelper.getUpdatedPayment(request.getSession());
        if (payment == null) {
            throw new ValidationException();
        }
        if (StringUtils.isEmpty(payment.getDescription())) {
            payment.setDescription(payment.getTransferType().getDescription());
        }
        return payment;
    }

    private boolean shouldValidateTransactionPassword(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response, final DoPaymentDTO payment) {
        final Channel channel = channelService.loadByInternalName(Channel.WEBSHOP);
        // Transaction password is only validated on default credentials
        if (channel.getCredentials() != Credentials.DEFAULT) {
            return false;
        }
        final Member member = getFetchService().fetch((Member) payment.getFrom(), Element.Relationships.USER);
        final ActionContext context = new ActionContext(mapping, actionForm, request, response, member.getUser(), getFetchService());
        final TransferType transferType = getFetchService().fetch(payment.getTransferType(), TransferType.Relationships.FROM);
        return context.isTransactionPasswordEnabled(transferType.getFrom());
    }
}

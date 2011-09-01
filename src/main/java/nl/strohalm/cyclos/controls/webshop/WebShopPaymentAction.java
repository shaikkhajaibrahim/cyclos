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

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BasePublicFormAction;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.access.User.TransactionPasswordStatus;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.WebShopTicket;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.LoginException;
import nl.strohalm.cyclos.services.transactions.DoPaymentDTO;
import nl.strohalm.cyclos.services.transactions.TicketService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.MessageHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Action used by a member to confirm a webshop payment
 * @author luis
 */
public class WebShopPaymentAction extends BasePublicFormAction {

    private ChannelService      channelService;
    private AccessService       accessService;
    private TransferTypeService transferTypeService;
    private TicketService       ticketService;

    @Inject
    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    @Inject
    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Inject
    public void setTicketService(final TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        final WebShopPaymentForm form = (WebShopPaymentForm) actionForm;

        // Clone the previous paymentDTO
        final DoPaymentDTO payment = resolvePayment(request).clone();

        // Check the user credentials
        final MemberUser user = checkCredentials(request, form, payment);

        // Complete the payment
        payment.setFrom(user.getMember());

        // Find the transfer type
        final TransferTypeQuery query = new TransferTypeQuery();
        query.setContext(TransactionContext.PAYMENT);
        query.setChannel(Channel.WEBSHOP);
        query.setCurrency(payment.getCurrency());
        query.setFromOwner(payment.getFrom());
        query.setToOwner(payment.getTo());
        query.setUsePriority(true);
        query.setUniqueResult();
        final List<TransferType> transferTypes = transferTypeService.search(query);
        if (CollectionUtils.isEmpty(transferTypes)) {
            throw new ValidationException("payment.error.noTransferType");
        }
        payment.setTransferType(transferTypes.iterator().next());

        // Store the updated payment in the session
        WebShopHelper.setUpdatedPayment(request.getSession(), payment);

        // Ensure that if TP should be validated, it's active
        if (shouldValidateTransactionPassword(mapping, actionForm, request, response, payment)) {
            if (user.getTransactionPasswordStatus() != TransactionPasswordStatus.ACTIVE) {
                cancelTicket(request, payment);
                throw new ValidationException("transactionPassword.error.pending");
            }
        }
        return mapping.findForward("success");
    }

    @Override
    protected void prepareForm(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) {
        // Ensure previous session data is removed
        WebShopHelper.setUpdatedPayment(request.getSession(), null);

        final WebShopPaymentForm form = (WebShopPaymentForm) actionForm;

        // Retrieve the paymentDTO
        final DoPaymentDTO payment = resolvePayment(request);
        request.setAttribute("payment", payment);
        final Credentials credentials = getLoginCredentials();
        request.setAttribute("credentials", credentials);
        final String credentialsString = MessageHelper.message(getServlet().getServletContext(), "channel.credentials." + credentials).toLowerCase();
        request.setAttribute("credentialsString", credentialsString);

        // Get the principal types
        final Channel channel = channelService.loadByInternalName(Channel.WEBSHOP);
        final Map<String, PrincipalType> principalTypes = new TreeMap<String, PrincipalType>();
        final PrincipalType selectedPrincipalType = channelService.resolvePrincipalType(channel.getInternalName(), form.getPrincipalType());
        form.setPrincipalType(selectedPrincipalType.toString());
        for (final PrincipalType principalType : channel.getPrincipalTypes()) {
            principalTypes.put(principalTypeLabel(principalType), principalType);
        }
        request.setAttribute("principalTypes", principalTypes);
        request.setAttribute("selectedPrincipalType", selectedPrincipalType);
        request.setAttribute("selectedPrincipalLabel", principalTypeLabel(selectedPrincipalType));

        // Get the credentials
        final AccessSettings accessSettings = SettingsHelper.getAccessSettings(getServlet().getServletContext());
        boolean useVirtualKeyboard;
        String virtualKeyboardChars = null;
        if (credentials == Credentials.TRANSACTION_PASSWORD) {
            useVirtualKeyboard = accessSettings.isVirtualKeyboardTransactionPassword();
            if (useVirtualKeyboard) {
                virtualKeyboardChars = accessSettings.getTransactionPasswordChars();
            }
        } else {
            useVirtualKeyboard = accessSettings.isVirtualKeyboard();
            if (useVirtualKeyboard && (accessSettings.isNumericPassword() || credentials == Credentials.PIN)) {
                virtualKeyboardChars = "0123456789";
            }
        }
        request.setAttribute("useVirtualKeyboard", useVirtualKeyboard);
        request.setAttribute("virtualKeyboardChars", virtualKeyboardChars);

        request.getSession().setAttribute("errorReturnTo", "/do/webshop/doPayment");
    }

    @Override
    protected void validateForm(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws ValidationException {
        final WebShopPaymentForm form = (WebShopPaymentForm) actionForm;
        final String principal = StringUtils.trimToNull(form.getPrincipal());
        if (principal == null) {
            final PrincipalType principalType = channelService.resolvePrincipalType(Channel.WEBSHOP, form.getPrincipalType());
            final ValidationException validationException = new ValidationException("principal", new RequiredError());
            final Principal principalEnum = principalType.getPrincipal();
            if (principalEnum == Principal.CUSTOM_FIELD) {
                validationException.setPropertyDisplayName("principal", principalType.getCustomField().getName());
            } else {
                validationException.setPropertyKey("principal", principalEnum.getKey());
            }
            throw validationException;
        }
        final String credentials = StringUtils.trimToNull(form.getCredentials());
        if (credentials == null) {
            throw new ValidationException("credentials", "channel.credentials." + getLoginCredentials(), new RequiredError());
        }
    }

    private void cancelTicket(final HttpServletRequest request, final DoPaymentDTO payment) {
        final WebShopTicket ticket = (WebShopTicket) getFetchService().fetch(payment.getTicket());
        ticketService.cancelWebShopTicket(ticket, request.getRemoteAddr());

        final HttpSession session = request.getSession();
        session.removeAttribute("forceBack");
        session.setAttribute("errorReturnTo", ticket.getReturnUrl());
    }

    private MemberUser checkCredentials(final HttpServletRequest request, final WebShopPaymentForm form, final DoPaymentDTO payment) {
        // Check the user password or pin
        MemberUser user;
        try {
            final String principal = form.getPrincipal();
            final PrincipalType principalType = channelService.resolvePrincipalType(Channel.WEBSHOP, form.getPrincipalType());

            // Check the credentials
            final AccountOwner to = payment.getTo();
            final Member toMember = (Member) (to instanceof Member ? to : null);
            user = accessService.checkCredentials(Channel.WEBSHOP, principalType, principal, form.getCredentials(), request.getRemoteAddr(), toMember);

            // Check if the member is related to the WebShop channel
            if (!accessService.isChannelEnabledForMember(Channel.WEBSHOP, user.getMember())) {
                // Not related. Cancel the ticket right now
                cancelTicket(request, payment);
                throw new ValidationException("webshop.error.paymentDisabled");
            }

        } catch (final BlockedCredentialsException e) {
            String key;
            switch (e.getCredentialsType()) {
                case TRANSACTION_PASSWORD:
                    key = "transactionPassword.error.blockedByTrials";
                    break;
                case PIN:
                    key = "pin.error.blocked";
                    break;
                default:
                    key = "login.error.blocked";
                    break;
            }
            throw new ValidationException(key);
        } catch (final LoginException e) {
            throw new ValidationException("login.error");
        }
        return user;
    }

    private Credentials getLoginCredentials() {
        Credentials credentials = getWebShopChannel().getCredentials();
        if (credentials == Credentials.DEFAULT) {
            credentials = Credentials.LOGIN_PASSWORD;
        }
        return credentials;
    }

    private Channel getWebShopChannel() {
        return channelService.loadByInternalName(Channel.WEBSHOP);
    }

    private String principalTypeLabel(final PrincipalType principalType) {
        String label;
        final Principal principal = principalType.getPrincipal();
        if (principal == Principal.CUSTOM_FIELD) {
            label = principalType.getCustomField().getName();
        } else {
            label = MessageHelper.message(getServlet().getServletContext(), principal.getKey());
        }
        return label;
    }

    private DoPaymentDTO resolvePayment(final HttpServletRequest request) {
        final DoPaymentDTO payment = WebShopHelper.getNewPayment(request.getSession());
        if (payment == null) {
            throw new ValidationException();
        }
        return payment;
    }

    private boolean shouldValidateTransactionPassword(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response, final DoPaymentDTO payment) {
        if (getWebShopChannel().getCredentials() != Credentials.DEFAULT) {
            // Only default credentials validate transaction password
            return false;
        }
        final Member member = getFetchService().fetch((Member) payment.getFrom(), Element.Relationships.USER);
        final ActionContext context = new ActionContext(mapping, actionForm, request, response, member.getUser(), getFetchService());
        final TransferType transferType = getFetchService().fetch(payment.getTransferType(), TransferType.Relationships.FROM);
        return context.isTransactionPasswordEnabled(transferType.getFrom());
    }
}

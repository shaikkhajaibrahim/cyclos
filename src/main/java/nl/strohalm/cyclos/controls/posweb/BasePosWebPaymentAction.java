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

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.payments.PaymentAction;
import nl.strohalm.cyclos.controls.posweb.PosWebHelper.Action;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidUserForChannelException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.DoPaymentDTO;
import nl.strohalm.cyclos.services.transactions.exceptions.AuthorizedPaymentInPastException;
import nl.strohalm.cyclos.services.transactions.exceptions.CreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Base class for posweb payments
 * @author luis
 */
public abstract class BasePosWebPaymentAction extends PaymentAction {

    protected CustomFieldService customFieldService;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    protected abstract Payment doPayment(final ActionContext context, final DoPaymentDTO dto);

    @Override
    protected void formAction(final ActionContext context) throws Exception {
        final HttpSession session = context.getSession();
        session.setAttribute("isExternalOperator", true);

        Payment payment;
        try {
            payment = doPayment(context, resolvePaymentDTO(context));
            session.removeAttribute("payment");
        } catch (final InvalidUserForChannelException e) {
            throw new ValidationException("posweb.error.channelDisabled");
        } catch (final NotEnoughCreditsException e) {
            throw new ValidationException("posweb.error.notEnoughCredits", ActionHelper.resolveParameters(e));
        } catch (final CreditsException e) {
            throw new ValidationException(ActionHelper.resolveErrorKey(e), ActionHelper.resolveParameters(e));
        } catch (final UnexpectedEntityException e) {
            throw new ValidationException("payment.error.invalidTransferType");
        } catch (final AuthorizedPaymentInPastException e) {
            throw new ValidationException("payment.error.authorizedInPast");
        }

        session.setAttribute("lastPayment", payment);
        session.setAttribute("lastPaymentIsScheduled", payment instanceof ScheduledPayment);
        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(payment.getType());
        final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, payment.getCustomValues());
        session.setAttribute("lastPaymentCustomValues", entries);
    }

    @Override
    protected MemberGroup getMemberGroup(final ActionContext context) {
        final Member member = (Member) context.getAccountOwner();
        return member.getMemberGroup();
    }

    /**
     * Returns the operation name for permission check
     */
    protected abstract String getOperationName();

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        if (context.isOperator() && !getPermissionService().checkPermission("operatorPayments", getOperationName())) {
            context.getSession().invalidate();
            throw new PermissionDeniedException();
        }

        final HttpServletRequest request = context.getRequest();

        // Resolve the possible currencies
        final List<Currency> currencies = resolveCurrencies(context);
        request.setAttribute("currencies", currencies);
        if (CollectionUtils.isEmpty(currencies)) {
            throw new ValidationException("payment.error.noTransferType");
        } else if (currencies.size() == 1) {
            request.setAttribute("singleCurrency", currencies.iterator().next());
        }

        request.setAttribute("loggedMember", context.getAccountOwner());

        // Show options when can both pay and receive
        boolean showOptions;
        if (context.isOperator()) {
            showOptions = getPermissionService().checkPermission("operatorPayments", "externalMakePayment") && getPermissionService().checkPermission("operatorPayments", "externalReceivePayment");
        } else {
            final Action action = PosWebHelper.getAction(request);
            showOptions = action == Action.BOTH;
        }
        request.setAttribute("showOptions", showOptions);
    }

    @Override
    protected ActionForward resolveLoginForward(final ActionMapping actionMapping, final HttpServletRequest request) {
        return new ActionForward(PosWebHelper.loginUrl(request), true);
    }

    @Override
    protected TransferTypeQuery resolveTransferTypeQuery(final ActionContext context) {
        return null;
    }
}
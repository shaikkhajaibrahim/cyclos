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
package nl.strohalm.cyclos.controls.payments;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomFieldValue;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.DoPaymentDTO;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.ScheduledPaymentDTO;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transactions.exceptions.AuthorizedPaymentInPastException;
import nl.strohalm.cyclos.services.transactions.exceptions.CreditsException;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewDTO;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeeService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to display a message and optionally request the transaction password in order to make a payment
 * @author luis
 */
public class ConfirmPaymentAction extends BaseFormAction {

    private PaymentService        paymentService;
    private TransferTypeService   transferTypeService;
    private TransactionFeeService transactionFeeService;
    private CustomFieldService    customFieldService;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Inject
    public void setTransactionFeeService(final TransactionFeeService transactionFeeService) {
        this.transactionFeeService = transactionFeeService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final ConfirmPaymentForm form = context.getForm();
        final DoPaymentDTO paymentDTO = validatePayment(context);
        // Validate the transaction password if needed
        if (shouldValidateTransactionPassword(context, paymentDTO)) {
            context.checkTransactionPassword(form.getTransactionPassword());
        }
        // Perform the actual payment
        Payment payment;
        try {
            payment = doPayment(context, paymentDTO);
            context.getSession().removeAttribute("payment");
        } catch (final CreditsException e) {
            return context.sendError(ActionHelper.resolveErrorKey(e), ActionHelper.resolveParameters(e));
        } catch (final UnexpectedEntityException e) {
            return context.sendError("payment.error.invalidTransferType");
        } catch (final AuthorizedPaymentInPastException e) {
            return context.sendError("payment.error.authorizedInPast");
        }
        // Redirect to the next action
        final Map<String, Object> params = new HashMap<String, Object>();
        ActionForward forward;
        if (payment instanceof Transfer) {
            params.put("transferId", payment.getId());
            forward = context.getSuccessForward();
        } else if (payment instanceof ScheduledPayment) {
            params.put("paymentId", payment.getId());
            forward = context.findForward("scheduledPayment");
        } else {
            throw new IllegalStateException("Unknown payment type: " + payment);
        }
        params.put("selectMember", form.getSelectMember());
        params.put("from", form.getFrom());
        return ActionHelper.redirectWithParams(context.getRequest(), forward, params);
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final DoPaymentDTO payment = validatePayment(context);

        // Clear the from when the same as logged owner
        if (payment.getFrom() != null && context.getAccountOwner().equals(payment.getFrom())) {
            payment.setFrom(null);
        }

        // Check for transaction password
        final HttpServletRequest request = context.getRequest();
        final boolean requestTransactionPassword = shouldValidateTransactionPassword(context, payment);
        if (requestTransactionPassword) {
            context.validateTransactionPassword();
        }

        final boolean wouldRequireAuthorization = paymentService.wouldRequireAuthorization(payment);
        request.setAttribute("requestTransactionPassword", requestTransactionPassword);
        request.setAttribute("wouldRequireAuthorization", wouldRequireAuthorization);

        if (wouldRequireAuthorization && payment.getDate() != null) {
            throw new ValidationException("payment.error.authorizedInPast");
        }

        // Fetch related data
        AccountOwner from = payment.getFrom();
        AccountOwner to = payment.getTo();
        final TransferType transferType = getFetchService().fetch(payment.getTransferType(), RelationshipHelper.nested(TransferType.Relationships.FROM, AccountType.Relationships.CURRENCY), TransferType.Relationships.TO);
        final BigDecimal amount = payment.getAmount();
        if (from instanceof Member) {
            from = getFetchService().fetch((Member) from);
            request.setAttribute("fromMember", from);
            payment.setFrom(from);
        }
        if (to instanceof Member) {
            to = getFetchService().fetch((Member) to);
            request.setAttribute("toMember", to);
            payment.setTo(to);
        }
        // request.setAttribute("relatedMember", from != null ? from.g : to);
        payment.setTransferType(transferType);
        request.setAttribute("unitsPattern", transferType.getFrom().getCurrency().getPattern());

        // Check if there are multiple transfer type choices - used to show or hide the transfer type
        final int transferTypes = countPossibleTransferTypes(context, payment);
        if (transferTypes == 0) {
            throw new ValidationException();
        }
        request.setAttribute("showTransferType", transferTypes > 1);

        // Store the transaction fees
        final TransactionFeePreviewDTO preview = transactionFeeService.preview(from, to, transferType, amount);
        request.setAttribute("finalAmount", preview.getFinalAmount());
        request.setAttribute("fees", preview.getFees());

        // Calculate the transaction fees for every scheduled payment
        final List<ScheduledPaymentDTO> payments = payment.getPayments();
        final boolean isScheduled = CollectionUtils.isNotEmpty(payments);
        if (isScheduled) {
            for (final ScheduledPaymentDTO current : payments) {
                final TransactionFeePreviewDTO currentPreview = transactionFeeService.preview(from, to, transferType, current.getAmount());
                current.setFinalAmount(currentPreview.getFinalAmount());
            }
        }
        request.setAttribute("isScheduled", isScheduled);

        // Return the custom field values
        final Collection<PaymentCustomFieldValue> customValues = payment.getCustomValues();
        if (customValues != null) {
            final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(transferType);
            final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, customValues);
            // Load the value for enumerated values, since this collection was built from direct databinding with ids only
            for (final Entry entry : entries) {
                final CustomField field = entry.getField();
                if (field.getType() == CustomField.Type.ENUMERATED) {
                    final CustomFieldValue fieldValue = entry.getValue();
                    Long possibleValueId;
                    final CustomFieldPossibleValue possibleValue = fieldValue.getPossibleValue();
                    if (possibleValue != null) {
                        possibleValueId = possibleValue.getId();
                    } else {
                        possibleValueId = CoercionHelper.coerce(Long.class, fieldValue.getValue());
                    }
                    if (possibleValueId != null) {
                        fieldValue.setPossibleValue(getFetchService().fetch(EntityHelper.reference(CustomFieldPossibleValue.class, possibleValueId)));
                    }
                }
            }
            request.setAttribute("customFields", entries);
        }
    }

    @Override
    protected void validateForm(final ActionContext context) {
        if (shouldValidateTransactionPassword(context, validatePayment(context))) {
            final ConfirmPaymentForm form = context.getForm();
            if (StringUtils.isEmpty(form.getTransactionPassword())) {
                throw new ValidationException("_transactionPassword", "login.transactionPassword", new RequiredError());
            }
        }
    }

    private int countPossibleTransferTypes(final ActionContext context, final DoPaymentDTO payment) {
        final TransferTypeQuery ttQuery = new TransferTypeQuery();
        ttQuery.setPageForCount();
        ttQuery.setCurrency(payment.getCurrency());
        ttQuery.setContext(payment.getContext());
        if (payment.getContext() == TransactionContext.PAYMENT) {
            ttQuery.setChannel(Channel.WEB);
        }
        ttQuery.setFromOwner(payment.getFrom() == null ? context.getAccountOwner() : payment.getFrom());
        ttQuery.setToOwner(payment.getTo());
        if (payment.getFrom() != null && !context.getAccountOwner().equals(payment.getFrom())) {
            ttQuery.setBy(context.getElement());
        }
        ttQuery.setUsePriority(true);
        final List<TransferType> tts = transferTypeService.search(ttQuery);
        return PageHelper.getTotalCount(tts);
    }

    /**
     * Process the payment itself
     */
    private Payment doPayment(final ActionContext context, final DoPaymentDTO dto) {
        Payment payment;
        // Check which method we have to call
        if (dto.getFrom() != null && !context.getAccountOwner().equals(dto.getFrom())) {
            // As another member
            if (dto.getTo() instanceof SystemAccountOwner) {
                payment = paymentService.doPaymentAsMemberToSystem(dto);
            } else if (dto.getFrom().equals(dto.getTo())) {
                payment = paymentService.doPaymentAsMemberToSelf(dto);
            } else {
                payment = paymentService.doPaymentAsMemberToMember(dto);
            }
        } else {
            dto.setFrom(context.getAccountOwner());
            if (context.getAccountOwner().equals(dto.getTo())) {
                // Self payment
                if (context.isAdmin()) {
                    // System to system
                    payment = paymentService.doPaymentFromSystemToSystem(dto);
                } else {
                    payment = paymentService.doPaymentFromMemberToSelf(dto);
                }
            } else {
                // Payment between distinct owners
                if (dto.getTo() instanceof SystemAccountOwner) {
                    // From member to system
                    payment = paymentService.doPaymentFromMemberToSystem(dto);
                } else if (context.isAdmin()) {
                    // From system to member
                    payment = paymentService.doPaymentFromSystemToMember(dto);
                } else {
                    // Between members
                    payment = paymentService.doPaymentFromMemberToMember(dto);
                }
            }
        }

        return payment;
    }

    private boolean shouldValidateTransactionPassword(final ActionContext context, final DoPaymentDTO payment) {
        if (payment.getFrom() == null) {
            // When a logged member performing payments from himself
            final TransferType transferType = getFetchService().fetch(payment.getTransferType(), TransferType.Relationships.FROM);
            return context.isTransactionPasswordEnabled(transferType.getFrom());
        } else {
            return context.isTransactionPasswordEnabled();
        }
    }

    private DoPaymentDTO validatePayment(final ActionContext context) {
        final DoPaymentDTO payment = (DoPaymentDTO) context.getSession().getAttribute("payment");
        if (payment == null) {
            throw new ValidationException();
        }
        return payment;
    }
}

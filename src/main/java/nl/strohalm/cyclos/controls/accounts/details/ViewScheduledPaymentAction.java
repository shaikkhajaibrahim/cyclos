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
package nl.strohalm.cyclos.controls.accounts.details;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.controls.payments.scheduled.ScheduledPaymentForm;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment.Status;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment.Action;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.ScheduledPaymentService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;

/**
 * Action used to view the details of a scheduled payment
 * @author Jefferson Magno
 */
public class ViewScheduledPaymentAction extends BaseFormAction {

    protected static final Relationship[] FETCH = { ScheduledPayment.Relationships.CUSTOM_VALUES, RelationshipHelper.nested(ScheduledPayment.Relationships.FROM, MemberAccount.Relationships.MEMBER), RelationshipHelper.nested(ScheduledPayment.Relationships.TO, MemberAccount.Relationships.MEMBER), RelationshipHelper.nested(ScheduledPayment.Relationships.TYPE, TransferType.Relationships.TO), ScheduledPayment.Relationships.BY, ScheduledPayment.Relationships.TRANSFERS };

    private CustomFieldService            customFieldService;
    protected ScheduledPaymentService     scheduledPaymentService;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setScheduledPaymentService(final ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    protected void checkTransactionPassword(final ActionContext context, final ScheduledPayment payment) {
        if (shouldValidateTransactionPassword(context, payment)) {
            String transactionPassword;
            final ActionForm form = context.getForm();
            if (form instanceof ViewTransactionForm) {
                transactionPassword = ((ViewTransactionForm) form).getTransactionPassword();
            } else {
                transactionPassword = ((ScheduledPaymentForm) form).getTransactionPassword();
            }
            context.checkTransactionPassword(transactionPassword);
        }
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();

        // Load the scheduled payment
        final ScheduledPayment payment = resolveScheduledPayment(context);

        // Check if the logged user can block, cancel, or pay next open payment
        final boolean canCancel = userCanCancel(context, payment);
        final boolean canBlock = userCanBlock(context, payment);
        final boolean canUnblock = userCanUnblock(context, payment);
        boolean canPayNow = false;
        if (payment.getTransfers().size() == 1) {
            canPayNow = ViewTransactionAction.canPayNow(context, payment.getTransfers().iterator().next());
        }
        final List<Boolean> payableTransfers = payableTransfers(context, payment);
        final String comments = null;

        // Resolve the by element
        boolean showBy = false;
        final Element by = payment.getBy();
        if (by != null) {
            if (by instanceof Administrator) {
                if (context.isAdmin()) {
                    request.setAttribute("byAdmin", by);
                } else {
                    // Don't disclose to member which admin made the payment
                    request.setAttribute("bySystem", true);
                }
                showBy = true;
            } else if ((by instanceof Operator) && (context.isMemberOf((Operator) by) || context.getElement().equals(by))) {
                request.setAttribute("byOperator", by);
                showBy = true;
            } else {
                final AccountOwner fromOwner = payment.getFrom().getOwner();
                final Member member = (Member) by.getAccountOwner();
                request.setAttribute("byMember", member);
                showBy = !member.equals(fromOwner);
            }
        }

        // Resolve the custom fields
        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(payment.getType());
        final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, payment.getCustomValues());
        request.setAttribute("customFields", entries);

        // Store the request attributes
        if (canCancel || canBlock || canUnblock) {
            request.setAttribute("requestTransactionPassword", shouldValidateTransactionPassword(context, payment));
        }
        if (canPayNow) {
            final ScheduledPaymentForm form = context.getForm();
            form.setShowActions(true);
        }
        request.setAttribute("canCancel", canCancel);
        request.setAttribute("canBlock", canBlock);
        request.setAttribute("canPayNow", canPayNow);
        request.setAttribute("payableTransfers", payableTransfers);
        request.setAttribute("canUnblock", canUnblock);
        request.setAttribute("comments", comments);
        request.setAttribute("showBy", showBy);
        request.setAttribute("payment", payment);
    }

    protected ScheduledPayment resolveScheduledPayment(final ActionContext context) {
        final ScheduledPaymentForm form = context.getForm();
        final long id = form.getPaymentId();
        if (id <= 0L) {
            throw new ValidationException();
        }
        return scheduledPaymentService.load(form.getPaymentId(), FETCH);
    }

    protected boolean shouldValidateTransactionPassword(final ActionContext context, final ScheduledPayment scheduledPayment) {
        if (context.getAccountOwner().equals(scheduledPayment.getFromOwner())) {
            // When the logged member is the payment performer
            return context.isTransactionPasswordEnabled(scheduledPayment.getType().getFrom());
        } else {
            return context.isTransactionPasswordEnabled();
        }
    }

    @Override
    protected void validateForm(final ActionContext context) {
        if (shouldValidateTransactionPassword(context, resolveScheduledPayment(context))) {
            final ScheduledPaymentForm form = context.getForm();
            if (StringUtils.isEmpty(form.getTransactionPassword())) {
                throw new ValidationException("_transactionPassword", "login.transactionPassword", new RequiredError());
            }
        }
    }

    private List<Boolean> payableTransfers(final ActionContext context, final ScheduledPayment scheduledPayment) {
        final Calendar today = DateHelper.truncate(Calendar.getInstance());
        final Transfer firstOpenTransfer = scheduledPayment.getFirstOpenTransfer();
        boolean hasDelayedTransfers = false;
        if (firstOpenTransfer != null && firstOpenTransfer.getDate().before(today)) {
            hasDelayedTransfers = true;
        }
        final List<Transfer> transfers = scheduledPayment.getTransfers();
        final List<Boolean> payableTransfers = new ArrayList<Boolean>(transfers.size());
        for (final Transfer transfer : transfers) {
            final Status status = transfer.getStatus();
            boolean canPay = false;
            if (status.canPayNow()) {
                if (hasDelayedTransfers) {
                    canPay = transfer.equals(firstOpenTransfer);
                } else {
                    canPay = true;
                }
            }
            payableTransfers.add(canPay && userCanExecuteAction(context, scheduledPayment, Action.PAY_CURRENT_TRANSFER));
        }
        return payableTransfers;
    }

    private boolean userCanBlock(final ActionContext context, final ScheduledPayment scheduledPayment) {
        final Status status = scheduledPayment.getStatus();
        if (status == Status.PROCESSED || status == Status.BLOCKED || status == Status.CANCELED || status == Status.DENIED) {
            return false;
        }
        final AccountOwner accountOwner = context.getAccountOwner();
        if (accountOwner instanceof Member) {
            final Member member = (Member) accountOwner;
            if (member.equals(scheduledPayment.getFromOwner())) {
                final boolean allowUserToBlock = scheduledPayment.getType().isAllowBlockScheduledPayments();
                if (!allowUserToBlock) {
                    return false;
                }
            }
        }
        return userCanExecuteAction(context, scheduledPayment, Action.BLOCK);
    }

    private boolean userCanCancel(final ActionContext context, final ScheduledPayment scheduledPayment) {
        final Status status = scheduledPayment.getStatus();
        if (status == Status.PROCESSED || status == Status.CANCELED || status == Status.DENIED) {
            return false;
        }
        final AccountOwner accountOwner = context.getAccountOwner();
        if (accountOwner instanceof Member) {
            final Member member = (Member) accountOwner;
            if (member.equals(scheduledPayment.getFromOwner())) {
                final boolean allowUserToCancel = scheduledPayment.getType().isAllowCancelScheduledPayments();
                if (!allowUserToCancel) {
                    return false;
                }
            }
        }
        return userCanExecuteAction(context, scheduledPayment, Action.CANCEL);
    }

    private boolean userCanExecuteAction(final ActionContext context, final ScheduledPayment scheduledPayment, final Action action) {
        final AccountOwner fromOwner = scheduledPayment.getFromOwner();
        boolean canExecute = false;
        String permission = null;
        switch (action) {
            case CANCEL:
                permission = "cancelScheduled";
                break;
            case BLOCK:
            case UNBLOCK:
                permission = "blockScheduled";
                break;
            case PAY_CURRENT_TRANSFER:
                if (scheduledPayment.getToOwner() instanceof SystemAccountOwner) {
                    permission = "paymentToSystem";
                } else {
                    permission = "paymentToMember";
                }
                break;
        }
        if (fromOwner.equals(context.getAccountOwner())) {
            if (context.isAdmin()) {
                canExecute = getPermissionService().checkPermission("systemPayments", permission);
            } else {
                canExecute = getPermissionService().checkPermission("memberPayments", permission);
            }
        } else {
            if (context.isAdmin()) {
                canExecute = getPermissionService().checkPermission("adminMemberPayments", permission + "AsMember");
            } else if (fromOwner instanceof Member) {
                final Member fromMember = (Member) fromOwner;
                canExecute = context.isBrokerOf(fromMember) && getPermissionService().checkPermission("brokerMemberPayments", permission + "AsMember");
            }
        }
        return canExecute;
    }

    private boolean userCanUnblock(final ActionContext context, final ScheduledPayment scheduledPayment) {
        final Status status = scheduledPayment.getStatus();
        if (status != Status.BLOCKED) {
            return false;
        }
        final Transfer firstOpenTransfer = scheduledPayment.getFirstOpenTransfer();
        final Calendar now = Calendar.getInstance();
        final Calendar date = firstOpenTransfer.getDate();
        if (now.after(date)) {
            return false;
        }
        return userCanExecuteAction(context, scheduledPayment, Action.UNBLOCK);
    }

}
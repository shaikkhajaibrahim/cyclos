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

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.services.accounts.rates.ARateService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Action used to view the details of a transaction
 * @author luis
 * @author jefferson
 */
public class ViewTransactionAction extends BaseFormAction {

    protected static final Relationship[] FETCH = { Transfer.Relationships.CUSTOM_VALUES, RelationshipHelper.nested(Transfer.Relationships.FROM, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP), RelationshipHelper.nested(Transfer.Relationships.FROM, Account.Relationships.TYPE), RelationshipHelper.nested(Transfer.Relationships.TO, MemberAccount.Relationships.MEMBER, Element.Relationships.GROUP), RelationshipHelper.nested(Transfer.Relationships.TO, Account.Relationships.TYPE), RelationshipHelper.nested(Transfer.Relationships.TYPE, TransferType.Relationships.TO), RelationshipHelper.nested(Transfer.Relationships.SCHEDULED_PAYMENT, ScheduledPayment.Relationships.TRANSFERS), Transfer.Relationships.BY, Transfer.Relationships.RECEIVER, Transfer.Relationships.NEXT_AUTHORIZATION_LEVEL, Transfer.Relationships.AUTHORIZATIONS, Transfer.Relationships.CHARGEBACK_OF, Transfer.Relationships.CHARGED_BACK_BY };

    /**
     * Checks whether the given transfer can be paid immediately
     */
    public static boolean canPayNow(final ActionContext context, final Transfer transfer) {
        // Check, when part of scheduled payment, if can pay now
        boolean canPayNow = false;
        final ScheduledPayment scheduledPayment = transfer.getScheduledPayment();
        if (scheduledPayment != null) {
            canPayNow = transfer.getStatus().canPayNow();
            // Check if there's an expired payment
            if (canPayNow) {
                final List<Transfer> transfers = scheduledPayment.getTransfers();
                final Calendar now = DateHelper.truncate(Calendar.getInstance());
                for (final Transfer current : transfers) {
                    // When there's an expired payment, only that one is payable
                    if (current.getStatus().canPayNow() && current.getDate().before(now)) {
                        canPayNow = transfer.equals(current);
                        break;
                    }
                }
            }
            final FetchService fetchService = context.getFetchService();
            if (canPayNow) {
                if (context.isAdmin()) {
                    AdminGroup group = fetchService.fetch((AdminGroup) context.getGroup());
                    if (transfer.isFromSystem()) {
                        group = fetchService.fetch((AdminGroup) context.getGroup(), Group.Relationships.TRANSFER_TYPES);
                        canPayNow = group.getTransferTypes().contains(transfer.getType());
                    } else {
                        final Member member = (Member) transfer.getFromOwner();
                        group = fetchService.fetch((AdminGroup) context.getGroup(), AdminGroup.Relationships.TRANSFER_TYPES_AS_MEMBER, AdminGroup.Relationships.MANAGES_GROUPS);
                        canPayNow = group.getManagesGroups().contains(member.getGroup()) && group.getTransferTypesAsMember().contains(transfer.getType());
                    }
                } else if (!transfer.isFromSystem()) {
                    final Member member = (Member) transfer.getFromOwner();
                    if (context.isBrokerOf(member)) {
                        final BrokerGroup brokerGroup = fetchService.fetch((BrokerGroup) context.getGroup(), BrokerGroup.Relationships.TRANSFER_TYPES_AS_MEMBER);
                        canPayNow = context.isBrokerOf(member) && brokerGroup.getTransferTypesAsMember().contains(transfer.getType());
                    } else if (!context.getAccountOwner().equals(member)) {
                        canPayNow = false;
                    } else {
                        final Group group = fetchService.fetch(context.getGroup(), Group.Relationships.TRANSFER_TYPES);
                        canPayNow = group.getTransferTypes().contains(transfer.getType());
                    }
                }
            }
        }
        return canPayNow;
    }

    protected PaymentService   paymentService;

    private CustomFieldService customFieldService;

    private ARateService       aRateService;

    @Inject
    public void setaRateService(final ARateService aRateService) {
        this.aRateService = aRateService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    protected void checkTransactionPassword(final ActionContext context, final Transfer transfer) {
        if (shouldValidateTransactionPassword(context, transfer)) {
            final ViewTransactionForm form = context.getForm();
            context.checkTransactionPassword(form.getTransactionPassword());
        }
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();

        final ViewTransactionForm form = context.getForm();

        // Load the transfer
        final Transfer transfer = resolveTransfer(context);

        final String unitsPattern = transfer.getType().getFrom().getCurrency().getPattern();
        request.setAttribute("unitsPattern", unitsPattern);

        // Check if the logged user can view the transfer (if not the checkView method throws a PermissionDeniedException)
        paymentService.checkView(transfer);

        // Resolve the custom fields
        final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(transfer.getType());
        final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, transfer.getCustomValues());
        request.setAttribute("customFields", entries);

        // Set the scheduled payment details
        final ScheduledPayment scheduledPayment = getFetchService().fetch(transfer.getScheduledPayment(), ScheduledPayment.Relationships.TRANSFERS);
        if (scheduledPayment != null) {
            final int number = scheduledPayment.getTransfers().indexOf(transfer) + 1;
            request.setAttribute("scheduledPayment", scheduledPayment);
            request.setAttribute("scheduledPaymentNumber", number);
            request.setAttribute("scheduledPaymentCount", scheduledPayment.getTransfers().size());
        }

        final Collection<TransferAuthorization> authorizations = getFetchService().fetch(transfer.getAuthorizations(), TransferAuthorization.Relationships.BY);

        // Check if the logged user can authorize or deny the transfer and if the user can see comments
        boolean canCancel = false;
        boolean canAuthorize = false;
        boolean suppressDeny = false;
        boolean canChargeback = false;
        String comments = null;

        if (transfer.isRoot() && transfer.getProcessDate() == null || !CollectionUtils.isEmpty(transfer.getAuthorizations())) {

            if (transfer.getStatus() == Transfer.Status.PENDING) {
                AuthorizationLevel currentAuthorizationLevel = transfer.getNextAuthorizationLevel();
                final AuthorizationLevel.Authorizer authorizer = currentAuthorizationLevel.getAuthorizer();

                // Check if can authorize / deny
                final AccountOwner fromOwner = transfer.getFromOwner();
                if (context.isAdmin() && (authorizer == AuthorizationLevel.Authorizer.ADMIN || authorizer == AuthorizationLevel.Authorizer.BROKER)) {
                    final AdminGroup adminGroup = (AdminGroup) context.getGroup();
                    currentAuthorizationLevel = getFetchService().fetch(currentAuthorizationLevel, AuthorizationLevel.Relationships.ADMIN_GROUPS);
                    if (currentAuthorizationLevel.getAdminGroups().contains(adminGroup)) {
                        if (transfer.isActuallyFromSystem()) {
                            canAuthorize = getPermissionService().checkPermission("systemPayments", "authorize");
                        } else {
                            canAuthorize = getPermissionService().checkPermission("adminMemberPayments", "authorize");
                        }
                    }
                } else if (context.isBroker() && authorizer == AuthorizationLevel.Authorizer.BROKER && !transfer.isFromSystem()) {
                    // Brokers can authorize or deny payments from their brokered members
                    final Member member = (Member) fromOwner;
                    if (context.isBrokerOf(member)) {
                        canAuthorize = getPermissionService().checkPermission("brokerMemberPayments", "authorize");
                    }
                } else if ((context.isMember() || context.isOperator()) && authorizer == AuthorizationLevel.Authorizer.RECEIVER && !transfer.isToSystem()) {
                    // An operator can authorize or deny payments to his member if he has permission
                    if (transfer.getToOwner().equals(context.getAccountOwner())) {
                        canAuthorize = (context.isMember() && getPermissionService().checkPermission("memberPayments", "authorize")) || (context.isOperator() && getPermissionService().checkPermission("operatorPayments", "authorize"));
                    }
                } else if ((context.isMember() || context.isOperator()) && authorizer == AuthorizationLevel.Authorizer.PAYER && !transfer.isFromSystem()) {
                    // An operator can authorize or deny payments to his member if he has permission
                    if (transfer.getFromOwner().equals(context.getAccountOwner())) {
                        canAuthorize = (context.isMember() && getPermissionService().checkPermission("memberPayments", "authorize")) || (context.isOperator() && getPermissionService().checkPermission("operatorPayments", "authorize"));
                    }
                    suppressDeny = true;
                }

                // Check if can cancel
                if (!canAuthorize && scheduledPayment == null) {
                    // Can never cancel and authorize at the same time - he can already deny
                    if (fromOwner.equals(context.getAccountOwner())) {
                        if (context.isAdmin()) {
                            canCancel = getPermissionService().checkPermission("systemPayments", "cancel");
                        } else {
                            canCancel = (context.isMember() && getPermissionService().checkPermission("memberPayments", "cancelAuthorized")) || (context.isOperator() && getPermissionService().checkPermission("operatorPayments", "cancelAuthorized"));
                        }
                    } else if (transfer.getToOwner().equals(context.getAccountOwner())) {
                        // canceling a pending transfer by the receiver is like a charge back / reverse
                        // because of that we're checking the chargeback permission
                        final Group group = getFetchService().fetch(context.getGroup(), Group.Relationships.CHARGEBACK_TRANSFER_TYPES);
                        if (group.getChargebackTransferTypes().contains(transfer.getType()) && paymentService.canChargeback(transfer, true)) {
                            canCancel = context.isMember() && getPermissionService().checkPermission("memberPayments", "chargeback");
                        }
                    } else {
                        if (context.isAdmin()) {
                            canCancel = getPermissionService().checkPermission("adminMemberPayments", "cancelAuthorizedAsMember");
                        } else if (fromOwner instanceof Member) {
                            final Member fromMember = (Member) fromOwner;
                            canCancel = context.isBrokerOf(fromMember) && getPermissionService().checkPermission("brokerMemberPayments", "cancelAuthorizedAsMember");
                        }
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(transfer.getAuthorizations())) {
                final TransferAuthorization lastAuthorization = new LinkedList<TransferAuthorization>(transfer.getAuthorizations()).getLast();
                if (lastAuthorization.isShowToMember() || (lastAuthorization.getLevel().getAuthorizer() == AuthorizationLevel.Authorizer.BROKER && context.getElement().equals(lastAuthorization.getBy()))) {
                    comments = lastAuthorization.getComments();
                }
            }

        }

        final boolean canPayNow = canPayNow(context, transfer);

        // Check if the payment may be charged back
        String paymentsModule;
        if (context.isMember()) {
            paymentsModule = "memberPayments";
        } else if (transfer.isFromSystem()) {
            paymentsModule = "systemPayments";
        } else {
            paymentsModule = "adminMemberPayments";
        }
        if (getPermissionService().checkPermission(paymentsModule, "chargeback")) {
            final Group group = getFetchService().fetch(context.getGroup(), Group.Relationships.CHARGEBACK_TRANSFER_TYPES);
            if (group.getChargebackTransferTypes().contains(transfer.getType()) && paymentService.canChargeback(transfer, false)) {
                if (context.isMember()) {
                    // Members can only chargeback received payments
                    canChargeback = context.getElement().equals(transfer.getToOwner());
                } else {
                    // Admins with permissions can chargeback
                    canChargeback = true;
                }
            }
        }
        request.setAttribute("canChargeback", canChargeback);

        // Load the parent, if any
        final Transfer parent = getFetchService().fetch(transfer.getParent(), FETCH);
        request.setAttribute("parent", parent);
        if (parent != null) {
            // Get the parent custom fields
            final List<PaymentCustomField> parentCustomFields = customFieldService.listPaymentFields(parent.getType());
            final Collection<Entry> parentEntries = CustomFieldHelper.buildEntries(parentCustomFields, parent.getCustomValues());
            request.setAttribute("parentCustomFields", parentEntries);
        }

        // List the children
        final TransferQuery query = new TransferQuery();
        query.setStatus(null);
        query.setParent(transfer);
        query.fetch(RelationshipHelper.nested(Transfer.Relationships.FROM, MemberAccount.Relationships.MEMBER), RelationshipHelper.nested(Transfer.Relationships.TO, MemberAccount.Relationships.MEMBER), Transfer.Relationships.TYPE);
        final List<Transfer> children = paymentService.search(query);
        if (!context.isAdmin()) {
            // When a member is viewing, remove all payments that don't belong to him
            final AccountOwner loggedOwner = context.getAccountOwner();
            for (final Iterator<Transfer> it = children.iterator(); it.hasNext();) {
                final Transfer current = it.next();
                final AccountOwner fromOwner = current.getFrom().getOwner();
                final AccountOwner toOwner = current.getTo().getOwner();
                if (!loggedOwner.equals(fromOwner) && !loggedOwner.equals(toOwner)) {
                    // Not the logged owner - removed if not a broker
                    boolean remove = true;
                    if (context.isBroker()) {
                        final Member fromMember = (Member) (fromOwner instanceof Member ? fromOwner : null);
                        final Member toMember = (Member) (toOwner instanceof Member ? toOwner : null);
                        // Remove only when the broker is not the broker of neither from or to
                        remove = !context.isBrokerOf(fromMember) && !context.isBrokerOf(toMember);
                    }
                    if (remove) {
                        it.remove();
                    }
                }
            }
        }
        request.setAttribute("children", children);

        // Resolve the by element
        Element by = transfer.getReceiver();
        if (by == null) {
            by = transfer.getBy();
        }
        boolean showBy = false;
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
                final AccountOwner fromOwner = transfer.getFrom().getOwner();
                final Member member = (Member) by.getAccountOwner();
                request.setAttribute("byMember", member);
                showBy = !member.equals(fromOwner);
            }
        }

        // Store the request attributes
        if (canCancel || canAuthorize) {
            boolean alreadyAuthorized = false;
            if (canAuthorize) {
                // Check if the logged user has already authorized this payment
                for (final TransferAuthorization authorization : authorizations) {
                    if (context.getElement().equals(authorization.getBy())) {
                        alreadyAuthorized = true;
                        break;
                    }
                }
            }
            // Only show authorization when the logged member hasn't authorized this payment already (on another level)
            if (!alreadyAuthorized) {
                request.setAttribute("canCancel", canCancel);
                request.setAttribute("canAuthorize", canAuthorize);
                request.setAttribute("suppressDeny", suppressDeny);
                if (!context.getAccountOwner().equals(transfer.getToOwner())) {
                    request.setAttribute("showCommentsCheckBox", canAuthorize);
                }
            }
            request.setAttribute("alreadyAuthorized", alreadyAuthorized);
        }
        // Check if transaction password will be requested
        if (canCancel || canAuthorize || canChargeback) {
            request.setAttribute("requestTransactionPassword", shouldValidateTransactionPassword(context, transfer));
            request.setAttribute("showActions", true);
        }
        // Show the authorization history for admins
        if (context.isAdmin()) {
            request.setAttribute("authorizations", authorizations);
        }
        request.setAttribute("canPayNow", canPayNow);
        request.setAttribute("comments", comments);
        request.setAttribute("showBy", showBy);
        request.setAttribute("transfer", transfer);
        final Calendar date = (transfer.getProcessDate() != null) ? transfer.getProcessDate() : transfer.getDate();
        request.setAttribute("aRate", aRateService.emissionDateToRate(transfer.getEmissionDate(), date));
        if (form.getMemberId() > 0L) {
            request.setAttribute("memberId", form.getMemberId());
        }
        if (form.getTypeId() > 0L) {
            request.setAttribute("typeId", form.getTypeId());
        }
    }

    /**
     * Resolve a Map containing parameters for the next request after a form submit
     */
    protected Map<String, Object> resolveForwardParams(final ActionContext context) {
        final ViewTransactionForm form = context.getForm();
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("transferId", form.getTransferId());
        params.put("memberId", form.getMemberId());
        params.put("typeId", form.getTypeId());
        return params;
    }

    protected Transfer resolveTransfer(final ActionContext context) {
        final ViewTransactionForm form = context.getForm();
        final long id = form.getTransferId();
        if (id <= 0L) {
            throw new ValidationException();
        }
        return paymentService.load(form.getTransferId(), FETCH);
    }

    protected boolean shouldValidateTransactionPassword(final ActionContext context, final Transfer transfer) {
        if (context.getAccountOwner().equals(transfer.getToOwner())) {
            // When the logged member is the payment receiver
            return context.isTransactionPasswordEnabled(transfer.getType().getTo());
        } else if (context.getAccountOwner().equals(transfer.getFromOwner())) {
            // When the logged member is the payment performer
            return context.isTransactionPasswordEnabled(transfer.getType().getFrom());
        } else {
            return context.isTransactionPasswordEnabled();
        }
    }

    @Override
    protected void validateForm(final ActionContext context) {
        if (shouldValidateTransactionPassword(context, resolveTransfer(context))) {
            final ViewTransactionForm form = context.getForm();
            if (StringUtils.isEmpty(form.getTransactionPassword())) {
                throw new ValidationException("_transactionPassword", "login.transactionPassword", new RequiredError());
            }
        }
    }

}

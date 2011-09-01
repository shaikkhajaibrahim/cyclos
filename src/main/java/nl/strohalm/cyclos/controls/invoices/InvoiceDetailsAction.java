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
package nl.strohalm.cyclos.controls.invoices;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.fields.PaymentCustomField;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper.Entry;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to show an invoice details
 * @author luis
 */
public class InvoiceDetailsAction extends BaseAction {

    private static final Relationship[] FETCH = { Invoice.Relationships.CUSTOM_VALUES, RelationshipHelper.nested(Invoice.Relationships.FROM_MEMBER, Element.Relationships.USER), RelationshipHelper.nested(Invoice.Relationships.TO_MEMBER, Element.Relationships.USER), RelationshipHelper.nested(Invoice.Relationships.SENT_BY, Element.Relationships.USER), RelationshipHelper.nested(Invoice.Relationships.PERFORMED_BY, Element.Relationships.USER), RelationshipHelper.nested(Invoice.Relationships.DESTINATION_ACCOUNT_TYPE, AccountType.Relationships.CURRENCY), Invoice.Relationships.TRANSFER, RelationshipHelper.nested(Invoice.Relationships.TRANSFER_TYPE, TransferType.Relationships.TO), Invoice.Relationships.PAYMENTS };
    private InvoiceService              invoiceService;
    private TransferTypeService         transferTypeService;
    private CustomFieldService          customFieldService;

    public InvoiceService getInvoiceService() {
        return invoiceService;
    }

    public TransferTypeService getTransferTypeService() {
        return transferTypeService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final InvoiceDetailsForm form = context.getForm();
        final long id = form.getInvoiceId();
        if (id <= 0) {
            throw new ValidationException();
        }

        long memberId;
        if (context.isBroker() || context.isAdmin()) {
            memberId = form.getMemberId();
        } else { // member or operator
            memberId = ((Member) context.getAccountOwner()).getId();
        }

        final Invoice invoice = invoiceService.load(id, FETCH);

        // Get the custom values
        final List<TransferType> possibleTransferTypes = invoiceService.getPossibleTransferTypes(invoice);
        if (possibleTransferTypes.size() == 1) {
            final List<PaymentCustomField> customFields = customFieldService.listPaymentFields(possibleTransferTypes.iterator().next());
            final Collection<Entry> entries = CustomFieldHelper.buildEntries(customFields, invoice.getCustomValues());
            request.setAttribute("customFields", entries);
        }

        final long fromMemberId = invoice.getFromMember() == null ? 0L : invoice.getFromMember().getId();
        final long toMemberId = invoice.getToMember() == null ? 0L : invoice.getToMember().getId();
        final boolean incoming = toMemberId == memberId;
        final boolean outgoing = fromMemberId == memberId;

        final TransferType transferType = invoice.getTransferType();
        AccountType accountType = invoice.getDestinationAccountType();
        if (accountType == null && transferType != null) {
            accountType = transferType.getTo();
        }

        final boolean toMe = context.getAccountOwner().equals(invoice.getTo());
        final boolean fromMe = context.getAccountOwner().equals(invoice.getFrom());

        // Only show the destination account type when not logged as the invoice to and the invoice sender has more than one accounts
        boolean showDestinationAccountType = false;
        if (!context.getElement().equals(invoice.getToMember()) && transferType == null && invoice.getDestinationAccountType() != null) {
            final Member member = getFetchService().fetch(invoice.getFromMember(), RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.ACCOUNT_SETTINGS));
            final int accounts = member.getMemberGroup().getAccountSettings().size();
            showDestinationAccountType = accounts > 1;
        }

        final boolean isOpen = invoice.isOpen();
        boolean canAccept = isOpen && (incoming || context.isAdmin()) && CollectionUtils.isNotEmpty(possibleTransferTypes);
        boolean canDeny = isOpen && (incoming || context.isAdmin()) && !invoice.isFromSystem();
        boolean canCancel = isOpen && (outgoing || context.isAdmin());

        // If a broker viewing the invoice, ensure he's the member's broker
        if (context.isBroker() && !toMe && !fromMe) {
            final boolean brokerOfTo = context.isBrokerOf(invoice.getToMember());
            canAccept = canAccept && brokerOfTo;
            canDeny = canDeny && brokerOfTo;
            canCancel = canCancel && context.isBrokerOf(invoice.getFromMember());
        }

        // The module to test
        String module = null;
        if (context.isAdmin()) {
            module = "adminMemberInvoices";
        } else if (context.isOperator()) {
            module = "operatorInvoices";
        } else if (context.isBroker() && !fromMe && !toMe) {
            module = "brokerInvoices";
        } else {
            module = "memberInvoices";
        }

        // Check permission for accept
        if (canAccept) {
            if (toMe) {
                // Check if a member or an operator can accept the invoice
                if (invoice.isFromSystem()) {
                    if (context.isOperator()) {
                        canAccept = getPermissionService().checkPermission(module, "manage");
                    } else {
                        // Members always can accept invoices from system
                        canAccept = true;
                    }
                } else {
                    // Invoice from member
                    if (context.isAdmin()) {
                        canAccept = getPermissionService().checkPermission(module, "accept");
                    } else {
                        if (context.isOperator()) {
                            canAccept = getPermissionService().checkPermission(module, "manage");
                        } else {
                            // Members always can accept invoices from other members
                            canAccept = true;
                        }
                    }
                }
            } else {
                // Check if the admin or broker can accept the invoice
                String operation;
                if (invoice.isFromSystem()) {
                    operation = "acceptAsMemberFromSystem";
                } else {
                    operation = "acceptAsMemberFromMember";
                }
                canAccept = getPermissionService().checkPermission(module, operation);
            }
        }

        // Check permission for deny
        if (canDeny) {
            if (toMe) {
                if (context.isOperator()) {
                    canDeny = getPermissionService().checkPermission(module, "manage");
                } else {
                    // Members always can deny invoices to them
                    canDeny = true;
                }
            } else {
                // Check if the admin or broker can deny as member
                canDeny = getPermissionService().checkPermission(module, "denyAsMember");
            }
        }

        // Check permission for cancel
        if (canCancel && (!fromMe || context.isOperator())) {
            if (fromMe) {
                if (context.isOperator()) {
                    canCancel = getPermissionService().checkPermission(module, "manage");
                } else {
                    // Members always can cancel invoices send by themselves
                    canCancel = true;
                }
            } else {
                // Check if the admin or broker can cancel the invoice
                canCancel = getPermissionService().checkPermission(module, "cancelAsMember");
            }
        }

        // Check system invoices
        if (context.isAdmin()) {
            final boolean canAdminAcceptSystemInvoices = getPermissionService().checkPermission(module, "accept");
            final boolean canAdminCancelSystemInvoices = getPermissionService().checkPermission(module, "cancel");
            final boolean canAdminDenySystemInvoices = getPermissionService().checkPermission(module, "deny");

            if (canAccept && invoice.isToSystem()) {
                // Check if the administrator has the permission to accept the invoice
                canAccept = canAdminAcceptSystemInvoices;
            }

            if (canCancel && invoice.isFromSystem()) {
                // Check if the administrator has the permission to cancel the invoice
                canCancel = canAdminCancelSystemInvoices;
            }

            if (canDeny && invoice.isToSystem()) {
                // Check if the administrator has the permission to deny the invoice
                canDeny = canAdminDenySystemInvoices;
            }
        }

        final boolean showSentBy = shouldShowSentBy(context, invoice);
        final boolean showPerformedBy = shouldShowPerformedBy(context, invoice);

        final Payment payment = invoice.getPayment();
        if (payment instanceof Transfer) {
            request.setAttribute("transferId", payment.getId());
        } else if (payment instanceof ScheduledPayment) {
            request.setAttribute("paymentId", payment.getId());
        }

        request.setAttribute("invoice", invoice);
        request.setAttribute("unitsPattern", accountType.getCurrency().getPattern());
        request.setAttribute("transferTypes", possibleTransferTypes);
        request.setAttribute("toMe", toMe);
        request.setAttribute("fromMe", fromMe);
        request.setAttribute("canAccept", canAccept);
        request.setAttribute("canDeny", canDeny);
        request.setAttribute("canCancel", canCancel);
        request.setAttribute("showDestinationAccountType", showDestinationAccountType);
        request.setAttribute("showPerformedBy", showPerformedBy);
        request.setAttribute("showSentBy", showSentBy);

        return context.getInputForward();
    }

    /**
     * Returns whether the 'performed by' should be shown to the user
     */
    private boolean shouldShowPerformedBy(final ActionContext context, final Invoice invoice) {
        boolean showPerformedBy = false;
        final HttpServletRequest request = context.getRequest();
        final Element performedBy = invoice.getPerformedBy();
        if (performedBy != null) {
            if (performedBy instanceof Administrator) {
                if (context.isAdmin()) {
                    request.setAttribute("performedByAdmin", performedBy);
                    showPerformedBy = true;
                } else {
                    // Don't disclose to member which admin performed the action
                    request.setAttribute("performedBySystem", true);
                    // Only show performed by if invoice was not performed by the normal owner
                    final boolean shouldBeFromSystem = invoice.getStatus() == Invoice.Status.CANCELLED;
                    showPerformedBy = shouldBeFromSystem && !invoice.isFromSystem();
                }
            } else {
                Member shouldHavePerformed = null;
                switch (invoice.getStatus()) {
                    case ACCEPTED:
                    case DENIED:
                        shouldHavePerformed = invoice.getToMember();
                        break;
                    case CANCELLED:
                        shouldHavePerformed = invoice.getFromMember();
                        break;
                }
                if (performedBy.equals(shouldHavePerformed)) {
                    showPerformedBy = false;
                } else if ((performedBy instanceof Operator) && (context.isMemberOf((Operator) performedBy) || context.isOperator())) {
                    request.setAttribute("performedByOperator", performedBy);
                    showPerformedBy = true;
                } else {
                    request.setAttribute("performedByMember", performedBy.getAccountOwner());
                    showPerformedBy = true;
                }
            }
        }
        return showPerformedBy;
    }

    /**
     * Returns whether the 'sent by' should be shown to the user
     */
    private boolean shouldShowSentBy(final ActionContext context, final Invoice invoice) {
        boolean showSentBy = false;
        final HttpServletRequest request = context.getRequest();
        final Element sentBy = invoice.getSentBy();
        if (sentBy != null) {
            if (sentBy instanceof Administrator) {
                if (context.isAdmin()) {
                    request.setAttribute("sentByAdmin", sentBy);
                    showSentBy = true;
                } else {
                    // Don't disclose to member which admin sent the invoice
                    request.setAttribute("sentBySystem", true);
                    showSentBy = !invoice.isFromSystem(); // Only show sent by if not a regular system -> member invoice,
                }
            } else if ((sentBy instanceof Operator) && (context.isMemberOf((Operator) sentBy) || context.isOperator())) {
                request.setAttribute("sentByOperator", sentBy);
                showSentBy = true;
            } else if (!invoice.getFromMember().equals(sentBy)) {
                final Member member = (Member) sentBy.getAccountOwner();
                request.setAttribute("sentByMember", member);
                showSentBy = !invoice.getFrom().equals(member);
            }
        }
        return showSentBy;
    }
}

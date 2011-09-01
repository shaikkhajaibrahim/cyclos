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
package nl.strohalm.cyclos.services.transactions;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceSummaryDTO;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.reports.InvoiceSummaryType;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for invoices (to member/broker and to system)
 * @author luis
 */
public interface InvoiceService extends Service {

    /**
     * Process expired invoices, generating a member alert for system to member invoices that expires today, according to the
     * AlertSettings.idleInvoiceExpiration
     * @return The number of expired invoices
     */
    @SystemAction
    public int processExpiredInvoices(Calendar time);

    /**
     * Accept the invoice from a member as another member
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "acceptAsMemberFromMember"))
    @BrokerAction(@Permission(module = "brokerInvoices", operation = "acceptAsMemberFromMember"))
    @PathToMember("toMember")
    Invoice acceptAsMemberFromMember(Invoice invoice) throws NotEnoughCreditsException, UpperCreditLimitReachedException, MaxAmountPerDayExceededException, PermissionDeniedException, UnexpectedEntityException;

    /**
     * Accept the invoice from system as another member
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "acceptAsMemberFromSystem"))
    @BrokerAction(@Permission(module = "brokerInvoices", operation = "acceptAsMemberFromSystem"))
    @PathToMember("fromMember")
    Invoice acceptAsMemberFromSystem(Invoice invoice) throws NotEnoughCreditsException, UpperCreditLimitReachedException, MaxAmountPerDayExceededException, PermissionDeniedException, UnexpectedEntityException;

    /**
     * Accept the invoice coming from a member to member
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorInvoices", operation = "manage"))
    @PathToMember("toMember")
    Invoice acceptFromMemberToMember(Invoice invoice) throws NotEnoughCreditsException, UpperCreditLimitReachedException, MaxAmountPerDayExceededException, PermissionDeniedException, UnexpectedEntityException;

    /**
     * Accept the invoice coming from a member to system
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "accept"))
    @PathToMember("fromMember")
    Invoice acceptFromMemberToSystem(Invoice invoice) throws NotEnoughCreditsException, UpperCreditLimitReachedException, MaxAmountPerDayExceededException, PermissionDeniedException, UnexpectedEntityException;

    /**
     * Accept the invoice coming from the system
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorInvoices", operation = "manage"))
    @PathToMember("toMember")
    Invoice acceptFromSystemToMember(Invoice invoice) throws NotEnoughCreditsException, UpperCreditLimitReachedException, MaxAmountPerDayExceededException, PermissionDeniedException, UnexpectedEntityException;

    /**
     * Cancels an invoice as a member
     * @throws UnexpectedEntityException When the invoice is invalid
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "cancelAsMember"))
    @BrokerAction(@Permission(module = "brokerInvoices", operation = "cancelAsMember"))
    @PathToMember("fromMember")
    Invoice cancelAsMember(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Cancels the specified member invoice
     * @throws UnexpectedEntityException When the invoice is invalid
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorInvoices", operation = "manage"))
    @PathToMember("fromMember")
    Invoice cancelMember(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Cancels the specified system invoice
     * @throws UnexpectedEntityException When the invoice is invalid
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "cancel"))
    @PathToMember("toMember")
    Invoice cancelSystem(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Denies the specified invoice as another member
     * @throws UnexpectedEntityException When the invoice is invalid
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "denyAsMember"))
    @BrokerAction(@Permission(module = "brokerInvoices", operation = "denyAsMember"))
    @PathToMember("toMember")
    Invoice denyAsMember(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Denies the specified invoice from member to member
     * @throws UnexpectedEntityException When the invoice is invalid
     */
    @MemberAction
    @OperatorAction(@Permission(module = "operatorInvoices", operation = "manage"))
    @PathToMember("toMember")
    Invoice denyByMember(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Denies the specified invoice from member to system
     * @throws UnexpectedEntityException When the invoice is invalid
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "deny"))
    @PathToMember("fromMember")
    Invoice denyBySystem(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Expire the given invoice. Called by the invoice expiration scheduled task
     */
    @SystemAction
    Invoice expireInvoice(Invoice invoice);

    /**
     * Returns the possible transfer types for the given invoice
     */
    List<TransferType> getPossibleTransferTypes(Invoice invoice);

    /**
     * Returns a summary for the invoices
     */
    @SystemAction
    TransactionSummaryVO getSummary(InvoiceSummaryDTO dto);

    /**
     * Returns a summary for the invoices of a given type
     */
    @AdminAction(@Permission(module = "systemReports", operation = "current"))
    TransactionSummaryVO getSummaryByType(Currency currency, InvoiceSummaryType invoiceSummaryType);

    /**
     * Loads an invoice
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    Invoice load(final Long id, final Relationship... fetch);

    /**
     * Loads an invoice using an associated payment
     * @throws EntityNotFoundException There's no invoice associated with the given payment
     */
    @SystemAction
    Invoice loadByPayment(Payment payment, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Searches for invoices
     */
    @DontEnforcePermission(traceable = true, value = "If the client of this method is outside the service layer it must carry out the permission control")
    List<Invoice> search(InvoiceQuery queryParameters);

    /**
     * Sends an invoice as member to member, returning the resulting object
     * @throws UnexpectedEntityException The transfer type is invalid for the specified context, or not visible to the member
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "sendAsMemberToMember"))
    @BrokerAction(@Permission(module = "brokerInvoices", operation = "sendAsMemberToMember"))
    @PathToMember("fromMember")
    Invoice sendAsMemberToMember(Invoice invoice) throws UnexpectedEntityException;

    /**
     * Sends an invoice as member to system, returning the resulting object
     * @throws UnexpectedEntityException The transfer type is invalid for the specified context, or not visible to the member
     * @throws PermissionDeniedException When logged in as administrator
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "sendAsMemberToSystem"))
    @BrokerAction(@Permission(module = "brokerInvoices", operation = "sendAsMemberToSystem"))
    @PathToMember("fromMember")
    Invoice sendAsMemberToSystem(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Sends an invoice, without testing the logged member. Should be called from internal procedures that send invoice (like account fees)
     * @throws UnexpectedEntityException
     */
    @SystemAction
    Invoice sendAutomatically(Invoice invoice) throws UnexpectedEntityException;

    /**
     * Sends an invoice from member to member, returning the resulting object
     * @throws UnexpectedEntityException The transfer type is invalid for the specified context, or not visible to the member
     */
    @MemberAction(@Permission(module = "memberInvoices", operation = "sendToMember"))
    @OperatorAction(@Permission(module = "operatorInvoices", operation = "sendToMember"))
    @PathToMember("fromMember")
    Invoice sendFromMemberToMember(Invoice invoice) throws UnexpectedEntityException;

    /**
     * Sends an invoice from member to system, returning the resulting object
     * @throws UnexpectedEntityException The transfer type is invalid for the specified context, or not visible to the member
     * @throws PermissionDeniedException When logged in as administrator
     */
    @MemberAction(@Permission(module = "memberInvoices", operation = "sendToSystem"))
    @OperatorAction(@Permission(module = "operatorInvoices", operation = "sendToSystem"))
    @PathToMember("fromMember")
    Invoice sendFromMemberToSystem(Invoice invoice) throws UnexpectedEntityException, PermissionDeniedException;

    /**
     * Sends an invoice from system to a member, returning the resulting object
     * @throws UnexpectedEntityException The transfer type is invalid for the specified context, or not visible to the member
     */
    @AdminAction(@Permission(module = "adminMemberInvoices", operation = "send"))
    @PathToMember("toMember")
    Invoice sendFromSystemToMember(Invoice invoice) throws UnexpectedEntityException;

    /**
     * Validates the invoice
     */
    @DontEnforcePermission(traceable = true)
    void validate(Invoice invoice);

    /**
     * Validates the invoice for accept
     */
    @DontEnforcePermission(traceable = true)
    void validateForAccept(Invoice invoice);

}
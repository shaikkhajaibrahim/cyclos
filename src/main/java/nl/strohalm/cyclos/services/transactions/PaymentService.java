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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransfer;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.accounts.rates.ConversionSimulationDTO;
import nl.strohalm.cyclos.services.stats.StatisticalResultDTO;
import nl.strohalm.cyclos.services.transactions.exceptions.AuthorizedPaymentInPastException;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeePreviewForRatesDTO;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for payments
 * @author luis
 */
public interface PaymentService extends Service {

    /**
     * Calculates the scheduled payment projection
     */
    public List<ScheduledPaymentDTO> calculatePaymentProjection(ProjectionDTO params);

    /**
     * Checks whether the given transfer may be charged back, but without taking in account the logged user. The criteria is:
     * <ul>
     * <li>The payment must be processed (have a non-null processDate)
     * <li>The maximum chargeback time on local settings must be respected
     * <li>The payment must be a root payment (cannot have a parent transfer)
     * <li>The payment must not have been already charged back
     * <li>The payment must not be a chargeback itself
     * @param ignorePendingPayment if true it won't check if the transfer was processed (is pending)
     */
    boolean canChargeback(Transfer transfer, boolean ignorePendingPayment);

    /**
     * Chargebacks a transfer, returning it's amount to the original account, and does this for all generated transfer tree
     * @return The chargeback payment
     * @throws UnexpectedEntityException When the payment cannot be charged back
     * @param clientId the web service client identifier
     */
    Transfer chargeback(Transfer transfer, Long clientId) throws UnexpectedEntityException;

    /**
     * Chargeback a transfer from a member account, returning it's amount to the original account, and does this for all generated transfer tree
     * @return The chargeback payment
     * @throws UnexpectedEntityException When the payment cannot be charged back
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "chargeback"))
    @PathToMember("from.member")
    Transfer chargebackMemberPayment(Transfer transfer) throws UnexpectedEntityException;

    /**
     * Chargeback a transfer to the logged member, returning it's amount to the original account, and does this for all generated transfer tree
     * @return The chargeback payment
     * @throws UnexpectedEntityException When the payment cannot be charged back
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "chargeback"))
    @PathToMember("to.member")
    Transfer chargebackReceivedPayment(Transfer transfer) throws UnexpectedEntityException;

    /**
     * Chargeback a payment for SMS messages when the message couldn't be send
     */
    void chargebackSmsCharge(Transfer transfer);

    /**
     * Chargeback a transfer from a system account, returning it's amount to the original account, and does this for all generated transfer tree
     * @return The chargeback payment
     * @throws UnexpectedEntityException When the payment cannot be charged back
     */
    @AdminAction(@Permission(module = "systemPayments", operation = "chargeback"))
    Transfer chargebackSystemPayment(Transfer transfer) throws UnexpectedEntityException;

    /**
     * Checks if the logged user can view this transfer.<br>
     * Because this method is used to show/hide controls, etc. it can't be annotated with the permissions annotations
     * @param transfer
     * @throws #{@link PermissionDeniedException} if the logged user can't view the transfer
     */
    void checkView(Transfer transfer) throws PermissionDeniedException;

    /**
     * Conciliates the given transfer with the given external transfer
     */
    Transfer conciliate(Transfer transfer, ExternalTransfer externalTransfer);

    /**
     * Confirms a payment which is pending for the given ticket
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws EntityNotFoundException Invalid ticket
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not the system
     */
    Payment confirmPayment(String ticket) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, EntityNotFoundException, UpperCreditLimitReachedException;

    /**
     * Performs an external payment from operator to member
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    @OperatorAction(@Permission(module = "operatorPayments", operation = "externalMakePayment"))
    @PathToMember("from")
    Transfer doExternalPaymentByOperator(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Performs a payment as a member to another member
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "paymentAsMemberToMember"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "paymentAsMemberToMember"))
    @PathToMember("from")
    Payment doPaymentAsMemberToMember(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * Performs a self payment as a member (so: between the accounts of one member)
     * @return The main transfer.
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "paymentAsMemberToSelf"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "paymentAsMemberToSelf"))
    @PathToMember("from")
    Payment doPaymentAsMemberToSelf(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * Performs a payment as a member to system
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "paymentAsMemberToSystem"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "paymentAsMemberToSystem"))
    @PathToMember("from")
    Payment doPaymentAsMemberToSystem(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * Performs a payment from member to member
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "paymentToMember"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "paymentToMember"))
    @PathToMember("from")
    Payment doPaymentFromMemberToMember(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Performs a self payment
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not the system
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "paymentToSelf"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "paymentToSelf"))
    @PathToMember("from")
    Transfer doPaymentFromMemberToSelf(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * Performs a payment from member to system
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not the system
     */
    @MemberAction(@Permission(module = "memberPayments", operation = "paymentToSystem"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "paymentToSystem"))
    @PathToMember("from")
    Payment doPaymentFromMemberToSystem(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Performs a payment from system to member
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "payment"))
    @PathToMember("to")
    Payment doPaymentFromSystemToMember(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * Performs a system to system payment
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not the system
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    @AdminAction(@Permission(module = "systemPayments", operation = "payment"))
    Transfer doPaymentFromSystemToSystem(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * Returns a ConversionSimulationDTO containing the default values for the given member's default account, and chooses the transferType from the
     * transferTypes param which belongs to the member and has rated fees. If no TransferType with rated fees available, it just uses the first
     * transferType
     */
    @BrokerAction(@Permission(module = "brokerAccounts", operation = "brokerSimulateConversion"))
    @AdminAction(@Permission(module = "adminMemberAccounts", operation = "simulateConversion"))
    @PathToMember("member")
    ConversionSimulationDTO getDefaultConversionDTOForMember(MemberAccount account, List<TransferType> transferTypes);

    /**
     * Returns a ConversionSimulationDTO containing the default values for the given member's default account, and chooses the transferType from the
     * transferTypes param which belongs to the member and has rated fees. If no TransferType with rated fees available, it just uses the first
     * transferType
     */
    @MemberAction(@Permission(module = "memberAccount", operation = "simulateConversion"))
    @OperatorAction(@Permission(module = "operatorAccount", operation = "simulateConversion"))
    @PathToMember("member")
    ConversionSimulationDTO getDefaultConversionDTOForSelf(MemberAccount account, List<TransferType> transferTypes);

    /**
     * Returns the minimum payment
     */
    BigDecimal getMinimumPayment();

    /**
     * creates data for a graph showing the decline of the fees over time on a conversion. Called from Simulate conversion.
     */
    StatisticalResultDTO getSimulateConversionGraph(ConversionSimulationDTO dto);

    /**
     * Inserts a payment. Intended to be called by external payments
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    Payment insertExternalPayment(DoExternalPaymentDTO dto) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Inserts a payment, notifying users. Intended to be called by automatically generated transfers, like loan repayments or fee charges.
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    Payment insertWithNotification(TransferDTO dto) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Inserts a payment, without notifying users. Intended to be called by automatically generated transfers, like loan repayments or fee charges.
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    Payment insertWithoutNotification(TransferDTO dto) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Returns details of a single transfer
     * @throws EntityNotFoundException No such id
     */
    Transfer load(Long id, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads a transfer by the trace number and the client id
     * @param traceNumber
     * @param clientId
     * @return the unique transfer or null
     */
    Transfer loadTransferByTraceNumber(String traceNumber, Long clientId);

    /**
     * Pay a scheduled payment component setting the specified date as the transfer's process date
     * @param processedDate the transfer's process date, if it's null it takes the current date.
     */
    Transfer pay(Transfer transfer, Calendar processedDate);

    /**
     * Process a scheduled payment component
     */
    Transfer processScheduled(Transfer transfer, Calendar time);

    /**
     * Searches for transfers
     */
    List<Transfer> search(TransferQuery query);

    /**
     * Simulates the conversion fees as broker or admin for a member
     */
    @BrokerAction(@Permission(module = "brokerAccounts", operation = "brokerSimulateConversion"))
    @AdminAction(@Permission(module = "adminMemberAccounts", operation = "simulateConversion"))
    @PathToMember("account.member")
    TransactionFeePreviewForRatesDTO simulateConversionForMember(ConversionSimulationDTO params);

    /**
     * Simulates the conversion fees for myAccount
     */
    @MemberAction(@Permission(module = "memberAccount", operation = "simulateConversion"))
    @OperatorAction(@Permission(module = "operatorAccount", operation = "simulateConversion"))
    @PathToMember("account.member")
    TransactionFeePreviewForRatesDTO simulateConversionForSelf(ConversionSimulationDTO params);

    /**
     * The no-permissions version, to be called from other services.
     */
    TransactionFeePreviewForRatesDTO simulateConversionInternal(ConversionSimulationDTO params);

    /**
     * Simulates a payment, without actually performing it. The result (and exception) are the same as doPayment*
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not the system
     * @throws AuthorizedPaymentInPastException When the payment would require authorization and is set to a past date
     */
    Payment simulatePayment(DoExternalPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException, AuthorizedPaymentInPastException;

    /**
     * validates the conversion simulation form
     */
    void validate(ConversionSimulationDTO dto);

    /**
     * Validates the specified payment
     */
    void validate(DoPaymentDTO payment);

    /**
     * Check if the logged user can perform the given payment, without actually doing it. If the method was completed successfully, the payment is
     * possible. An specific exception will be thrown otherwise.
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException The transfer type is invalid for the specified context, or not visible to the member
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     */
    TransferDTO verify(DoPaymentDTO params) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Checks whether the given payment would require authorization
     */
    boolean wouldRequireAuthorization(DoPaymentDTO dto);

    /**
     * Checks whether the given transfer would require authorization
     */
    boolean wouldRequireAuthorization(Transfer transfer);

}
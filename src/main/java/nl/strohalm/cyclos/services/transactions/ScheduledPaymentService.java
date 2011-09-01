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

import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPaymentQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.transactions.exceptions.MaxAmountPerDayExceededException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.services.transactions.exceptions.UpperCreditLimitReachedException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for scheduled payments
 * @author Jefferson Magno
 */
public interface ScheduledPaymentService extends Service {

    /**
     * Blocks a scheduled payment
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "blockScheduledAsMember"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "blockScheduledAsMember"))
    @MemberAction(@Permission(module = "memberPayments", operation = "blockScheduled"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "blockScheduled"))
    @PathToMember("from.member")
    ScheduledPayment block(ScheduledPayment scheduledPayment);

    /**
     * Cancels a scheduled payment
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "cancelScheduledAsMember"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "cancelScheduledAsMember"))
    @MemberAction(@Permission(module = "memberPayments", operation = "cancelScheduled"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "cancelScheduled"))
    @PathToMember("from.member")
    ScheduledPayment cancel(ScheduledPayment scheduledPayment);

    /**
     * It returns true if the member has pending scheduled payments related to accounts<br>
     * different than the specified accounts.
     */
    @AdminAction(@Permission(module = "adminMemberAccounts", operation = "scheduledInformation"))
    @PathToMember("")
    boolean hasUnrelatedPendingPayments(final Member member, final Collection<MemberAccountType> accountTypes);

    /**
     * Returns details of a scheduled payment
     */
    @AdminAction( { @Permission(module = "systemAccounts", operation = "scheduledInformation"), @Permission(module = "adminMemberAccounts", operation = "scheduledInformation") })
    @BrokerAction(@Permission(module = "brokerAccounts", operation = "scheduledInformation"))
    @MemberAction(@Permission(module = "memberAccount", operation = "scheduledInformation"))
    @OperatorAction(@Permission(module = "operatorAccount", operation = "scheduledInformation"))
    @IgnoreMember
    ScheduledPayment load(Long id, Relationship... fetch);

    /**
     * Process a scheduled payment transfer as a member to another member
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "paymentAsMemberToMember"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "paymentAsMemberToMember"))
    @PathToMember("from.member")
    Transfer processTransferAsMemberToMember(Transfer transfer);

    /**
     * Process a scheduled payment transfer as a member to Self
     * @return The main transfer.
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "paymentAsMemberToSelf"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "paymentAsMemberToSelf"))
    @PathToMember("from.member")
    Transfer processTransferAsMemberToSelf(Transfer transfer);

    /**
     * Process a scheduled payment transfer as a member to system
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not a member
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "paymentAsMemberToSystem"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "paymentAsMemberToSystem"))
    @PathToMember("from.member")
    Transfer processTransferAsMemberToSystem(Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Process a scheduled payment transfer from member to member
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
    @PathToMember("from.member")
    Transfer processTransferFromMemberToMember(Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Process a scheduled payment from member to system
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
    @PathToMember("from.member")
    Transfer processTransferFromMemberToSystem(Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Process a scheduled payment from system to member
     * @return The main transfer. It may contain children transfers (generated, for example, by fees)
     * @throws NotEnoughCreditsException The account does not have enough credits
     * @throws MaxAmountPerDayExceededException The account has exceeded the maximum transaction amount per day for the specified transfer type
     * @throws UnexpectedEntityException Either transfer type or payment receiver are invalid
     * @throws UpperCreditLimitReachedException The payment cannot be performed because it would make the receiving account go beyond the upper credit
     * limit
     * @throws InvalidPaymentReceiverException When the given <code>DoPaymentDTO</code> is not the system
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "payment"))
    @PathToMember("to.member")
    Transfer processTransferFromSystemToMember(Transfer transfer) throws NotEnoughCreditsException, MaxAmountPerDayExceededException, UnexpectedEntityException, UpperCreditLimitReachedException;

    /**
     * Process payments that should have been processed, but for some reason they were not (i.e: server was down when the scheduled task should have
     * run)
     */
    @SystemAction
    int recoverScheduledPayments();

    /**
     * Searches for scheduled payments
     */
    @AdminAction(@Permission(module = "adminMemberAccounts", operation = "scheduledInformation"))
    @BrokerAction(@Permission(module = "brokerAccounts", operation = "scheduledInformation"))
    @MemberAction(@Permission(module = "memberAccount", operation = "scheduledInformation"))
    @OperatorAction(@Permission(module = "operatorAccount", operation = "scheduledInformation"))
    @PathToMember("owner")
    List<ScheduledPayment> search(ScheduledPaymentQuery query);

    @AdminAction(@Permission(module = "systemAccounts", operation = "scheduledInformation"))
    @IgnoreMember
    List<ScheduledPayment> searchSystem(ScheduledPaymentQuery query);

    /**
     * Unblocks a scheduled payment
     */
    @AdminAction(@Permission(module = "adminMemberPayments", operation = "blockScheduledAsMember"))
    @BrokerAction(@Permission(module = "brokerMemberPayments", operation = "blockScheduledAsMember"))
    @MemberAction(@Permission(module = "memberPayments", operation = "blockScheduled"))
    @OperatorAction(@Permission(module = "operatorPayments", operation = "blockScheduled"))
    @PathToMember("from.member")
    ScheduledPayment unblock(ScheduledPayment scheduledPayment);

    /**
     * Updates the scheduled payment status based on its first open transfer status
     */
    @SystemAction
    ScheduledPayment updateScheduledPaymentStatus(final ScheduledPayment scheduledPayment);

}
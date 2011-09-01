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
package nl.strohalm.cyclos.services.accountfees;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeCharge;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLogQuery;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeQuery;
import nl.strohalm.cyclos.entities.accounts.fees.account.MemberAccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for account fees.
 * @author luis
 */
public interface AccountFeeService extends Service {

    /**
     * Returns a new account status (or the same when nothing changed) with the updated amount for account fee charges to the given date
     */
    MemberAccountStatus adjustVolumeChargesToDate(MemberAccountStatus memberStatus, Calendar date);

    /**
     * Calculates the fee amount over the transactioned volume for the given member on the given account fee execution. When either the account status
     * feature or the account fee were enabled in the mid of the period, the AccountFeeCharges are not availiable. So, on those cases, calculate the
     * volume by iterating on transactions. Otherwise, just sum the AccountFeeCharges
     */
    BigDecimal calculateChargeOverTransactionedVolume(AccountFeeLog feeLog, Member member);

    /**
     * Calculates all volume account fee charges for the given account status at the given date
     */
    Collection<AccountFeeCharge> calculateVolumeCharges(MemberAccountStatus lastStatus, Calendar date);

    /**
     * Cancels a running account fee
     */
    @AdminAction(@Permission(module = "systemAccountFees", operation = "charge"))
    void cancel(AccountFeeLog log);

    /**
     * Charges the specified manual account fee
     */
    @AdminAction(@Permission(module = "systemAccountFees", operation = "charge"))
    ChargeAccountFeeThread chargeManual(AccountFee accountFee);

    /**
     * Charges the specified account fee. It must be scheduled, since this method is intented to be called periodically.
     * @throws UnexpectedEntityException When the fee is invalid or manually charged
     */
    ChargeAccountFeeThread chargeScheduled(AccountFee accountFee) throws UnexpectedEntityException;

    /**
     * Charges scheduled fees that should run on the given time, returning the number of fees that were charged
     */
    int chargeScheduledFees(Calendar time);

    /**
     * Finishes the charge thread
     */
    @AdminAction(@Permission(module = "systemAccountFees", operation = "charge"))
    void finishChargeFee(AccountFeeLog log);

    /**
     * Returns the last account fee log
     */
    AccountFeeLog getLastLog(AccountFee acctFee);

    /**
     * Returns the account fee log for the next execution
     */
    AccountFeeLog getNextExecution(AccountFee fee);

    /**
     * Inserts AccountFeeCharges for missing account fee periods for the given account, at a given date
     */
    Collection<AccountFeeCharge> insertMissingChargesForPreviousPeriods(MemberAccountStatus memberStatus, Calendar date);

    /**
     * Loads the account fee, fetching the specified relationships
     */
    AccountFee load(Long id, Relationship... fetch);

    /**
     * Loads the account fee log, fetching the specified relationships
     */
    AccountFeeLog loadLog(Long id, Relationship... fetch);

    /**
     * Returns the next batch of members which should be charged by the given account fee log
     */
    List<Member> nextMembersToCharge(AccountFeeLog feeLog);

    /**
     * Recharge a failed account fee.
     * @throws UnexpectedEntityException When the fee log is invalid or not failed
     */
    @AdminAction(@Permission(module = "systemAccountFees", operation = "charge"))
    ChargeAccountFeeThread rechargeFailed(AccountFeeLog log) throws UnexpectedEntityException;

    /**
     * Removes the specified account fees, unless any of them has been already charged
     * @return The number of removed account fees
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    int remove(Long... ids);

    /**
     * This method is invoked upon system initialization, and should check:
     * <ul>
     * <li>If there are logs with status == RUNNING, marking them as FAILED</li>
     * <li>For each scheduled task, insert missing logs: If the system was down when a scheduled fee should have run, a log must be created with
     * status == NEVER_RAN, at the date the fee should have run. That's the only way an administrator can recharge it later.</li>
     * </ul>
     */
    void resolveFailures();

    /**
     * Save the specified account fee
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    AccountFee save(AccountFee accountFee);

    /**
     * Save the specified account fee log
     */
    @SystemAction
    AccountFeeLog save(AccountFeeLog accountFee);

    /**
     * Search account fees
     */
    List<AccountFee> search(AccountFeeQuery query);

    /**
     * Search account fee logs
     */
    List<AccountFeeLog> searchLogs(AccountFeeLogQuery query);

    /**
     * Sets the result for the charging of the given member, for the given account fee log: a transfer or an invoice (both could be null)
     */
    MemberAccountFeeLog setChargingResult(AccountFeeLog feeLog, Member member, Transfer transfer, Invoice invoice);

    /**
     * Apply the given delay to the charging thread
     * @param delay The delay in milliseconds
     */
    void setThreadDelay(long delay);

    /**
     * Validate the specified account fee. There are some dependent validations:
     * <ul>
     * <li>When paymentDirection == TO_MEMBER then runMode must beMANUAL, chargeMode cannot be VOLUME_PERCENTAGE or NEGATIVE_VOLUME_PERCENTAGE</li>
     * <li>When paymentDirection == TO_SYSTEM then invoiceMode is required</li>
     * <li>When runMode == SCHEDULED then recurrence, day and hour are all required</li>
     * </ul>
     */
    @DontEnforcePermission(traceable = true)
    void validate(AccountFee accountFee);
}

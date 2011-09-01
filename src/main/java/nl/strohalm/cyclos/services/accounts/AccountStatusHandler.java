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
package nl.strohalm.cyclos.services.accounts;

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.PendingAccountStatus;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.utils.CurrentTransactionData;

/**
 * Handles the creation and mantainence of account status
 * 
 * @author luis
 */
public interface AccountStatusHandler {

    /**
     * Returns the account status for the given account and date. When the date is not specified, use the current date
     * @param processCurrentStatus When set to true, instead of returning the persistent status, calculate another one based on the real status, but
     * calculating the account fee charges to date
     * @param processAccountFeePeriod When set to true, perform an additional check to determine whether the period for account fee charges has
     * changed. When it has changed, insert a new AccountStatus and AccountFeeCharge for the period that ended
     */
    AccountStatus getStatus(Account account, Calendar date, boolean processCurrentStatus, boolean processAccountFeePeriod);

    /**
     * Process any pending account status
     */
    void initialize();

    /**
     * Process a liberation for the given installment which had previously reserved amount (for example, when the installment failed or was cancelled)
     */
    PendingAccountStatus liberateReservedAmountForInstallment(Transfer transfer);

    /**
     * Creates a PendingAccountStatus for an account fee disabled, which should affect the given account
     */
    PendingAccountStatus processAccountFeeDisabled(MemberAccount account, BigDecimal amount);

    /**
     * Process a transfer authorization
     */
    PendingAccountStatus processAuthorization(Account from, Transfer transfer, TransferAuthorization authorization);

    /**
     * Creates a PendingAccountStatus for a credit limit change
     */
    PendingAccountStatus processLimitChange(Account account);

    /**
     * Notifies that a number of pending account statuses may be processed
     */
    void processNext(int pendingAccountStatuses);

    /**
     * Processes the next pending status, according to the {@link CurrentTransactionData}
     */
    void processFromCurrentTransaction();

    /**
     * Process a scheduled payment which reserves the total amount
     */
    PendingAccountStatus processReservedScheduledPayment(ScheduledPayment scheduledPayment);

    /**
     * Process a transfer creation
     */
    PendingAccountStatus processTransfer(Transfer transfer);

    /**
     * Makes the current thread wait until all pending statuses are processed
     */
    void waitUntilProcessAll();
}

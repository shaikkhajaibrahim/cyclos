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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.strohalm.cyclos.dao.accounts.AccountDAO;
import nl.strohalm.cyclos.dao.accounts.AccountStatusDAO;
import nl.strohalm.cyclos.dao.accounts.PendingAccountStatusDAO;
import nl.strohalm.cyclos.dao.accounts.fee.account.AccountFeeChargeDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.entities.accounts.PendingAccountStatus;
import nl.strohalm.cyclos.entities.accounts.SystemAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountStatus;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeCharge;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.ChargeMode;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferAuthorization;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment.Status;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.accountfees.AccountFeeService;
import nl.strohalm.cyclos.services.accounts.rates.ARateService;
import nl.strohalm.cyclos.services.accounts.rates.DRateService;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.DataIteratorHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.CurrentTransactionData.Entry;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.conversion.CalendarConverter;
import nl.strohalm.cyclos.utils.conversion.UnitsConverter;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Handles the updating of account status
 * @author luis
 * @author Rinke (all dealing with rates)
 */
public class AccountStatusHandlerImpl implements AccountStatusHandler, Runnable, InitializingBean, DisposableBean {

    private static enum RunStatus {
        SUCCESS, ERROR, RETRY, EMPTY
    }

    private static final float      PRECISION_DELTA      = 0.0001F;

    // set MAX_PROCESSING_BATCH to 1 to avoid potential deadlocks.
    private static final int        MAX_PROCESSING_BATCH = 1;
    private static final int        MAX_ERROR_ATTEMPTS   = 6;
    private BlockingQueue<Integer>  queue                = new LinkedBlockingQueue<Integer>();
    private PendingAccountStatusDAO pendingAccountStatusDao;
    private TransactionTemplate     transactionTemplate;
    private Thread                  thread;
    private AccountStatusDAO        accountStatusDao;
    private AccountFeeChargeDAO     accountFeeChargeDao;
    private AccountFeeService       accountFeeService;
    private SettingsService         settingsService;
    private AlertService            alertService;
    private FetchService            fetchService;
    private DRateService            dRateService;
    private ARateService            aRateService;
    private boolean                 inProcess;
    private int                     errors;
    private AccountDAO              accountDao;

    public void afterPropertiesSet() throws Exception {
        thread = new Thread(this, "AccountStatusHandler");
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void destroy() throws Exception {
        stopThread();
    }

    public AccountStatus getStatus(Account account, final Calendar date, final boolean processCurrentStatus, final boolean processAccountFeePeriod) {
        AccountStatus status;
        lock(false, account);
        try {
            status = accountStatusDao.getByDate(account, date);
        } catch (final EntityNotFoundException e) {
            // When no account status is found, create a new one with values = zero
            account = fetchService.fetch(account);
            if (account instanceof MemberAccount) {
                status = new MemberAccountStatus((MemberAccount) account);
            } else {
                status = new SystemAccountStatus((SystemAccount) account);
            }
        }

        if (status instanceof MemberAccountStatus) {
            MemberAccountStatus memberStatus = (MemberAccountStatus) status;

            // When the account fee period should be processed, ensure a new AccountStatus is created on the period end
            if (processAccountFeePeriod) {
                final Collection<AccountFeeCharge> charges = accountFeeService.insertMissingChargesForPreviousPeriods(memberStatus, date);
                if (CollectionUtils.isNotEmpty(charges)) {
                    for (final AccountFeeCharge charge : charges) {
                        final MemberAccountStatus newStatus = memberStatus.newBasedOnThis();
                        newStatus.setDate(charge.getPeriod().getEnd());
                        newStatus.setAccountFeeLog(charge.getAccountFeeLog());
                        newStatus.setVolumeAccountFees(newStatus.getVolumeAccountFees().add(charge.getAmount()));
                        // Insert the status, updating the memberStatus instance
                        memberStatus = accountStatusDao.insert(newStatus);
                    }
                }
            }

            // For member accounts, get the updated reserved amount for volume account fees
            if (processCurrentStatus) {
                status = accountFeeService.adjustVolumeChargesToDate(memberStatus, date);
            }
        }

        // If the current status should be processed, also take into account the delta by pending account statuses
        if (processCurrentStatus) {
            final Iterator<PendingAccountStatus> iterator = pendingAccountStatusDao.iterateFor(account);
            try {
                if (iterator.hasNext()) {
                    while (iterator.hasNext()) {
                        final PendingAccountStatus pendingStatus = iterator.next();
                        switch (pendingStatus.getType()) {
                            case PAYMENT:
                                final Transfer transfer = fetchService.fetch(pendingStatus.getTransfer(), Transfer.Relationships.FROM, Transfer.Relationships.TO);
                                if (shouldUpdateToAccountOnPayment(pendingStatus, transfer) && transfer.getTo().equals(account)) {
                                    status = updateAccountStatusOnPayment(pendingStatus, true, transfer, status);
                                }
                                if (shouldUpdateFromAccountOnPayment(pendingStatus, transfer) && transfer.getFrom().equals(account)) {
                                    status = updateAccountStatusOnPayment(pendingStatus, false, transfer, status);
                                }
                                break;
                            case RESERVED_SCHEDULED_PAYMENT:
                                status = updateAccountStatusOnReservedScheduledPayment(pendingStatus, status, false);
                                break;
                            case LIBERATE_RESERVED_INSTALLMENT:
                                status = updateAccountStatusOnLiberateReservedInstallment(pendingStatus, status, false);
                                break;
                            case ACCOUNT_FEE_DISABLED:
                                status = updateAccountStatusAccountFeeDisabled(pendingStatus, (MemberAccountStatus) status, false);
                                break;
                            case ACCOUNT_FEE_INVOICE:
                                status = updateAccountStatusOnAccountFeeInvoice(pendingStatus, (MemberAccountStatus) status, false);
                                break;
                            case AUTHORIZATION:
                                status = updateAccountStatusOnAuthorization(pendingStatus, status, false);
                                break;
                            case LIMIT_CHANGE:
                                status = updateAccountStatusOnCreditLimitChange(pendingStatus, status, false);
                                break;
                            default:
                                System.out.println("Invalid pending account status type: " + pendingStatus.getType());
                                break;
                        }
                    }
                }
            } finally {
                DataIteratorHelper.close(iterator);
            }
        }
        return status;
    }

    public void initialize() {
        final int count = pendingAccountStatusDao.count();
        processNext(count);
    }

    public PendingAccountStatus liberateReservedAmountForInstallment(final Transfer transfer) {
        ensureAlive();
        final ScheduledPayment scheduledPayment = transfer.getScheduledPayment();
        if (scheduledPayment == null || !scheduledPayment.isReserveAmount()) {
            return null;
        }
        // Insert the pending Status
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.LIBERATE_RESERVED_INSTALLMENT);
        pendingStatus.setDate(scheduledPayment.getDate());
        pendingStatus.setScheduledPayment(scheduledPayment);
        pendingStatus.setAccount(scheduledPayment.getFrom());
        pendingStatus.setTransfer(transfer);
        pendingStatus.setTransferStatus(transfer.getStatus());
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    /**
     * Creates a PendingAccountStatus for an account fee disabled, which should affect the given account
     */
    public PendingAccountStatus processAccountFeeDisabled(final MemberAccount account, final BigDecimal subtractedAmount) {
        ensureAlive();
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.ACCOUNT_FEE_DISABLED);
        pendingStatus.setDate(Calendar.getInstance());
        pendingStatus.setAccount(account);
        pendingStatus.setSubtractedAmount(subtractedAmount);
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    /**
     * Creates a PendingAccountStatus for an account fee disabled, which should affect the given account
     */
    public PendingAccountStatus processAccountFeeInvoice(final Account account, final Invoice invoice) {
        ensureAlive();
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.ACCOUNT_FEE_INVOICE);
        pendingStatus.setDate(Calendar.getInstance());
        pendingStatus.setAccount(account);
        pendingStatus.setInvoice(invoice);
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    /**
     * Creates a PendingAccountStatus for a transfer authorization <br>
     * <b>IMPORTANT</b>: Make sure that this is first called on the to account, then on the from account. Otherwise rate calculations work with the
     * wrong balance. See remark on updateAccounStatusonPayment.
     * 
     */
    public PendingAccountStatus processAuthorization(final Account account, final Transfer transfer, final TransferAuthorization transferAuthorization) {
        ensureAlive();
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.AUTHORIZATION);
        pendingStatus.setDate(Calendar.getInstance());
        pendingStatus.setAccount(account);
        pendingStatus.setTransfer(transfer);
        pendingStatus.setTransferStatus(transfer.getStatus());
        pendingStatus.setTransferAuthorization(transferAuthorization);
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    public void processFromCurrentTransaction() {
        final Entry entry = CurrentTransactionData.getEntry();
        final int pendingAccountStatuses = entry == null ? 0 : entry.getPendingAccountStatuses();
        if (pendingAccountStatuses > 0) {
            processNext(pendingAccountStatuses);
        }
    }

    public PendingAccountStatus processLimitChange(final Account account) {
        ensureAlive();
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.LIMIT_CHANGE);
        pendingStatus.setDate(Calendar.getInstance());
        pendingStatus.setAccount(account);
        pendingStatus.setLowerLimit(account.getCreditLimit());
        pendingStatus.setUpperLimit(account.getUpperCreditLimit());
        if (LoggedUser.isValid()) {
            pendingStatus.setBy(LoggedUser.element());
        }
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    public void processNext(final int count) {
        // Ensure a max count is offered each time
        int current = count;
        while (current > 0) {
            queue.offer(Math.min(MAX_PROCESSING_BATCH, current));
            current -= MAX_PROCESSING_BATCH;
        }
    }

    public PendingAccountStatus processReservedScheduledPayment(final ScheduledPayment scheduledPayment) {
        ensureAlive();
        if (!scheduledPayment.isReserveAmount()) {
            throw new IllegalArgumentException("Scheduled payment is not set to reserve the amount");
        }
        // Insert the pending Status
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.RESERVED_SCHEDULED_PAYMENT);
        pendingStatus.setDate(scheduledPayment.getDate());
        pendingStatus.setScheduledPayment(scheduledPayment);
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    public PendingAccountStatus processTransfer(final Transfer transfer) {
        ensureAlive();
        // Insert the pending Status
        PendingAccountStatus pendingStatus = new PendingAccountStatus();
        pendingStatus.setType(PendingAccountStatus.Type.PAYMENT);
        pendingStatus.setDate(transfer.getDate());
        pendingStatus.setTransfer(transfer);
        pendingStatus.setTransferStatus(transfer.getStatus());
        pendingStatus = pendingAccountStatusDao.insert(pendingStatus);
        CurrentTransactionData.addPendingAccountStatus();
        return pendingStatus;
    }

    public void run() {
        try {
            while (true) {
                // Wait until something is available
                final Integer count = queue.take();
                if (count != null && count > 0) {
                    processQueue(count);
                }
            }
        } catch (final InterruptedException e) {
            // Ignore
        }
    }

    public void setAccountDao(final AccountDAO accountDao) {
        this.accountDao = accountDao;
    }

    public void setAccountFeeChargeDao(final AccountFeeChargeDAO accountFeeChargeDao) {
        this.accountFeeChargeDao = accountFeeChargeDao;
    }

    public void setAccountFeeService(final AccountFeeService accountFeeService) {
        this.accountFeeService = accountFeeService;
    }

    public void setAccountStatusDao(final AccountStatusDAO accountStatusDao) {
        this.accountStatusDao = accountStatusDao;
    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setaRateService(final ARateService aRateService) {
        this.aRateService = aRateService;
    }

    public void setdRateService(final DRateService dRateService) {
        this.dRateService = dRateService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setPendingAccountStatusDao(final PendingAccountStatusDAO pendingAccountStatusDao) {
        this.pendingAccountStatusDao = pendingAccountStatusDao;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void waitUntilProcessAll() {
        while (!queue.isEmpty() || inProcess) {
            try {
                Thread.sleep(500);
            } catch (final InterruptedException e) {
                return;
            }
        }
    }

    private void calculateVolumeCharges(final AccountStatus last, final AccountStatus current) {
        if (current instanceof MemberAccountStatus) {
            final MemberAccountStatus memberStatus = (MemberAccountStatus) current;
            final Collection<AccountFeeCharge> charges = accountFeeService.calculateVolumeCharges((MemberAccountStatus) last, memberStatus.getDate());
            for (final AccountFeeCharge charge : charges) {
                if (charge.getAmount().floatValue() > PRECISION_DELTA) {
                    memberStatus.setVolumeAccountFees(memberStatus.getVolumeAccountFees().add(charge.getAmount()));
                    accountFeeChargeDao.insert(charge);
                }
            }
        }
    }

    private void ensureAlive() {
        if (thread == null) {
            throw new ValidationException("general.error.accountStatusProcessing");
        }
    }

    private boolean lock(final boolean forWrite, final Account... accounts) {
        try {
            accountDao.lock(forWrite, Arrays.asList(accounts));
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Processes the next n pending account statuses. Warning: if passing more than 1, on high concurrent systems, it's likely to cause deadlocks, as
     * accounts are locked in the database.
     * @return Returns whether something was actually processed - false if there were no pending records to process
     */
    private boolean processQueue(final int count) throws InterruptedException {
        inProcess = true;
        final RunStatus status = transactionTemplate.execute(new TransactionCallback<RunStatus>() {
            public RunStatus doInTransaction(final TransactionStatus status) {
                return runInTransaction(status, count);
            }
        });
        CurrentTransactionData.cleanup();
        switch (status) {
            case RETRY:
                // Just retry the same pending account status after a small sleep
                queue.offer(count);
                break;
            case ERROR:
                errors++;
                if (errors <= MAX_ERROR_ATTEMPTS) {
                    // Sleep 2sec, then 4, then 8, then 16...
                    Thread.sleep((long) (Math.pow(2, errors) * 1000));
                    // Ensure there will be another try for this status
                    queue.offer(count);
                } else {
                    // CRITICAL STATE!!!
                    // We've not managed to resolve the error, and no more account status processing is done, which means
                    // no more payments can be done! Kill the thread and raise an alert
                    stopThread();
                    transactionTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        protected void doInTransactionWithoutResult(final TransactionStatus status) {
                            PendingAccountStatus pendingStatus = null;
                            Object[] arguments = {};
                            try {
                                pendingStatus = pendingAccountStatusDao.next(1).get(0);
                                final Transfer transfer = pendingStatus.getTransfer();
                                if (transfer != null) {
                                    final LocalSettings localSettings = settingsService.getLocalSettings();
                                    final UnitsConverter unitsConverter = localSettings.getUnitsConverter(transfer.getType().getFrom().getCurrency().getPattern());
                                    final CalendarConverter dateConverter = localSettings.getDateConverter();
                                    arguments = new Object[] { dateConverter.toString(pendingStatus.getDate()), transfer.getFrom().getOwnerName(), transfer.getTo().getOwnerName(), unitsConverter.toString(transfer.getAmount()) };
                                }
                            } catch (final Exception e) {
                                // Ok, leave null
                            }
                            alertService.create(SystemAlert.Alerts.ERROR_PROCESSING_ACCOUNT_STATUS, arguments);
                        }
                    });
                }
        }
        inProcess = false;
        return status != RunStatus.EMPTY;
    }

    private RunStatus runInTransaction(final TransactionStatus status, final int count) {
        try {
            final List<PendingAccountStatus> pendings = pendingAccountStatusDao.next(count);
            if (pendings.isEmpty()) {
                return RunStatus.EMPTY;
            }
            for (final PendingAccountStatus pendingStatus : pendings) {
                final Account account = pendingStatus.getAccount();
                if (account != null) {
                    // In cases where the pending account status references the account (ACCOUNT_FEE_DISABLED, ACCOUNT_FEE_INVOICE, AUTHORIZATION
                    // and LIMIT_CHANGE), perform the lock outside the switch to avoid code replication
                    if (!lock(true, account)) {
                        status.setRollbackOnly();
                        return RunStatus.RETRY;
                    }
                }
                switch (pendingStatus.getType()) {
                    case PAYMENT:
                        final Transfer transfer = pendingStatus.getTransfer();
                        final boolean updateTo = shouldUpdateToAccountOnPayment(pendingStatus, transfer);
                        final boolean updateFrom = shouldUpdateFromAccountOnPayment(pendingStatus, transfer);

                        // Lock the required accounts
                        Account[] toLock = null;
                        if (updateFrom && updateTo) {
                            toLock = new Account[] { transfer.getFrom(), transfer.getTo() };
                        } else if (updateFrom) {
                            toLock = new Account[] { transfer.getFrom() };
                        } else if (updateTo) {
                            toLock = new Account[] { transfer.getTo() };
                        }
                        if (toLock != null) {
                            if (!lock(true, toLock)) {
                                status.setRollbackOnly();
                                return RunStatus.RETRY;
                            }
                        }

                        // IMPORTANT: The to-account status must be updated before the from-account status
                        if (updateTo) {
                            updateAccountStatusOnPayment(pendingStatus, true, transfer, null);
                        }
                        if (updateFrom) {
                            updateAccountStatusOnPayment(pendingStatus, false, transfer, null);
                        }
                        break;
                    case RESERVED_SCHEDULED_PAYMENT:
                        // Lock the from account, to ensure the available balance is ok
                        if (!lock(true, pendingStatus.getScheduledPayment().getFrom())) {
                            status.setRollbackOnly();
                            return RunStatus.RETRY;
                        }
                        updateAccountStatusOnReservedScheduledPayment(pendingStatus, null, true);
                        break;
                    case LIBERATE_RESERVED_INSTALLMENT:
                        updateAccountStatusOnLiberateReservedInstallment(pendingStatus, null, true);
                        break;
                    case ACCOUNT_FEE_DISABLED:
                        updateAccountStatusAccountFeeDisabled(pendingStatus, null, true);
                        break;
                    case ACCOUNT_FEE_INVOICE:
                        updateAccountStatusOnAccountFeeInvoice(pendingStatus, null, true);
                        break;
                    case AUTHORIZATION:
                        updateAccountStatusOnAuthorization(pendingStatus, null, true);
                        break;
                    case LIMIT_CHANGE:
                        updateAccountStatusOnCreditLimitChange(pendingStatus, null, true);
                        break;
                    default:
                        System.out.println("Invalid pending account status type: " + pendingStatus.getType());
                        break;
                }
                // Remove the pending status after processing the new account status
                pendingAccountStatusDao.delete(pendingStatus.getId());
                errors = 0;
            }
            return RunStatus.SUCCESS;

        } catch (final Exception e) {
            e.printStackTrace();
            status.setRollbackOnly();
            return RunStatus.ERROR;
        }
    }

    private boolean shouldUpdateFromAccountOnPayment(final PendingAccountStatus pendingAccountStatus, final Transfer transfer) {
        /*
         * On pending debits, generated fees should not update the account status, or there would be a reserved amount for pending debits that would
         * only be generated when the main transfer were authorized. This is, however, distinct from when the same account is paying a generated
         * transfer (such as a conversion fee), because both amounts will be charged from the same account at once
         */
        final Transfer parent = transfer.getParent();
        final boolean parentAndChildFromSameAccount = parent != null && parent.getFrom().equals(transfer.getFrom());
        final boolean pending = (parent != null && pendingAccountStatus.getTransferStatus() == Payment.Status.PENDING);
        return parentAndChildFromSameAccount || !pending;
    }

    private boolean shouldUpdateToAccountOnPayment(final PendingAccountStatus pendingAccountStatus, final Transfer transfer) {
        // Should update the to account status only if the transfer is processed
        return pendingAccountStatus.getTransferStatus() == Payment.Status.PROCESSED;
    }

    private void stopThread() {
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    private MemberAccountStatus updateAccountStatusAccountFeeDisabled(final PendingAccountStatus pendingAccountStatus, MemberAccountStatus last, final boolean insert) {
        final Account account = pendingAccountStatus.getAccount();
        final BigDecimal amount = pendingAccountStatus.getSubtractedAmount();
        if (last == null) {
            last = (MemberAccountStatus) getStatus(account, null, false, false);
        }
        MemberAccountStatus status = last.newBasedOnThis();
        status.setVolumeAccountFees(status.getVolumeAccountFees().subtract(amount));
        if (insert) {
            status = accountStatusDao.insert(status, false);
        }
        return status;
    }

    private AccountStatus updateAccountStatusOnAccountFeeInvoice(final PendingAccountStatus pendingAccountStatus, MemberAccountStatus last, final boolean insert) {
        final Account account = pendingAccountStatus.getAccount();
        final Invoice invoice = fetchService.fetch(pendingAccountStatus.getInvoice(), RelationshipHelper.nested(Invoice.Relationships.ACCOUNT_FEE_LOG, AccountFeeLog.Relationships.ACCOUNT_FEE));
        final AccountFeeLog accountFeeLog = invoice.getAccountFeeLog();
        if (accountFeeLog != null) {
            final ChargeMode chargeMode = accountFeeLog.getAccountFee().getChargeMode();
            if (chargeMode.isVolume()) {
                if (last == null) {
                    last = (MemberAccountStatus) getStatus(account, null, false, true);
                }
                final AccountStatus status = last.newBasedOnThis();
                MemberAccountStatus memberStatus = (MemberAccountStatus) status;
                final BigDecimal totalFeeAmount = accountFeeChargeDao.totalAmoutForPeriod((MemberAccount) account, accountFeeLog);
                memberStatus.setVolumeAccountFees(memberStatus.getVolumeAccountFees().subtract(totalFeeAmount));
                // Save the status
                if (insert) {
                    memberStatus = (MemberAccountStatus) accountStatusDao.insert(status, false);
                }
                return memberStatus;
            }
        }
        return last;
    }

    /**
     * Beware the order in which this is called. First on the to account, then on the from account. Otherwise rate calculations work with the wrong
     * balance. See remark on updateAccounStatusonPayment.
     * 
     */
    private AccountStatus updateAccountStatusOnAuthorization(final PendingAccountStatus pendingAccountStatus, AccountStatus last, final boolean insert) {
        final Account account = pendingAccountStatus.getAccount();
        final Transfer transfer = pendingAccountStatus.getTransfer();
        final TransferAuthorization transferAuthorization = pendingAccountStatus.getTransferAuthorization();

        if (last == null) {
            last = getStatus(account, null, false, true);
        }
        AccountStatus status = last.newBasedOnThis();
        final boolean isDebit = account.equals(transfer.getFrom());
        final boolean isAuthorized = pendingAccountStatus.getTransferStatus() == Payment.Status.PROCESSED;
        final BigDecimal amount = transfer.getAmount();

        // update balance
        if (isAuthorized) {
            // A PendingAccountStatus for authorization is only inserted when the last level has been reached (the payment is processed).
            // So, we don't have to worry here that an authorization action has been done but the payment is not yet processed.

            // APPLY RATES (d-rate, a-rate)
            updateRatesOnAccountStatus(status, transfer);

            // Update the balance
            updateBalanceWithTransfer(pendingAccountStatus, status, transfer);
        }
        if (isDebit) {
            status.setPendingDebits(status.getPendingDebits().subtract(1, amount));
        }
        status.setTransferAuthorization(transferAuthorization);

        // For member accounts, we must also calculate the account fee charge
        calculateVolumeCharges(last, status);

        // Save the status
        if (insert) {
            status = accountStatusDao.insert(status, false);
        }
        return status;
    }

    private AccountStatus updateAccountStatusOnCreditLimitChange(final PendingAccountStatus pendingAccountStatus, AccountStatus last, final boolean insert) {
        final Account account = pendingAccountStatus.getAccount();

        // Update the account status
        if (last == null) {
            last = getStatus(account, null, false, true);
        }
        AccountStatus status = last.newBasedOnThis();
        status.setCreditLimit(pendingAccountStatus.getLowerLimit());
        status.setUpperCreditLimit(pendingAccountStatus.getUpperLimit());
        status.setCreditLimitChangedBy(pendingAccountStatus.getBy());

        // For member accounts, we must also calculate the account fee charge
        calculateVolumeCharges(last, status);

        // Save the status
        if (insert) {
            status = accountStatusDao.insert(status, false);
        }
        return status;
    }

    private AccountStatus updateAccountStatusOnLiberateReservedInstallment(final PendingAccountStatus pendingAccountStatus, AccountStatus last, final boolean insert) {
        final Account account = pendingAccountStatus.getAccount();

        // Update the account status
        if (last == null) {
            last = getStatus(account, null, false, true);
        }
        AccountStatus status = last.newBasedOnThis();
        final Transfer transfer = pendingAccountStatus.getTransfer();
        final ScheduledPayment scheduledPayment = transfer.getScheduledPayment();
        status.setTransfer(transfer);
        status.setScheduledPayment(scheduledPayment);
        status.setReservedScheduledPayments(status.getReservedScheduledPayments().subtract(transfer.getActualAmount()));

        // Save the status
        if (insert) {
            status = accountStatusDao.insert(status, false);
        }
        return status;
    }

    /**
     * <b>Very important:</b> The order in which methods are called for a transfer is very important for the correct handling of rates.<br>
     * This order should be as follows:
     * <ol>
     * <li>First handle the complete procedure for the to account. This is because the balance and rateBalanceCorrection of the from account are
     * needed for correctly passing rates to the to-account. If you would first update the accountStatus of the from account, then balance and RBC of
     * the from account are already changed, meaning that the next call for the to-account would use the wrong values for passing the rates.
     * <li>when changing an account (to or from), first take care that the rate fields are initialized. Null rate fields will result in repeated alert
     * fires.
     * <li>First apply rates before changing the account balances, otherwise the rates methods will work with the wrong account balance.
     * <li>Only then update the balance. Use AccountStatusDAO.updateBalanceWithTransfer for this, as it will also correct the RateBalanceCorrection
     * Field.
     * </ol>
     */
    private AccountStatus updateAccountStatusOnPayment(final PendingAccountStatus pendingStatus, final boolean to, final Transfer transfer, AccountStatus last) {
        final Account account = to ? transfer.getTo() : transfer.getFrom();
        final boolean from = !to;

        final Status transferStatus = pendingStatus.getTransferStatus();
        final boolean isProcessed = transferStatus == Payment.Status.PROCESSED;
        if (!isProcessed && transferStatus != Payment.Status.PENDING) {
            // When it's neither processed nor pending, we won't generate a new account status
            return last;
        }

        final boolean insert = last == null;

        // A very special case here is an scheduled payment which is manually processed (pay now) and still has to be authorized.
        // In this case, the process date is null and the date is on the future. We can't use either, but the current date instead
        final Calendar date = transfer.getProcessDate() == null ? Calendar.getInstance() : transfer.getProcessDate();
        final Currency currency = account.getType().getCurrency();
        if (last == null) {
            if (isProcessed && (currency.isEnableARate() || currency.isEnableDRate())) {
                // for rates, the accountstatus MUST be the last one available up to present, because updating next statuses gets really complicated
                last = getStatus(account, null, false, false);
            } else {
                last = getStatus(account, date, false, false);
            }
        }
        AccountStatus status = last.newBasedOnThis();

        // For payments that are not on the past date, update the amount for the new one
        final boolean isDebit = account.equals(transfer.getFrom());
        final BigDecimal amount = transfer.getAmount();

        if (isProcessed) {
            // APPLY RATES (d-rate, a-rate)
            updateRatesOnAccountStatus(status, transfer);

            // update balance
            updateBalanceWithTransfer(pendingStatus, status, transfer);
        } else if (isDebit) {
            // Update only the pending debits
            status.setPendingDebits(status.getPendingDebits().add(amount));
        }

        // Process the reserved amount for total amount on scheduled payments, if applicable
        if (from && transfer.getScheduledPayment() != null && transfer.getScheduledPayment().isReserveAmount()) {
            status.setReservedScheduledPayments(status.getReservedScheduledPayments().subtract(transfer.getAmount()));
        }

        // For member accounts:
        if (status instanceof MemberAccountStatus) {
            final MemberAccountStatus memberStatus = (MemberAccountStatus) status;

            // On top-level payments of account fees, reduce the account fees accumulator
            boolean isVolumeAccountFee = false;
            final AccountFeeLog accountFeeLog = transfer.getAccountFeeLog();
            if (transfer.getParent() == null && accountFeeLog != null) {
                isVolumeAccountFee = accountFeeLog.getAccountFee().getChargeMode().isVolume();
            }

            if (isVolumeAccountFee) {
                // Subtract the account fees when this transfer is related to a volume account fee
                final BigDecimal totalFeeAmount = accountFeeChargeDao.totalAmoutForPeriod((MemberAccount) account, transfer.getAccountFeeLog());
                memberStatus.setVolumeAccountFees(memberStatus.getVolumeAccountFees().subtract(totalFeeAmount));
            } else {
                // Calculate the account fee charges
                calculateVolumeCharges(last, status);
            }
        }

        // Save the status
        status.setTransfer(transfer);
        status.setDate(date);
        if (insert) {
            status = accountStatusDao.insert(status, false);

            // Ensure the account status in future, if exists, are ok
            if (!currency.isEnableARate() && !currency.isEnableDRate()) {
                accountStatusDao.updateStatusesInFuture(status);
            }
        }
        return status;
    }

    private AccountStatus updateAccountStatusOnReservedScheduledPayment(final PendingAccountStatus pendingStatus, AccountStatus last, final boolean insert) {
        final ScheduledPayment scheduledPayment = pendingStatus.getScheduledPayment();
        final Calendar date = scheduledPayment.getDate();
        final Account account = scheduledPayment.getFrom();
        if (last == null) {
            last = getStatus(account, date, false, false);
        }

        // Calculate what will be added
        final int payments = scheduledPayment.getTransfers().size();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (final Transfer transfer : scheduledPayment.getTransfers()) {
            totalAmount = totalAmount.add(transfer.getAmount());
        }

        // Create the new status
        AccountStatus status = last.newBasedOnThis();
        status.setDate(date);
        status.setScheduledPayment(scheduledPayment);
        status.setReservedScheduledPayments(status.getReservedScheduledPayments().add(payments, totalAmount));
        if (insert) {
            status = accountStatusDao.insert(status, false);
        }

        return status;
    }

    /**
     * adds or substracts an amount to the status balance. Call this in stead of directly increasing credits or debits, as it takes care for rate
     * balance correction field too. Only updates the real credits and debits, not the pending debits and credits.
     * 
     * @param status The status to be updated. Nothing is saved; the status fields are just updated.
     * @param transfer the transfer being processed.
     */
    private void updateBalanceWithTransfer(final PendingAccountStatus pendingStatus, final AccountStatus status, final Transfer transfer) {
        if (pendingStatus.getTransferStatus() != Payment.Status.PROCESSED) {
            // method only deals with real debits and credits; not with pendings...
            return;
        }
        final BigDecimal amount = transfer.getAmount();
        final boolean isDebit = status.getAccount().equals(transfer.getFrom());
        final boolean isRoot = transfer.isRoot();

        final Currency currency = status.getAccount().getType().getCurrency();
        final Calendar date = transfer.getProcessDate();
        final boolean rated = (currency.isEnableARate(date) || currency.isEnableDRate(date));
        if (isDebit) {
            // call this first, before the balance is updated.
            if (rated) {
                aRateService.updateRateBalanceCorrectionOnFromAccount(status, amount);
            }
            if (isRoot) {
                status.setRootDebits(status.getRootDebits().add(amount));
            } else {
                status.setNestedDebits(status.getNestedDebits().add(amount));
            }
        } else {
            // call this first, before the balance is updated.
            if (rated && amount.compareTo(BigDecimal.ZERO) < 0) {
                // special case for chargebacks (having negative transfer amounts)
                aRateService.updateRateBalanceCorrectionOnFromAccount(status, amount.negate());
            }
            if (isRoot) {
                status.setRootCredits(status.getRootCredits().add(amount));
            } else {
                status.setNestedCredits(status.getNestedCredits().add(amount));
            }
        }
    }

    private void updateRatesOnAccountStatus(AccountStatus status, final Transfer transfer) {
        final Calendar date = transfer.getProcessDate() == null ? transfer.getDate() : transfer.getProcessDate();
        final Currency currency = status.getAccount().getType().getCurrency();
        if (currency.isEnableARate(date) || currency.isEnableDRate(date)) {
            status = aRateService.initializeRateBalanceCorrectionOnAccounts(status, transfer);
        }
        final boolean isDebit = status.getAccount().equals(transfer.getFrom());
        if (!isDebit) {
            aRateService.applyTransfer(status, transfer);
            dRateService.applyTransfer(status, transfer);
        }
    }

}

/*
 This file is part of Cyclos.

 Cyclos is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 Foobar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
  
 */
package nl.strohalm.cyclos.services.accountfees;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.ChargeMode;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.InvoiceMode;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.PaymentDirection;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.utils.Amount;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.conversion.AmountConverter;
import nl.strohalm.cyclos.utils.conversion.CalendarConverter;
import nl.strohalm.cyclos.utils.conversion.UnitsConverter;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * A thread used to charge and control a specific account fee. An account fee is charged by member group and member account.
 * @author luis
 */
public class ChargeAccountFeeThread extends Thread {

    private LoggingHandler                             loggingHandler;
    private AccountFeeService                          accountFeeService;
    private AccountService                             accountService;
    private FetchService                               fetchService;
    private AlertService                               alertService;
    private ChargeFeeDTO                               dto;
    private InvoiceService                             invoiceService;
    private PaymentService                             paymentService;
    private SettingsService                            settingsService;
    private TransactionTemplate                        transactionTemplate;
    private AccountStatusHandler                       accountStatusHandler;
    private boolean                                    cancelled;
    private int                                        processedMembers;
    private int                                        charges;
    private int                                        invoices;
    private long                                       delay;
    private Throwable                                  throwable;

    /** Collection of registered listeners */
    private final Collection<AccountFeeThreadListener> listeners = Collections.synchronizedSet(new HashSet<AccountFeeThreadListener>());

    public ChargeAccountFeeThread() {
    }

    /**
     * Adds a thread listener for this thread
     */
    public void addListener(final AccountFeeThreadListener listener) {
        listeners.add(listener);
    }

    /**
     * Cancels this running thread
     */
    public void cancel() {
        if (!isAlive()) {
            throw new IllegalStateException("Not running");
        }
        cancelled = true;
        try {
            interrupt();
        } finally {
            finish();
            loggingHandler.logAccountFeeStatus(getAccountFeeLog());
        }
    }

    /**
     * Starts charging an account fee on a new thread, but may be skipped if the fee is scheduled to the future only
     */
    public boolean charge(final ChargeFeeDTO chargeFeeDTO) {

        // Check the thread status
        if (isAlive() || isInterrupted()) {
            throw new IllegalStateException("Already running or cancelled");
        }

        dto = chargeFeeDTO;

        // Initializes the charge
        initialize();

        if (dto.getFeeLog() == null) {
            // No log means it shouldn't execute right now
            return false;
        }

        // Set this thread name (useful for debugging)
        setName("Account fee charge: " + dto.getFee().getName() + " for " + settingsService.getLocalSettings().getApplicationName());

        // Run the account fee
        start();

        return true;
    }

    /**
     * Returns the account fee log associated with this thread
     */
    public AccountFeeLog getAccountFeeLog() {
        return dto.getFeeLog();
    }

    /**
     * Returns the total number of charges
     */
    public int getCharges() {
        return charges;
    }

    public long getDelay() {
        return delay;
    }

    /**
     * Returns the total number of invoices sent
     */
    public int getInvoices() {
        return invoices;
    }

    /**
     * Returns the total number of processed members
     */
    public int getProcessedMembers() {
        return processedMembers;
    }

    /**
     * Returns if this thread has been cancelled
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns if this thread has generated an error
     */
    public boolean isError() {
        return throwable != null;
    }

    /**
     * Removes a thread listener for this thread
     */
    public void removeListener(final AccountFeeThreadListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void run() {

        // Apply the dalay if set
        if (delay > 0L) {
            try {
                Thread.sleep(delay);
            } catch (final InterruptedException e) {
                return;
            }
        }

        final AccountFeeLog feeLog = dto.getFeeLog();
        loggingHandler.logAccountFeeStatus(feeLog);
        try {
            boolean finished;
            do {
                finished = processNextBatch();
            } while (!finished);
        } catch (final Throwable e) {
            // Log the error if not cancelled
            if (!isCancelled()) {
                throwable = e;
                loggingHandler.logAccountFeeError(feeLog, e);
                // Rethrow the exception
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                } else if (e instanceof Error) {
                    throw (Error) e;
                } else {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            if (!isCancelled()) {
                finish();
                loggingHandler.logAccountFeeStatus(feeLog);
            }
        }
    }

    public void setAccountFeeService(final AccountFeeService accountFeeService) {
        this.accountFeeService = accountFeeService;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setDelay(final long delay) {
        this.delay = delay;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * Runs the charging within a transaction, charging the next batch of members
     */
    private boolean chargeNext() {
        final AccountFeeLog feeLog = dto.getFeeLog();

        // Charge each member
        final List<Member> toCharge = accountFeeService.nextMembersToCharge(feeLog);
        if (toCharge.isEmpty()) {
            // No more members to charge
            return false;
        }

        // Get the charge data
        final BigDecimal minPayment = paymentService.getMinimumPayment();
        final AccountFee fee = fetchService.fetch(dto.getFee(), RelationshipHelper.nested(AccountFee.Relationships.ACCOUNT_TYPE, AccountType.Relationships.CURRENCY));
        final LocalSettings localSettings = settingsService.getLocalSettings();

        final Period period = feeLog.getPeriod();
        final MemberAccountType accountType = fee.getAccountType();
        final Amount am = feeLog.getAmountValue();
        final ChargeMode chargeMode = fee.getChargeMode();

        final BigDecimal freeBase = fee.getFreeBase();

        // Iterate each member
        for (final Member member : toCharge) {
            if (isCancelled()) {
                throw new IllegalStateException("cancelled");
            }

            Transfer transfer = null;
            Invoice invoice = null;

            // Calculate the charge amount
            BigDecimal chargedAmount = BigDecimal.ZERO;
            BigDecimal amount = BigDecimal.ZERO;
            final GetTransactionsDTO trxDto = new GetTransactionsDTO(member, accountType, period);
            if (chargeMode.isFixed()) {
                boolean charge = true;
                if (freeBase != null) {
                    final BigDecimal balance = accountService.getBalance(trxDto);
                    if (balance.compareTo(freeBase) <= 0) {
                        charge = false;
                    }
                }
                // Fixed fee amount
                if (charge) {
                    amount = feeLog.getAmount();
                }
            } else if (chargeMode.isBalance()) {
                // Percentage over balance
                final boolean positiveBalance = !chargeMode.isNegative();
                BigDecimal balance = accountService.getBalance(trxDto);
                // Skip if balance is out of range
                boolean charge = true;
                // Apply the free base
                if (freeBase != null) {
                    if (positiveBalance) {
                        balance = balance.subtract(freeBase);
                    } else {
                        balance = balance.add(freeBase);
                    }
                }
                // Check if something will be charged
                if ((positiveBalance && balance.compareTo(BigDecimal.ZERO) <= 0) || (!positiveBalance && balance.compareTo(BigDecimal.ZERO) >= 0)) {
                    charge = false;
                }
                if (charge) {
                    // Get the charged amount
                    chargedAmount = am.apply(balance.abs());
                    amount = localSettings.round(chargedAmount);
                }
            } else if (chargeMode.isVolume()) {
                // Percentage over average transactioned volume
                amount = accountFeeService.calculateChargeOverTransactionedVolume(feeLog, member);
            }

            // Charge the account fee if the amount is ok
            if (amount.compareTo(minPayment) >= 0) {
                // Check who is paying: the member or the system
                if (fee.getPaymentDirection() == PaymentDirection.TO_SYSTEM) {
                    // Member paying to system
                    if (fee.getInvoiceMode() == InvoiceMode.ALWAYS) {
                        invoice = sendInvoice(fee, feeLog, member, amount);
                    } else {
                        try {
                            transfer = insertTransfer(fee, feeLog, member, amount);
                        } catch (final NotEnoughCreditsException e) {
                            // Sends an Invoice to this member!
                            invoice = sendInvoice(fee, feeLog, member, amount);
                        }
                    }
                } else {
                    // System paying to member
                    transfer = insertTransfer(fee, feeLog, member, amount);
                }
            }

            // Compute the result
            processedMembers++;
            if (transfer != null) {
                charges++;
            } else if (invoice != null) {
                invoices++;
            }
            accountFeeService.setChargingResult(feeLog, member, transfer, invoice);

            // Yield the thread, so if there are other threads on the same CPU, they will be executed
            Thread.yield();
        }
        return true;
    }

    /**
     * Finishes charging the fee, updating a log status. Runs on a separate transaction.
     */
    private void finish() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                // Determine the status
                SystemAlert.Alerts alert;
                AccountFeeLog.Status logStatus;
                if (isCancelled()) {
                    logStatus = AccountFeeLog.Status.CANCELED;
                    alert = SystemAlert.Alerts.ACCOUNT_FEE_CANCELLED;
                } else if (isError()) {
                    logStatus = AccountFeeLog.Status.PARTIALLY_FAILED;
                    alert = SystemAlert.Alerts.ACCOUNT_FEE_FAILED;
                } else {
                    logStatus = AccountFeeLog.Status.FINISHED;
                    alert = SystemAlert.Alerts.ACCOUNT_FEE_FINISHED;
                }

                // Update the fee log
                final AccountFeeLog feeLog = dto.getFeeLog();
                feeLog.setStatus(logStatus);
                dto.setFeeLog(accountFeeService.save(feeLog));

                // Create an alert
                alertService.create(alert, dto.getFee().getName());
            }
        });

        // Notify listeners
        for (final AccountFeeThreadListener listener : listeners) {
            listener.threadFinished(this);
        }
    }

    /**
     * Returns a description for a transfer
     */
    private String getPaymentDescription(AccountFee fee, final AccountFeeLog feeLog, final Member member, final BigDecimal amount) {

        final LocalSettings localSettings = settingsService.getLocalSettings();
        final AmountConverter amountConverter = localSettings.getAmountConverter();
        final UnitsConverter unitsConverter = localSettings.getUnitsConverter(fee.getAccountType().getCurrency().getPattern());
        final CalendarConverter dateConverter = localSettings.getDateConverter();

        final Map<String, Object> values = new HashMap<String, Object>();
        final Amount amountValue = feeLog.getAmountValue();
        if (amountValue.getType() == Amount.Type.PERCENTAGE) {
            values.put("fee_amount", amountConverter.toString(amountValue));
        } else {
            values.put("fee_amount", unitsConverter.toString(amountValue.getValue()));
        }
        values.put("free_base", unitsConverter.toString(fee.getFreeBase()));
        values.put("result", unitsConverter.toString(amount));
        final Period period = feeLog.getPeriod();
        values.put("begin_date", dateConverter.toString(period == null ? null : period.getBegin()));
        values.put("end_date", dateConverter.toString(period == null ? null : period.getEnd()));

        fee = fetchService.fetch(fee, AccountFee.Relationships.TRANSFER_TYPE);

        return MessageProcessingHelper.processVariables(fee.getTransferType().getDescription(), values);
    }

    /**
     * Initializes the charging, creating a log if necessary, or updating a log status. Runs on a separate transaction.
     */
    private void initialize() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {

                // Fetch the fee we're charging
                final AccountFee fee = fetchService.fetch(dto.getFee(), AccountFee.Relationships.ACCOUNT_TYPE, AccountFee.Relationships.GROUPS);
                AccountFeeLog feeLog = fetchService.fetch(dto.getFeeLog());

                // Validates the entities
                if (fee == null) {
                    throw new UnexpectedEntityException();
                }
                if (feeLog != null && !feeLog.getStatus().isCanRecharge()) {
                    throw new UnexpectedEntityException("Cannot recharge fee with status = " + feeLog.getStatus());
                }

                // Update the fee log
                if (feeLog == null) {
                    // New execution
                    feeLog = accountFeeService.getNextExecution(fee);
                }
                if (feeLog == null) {
                    // When the log is null, it means it shouldn't be running now, only in future. Skip
                    return;
                }
                feeLog.setDate(Calendar.getInstance());
                feeLog.setStatus(AccountFeeLog.Status.RUNNING);
                feeLog = accountFeeService.save(feeLog);

                // Update the dto with the updated instances
                dto.setFee(fee);
                dto.setFeeLog(feeLog);

                // Create a system alert
                alertService.create(SystemAlert.Alerts.ACCOUNT_FEE_RUNNING, dto.getFee().getName());
            }
        });
    }

    /**
     * Insert the transfer that charges a fee
     */
    private Transfer insertTransfer(final AccountFee fee, final AccountFeeLog feeLog, final Member member, final BigDecimal amount) {
        final TransferDTO dto = new TransferDTO();
        dto.setAutomatic(true);
        // Determine who pays
        if (fee.getPaymentDirection() == PaymentDirection.TO_SYSTEM) {
            // Member paying to system
            dto.setFromOwner(member);
            dto.setToOwner(SystemAccountOwner.instance());

            // We force the payment if member paying to system and fee never sends invoices
            dto.setForced(fee.getInvoiceMode() == InvoiceMode.NEVER);
        } else {
            // System paying to member
            dto.setFromOwner(SystemAccountOwner.instance());
            dto.setToOwner(member);
        }
        dto.setAmount(amount);
        dto.setTransferType(fee.getTransferType());
        dto.setDescription(getPaymentDescription(fee, feeLog, member, amount));
        dto.setAccountFeeLog(feeLog);
        final Transfer transfer = (Transfer) paymentService.insertWithNotification(dto);
        loggingHandler.logAccountFeePayment(transfer);
        return transfer;
    }

    private boolean processNextBatch() {
        // Run inside a new transaction
        final boolean finished = !transactionTemplate.execute(new TransactionCallback<Boolean>() {
            public Boolean doInTransaction(final TransactionStatus status) {
                try {
                    return chargeNext();
                } catch (final RuntimeException e) {
                    status.setRollbackOnly();
                    throw e;
                }
            }
        });

        // Ensure the generated pending account statuses are processed
        accountStatusHandler.processFromCurrentTransaction();
        CurrentTransactionData.runCurrentTransactionCommitListeners();
        CurrentTransactionData.cleanup();
        return finished;
    }

    /**
     * Sends an invoice for a fee
     */
    private Invoice sendInvoice(final AccountFee fee, final AccountFeeLog feeLog, final Member member, final BigDecimal amount) {
        Invoice invoice = new Invoice();
        invoice.setFromMember(null);
        invoice.setFrom(SystemAccountOwner.instance());
        invoice.setTo(member);
        invoice.setAmount(amount);
        invoice.setTransferType(fee.getTransferType());
        invoice.setDescription(getPaymentDescription(fee, feeLog, member, amount));
        invoice.setAccountFeeLog(feeLog);
        invoice = invoiceService.sendAutomatically(invoice);
        loggingHandler.logAccountFeeInvoice(invoice);
        return invoice;
    }

}

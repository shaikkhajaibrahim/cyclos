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
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import nl.strohalm.cyclos.dao.accounts.fee.account.AccountFeeChargeDAO;
import nl.strohalm.cyclos.dao.accounts.fee.account.AccountFeeDAO;
import nl.strohalm.cyclos.dao.accounts.fee.account.AccountFeeLogDAO;
import nl.strohalm.cyclos.dao.accounts.fee.account.MemberAccountFeeLogDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountStatus;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeCharge;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLogQuery;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeQuery;
import nl.strohalm.cyclos.entities.accounts.fees.account.MemberAccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.ChargeMode;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.PaymentDirection;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee.RunMode;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings.Precision;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.AccountStatusHandler;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.TransactionVolumeDTO;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.application.ApplicationService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.BigDecimalHelper;
import nl.strohalm.cyclos.utils.DataIteratorHelper;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.TimePeriod.Field;
import nl.strohalm.cyclos.utils.query.IteratorList;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.AnyOfValidation;
import nl.strohalm.cyclos.utils.validation.CompareToValidation;
import nl.strohalm.cyclos.utils.validation.PositiveNonZeroValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;

/**
 * Implementation class for account fee service
 * @author rafael
 * @author luis
 */
public class AccountFeeServiceImpl implements AccountFeeService, AccountFeeThreadListener, BeanFactoryAware, DisposableBean {
    /**
     * Validates day as required if recurrence > DAY
     * @author luis
     */
    public final class DayValidation implements PropertyValidation {

        private static final long serialVersionUID = 21232137527653089L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final AccountFee fee = (AccountFee) object;
            final TimePeriod recurrence = fee.getRecurrence();
            final TimePeriod.Field field = recurrence == null ? null : recurrence.getField();
            // Day is required when recurrence != days
            if (TimePeriod.Field.DAYS != field) {
                return RequiredValidation.instance().validate(object, property, field);
            }
            return null;
        }
    }

    /**
     * A property validation that uses a nested validation depending on a property value
     * @author luis
     */
    public final class When implements PropertyValidation {
        private static final long        serialVersionUID = 2323698706458315087L;
        private final Collection<?>      conditions;
        private final PropertyValidation validation;

        public When(final Collection<?> conditions, final PropertyValidation validation) {
            this.conditions = conditions;
            this.validation = validation;
        }

        public When(final Object condition, final PropertyValidation validation) {
            this(Collections.singleton(condition), validation);
        }

        public ValidationError validate(final Object object, final Object name, final Object value) {
            final AccountFee fee = (AccountFee) object;
            boolean eval = false;
            final Object condition = conditions.iterator().next();
            if (condition instanceof RunMode && conditions.contains(fee.getRunMode())) {
                eval = true;
            } else if (condition instanceof PaymentDirection && conditions.contains(fee.getPaymentDirection())) {
                eval = true;
            } else if (condition instanceof ChargeMode && conditions.contains(fee.getChargeMode())) {
                eval = true;
            }
            if (eval) {
                return validation.validate(object, name, value);
            }
            return null;
        }
    }

    private static final MathContext                           ACCOUNT_FEE_MATH_CONTEXT      = Precision.SIX.getMathContext();
    private static final BigDecimal                            MINIMUM_ACCOUNT_FEE_PAYMENT   = new BigDecimal(BigInteger.ONE, Precision.SIX.getValue());
    private static int                                         ACCOUNT_FEE_CHARGE_BATCH_SIZE = 20;

    /** The running threads by account fee log */
    private final Map<AccountFeeLog, ChargeAccountFeeThread>   chargeFeeThreads              = new HashMap<AccountFeeLog, ChargeAccountFeeThread>();

    private AccountFeeDAO                                      accountFeeDao;
    private AccountFeeLogDAO                                   accountFeeLogDao;
    private AccountFeeChargeDAO                                accountFeeChargeDao;
    private ApplicationService                                 applicationService;
    private AccountService                                     accountService;
    private SettingsService                                    settingsService;
    private FetchService                                       fetchService;
    private AccountStatusHandler                               accountStatusHandler;
    private AlertService                                       alertService;
    private BeanFactory                                        beanFactory;
    private long                                               threadDelay;
    private MemberAccountFeeLogDAO                             memberAccountFeeLogDao;
    private ConcurrentMap<MemberAccountType, List<AccountFee>> cachedVolumeFeesByAccount     = new ConcurrentHashMap<MemberAccountType, List<AccountFee>>();

    public MemberAccountStatus adjustVolumeChargesToDate(MemberAccountStatus status, final Calendar date) {

        final MemberAccount account = (MemberAccount) fetchService.fetch(status.getAccount(), Account.Relationships.TYPE);
        BigDecimal amountInTolerance = BigDecimal.ZERO;
        BigDecimal amountToDate = BigDecimal.ZERO;
        final BigDecimal availableBalance = status.getAvailableBalanceWithoutCreditLimit();

        // Get the account scheduled and enabled fees
        final List<AccountFee> volumeFees = getVolumeFees((MemberAccountType) account.getType());
        for (final AccountFee accountFee : volumeFees) {
            // We only want to calculate charges over scheduled logs
            final AccountFeeLog accountFeeLog = resolveLogForDate(accountFee, date);
            if (accountFeeLog.getStatus() != AccountFeeLog.Status.SCHEDULED) {
                continue;
            }

            // Get the charge to date
            final AccountFeeCharge chargeToDate = calculateAccountFeeCharge(status, date, accountFee);

            // Check the tolerance period
            final TimePeriod tolerance = accountFeeLog.getTolerance();
            if (tolerance != null && tolerance.getNumber() > 0) {

                // Check whether the amount to date is within the tolerance period
                if (chargeToDate != null && chargeToDate.getPeriod().getBegin().before(tolerance.remove(date))) {
                    // Before the tolerance period. Use the amount
                    amountToDate = amountToDate.add(chargeToDate.getAmount());
                }

                // Find the charges within the tolerance period
                final BigDecimal periodSeconds = new BigDecimal(accountFeeLog.getPeriod().getDifference());
                final IteratorList<AccountFeeCharge> chargesInTolerance = accountFeeChargeDao.listChargesInTolerance(account, date, accountFeeLog);
                for (final AccountFeeCharge charge : chargesInTolerance) {
                    // When the charged balance was smaller than the current balance, ignore this charge
                    if (charge.getAvailableBalance().compareTo(availableBalance) <= 0) {
                        continue;
                    }
                    // Calculate the amount again, using the current balance
                    final BigDecimal chargeSeconds = new BigDecimal(charge.getPeriod().getDifference());
                    final BigDecimal newAmount = accountFeeLog.getAmountValue().apply(availableBalance.multiply(chargeSeconds).divide(periodSeconds, ACCOUNT_FEE_MATH_CONTEXT));
                    final BigDecimal diff = charge.getAmount().subtract(newAmount);
                    if (diff.compareTo(BigDecimal.ZERO) > 0) {
                        amountInTolerance = amountInTolerance.add(diff);
                    }
                }
            } else {
                // No tolerance period. Use the charged amount to date as is
                if (chargeToDate != null) {
                    amountToDate = amountToDate.add(chargeToDate.getAmount());
                }
            }
        }

        // Apply the amounts
        final boolean useAmountToDate = amountToDate.compareTo(BigDecimal.ZERO) > 0;
        final boolean useAmountInTolerance = amountInTolerance.compareTo(BigDecimal.ZERO) > 0;
        if (useAmountToDate || useAmountInTolerance) {
            status = status.newBasedOnThis();
            final BigDecimal newAmount = status.getVolumeAccountFees().add(amountToDate).subtract(amountInTolerance);
            if (newAmount.compareTo(BigDecimal.ZERO) > 0) {
                status.setVolumeAccountFees(newAmount);
            }
        }

        return status;
    }

    public BigDecimal calculateChargeOverTransactionedVolume(AccountFeeLog feeLog, final Member member) {
        feeLog = fetchService.fetch(feeLog, AccountFeeLog.Relationships.ACCOUNT_FEE);
        final AccountFee fee = feeLog.getAccountFee();

        // Validate the preconditions
        final Period period = feeLog.getPeriod();
        final ChargeMode chargeMode = fee.getChargeMode();
        if (!fee.isEnabled() || !chargeMode.isVolume() || period == null) {
            return BigDecimal.ZERO;
        }

        // Check which calculation method will be used
        final Calendar accountStatusEnabledSince = applicationService.getAccountStatusEnabledSince();
        final Calendar accountFeeEnabledSince = fee.getEnabledSince();
        final boolean calculateUsingAccountFeeCharge = !(accountStatusEnabledSince.after(period.getBegin()) || accountFeeEnabledSince.after(period.getBegin()));

        BigDecimal chargedAmount;
        if (calculateUsingAccountFeeCharge) {
            // Since we have the account fee charges, just sum the amounts
            final GetTransactionsDTO accountDTO = new GetTransactionsDTO(member, fee.getAccountType());

            // We just need to ensure that an AccountFeeCharge will be generated for the period end
            final MemberAccountStatus status = (MemberAccountStatus) accountService.getStatus(accountDTO);
            insertChargeForDate(status, feeLog, feeLog.getPeriod().getEnd());

            // Then, calculate the total amount
            chargedAmount = accountFeeChargeDao.totalAmoutForPeriod((MemberAccount) status.getAccount(), feeLog);
        } else {
            // We don't have account fee charges... Calculate the "hard" way
            final TransactionVolumeDTO vol = new TransactionVolumeDTO();
            vol.setAccountOwner(member);
            vol.setAccountType(fee.getAccountType());
            vol.setFreeBase(BigDecimalHelper.nvl(feeLog.getFreeBase()));
            vol.setPeriod(period);
            vol.setTolerance(feeLog.getTolerance());
            vol.setPositiveVolume(!chargeMode.isNegative());
            final BigDecimal volume = accountService.calculateTransactionedVolume(vol);
            chargedAmount = feeLog.getAmountValue().apply(volume.abs());
        }

        // Round the result
        final LocalSettings localSettings = settingsService.getLocalSettings();
        return localSettings.round(chargedAmount);
    }

    public Collection<AccountFeeCharge> calculateVolumeCharges(final MemberAccountStatus status, final Calendar date) {
        final MemberAccount account = (MemberAccount) fetchService.fetch(status.getAccount(), Account.Relationships.TYPE);
        final Collection<AccountFeeCharge> charges = new ArrayList<AccountFeeCharge>();
        // Get the account scheduled and enabled fees
        final List<AccountFee> volumeFees = getVolumeFees((MemberAccountType) account.getType());
        for (final AccountFee accountFee : volumeFees) {
            final AccountFeeCharge charge = calculateAccountFeeCharge(status, date, accountFee);
            if (charge != null) {
                charges.add(charge);
            }
        }
        return charges;
    }

    public void cancel(final AccountFeeLog log) {
        final ChargeAccountFeeThread thread = chargeFeeThreads.get(log);
        if (thread == null) {
            throw new UnexpectedEntityException();
        }
        thread.cancel();
    }

    public ChargeAccountFeeThread chargeManual(AccountFee fee) {
        // Validates the fee
        if (fee == null || fee.isTransient()) {
            throw new UnexpectedEntityException();
        }
        fee = fetchService.fetch(fee, RelationshipHelper.nested(AccountFee.Relationships.ACCOUNT_TYPE, AccountType.Relationships.CURRENCY), AccountFee.Relationships.TRANSFER_TYPE);
        if (fee.getRunMode() != RunMode.MANUAL) {
            throw new UnexpectedEntityException();
        }

        return doCharge(fee);
    }

    public ChargeAccountFeeThread chargeScheduled(AccountFee fee) {
        // Validates the fee
        if (fee == null || fee.isTransient()) {
            throw new UnexpectedEntityException();
        }
        fee = fetchService.fetch(fee, RelationshipHelper.nested(AccountFee.Relationships.ACCOUNT_TYPE, AccountType.Relationships.CURRENCY), AccountFee.Relationships.TRANSFER_TYPE);
        if (fee.getRunMode() != RunMode.SCHEDULED) {
            throw new UnexpectedEntityException();
        }

        return doCharge(fee);
    }

    public int chargeScheduledFees(final Calendar time) {
        final AccountFeeQuery query = new AccountFeeQuery();
        query.setReturnDisabled(false);
        query.setResultType(ResultType.LIST);
        query.setHour((byte) time.get(Calendar.HOUR_OF_DAY));
        query.setType(RunMode.SCHEDULED);
        query.fetch(AccountFee.Relationships.LOGS);

        final List<AccountFee> list = new ArrayList<AccountFee>();
        // Get the daily fees
        query.setRecurrence(TimePeriod.Field.DAYS);
        list.addAll(search(query));
        // Get the weekly fees
        query.setRecurrence(TimePeriod.Field.WEEKS);
        query.setDay((byte) time.get(Calendar.DAY_OF_WEEK));
        list.addAll(search(query));
        // Get the monthly fees
        query.setRecurrence(TimePeriod.Field.MONTHS);
        query.setDay((byte) time.get(Calendar.DAY_OF_MONTH));
        list.addAll(search(query));
        int count = 0;
        for (final AccountFee fee : list) {
            final AccountFeeLog lastExecution = fee.getLastExecution();
            boolean charge;
            if (lastExecution == null) {
                // Was never executed. Charge now
                charge = true;
            } else {
                final TimePeriod recurrence = fee.getRecurrence();
                if (recurrence.getNumber() == 1) {
                    // When recurrence is every day or week or month, charge now
                    charge = true;
                } else {
                    // Check the recurrence
                    final Calendar lastExecutionDate = lastExecution.getDate();
                    if (lastExecutionDate.after(time)) {
                        // Consistency check: don't charge if last execution was after the current time
                        charge = false;
                    }
                    // Find the number of elapsed periods
                    int number = 0;
                    final Calendar cal = DateHelper.truncate(lastExecutionDate);
                    final int calendarField = recurrence.getField().getCalendarValue();
                    final Calendar date = DateHelper.truncate(time);
                    while (cal.before(date)) {
                        number++;
                        cal.add(calendarField, 1);
                    }
                    // Charge each 'x' periods
                    charge = number % recurrence.getNumber() == 0;
                }
            }
            // Charge the fee
            if (charge) {
                chargeScheduled(fee);
                count++;
            }
        }
        return count;
    }

    public void destroy() throws Exception {
        for (final ChargeAccountFeeThread thread : chargeFeeThreads.values()) {
            try {
                thread.interrupt();
            } catch (final Exception e) {
                // Ignore
            }
        }
        chargeFeeThreads.clear();
    }

    public void finishChargeFee(final AccountFeeLog log) {
        chargeFeeThreads.remove(log);
    }

    public AccountFeeLog getLastLog(final AccountFee fee) {
        final AccountFeeLogQuery query = new AccountFeeLogQuery();
        query.setAccountFee(fee);
        query.setUniqueResult();
        final List<AccountFeeLog> list = accountFeeLogDao.search(query);
        if (list.isEmpty()) {
            return null;
        } else {
            return list.iterator().next();
        }
    }

    public AccountFeeLog getNextExecution(AccountFee fee) {
        fee = fetchService.fetch(fee);
        AccountFeeLog nextExecution = fee.getNextExecution();
        if (nextExecution == null) {
            // Probably a new manual execution
            nextExecution = new AccountFeeLog();
            nextExecution.setAccountFee(fee);
            nextExecution.setDate(Calendar.getInstance());
            nextExecution.setAmount(fee.getAmount());
            nextExecution.setFreeBase(fee.getFreeBase());
            nextExecution.setStatus(AccountFeeLog.Status.NEVER_RAN);
            nextExecution.setTolerance(fee.getTolerance());
        }
        if (nextExecution != null && nextExecution.isTransient()) {
            // The next execution can be persisted, but only if the execution date is after the fee enabling date
            if (nextExecution.getDate().after(fee.getEnabledSince())) {
                nextExecution = accountFeeLogDao.insert(nextExecution);
            } else {
                // The fee is scheduled to start execution in the future
                return null;
            }
        }
        return nextExecution;
    }

    public long getThreadDelay() {
        return threadDelay;
    }

    public Collection<AccountFeeCharge> insertMissingChargesForPreviousPeriods(final MemberAccountStatus status, final Calendar date) {
        final MemberAccount account = (MemberAccount) fetchService.fetch(status.getAccount(), Account.Relationships.TYPE);

        final Collection<AccountFeeCharge> charges = new ArrayList<AccountFeeCharge>();

        // Find the account fee logs for the given account type that where not complete
        final AccountFeeLogQuery logQuery = new AccountFeeLogQuery();
        logQuery.fetch(AccountFeeLog.Relationships.ACCOUNT_FEE);
        logQuery.setAccountFeeEnabled(true);
        logQuery.setAccountType((MemberAccountType) account.getType());
        logQuery.setStatus(AccountFeeLog.Status.SCHEDULED);
        logQuery.setReturnScheduled(true);
        final List<AccountFeeLog> scheduledLogs = accountFeeLogDao.search(logQuery);
        for (final AccountFeeLog feeLog : scheduledLogs) {
            // Insert the charge
            final AccountFeeCharge charge = insertChargeForDate(status, feeLog, feeLog.getPeriod().getBegin());
            if (charge != null) {
                charges.add(charge);
            }
        }
        return charges;
    }

    public AccountFee load(final Long id, final Relationship... fetch) {
        return accountFeeDao.load(id, fetch);
    }

    public AccountFeeLog loadLog(final Long id, final Relationship... fetch) {
        return accountFeeLogDao.load(id, fetch);
    }

    public List<Member> nextMembersToCharge(final AccountFeeLog feeLog) {
        return memberAccountFeeLogDao.nextToCharge(feeLog, ACCOUNT_FEE_CHARGE_BATCH_SIZE);
    }

    public ChargeAccountFeeThread rechargeFailed(AccountFeeLog feeLog) {
        if (feeLog == null || feeLog.isTransient()) {
            throw new UnexpectedEntityException();
        }
        feeLog = fetchService.fetch(feeLog, RelationshipHelper.nested(AccountFeeLog.Relationships.ACCOUNT_FEE, AccountFee.Relationships.TRANSFER_TYPE));
        // Tests whether it is possible to recharge the account fee
        if (feeLog.getStatus() != AccountFeeLog.Status.PARTIALLY_FAILED && feeLog.getStatus() != AccountFeeLog.Status.CANCELED && feeLog.getStatus() != AccountFeeLog.Status.NEVER_RAN) {
            throw new UnexpectedEntityException();
        }
        // Tests whether the account fee log is already running
        if (chargeFeeThreads.containsKey(feeLog)) {
            throw new UnexpectedEntityException();
        }

        // Start charging the thread
        final ChargeFeeDTO dto = new ChargeFeeDTO();
        dto.setFee(feeLog.getAccountFee());
        dto.setFeeLog(feeLog);
        return startChargingThread(dto);
    }

    public int remove(final Long... ids) {
        for (final Long id : ids) {
            final AccountFee fee = load(id, AccountFee.Relationships.ACCOUNT_TYPE);
            cachedVolumeFeesByAccount.remove(fee.getAccountType());
        }
        return accountFeeDao.delete(ids);
    }

    public void resolveFailures() {
        failUnfinishedLogs();
        handleScheduledLogsInPast();
        insertMissingLogs();
    }

    public AccountFee save(final AccountFee accountFee) {
        validate(accountFee);

        // Set some attributes to null depending on others
        if (accountFee.getPaymentDirection() == PaymentDirection.TO_MEMBER) {
            // A to member fee never uses invoices
            accountFee.setInvoiceMode(null);
        }
        if (accountFee.getRunMode() == RunMode.MANUAL) {
            // A manual fee does not have recurrence
            accountFee.setRecurrence(null);
            accountFee.setDay(null);
            accountFee.setHour(null);
        }
        if (accountFee.getChargeMode() != ChargeMode.VOLUME_PERCENTAGE) {
            // Only positive volume can have a tolerance
            accountFee.setTolerance(null);
        }

        // Ensure the cache for volume fees for that account type is cleared
        cachedVolumeFeesByAccount.remove(accountFee.getAccountType());

        // Persist the account fee
        if (accountFee.isTransient()) {
            if (accountFee.isEnabled() && accountFee.getEnabledSince() == null) {
                accountFee.setEnabledSince(Calendar.getInstance());
            }
            return accountFeeDao.insert(accountFee);
        } else {
            final AccountFee current = load(accountFee.getId());
            // Correctly handle the enabled since
            if (accountFee.isEnabled() && current.getEnabledSince() == null) {
                // When was not previously enabled, initialize the enabled since
                if (accountFee.getEnabledSince() == null) {
                    accountFee.setEnabledSince(Calendar.getInstance());
                }
            } else if (!accountFee.isEnabled() && current.isEnabled()) {
                // When is disabling, set the date to null
                accountFee.setEnabledSince(null);
                // When disabling a volume account fee, discard all data for charges
                if (accountFee.getChargeMode().isVolume()) {
                    discardCharges(accountFee);
                }
            } else if (accountFee.getEnabledSince() == null) {
                // Just updating other fields - keep the enabled since
                accountFee.setEnabledSince(current.getEnabledSince());
            }
            return accountFeeDao.update(accountFee);
        }
    }

    public AccountFeeLog save(final AccountFeeLog accountFeeLog) {
        if (accountFeeLog.isTransient()) {
            return accountFeeLogDao.insert(accountFeeLog);
        } else {
            return accountFeeLogDao.update(accountFeeLog);
        }
    }

    public List<AccountFee> search(final AccountFeeQuery query) {
        return accountFeeDao.search(query);
    }

    public List<AccountFeeLog> searchLogs(final AccountFeeLogQuery query) {
        return accountFeeLogDao.search(query);
    }

    public void setAccountFeeChargeDao(final AccountFeeChargeDAO accountFeeChargeDao) {
        this.accountFeeChargeDao = accountFeeChargeDao;
    }

    public void setAccountFeeDao(final AccountFeeDAO dao) {
        accountFeeDao = dao;
    }

    public void setAccountFeeLogDao(final AccountFeeLogDAO accountFeeLogDao) {
        this.accountFeeLogDao = accountFeeLogDao;
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

    public void setApplicationService(final ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public MemberAccountFeeLog setChargingResult(final AccountFeeLog feeLog, final Member member, final Transfer transfer, final Invoice invoice) {
        final MemberAccountFeeLog mafl = new MemberAccountFeeLog();
        mafl.setDate(Calendar.getInstance());
        mafl.setAccountFeeLog(feeLog);
        mafl.setMember(member);
        mafl.setTransfer(transfer);
        mafl.setInvoice(invoice);
        return memberAccountFeeLogDao.insert(mafl, false);
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMemberAccountFeeLogDao(final MemberAccountFeeLogDAO memberAccountFeeLogDao) {
        this.memberAccountFeeLogDao = memberAccountFeeLogDao;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setThreadDelay(final long delay) {
        threadDelay = delay;
    }

    public void threadFinished(final ChargeAccountFeeThread thread) {
        // Removes a finished thread from the map
        final AccountFeeLog log = thread.getAccountFeeLog();
        chargeFeeThreads.remove(log);
    }

    public void validate(final AccountFee accountFee) {
        getValidator().validate(accountFee);
    }

    private AccountFeeCharge calculateAccountFeeCharge(final MemberAccountStatus status, final Calendar date, final AccountFee accountFee) {
        // Resolve the log that would be charged
        final AccountFeeLog accountFeeLog = resolveLogForDate(accountFee, date);
        return calculateAccountFeeCharge(status, date, accountFeeLog);
    }

    private AccountFeeCharge calculateAccountFeeCharge(final MemberAccountStatus status, final Calendar date, final AccountFeeLog accountFeeLog) {
        final AccountFee accountFee = accountFeeLog.getAccountFee();

        // We only want account fees that charge over volume
        final ChargeMode chargeMode = accountFee.getChargeMode();
        if (!chargeMode.isVolume()) {
            return null;
        }
        BigDecimal balance = status.getAvailableBalanceWithoutCreditLimit();
        balance = chargeMode.isNegative() ? balance.negate() : balance;

        // Check the free base
        final BigDecimal freeBase = BigDecimalHelper.nvl(accountFee.getFreeBase());
        if (balance.compareTo(freeBase) <= 0) {
            return null;
        }

        // For payments in past, when the account fee log was already charged, ignore this account fee. But don't ignore for running fees
        if (accountFeeLog.getStatus() != AccountFeeLog.Status.SCHEDULED && accountFeeLog.getStatus() != AccountFeeLog.Status.RUNNING) {
            return null;
        }

        final Period feePeriod = accountFeeLog.getPeriod();
        final Calendar beginDate = status.getDate().before(feePeriod.getBegin()) ? feePeriod.getBegin() : status.getDate();
        final Period period = Period.between(beginDate, date == null ? Calendar.getInstance() : date);
        final BigDecimal seconds = new BigDecimal(period.getDifference());

        // If the account status was enabled after the charged period start, ignore this charge
        final Calendar accountStatusEnabledSince = applicationService.getAccountStatusEnabledSince();
        if (accountStatusEnabledSince.after(accountFeeLog.getPeriod().getBegin())) {
            return null;
        }

        // If the account fee was enabled after the charged period start, ignore this charge
        final Calendar accountFeeEnabledSince = accountFee.getEnabledSince();
        if (accountFeeEnabledSince == null || accountFeeEnabledSince.after(accountFeeLog.getPeriod().getBegin())) {
            return null;
        }

        // Calculate the volume
        final BigDecimal totalSeconds = new BigDecimal(feePeriod.getDifference());
        final BigDecimal chargedVolume = balance.subtract(freeBase).multiply(seconds).divide(totalSeconds, ACCOUNT_FEE_MATH_CONTEXT);
        final BigDecimal chargedAmount = accountFeeLog.getAmountValue().apply(chargedVolume);

        // Build the account fee charge
        if (chargedAmount.compareTo(MINIMUM_ACCOUNT_FEE_PAYMENT) >= 0) {
            final AccountFeeCharge charge = new AccountFeeCharge();
            charge.setAccount((MemberAccount) fetchService.fetch(status.getAccount()));
            charge.setAccountFeeLog(accountFeeLog);
            charge.setPeriod(period);
            charge.setAvailableBalance(balance);
            charge.setAmount(chargedAmount);
            return charge;
        }
        return null;
    }

    /**
     * Invoked when disabling a previously enabled volume account fee. Should delete all {@link AccountFeeCharge}s for any existing scheduled
     * {@link AccountFeeLog}'s and insert a new account status subtracting the previously reserved amount
     */
    private void discardCharges(final AccountFee accountFee) {
        final AccountFeeLogQuery query = new AccountFeeLogQuery();
        query.setAccountFee(accountFee);
        query.setStatus(AccountFeeLog.Status.SCHEDULED);
        query.setReturnScheduled(true);
        final List<AccountFeeLog> logs = accountFeeLogDao.search(query);
        for (final AccountFeeLog log : logs) {
            final Iterator<MemberAccount> accounts = accountFeeLogDao.iterateOverAccountsWithAccountFeeChargesFor(log);
            while (accounts.hasNext()) {
                final MemberAccount account = accounts.next();
                final BigDecimal amount = accountFeeChargeDao.totalAmoutForPeriod(account, log);
                accountFeeChargeDao.deleteOnPeriod(account, log);
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    accountStatusHandler.processAccountFeeDisabled(account, amount);
                }
            }
            DataIteratorHelper.close(accounts);
            accountService.removeStatusRelatedTo(log);
            accountFeeLogDao.delete(false, log.getId());
        }
    }

    private ChargeAccountFeeThread doCharge(final AccountFee fee) {
        // Start charging the thread
        final ChargeFeeDTO dto = new ChargeFeeDTO();
        dto.setFee(fee);
        return startChargingThread(dto);
    }

    /**
     * Marks all running logs as failed
     */
    private void failUnfinishedLogs() {
        final AccountFeeLogQuery query = new AccountFeeLogQuery();
        query.fetch(AccountFeeLog.Relationships.ACCOUNT_FEE);
        query.setStatus(AccountFeeLog.Status.RUNNING);
        final List<AccountFeeLog> failedLogs = accountFeeLogDao.search(query);
        for (final AccountFeeLog log : failedLogs) {
            log.setStatus(AccountFeeLog.Status.PARTIALLY_FAILED);
            accountFeeLogDao.update(log);
            alertService.create(SystemAlert.Alerts.ACCOUNT_FEE_FAILED, log.getAccountFee().getName());
        }
    }

    /**
     * Returns the missing periods for an account fee
     */
    private List<Period> getMissingPeriods(final AccountFee fee) {
        final TimePeriod recurrence = fee.getRecurrence();
        Calendar since;
        final Calendar now = DateUtils.truncate(Calendar.getInstance(), Calendar.HOUR_OF_DAY);

        // Determine since when the fee should have run
        final AccountFeeLog lastLog = getLastLog(fee);
        if (lastLog == null || lastLog.getDate().before(fee.getEnabledSince())) {
            // May be 2 cases: Either the fee never ran or was re-enabled after the last run
            since = fee.getEnabledSince();
        } else {
            // The fee ran and is enabled, just has missing logs
            since = lastLog.getDate();
        }

        // Resolve the periods
        final List<Period> periods = new ArrayList<Period>();
        Calendar date = DateHelper.truncate(since);
        Period period = recurrence.previousPeriod(date);
        while (true) {
            date = (Calendar) period.getEnd().clone();
            date.add(Calendar.SECOND, 1);
            period = recurrence.periodStartingAt(date);
            if (period.getEnd().before(now)) {
                periods.add(period);
            } else {
                break;
            }
        }

        // Check if the last one should be really there
        if (!periods.isEmpty()) {
            // Do not use the last period if the listener has not run yet
            final byte thisDay = (byte) now.get(Calendar.DAY_OF_MONTH);
            final byte thisHour = (byte) now.get(Calendar.HOUR_OF_DAY);
            boolean removeLast = false;

            final Byte feeDay = fee.getDay();
            if (feeDay != null && thisDay < feeDay) {
                removeLast = true;
            } else if (feeDay == null || thisDay == feeDay) {
                removeLast = thisHour < fee.getHour();
            }
            if (removeLast) {
                periods.remove(periods.size() - 1);
            }
        }

        // Check if any of those logs are present
        final Iterator<Period> it = periods.iterator();
        while (it.hasNext()) {
            final Period current = it.next();
            final AccountFeeLogQuery logQuery = new AccountFeeLogQuery();
            logQuery.setPageForCount();
            logQuery.setAccountFee(fee);
            logQuery.setPeriodStartAt(current.getBegin());
            final int count = PageHelper.getTotalCount(accountFeeLogDao.search(logQuery));
            if (count > 0) {
                it.remove();
            }
        }
        return periods;
    }

    private Validator getValidator() {
        final Validator validator = new Validator("accountFee");
        validator.property("accountType").required();
        validator.property("transferType").required();
        validator.property("name").required().maxLength(100);
        validator.property("description").maxLength(1000);
        validator.property("amount").required().positiveNonZero();
        validator.property("chargeMode").required(); // .add(new When(PaymentDirection.TO_MEMBER, new
        // NoneOfValidation(ChargeMode.VOLUME_PERCENTAGE,
        // ChargeMode.NEGATIVE_VOLUME_PERCENTAGE)));
        validator.property("paymentDirection").required();
        validator.property("runMode").add(new When(Arrays.asList(ChargeMode.VOLUME_PERCENTAGE, ChargeMode.NEGATIVE_VOLUME_PERCENTAGE), new AnyOfValidation(RunMode.SCHEDULED))).required();
        validator.property("recurrence.number").key("accountFee.recurrence").add(new When(RunMode.SCHEDULED, RequiredValidation.instance())).add(new When(RunMode.SCHEDULED, PositiveNonZeroValidation.instance())).add(new When(RunMode.SCHEDULED, CompareToValidation.lessEquals(28)));
        validator.property("recurrence.field").key("accountFee.recurrence").add(new When(RunMode.SCHEDULED, RequiredValidation.instance())).add(new When(RunMode.SCHEDULED, new AnyOfValidation(TimePeriod.Field.DAYS, TimePeriod.Field.WEEKS, TimePeriod.Field.MONTHS)));
        validator.property("day").between(1, 28).add(new When(RunMode.SCHEDULED, new DayValidation()));
        validator.property("hour").between(0, 23).add(new When(RunMode.SCHEDULED, RequiredValidation.instance()));
        validator.property("invoiceMode").add(new When(PaymentDirection.TO_SYSTEM, RequiredValidation.instance()));
        return validator;
    }

    private List<AccountFee> getVolumeFees(final MemberAccountType accountType) {
        List<AccountFee> cachedFees = cachedVolumeFeesByAccount.get(accountType);
        if (cachedFees == null) {
            final AccountFeeQuery query = new AccountFeeQuery();
            query.setAccountType(accountType);
            query.setType(RunMode.SCHEDULED);
            cachedFees = search(query);
            for (final Iterator<AccountFee> iterator = cachedFees.iterator(); iterator.hasNext();) {
                final AccountFee accountFee = iterator.next();
                if (!accountFee.getChargeMode().isVolume()) {
                    iterator.remove();
                }
            }
            cachedVolumeFeesByAccount.put(accountType, cachedFees);
        }
        return cachedFees;
    }

    /**
     * Marks as NEVER_RUN all logs that are scheduled in past
     */
    private void handleScheduledLogsInPast() {
        final Calendar threshold = Calendar.getInstance();
        threshold.add(Calendar.HOUR_OF_DAY, 1);

        final AccountFeeLogQuery query = new AccountFeeLogQuery();
        query.setStatus(AccountFeeLog.Status.SCHEDULED);
        for (final AccountFeeLog log : accountFeeLogDao.search(query)) {
            if (log.getDate().before(threshold)) {
                log.setStatus(AccountFeeLog.Status.NEVER_RAN);
                accountFeeLogDao.update(log);
            }
        }
    }

    private AccountFeeCharge insertChargeForDate(final MemberAccountStatus status, final AccountFeeLog feeLog, final Calendar date) {
        if (!feeLog.getAccountFee().getChargeMode().isVolume()) {
            return null;
        }
        final MemberAccount account = (MemberAccount) status.getAccount();
        try {
            // Check whether the account fee charge for the period end exists
            accountFeeChargeDao.forData(account, feeLog, date);
        } catch (final EntityNotFoundException e) {
            // The charge doesn't exist. Calculate the volume
            AccountFeeCharge charge = calculateAccountFeeCharge(status, date, feeLog);
            if (charge == null) {
                // If there would be no charge, create a new one with amount = 0
                charge = new AccountFeeCharge();
                charge.setPeriod(Period.exact(date));
                charge.setAccount(account);
                charge.setAccountFeeLog(feeLog);
                charge.setAvailableBalance(status.getAvailableBalanceWithoutCreditLimit());
                charge.setAmount(BigDecimal.ZERO);
            }
            // Insert the charge
            return accountFeeChargeDao.insert(charge);
        }
        return null;
    }

    private void insertMissingLogs() {
        final AccountFeeQuery query = new AccountFeeQuery();
        query.setReturnDisabled(false);
        query.setType(RunMode.SCHEDULED);
        final List<AccountFee> accountFees = accountFeeDao.search(query);
        for (final AccountFee fee : accountFees) {
            final Field recurrenceField = fee.getRecurrence().getField();
            final List<Period> missingPeriods = getMissingPeriods(fee);
            if (!missingPeriods.isEmpty()) {
                for (final Period period : missingPeriods) {
                    final Calendar shouldHaveChargedAt = DateHelper.truncate(period.getEnd());
                    shouldHaveChargedAt.add(Calendar.DAY_OF_MONTH, 1);
                    switch (recurrenceField) {
                        case WEEKS:
                            // Go to the day it should have been charged
                            int max = 7;
                            while (max > 0 && shouldHaveChargedAt.get(Calendar.DAY_OF_WEEK) < fee.getDay()) {
                                shouldHaveChargedAt.add(Calendar.DAY_OF_MONTH, 1);
                                max--;
                            }
                            break;
                        case MONTHS:
                            shouldHaveChargedAt.set(Calendar.DAY_OF_MONTH, fee.getDay());
                            break;
                    }
                    shouldHaveChargedAt.set(Calendar.HOUR_OF_DAY, fee.getHour());
                    final AccountFeeLog log = new AccountFeeLog();
                    log.setAccountFee(fee);
                    log.setDate(shouldHaveChargedAt);
                    log.setPeriod(period);
                    log.setAmount(fee.getAmount());
                    log.setFreeBase(fee.getFreeBase());
                    log.setTolerance(fee.getTolerance());
                    log.setStatus(AccountFeeLog.Status.NEVER_RAN);
                    accountFeeLogDao.insert(log);
                }
                alertService.create(SystemAlert.Alerts.ACCOUNT_FEE_RECOVERED, fee.getName());
            }
        }
    }

    private synchronized AccountFeeLog resolveLogForDate(AccountFee accountFee, final Calendar date) {
        try {
            return accountFeeLogDao.forDate(accountFee, date);
        } catch (final EntityNotFoundException e) {
            // When there's no log already, we must create one
            accountFee = fetchService.fetch(accountFee, AccountFee.Relationships.LOGS);
            return accountFeeLogDao.insert(accountFee.calculateNextExecution());
        }
    }

    private ChargeAccountFeeThread startChargingThread(final ChargeFeeDTO dto) {
        final ChargeAccountFeeThread thread = (ChargeAccountFeeThread) beanFactory.getBean("chargeAccountFeeThread");
        thread.setDelay(threadDelay);
        thread.addListener(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        if (thread.charge(dto)) {
            // Charging is running
            final AccountFeeLog feeLog = dto.getFeeLog();
            chargeFeeThreads.put(feeLog, thread);
            return thread;
        } else {
            // Wasn't actually charged
            return null;
        }
    }

}
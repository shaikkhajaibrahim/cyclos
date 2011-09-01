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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.accounts.AccountDAO;
import nl.strohalm.cyclos.dao.accounts.AccountStatusDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.SimpleTransferVO;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountQuery;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberQuery;
import nl.strohalm.cyclos.entities.members.MemberTransactionDetailsReportData;
import nl.strohalm.cyclos.entities.members.MemberTransactionSummaryReportData;
import nl.strohalm.cyclos.entities.members.MemberTransactionSummaryVO;
import nl.strohalm.cyclos.entities.members.MembersTransactionsReportParameters;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings.MemberResultDisplay;
import nl.strohalm.cyclos.services.accounts.CreditLimitDTO.Entry;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.CombinedIterator;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.SerializationUtils;

/**
 * Account service implementation
 * @author luis
 */
public class AccountServiceImpl implements AccountService {

    /**
     * A combined iterator which iterates members and the combination of payment filters x debits x credits
     * 
     * @author luis
     */
    private class MembersTransactionsSummaryIterator extends CombinedIterator<MemberTransactionSummaryReportData, Member, MemberTransactionSummaryVO, TransactionSummaryReportKey> {
        private final MembersTransactionsReportParameters params;
        private final List<Boolean>                       creditOrDebitToQuery;

        private MembersTransactionsSummaryIterator(final Iterator<Member> masterIterator, final MembersTransactionsReportParameters params) {
            super(masterIterator);
            this.params = params;

            // Check whether to get credits / debits
            creditOrDebitToQuery = new ArrayList<Boolean>();
            if (params.isCredits()) {
                creditOrDebitToQuery.add(true);
            }
            if (params.isDebits()) {
                creditOrDebitToQuery.add(false);
            }
        }

        @Override
        protected boolean belongsToMasterElement(final Member member, final TransactionSummaryReportKey key, final MemberTransactionSummaryVO vo) {
            return vo.getMemberId().equals(member.getId());
        }

        @Override
        protected MemberTransactionSummaryReportData combine(final Member member, final Map<TransactionSummaryReportKey, MemberTransactionSummaryVO> elements) {
            final MemberTransactionSummaryReportData data = new MemberTransactionSummaryReportData();
            data.setMember(member);
            for (final Map.Entry<TransactionSummaryReportKey, MemberTransactionSummaryVO> entry : elements.entrySet()) {
                final TransactionSummaryReportKey key = entry.getKey();
                final MemberTransactionSummaryVO transactions = entry.getValue();
                if (key.credits) {
                    data.addCredits(key.paymentFilter, transactions);
                } else {
                    data.addDebits(key.paymentFilter, transactions);
                }
            }
            return data;
        }

        @Override
        protected void registerInnerIterators() {
            final Collection<PaymentFilter> paymentFilters = params.getPaymentFilters();
            final MemberResultDisplay memberDisplay = settingsService.getLocalSettings().getMemberResultDisplay();
            for (final PaymentFilter paymentFilter : paymentFilters) {
                for (final Boolean isCredit : creditOrDebitToQuery) {
                    final Iterator<MemberTransactionSummaryVO> iterator = accountDao.membersTransactionSummaryReport(params.getMemberGroups(), paymentFilter, params.getPeriod(), isCredit, memberDisplay);
                    final TransactionSummaryReportKey key = new TransactionSummaryReportKey(paymentFilter, isCredit);
                    registerInnerIterator(key, iterator);
                }
            }
        }
    }

    private static class TransactionSummaryReportKey {
        private PaymentFilter paymentFilter;
        private boolean       credits;

        public TransactionSummaryReportKey(final PaymentFilter paymentFilter, final boolean credits) {
            this.paymentFilter = paymentFilter;
            this.credits = credits;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            final TransactionSummaryReportKey other = (TransactionSummaryReportKey) obj;
            return ObjectUtils.equals(paymentFilter, other.paymentFilter) && credits == other.credits;
        }

        @Override
        public int hashCode() {
            return paymentFilter.hashCode() * (credits ? 1 : -1);
        }
    }

    private static final float PRECISION_DELTA = 0.0001F;

    /**
     * Returns the balance that should be used to calculate the volume, given a freebase and a flag indicating if positive or negative volume is being
     * calculated
     */
    private static BigDecimal applyFreeBase(final BigDecimal balance, final BigDecimal freeBase, final boolean isPositive) {
        BigDecimal currentBalance = balance;
        if (isPositive) {
            currentBalance = currentBalance.subtract(freeBase);
            if (currentBalance.compareTo(BigDecimal.ZERO) < 0) {
                currentBalance = BigDecimal.ZERO;
            }
        } else {
            currentBalance = currentBalance.add(freeBase);
            if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                currentBalance = BigDecimal.ZERO;
            }
        }
        return currentBalance;
    }

    /**
     * Apply the tolerance period, removing parts from volume that would be inside the tolerance period
     */
    @SuppressWarnings("unchecked")
    private static List<SimpleTransferVO> applyTolerance(List<SimpleTransferVO> vos, final TimePeriod tolerance) {
        // Create a defensive copy of vos, because the references are changed
        Serializable serializable;
        try {
            serializable = (Serializable) vos;
        } catch (final ClassCastException e) {
            // If the list is not serializable, use a serializable one
            serializable = new ArrayList<SimpleTransferVO>(vos);
        }
        vos = (List<SimpleTransferVO>) SerializationUtils.clone(serializable);

        // Calculate the result
        final List<SimpleTransferVO> result = new ArrayList<SimpleTransferVO>(vos.size());
        for (int i = 0, size = vos.size(); i < size; i++) {
            final SimpleTransferVO vo = vos.get(i);
            final BigDecimal amount = vo.getAmount();
            boolean useCurrent = true;
            if (amount.compareTo(BigDecimal.ZERO) < 0) {
                // Apply the tolerance
                for (int j = result.size() - 1; j >= 0; j--) {
                    final SimpleTransferVO previous = result.get(j);
                    BigDecimal previousAmount = previous.getAmount();
                    // Stop loop when finds another debit
                    if (previousAmount.compareTo(BigDecimal.ZERO) < 0) {
                        break;
                    }
                    // Also stop when tolerance period is exhausted
                    final Calendar applyUpTo = tolerance.remove(vo.getDate());
                    if (applyUpTo.after(previous.getDate())) {
                        break;
                    }
                    final BigDecimal toCrop = amount.negate().compareTo(previousAmount) > 0 ? previousAmount : amount.negate();
                    final BigDecimal remainingAmount = amount.add(toCrop);

                    // Crop the exceeding bar
                    previousAmount = previousAmount.subtract(toCrop);
                    if (previousAmount.abs().floatValue() > PRECISION_DELTA) {
                        // If there's amount left, set it
                        previous.setAmount(previousAmount);
                    } else {
                        // Otherwise, remove from list
                        result.remove(previous);
                    }
                    // Don't use this vo on the computation if there's no amount left
                    if (remainingAmount.abs().floatValue() < PRECISION_DELTA) {
                        useCurrent = false;
                    } else {
                        vo.setAmount(remainingAmount);
                    }
                }
            }
            if (useCurrent) {
                result.add(vo);
            }
        }
        return result;
    }

    /**
     * Performs the average transactioned volume calculation
     */
    private static BigDecimal calculateVolume(final TransactionVolumeDTO dto, final BigDecimal initialBalance, List<SimpleTransferVO> transfers, final LocalSettings localSettings) {
        // Retrieve the parameters
        final Period calculationPeriod = dto.getPeriod();
        final TimePeriod tolerance = dto.getTolerance();
        Calendar currentDate = calculationPeriod.getBegin();
        final BigDecimal freeBase = dto.getFreeBase().abs();
        final boolean isPositive = dto.isPositiveVolume();
        BigDecimal volume = BigDecimal.ZERO;
        long totalSeconds = 0;
        BigDecimal lastBalance = initialBalance;

        // Apply the tolerance period
        if (tolerance != null && tolerance.getNumber() > 0) {
            transfers = applyTolerance(transfers, tolerance);
        }

        // Calculate each transfer
        for (int i = 0, size = transfers.size(); i < size; i++) {
            final SimpleTransferVO vo = transfers.get(i);
            final Period period = Period.between(currentDate, vo.getDate());
            final long diff = period.getDifference();
            final BigDecimal currentBalance = applyFreeBase(lastBalance, freeBase, isPositive);
            volume = volume.add(currentBalance.multiply(new BigDecimal(diff)));
            currentDate = vo.getDate();
            final BigDecimal amount = localSettings.round(vo.getAmount());
            lastBalance = lastBalance.add(amount);
            totalSeconds += diff;
        }

        // Apply the period end
        final Period period = Period.between(currentDate, calculationPeriod.getEnd());
        final BigDecimal currentBalance = applyFreeBase(lastBalance, freeBase, isPositive);
        final long diff = period.getDifference();
        volume = volume.add(currentBalance.multiply(new BigDecimal(diff)));
        totalSeconds += diff;
        // Calculate the total value
        final MathContext mathContext = localSettings.getMathContext();
        return volume.divide(new BigDecimal(totalSeconds), mathContext);
    }

    private AccountDAO           accountDao;
    private AccountStatusDAO     accountStatusDao;
    private TransferDAO          transferDao;
    private SettingsService      settingsService;
    private FetchService         fetchService;
    private AccountStatusHandler accountStatusHandler;
    private AccountTypeService   accountTypeService;
    private GroupService         groupService;
    private ElementService       elementService;

    public BigDecimal calculateTransactionedVolume(final TransactionVolumeDTO dto) {
        Period period = dto.getPeriod();
        if (period == null || period.getBegin() == null || period.getEnd() == null) {
            throw new IllegalArgumentException("Must have a period");
        }
        period = period.clone().useTime();
        final Account account = getAccount(new AccountDTO(dto.getAccountOwner(), dto.getAccountType()));

        // Get the initial balance
        final Calendar balanceDate = (Calendar) period.getBegin().clone();
        final BigDecimal initialBalance = getBalance(new GetTransactionsDTO(account, balanceDate));

        // Retrieve the payments
        final List<SimpleTransferVO> transfers = transferDao.paymentVOs(account, period);

        // Calculate the volume
        final BigDecimal volume = calculateVolume(dto, initialBalance, transfers, settingsService.getLocalSettings());
        return volume;
    }

    public Account getAccount(final AccountDTO params, final Relationship... fetch) {
        final AccountOwner owner = params.getOwner();
        final AccountType type = params.getType();

        if (LoggedUser.isValid() && LoggedUser.isAdministrator() && (owner instanceof SystemAccountOwner)) {
            // For administrator viewing system accounts, ensure return only the types he can view information about
            AdminGroup group = LoggedUser.group();
            group = fetchService.fetch(group, AdminGroup.Relationships.VIEW_INFORMATION_OF);
            for (final SystemAccountType current : group.getViewInformationOf()) {
                if (current.equals(type)) {
                    return fetchService.fetch(current.getAccount(), fetch);
                }
            }
            throw new EntityNotFoundException(SystemAccount.class);
        }

        return accountDao.load(owner, type, fetch);
    }

    public List<? extends Account> getAccounts(final AccountOwner owner, final Relationship... fetch) {
        return getAccounts(owner, false, fetch);
    }

    /**
     * gets a Set with accounts belonging to the allowedTTs AND to the member
     * @param member the members whose accounts are checked on this
     * @param allowedTTs the transfer types to be checked
     * @param direction a TransferType.Direction enum.
     * <ul>
     * <li>If FROM, only accounts from which the checked transfer types come from are included.
     * <li>If TO, only accounts to which the checked transfer types go are included.
     * <li>If BOTH, both from and to accounts of the transfer types are included.
     * @return a Set with accounts belonging to the member, and containing the transfer types in allowedTTs.
     */
    @SuppressWarnings("unchecked")
    public Set<? extends Account> getAccountsFromTTs(final Member member, final Collection<TransferType> allowedTTs, final TransferType.Direction direction) {
        final Set<MemberAccount> allowedAccounts = new HashSet<MemberAccount>(allowedTTs.size());
        final List<MemberAccount> accounts = (List<MemberAccount>) getAccounts(member);
        for (final TransferType currentTT : allowedTTs) {
            for (final MemberAccount currentAccount : accounts) {
                if (direction.equals(TransferType.Direction.BOTH)) {
                    if (currentAccount.getType().equals(currentTT.getFrom()) || (currentAccount.getType().equals(currentTT.getTo()))) {
                        allowedAccounts.add(currentAccount);
                    }
                } else if (direction.equals(TransferType.Direction.FROM) && currentAccount.getType().equals(currentTT.getFrom())) {
                    allowedAccounts.add(currentAccount);
                } else if (direction.equals(TransferType.Direction.TO) && currentAccount.getType().equals(currentTT.getTo())) {
                    allowedAccounts.add(currentAccount);
                }
            }
        }
        return allowedAccounts;
    }

    /**
     * Filters the account types the logged user may see. Returning null means any type
     */
    public Collection<? extends AccountType> getAllowedTypes(final AccountOwner owner) {
        Collection<? extends AccountType> allowedTypes;
        if (LoggedUser.isValid()) {
            // There is a logged user
            if (LoggedUser.isAdministrator()) {
                AdminGroup group = LoggedUser.group();
                if (owner instanceof SystemAccountOwner) {
                    // For administrator viewing system accounts, ensure return only the types he can view information about
                    group = fetchService.fetch(group, AdminGroup.Relationships.VIEW_INFORMATION_OF);
                    allowedTypes = group.getViewInformationOf();
                } else {
                    // Admin viewing a member account
                    group = fetchService.fetch(group, AdminGroup.Relationships.MANAGES_GROUPS);
                    if (group.getManagesGroups().contains(((Member) owner).getGroup())) {
                        // A member he manages. Can see all accounts
                        allowedTypes = null;
                    } else {
                        // A member he does not manage. No accounts visible
                        allowedTypes = Collections.emptySet();
                    }
                }
            } else if (LoggedUser.isMember()) {
                if (LoggedUser.element().equals(owner)) {
                    // A member viewing his
                    allowedTypes = null;
                } else if (LoggedUser.isBroker() && (owner instanceof Member) && LoggedUser.element().equals(((Member) owner).getBroker())) {
                    // A broker viewing his member's accounts
                    allowedTypes = null;
                } else {
                    // A member can't see accounts from others
                    allowedTypes = Collections.emptySet();
                }
            } else if (LoggedUser.isOperator()) {
                OperatorGroup group = LoggedUser.group();
                if (group.getMember().equals(owner)) {
                    // Operator viewing his member's accounts
                    group = fetchService.fetch(group, OperatorGroup.Relationships.CAN_VIEW_INFORMATION_OF);
                    allowedTypes = group.getCanViewInformationOf();
                } else {
                    // No other owner is allowed
                    allowedTypes = Collections.emptySet();
                }
            } else {
                // Who's logged in?!?
                allowedTypes = Collections.emptySet();
            }
        } else {
            allowedTypes = Collections.emptySet();
        }
        return allowedTypes;
    }

    public BigDecimal getBalance(final GetTransactionsDTO params) {
        return getStatus(params).getBalance();
    }

    public TransactionSummaryVO getBrokerCommissions(final GetTransactionsDTO params) {
        return accountDao.getBrokerCommissions(params);
    }

    public BigDecimal getCreditLimit(final AccountDTO params) {
        final Account account = getAccount(params);
        return account.getCreditLimit();
    }

    public CreditLimitDTO getCreditLimits(final Member owner) {
        final Map<AccountType, BigDecimal> limits = new HashMap<AccountType, BigDecimal>();
        final Map<AccountType, BigDecimal> upperLimits = new HashMap<AccountType, BigDecimal>();
        final List<? extends Account> accts = getAccounts(owner, true);
        for (final Account acct : accts) {
            final AccountType type = acct.getType();
            limits.put(type, acct.getCreditLimit());
            upperLimits.put(type, acct.getUpperCreditLimit());
        }
        final CreditLimitDTO dto = new CreditLimitDTO();
        dto.setLimitPerType(limits);
        dto.setUpperLimitPerType(upperLimits);
        return dto;
    }

    public Account getDefaultAccountFromList(Member member, final List<Account> allowedAccounts) {
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        // check if the default account is amongst this
        final MemberAccountType defaultType = accountTypeService.getDefault(member.getMemberGroup());
        for (final Account currentAccount : allowedAccounts) {
            if (currentAccount.getType().equals(defaultType)) {
                // Found the default account: return the DTO based on it
                return currentAccount;
            }
        }
        // if no default account, just take the first
        if (allowedAccounts.size() > 0) {
            return allowedAccounts.get(0);
        }
        // No accounts: return null
        return null;
    }

    public AccountStatus getStatus(final GetTransactionsDTO params) {
        return getStatus(params, true);
    }

    public AccountStatus getStatus(final GetTransactionsDTO params, final boolean processCurrentStatus) {
        final Account account = getAccount(params);
        final Period period = params.getPeriod();
        Calendar date;
        if (period == null) {
            date = null;
        } else if (period.getEnd() != null) {
            if (period.isUseTime()) {
                // When using explicit time, leave the end date as is
                date = period.getEnd();
            } else {
                // As the getStatus uses date <= :param, we should subtract 1 milli to ensure it's on the previous day
                date = DateHelper.truncateNextDay(period.getEnd());
                date.add(Calendar.MILLISECOND, -1);
            }
        } else {
            date = period.isUseTime() ? period.getBegin() : DateHelper.truncate(period.getBegin());
        }
        return accountStatusHandler.getStatus(account, date, processCurrentStatus, false);
    }

    public Map<PaymentFilter, TransactionSummaryVO> getTransactionsSummary(final Member member, final AccountType accountType, final Period period, final Collection<PaymentFilter> paymentFilters, final boolean credits) {
        final Map<PaymentFilter, TransactionSummaryVO> summary = new HashMap<PaymentFilter, TransactionSummaryVO>();
        final GetTransactionsDTO params = new GetTransactionsDTO();
        params.setOwner(member);
        params.setType(accountType);
        params.setPeriod(period);
        for (final PaymentFilter paymentFilter : paymentFilters) {
            params.setPaymentFilter(paymentFilter);
            TransactionSummaryVO vo;
            if (credits) {
                vo = accountDao.getCredits(params);
            } else {
                vo = accountDao.getDebits(params);
            }
            summary.put(paymentFilter, vo);
        }
        return summary;
    }

    public void lock(final boolean forWrite, final List<Account> accounts) {
        accountDao.lock(forWrite, accounts);
    }

    public void lockAccounts(final boolean forWrite, final List<AccountDTO> dtos) {
        accountDao.lockAccounts(forWrite, dtos);
    }

    @SuppressWarnings("unchecked")
    public Iterator<MemberTransactionDetailsReportData> membersTransactionsDetailsReport(final MembersTransactionsReportParameters params) {
        // Ensure the parameters are valid
        if (!isValid(params)) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        return accountDao.membersTransactionsDetailsReport(params);
    }

    @SuppressWarnings("unchecked")
    public Iterator<MemberTransactionSummaryReportData> membersTransactionsSummaryReport(final MembersTransactionsReportParameters params) {

        // Ensure the parameters are valid
        if (!isValid(params)) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        // Retrieve the members
        final Iterator<Member> membersIterator = resolveMembersForTransactionsReport(params);

        return new MembersTransactionsSummaryIterator(membersIterator, params);
    }

    public void removeStatusRelatedTo(final AccountFeeLog log) {
        accountStatusDao.removeStatusRelatedTo(log);
    }

    public void setAccountDao(final AccountDAO accountDao) {
        this.accountDao = accountDao;
    }

    public void setAccountStatusDao(final AccountStatusDAO accountStatusDao) {
        this.accountStatusDao = accountStatusDao;
    }

    public void setAccountStatusHandler(final AccountStatusHandler accountStatusHandler) {
        this.accountStatusHandler = accountStatusHandler;
    }

    public void setAccountTypeService(final AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    public void setCreditLimit(final Member owner, final CreditLimitDTO limits) {
        validate(owner, limits);

        Map<? extends AccountType, BigDecimal> limitPerType = limits.getLimitPerType();
        final Map<AccountType, BigDecimal> newLimitPerType = new HashMap<AccountType, BigDecimal>();
        if (limitPerType != null) {
            for (AccountType accountType : limitPerType.keySet()) {
                final BigDecimal limit = limitPerType.get(accountType);
                accountType = fetchService.fetch(accountType);
                newLimitPerType.put(accountType, limit);
            }
        }
        limitPerType = newLimitPerType;
        limits.setLimitPerType(limitPerType);

        Map<? extends AccountType, BigDecimal> upperLimitPerType = limits.getUpperLimitPerType();
        final Map<AccountType, BigDecimal> newUpperLimitPerType = new HashMap<AccountType, BigDecimal>();
        if (upperLimitPerType != null) {
            for (AccountType accountType : upperLimitPerType.keySet()) {
                final BigDecimal limit = upperLimitPerType.get(accountType);
                accountType = fetchService.fetch(accountType);
                newUpperLimitPerType.put(accountType, limit);
            }
        }
        upperLimitPerType = newUpperLimitPerType;
        limits.setUpperLimitPerType(upperLimitPerType);

        final List<Entry> entries = limits.getEntries();
        for (final Entry entry : entries) {
            final AccountType type = entry.getAccountType();
            final BigDecimal limit = entry.getCreditLimit();
            final BigDecimal upperLimit = entry.getUpperCreditLimit();
            if (limit == null && upperLimit == null) {
                continue;
            }
            List<? extends Account> accts;
            if (owner == null) {
                accts = getAccounts(type);
            } else {
                accts = Arrays.asList(getAccount(new AccountDTO(owner, type)));
            }
            for (Account account : accts) {

                boolean limitHasChanged = false;

                if (limit != null && !account.getCreditLimit().equals(limit.abs())) {
                    account.setCreditLimit(limit.abs());
                    limitHasChanged = true;
                }
                if (upperLimit != null && (account.getUpperCreditLimit() == null || !account.getUpperCreditLimit().equals(upperLimit.abs()))) {
                    account.setUpperCreditLimit(upperLimit.abs());
                    limitHasChanged = true;
                }

                if (limitHasChanged) {
                    // Update the account
                    account = accountDao.update(account);
                    accountStatusHandler.processLimitChange(account);
                }
            }
        }
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransferDao(final TransferDAO transferDao) {
        this.transferDao = transferDao;
    }

    public void validate(Member member, final CreditLimitDTO creditLimit) {
        // Fetch the member
        try {
            member = fetchService.fetch(member);
        } catch (final Exception e) {
            throw new ValidationException();
        }

        // Retrieve all given account types
        final Map<? extends AccountType, BigDecimal> limitPerType = creditLimit.getLimitPerType();
        final Map<? extends AccountType, BigDecimal> upperLimitPerType = creditLimit.getUpperLimitPerType();
        final Set<AccountType> accountTypes = new HashSet<AccountType>();
        if (limitPerType != null) {
            for (final AccountType at : limitPerType.keySet()) {
                accountTypes.add(at);
            }
        }
        if (upperLimitPerType != null) {
            for (final AccountType at : upperLimitPerType.keySet()) {
                accountTypes.add(at);
            }
        }

        // Check if the member has all account types
        for (final AccountType type : accountTypes) {
            try {
                getAccount(new AccountDTO(member, type));
            } catch (final EntityNotFoundException e) {
                throw new ValidationException();
            }
        }
    }

    private List<? extends Account> getAccounts(final AccountOwner owner, final boolean forceAllAccounts, final Relationship... fetch) {
        if (LoggedUser.isValid() && LoggedUser.isAdministrator() && (owner instanceof SystemAccountOwner)) {
            // For administrator viewing system accounts, ensure return only the types he can view information about
            AdminGroup group = LoggedUser.group();
            group = fetchService.fetch(group, AdminGroup.Relationships.VIEW_INFORMATION_OF);
            final Collection<SystemAccountType> accountTypes = group.getViewInformationOf();
            final Collection<SystemAccount> accounts = new ArrayList<SystemAccount>(accountTypes.size());
            for (final SystemAccountType accountType : accountTypes) {
                accounts.add(fetchService.fetch(accountType.getAccount(), fetch));
            }
            return (List<? extends Account>) accounts;
        }

        final AccountQuery query = new AccountQuery();
        query.setOwner(owner);
        query.fetch(fetch);
        List<? extends Account> accounts = accountDao.search(query);
        if (forceAllAccounts) {
            return accounts;
        }
        if (owner instanceof Member) {
            accounts = new ArrayList<Account>(accounts);
            final Member member = fetchService.fetch((Member) owner);
            for (final Iterator<? extends Account> iterator = accounts.iterator(); iterator.hasNext();) {
                final Account account = iterator.next();
                MemberGroupAccountSettings accountSettings;
                boolean remove = false;
                final Group group = member.getGroup();
                if (group.getStatus() == Group.Status.NORMAL) {
                    try {
                        accountSettings = groupService.loadAccountSettings(group.getId(), account.getType().getId());
                    } catch (final EntityNotFoundException e) {
                        accountSettings = null;
                        remove = true;
                    }
                } else {
                    // Removed group
                    accountSettings = null;
                }
                // Check whether the account is hidden
                if (accountSettings != null && accountSettings.isHideWhenNoCreditLimit()) {
                    // Hide the account: it should be visible only when has credit limit, and credit limit is zero or there are at least one transfer
                    final AccountStatus status = getStatus(new GetTransactionsDTO(account), false);
                    final boolean hasCreditLimit = Math.abs(status.getCreditLimit().floatValue()) > PRECISION_DELTA;
                    final boolean hasCredits = status.getCredits().getCount() > 0;
                    final boolean hasDebits = status.getDebits().getCount() > 0;
                    if (!hasCreditLimit && !(hasCredits || hasDebits)) {
                        remove = true;
                    }
                }
                if (remove) {
                    iterator.remove();
                }
            }
        }
        return accounts;
    }

    private List<? extends Account> getAccounts(final AccountType type) {
        final AccountQuery query = new AccountQuery();
        query.setType(type);
        return accountDao.search(query);
    }

    private boolean isValid(final MembersTransactionsReportParameters params) {
        final Collection<MemberGroup> memberGroups = params.getMemberGroups();
        if (CollectionUtils.isEmpty(memberGroups)) {
            return false;
        }
        if (!params.isDebits() && !params.isCredits()) {
            return false;
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private Iterator<Member> resolveMembersForTransactionsReport(final MembersTransactionsReportParameters params) {
        final Collection<MemberGroup> groups = params.getMemberGroups();
        final Period period = params.getPeriod();
        final MemberQuery query = new MemberQuery();
        query.setPageParameters(params.getPageParameters());
        if (params.isFetchBroker()) {
            query.fetch(Member.Relationships.BROKER);
        }
        query.setGroups(groups);
        if (period != null && period.getEnd() != null) {
            query.setActivationPeriod(Period.endingAt(period.getEnd()));
        }
        query.setResultType(ResultType.ITERATOR);
        final List<Member> members = (List<Member>) elementService.search(query);
        final Iterator<Member> membersIterator = members.iterator();
        return membersIterator;
    }

}
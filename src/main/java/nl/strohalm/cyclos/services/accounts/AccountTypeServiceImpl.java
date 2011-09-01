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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.accounts.AccountDAO;
import nl.strohalm.cyclos.dao.accounts.AccountStatusDAO;
import nl.strohalm.cyclos.dao.accounts.AccountTypeDAO;
import nl.strohalm.cyclos.dao.groups.GroupDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.AccountTypeQuery;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountStatus;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.AccountType.Nature;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation for account type service
 * @author luis
 */
public class AccountTypeServiceImpl implements AccountTypeService {

    private TransferTypeService  transferTypeService;
    private AccountDAO           accountDao;
    private AccountStatusDAO     accountStatusDao;
    private AccountTypeDAO       accountTypeDao;
    private GroupDAO             groupDao;
    private FetchService         fetchService;
    private AccountStatusHandler accountStatusHandler;

    @SuppressWarnings("unchecked")
    public Map<AccountType, BigDecimal> getAccountTypesBalance(final Nature nature) {
        final Map<AccountType, BigDecimal> accountTypesBalance = new LinkedHashMap<AccountType, BigDecimal>();
        switch (nature) {
            case MEMBER:
                final MemberAccountTypeQuery memberAtQuery = new MemberAccountTypeQuery();
                memberAtQuery.fetch(AccountType.Relationships.CURRENCY);
                if (LoggedUser.isValid()) {
                    AdminGroup adminGroup = LoggedUser.group();
                    adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                    memberAtQuery.setRelatedToGroups(adminGroup.getManagesGroups());
                }
                final List<MemberAccountType> memberAts = (List<MemberAccountType>) search(memberAtQuery);
                for (final MemberAccountType memberAt : memberAts) {
                    final BigDecimal memberAtBalance = accountTypeDao.getBalance(memberAt);
                    accountTypesBalance.put(memberAt, memberAtBalance);
                }
                break;
            case SYSTEM:
                if (LoggedUser.isValid()) {
                    AdminGroup adminGroup = LoggedUser.group();
                    adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.VIEW_INFORMATION_OF);
                    for (final SystemAccountType systemAt : adminGroup.getViewInformationOf()) {
                        final BigDecimal systemAtBalance = accountTypeDao.getBalance(systemAt);
                        accountTypesBalance.put(fetchService.fetch(systemAt, AccountType.Relationships.CURRENCY), systemAtBalance);
                    }
                }
        }
        return accountTypesBalance;
    }

    public MemberAccountType getDefault(MemberGroup group, final Relationship... fetch) {
        group = fetchService.fetch(group, MemberGroup.Relationships.ACCOUNT_SETTINGS);
        Collection<MemberGroupAccountSettings> accountSettings = group.getAccountSettings();
        MemberGroupAccountSettings defaultAccount = null;
        if (CollectionUtils.isNotEmpty(accountSettings)) {
            accountSettings = fetchService.fetch(accountSettings, MemberGroupAccountSettings.Relationships.ACCOUNT_TYPE);
            for (final MemberGroupAccountSettings current : accountSettings) {
                if (current.isDefault()) {
                    // Found the default account
                    defaultAccount = current;
                    break;
                }
            }
            if (defaultAccount == null) {
                // None found: get the first one
                defaultAccount = accountSettings.iterator().next();
            }
        }
        return defaultAccount == null ? null : fetchService.fetch(defaultAccount.getAccountType(), fetch);
    }

    public boolean hasAuthorizedPayments(AccountType accountType) {
        accountType = fetchService.fetch(accountType, AccountType.Relationships.FROM_TRANSFER_TYPES);
        for (final TransferType transferType : accountType.getFromTransferTypes()) {
            if (transferType.isRequiresAuthorization()) {
                return true;
            }
        }
        return false;
    }

    public List<? extends AccountType> listAll() {
        return accountTypeDao.listAll();
    }

    public AccountType load(final Long id, final Relationship... fetch) {
        final AccountType accountType = accountTypeDao.load(id, fetch);
        if (accountType instanceof SystemAccountType) {
            final SystemAccountType sat = (SystemAccountType) accountType;
            final SystemAccount account = sat.getAccount();
            if (account != null) {
                sat.setCreditLimit(account.getCreditLimit());
                sat.setUpperCreditLimit(account.getUpperCreditLimit());
            }
        }
        return accountType;
    }

    public int remove(final Long... ids) {
        for (final Long id : ids) {
            final AccountType accountType = load(id, AccountType.Relationships.FROM_TRANSFER_TYPES, AccountType.Relationships.TO_TRANSFER_TYPES, AccountType.Relationships.PAYMENT_FILTERS, SystemAccountType.Relationships.ACCOUNT, MemberAccountType.Relationships.ACCOUNT_FEES);
            if (accountType instanceof SystemAccountType) {
                final SystemAccountType systemAccountType = ((SystemAccountType) accountType);
                final SystemAccount account = systemAccountType.getAccount();
                systemAccountType.setAccount(null);
                accountTypeDao.update(systemAccountType);
                accountDao.delete(account.getId());
            }
        }
        return accountTypeDao.delete(ids);
    }

    public <AT extends AccountType> AT save(final AT accountType) {
        AT saved = null;
        validate(accountType);
        SystemAccount systemAccount = null;
        if (accountType.isTransient()) {
            saved = accountTypeDao.insert(accountType);
            if (saved instanceof SystemAccountType) {
                // Create the system account now
                final SystemAccountType systemAccountType = ((SystemAccountType) accountType);
                systemAccount = new SystemAccount();
                systemAccount.setCreationDate(Calendar.getInstance());
                systemAccount.setCreditLimit(systemAccountType.getCreditLimit());
                systemAccount.setUpperCreditLimit(systemAccountType.getUpperCreditLimit());
                systemAccount.setType(saved);
                systemAccount.setOwnerName(saved.getName());
                systemAccount = accountDao.insert(systemAccount);

                // Insert the initial status
                final AccountStatus status = new SystemAccountStatus();
                status.setAccount(systemAccount);
                status.setDate(systemAccount.getCreationDate());
                status.setCreditLimit(systemAccount.getCreditLimit());
                status.setUpperCreditLimit(systemAccount.getUpperCreditLimit());
                accountStatusDao.insert(status);

                // Add permission to the admin group
                AdminGroup group = (AdminGroup) LoggedUser.group();
                group = groupDao.load(group.getId(), AdminGroup.Relationships.VIEW_INFORMATION_OF);
                final Collection<SystemAccountType> systemAccountTypes = group.getViewInformationOf();
                systemAccountTypes.add(systemAccountType);
                groupDao.update(group);
            }
            // Member accounts are created when an account type gets related to a group
        } else {
            if (accountType instanceof SystemAccountType) {

                final SystemAccountType currentAccountType = (SystemAccountType) load(accountType.getId(), SystemAccountType.Relationships.VIEWED_BY_GROUPS);
                final Collection<AdminGroup> viewedByGroups = new ArrayList<AdminGroup>();
                if (currentAccountType.getViewedByGroups() != null) {
                    viewedByGroups.addAll(currentAccountType.getViewedByGroups());
                }

                final SystemAccountType systemAccountType = (SystemAccountType) accountType;
                systemAccountType.setViewedByGroups(viewedByGroups);

                // When updating a system account type, should update it's account too
                systemAccount = (SystemAccount) accountDao.load(SystemAccountOwner.instance(), systemAccountType);

                final BigDecimal oldLimit = systemAccount.getCreditLimit();
                final BigDecimal oldUpperLimit = systemAccount.getUpperCreditLimit();

                // When there was a credit limit, and it has changed, we must update the account
                final BigDecimal newLimit = systemAccountType.getCreditLimit();
                final BigDecimal newUpperLimit = systemAccountType.getUpperCreditLimit();
                final boolean updateLimit = (newLimit != null && !newLimit.equals(oldLimit)) || ((newUpperLimit != null) && !newUpperLimit.equals(oldUpperLimit));
                if (updateLimit) {
                    systemAccount.setCreditLimit(newLimit);
                    systemAccount.setUpperCreditLimit(newUpperLimit);
                }
                systemAccount.setOwnerName(systemAccountType.getName());
                systemAccountType.setAccount(systemAccount);
                accountDao.update(systemAccount);

                if (updateLimit) {
                    accountStatusHandler.processLimitChange(systemAccount);
                }
            }
            saved = accountTypeDao.update(accountType);
        }
        if (systemAccount != null) {
            ((SystemAccountType) saved).setAccount(systemAccount);
            saved = accountTypeDao.update(saved);
        }
        return saved;
    }

    @SuppressWarnings("unchecked")
    public List<? extends AccountType> search(final AccountTypeQuery query) {
        if (query instanceof MemberAccountTypeQuery) {
            final MemberAccountTypeQuery memberQuery = (MemberAccountTypeQuery) query;
            final AccountOwner canPay = memberQuery.getCanPay();
            if (canPay != null) {
                Group group = null;
                if (canPay instanceof Member) {
                    final Member member = fetchService.fetch((Member) canPay, Element.Relationships.GROUP);
                    group = member.getGroup();
                }
                Member owner = memberQuery.getOwner();
                if (owner == null && LoggedUser.isValid() && LoggedUser.isMember()) {
                    owner = LoggedUser.element();
                }
                // Can pay is handled differently: let's reuse the TransferTypeService to check which accounts have possible payment types
                final List<MemberAccountType> accountTypes = new ArrayList<MemberAccountType>();
                // I know: double casts looks awful, but...
                for (final MemberAccountType accountType : (List<MemberAccountType>) (List) accountTypeDao.search(new MemberAccountTypeQuery())) {
                    final TransferTypeQuery transferTypeQuery = new TransferTypeQuery();
                    transferTypeQuery.setPageForCount();
                    transferTypeQuery.setContext(TransactionContext.PAYMENT);
                    transferTypeQuery.setChannel(Channel.WEB);
                    transferTypeQuery.setUsePriority(true);
                    transferTypeQuery.setToAccountType(accountType);
                    transferTypeQuery.setToOwner(owner);
                    transferTypeQuery.setFromOwner(canPay);
                    transferTypeQuery.setGroup(group);
                    if (PageHelper.getTotalCount(transferTypeService.search(transferTypeQuery)) > 0) {
                        accountTypes.add(accountType);
                    }
                }
                return accountTypes;
            }
        }
        return accountTypeDao.search(query);
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

    public void setAccountTypeDao(final AccountTypeDAO accountTypeDao) {
        this.accountTypeDao = accountTypeDao;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupDao(final GroupDAO groupDao) {
        this.groupDao = groupDao;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public void validate(final AccountType accountType) {
        getValidator().validate(accountType);
    }

    private Validator getValidator() {
        final Validator validator = new Validator("accountType");
        validator.property("name").required().maxLength(100);
        validator.property("description").maxLength(1000);
        validator.property("currency").required();
        return validator;
    }
}
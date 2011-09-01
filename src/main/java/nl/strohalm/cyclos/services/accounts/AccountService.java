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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberTransactionDetailsReportData;
import nl.strohalm.cyclos.entities.members.MemberTransactionSummaryReportData;
import nl.strohalm.cyclos.entities.members.MembersTransactionsReportParameters;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Account service: manages accounts and account types related operations and calculations broker commission, credit limits, transaction volumes etc.
 * @author luis
 */
public interface AccountService extends Service {

    /**
     * Calculates the transactioned volume for the account during the period.
     */
    BigDecimal calculateTransactionedVolume(TransactionVolumeDTO params);

    /**
     * Returns the account that matches the owner and type
     */
    Account getAccount(AccountDTO params, Relationship... fetch);

    /**
     * Returns all the accounts of the given owner
     */
    List<? extends Account> getAccounts(AccountOwner owner, Relationship... fetch);

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
    Set<? extends Account> getAccountsFromTTs(final Member member, final Collection<TransferType> allowedTTs, final TransferType.Direction direction);

    /**
     * Returns the allowed account types for the current logged user and the given account owner
     */
    @DontEnforcePermission(traceable = false, value = "The implementation must carry out with the permissions control")
    Collection<? extends AccountType> getAllowedTypes(final AccountOwner owner);

    /**
     * Returns the account balance
     */
    BigDecimal getBalance(GetTransactionsDTO params);

    /**
     * Returns the a summary of received broker commission
     */
    TransactionSummaryVO getBrokerCommissions(GetTransactionsDTO params);

    /**
     * Return the credit limit for a given account
     */
    BigDecimal getCreditLimit(AccountDTO params);

    /**
     * Retrieves the current credit limit
     */
    @AdminAction(@Permission(module = "adminMemberAccounts", operation = "creditLimit"))
    @PathToMember("")
    CreditLimitDTO getCreditLimits(Member member);

    /**
     * gets the default account of the member from a list of allowed accounts. If the default account is not in this list, it gets the first account
     * in the list. If the list is empty, it returns null.
     */
    Account getDefaultAccountFromList(Member member, final List<Account> allowedAccounts);

    /**
     * Returns the account status for the given GetTransactionsDTO, using the account owner and account type to resolve the Account instance and the
     * period end, when present, to get the status date. When a period only with begin date is used, that date is used. Otherwise, when the period is
     * not present, use the current date. When a member account with volume account fees, returns a transient instance, calculating the account fee
     * charges to date
     */
    AccountStatus getStatus(GetTransactionsDTO params);

    /**
     * Returns the account status for the given GetTransactionsDTO, using the account owner and account type to resolve the Account instance and the
     * period end, when present, to get the status date. When a period only with begin date is used, that date is used. Otherwise, when the period is
     * not present, use the current date
     */
    AccountStatus getStatus(GetTransactionsDTO params, boolean calculateAccountFees);

    /**
     * Retrieves transactions summary
     * @param member the owner of the account
     * @param accountType the account type
     * @param period the period
     * @param paymentFilters collection of the payment filters
     * @param credits true = credits, false = debits
     */
    Map<PaymentFilter, TransactionSummaryVO> getTransactionsSummary(Member member, AccountType accountType, Period period, Collection<PaymentFilter> paymentFilters, boolean credits);

    /**
     * Locks all the given accounts, for write or read, depending on the flag
     */
    void lock(boolean forWrite, List<Account> accounts);

    /**
     * Locks all the given accounts, for write or read, depending on the flag
     */
    void lockAccounts(boolean forWrite, List<AccountDTO> dtos);

    /**
     * Runs a member report with transactions details
     */
    Iterator<MemberTransactionDetailsReportData> membersTransactionsDetailsReport(MembersTransactionsReportParameters params);

    /**
     * Runs a member report with transactions summaries
     */
    Iterator<MemberTransactionSummaryReportData> membersTransactionsSummaryReport(MembersTransactionsReportParameters params);

    /**
     * Removes all account statuses related to the given account fee log
     */
    void removeStatusRelatedTo(AccountFeeLog log);

    /**
     * Sets the new credit limit
     */
    @AdminAction(@Permission(module = "adminMemberAccounts", operation = "creditLimit"))
    @PathToMember("")
    void setCreditLimit(Member member, CreditLimitDTO limits);

    /**
     * Validates a credit limit DTO
     */
    @DontEnforcePermission(traceable = true)
    void validate(Member member, CreditLimitDTO creditLimit);
}
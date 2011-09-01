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
package nl.strohalm.cyclos.dao.accounts.transactions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.accounts.AccountDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountQuery;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransfer;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransfersAwaitingAuthorizationQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.AuthorizationLevel.Authorizer;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.reports.StatisticalDTO;
import nl.strohalm.cyclos.services.stats.general.KeyDevelopmentsStatsPerMonthVO;
import nl.strohalm.cyclos.utils.Pair;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.hibernate.HibernateCustomFieldHandler;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;
import nl.strohalm.cyclos.utils.statistics.ListOperations;

import org.apache.commons.collections.CollectionUtils;

/**
 * Implementation class for transfer DAO
 * @author rafael, Jefferson Magno
 */
public class TransferDAOImpl extends BaseDAOImpl<Transfer> implements TransferDAO {

    private AccountDAO                  accountDao;
    private HibernateCustomFieldHandler hibernateCustomFieldHandler;

    public TransferDAOImpl() {
        super(Transfer.class);
    }

    // Used by Activity: all using gross product.
    public List<Pair<Member, BigDecimal>> getGrossProductPerMember(final StatisticalDTO dto) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder("select new " + Pair.class.getName());
        hql.append("(m, sum(t.amount)) from Transfer t, Member m where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");
        hql.append(" and exists (select ma.id from MemberAccount ma where t.to = ma and m = ma.member) ");
        appendGroupAndPaymentFilterAndPeriod(hql, namedParameters, dto);
        // Group by the member that received the transfer
        hql.append(" group by m ");

        // Order by the sum of amounts
        hql.append(" order by sum(t.amount) desc ");

        return list(hql.toString(), namedParameters);
    }

    // performance tested 2010: 24 secs
    public List<KeyDevelopmentsStatsPerMonthVO> getGrossProductPerMonth(final StatisticalDTO dto) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append(" select new " + KeyDevelopmentsStatsPerMonthVO.class.getName());
        hql.append(" (sum(t.amount), month(t.processDate), year(t.processDate)) ");
        hql.append(" from Transfer t, Member m where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");
        hql.append(" and exists (select ma.id from MemberAccount ma where t.to = ma and m = ma.member) ");
        appendGroupAndPaymentFilterAndPeriod(hql, namedParameters, dto);
        hql.append(" group by month(t.processDate), year(t.processDate) ");
        hql.append(" order by year(t.processDate), month(t.processDate) ");

        final List<KeyDevelopmentsStatsPerMonthVO> list = list(hql.toString(), namedParameters);
        return list;
    }

    // Used by Activity Stats > all using number of trans, % not trading
    public List<Pair<Member, Integer>> getNumberOfTransactionsPerMember(final StatisticalDTO dto) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append("select new " + Pair.class.getName() + "(m, count(t.id))");
        hql.append(" from Transfer t, Member m where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");
        // transaction to
        hql.append(" and (exists (select ma.id from MemberAccount ma where t.to = ma and m = ma.member) ");
        // transaction from (added by Rinke, because eventually this seemed more logical)
        hql.append(" or exists (select ma.id from MemberAccount ma where t.from = ma and m = ma.member)) ");
        appendGroupAndPaymentFilterAndPeriod(hql, namedParameters, dto);

        // Group by the member that received the transfer
        hql.append(" group by m ");

        // Order by the sum of amounts
        hql.append(" order by count(t.id) desc ");

        return list(hql.toString(), namedParameters);
    }

    public List<KeyDevelopmentsStatsPerMonthVO> getNumberOfTransactionsPerMonth(final StatisticalDTO dto) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append(" select new " + KeyDevelopmentsStatsPerMonthVO.class.getName());
        hql.append(" (count(distinct t.id), month(t.processDate), year(t.processDate)) ");
        hql.append(" from Transfer t, Member m where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");
        hql.append(" and (exists (select ma.id from MemberAccount ma where t.to = ma and m = ma.member) ");
        hql.append(" or exists (select ma.id from MemberAccount ma where t.from = ma and m = ma.member)) ");
        appendGroupAndPaymentFilterAndPeriod(hql, namedParameters, dto);
        hql.append(" group by month(t.processDate), year(t.processDate) ");
        hql.append(" order by year(t.processDate), month(t.processDate) ");

        final List<KeyDevelopmentsStatsPerMonthVO> list = list(hql.toString(), namedParameters);
        return list;
    }

    public List<Pair<Member, BigDecimal>> getPaymentsPerMember(final StatisticalDTO dto) throws DaoException {
        // TODO 999: change later on to its own implementation (it should sum outgoing transfers, not incoming as in gross product)
        return getGrossProductPerMember(dto);
    }

    public BigDecimal getSumOfTransactions(final StatisticalDTO dto) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        // the query uses the same as getGrossProductPerMember, because mysql appears to be much faster with this query than with a simple select
        // sum(t.amount) without "group by".
        final StringBuilder hql = new StringBuilder("select new " + Pair.class.getName());
        hql.append("(m, sum(t.amount)) from Transfer t, Member m where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");
        hql.append(" and exists (select ma.id from MemberAccount ma where t.to = ma and m = ma.member) ");
        appendGroupAndPaymentFilterAndPeriod(hql, namedParameters, dto);
        // TransferType
        if (dto.getTransferType() != null) {
            hql.append(" and t.type = :transferType ");
            namedParameters.put("transferType", dto.getTransferType());
        }
        hql.append(" group by m ");

        final List<Pair<Member, BigDecimal>> sums = list(hql.toString(), namedParameters);
        BigDecimal sumOfTransactions = BigDecimal.ZERO;
        for (final Pair<Member, BigDecimal> item : sums) {
            sumOfTransactions = sumOfTransactions.add(item.getSecond());
        }
        return sumOfTransactions;
    }

    // TEST PERFORMANCE
    public BigDecimal getSumOfTransactionsRest(final TransferQuery query) {
        final StringBuilder hql = new StringBuilder("select sum(t.amount) from Transfer t where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");

        final Map<String, Object> namedParameters = new HashMap<String, Object>();

        // Period
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "t.processDate", query.getPeriod());

        // From AccountType
        if (query.getFromAccountType() != null) {
            hql.append(" and t.from.type = :fromAccountType");
            namedParameters.put("fromAccountType", query.getFromAccountType());
        }

        // To AccountType
        if (query.getToAccountType() != null) {
            hql.append(" and t.to.type = :toAccountType");
            namedParameters.put("toAccountType", query.getToAccountType());
        }

        // TransferType not in the collection of payment filters
        // (that is the meaning of the word 'rest' on the name of the method)
        final Collection<PaymentFilter> paymentFilters = query.getPaymentFilters();
        if (paymentFilters != null && !CollectionUtils.isEmpty(paymentFilters)) {
            final Set<TransferType> transferTypesSet = new HashSet<TransferType>();
            for (final PaymentFilter paymentFilter : paymentFilters) {
                transferTypesSet.addAll(paymentFilter.getTransferTypes());
            }
            hql.append(" and t.type not in (:transferTypes) ");
            namedParameters.put("transferTypes", transferTypesSet);
        }

        final BigDecimal sumOfTransactions = uniqueResult(hql.toString(), namedParameters);
        if (sumOfTransactions == null) {
            return BigDecimal.ZERO;
        } else {
            return sumOfTransactions;
        }
    }

    // used by key dev: transactionamounts and number of transactions.
    public List<Number> getTransactionAmounts(final StatisticalDTO dto) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append("select new " + Pair.class.getName() + "(t.id, t.amount)");
        hql.append(" from Transfer t, Member m where 1=1 ");
        hql.append(" and t.chargedBackBy is null and t.chargebackOf is null ");
        hql.append(" and (exists (select ma.id from MemberAccount ma where t.to = ma and m = ma.member) ");
        hql.append(" or exists (select ma.id from MemberAccount ma where t.from = ma and m = ma.member)) ");
        appendGroupAndPaymentFilterAndPeriod(hql, namedParameters, dto);
        final List<Pair<Long, BigDecimal>> pairList = list(hql.toString(), namedParameters);
        // because using to and from's, duplicate id's may appear. Transferring to Set solves this.
        final Set<Pair<Long, BigDecimal>> pairSet = new HashSet<Pair<Long, BigDecimal>>(pairList);
        return ListOperations.getSecondNumberFromPairCollection(pairSet);
    }

    public Transfer loadTransferByTraceNumber(final String traceNumber, final Long clientId) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();

        final StringBuilder hql = HibernateHelper.getInitialQuery(getEntityType(), "t");
        HibernateHelper.addParameterToQuery(hql, namedParameters, "traceNumber", traceNumber);
        HibernateHelper.addParameterToQuery(hql, namedParameters, "clientId", clientId);

        return uniqueResult(hql.toString(), namedParameters);
    }

    public List<SimpleTransferVO> paymentVOs(final Account account, final Period period) throws DaoException {
        final StringBuilder hql = new StringBuilder();
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("account", account);
        hql.append("select new ").append(SimpleTransferVO.class.getName()).append("(t.date, case t.from when :account then -t.amount else t.amount end)");
        hql.append(" from ").append(getEntityType().getName()).append(" t");
        hql.append(" where (t.from = :account or t.to = :account) ");
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "t.date", period);
        hql.append(" order by t.date");

        return list(hql.toString(), namedParameters);
    }

    public List<Transfer> search(final TransferQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append(" select t");
        hql.append(" from Loan l right join l.transfer t ");
        hibernateCustomFieldHandler.appendJoins(hql, "t.customValues", query.getCustomValues());
        HibernateHelper.appendJoinFetch(hql, Transfer.class, "t", query.getFetch());
        hql.append(" where 1=1");

        if (!buildSearchQuery(query, hql, namedParameters)) {
            return Collections.emptyList();
        }

        return list(query, hql.toString(), namedParameters);
    }

    public List<Transfer> searchTransfersAwaitingAuthorization(final TransfersAwaitingAuthorizationQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append(" select t from Transfer t join t.nextAuthorizationLevel l ");
        HibernateHelper.appendJoinFetch(hql, getEntityType(), "t", query.getFetch());
        hql.append(" where 1=1");

        Element authorizer = query.getAuthorizer();
        if (authorizer == null) {
            return Collections.emptyList();
        }

        // Set common parameters
        authorizer = getFetchDao().fetch(authorizer, Element.Relationships.GROUP);
        for (final Authorizer auth : Authorizer.values()) {
            namedParameters.put(auth.name(), auth);
        }
        namedParameters.put("authorizer", authorizer);

        // The payment must be top-level
        hql.append(" and t.parent is null");

        // Status is PENDING and process date is null
        hql.append(" and t.processDate is null and t.status = :status ");
        namedParameters.put("status", Payment.Status.PENDING);

        // Filter by authorizer
        if (authorizer instanceof Administrator) {
            final Administrator administrator = (Administrator) authorizer;
            hql.append(" and l.authorizer in (:ADMIN, :BROKER)");
            hql.append(" and :adminGroup in elements(l.adminGroups)");
            namedParameters.put("adminGroup", administrator.getAdminGroup());
        } else if (authorizer instanceof Operator) {
            hql.append(" and ((l.authorizer = :RECEIVER and exists (");
            hql.append(" select ma.id from MemberAccount ma, Operator o where ma = t.to and o.member = ma.member and o = :authorizer");
            hql.append(" )) or (l.authorizer = :PAYER and exists (");
            hql.append(" select ma.id from MemberAccount ma, Operator o where ma = t.from and o.member = ma.member and o = :authorizer");
            hql.append(" ))) ");
        } else {
            hql.append(" and ((l.authorizer = :BROKER and exists(");
            hql.append(" select ma.id from MemberAccount ma where ma = t.from and ma.member.broker = :authorizer");
            hql.append(" )) or (l.authorizer = :RECEIVER and exists (");
            hql.append(" select ma.id from MemberAccount ma where ma = t.to and ma.member = :authorizer");
            hql.append(" )) or (l.authorizer = :PAYER and exists (");
            hql.append(" select ma.id from MemberAccount ma where ma = t.from and ma.member = :authorizer");
            hql.append(")))");
        }

        // Ensures that when the authorizer has already authorized once, the same transfer is not returned
        hql.append(" and not exists (select a.id from TransferAuthorization a where a.transfer = t and a.by = :authorizer)");

        // Add the from member
        final Member member = query.getMember();
        if (member != null) {
            hql.append(" and exists (select ma.id from MemberAccount ma where ma.member = :member and (ma = t.from or ma = t.to))");
            namedParameters.put("member", member);
        }

        // Add the payment filter
        PaymentFilter paymentFilter = query.getPaymentFilter();
        if (paymentFilter != null) {
            paymentFilter = getFetchDao().fetch(paymentFilter, PaymentFilter.Relationships.TRANSFER_TYPES);
            HibernateHelper.addInParameterToQuery(hql, namedParameters, "t.type", paymentFilter.getTransferTypes());
        }

        // Add the other filters
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "t.date", query.getPeriod());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "t.type", query.getTransferType());
        HibernateHelper.addLikeParameterToQuery(hql, namedParameters, "t.transactionNumber", query.getTransactionNumber());
        HibernateHelper.appendOrder(hql, "t.date desc");

        return list(query, hql.toString(), namedParameters);
    }

    public void setAccountDao(final AccountDAO accountDao) {
        this.accountDao = accountDao;
    }

    public void setHibernateCustomFieldHandler(final HibernateCustomFieldHandler hibernateCustomFieldHandler) {
        this.hibernateCustomFieldHandler = hibernateCustomFieldHandler;
    }

    public Transfer updateAuthorizationData(final Long id, final Transfer.Status status, final AuthorizationLevel nextAuthorizationLevel, final Calendar processDate) {
        final Transfer transfer = load(id);
        transfer.setStatus(status);
        transfer.setNextAuthorizationLevel(nextAuthorizationLevel);
        transfer.setProcessDate(processDate);
        return update(transfer);
    }

    public Transfer updateChargeBack(final Transfer transfer, final Transfer chargeback) {
        transfer.setChargedBackBy(chargeback);
        return update(transfer);
    }

    public Transfer updateExternalTransfer(final Long id, final ExternalTransfer externalTransfer) {
        final Transfer transfer = load(id);
        transfer.setExternalTransfer(externalTransfer);
        return update(transfer);
    }

    public Transfer updateStatus(final Long id, final Payment.Status status) {
        final Transfer transfer = load(id);
        transfer.setStatus(status);
        if (status == Payment.Status.FAILED) {
            transfer.setProcessDate(null);
        }
        return update(transfer);
    }

    public Transfer updateTransactionNumber(final Long id, final String transactionNumber) {
        final Transfer transfer = load(id);
        transfer.setTransactionNumber(transactionNumber);
        return update(transfer);
    }

    /**
     * convenience method for a very often repeated block of hql query append statements. It appends the group filter, payment filter and period to a
     * hql StringBuilder object for a query.
     * 
     * @param hql
     * @param namedParameters
     * @param dto
     */
    private void appendGroupAndPaymentFilterAndPeriod(final StringBuilder hql, final Map<String, Object> namedParameters, final StatisticalDTO dto) {
        // Period
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "t.processDate", dto.getPeriod());
        // PaymentFilter
        if (dto.getPaymentFilter() != null) {
            hql.append(" and exists (select 1 from " + PaymentFilter.class.getName() + " pf where pf = :filter and t.type in elements(pf.transferTypes)) ");
            namedParameters.put("filter", dto.getPaymentFilter());
        }
        // Members groups
        if (!CollectionUtils.isEmpty(dto.getGroups())) {
            hql.append(" and exists ");
            hql.append("    ( select ghl.id ");
            hql.append("      from GroupHistoryLog ghl ");
            hql.append("      where ghl.element = m ");
            hql.append("      and ghl.group in (:groups) ");
            hql.append("      and ghl.period.begin < :end ");
            hql.append("      and (ghl.period.end is null or ghl.period.end >= :begin) ");
            hql.append("      and t.processDate between ghl.period.begin and ifnull(ghl.period.end, t.processDate) ");
            hql.append("    ) ");
            namedParameters.put("groups", dto.getGroups());
            namedParameters.put("begin", dto.getPeriod().getBegin());
            namedParameters.put("end", dto.getPeriod().getEnd());
        }
    }

    private boolean buildSearchQuery(final TransferQuery query, final StringBuilder hql, final Map<String, Object> namedParameters) {
        // hql.append(" and not exists (select pas.id from PendingAccountStatus pas where pas.transfer = t)");
        HibernateHelper.addParameterToQuery(hql, namedParameters, "t.type.requiresAuthorization", query.getRequiresAuthorization());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "t.status", query.getStatus());
        HibernateHelper.addLikeParameterToQuery(hql, namedParameters, "t.description", query.getDescription());
        HibernateHelper.addLikeParameterToQuery(hql, namedParameters, "t.transactionNumber", query.getTransactionNumber());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "t.loanPayment", query.getLoanPayment());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "t.parent", query.getParent());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "t.type", query.getTransferType());
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "ifnull(t.processDate, t.date)", query.getPeriod());
        if (query.isRootOnly()) {
            hql.append(" and t.parent is null");
        }
        if (query.getLoanTransfer() != null) {
            if (query.getLoanTransfer()) {
                hql.append(" and l is not null");
            } else {
                hql.append(" and l is null");
            }
        }

        // By conciliation status
        if (query.getConciliated() != null) {
            hql.append(" and t.externalTransfer is " + (query.getConciliated() ? "not" : "") + " null");
        }

        // By account
        if (query.getOwner() != null && query.getType() != null) {
            // Load the account
            final Account account = accountDao.load(query.getOwner(), query.getType());
            namedParameters.put("account", account);

            if (query.getMember() != null) {
                // Load the related member accounts
                final AccountQuery otherAccountsQuery = new AccountQuery();
                otherAccountsQuery.setOwner(query.getMember());
                final List<? extends Account> otherAccounts = accountDao.search(otherAccountsQuery);
                if (otherAccounts.isEmpty()) {
                    // No accounts - ensure nothing will be returned
                    return false;
                } else {
                    hql.append(" and ((t.from = :account and t.to in (:relatedAccounts)) or (t.to = :account and t.from in (:relatedAccounts)))");
                    namedParameters.put("relatedAccounts", otherAccounts);
                }
            } else {
                hql.append(" and (t.from = :account or t.to = :account)");
            }

            // Use the groups / group filters
            final Collection<MemberGroup> groups = new HashSet<MemberGroup>();
            if (query.getGroups() != null) {
                groups.addAll(query.getGroups());
            }
            if (query.getGroupFilters() != null) {
                for (GroupFilter groupFilter : query.getGroupFilters()) {
                    if (groupFilter != null && groupFilter.isPersistent()) {
                        groupFilter = getFetchDao().fetch(groupFilter, GroupFilter.Relationships.GROUPS);
                        groups.addAll(groupFilter.getGroups());
                    }
                }
            }
            if (!groups.isEmpty()) {
                hql.append(" and ((t.to = :account and exists (select ma.id from MemberAccount ma where ma = t.from and ma.member.group in (:groups)))");
                hql.append("   or (t.from = :account and exists (select ma.id from MemberAccount ma where ma = t.to and ma.member.group in (:groups))))");
                namedParameters.put("groups", groups);
            }
        }

        // From account owner
        if (query.getFromAccountOwner() != null) {
            final AccountQuery accountQuery = new AccountQuery();
            accountQuery.setOwner(query.getFromAccountOwner());
            final List<? extends Account> fromAccounts = accountDao.search(accountQuery);
            hql.append(" and t.from in (:fromAccounts) ");
            namedParameters.put("fromAccounts", fromAccounts);
        }

        // To account owner
        if (query.getToAccountOwner() != null) {
            final AccountQuery accountQuery = new AccountQuery();
            accountQuery.setOwner(query.getToAccountOwner());
            final List<? extends Account> toAccounts = accountDao.search(accountQuery);
            hql.append(" and t.to in (:toAccounts) ");
            namedParameters.put("toAccounts", toAccounts);
        }

        // PaymentFilter
        final Collection<PaymentFilter> paymentFilters = query.getPaymentFilters();
        if (CollectionUtils.isNotEmpty(paymentFilters)) {
            // Get all TTs from all those payment filters
            final String ttHql = "from TransferType tt where exists ("
                    + " select 1"
                    + " from PaymentFilter pf"
                    + " where pf in (:pfs)"
                    + " and tt in elements(pf.transferTypes))";
            final List<TransferType> transferTypes = list(ttHql, Collections.singletonMap("pfs", paymentFilters));

            HibernateHelper.addInParameterToQuery(hql, namedParameters, "t.type", transferTypes);
        }

        if (query.getExcludeTransferType() != null) {
            hql.append(" and t.type != :excludeTransferType ");
            namedParameters.put("excludeTransferType", query.getExcludeTransferType());
        }

        // Operated by
        if (query.getBy() != null) {
            hql.append(" and (t.by = :by or t.receiver = :by)");
            namedParameters.put("by", query.getBy());
        }

        // Custom fields
        hibernateCustomFieldHandler.appendConditions(hql, namedParameters, query.getCustomValues());

        // Set the order
        if (!query.isUnordered()) {
            final List<String> orders = new ArrayList<String>();

            // Order by date ...
            String order = "ifnull(t.processDate, t.date)";
            if (query.isReverseOrder()) {
                order += " desc";
            }
            orders.add(order);

            // ... then by id, to ensure that payments in the same second are ordered correctly
            order = "t.id";
            if (query.isReverseOrder()) {
                order += " desc";
            }
            orders.add(order);

            HibernateHelper.appendOrder(hql, orders);
        }

        return true;
    }

}
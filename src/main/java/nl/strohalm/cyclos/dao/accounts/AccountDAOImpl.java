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
package nl.strohalm.cyclos.dao.accounts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountLock;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.AccountQuery;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccount;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.BrokerCommission;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberTransactionDetailsReportData;
import nl.strohalm.cyclos.entities.members.MemberTransactionSummaryVO;
import nl.strohalm.cyclos.entities.members.MembersTransactionsReportParameters;
import nl.strohalm.cyclos.entities.settings.LocalSettings.MemberResultDisplay;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.accounts.AccountDTO;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.ScrollableResultsIterator;
import nl.strohalm.cyclos.utils.conversion.Transformer;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.SQLQuery;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

/**
 * Implementation DAO for accounts
 * @author rafael, Jefferson Magno, luis
 */
public class AccountDAOImpl extends BaseDAOImpl<Account> implements AccountDAO {

    /**
     * Key for an account id
     * 
     * @author luis
     */
    private static class CacheKey {
        private Long memberId;
        private Long accountTypeId;

        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof CacheKey)) {
                return false;
            }
            final CacheKey ck = (CacheKey) obj;
            return ObjectUtils.equals(memberId, ck.memberId) && ObjectUtils.equals(accountTypeId, ck.accountTypeId);
        }

        @Override
        public int hashCode() {
            return (memberId == null ? 0 : memberId.intValue()) + (accountTypeId == null ? 0 : accountTypeId.intValue());
        }
    }

    private static final char[] COLUMN_DELIMITERS = new char[] { '_' };

    private Map<CacheKey, Long> cachedAccountIds  = new HashMap<CacheKey, Long>();

    public AccountDAOImpl() {
        super(Account.class);
    }

    @Override
    public int delete(final boolean flush, final Long... ids) {
        getSession()
                .createQuery("delete from AccountLock l where l.id in (:ids)")
                .setParameterList("ids", ids)
                .executeUpdate();
        return super.delete(flush, ids);
    }

    public TransactionSummaryVO getBrokerCommissions(final GetTransactionsDTO dto) throws EntityNotFoundException, DaoException {
        final Account account = load(dto.getOwner(), dto.getType());
        final Period period = dto.getPeriod();
        final StringBuilder hql = new StringBuilder();
        final Map<String, Object> namedParams = new HashMap<String, Object>();
        hql.append(" select count(*), sum(t.amount)");
        hql.append(" from " + Transfer.class.getName() + " t, " + BrokerCommission.class.getName() + " f");
        hql.append(" where t.accountFeeLog.accountFee = f ");
        // Here we use just one payment filter
        final Collection<PaymentFilter> paymentFilters = dto.getPaymentFilters();
        if (CollectionUtils.isNotEmpty(paymentFilters)) {
            final PaymentFilter paymentFilter = paymentFilters.iterator().next();
            if (paymentFilter != null) {
                hql.append(" and t.type in (select pf.transferTypes from " + PaymentFilter.class.getName() + " pf where pf = :pf) ");
                namedParams.put("pf", paymentFilter);
            }
        }
        hql.append("   and t.to = :account ");
        namedParams.put("account", account);
        HibernateHelper.addPeriodParameterToQuery(hql, namedParams, "ifnull(t.processDate, t.date)", period);
        return buildSummary(uniqueResult(hql.toString(), namedParams));
    };

    public TransactionSummaryVO getCredits(final GetTransactionsDTO dto) {
        return getSummary(dto, true, Transfer.Status.PROCESSED);
    }

    public TransactionSummaryVO getDebits(final GetTransactionsDTO dto) {
        return getSummary(dto, false, Transfer.Status.PROCESSED);
    }

    public TransactionSummaryVO getLoans(final GetTransactionsDTO dto) throws EntityNotFoundException, DaoException {
        final Account account = load(dto.getOwner(), dto.getType());
        final Period period = dto.getPeriod();
        final StringBuilder hql = new StringBuilder();
        final Map<String, Object> namedParams = new HashMap<String, Object>();
        hql.append(" select count(*), sum(t.amount)");
        hql.append(" from " + Loan.class.getName() + " l join l.transfer t");
        hql.append(" where t.to = :account ");
        // Here we use just one payment filter
        final Collection<PaymentFilter> paymentFilters = dto.getPaymentFilters();
        if (CollectionUtils.isNotEmpty(paymentFilters)) {
            final PaymentFilter paymentFilter = paymentFilters.iterator().next();
            if (paymentFilter != null) {
                hql.append(" and t.type in (select pf.transferTypes from " + PaymentFilter.class.getName() + " pf where pf = :pf) ");
                namedParams.put("pf", paymentFilter);
            }
        }
        namedParams.put("account", account);
        HibernateHelper.addPeriodParameterToQuery(hql, namedParams, "ifnull(t.processDate, t.date)", period);
        return buildSummary(uniqueResult(hql.toString(), namedParams));
    }

    public TransactionSummaryVO getPendingCredits(final GetTransactionsDTO dto) throws EntityNotFoundException, DaoException {
        return getSummary(dto, true, Transfer.Status.PENDING);
    }

    public TransactionSummaryVO getPendingDebits(final GetTransactionsDTO dto) throws EntityNotFoundException, DaoException {
        return getSummary(dto, false, Transfer.Status.PENDING);
    }

    @Override
    public <T extends Account> T insert(final T entity, final boolean flush) throws UnexpectedEntityException, DaoException {
        final T account = super.insert(entity, false);
        getSession().persist(new AccountLock(account));
        if (flush) {
            getSession().flush();
        }
        return account;
    }

    public Account load(final AccountOwner owner, final AccountType type, final Relationship... fetch) throws EntityNotFoundException, DaoException {
        return load(getAccountId(owner, type), fetch);
    }

    public List<Account> loadAll(final List<AccountDTO> dtos, final Relationship... fetch) throws EntityNotFoundException, DaoException {
        final List<Account> accounts = new ArrayList<Account>();
        for (final AccountDTO dto : dtos) {
            accounts.add(load(dto.getOwner(), dto.getType(), fetch));
        }
        return accounts;
    }

    public void lock(final boolean forWrite, final List<Account> accounts) {
        doLock(forWrite, Arrays.asList(EntityHelper.toIds(accounts)));
    }

    public void lockAccounts(final boolean forWrite, final List<AccountDTO> dtos) {
        final List<Long> ids = new ArrayList<Long>(dtos.size());
        for (final AccountDTO dto : dtos) {
            final Long id = getAccountId(dto.getOwner(), dto.getType());
            ids.add(id);
        }
        doLock(forWrite, ids);
    }

    public Iterator<MemberTransactionDetailsReportData> membersTransactionsDetailsReport(final MembersTransactionsReportParameters params) {
        final StringBuilder sql = new StringBuilder();
        final Map<String, Object> parameters = new HashMap<String, Object>();

        // Find the transfer types ids
        Set<Long> ttIds = null;
        if (CollectionUtils.isNotEmpty(params.getPaymentFilters())) {
            ttIds = new HashSet<Long>();
            for (PaymentFilter pf : params.getPaymentFilters()) {
                pf = getFetchDao().fetch(pf, PaymentFilter.Relationships.TRANSFER_TYPES);
                final Long[] ids = EntityHelper.toIds(pf.getTransferTypes());
                CollectionUtils.addAll(ttIds, ids);
            }
        }

        // Get the member group ids
        Set<Long> groupIds = null;
        if (CollectionUtils.isNotEmpty(params.getMemberGroups())) {
            groupIds = new HashSet<Long>();
            CollectionUtils.addAll(groupIds, EntityHelper.toIds(params.getMemberGroups()));
        }

        // Get the period
        final Period period = params.getPeriod();
        final Calendar beginDate = DateHelper.getBeginForParameter(period);
        final Calendar endDate = DateHelper.getEndForParameter(period);

        // Set the parameters
        final boolean useTT = CollectionUtils.isNotEmpty(ttIds);
        final boolean useBegin = beginDate != null;
        final boolean useEnd = endDate != null;
        if (useTT) {
            parameters.put("ttIds", ttIds);
        }
        if (useBegin) {
            parameters.put("beginDate", beginDate);
        }
        if (useEnd) {
            parameters.put("endDate", endDate);
        }
        parameters.put("processed", Payment.Status.PROCESSED.getValue());

        // Build the sql string
        sql.append(" select u.username, m.name, bu.username broker_username, b.name broker_name, h.account_type_name, h.date, h.amount, h.description, h.related_username, h.related_name, h.transfer_type_name, h.transaction_number");
        sql.append(" from members m inner join users u on m.id = u.id left join members b on m.member_broker_id = b.id left join users bu on b.id = bu.id,");
        sql.append(" (");
        if (params.isCredits()) {
            appendMembersTransactionsDetailsReportSqlPart(sql, useTT, useBegin, useEnd, true, true);
            sql.append(" union");
            appendMembersTransactionsDetailsReportSqlPart(sql, useTT, useBegin, useEnd, true, false);
            if (params.isDebits()) {
                sql.append(" union");
            }
        }
        if (params.isDebits()) {
            appendMembersTransactionsDetailsReportSqlPart(sql, useTT, useBegin, useEnd, false, true);
            sql.append(" union");
            appendMembersTransactionsDetailsReportSqlPart(sql, useTT, useBegin, useEnd, false, false);
        }
        sql.append(" ) h");
        sql.append(" where m.id = h.member_id");
        if (groupIds != null) {
            parameters.put("groupIds", groupIds);
            sql.append(" and m.group_id in (:groupIds)");
        }
        sql.append(" order by m.name, u.username, h.account_type_name, h.date desc, h.transfer_id desc");

        // Prepare the query
        final SQLQuery query = getSession().createSQLQuery(sql.toString());
        final Map<String, Type> columns = new LinkedHashMap<String, Type>();
        columns.put("username", StandardBasicTypes.STRING);
        columns.put("name", StandardBasicTypes.STRING);
        columns.put("broker_username", StandardBasicTypes.STRING);
        columns.put("broker_name", StandardBasicTypes.STRING);
        columns.put("account_type_name", StandardBasicTypes.STRING);
        columns.put("date", StandardBasicTypes.CALENDAR);
        columns.put("amount", StandardBasicTypes.BIG_DECIMAL);
        columns.put("description", StandardBasicTypes.STRING);
        columns.put("related_username", StandardBasicTypes.STRING);
        columns.put("related_name", StandardBasicTypes.STRING);
        columns.put("transfer_type_name", StandardBasicTypes.STRING);
        columns.put("transaction_number", StandardBasicTypes.STRING);
        for (final Map.Entry<String, Type> entry : columns.entrySet()) {
            query.addScalar(entry.getKey(), entry.getValue());
        }
        getHibernateQueryHandler().setQueryParameters(query, parameters);

        // Create a transformer, which will read rows as Object[] and transform them to MemberTransactionDetailsReportData
        final Transformer<Object[], MemberTransactionDetailsReportData> transformer = new Transformer<Object[], MemberTransactionDetailsReportData>() {
            public MemberTransactionDetailsReportData transform(final Object[] input) {
                final MemberTransactionDetailsReportData data = new MemberTransactionDetailsReportData();
                int i = 0;
                for (final Map.Entry<String, Type> entry : columns.entrySet()) {
                    final String columnName = entry.getKey();
                    // Column names are transfer_type_name, property is transferTypeName
                    String propertyName = WordUtils.capitalize(columnName, COLUMN_DELIMITERS);
                    propertyName = Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
                    propertyName = StringUtils.replace(propertyName, "_", "");
                    PropertyHelper.set(data, propertyName, input[i]);
                    i++;
                }
                return data;
            }
        };

        return new ScrollableResultsIterator<MemberTransactionDetailsReportData>(query, transformer);
    }

    public Iterator<MemberTransactionSummaryVO> membersTransactionSummaryReport(final Collection<MemberGroup> memberGroups, final PaymentFilter paymentFilter, final Period period, final boolean credits, final MemberResultDisplay order) {
        final Map<String, Object> parameters = new HashMap<String, Object>();
        final StringBuilder sql = new StringBuilder();

        // Get the transfer types ids
        final List<Long> ttIds = paymentFilter == null ? null : Arrays.asList(EntityHelper.toIds(paymentFilter.getTransferTypes()));

        // Get the member group ids
        List<Long> groupIds = null;
        if (CollectionUtils.isNotEmpty(memberGroups)) {
            groupIds = Arrays.asList(EntityHelper.toIds(memberGroups));
        }

        // Get the period
        final Calendar beginDate = DateHelper.getBeginForParameter(period);
        final Calendar endDate = DateHelper.getEndForParameter(period);

        // Set the parameters
        final boolean useGroups = CollectionUtils.isNotEmpty(groupIds);
        final boolean useTT = CollectionUtils.isNotEmpty(ttIds);
        final boolean useBegin = beginDate != null;
        final boolean useEnd = endDate != null;
        if (useGroups) {
            parameters.put("groupIds", groupIds);
        }
        if (useTT) {
            parameters.put("ttIds", ttIds);
        }
        if (useBegin) {
            parameters.put("beginDate", beginDate);
        }
        if (useEnd) {
            parameters.put("endDate", endDate);
        }
        parameters.put("processed", Payment.Status.PROCESSED.getValue());

        // Create the SQL query
        sql.append(" select member_id, sum(count) as count, sum(amount) as amount");
        sql.append(" from (");
        appendMembersTransactionsSummaryReportSqlPart(sql, useGroups, useTT, useBegin, useEnd, credits, true);
        sql.append(" union");
        appendMembersTransactionsSummaryReportSqlPart(sql, useGroups, useTT, useBegin, useEnd, credits, false);
        sql.append(" ) ts");
        sql.append(" group by member_id");
        sql.append(" order by ").append(order == MemberResultDisplay.NAME ? "member_name, member_id" : "username");

        final SQLQuery query = getSession().createSQLQuery(sql.toString());
        query.addScalar("member_id", StandardBasicTypes.LONG);
        query.addScalar("count", StandardBasicTypes.INTEGER);
        query.addScalar("amount", StandardBasicTypes.BIG_DECIMAL);
        getHibernateQueryHandler().setQueryParameters(query, parameters);

        final Transformer<Object[], MemberTransactionSummaryVO> transformer = new Transformer<Object[], MemberTransactionSummaryVO>() {
            public MemberTransactionSummaryVO transform(final Object[] input) {
                final MemberTransactionSummaryVO vo = new MemberTransactionSummaryVO();
                vo.setMemberId((Long) input[0]);
                vo.setCount((Integer) input[1]);
                vo.setAmount((BigDecimal) input[2]);
                return vo;
            }
        };

        return new ScrollableResultsIterator<MemberTransactionSummaryVO>(query, transformer);
    }

    public List<Account> search(final AccountQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final Set<Relationship> fetch = query.getFetch();
        Class<? extends Account> entityClass = getEntityType();
        if (query.getOwner() != null) {
            if (query.getOwner() instanceof SystemAccountOwner) {
                entityClass = SystemAccount.class;
            } else {
                entityClass = MemberAccount.class;
            }
        }
        final StringBuilder hql = HibernateHelper.getInitialQuery(entityClass, "a", fetch);
        HibernateHelper.addParameterToQuery(hql, namedParameters, "a.type", query.getType());
        if (query.getOwner() instanceof Member) {
            HibernateHelper.addParameterToQuery(hql, namedParameters, "a.member", query.getOwner());
        }
        HibernateHelper.appendOrder(hql, "a.type.name");
        return list(query, hql.toString(), namedParameters);
    }

    @Override
    protected boolean shouldEvictSecondLevelCacheForCollections() {
        return false;
    }

    private void appendMembersTransactionsDetailsReportSqlPart(final StringBuilder sql, final boolean useTT, final boolean useBegin, final boolean useEnd, final boolean credits, final boolean notChargeBack) {
        final boolean flag = notChargeBack ? credits : !credits;
        final String account = flag ? "to_account_id" : "from_account_id";
        final String related = flag ? "from_account_id" : "to_account_id";
        sql.append(" select a.member_id, at.id as account_type_id, at.name account_type_name, t.id transfer_id, t.process_date date, " + (credits ? "" : "-1 * ") + "abs(t.amount) amount, t.description, ra.owner_name related_username, rm.name related_name, tt.name transfer_type_name, t.transaction_number");
        sql.append(" from transfers t inner join accounts a on t.").append(account).append(" = a.id inner join accounts ra on t.").append(related).append(" = ra.id inner join transfer_types tt on t.type_id = tt.id inner join account_types at on a.type_id = at.id left join members rm on ra.member_id = rm.id");
        sql.append(" where t.status = :processed");
        sql.append("   and t.chargeback_of_id is ").append(notChargeBack ? "" : "not ").append("null");
        if (useTT) {
            sql.append("   and t.type_id in (:ttIds)");
        }
        if (useBegin) {
            sql.append("   and t.process_date >= :beginDate");
        }
        if (useEnd) {
            sql.append("   and t.process_date < :endDate");
        }
    }

    private void appendMembersTransactionsSummaryReportSqlPart(final StringBuilder sql, final boolean useGroups, final boolean useTT, final boolean useBegin, final boolean useEnd, final boolean credits, final boolean notChargeBack) {
        final boolean flag = notChargeBack ? credits : !credits;
        final String account = flag ? "to_account_id" : "from_account_id";

        sql.append(" select m.id as member_id, m.name as member_name, u.username, count(t.id) as count, sum(abs(t.amount)) as amount");
        sql.append(" from transfers t inner join accounts a on t.").append(account).append(" = a.id inner join members m on a.member_id = m.id inner join users u on m.id = u.id");
        sql.append(" where t.status = :processed");
        sql.append("   and t.chargeback_of_id is ").append(notChargeBack ? "null" : "not null");
        if (useGroups) {
            sql.append("   and m.group_id in (:groupIds)");
        }
        if (useTT) {
            sql.append("   and t.type_id in (:ttIds)");
        }
        if (useBegin) {
            sql.append("   and t.process_date >= :beginDate");
        }
        if (useEnd) {
            sql.append("   and t.process_date < :endDate");
        }
        sql.append(" group by m.id, m.name, u.username");
    }

    private TransactionSummaryVO buildSummary(final Object object) {
        final Object[] row = (Object[]) object;
        final int count = row[0] == null ? 0 : (Integer) row[0];
        final BigDecimal amount = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
        return new TransactionSummaryVO(count, amount);
    }

    @SuppressWarnings("unchecked")
    private void doLock(final boolean forWrite, List<Long> ids) {
        if (ids == null) {
            return;
        }
        if (!(ids instanceof ArrayList)) {
            ids = new ArrayList<Long>(ids);
        }
        final Map<Long, Boolean> lockedAccounts = CurrentTransactionData.getLockedAccounts();
        for (final Iterator<Long> it = ids.iterator(); it.hasNext();) {
            final Long id = it.next();
            final Boolean currentLock = lockedAccounts.get(id);
            if (currentLock != null) {
                // Already owns lock on the given id. Only allow lock upgrades (had read, wants write). Otherwise, ignore.
                if (!currentLock && forWrite) {
                    // A lock upgrade. Leave it there
                    continue;
                }
                // Already have the lock. Skip this id
                it.remove();
            }
        }
        if (ids.isEmpty()) {
            // Already had all locks
            return;
        }
        final List<Long> lockedIds = getSession()
                .createQuery("select l.id from AccountLock l where l.id in (:ids)")
                .setLockOptions(new LockOptions(forWrite ? LockMode.PESSIMISTIC_WRITE : LockMode.PESSIMISTIC_READ))
                .setParameterList("ids", ids)
                .list();
        for (final Long id : lockedIds) {
            lockedAccounts.put(id, forWrite);
        }
    }

    private Long getAccountId(final AccountOwner owner, final AccountType type) {
        if (owner == null || type == null) {
            throw new UnexpectedEntityException();
        }
        // First look up on the cache
        final CacheKey key = key(owner, type);
        Long id = cachedAccountIds.get(key);
        if (id == null) {
            final AccountQuery query = new AccountQuery();
            query.setUniqueResult();
            query.setOwner(owner);
            query.setType(type);
            final List<Account> list = search(query);
            if (list.isEmpty()) {
                throw new EntityNotFoundException(getEntityType());
            }
            id = list.iterator().next().getId();
            cachedAccountIds.put(key, id);
        }
        return id;
    }

    private TransactionSummaryVO getSummary(final GetTransactionsDTO dto, final boolean credits, final Transfer.Status status) {
        final Account account = load(dto.getOwner(), dto.getType());
        final Member relatedToMember = dto.getRelatedToMember();
        final Element by = dto.getBy();
        final Period period = dto.getPeriod();
        final Collection<PaymentFilter> paymentFilters = dto.getPaymentFilters();

        final StringBuilder hql = new StringBuilder();
        final Map<String, Object> namedParams = new HashMap<String, Object>();
        hql.append(" select count(*), sum(abs(t.amount))");
        hql.append(" from " + Transfer.class.getName() + " t");
        hql.append(" where ((t.amount > 0 and t.").append(credits ? "to" : "from").append(" = :account) ");
        hql.append("  or (t.amount < 0 and t.").append(credits ? "from" : "to").append(" = :account)) ");
        namedParams.put("account", account);
        HibernateHelper.addParameterToQuery(hql, namedParams, "t.status", status);

        // Get only transfers related to (from or to) the specified member
        if (relatedToMember != null) {
            hql.append(" and exists (");
            hql.append("     select ma.id from MemberAccount ma ");
            hql.append("     where ma.member = :relatedToMember ");
            hql.append("     and (t.from = ma or t.to = ma) ");
            hql.append(" )");
            namedParams.put("relatedToMember", relatedToMember);
        }

        // Apply the payments filters
        if (CollectionUtils.isNotEmpty(paymentFilters)) {
            final Set<TransferType> transferTypes = new HashSet<TransferType>();
            for (PaymentFilter paymentFilter : paymentFilters) {
                if (paymentFilter == null || paymentFilter.isTransient()) {
                    continue;
                }
                paymentFilter = getFetchDao().fetch(paymentFilter, PaymentFilter.Relationships.TRANSFER_TYPES);
                if (paymentFilter.getTransferTypes() != null) {
                    transferTypes.addAll(paymentFilter.getTransferTypes());
                }
            }
            if (CollectionUtils.isNotEmpty(transferTypes)) {
                hql.append(" and t.type in (:transferTypes) ");
                namedParams.put("transferTypes", transferTypes);
            }
        }

        // Apply the operated by
        if (by != null) {
            hql.append(" and (t.by = :by or t.receiver = :by)");
            namedParams.put("by", by);
        }

        HibernateHelper.addPeriodParameterToQuery(hql, namedParams, "ifnull(t.processDate,t.date)", period);
        return buildSummary(uniqueResult(hql.toString(), namedParams));
    }

    private CacheKey key(final AccountOwner owner, final AccountType type) {
        final CacheKey key = new CacheKey();
        if (owner instanceof Member) {
            key.memberId = ((Member) owner).getId();
        }
        key.accountTypeId = type.getId();
        return key;
    }
}
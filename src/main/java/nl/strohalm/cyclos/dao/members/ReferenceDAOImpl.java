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
package nl.strohalm.cyclos.dao.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.ScheduledPayment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.GeneralReference;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.PaymentsAwaitingFeedbackQuery;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.entities.members.ReferenceQuery;
import nl.strohalm.cyclos.entities.members.TransactionFeedback;
import nl.strohalm.cyclos.entities.members.Reference.Level;
import nl.strohalm.cyclos.entities.members.Reference.Nature;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;
import nl.strohalm.cyclos.utils.query.PageImpl;
import nl.strohalm.cyclos.utils.query.PageParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.ArrayUtils;

/**
 * Implementation class for reference DAO
 * @author rafael
 */
public class ReferenceDAOImpl extends BaseDAOImpl<Reference> implements ReferenceDAO {

    public ReferenceDAOImpl() {
        super(Reference.class);
    }

    public Map<Level, Integer> countGivenReferencesByLevel(final Reference.Nature nature, final Collection<MemberGroup> memberGroups) {
        return countReferencesByLevel(nature, null, null, memberGroups, false);
    }

    public Map<Level, Integer> countReferencesByLevel(final Reference.Nature nature, final Period period, final Member member, final boolean received) {
        return countReferencesByLevel(nature, period, member, null, received);
    }

    public Map<Level, Integer> countReferencesByLevel(final Reference.Nature nature, final Period period, final Member member, final Collection<MemberGroup> memberGroups, final boolean received) {
        final Map<Level, Integer> countGivenReferences = new EnumMap<Level, Integer>(Level.class);
        for (final Level level : Level.values()) {
            countGivenReferences.put(level, 0);
        }
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final Class<? extends Reference> type = typeForNature(nature);
        final StringBuilder hql = new StringBuilder("select r.level, count(r.id) from ").append(type.getName()).append(" r where 1=1 ");
        HibernateHelper.addParameterToQuery(hql, namedParameters, (received ? "r.to" : "r.from"), member);
        if (memberGroups != null && !memberGroups.isEmpty()) {
            hql.append(" and " + (received ? "r.to" : "r.from") + ".group in (:memberGroups) ");
            namedParameters.put("memberGroups", memberGroups);
        }
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "r.date", period);
        hql.append(" group by r.level order by r.level");
        final List<Object[]> rows = list(hql.toString(), namedParameters);
        for (final Object[] row : rows) {
            countGivenReferences.put((Level) row[0], (Integer) row[1]);
        }
        return countGivenReferences;
    }

    public List<? extends Reference> search(final ReferenceQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final Set<Relationship> fetch = query.getFetch();
        final Nature nature = query.getNature();
        final Class<? extends Reference> type = typeForNature(nature);
        final StringBuilder hql = HibernateHelper.getInitialQuery(type, "r", fetch);
        HibernateHelper.addParameterToQuery(hql, namedParameters, "r.from", query.getFrom());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "r.to", query.getTo());
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "r.date", query.getPeriod());
        if (nature == Nature.TRANSACTION) {
            HibernateHelper.addParameterToQuery(hql, namedParameters, "r.transfer", query.getTransfer());
            HibernateHelper.addParameterToQuery(hql, namedParameters, "r.scheduledPayment", query.getScheduledPayment());
        }
        HibernateHelper.appendOrder(hql, "r.id desc");
        return list(query, hql.toString(), namedParameters);
    }

    @SuppressWarnings("unchecked")
    public List<Payment> searchPaymentsAwaitingFeedback(final PaymentsAwaitingFeedbackQuery query) {
        final Member member = query.getMember();
        final Map<String, ?> namedParameters = Collections.singletonMap("member", member);

        // Store the original result type parameters
        final ResultType resultType = query.getResultType();
        final PageParameters pageParameters = query.getPageParameters();
        query.setResultType(ResultType.LIST);
        query.setPageParameters(PageParameters.max(100));

        final boolean countOnly = resultType == ResultType.PAGE && pageParameters != null && pageParameters.getMaxResults() == 0;

        // Since a transaction feedback may be related to either Transfer or ScheduledPayment, we should perform both queries and join them
        final List<Payment> allPayments = new ArrayList<Payment>();
        int totalCount = 0;

        // Get transfers
        final StringBuilder transferHQL = new StringBuilder();
        transferHQL.append(" select " + (countOnly ? "count(t)" : "t"));
        transferHQL.append(" from Transfer t, MemberAccount ma");
        transferHQL.append(" where t.from = ma");
        transferHQL.append(" and t.type.requiresFeedback = true");
        transferHQL.append(" and t.date >= t.type.feedbackEnabledSince");
        transferHQL.append(" and t.parent is null");
        transferHQL.append(" and t.processDate is not null");
        transferHQL.append(" and not exists (");
        transferHQL.append("     select tf.id ");
        transferHQL.append("     from TransactionFeedback tf");
        transferHQL.append("     where tf.transfer = t ");
        transferHQL.append(" )");
        if (member != null) {
            transferHQL.append(" and ma.member = :member");
        }
        if (countOnly) {
            totalCount += CoercionHelper.coerce(int.class, uniqueResult(transferHQL.toString(), namedParameters));
        } else {
            HibernateHelper.appendOrder(transferHQL, "t.date");
            final List<Transfer> transfers = list(query, transferHQL.toString(), namedParameters);
            allPayments.addAll(transfers);
        }

        // Get scheduled payments
        final StringBuilder scheduledPaymentHQL = new StringBuilder();
        scheduledPaymentHQL.append(" select sp");
        scheduledPaymentHQL.append(" from ScheduledPayment sp, MemberAccount ma");
        scheduledPaymentHQL.append(" where sp.from = ma");
        scheduledPaymentHQL.append(" and sp.type.requiresFeedback = true");
        scheduledPaymentHQL.append(" and sp.date >= sp.type.feedbackEnabledSince");
        scheduledPaymentHQL.append(" and not exists (");
        scheduledPaymentHQL.append("     select tf.id ");
        scheduledPaymentHQL.append("     from TransactionFeedback tf");
        scheduledPaymentHQL.append("     where tf.scheduledPayment = sp ");
        scheduledPaymentHQL.append(" )");
        if (member != null) {
            scheduledPaymentHQL.append(" and ma.member = :member");
        }
        if (countOnly) {
            totalCount += CoercionHelper.coerce(int.class, uniqueResult(scheduledPaymentHQL.toString(), namedParameters));
        } else {
            HibernateHelper.appendOrder(scheduledPaymentHQL, "sp.date");
            final List<ScheduledPayment> scheduledPayments = list(query, scheduledPaymentHQL.toString(), namedParameters);
            allPayments.addAll(scheduledPayments);
        }

        if (countOnly) {
            return new PageImpl<Payment>(pageParameters, totalCount, Collections.<Payment> emptyList());
        } else {
            // Apply fetch
            final Relationship[] fetch = query.getFetch().toArray(new Relationship[query.getFetch().size()]);
            if (ArrayUtils.isNotEmpty(fetch)) {
                for (int i = 0; i < allPayments.size(); i++) {
                    Payment payment = allPayments.get(i);
                    payment = getFetchDao().fetch(payment, fetch);
                    allPayments.set(i, payment);
                }
            }
            Collections.sort(allPayments, new BeanComparator("date"));
            return getHibernateQueryHandler().applyResultParameters(resultType, pageParameters, allPayments);
        }
    }

    private Class<? extends Reference> typeForNature(final Nature nature) {
        Class<? extends Reference> type;
        if (nature == Reference.Nature.TRANSACTION) {
            type = TransactionFeedback.class;
        } else if (nature == Reference.Nature.GENERAL) {
            type = GeneralReference.class;
        } else {
            type = Reference.class;
        }
        return type;
    }

}
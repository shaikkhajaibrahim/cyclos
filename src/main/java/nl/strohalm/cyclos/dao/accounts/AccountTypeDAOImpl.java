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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.accounts.transactions.TransferTypeDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.AccountTypeQuery;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.accounts.MemberAccountTypeQuery;
import nl.strohalm.cyclos.services.accounts.SystemAccountTypeQuery;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;

import org.apache.commons.lang.ArrayUtils;

/**
 * Implementation dao for account types
 * @author rafael
 */
public class AccountTypeDAOImpl extends BaseDAOImpl<AccountType> implements AccountTypeDAO {

    private TransferTypeDAO transferTypeDao;

    public AccountTypeDAOImpl() {
        super(AccountType.class);
    }

    @Override
    public int delete(final boolean flush, final Long... ids) {
        // We must remove the many-to-many inverse relationships manually
        int rows = 0;
        for (final Long id : ids) {
            try {
                final AccountType accountType = load(id, AccountType.Relationships.FROM_TRANSFER_TYPES, AccountType.Relationships.TO_TRANSFER_TYPES);
                // Delete the transfer types manually
                final Long[] ttIds = (Long[]) ArrayUtils.addAll(EntityHelper.toIds(accountType.getFromTransferTypes()), EntityHelper.toIds(accountType.getToTransferTypes()));
                transferTypeDao.delete(ttIds);
                // Remove the relationship with admin groups
                if (accountType instanceof SystemAccountType) {
                    final SystemAccountType systemAccountType = (SystemAccountType) accountType;
                    for (final AdminGroup group : systemAccountType.getViewedByGroups()) {
                        group.getViewInformationOf().remove(systemAccountType);
                    }
                }
                // Delete the transaction fees
                if (accountType instanceof MemberAccountType) {
                    getHibernateTemplate().bulkUpdate("delete from " + AccountFee.class.getName() + " e where e.accountType = ?", accountType);
                }
                getHibernateTemplate().refresh(accountType);
                getHibernateTemplate().delete(accountType);
                rows++;
            } catch (final EntityNotFoundException e) {
                // Ignore
            }
        }
        return rows;
    }

    public BigDecimal getBalance(final AccountType accountType) {
        final BigDecimal credits = getSum(accountType, true);
        final BigDecimal debits = getSum(accountType, false);
        return credits.subtract(debits);
    }

    public List<? extends AccountType> listAll() {
        return list("from AccountType at order by at.name", null);
    }

    public List<AccountType> search(final AccountTypeQuery query) {
        final Class<? extends AccountType> entityType = (query instanceof SystemAccountTypeQuery) ? SystemAccountType.class : MemberAccountType.class;
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final Set<Relationship> fetch = query.getFetch();
        final StringBuilder hql = HibernateHelper.getInitialQuery(entityType, "at", fetch);
        HibernateHelper.addLikeParameterToQuery(hql, namedParameters, "at.description", query.getDescription());
        HibernateHelper.addLikeParameterToQuery(hql, namedParameters, "at.name", query.getName());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "at.currency", query.getCurrency());
        // Handle nature-specific parameters
        if (query instanceof SystemAccountTypeQuery) {
            // System accounts
            final SystemAccountTypeQuery systemQuery = (SystemAccountTypeQuery) query;
            if (systemQuery.getLimited() != null) {
                if (systemQuery.getLimited()) {
                    hql.append(" and at.account.creditLimit is not null ");
                } else {
                    hql.append(" and at.account.creditLimit is null ");
                }
            }
        } else {
            // Member accounts
            final MemberAccountTypeQuery memberQuery = (MemberAccountTypeQuery) query;

            if (memberQuery.getOwner() != null) {
                final Member member = getFetchDao().fetch(memberQuery.getOwner(), Element.Relationships.GROUP);
                memberQuery.setRelatedToGroup(member.getMemberGroup());
            }
            if (memberQuery.getRelatedToGroups() != null && !memberQuery.getRelatedToGroups().isEmpty()) {
                hql.append(" and exists (select mgaso.id from " + MemberGroupAccountSettings.class.getName() + " mgaso where mgaso.group in (:relatedGroups) and mgaso.accountType = at)");
                namedParameters.put("relatedGroups", memberQuery.getRelatedToGroups());
            }
            if (memberQuery.getNotRelatedToGroups() != null && !memberQuery.getNotRelatedToGroups().isEmpty()) {
                hql.append(" and not exists (select mgaso.id from " + MemberGroupAccountSettings.class.getName() + " mgaso where mgaso.group in (:notRelatedGroups) and mgaso.accountType = at)");
                namedParameters.put("notRelatedGroups", memberQuery.getNotRelatedToGroups());
            }
        }
        HibernateHelper.appendOrder(hql, "at.class", "at.name");
        return list(query, hql.toString(), namedParameters);
    }

    public void setTransferTypeDao(final TransferTypeDAO transferTypeDao) {
        this.transferTypeDao = transferTypeDao;
    }

    private BigDecimal getSum(final AccountType accountType, final boolean credits) {
        final StringBuilder hql = new StringBuilder();
        final Map<String, Object> namedParams = new HashMap<String, Object>();
        hql.append(" select sum(t.amount)");
        hql.append(" from Transfer t");
        hql.append(" where t.").append(credits ? "to" : "from").append(".type = :accountType ");
        namedParams.put("accountType", accountType);
        final BigDecimal result = uniqueResult(hql.toString(), namedParams);
        if (result == null) {
            return BigDecimal.ZERO;
        } else {
            return result;
        }
    }

}
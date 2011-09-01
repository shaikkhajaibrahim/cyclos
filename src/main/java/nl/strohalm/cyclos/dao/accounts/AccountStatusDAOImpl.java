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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.services.accounts.rates.RateService;
import nl.strohalm.cyclos.utils.DataIteratorHelper;

/**
 * Implementation for AccountStatusDAO
 * @author luis
 */
public class AccountStatusDAOImpl extends BaseDAOImpl<AccountStatus> implements AccountStatusDAO {

    public AccountStatusDAOImpl() {
        super(AccountStatus.class);
    }

    public Integer countNullRateFields(final Account account, final Calendar date, final RateService.NullType nullType) {
        final StringBuilder hql = new StringBuilder();
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        hql.append(" select count(*)");
        hql.append(" from AccountStatus s");
        hql.append(" where s.account = :account ");
        hql.append(" and s.date >= :date ");
        if (nullType == RateService.NullType.A_RATE) {
            hql.append(" and s.emissionDate is null ");
        }
        if (nullType == RateService.NullType.D_RATE) {
            hql.append(" and (s.dRate is null or s.lastDRateUpdate is null) ");
        }
        if (nullType == RateService.NullType.RBC) {
            hql.append(" and s.rateBalanceCorrection is null ");
        }

        namedParameters.put("account", account);
        namedParameters.put("date", date);
        final Integer count = (Integer) (uniqueResult(hql.toString(), namedParameters));
        return count;
    }

    public AccountStatus getByDate(final Account account, final Calendar date, final Relationship... fetch) throws EntityNotFoundException {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("account", account);
        final StringBuilder hql = new StringBuilder();
        hql.append("from AccountStatus s where s.account = :account");
        if (date != null) {
            params.put("date", date);
            hql.append(" and s.date <= :date");
        }
        hql.append(" order by s.date desc, s.id desc");
        final AccountStatus status = uniqueResult(hql.toString(), params);
        if (status == null) {
            throw new EntityNotFoundException(getEntityType());
        }
        return getFetchDao().fetch(status, fetch);
    }

    public void removeStatusRelatedTo(final AccountFeeLog log) {
        getHibernateTemplate().bulkUpdate("delete from MemberAccountStatus s where s.accountFeeLog = ?", log);
    }

    public void updateStatusesInFuture(final AccountStatus status) {
        final Transfer transfer = status.getTransfer();
        final Account from = transfer.getFrom();
        final Account account = status.getAccount();

        // Find out which update will be executed
        final boolean isDebit = account.equals(from);
        final boolean isRoot = transfer.isRoot();
        final BigDecimal amount = transfer.getAmount();

        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("date", status.getDate());
        params.put("account", account);
        params.put("id", status.getId());
        final Iterator<AccountStatus> iterator = iterate("from AccountStatus s where s.account = :account and s.date > :date and s.id < :id", params);
        try {
            while (iterator.hasNext()) {
                final AccountStatus current = iterator.next();
                if (transfer.getProcessDate() == null) {
                    // Pending
                    if (isDebit) {
                        current.setPendingDebits(current.getPendingDebits().add(amount));
                    }
                } else {
                    // Processed
                    if (isRoot) {
                        // Root
                        if (isDebit) {
                            current.setRootDebits(current.getRootDebits().add(amount));
                        } else {
                            current.setRootCredits(current.getRootCredits().add(amount));
                        }
                    } else {
                        // Nested
                        if (isDebit) {
                            current.setNestedDebits(current.getNestedDebits().add(amount));
                        } else {
                            current.setNestedCredits(current.getNestedCredits().add(amount));
                        }
                    }
                }
            }
        } finally {
            DataIteratorHelper.close(iterator);
        }
        getHibernateTemplate().flush();
    }
}

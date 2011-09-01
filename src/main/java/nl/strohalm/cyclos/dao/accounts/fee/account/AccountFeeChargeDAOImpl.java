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
package nl.strohalm.cyclos.dao.accounts.fee.account;

import static nl.strohalm.cyclos.utils.BigDecimalHelper.nvl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeCharge;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.utils.query.IteratorList;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Implementation for AccountFeeChargeDAO
 * @author luis
 */
public class AccountFeeChargeDAOImpl extends BaseDAOImpl<AccountFeeCharge> implements AccountFeeChargeDAO {

    public AccountFeeChargeDAOImpl() {
        super(AccountFeeCharge.class);
    }

    public void deleteOnPeriod(final MemberAccount account, final AccountFeeLog log) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("account", account);
        namedParameters.put("log", log);
        final String hql = "delete from AccountFeeCharge c where c.account = :account and c.accountFeeLog = :log";
        executeUpdate(hql, namedParameters);
    }

    public AccountFeeCharge forData(final MemberAccount account, final AccountFeeLog accountFeeLog, final Calendar date) throws EntityNotFoundException {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("account", account);
        params.put("accountFeeLog", accountFeeLog);
        params.put("date", date);
        final AccountFeeCharge charge = uniqueResult("from AccountFeeCharge c where c.account = :account and c.accountFeeLog = :accountFeeLog and c.period.end = :date", params);
        if (charge == null) {
            throw new EntityNotFoundException(getEntityType());
        }
        return charge;
    }

    @SuppressWarnings("unchecked")
    public IteratorList<AccountFeeCharge> listChargesInTolerance(final MemberAccount account, final Calendar date, final AccountFeeLog accountFeeLog) {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("account", account);
        params.put("accountFeeLog", accountFeeLog);
        params.put("beginDate", accountFeeLog.getTolerance().remove(date));
        params.put("endDate", date);
        final StringBuilder hql = new StringBuilder();
        hql.append(" from AccountFeeCharge c");
        hql.append(" where c.account = :account");
        hql.append(" and c.accountFeeLog = :accountFeeLog");
        hql.append(" and c.period.begin >= :beginDate");
        hql.append(" and c.period.end <= :endDate");
        hql.append(" order by c.period.end desc");
        return (IteratorList) list(ResultType.ITERATOR, hql.toString(), params, null);
    }

    public BigDecimal totalAmoutForPeriod(final MemberAccount account, final AccountFeeLog accountFeeLog) {
        final Map<String, Object> params = new HashMap<String, Object>();
        params.put("account", account);
        params.put("accountFeeLog", accountFeeLog);
        final BigDecimal amount = uniqueResult("select sum(c.amount) from AccountFeeCharge c where c.account = :account and c.accountFeeLog = :accountFeeLog", params);
        return nvl(amount);
    }

}

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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.PendingAccountStatus;
import nl.strohalm.cyclos.utils.query.PageParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * Implementation for PendingAccountStatusDAO
 * @author luis
 */
public class PendingAccountStatusDAOImpl extends BaseDAOImpl<PendingAccountStatus> implements PendingAccountStatusDAO {

    public PendingAccountStatusDAOImpl() {
        super(PendingAccountStatus.class);
    }

    public int count() {
        final Number count = uniqueResult("select count(*) from PendingAccountStatus s", null);
        return count.intValue();
    }

    public Iterator<PendingAccountStatus> iterateFor(final Account account) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        for (final PendingAccountStatus.Type type : PendingAccountStatus.Type.values()) {
            namedParameters.put(type.name(), type);
        }
        namedParameters.put("account", account);
        final StringBuilder hql = new StringBuilder();
        hql.append(" select s");
        hql.append(" from PendingAccountStatus s");
        hql.append("     left join fetch s.transfer t");
        hql.append("         left join fetch t.from tfa");
        hql.append("         left join fetch t.to tta");
        hql.append("     left join fetch s.transferAuthorization a");
        hql.append("     left join fetch s.scheduledPayment sp");
        hql.append("         left join fetch sp.from spfa");
        hql.append("     left join fetch s.account a");
        hql.append(" where (");
        hql.append("    ((tfa = :account or tta = :account) and s.type = :PAYMENT) or");
        hql.append("    (spfa = :account and s.type = :RESERVED_SCHEDULED_PAYMENT) or");
        hql.append("    (a = :account and s.type in (:LIMIT_CHANGE, :ACCOUNT_FEE_DISABLED, :ACCOUNT_FEE_INVOICE, :AUTHORIZATION))");
        hql.append(" )");
        hql.append(" order by s.id");
        return iterate(hql.toString(), namedParameters);
    }

    public List<PendingAccountStatus> next(final int count) {
        final StringBuilder hql = new StringBuilder();
        hql.append(" select s");
        hql.append(" from PendingAccountStatus s");
        hql.append("     left join fetch s.transfer t");
        hql.append("         left join fetch t.from tfa");
        hql.append("         left join fetch t.to tta");
        hql.append("     left join fetch s.transferAuthorization a");
        hql.append("     left join fetch s.scheduledPayment sp");
        hql.append("         left join fetch sp.from spfa");
        hql.append("     left join fetch s.account a");
        hql.append(" order by s.id");
        return list(ResultType.LIST, hql.toString(), null, PageParameters.max(count));
    }
}

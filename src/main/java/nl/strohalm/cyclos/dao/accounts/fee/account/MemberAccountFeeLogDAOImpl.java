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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.MemberAccountFeeLog;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;

/**
 * Implementation for {@link MemberAccountFeeLogDAO}
 * 
 * @author luis
 */
public class MemberAccountFeeLogDAOImpl extends BaseDAOImpl<MemberAccountFeeLog> implements MemberAccountFeeLogDAO {

    public MemberAccountFeeLogDAOImpl() {
        super(MemberAccountFeeLog.class);
    }

    public List<Member> nextToCharge(final AccountFeeLog feeLog, final int count) {
        final Collection<MemberGroup> groups = feeLog.getAccountFee().getGroups();
        if (groups.isEmpty()) {
            // Ensure no members are charged, as there are no groups
            return Collections.emptyList();
        }
        final Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("log", feeLog);
        final StringBuilder hql = HibernateHelper.getInitialQuery(Member.class, "m", Arrays.<Relationship> asList(Element.Relationships.USER));
        HibernateHelper.addInParameterToQuery(hql, parameters, "m.group", groups);
        // Only members activated after the fee period will be charged
        HibernateHelper.addParameterToQueryOperator(hql, parameters, "m.activationDate", ">=", DateHelper.getBeginForParameter(feeLog.getPeriod()));
        // Ensure the member was never charged (nor received, in case of system to member fees), nor received an invoice for the given log
        hql.append(" and not exists (");
        hql.append("     select 1");
        hql.append("     from MemberAccountFeeLog ml");
        hql.append("     where ml.member = m and ml.accountFeeLog = :log");
        hql.append(" )");
        return getHibernateQueryHandler().simpleList(null, hql.toString(), parameters, null, count);
    }

}

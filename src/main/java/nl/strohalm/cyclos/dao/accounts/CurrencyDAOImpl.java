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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.groups.MemberGroup;

/**
 * DAO implementation for currency
 * @author luis
 */
public class CurrencyDAOImpl extends BaseDAOImpl<Currency> implements CurrencyDAO {

    public CurrencyDAOImpl() {
        super(Currency.class);
    }

    @Override
    public int delete(final boolean flush, final Long... ids) {
        for (final Long id : ids) {
            final Map<String, Object> namedParameters = new HashMap<String, Object>();
            final Currency currency = load(id);
            currency.setaRateParameters(null);
            currency.setdRateParameters(null);
            update(currency);
            
            namedParameters.put("currency", id);

            // delete all related rates
            executeUpdate("delete from ARateParameters a where a.currency.id = :currency", namedParameters);
            executeUpdate("delete from DRateParameters d where d.currency.id = :currency", namedParameters);
        }
        return super.delete(flush, ids);
    }

    public List<Currency> listAll() {
        return list("from Currency c order by c.name", null);
    }

    public List<Currency> listByMemberGroup(final MemberGroup group) {
        final String hql = "from Currency c where exists (select mgas.id from MemberGroupAccountSettings mgas where mgas.group = :group and mgas.accountType.currency = c) order by c.name";
        final Map<String, ?> namedParameters = Collections.singletonMap("group", group);
        return list(hql, namedParameters);
    }
}

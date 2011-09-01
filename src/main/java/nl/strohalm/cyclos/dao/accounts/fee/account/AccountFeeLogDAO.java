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

import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.DeletableDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.UpdatableDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeCharge;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLogQuery;

/**
 * Interface for account fee log DAO
 * 
 * @author rafael
 * @author fireblade
 */
public interface AccountFeeLogDAO extends BaseDAO<AccountFeeLog>, InsertableDAO<AccountFeeLog>, UpdatableDAO<AccountFeeLog>, DeletableDAO<AccountFeeLog> {

    /**
     * Returns an existing log for the given fee and date
     * @throws EntityNotFoundException When no such log exists
     */
    AccountFeeLog forDate(AccountFee accountFee, Calendar date) throws EntityNotFoundException;

    /**
     * Iterates over all member accounts with {@link AccountFeeCharge}s related to the given {@link AccountFeeLog}
     */
    Iterator<MemberAccount> iterateOverAccountsWithAccountFeeChargesFor(AccountFeeLog log);

    /**
     * Searches for account fee logs, ordering by date descending. If no entity can be found, returns an empty List. If any exception is thrown by the
     * underlying implementation, it should be wrapped by a DaoException.
     * 
     * @throws DaoException
     */
    List<AccountFeeLog> search(AccountFeeLogQuery queryParameters) throws DaoException;
}

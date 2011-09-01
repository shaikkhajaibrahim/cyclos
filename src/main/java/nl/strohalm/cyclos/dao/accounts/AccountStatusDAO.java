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

import java.util.Calendar;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.services.accounts.rates.RateService;

/**
 * Data access interface for account status
 * @author luis
 */
public interface AccountStatusDAO extends BaseDAO<AccountStatus>, InsertableDAO<AccountStatus> {

    /**
     * counts how many records with null fields exist on a particular rate field for an account after a certain date.
     * @param account the account to be examined
     * @param date the null rates after this date are counted for the account
     * @param nullType indicates the field on accountStatus to be examined.
     * @return the number of records with a found null rate.
     */
    public Integer countNullRateFields(Account account, Calendar date, RateService.NullType nullType);

    /**
     * Returns the last status for the given account and date. When date is null, the last status is returned
     * @throws EntityNotFoundException When there are no account status for the given account
     */
    AccountStatus getByDate(Account account, Calendar date, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Removes all account statuses related to the given account fee log
     */
    void removeStatusRelatedTo(AccountFeeLog log);

    /**
     * Updates all account status for dates greater than this one. WARNING: does NOT update rates: too complicated. Therefore, payments in past MUST
     * be disabled when rates are enabled.
     */
    void updateStatusesInFuture(AccountStatus status);

}

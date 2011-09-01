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

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeCharge;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.utils.query.IteratorList;

/**
 * Data access interface for account fee charges.
 * @author luis
 */
public interface AccountFeeChargeDAO extends BaseDAO<AccountFeeCharge>, InsertableDAO<AccountFeeCharge> {

    /**
     * Deletes all {@link AccountFeeCharge}s for the given account and log
     */
    void deleteOnPeriod(MemberAccount account, AccountFeeLog log);

    /**
     * Returns the AccountFeeCharge for the given account and fee log, at the exact fee log period's begin
     * @throws EntityNotFoundException When no such charge exists
     */
    AccountFeeCharge forData(MemberAccount account, AccountFeeLog feeLog, Calendar date) throws EntityNotFoundException;

    /**
     * Lists the charges within the tolerance period, returning the result in form of an {@link IteratorList}
     */
    IteratorList<AccountFeeCharge> listChargesInTolerance(MemberAccount account, Calendar date, AccountFeeLog accountFeeLog);

    /**
     * Returns the total charged amount for a given account, account fee and period
     */
    BigDecimal totalAmoutForPeriod(MemberAccount account, AccountFeeLog feeLog);
}

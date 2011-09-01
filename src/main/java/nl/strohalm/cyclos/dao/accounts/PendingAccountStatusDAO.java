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

import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.DeletableDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.PendingAccountStatus;

/**
 * Data access interface for pending account status
 * @author luis
 */
public interface PendingAccountStatusDAO extends BaseDAO<PendingAccountStatus>, InsertableDAO<PendingAccountStatus>, DeletableDAO<PendingAccountStatus> {

    /**
     * Returns the number of pending statuses
     */
    int count();

    /**
     * Iterates through all existing pending account statuses for the given account. The transfer (and it's from and to accounts), the scheduled
     * payment (and it's from account) and the accounts are eagerly fetched.
     */
    Iterator<PendingAccountStatus> iterateFor(Account account);

    /**
     * Returns the next pending statuses to be processed. The transfer (and it's from and to accounts), the scheduled payment (and it's from account)
     * and the accounts are eagerly fetched.
     */
    List<PendingAccountStatus> next(int count);

}

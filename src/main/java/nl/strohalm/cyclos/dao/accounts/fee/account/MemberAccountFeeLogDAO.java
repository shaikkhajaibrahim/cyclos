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

import java.util.List;

import nl.strohalm.cyclos.dao.BaseDAO;
import nl.strohalm.cyclos.dao.InsertableDAO;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.MemberAccountFeeLog;
import nl.strohalm.cyclos.entities.members.Member;

/**
 * DAO interface for member account fee logs
 * 
 * @author luis
 */
public interface MemberAccountFeeLogDAO extends BaseDAO<MemberAccountFeeLog>, InsertableDAO<MemberAccountFeeLog> {

    /**
     * Returns the next members which should be charged by the given account fee log
     */
    List<Member> nextToCharge(AccountFeeLog log, int count);

}

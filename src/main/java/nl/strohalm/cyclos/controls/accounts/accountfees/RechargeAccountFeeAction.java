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
package nl.strohalm.cyclos.controls.accounts.accountfees;

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Action used to recharge a failed an account fee log
 * @author luis
 */
public class RechargeAccountFeeAction extends BaseAccountFeeExecutionAction {

    @Override
    protected void executeAction(final ActionContext context, final AccountFee fee, final AccountFeeLog log) {
        if (log == null) {
            throw new ValidationException();
        }
        getAccountFeeService().rechargeFailed(log);
        context.sendMessage("accountFee.action.recharged");
    }
}

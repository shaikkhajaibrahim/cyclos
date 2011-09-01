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

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLog;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeLogQuery;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFeeQuery;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.services.accountfees.AccountFeeService;
import nl.strohalm.cyclos.utils.RelationshipHelper;

import org.apache.struts.action.ActionForward;

/**
 * Action used to list an account fee log
 */
public class ListAccountFeeLogAction extends BaseAction {

    private AccountFeeService accountFeeService;

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();

        // Groups managed by the admin group
        AdminGroup adminGroup = context.getGroup();
        adminGroup = getFetchService().fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        final Collection<MemberGroup> managedGroups = (adminGroup.getManagesGroups());

        final AccountFeeQuery feeQuery = new AccountFeeQuery();
        feeQuery.fetch(AccountFee.Relationships.LOGS, RelationshipHelper.nested(AccountFee.Relationships.ACCOUNT_TYPE, AccountType.Relationships.CURRENCY));
        feeQuery.setReturnDisabled(false);
        feeQuery.setGroups(managedGroups);
        final List<AccountFee> fees = accountFeeService.search(feeQuery);

        feeQuery.setReturnDisabled(true);
        final AccountFeeLogQuery logQuery = new AccountFeeLogQuery();
        logQuery.setAccountFees(accountFeeService.search(feeQuery));
        final List<AccountFeeLog> logs = accountFeeService.searchLogs(logQuery);

        request.setAttribute("accountFees", fees);
        request.setAttribute("accountFeeLogs", logs);

        return context.getInputForward();
    }

    public AccountFeeService getAccountFeeService() {
        return accountFeeService;
    }

    @Inject
    public void setAccountFeeService(final AccountFeeService accountFeeService) {
        this.accountFeeService = accountFeeService;
    }

}
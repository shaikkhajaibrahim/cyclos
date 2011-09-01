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
package nl.strohalm.cyclos.controls.members.brokering;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.services.elements.CommissionService;
import nl.strohalm.cyclos.utils.ActionHelper;

import org.apache.struts.action.ActionForward;

/**
 * Action used to remove a broker commission contract
 * @author Jefferson Magno
 */
public class RemoveBrokerCommissionContractAction extends BaseAction {

    private CommissionService commissionService;

    @Inject
    public void setCommissionService(final CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final RemoveBrokerCommissionContractForm form = context.getForm();
        final long brokerCommissionContractId = form.getBrokerCommissionContractId();
        final long memberId = form.getMemberId();

        // Parameters to redirect to the appropriate page
        ActionForward actionForward = null;
        String paramName = null;
        Long paramValue = null;

        try {
            commissionService.removeBrokerCommissionContracts(brokerCommissionContractId);
            context.sendMessage("brokerCommissionContract.removed");
            paramName = "memberId";
            paramValue = memberId;
            if (context.getNavigation().contains("/member/editBrokerCommissionContract")) {
                context.getNavigation().back(); // Pop the edit page, so we can return to the correct page
            }

            actionForward = new ActionForward("/do" + context.getNavigation().back());
        } catch (final Exception e) {
            context.sendMessage("brokerCommissionContract.error.removing");
            paramName = "brokerCommissionContractId";
            paramValue = brokerCommissionContractId;
            actionForward = context.getSuccessForward();
        }
        return ActionHelper.redirectWithParam(request, actionForward, paramName, paramValue);
    }

}
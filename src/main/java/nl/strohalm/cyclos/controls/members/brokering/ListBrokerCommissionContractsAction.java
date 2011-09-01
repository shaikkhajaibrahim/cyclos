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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseQueryAction;
import nl.strohalm.cyclos.entities.accounts.AccountType.Nature;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.BrokerCommission;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFeeQuery;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContract;
import nl.strohalm.cyclos.entities.members.brokerings.BrokerCommissionContractQuery;
import nl.strohalm.cyclos.services.elements.CommissionChargeStatusDTO;
import nl.strohalm.cyclos.services.elements.CommissionService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.transfertypes.TransactionFeeService;
import nl.strohalm.cyclos.utils.query.QueryParameters;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * List broker commission contracts
 * @author Jefferson Magno
 */
public class ListBrokerCommissionContractsAction extends BaseQueryAction {

    private ElementService        elementService;
    private CommissionService     commissionService;
    private TransactionFeeService transactionFeeService;

    public ElementService getElementService() {
        return elementService;
    }

    public TransactionFeeService getTransactionFeeService() {
        return transactionFeeService;
    }

    @Inject
    public void setCommissionService(final CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setTransactionFeeService(final TransactionFeeService transactionFeeService) {
        this.transactionFeeService = transactionFeeService;
    }

    @Override
    protected void executeQuery(final ActionContext context, final QueryParameters queryParameters) {
        final BrokerCommissionContractQuery query = (BrokerCommissionContractQuery) queryParameters;
        final List<BrokerCommissionContract> brokerCommissionContracts = commissionService.searchBrokerCommissionContracts(query);
        context.getRequest().setAttribute("brokerCommissionContracts", brokerCommissionContracts);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected QueryParameters prepareForm(final ActionContext context) {
        final HttpServletRequest request = context.getRequest();
        final ListBrokerCommissionContractsForm form = context.getForm();

        Member broker = null;
        Member member = null;
        boolean myContracts = false;

        final long memberId = form.getMemberId();
        if (memberId > 1) {
            // Broker viewing contracts and commissions charge status related to a member
            member = (Member) elementService.load(memberId, Element.Relationships.GROUP);
            if (!context.isBrokerOf(member)) {
                throw new ValidationException();
            }
            broker = context.getElement();
        } else {
            // Member viewing contracts and commissions charge status related to himself
            member = context.getElement();
            myContracts = true;
        }
        request.setAttribute("member", member);
        request.setAttribute("broker", broker);
        request.setAttribute("myContracts", myContracts);
        
        final List<CommissionChargeStatusDTO> commissionChargeStatusList = commissionService.getCommissionChargeStatus(member);
        request.setAttribute("commissionChargeStatusList", commissionChargeStatusList);

        if (context.isBroker() && memberId > 1) {
            final BrokerGroup brokerGroup = (BrokerGroup) broker.getGroup();
            final MemberGroup memberGroup = (MemberGroup) member.getGroup();

            // Get broker commission transaction fees related to the broker group
            final TransactionFeeQuery transactionFeeQuery = new TransactionFeeQuery();
            transactionFeeQuery.setGeneratedTransferTypeFromNature(Nature.MEMBER);
            transactionFeeQuery.setEntityType(BrokerCommission.class);
            transactionFeeQuery.setBrokerGroup(brokerGroup);
            transactionFeeQuery.setMemberGroup(memberGroup);

            final List<BrokerCommission> brokerCommissions = (List<BrokerCommission>) transactionFeeService.search(transactionFeeQuery);
            request.setAttribute("brokerCommissions", brokerCommissions);
        }

        final BrokerCommissionContractQuery query = new BrokerCommissionContractQuery();
        query.setBroker(broker);
        query.setMember(member);
        return query;
    }

    @Override
    protected boolean willExecuteQuery(final ActionContext context, final QueryParameters queryParameters) throws Exception {
        return true;
    }

}
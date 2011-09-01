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
package nl.strohalm.cyclos.controls.members.loangroups;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.accounts.loans.LoanGroupQuery;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.loangroups.LoanGroupService;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to list the loan groups of a given member
 * @author luis
 */
public class MemberLoanGroupsAction extends BaseAction {

    private ElementService   elementService;
    private LoanGroupService loanGroupService;

    public ElementService getElementService() {
        return elementService;
    }

    public LoanGroupService getLoanGroupService() {
        return loanGroupService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setLoanGroupService(final LoanGroupService loanGroupService) {
        this.loanGroupService = loanGroupService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final MemberLoanGroupsForm form = context.getForm();
        final long id = form.getMemberId();
        boolean myLoanGroups = false;
        boolean byBroker = false;
        boolean editable = false;
        Member member = null;
        final Element loggedElement = context.getElement();
        if (id <= 0L || loggedElement.getId().equals(id)) {
            if (context.isMember()) {
                member = context.getElement();
                myLoanGroups = true;
            }
        } else {
            try {
                member = (Member) elementService.load(id, Element.Relationships.USER);
                if (context.isMember()) {
                    if (!context.isBrokerOf(member)) {
                        throw new PermissionDeniedException();
                    } else {
                        byBroker = true;
                    }
                } else {
                    editable = getPermissionService().checkPermission("adminMemberLoanGroups", "manage");
                }
            } catch (final PermissionDeniedException e) {
                throw e;
            } catch (final Exception e) {
                member = null;
            }
        }
        if (member == null) {
            throw new ValidationException();
        }
        if (editable) {
            final LoanGroupQuery query = new LoanGroupQuery();
            query.setMember(member);
            query.setNotOfMember(true);
            request.setAttribute("unrelatedLoanGroups", loanGroupService.searchForMember(query));
        }

        final LoanGroupQuery query = new LoanGroupQuery();
        query.setMember(member);
        request.setAttribute("loanGroups", loanGroupService.searchForMember(query));

        request.setAttribute("member", member);
        request.setAttribute("myLoanGroups", myLoanGroups);
        request.setAttribute("byBroker", byBroker);
        request.setAttribute("editable", editable);

        return context.getInputForward();
    }

}

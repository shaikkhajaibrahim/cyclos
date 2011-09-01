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
package nl.strohalm.cyclos.controls.members.activities;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.services.elements.ActivitiesVO;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action to retrieve a member's activities
 * @author luis
 * @author Jefferson Magno
 */
public class ActivitiesAction extends BaseAction {

    private ElementService elementService;
    private MemberService  memberService;

    public ElementService getElementService() {
        return elementService;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setMemberService(final MemberService memberService) {
        this.memberService = memberService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ActivitiesForm form = context.getForm();

        boolean myActivities = false;
        boolean byBroker = false;
        boolean byMembersOperator = false;
        boolean showAccountInformation;

        Member member;
        if (form.getMemberId() <= 0) {
            if (context.isMember()) {
                member = context.getElement();
                myActivities = true;
                showAccountInformation = true;
            } else if (context.isOperator()) {
                member = ((Operator) context.getElement()).getMember();
                byMembersOperator = true;
                showAccountInformation = getPermissionService().checkPermission("operatorAccount", "accountInformation");
            } else {
                throw new ValidationException();
            }
        } else {
            final Element element = elementService.load(form.getMemberId(), Element.Relationships.USER, Element.Relationships.GROUP);
            if (!(element instanceof Member)) {
                throw new ValidationException();
            }
            member = (Member) element;
            Group group;
            String module;
            String operation;
            if (context.isAdmin()) {
                group = context.getGroup();
                module = "adminMemberReports";
                operation = "showAccountInformation";
            } else if (context.isOperator()) {
                final Operator operator = getFetchService().fetch((Operator) context.getElement(), Operator.Relationships.MEMBER);
                if (member.equals(operator.getMember())) {
                    byMembersOperator = true;
                    group = context.getGroup();
                    module = "operatorAccount";
                    operation = "accountInformation";
                } else {
                    group = ((Member) context.getAccountOwner()).getGroup();
                    module = "memberReports";
                    operation = "showAccountInformation";
                }
            } else {
                group = ((Member) context.getAccountOwner()).getGroup();
                if (context.isBroker()) {
                    byBroker = context.isBrokerOf(member);
                } else {
                    byBroker = false;
                }
                module = byBroker ? "brokerReports" : "memberReports";
                operation = "showAccountInformation";
            }
            showAccountInformation = getPermissionService().checkPermission(group, module, operation);
        }

        // References and invoices view permissions
        boolean showReferencesInformation = false;
        boolean showInvoicesInformation = false;
        // First check logged member's permissions
        if (context.isAdmin()) {
            showReferencesInformation = getPermissionService().checkPermission("adminMemberReferences", "view");
            showInvoicesInformation = getPermissionService().checkPermission("adminMemberInvoices", "view");
        } else if (context.isMember()) {
            showReferencesInformation = getPermissionService().checkPermission("memberReferences", "view");
            showInvoicesInformation = getPermissionService().checkPermission("memberInvoices", "view");
        } else { // context.isOperator ()
            showReferencesInformation = getPermissionService().checkPermission("operatorReferences", "view");
            showInvoicesInformation = getPermissionService().checkPermission("operatorInvoices", "view");
        }
        // Then, target member's permissions
        final MemberGroup memberGroup = member.getMemberGroup();
        showReferencesInformation = showReferencesInformation && getPermissionService().checkPermission(memberGroup, "memberReferences", "view");
        showInvoicesInformation = showInvoicesInformation && getPermissionService().checkPermission(memberGroup, "memberInvoices", "view");

        // Get the activities
        ActivitiesVO activities;
        if (myActivities) {
            activities = memberService.getMyActivities();
        } else if (byBroker || context.isAdmin()) {
            activities = memberService.getMemberActivities(member);
        } else if (context.isOperator()) {
            if (byMembersOperator) {
                activities = memberService.getMemberActivitiesByOperator();
            } else {
                activities = memberService.getOtherMemberActivitiesByOperator(member);
            }
        } else {
            activities = memberService.getMemberActivitiesByMember(member);
        }

        request.setAttribute("member", member);
        request.setAttribute("activities", activities);
        request.setAttribute("myActivities", myActivities);
        request.setAttribute("byBroker", byBroker);
        request.setAttribute("showAccountInformation", showAccountInformation);
        request.setAttribute("showReferencesInformation", showReferencesInformation);
        request.setAttribute("showInvoicesInformation", showInvoicesInformation);

        RequestHelper.storeEnum(request, Reference.Level.class, "referenceLevels");
        RequestHelper.storeEnumMap(request, Ad.Status.class, "adStatus");

        return context.getInputForward();
    }

}

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
package nl.strohalm.cyclos.controls.access.channels;

import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.User.TransactionPasswordStatus;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.RelationshipHelper;

import org.apache.struts.action.ActionForward;

/**
 * Action used to prepare the external access screen
 * @author Jefferson Magno
 */
public class ManageExternalAccessAction extends BaseAction {

    private ChannelService channelService;
    private ElementService elementService;
    private GroupService   groupService;

    @Inject
    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ManageExternalAccessForm form = context.getForm();

        boolean myAccess = false;
        boolean memberCanHavePin = false;

        // Get the member
        Member member;
        final long memberId = form.getMemberId();
        if (memberId > 1) {
            member = (Member) elementService.load(memberId, Element.Relationships.USER, Member.Relationships.CHANNELS, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.CHANNELS));
            if (context.getElement().equals(member)) {
                myAccess = true;
            }

            // Check if the member can have pin
            memberCanHavePin = groupService.usesPin(member.getMemberGroup());

        } else {
            // Member managing his/her own external access settings
            member = (Member) getFetchService().fetch(context.getElement(), Element.Relationships.USER, Member.Relationships.CHANNELS, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.CHANNELS));
            myAccess = true;
        }
        final MemberGroup memberGroup = member.getMemberGroup();

        // If the pin is blocked, check the permission to unblock it
        final boolean pinBlocked = getAccessService().isPinBlocked(member.getMemberUser());
        if (pinBlocked) {
            boolean canUnblockPin = false;
            if (context.getElement().equals(member)) {
                canUnblockPin = getPermissionService().checkPermission("memberAccess", "unblockPin");
            } else if (context.isAdmin()) {
                canUnblockPin = getPermissionService().checkPermission("adminMemberAccess", "unblockPin");
            } else if (context.isBrokerOf(member)) {
                canUnblockPin = getPermissionService().checkPermission("brokerMemberAccess", "unblockPin");
            }
            request.setAttribute("canUnblockPin", canUnblockPin);
        }

        // Check if the group of member uses a transaction password
        final boolean usesTransactionPassword = memberGroup.getBasicSettings().getTransactionPassword().isUsed();
        if (usesTransactionPassword) {
            request.setAttribute("usesTransactionPassword", usesTransactionPassword);
            final TransactionPasswordStatus transactionPasswordStatus = getFetchService().reload(context.getUser()).getTransactionPasswordStatus();
            if (transactionPasswordStatus == TransactionPasswordStatus.BLOCKED) {
                request.setAttribute("transactionPasswordBlocked", true);
            } else if (transactionPasswordStatus.isGenerationAllowed()) {
                request.setAttribute("transactionPasswordPending", true);
            }
        }

        // Channels that the group of member have access
        final Channel webChannel = channelService.loadByInternalName(Channel.WEB);

        final Collection<Channel> memberGroupChannels = new ArrayList<Channel>(memberGroup.getChannels());
        // The "web" channel can not be customized by the user, so it should not be sent to the JSP page
        // We need to clone the channels collection because it's associated to the hibernate session
        memberGroupChannels.remove(webChannel);

        // When the SMS channel is in use, it is not added / removed from this page, but from notifications
        final Channel smsChannel = channelService.getSmsChannel();
        if (smsChannel != null) {
            memberGroupChannels.remove(smsChannel);
        }

        // Store member and settings in the request
        request.setAttribute("member", member);
        request.setAttribute("myAccess", myAccess);
        request.setAttribute("channels", memberGroupChannels);
        request.setAttribute("memberCanHavePin", memberCanHavePin);

        return context.getInputForward();
    }
}
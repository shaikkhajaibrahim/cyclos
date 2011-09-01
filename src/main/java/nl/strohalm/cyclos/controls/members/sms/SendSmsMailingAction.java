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
package nl.strohalm.cyclos.controls.members.sms;

import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.sms.SmsMailing;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.sms.SmsMailingService;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;

/**
 * Action used to send an SMS mailing
 * @author luis
 */
@AdminAction( { @Permission(module = "adminMemberSmsMailings", operation = "freeSmsMailings"), @Permission(module = "adminMemberSmsMailings", operation = "paidSmsMailings") })
@BrokerAction( { @Permission(module = "brokerSmsMailings", operation = "freeSmsMailings"), @Permission(module = "brokerSmsMailings", operation = "paidSmsMailings") })
@IgnoreMember
public class SendSmsMailingAction extends BaseFormAction {

    private GroupService           groupService;
    private DataBinder<SmsMailing> dataBinder;
    private SmsMailingService      smsMailingService;

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Inject
    public void setSmsMailingService(final SmsMailingService smsMailingService) {
        this.smsMailingService = smsMailingService;
    }

    @Override
    protected void formAction(final ActionContext context) throws Exception {
        final SendSmsMailingForm form = context.getForm();
        final SmsMailing smsMailing = getDataBinder().readFromString(form.getSmsMailing());
        final String module = context.isAdmin() ? "adminMemberSmsMailings" : "brokerSmsMailings";
        final String operation = smsMailing.isFree() ? "freeSmsMailings" : "paidSmsMailings";
        getPermissionService().checkPermission(module, operation);

        if (smsMailing.isSingleMember()) {
            smsMailing.setGroups(Collections.<MemberGroup> emptyList());
            smsMailingService.sendToMember(smsMailing);
        } else if (smsMailing.isFree()) {
            smsMailingService.sendFreeToGroups(smsMailing);
        } else {
            smsMailingService.sendPaidToGroups(smsMailing);
        }
        context.sendMessage("smsMailing.sent");
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final SendSmsMailingForm form = context.getForm();

        boolean canSendFree;
        boolean canSendPaid;
        if (context.isAdmin()) {
            canSendFree = getPermissionService().checkPermission("adminMemberSmsMailings", "freeSmsMailings");
            canSendPaid = getPermissionService().checkPermission("adminMemberSmsMailings", "paidSmsMailings");

            final GroupQuery query = new GroupQuery();
            query.setManagedBy((AdminGroup) context.getGroup());
            query.setOnlyActive(true);
            request.setAttribute("groups", groupService.search(query));
        } else {
            canSendFree = getPermissionService().checkPermission("brokerSmsMailings", "freeSmsMailings");
            canSendPaid = getPermissionService().checkPermission("brokerSmsMailings", "paidSmsMailings");
        }

        request.setAttribute("canSendFree", canSendFree);
        request.setAttribute("canSendPaid", canSendPaid);
        if (canSendFree && canSendPaid) {
            form.setSmsMailing("free", "true");
        }
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final SendSmsMailingForm form = context.getForm();
        final SmsMailing smsMailing = getDataBinder().readFromString(form.getSmsMailing());
        smsMailingService.validate(smsMailing, form.isSingleMember());
    }

    private DataBinder<SmsMailing> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<SmsMailing> binder = BeanBinder.instance(SmsMailing.class);
            binder.registerBinder("free", PropertyBinder.instance(Boolean.TYPE, "free"));
            binder.registerBinder("text", PropertyBinder.instance(String.class, "text"));
            binder.registerBinder("member", PropertyBinder.instance(Member.class, "member"));
            binder.registerBinder("groups", SimpleCollectionBinder.instance(MemberGroup.class, "groups"));
            dataBinder = binder;
        }
        return dataBinder;
    }

}

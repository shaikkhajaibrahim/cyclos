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

package nl.strohalm.cyclos.controls.members.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.annotations.RequestParameter;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Reference.Nature;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.Message.Type;
import nl.strohalm.cyclos.entities.members.preferences.NotificationPreference;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.sms.MemberSmsStatus;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeTypeService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.services.elements.ReferenceService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.preferences.PreferenceService;
import nl.strohalm.cyclos.services.sms.ISmsContext;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;

/**
 * The action for handle the notification preferences.
 * @author Jefferson Magno
 * @author jeancarlo
 */
@MemberAction(@Permission(module = "memberPreferences", operation = "manageNotifications"))
@AdminAction(@Permission(module = "adminMemberPreferences", operation = "manageNotifications"))
@BrokerAction(@Permission(module = "brokerPreferences", operation = "manageNotifications"))
@RequestParameter("memberId")
@RelatedEntity(Member.class)
@PathToMember("")
public class NotificationPreferenceAction extends BaseFormAction {

    private ElementService       elementService;
    private PreferenceService    preferenceService;
    private GuaranteeTypeService guaranteeTypeService;
    private GroupService         groupService;
    private MemberService        memberService;
    private ChannelService       channelService;
    private AccountService       accountService;
    private ReferenceService     referenceService;

    @Inject
    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

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

    @Inject
    public void setGuaranteeTypeService(final GuaranteeTypeService guaranteeTypeService) {
        this.guaranteeTypeService = guaranteeTypeService;
    }

    @Inject
    public void setMemberService(final MemberService memberService) {
        this.memberService = memberService;
    }

    @Inject
    public void setPreferenceService(final PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @Inject
    public void setReferenceService(final ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final NotificationPreferenceForm form = context.getForm();
        long memberId = form.getMemberId();

        if (memberId < 1) {
            memberId = context.getElement().getId();
        }

        // Load member and member group
        final Member member = (Member) elementService.load(memberId, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.SMS_MESSAGES), Member.Relationships.CHANNELS);

        // Load member notification preferences
        Collection<NotificationPreference> list = preferenceService.load(member);
        if (list == null) {
            list = new ArrayList<NotificationPreference>();
        }

        // Store notification preferences by type
        final Map<Message.Type, NotificationPreference> map = new HashMap<Message.Type, NotificationPreference>();
        for (final NotificationPreference preference : list) {
            map.put(preference.getType(), preference);
        }

        // Check if the member have e-mail
        final boolean hasEmail = StringUtils.isNotEmpty(member.getEmail());

        final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
        final boolean smsEnabled = localSettings.isSmsNotificationEnabled();
        boolean hasNotificationsBySms = false;
        // Save the notification preferences
        for (final Message.Type type : Message.Type.values()) {
            final String isEmailStr = (String) form.getNotificationPreference(type.name() + "_email");
            final String isMessageStr = (String) form.getNotificationPreference(type.name() + "_message");
            final String isSmsStr = (String) form.getNotificationPreference(type.name() + "_sms");

            boolean isEmail = false;
            boolean isMessage = false;
            boolean isSms = false;

            // If not use type, use both as false
            if (useType(context, type, member)) {
                isEmail = hasEmail ? CoercionHelper.coerce(Boolean.TYPE, isEmailStr) : false;
                isMessage = CoercionHelper.coerce(Boolean.TYPE, isMessageStr);
                if (type == Message.Type.FROM_ADMIN_TO_MEMBER || type == Message.Type.FROM_ADMIN_TO_GROUP) {
                    isMessage = true;
                }
                if (smsEnabled) {
                    isSms = CoercionHelper.coerce(Boolean.TYPE, isSmsStr);
                }
            }
            hasNotificationsBySms |= isSms;
            NotificationPreference preference = map.get(type);
            if (preference == null && (isEmail || isMessage || isSms)) {
                // Insert new notification preference
                preference = new NotificationPreference();
                preference.setType(type);
                preference.setEmail(isEmail);
                preference.setMessage(isMessage);
                preference.setSms(isSms);
                map.put(type, preference);
            } else if (preference != null) {
                // Update an existing notification preference
                preference.setEmail(isEmail);
                preference.setMessage(isMessage);
                preference.setSms(isSms);
            }
        }
        preferenceService.save(member, map.values());

        if (smsEnabled) {
            // Store the sms operations channel
            final Channel smsChannel = channelService.getSmsChannel();
            if (smsChannel != null) {
                final Set<Channel> channels = new HashSet<Channel>(member.getChannels());
                if (form.isEnableSmsOperations()) {
                    channels.add(smsChannel);
                } else {
                    channels.remove(smsChannel);
                }
                if (context.getElement().equals(member)) {
                    getAccessService().changeMyChannelsAccess(channels, false);
                } else {
                    getAccessService().changeMemberChannelsAccess(member, channels, false);
                }
            }
            // The other flags come from the member sms status
            preferenceService.saveSmsStatusPreferences(member, form.isAcceptFreeMailing(), form.isAcceptPaidMailing(), form.isAllowChargingSms(), hasNotificationsBySms);
        }

        context.sendMessage("notificationPreferences.modified");
        if (context.getElement().getId() == memberId) { // my preferences
            return context.getSuccessForward();
        } else {
            return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "memberId", memberId);
        }
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final NotificationPreferenceForm form = context.getForm();
        long memberId = form.getMemberId();

        if (memberId < 1) {
            memberId = context.getElement().getId();
        }

        // Load member and member group
        final Member member = (Member) elementService.load(memberId, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.SMS_MESSAGES), Member.Relationships.CHANNELS);
        request.setAttribute("member", member);

        final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());

        // Check which messages types can be sent by sms (group setting)
        final Collection<Type> smsMessages = member.getMemberGroup().getSmsMessages();
        final boolean hasSmsMessages = CollectionUtils.isNotEmpty(smsMessages);
        final boolean smsEnabled = localSettings.isSmsNotificationEnabled();
        request.setAttribute("smsEnabled", smsEnabled);
        request.setAttribute("hasSmsMessages", hasSmsMessages);
        request.setAttribute("smsEnabledTypes", smsMessages);

        // Load member notification preferences
        Collection<NotificationPreference> list = preferenceService.load(member);
        if (list == null) {
            list = new ArrayList<NotificationPreference>();
        }

        // Store notification preferences by type
        final Map<Message.Type, NotificationPreference> map = new HashMap<Message.Type, NotificationPreference>();
        for (final NotificationPreference preference : list) {
            map.put(preference.getType(), preference);
        }

        // Check if the member have e-mail
        final boolean hasEmail = StringUtils.isNotEmpty(member.getEmail()) ? true : false;
        request.setAttribute("hasEmail", hasEmail);

        final List<Message.Type> types = new ArrayList<Message.Type>(Arrays.asList(Message.Type.values()));
        final Iterator<Message.Type> iterator = types.iterator();
        while (iterator.hasNext()) {
            final Message.Type type = iterator.next();

            // Check if the type should be removed
            if (!useType(context, type, member)) {
                iterator.remove();
                continue;
            }

            final NotificationPreference preference = map.get(type);
            final String typeMessage = type.name() + "_message";
            final String typeEmail = type.name() + "_email";
            final String typeSms = type.name() + "_sms";

            final boolean isEmail = preference != null ? preference.isEmail() : false;
            boolean isMessage = preference != null ? preference.isMessage() : false;
            if (type == Message.Type.FROM_ADMIN_TO_MEMBER || type == Message.Type.FROM_ADMIN_TO_GROUP) {
                isMessage = true;
            }
            final boolean isSms = preference != null ? preference.isSms() : false;

            form.setNotificationPreference(typeMessage, isMessage);
            form.setNotificationPreference(typeEmail, isEmail);
            form.setNotificationPreference(typeSms, isSms);
        }
        request.setAttribute("types", types);

        if (smsEnabled) {
            final ISmsContext smsContext = groupService.getSmsContext(member.getMemberGroup());
            request.setAttribute("maxFreeSms", smsContext.getFreeSms(member));
            request.setAttribute("additionalChargedSms", smsContext.getAdditionalChargedSms(member));
            request.setAttribute("additionalChargeAmount", smsContext.getAdditionalChargeAmount(member));
            final TransferType tt = member.getMemberGroup().getMemberSettings().getSmsChargeTransferType();
            request.setAttribute("additionalChargeCurrency", tt == null ? null : tt.getCurrency());
            request.setAttribute("additionalChargePeriod", smsContext.getAdditionalChargedPeriod(member));

            final MemberSmsStatus smsStatus = memberService.getSmsStatus(member);
            request.setAttribute("showFreeSms", smsContext.showFreeSms(smsStatus));
            final Channel smsChannel = channelService.getSmsChannel();
            if (smsChannel != null) {
                final boolean hasAccessToSmsChannel = member.getMemberGroup().getChannels().contains(smsChannel);
                form.setEnableSmsOperations(hasAccessToSmsChannel && member.getChannels().contains(smsChannel));
                request.setAttribute("hasAccessToSmsChannel", hasAccessToSmsChannel);
            }
            form.setAllowChargingSms(smsStatus.isAllowChargingSms());
            form.setAcceptFreeMailing(smsStatus.isAcceptFreeMailing());
            form.setAcceptPaidMailing(smsStatus.isAcceptPaidMailing());
            request.setAttribute("smsStatus", smsStatus);
        }
    }

    /**
     * Checks if a given type will be used for the member
     */
    private boolean useType(final ActionContext context, final Message.Type type, final Member member) {
        final MemberGroup group = member.getMemberGroup();
        final HttpSession session = context.getSession();
        boolean useType;

        switch (type) {
            case ACCOUNT:
                if (member.equals(context.getElement())) {
                    useType = (Boolean) session.getAttribute("loggedMemberHasAccounts");
                } else {
                    final List<? extends Account> accounts = accountService.getAccounts(member);
                    useType = !accounts.isEmpty();
                }
                break;
            case ACCESS:
                useType = true;
                break;
            case AD_EXPIRATION:
                useType = getPermissionService().checkPermission(group, "memberAds", "publish");
                break;
            case AD_INTEREST:
                useType = getPermissionService().checkPermission(group, "memberPreferences", "manageAdInterests");
                break;
            case BROKERING:
                useType = member.getGroup().getNature() == Group.Nature.BROKER || member.getBroker() != null;
                break;
            case FROM_MEMBER:
                useType = getPermissionService().checkPermission(group, "memberProfile", "view");
                break;
            case INVOICE:
                useType = getPermissionService().checkPermission(group, "memberInvoices", "view");
                break;
            case LOAN:
                useType = getPermissionService().checkPermission(group, "memberLoans", "view");
                break;
            case PAYMENT:
                useType = getPermissionService().checkPermission(group, "memberPayments", null);
                break;
            case EXTERNAL_PAYMENT:
                useType = false;
                // External payment notifications will be enabled when the member has access to non-builtin channels
                for (final Channel channel : group.getChannels()) {
                    if (Channel.listBuiltin().contains(channel.getInternalName())) {
                        useType = true;
                        break;
                    }
                }
                break;
            case REFERENCE:
                useType = getPermissionService().checkPermission(group, "memberReferences", "view");
                break;
            case TRANSACTION_FEEDBACK:
                if (member.equals(context.getElement())) {
                    useType = (Boolean) session.getAttribute("loggedMemberHasTransactionFeedbacks");
                } else {
                    final Collection<Nature> referenceNatures = referenceService.getNaturesByGroup(member.getMemberGroup());
                    useType = referenceNatures.contains(Nature.TRANSACTION);
                }
                break;
            case CERTIFICATION:
                useType = (Boolean) session.getAttribute("isBuyer") || getPermissionService().checkPermission(group, "memberGuarantees", "issueCertifications");
                break;
            case GUARANTEE:
                // every member could have a guarantee, only check if there are some GT defined
                useType = guaranteeTypeService.areEnabledGuaranteeTypes();
                break;
            case PAYMENT_OBLIGATION:
                useType = (Boolean) session.getAttribute("isBuyer") || (Boolean) session.getAttribute("isSeller");
                break;
            default:
                useType = true;
                break;
        }
        return useType;
    }
}
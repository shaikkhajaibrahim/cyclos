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
package nl.strohalm.cyclos.controls.members;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.elements.CreateElementAction;
import nl.strohalm.cyclos.controls.elements.CreateElementForm;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.exceptions.MailSendingException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.WhenSaving;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to create members
 * @author luis
 */
public class CreateMemberAction extends CreateElementAction<Member> {

    private CustomFieldService customFieldService;
    private SettingsService settingsService;
    private GroupService       groupService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    @Override
    public DataBinder<? extends Element> getDataBinder() {
        if (dataBinder == null) {
            final DataBinder<? extends Element> binder = getBaseBinder();
            ((BeanBinder<? extends Element>) binder).registerBinder("broker", PropertyBinder.instance(Member.class, "broker"));
            dataBinder = binder;
        }
        return dataBinder;
    }

    public GroupService getGroupService() {
        return groupService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected ActionForward create(final Element element, final ActionContext context) {
        final CreateElementForm form = context.getForm();
        final Member member = (Member) element;

        setFullNameIfNeeded(member);

        final boolean sendPasswordByEmail = member.getMemberGroup().getMemberSettings().isSendPasswordByEmail();
        final boolean canChangePassword = getPermissionService().checkPermission(context.isAdmin() ? "adminMemberAccess" : "brokerMemberAccess", "changePassword");
        final boolean allowSetPassword = !sendPasswordByEmail || canChangePassword;

        // When password cannot be set, ensure it's null
        if (!allowSetPassword) {
            final User user = member.getUser();
            if (user != null) {
                user.setPassword(null);
            }
        }

        // When password is not sent by e-mail and can't set a definitive password, ensure the force change is set
        if (!sendPasswordByEmail && !canChangePassword) {
            form.setForceChangePassword(true);
        }

        RegisteredMember registeredMember;
        String successKey = null;
        try {
            if (context.isAdmin()) {
                registeredMember = getElementService().registerMemberByAdmin(member, form.isForceChangePassword());
                successKey = "createMember.admin.created";
            } else if (context.isBroker()) {
                registeredMember = getElementService().registerMemberByBroker(member, form.isForceChangePassword());
                successKey = "createMember.broker.created";
            } else {
                throw new ValidationException();
            }
        } catch (final MailSendingException e) {
            return context.sendError("createMember.error.mailSending");
        }

        boolean sendMessage = false;

        // Check if there's a mail exception
        if (MailHandler.hasException()) {
            successKey += ".mailError";
            sendMessage = true;
        }

        // Resolve the forward
        String paramName;
        Object paramValue;
        ActionForward forward;
        if (form.isOpenProfile()) {
            if (registeredMember instanceof Member) {
                // The member was already created
                paramName = "memberId";
                forward = context.findForward("profile");
            } else {
                // It's a PendingMember, awaiting mail confirmation
                paramName = "pendingMemberId";
                forward = context.findForward("pendingMemberProfile");
            }
            paramValue = registeredMember.getId();
        } else {
            sendMessage = true;
            paramName = "groupId";
            paramValue = registeredMember.getMemberGroup().getId();
            forward = context.findForward("new");
        }

        if (sendMessage) {
            context.sendMessage(successKey);
        }
        return ActionHelper.redirectWithParam(context.getRequest(), forward, paramName, paramValue);
    }

    protected void setFullNameIfNeeded(Member member) {
        String fullNameExpression = settingsService.getLocalSettings().getFullNameExpression();
        if (!StringUtils.isEmpty(fullNameExpression)) {
            String fullName = prepareFullName(member.getCustomValues(), fullNameExpression);
            member.setName(fullName);
        }
    }

    private String prepareFullName(Collection<MemberCustomFieldValue> fields, String fullNameExpression) {
        String fullName = fullNameExpression;
        for (MemberCustomFieldValue value : fields) {
            CustomField cf = customFieldService.load(value.getField().getId());
            String fieldValue = value.getValue() == null ? "" : value.getValue();
            fullName = fullName.replaceAll("#"+cf.getInternalName()+"#",fieldValue);
        }
        return fullName;
    }

    @Override
    protected void formAction(final ActionContext context) throws Exception {
        super.formAction(context);
        context.sendMessage(context.isAdmin() ? "createMember.admin.created" : "createMember.broker.created");
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<MemberCustomField> getCustomFieldClass() {
        return MemberCustomField.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<MemberCustomFieldValue> getCustomFieldValueClass() {
        return MemberCustomFieldValue.class;
    }

    @Override
    protected Class<Member> getElementClass() {
        return Member.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<MemberGroup> getGroupClass() {
        return MemberGroup.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Class<MemberUser> getUserClass() {
        return MemberUser.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final CreateElementForm form = context.getForm();

        // Get the initial group
        if (form.getGroupId() <= 0L) {
            throw new ValidationException();
        }

        final MemberGroup group = (MemberGroup) groupService.load(form.getGroupId());
        // Get the custom fields for the initial group
        final List<MemberCustomField> customFields = CustomFieldHelper.onlyForGroup((List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER), group);
        final boolean byBroker = context.isBroker();
        for (final Iterator<MemberCustomField> iterator = customFields.iterator(); iterator.hasNext();) {
            final MemberCustomField field = iterator.next();
            if (!field.getUpdateAccess().granted(context.getGroup(), false, byBroker, false)) {
                iterator.remove();
            }
        }
        request.setAttribute("customFields", customFields);
        request.setAttribute("group", group);

        request.setAttribute("displayFullName", StringUtils.isEmpty(
                settingsService.getLocalSettings().getFullNameExpression()));

        // Store the password control flags
        final boolean sendPasswordByEmail = group.getMemberSettings().isSendPasswordByEmail();
        final boolean canChangePassword = getPermissionService().checkPermission(context.isAdmin() ? "adminMemberAccess" : "brokerMemberAccess", "changePassword");
        request.setAttribute("allowAutomaticPassword", sendPasswordByEmail && canChangePassword);
        request.setAttribute("allowSetPassword", !sendPasswordByEmail || canChangePassword);
        request.setAttribute("allowSetForceChangePassword", canChangePassword);

        if (context.isAdmin()) {
            final GroupQuery query = new GroupQuery();
            query.setNatures(Group.Nature.BROKER);
            query.setStatus(Group.Status.NORMAL);
            query.setPageForCount();
            final boolean allowSetBroker = PageHelper.getTotalCount(groupService.search(query)) > 0;
            request.setAttribute("allowSetBroker", allowSetBroker);
        } else if (context.isBroker()) {
            request.setAttribute("byBroker", true);
        } else {
            throw new ValidationException();
        }
    }

    @Override
    protected void runValidation(final ActionContext context, final Element element) {
        final CreateElementForm form = context.getForm();
        final boolean manualPassword = form.isManualPassword();
        final WhenSaving when = context.isAdmin() ? WhenSaving.MEMBER_BY_ADMIN : WhenSaving.BY_BROKER;
        getElementService().validate(element, when, manualPassword);
    }

}

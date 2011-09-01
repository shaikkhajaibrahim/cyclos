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
package nl.strohalm.cyclos.controls.admins;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.elements.ProfileAction;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.customization.fields.AdminCustomField;
import nl.strohalm.cyclos.entities.customization.fields.AdminCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberRecordService;
import nl.strohalm.cyclos.services.elements.WhenSaving;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.BeanCollectionBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.conversion.HtmlConverter;
import nl.strohalm.cyclos.utils.conversion.ReferenceConverter;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Profile action for admins
 * @author luis
 */
public class AdminProfileAction extends ProfileAction<Administrator> {

    private static final Relationship[] FETCH = { RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP), RelationshipHelper.nested(User.Relationships.ELEMENT, Administrator.Relationships.CUSTOM_VALUES) };

    private CustomFieldService          customFieldService;
    private ElementService              elementService;
    private MemberRecordService         memberRecordService;

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setMemberRecordService(final MemberRecordService memberRecordService) {
        this.memberRecordService = memberRecordService;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <CFV extends CustomFieldValue> Class<CFV> getCustomFieldValueClass() {
        return (Class<CFV>) AdminCustomFieldValue.class;
    }

    @Override
    protected Class<Administrator> getElementClass() {
        return Administrator.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <G extends Group> Class<G> getGroupClass() {
        return (Class<G>) AdminGroup.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <U extends User> Class<U> getUserClass() {
        return (Class<U>) AdminUser.class;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final AdminProfileForm form = context.getForm();

        // Save the administrator
        Administrator admin = getWriteDataBinder(context).readFromString(form.getAdmin());
        final Administrator savedAdmin = (Administrator) elementService.load(admin.getId(), Element.Relationships.USER);
        admin.getUser().setUsername(savedAdmin.getUsername());

        final Element loggedElement = context.getElement();

        if (loggedElement.equals(admin)) {
            admin = elementService.changeMyProfile(admin);
            updateLoggedUser(admin, context);
        } else {
            admin = elementService.changeAdminProfile(admin);
        }

        context.sendMessage("profile.modified");

        return ActionHelper.redirectWithParam(context.getRequest(), super.handleSubmit(context), "adminId", admin.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected DataBinder<Administrator> initDataBinderForWrite(final ActionContext context) {
        final BeanBinder<Administrator> dataBinder = (BeanBinder<Administrator>) super.initDataBinderForWrite(context);

        final BeanBinder<AdminCustomFieldValue> customValueBinder = BeanBinder.instance(AdminCustomFieldValue.class);
        customValueBinder.registerBinder("field", PropertyBinder.instance(AdminCustomField.class, "field", ReferenceConverter.instance(AdminCustomField.class)));
        customValueBinder.registerBinder("value", PropertyBinder.instance(String.class, "value", HtmlConverter.instance()));

        // Replace the normal custom field binder for an admin custom field binder, because it has another property - hidden
        final BeanCollectionBinder collectionBinder = (BeanCollectionBinder) dataBinder.getMappings().get("customValues");
        collectionBinder.setElementBinder(customValueBinder);

        return dataBinder;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void prepareForm(final ActionContext context) throws Exception {
        final AdminProfileForm form = context.getForm();
        boolean myProfile = false;
        AdminUser adminUser = null;
        final HttpServletRequest request = context.getRequest();

        final Element loggedElement = context.getElement();
        // Load the admin
        if (form.getAdminId() > 0 && form.getAdminId() != loggedElement.getId()) {
            final User loaded = elementService.loadUser(form.getAdminId(), FETCH);
            if (loaded instanceof AdminUser) {
                adminUser = (AdminUser) loaded;
                try {
                    request.setAttribute("isLoggedIn", getAccessService().isLoggedIn(adminUser));
                } catch (final NotConnectedException e) {
                    // Ok - user is not online
                }
            } else {
                throw new ValidationException();
            }
        } else {
            // My profile
            adminUser = getFetchService().fetch((AdminUser) context.getUser(), FETCH);
            myProfile = true;
        }

        // Write the admin to the form
        final Administrator admin = adminUser.getAdministrator();
        getReadDataBinder(context).writeAsString(form.getAdmin(), admin);

        // Retrieve the custom fields
        final List<AdminCustomField> customFields = CustomFieldHelper.onlyForGroup((List<AdminCustomField>) customFieldService.listByNature(CustomField.Nature.ADMIN), admin.getAdminGroup());

        // Check the permissions
        boolean editable = myProfile;
        if (!myProfile) {
            editable = getPermissionService().checkPermission("adminAdmins", "changeProfile");
            if (getPermissionService().checkPermission("adminMemberRecords", "view")) {
                request.setAttribute("countByRecordType", memberRecordService.countByType(admin));
            }
        }

        // Store the request attributes
        request.setAttribute("admin", admin);
        request.setAttribute("disabledLogin", getAccessService().isLoginBlocked(admin.getUser()));
        request.setAttribute("customFields", CustomFieldHelper.buildEntries(customFields, admin.getCustomValues()));
        request.setAttribute("editable", editable);
        request.setAttribute("myProfile", myProfile);
        request.setAttribute("removed", admin.getGroup().getStatus() == Group.Status.REMOVED);
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final AdminProfileForm form = context.getForm();
        final Administrator administrator = getWriteDataBinder(context).readFromString(form.getAdmin());
        elementService.validate(administrator, WhenSaving.PROFILE, false);
    }
}

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
package nl.strohalm.cyclos.controls.groups.permissions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.controls.groups.EditGroupAction;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.Module;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeTypeQuery;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType.Model;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType.Context;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DocumentQuery;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.entities.members.records.MemberRecordTypeQuery;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.accounts.AccountTypeService;
import nl.strohalm.cyclos.services.accounts.CurrencyService;
import nl.strohalm.cyclos.services.accounts.MemberAccountTypeQuery;
import nl.strohalm.cyclos.services.accounts.SystemAccountTypeQuery;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeTypeService;
import nl.strohalm.cyclos.services.customization.DocumentService;
import nl.strohalm.cyclos.services.elements.MemberRecordTypeService;
import nl.strohalm.cyclos.services.elements.MessageCategoryService;
import nl.strohalm.cyclos.services.groups.AdminGroupPermissionsDTO;
import nl.strohalm.cyclos.services.groups.BrokerGroupPermissionsDTO;
import nl.strohalm.cyclos.services.groups.GroupPermissionsDTO;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.groups.MemberGroupPermissionsDTO;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.conversion.ReferenceConverter;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to edit a group's permissions
 *
 * @author luis
 */
public class EditGroupPermissionsAction extends BaseFormAction {

    private static final Relationship[] FETCH = {Group.Relationships.PERMISSIONS, Group.Relationships.TRANSFER_TYPES, Group.Relationships.DOCUMENTS, Group.Relationships.MESSAGE_CATEGORIES, BrokerGroup.Relationships.BROKER_DOCUMENTS, Group.Relationships.CHARGEBACK_TRANSFER_TYPES, AdminGroup.Relationships.MANAGES_GROUPS, AdminGroup.Relationships.TRANSFER_TYPES_AS_MEMBER, AdminGroup.Relationships.VIEW_INFORMATION_OF, AdminGroup.Relationships.VIEW_CONNECTED_ADMINS_OF, MemberGroup.Relationships.CAN_VIEW_ADS_OF_GROUPS, MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS};
    private GroupService groupService;
    private ChannelService channelService;
    private TransferTypeService transferTypeService;
    private DocumentService documentService;
    private AccountTypeService accountTypeService;
    private MessageCategoryService messageCategoryService;
    private MemberRecordTypeService memberRecordTypeService;
    private CurrencyService currencyService;
    private GuaranteeTypeService guaranteeTypeService;
    private DataBinder<AdminGroupPermissionsDTO> adminDataBinder;
    private DataBinder<MemberGroupPermissionsDTO<MemberGroup>> memberDataBinder;
    private DataBinder<BrokerGroupPermissionsDTO> brokerDataBinder;

    public DataBinder<AdminGroupPermissionsDTO> getAdminDataBinder() {
        if (adminDataBinder == null) {
            final BeanBinder<AdminGroupPermissionsDTO> binder = BeanBinder.instance(AdminGroupPermissionsDTO.class);
            initBasic(binder);
            binder.registerBinder("memberChargebackTTs", SimpleCollectionBinder.instance(TransferType.class, "memberChargebackTTs"));
            binder.registerBinder("systemChargebackTTs", SimpleCollectionBinder.instance(TransferType.class, "systemChargebackTTs"));
            binder.registerBinder("grantLoanTTs", SimpleCollectionBinder.instance(TransferType.class, "grantLoanTTs"));
            binder.registerBinder("asMemberToMemberTTs", SimpleCollectionBinder.instance(TransferType.class, "asMemberToMemberTTs"));
            binder.registerBinder("asMemberToSelfTTs", SimpleCollectionBinder.instance(TransferType.class, "asMemberToSelfTTs"));
            binder.registerBinder("asMemberToSystemTTs", SimpleCollectionBinder.instance(TransferType.class, "asMemberToSystemTTs"));
            binder.registerBinder("systemToMemberTTs", SimpleCollectionBinder.instance(TransferType.class, "systemToMemberTTs"));
            binder.registerBinder("systemToSystemTTs", SimpleCollectionBinder.instance(TransferType.class, "systemToSystemTTs"));
            binder.registerBinder("conversionSimulationTTs", SimpleCollectionBinder.instance(TransferType.class, "conversionSimulationTTs"));
            binder.registerBinder("managesGroups", SimpleCollectionBinder.instance(MemberGroup.class, "managesGroups"));

            binder.registerBinder("managesAdminGroups", SimpleCollectionBinder.instance(AdminGroup.class, "managesAdminGroups"));


            binder.registerBinder("viewInformationOf", SimpleCollectionBinder.instance(SystemAccountType.class, "viewInformationOf"));
            binder.registerBinder("viewConnectedAdminsOf", SimpleCollectionBinder.instance(AdminGroup.class, "viewConnectedAdminsOf"));
            binder.registerBinder("viewAdminRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "viewAdminRecordTypes"));
            binder.registerBinder("createAdminRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "createAdminRecordTypes"));
            binder.registerBinder("modifyAdminRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "modifyAdminRecordTypes"));
            binder.registerBinder("deleteAdminRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "deleteAdminRecordTypes"));
            binder.registerBinder("viewMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "viewMemberRecordTypes"));
            binder.registerBinder("createMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "createMemberRecordTypes"));
            binder.registerBinder("modifyMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "modifyMemberRecordTypes"));
            binder.registerBinder("deleteMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "deleteMemberRecordTypes"));
            adminDataBinder = binder;
        }
        return adminDataBinder;
    }

    public DataBinder<BrokerGroupPermissionsDTO> getBrokerDataBinder() {
        if (brokerDataBinder == null) {
            final BeanBinder<BrokerGroupPermissionsDTO> binder = BeanBinder.instance(BrokerGroupPermissionsDTO.class);
            initBasic(binder);
            initMember(binder);
            binder.registerBinder("brokerDocuments", SimpleCollectionBinder.instance(Document.class, "brokerDocuments"));
            binder.registerBinder("asMemberToMemberTTs", SimpleCollectionBinder.instance(TransferType.class, "asMemberToMemberTTs"));
            binder.registerBinder("asMemberToSelfTTs", SimpleCollectionBinder.instance(TransferType.class, "asMemberToSelfTTs"));
            binder.registerBinder("asMemberToSystemTTs", SimpleCollectionBinder.instance(TransferType.class, "asMemberToSystemTTs"));
            binder.registerBinder("memberToMemberTTs", SimpleCollectionBinder.instance(TransferType.class, "memberToMemberTTs"));
            binder.registerBinder("memberToSystemTTs", SimpleCollectionBinder.instance(TransferType.class, "memberToSystemTTs"));
            binder.registerBinder("selfPaymentTTs", SimpleCollectionBinder.instance(TransferType.class, "selfPaymentTTs"));
            binder.registerBinder("brokerConversionSimulationTTs", SimpleCollectionBinder.instance(TransferType.class, "brokerConversionSimulationTTs"));
            binder.registerBinder("brokerCanViewInformationOf", SimpleCollectionBinder.instance(AccountType.class, "brokerCanViewInformationOf"));
            binder.registerBinder("brokerMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "brokerMemberRecordTypes"));
            binder.registerBinder("brokerCreateMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "brokerCreateMemberRecordTypes"));
            binder.registerBinder("brokerModifyMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "brokerModifyMemberRecordTypes"));
            binder.registerBinder("brokerDeleteMemberRecordTypes", SimpleCollectionBinder.instance(MemberRecordType.class, "brokerDeleteMemberRecordTypes"));
            brokerDataBinder = binder;
        }
        return brokerDataBinder;
    }

    @SuppressWarnings("unchecked")
    public DataBinder<MemberGroupPermissionsDTO<MemberGroup>> getMemberDataBinder() {
        if (memberDataBinder == null) {
            final BeanBinder<MemberGroupPermissionsDTO<MemberGroup>> binder = (BeanBinder) BeanBinder.instance(MemberGroupPermissionsDTO.class);
            initBasic(binder);
            initMember(binder);
            binder.registerBinder("memberToMemberTTs", SimpleCollectionBinder.instance(TransferType.class, "memberToMemberTTs"));
            binder.registerBinder("memberToSystemTTs", SimpleCollectionBinder.instance(TransferType.class, "memberToSystemTTs"));
            binder.registerBinder("selfPaymentTTs", SimpleCollectionBinder.instance(TransferType.class, "selfPaymentTTs"));
            binder.registerBinder("conversionSimulationTTs", SimpleCollectionBinder.instance(TransferType.class, "conversionSimulationTTs"));
            binder.registerBinder("requestPaymentByChannels", SimpleCollectionBinder.instance(Channel.class, "requestPaymentByChannels"));
            binder.registerBinder("chargebackTTs", SimpleCollectionBinder.instance(TransferType.class, "chargebackTTs"));
            memberDataBinder = binder;
        }
        return memberDataBinder;
    }

    @Inject
    public void setAccountTypeService(final AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    @Inject
    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    @Inject
    public void setCurrencyService(final CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Inject
    public void setDocumentService(final DocumentService documentService) {
        this.documentService = documentService;
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
    public void setMemberRecordTypeService(final MemberRecordTypeService memberRecordTypeService) {
        this.memberRecordTypeService = memberRecordTypeService;
    }

    @Inject
    public void setMessageCategoryService(final MessageCategoryService messageCategoryService) {
        this.messageCategoryService = messageCategoryService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final EditGroupPermissionsForm form = context.getForm();
        final long groupId = form.getGroupId();
        EditGroupAction.checkPermission(context, getPermissionService(), groupService, false, groupId);
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final EditGroupPermissionsForm form = context.getForm();
        final long id = form.getGroupId();
        if (id <= 0L) {
            throw new ValidationException();
        }
        final Group group = groupService.load(id);
        if (group instanceof AdminGroup) {
            final AdminGroupPermissionsDTO dto = getAdminDataBinder().readFromString(form.getPermission());
            groupService.setAdminPermissions(dto);
        } else if (group instanceof BrokerGroup) {
            final BrokerGroupPermissionsDTO dto = getBrokerDataBinder().readFromString(form.getPermission());
            groupService.setBrokerPermissions(dto);
        } else {
            final MemberGroupPermissionsDTO<MemberGroup> dto = getMemberDataBinder().readFromString(form.getPermission());
            groupService.setMemberPermissions(dto);
        }
        context.sendMessage("permission.modified");
        return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "groupId", id);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final EditGroupPermissionsForm form = context.getForm();
        boolean editable = false;
        final long groupId = form.getGroupId();
        if (groupId <= 0) {
            throw new ValidationException();
        }
        final Group group = groupService.load(groupId, FETCH);

        final List<Module> modules = getPermissionService().listModules(group.getNature());
        request.setAttribute("group", group);
        request.setAttribute("modules", modules);

        final DocumentQuery documentQuery = new DocumentQuery();
        final List<Document> documents = documentService.search(documentQuery);
        request.setAttribute("documents", documents);

        // Put in the request the name of permission used to manage a type of group
        final Map<Group.Nature, String> permissionByNature = new HashMap<Group.Nature, String>();
        permissionByNature.put(Group.Nature.ADMIN, "manageAdmin");
        permissionByNature.put(Group.Nature.BROKER, "manageBroker");
        permissionByNature.put(Group.Nature.MEMBER, "manageMember");
        request.setAttribute("permissionByNature", permissionByNature);

        // Get the associated account types
        Collection<MemberAccountType> memberAccountTypes = null;
        if (group instanceof BrokerGroup) {
            memberAccountTypes = (Collection<MemberAccountType>) accountTypeService.search(new MemberAccountTypeQuery());
        } else if (group instanceof MemberGroup) {
            memberAccountTypes = ((MemberGroup) group).getAccountTypes();
        } else {
            final AdminGroup adminGroup = (AdminGroup) group;
            final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
            final Collection<AdminGroup> managesAdminGroups = adminGroup.getManagesAdminGroups();

            memberAccountTypes = new HashSet<MemberAccountType>();
            for (final MemberGroup memberGroup : managesGroups) {
                memberAccountTypes.addAll(memberGroup.getAccountTypes());
            }

        }

        // Get the associated account types
        Collection<SystemAccountType> systemAccountTypes = null;
        if (group instanceof AdminGroup) {
            systemAccountTypes = ((AdminGroup) group).getViewInformationOf();
        }

        // Load the required data, according to the group nature

        // Message categories (common to all group natures)
        request.setAttribute("messageCategories", messageCategoryService.listAll());

        // Transfer types
        TransferTypeQuery ttQuery;

        if (group.getNature() == Group.Nature.ADMIN) { // For admins

            // System accounts
            final List<? extends AccountType> systemAccounts = accountTypeService.search(new SystemAccountTypeQuery());
            request.setAttribute("systemAccounts", systemAccounts);

            // System to system payments
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.SELF_PAYMENT);
            ttQuery.setFromNature(AccountType.Nature.SYSTEM);
            ttQuery.setToNature(AccountType.Nature.SYSTEM);
            ttQuery.setFromAccountTypes(systemAccountTypes);
            ttQuery.setToAccountTypes(systemAccountTypes);
            final List<TransferType> systemSystemTTs = transferTypeService.search(ttQuery);
            request.setAttribute("systemSystemTTs", systemSystemTTs);

            // System to member payments
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.PAYMENT);
            ttQuery.setFromNature(AccountType.Nature.SYSTEM);
            ttQuery.setToNature(AccountType.Nature.MEMBER);
            ttQuery.setFromAccountTypes(systemAccountTypes);
            ttQuery.setToAccountTypes(memberAccountTypes);
            final List<TransferType> systemMemberTTs = transferTypeService.search(ttQuery);
            request.setAttribute("systemMemberTTs", systemMemberTTs);

            // View connected admins
            GroupQuery groupQuery = new GroupQuery();
            groupQuery.setNatures(Group.Nature.ADMIN);
            groupQuery.setIgnoreManagedBy(true);
            final List<AdminGroup> adminGroups = (List<AdminGroup>) groupService.search(groupQuery);
            request.setAttribute("adminGroups", adminGroups);

            // Loans
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.LOAN);
            ttQuery.setFromAccountTypes(systemAccountTypes);
            ttQuery.setToAccountTypes(memberAccountTypes);
            final List<TransferType> loanTTs = transferTypeService.search(ttQuery);
            request.setAttribute("loanTTs", loanTTs);

            // Member groups
            groupQuery = new GroupQuery();
            groupQuery.setIgnoreManagedBy(true);
            groupQuery.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
            final List<? extends Group> memberGroups = groupService.search(groupQuery);
            request.setAttribute("memberGroups", memberGroups);

            // System chargebacks
            final TransferTypeQuery systemChargebackQuery = new TransferTypeQuery();
            systemChargebackQuery.setFromNature(AccountType.Nature.SYSTEM);
            final List<TransferType> systemChargebacks = transferTypeService.search(systemChargebackQuery);
            for (final Iterator<TransferType> iter = systemChargebacks.iterator(); iter.hasNext(); ) {
                final TransferType tt = iter.next();
                final Context ctx = tt.getContext();
                if (!ctx.isPayment() && !ctx.isSelfPayment()) {
                    iter.remove();
                }
            }
            request.setAttribute("systemChargebackTTs", systemChargebacks);

            // Member chargebacks
            final TransferTypeQuery memberChargebackQuery = new TransferTypeQuery();
            memberChargebackQuery.setFromNature(AccountType.Nature.MEMBER);
            final List<TransferType> memberChargebacks = transferTypeService.search(memberChargebackQuery);
            for (final Iterator<TransferType> iter = memberChargebacks.iterator(); iter.hasNext(); ) {
                final TransferType tt = iter.next();
                final Context ctx = tt.getContext();
                if (!ctx.isPayment() && !ctx.isSelfPayment()) {
                    iter.remove();
                }
            }
            request.setAttribute("memberChargebackTTs", memberChargebacks);

            // Guarantee types
            setGuaranteeTypes(request, group);

        } else { // For members

            // Member to system payments
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.PAYMENT);
            ttQuery.setFromNature(AccountType.Nature.MEMBER);
            ttQuery.setToNature(AccountType.Nature.SYSTEM);
            ttQuery.setFromAccountTypes(memberAccountTypes);
            ttQuery.setToAccountTypes(systemAccountTypes);
            final List<TransferType> memberSystemTTs = transferTypeService.search(ttQuery);
            request.setAttribute("memberSystemTTs", memberSystemTTs);

            // Member to member payment
            ttQuery = new TransferTypeQuery();
            ttQuery.setFromNature(AccountType.Nature.MEMBER);
            ttQuery.setToNature(AccountType.Nature.MEMBER);
            ttQuery.setFromAccountTypes(memberAccountTypes);
            final List<TransferType> memberMemberTTs = new ArrayList<TransferType>(transferTypeService.search(ttQuery));
            for (final Iterator<TransferType> iter = memberMemberTTs.iterator(); iter.hasNext(); ) {
                final Context ctx = iter.next().getContext();
                if (!ctx.isPayment()) {
                    iter.remove();
                }
            }
            request.setAttribute("memberMemberTTs", memberMemberTTs);

            // Self payment
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.SELF_PAYMENT);
            ttQuery.setFromNature(AccountType.Nature.MEMBER);
            ttQuery.setToNature(AccountType.Nature.MEMBER);
            ttQuery.setFromAccountTypes(memberAccountTypes);
            ttQuery.setToAccountTypes(memberAccountTypes);
            request.setAttribute("memberSelfTTs", transferTypeService.search(ttQuery));

            // Chargeback received payment
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.PAYMENT);
            ttQuery.setToAccountTypes(memberAccountTypes);
            request.setAttribute("chargebackTTs", transferTypeService.search(ttQuery));

            // Groups
            final GroupQuery groupQuery = new GroupQuery();
            groupQuery.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
            groupQuery.setStatus(Group.Status.NORMAL);
            groupQuery.setIgnoreManagedBy(true);
            request.setAttribute("memberGroups", groupService.search(groupQuery));

            // Account information
            final MemberGroup memberGroup = (MemberGroup) group;
            final Collection<MemberGroup> canViewProfileOfGroups = memberGroup.getCanViewProfileOfGroups();
            if (CollectionUtils.isNotEmpty(canViewProfileOfGroups)) {
                final MemberAccountTypeQuery accountTypeQuery = new MemberAccountTypeQuery();
                accountTypeQuery.setRelatedToGroups(canViewProfileOfGroups);
                final Collection<MemberAccountType> accountTypes = (Collection<MemberAccountType>) accountTypeService.search(accountTypeQuery);
                request.setAttribute("accountTypes", accountTypes);
            }

            // Channels for payment request
            request.setAttribute("channelsSupportingPaymentRequest", channelService.listSupportingPaymentRequest());

            // Guarantee types
            setGuaranteeTypes(request, group);
        }

        if (group.getNature() == Group.Nature.ADMIN || group.getNature() == Group.Nature.BROKER) {

            // As member to system payments
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.PAYMENT);
            ttQuery.setFromNature(AccountType.Nature.MEMBER);
            ttQuery.setToNature(AccountType.Nature.SYSTEM);
            ttQuery.setToAccountTypes(systemAccountTypes);
            ttQuery.setFromAccountTypes(memberAccountTypes);
            final List<TransferType> memberSystemTTs = transferTypeService.search(ttQuery);
            request.setAttribute("asMemberToSystemTTs", memberSystemTTs);

            // As member to member payment
            ttQuery = new TransferTypeQuery();
            ttQuery.setFromNature(AccountType.Nature.MEMBER);
            ttQuery.setToNature(AccountType.Nature.MEMBER);
            ttQuery.setFromAccountTypes(memberAccountTypes);
            ttQuery.setToAccountTypes(memberAccountTypes);
            final List<TransferType> memberMemberTTs = new ArrayList<TransferType>(transferTypeService.search(ttQuery));
            for (final Iterator<TransferType> iter = memberMemberTTs.iterator(); iter.hasNext(); ) {
                final Context ctx = iter.next().getContext();
                if (!ctx.isPayment()) {
                    iter.remove();
                }
            }
            request.setAttribute("asMemberToMemberTTs", memberMemberTTs);

            // As member to self payment
            ttQuery = new TransferTypeQuery();
            ttQuery.setContext(TransactionContext.SELF_PAYMENT);
            ttQuery.setFromNature(AccountType.Nature.MEMBER);
            ttQuery.setToNature(AccountType.Nature.MEMBER);
            ttQuery.setFromAccountTypes(memberAccountTypes);
            ttQuery.setToAccountTypes(memberAccountTypes);
            final List<TransferType> memberSelfTTs = new ArrayList<TransferType>(transferTypeService.search(ttQuery));
            request.setAttribute("asMemberToSelfTTs", memberSelfTTs);

            // Member record types
            final MemberRecordTypeQuery memberRecordTypeQuery = new MemberRecordTypeQuery();
            final List<MemberRecordType> memberRecordTypes = memberRecordTypeService.search(memberRecordTypeQuery);
            request.setAttribute("memberRecordTypes", memberRecordTypes);

        }
        // should be added in all cases, as counts for all nature types:
        // conversion simulation TT's
        final List<TransferType> conversionSimulationTTs = transferTypeService.getConversionTTs();
        request.setAttribute("conversionSimulationTTs", conversionSimulationTTs);
        if (group.getNature() == Group.Nature.BROKER) {
            request.setAttribute("brokerConversionSimulationTTs", conversionSimulationTTs);
        }

        AdminGroup adminGroup = context.getGroup();
        adminGroup = getFetchService().fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        request.setAttribute("managesGroups", adminGroup.getManagesGroups());
        if (getPermissionService().checkPermission("systemGroups", permissionByNature.get(group.getNature())) && (Group.Nature.ADMIN.equals(group.getNature()) || adminGroup.getManagesGroups().contains(group))) {
            editable = true;
        }
        request.setAttribute("editable", editable);
    }

    private void initBasic(final BeanBinder<? extends GroupPermissionsDTO<?>> binder) {
        binder.registerBinder("group", PropertyBinder.instance(Group.class, "group", ReferenceConverter.instance(Group.class)));
        binder.registerBinder("operations", SimpleCollectionBinder.instance(Operation.class, "operations"));
        binder.registerBinder("documents", SimpleCollectionBinder.instance(Document.class, "documents"));
        binder.registerBinder("messageCategories", SimpleCollectionBinder.instance(MessageCategory.class, "messageCategories"));
        binder.registerBinder("guaranteeTypes", SimpleCollectionBinder.instance(GuaranteeType.class, "guaranteeTypes"));
    }

    private void initMember(final BeanBinder<? extends MemberGroupPermissionsDTO<?>> binder) {
        binder.registerBinder("canViewProfileOfGroups", SimpleCollectionBinder.instance(Group.class, "canViewProfileOfGroups"));
        binder.registerBinder("canViewAdsOfGroups", SimpleCollectionBinder.instance(Group.class, "canViewAdsOfGroups"));
        binder.registerBinder("canViewInformationOf", SimpleCollectionBinder.instance(AccountType.class, "canViewInformationOf"));
        binder.registerBinder("canIssueCertificationToGroups", SimpleCollectionBinder.instance(Group.class, "canIssueCertificationToGroups"));
        binder.registerBinder("canBuyWithPaymentObligationsFromGroups", SimpleCollectionBinder.instance(Group.class, "canBuyWithPaymentObligationsFromGroups"));
        binder.registerBinder("conversionSimulationTTs", SimpleCollectionBinder.instance(TransferType.class, "conversionSimulationTTs"));
    }

    /**
     * Sets the guarantee types list into the request as an attribute. If the specified group's nature is ADMIN then retrieves all the enabled
     * guarantee types else retrieve only the guarantee types according to the member group's currencies.
     *
     * @param request used to store the attribute
     * @param group   used to create the search query
     */
    private void setGuaranteeTypes(final HttpServletRequest request, final Group group) {
        List<GuaranteeType> guaranteeTypes = null;
        final GuaranteeTypeQuery guaranteeTypeQuery = new GuaranteeTypeQuery();
        guaranteeTypeQuery.setEnabled(true);

        if (group.getNature() == Group.Nature.ADMIN) {
            final Collection<Model> models = new ArrayList<Model>();
            models.add(Model.WITH_BUYER_AND_SELLER);
            models.add(Model.WITH_BUYER_ONLY);
            guaranteeTypeQuery.setModels(models);
        } else {
            final List<Currency> currencies = currencyService.listByMemberGroup((MemberGroup) group);
            guaranteeTypeQuery.setCurrencies(currencies);
        }
        guaranteeTypes = guaranteeTypeService.search(guaranteeTypeQuery);

        request.setAttribute("guaranteeTypes", guaranteeTypes);
    }
}
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
package nl.strohalm.cyclos.controls.groups;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.TransactionPassword;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.cards.CardType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFileQuery;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings.PasswordPolicy;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings.EmailValidation;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.RegistrationAgreement;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.Message.Type;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.accounts.AccountTypeService;
import nl.strohalm.cyclos.services.accounts.MemberAccountTypeQuery;
import nl.strohalm.cyclos.services.customization.CustomizedFileService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.TimePeriod.Field;
import nl.strohalm.cyclos.utils.access.PermissionRequestorImpl;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.MapBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.conversion.IdConverter;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;

/**
 * Action used to edit a group
 * @author luis
 * @author Jefferson Magno
 */
public class EditGroupAction extends BaseFormAction {

    public static void checkPermission(final ActionContext context, final PermissionService permissionService, final GroupService groupService, final boolean acceptInsert, final long groupId) {
        final PermissionRequestorImpl permissionRequestor = new PermissionRequestorImpl();
        boolean hasPermissionForGroup = false;

        if (groupId <= 0) { // is an insert
            if (!acceptInsert) {
                hasPermissionForGroup = false;
            } else {
                final Group.Nature nature = getGroupNature(context, true);
                if (nature == null) {
                    permissionRequestor.adminPermissions("systemGroups", "manageMember", "manageBroker", "manageAdmin");
                } else {
                    permissionRequestor.adminPermissions("systemGroups", "manage" + StringUtils.capitalize(nature.toString().toLowerCase()));
                }
                permissionRequestor.memberPermissions("memberOperators", "manage");
                hasPermissionForGroup = true;
            }
        } else {
            final Group group = groupService.load(groupId);
            switch (group.getNature()) {
                case ADMIN:
                    permissionRequestor.adminPermissions("systemGroups", "manageAdmin");
                    hasPermissionForGroup = true;
                    break;
                case MEMBER:
                case BROKER:
                    if (context.isAdmin()) {
                        final Administrator admin = context.getElement();
                        final AdminGroup adminGroup = context.getFetchService().fetch(admin.getAdminGroup(), AdminGroup.Relationships.MANAGES_GROUPS);
                        hasPermissionForGroup = adminGroup.getManagesGroups().contains(group);
                        permissionRequestor.adminPermissions("systemGroups", "manage" + StringUtils.capitalize(group.getNature().toString().toLowerCase()));
                    }
                    break;
                case OPERATOR:
                    if (context.isMember()) {
                        final GroupQuery groupQuery = new GroupQuery();
                        groupQuery.setNatures(Group.Nature.OPERATOR);
                        groupQuery.setResultType(ResultType.LIST);
                        groupQuery.setMember((Member) context.getElement());
                        hasPermissionForGroup = groupService.search(groupQuery).contains(group);
                        permissionRequestor.memberPermissions("memberOperators", "manage");
                    }
                    break;
                default:
                    throw new PermissionDeniedException();
            }
        }

        if (!hasPermissionForGroup || !permissionService.checkPermissions(permissionRequestor)) {
            throw new PermissionDeniedException();
        }
    }

    private static Group.Nature getGroupNature(final ActionContext context, final boolean acceptEmpty) {
        try {
            final EditGroupForm form = context.getForm();
            final String nature = (String) form.getGroup("nature");
            if (acceptEmpty && StringUtils.isBlank(nature)) {
                return null;
            } else {
                return Group.Nature.valueOf(nature);
            }
        } catch (final Exception e) {
            throw new ValidationException();
        }
    }

    private AccountTypeService                             accountTypeService;
    private CustomizedFileService                          customizedFileService;
    private GroupService                                   groupService;

    private TransferTypeService                            transferTypeService;

    private Map<Group.Nature, DataBinder<? extends Group>> dataBinders;

    public AccountTypeService getAccountTypeService() {
        return accountTypeService;
    }

    public CustomizedFileService getCustomizedFileService() {
        return customizedFileService;
    }

    public DataBinder<? extends Group> getDataBinder(final Group.Nature nature) {
        if (dataBinders == null) {
            dataBinders = new EnumMap<Group.Nature, DataBinder<? extends Group>>(Group.Nature.class);

            final BeanBinder<BasicGroupSettings> basicSettingsBinder = BeanBinder.instance(BasicGroupSettings.class, "basicSettings");
            final BeanBinder<MemberGroupSettings> memberSettingsBinder = BeanBinder.instance(MemberGroupSettings.class, "memberSettings");

            final BeanBinder<AdminGroup> adminGroupBinder = BeanBinder.instance(AdminGroup.class);
            initBasic(adminGroupBinder, basicSettingsBinder);
            dataBinders.put(Group.Nature.ADMIN, adminGroupBinder);

            final BeanBinder<MemberGroup> memberGroupBinder = BeanBinder.instance(MemberGroup.class);
            initBasic(memberGroupBinder, basicSettingsBinder);
            initMember(memberGroupBinder, memberSettingsBinder);
            dataBinders.put(Group.Nature.MEMBER, memberGroupBinder);

            final BeanBinder<OperatorGroup> operatorGroupBinder = BeanBinder.instance(OperatorGroup.class);
            initBasic(operatorGroupBinder, basicSettingsBinder);
            initOperator(operatorGroupBinder);
            dataBinders.put(Group.Nature.OPERATOR, operatorGroupBinder);

            final BeanBinder<BrokerGroup> brokerGroupBinder = BeanBinder.instance(BrokerGroup.class);
            initBasic(brokerGroupBinder, basicSettingsBinder);
            initMember(brokerGroupBinder, memberSettingsBinder);
            initBroker(brokerGroupBinder);
            dataBinders.put(Group.Nature.BROKER, brokerGroupBinder);
        }
        return dataBinders.get(nature);
    }

    public GroupService getGroupService() {
        return groupService;
    }

    @Inject
    public void setAccountTypeService(final AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    @Inject
    public void setCustomizedFileService(final CustomizedFileService customizedFileService) {
        this.customizedFileService = customizedFileService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Inject
    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final EditGroupForm form = context.getForm();
        final long groupId = form.getGroupId();
        checkPermission(context, getPermissionService(), groupService, true, groupId);
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final EditGroupForm form = context.getForm();
        Group group = readGroup(context);
        final Group baseGroup = readBaseGroup(context);

        final boolean isInsert = group.getId() == null;
        if (isInsert) {
            if (group instanceof AdminGroup) {
                group = groupService.insertAdmin((AdminGroup) group, (AdminGroup) baseGroup);
            } else if (group instanceof OperatorGroup) {
                final Member member = (Member) context.getElement();
                final OperatorGroup operatorGroup = (OperatorGroup) group;
                operatorGroup.setMember(member);
                group = groupService.insertOperator(operatorGroup, (OperatorGroup) baseGroup);
            } else if (group instanceof BrokerGroup) {
                group = groupService.insertBroker((BrokerGroup) group, (BrokerGroup) baseGroup);
            } else {
                group = groupService.insertMember((MemberGroup) group, (MemberGroup) baseGroup);
            }
        } else {
            if (group instanceof AdminGroup) {
                group = groupService.updateAdmin((AdminGroup) group);
            } else if (group instanceof OperatorGroup) {
                final Member member = (Member) context.getElement();
                final OperatorGroup operatorGroup = (OperatorGroup) group;
                operatorGroup.setMember(member);
                group = groupService.updateOperator(operatorGroup);
            } else if (group instanceof BrokerGroup) {
                group = groupService.updateBroker((BrokerGroup) group);
            } else {
                group = groupService.updateMember((MemberGroup) group, form.isForceAccept());
            }
        }

        group = getFetchService().reload(group, Group.Relationships.CUSTOMIZED_FILES);

        // Physically update the files
        final ServletContext servletContext = getServlet().getServletContext();
        for (final CustomizedFile file : group.getCustomizedFiles()) {
            final File physicalFile = CustomizationHelper.customizedFileOf(servletContext, file);
            CustomizationHelper.updateFile(servletContext, physicalFile, file);
        }

        context.sendMessage(isInsert ? "group.inserted" : "group.modified");
        return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "groupId", group.getId());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final EditGroupForm form = context.getForm();
        final long id = form.getGroupId();

        boolean editable = false;

        final Map<Group.Nature, String> permissionByNature = new EnumMap<Group.Nature, String>(Group.Nature.class);
        permissionByNature.put(Group.Nature.ADMIN, "manageAdmin");
        permissionByNature.put(Group.Nature.BROKER, "manageBroker");
        permissionByNature.put(Group.Nature.MEMBER, "manageMember");

        final boolean isInsert = id <= 0L;
        if (isInsert) {
            // Prepare for insert
            List<Group.Nature> natures = new ArrayList<Group.Nature>();
            if (context.isAdmin()) {
                // Put in the request the name of permission used to manage a type of group
                request.setAttribute("permissionByNature", permissionByNature);

                final PermissionService permissionService = getPermissionService();
                for (final Group.Nature nature : permissionByNature.keySet()) {
                    final String operation = permissionByNature.get(nature);
                    if (permissionService.checkPermission("systemGroups", operation)) {
                        natures.add(nature);
                    }
                }
            } else {
                // It's a member inserting an operator group
                final GroupQuery groupQuery = new GroupQuery();
                groupQuery.setMember((Member) context.getElement());
                final List<OperatorGroup> baseGroups = (List<OperatorGroup>) groupService.search(groupQuery);
                request.setAttribute("baseGroups", baseGroups);
                request.setAttribute("isOperatorGroup", true);
                natures = Collections.singletonList(Group.Nature.OPERATOR);
            }
            request.setAttribute("natures", natures);
            RequestHelper.storeEnum(request, Group.Status.class, "status");
            editable = true;
        } else {
            // Prepare for modify
            Group group = groupService.load(id, Group.Relationships.CUSTOMIZED_FILES, MemberGroup.Relationships.CHANNELS);
            final boolean isMemberGroup = MemberGroup.class.isAssignableFrom(group.getNature().getGroupClass());
            final boolean isBrokerGroup = BrokerGroup.class.isAssignableFrom(group.getNature().getGroupClass());
            final boolean isOperatorGroup = OperatorGroup.class.isAssignableFrom(group.getNature().getGroupClass());
            if (group.getStatus().isEnabled()) {
                request.setAttribute("deactivationTimePeriodFields", Arrays.asList(Field.SECONDS, Field.MINUTES, Field.HOURS, Field.DAYS));
                request.setAttribute("passwordExpiresAfterFields", Arrays.asList(Field.DAYS, Field.WEEKS, Field.MONTHS, Field.YEARS));
                RequestHelper.storeEnum(request, TransactionPassword.class, "transactionPasswords");
                if (isMemberGroup) {
                    final MemberGroup memberGroup = (MemberGroup) group;

                    // Check if the group has access to a channel that uses pin
                    final boolean usesPin = groupService.usesPin(memberGroup);
                    request.setAttribute("usesPin", usesPin);

                    // Retrieve the registration agreements
                    final List<RegistrationAgreement> registrationAgreements = groupService.agreementsForGroupEdition();
                    request.setAttribute("registrationAgreements", registrationAgreements);

                    // Retrieve the associated accounts
                    request.setAttribute("timePeriodFields", Arrays.asList(Field.DAYS, Field.WEEKS, Field.MONTHS, Field.YEARS));
                    final MemberAccountTypeQuery atQuery = new MemberAccountTypeQuery();
                    atQuery.setRelatedToGroup(memberGroup);
                    request.setAttribute("accountTypes", accountTypeService.search(atQuery));

                    // Sort the message types using their messages
                    final List<Type> messageTypes = Arrays.asList(Message.Type.values());
                    final Comparator cmp = new Comparator() {
                        public int compare(final Object o1, final Object o2) {
                            final String msg1 = context.message("message.type." + o1);
                            final String msg2 = context.message("message.type." + o2);
                            return msg1.compareTo(msg2);
                        }
                    };
                    Collections.sort(messageTypes, cmp);
                    request.setAttribute("messageTypes", messageTypes);

                    // we create a wrapper ArrayList because the list must implement the remove method
                    final List<Type> smsMessageTypes = new ArrayList<Type>(Arrays.asList(Message.Type.values()));
                    CollectionUtils.filter(smsMessageTypes, new Predicate() {
                        public boolean evaluate(final Object object) {
                            final Message.Type type = (Message.Type) object;
                            switch (type) {
                                case FROM_MEMBER:
                                                            case FROM_ADMIN_TO_MEMBER:
                                                            case FROM_ADMIN_TO_GROUP:
                                                                return false;
                                                            default:
                                                                return true;
                                                        }
                                                    }
                    });
                    Collections.sort(smsMessageTypes, cmp);
                    request.setAttribute("smsMessageTypes", smsMessageTypes);

                    // Store the possible groups for expiration
                    final GroupQuery query = new GroupQuery();
                    query.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
                    final List<? extends Group> groups = groupService.search(query);
                    groups.remove(group);
                    request.setAttribute("possibleExpirationGroups", groups);
                    request.setAttribute("expirationTimeFields", Arrays.asList(TimePeriod.Field.DAYS, TimePeriod.Field.WEEKS, TimePeriod.Field.MONTHS, TimePeriod.Field.YEARS));

                    // Store transfer types for SMS charge
                    final TransferTypeQuery ttQuery = new TransferTypeQuery();
                    ttQuery.setFromGroups(Collections.singletonList(memberGroup));
                    ttQuery.setToNature(AccountType.Nature.SYSTEM);
                    final List<TransferType> smsChargeTransferTypes = transferTypeService.search(ttQuery);
                    request.setAttribute("smsChargeTransferTypes", smsChargeTransferTypes);

                    request.setAttribute("smsAdditionalChargedPeriodFields", Arrays.asList(Field.DAYS, Field.WEEKS, Field.MONTHS));

                    // Retrieve the card types
                    final List<CardType> cardTypes = new ArrayList<CardType>();
                    cardTypes.addAll(groupService.cardTypesForGroupEdition());
                    request.setAttribute("cardTypes", cardTypes);
                }
                if (isBrokerGroup) {
                    // Retrieve the possible groups for registered members by broker
                    final GroupQuery query = new GroupQuery();
                    query.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
                    query.setStatus(Group.Status.NORMAL);
                    request.setAttribute("memberGroups", groupService.search(query));
                }
                if (isMemberGroup || isBrokerGroup) {
                    RequestHelper.storeEnum(request, MemberGroupSettings.ExternalAdPublication.class, "externalAdPublications");
                }
                if (isOperatorGroup) {
                    // Load the associated transaction types for the max amount per day
                    group = getFetchService().fetch(group, RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP), OperatorGroup.Relationships.MAX_AMOUNT_PER_DAY_BY_TRANSFER_TYPE, OperatorGroup.Relationships.MAX_AMOUNT_PER_DAY_BY_TRANSFER_TYPE);
                    final OperatorGroup operatorGroup = (OperatorGroup) group;
                    request.setAttribute("transferTypes", operatorGroup.getMember().getGroup().getTransferTypes());
                }

                // Retrieve the associated customized files
                final CustomizedFileQuery cfQuery = new CustomizedFileQuery();
                cfQuery.setGroup(group);
                request.setAttribute("customizedFiles", customizedFileService.search(cfQuery));

                // Check whether the login page name will be shown
                request.setAttribute("showLoginPageName", CustomizationHelper.isAnyFileRelatedToLoginPage(group.getCustomizedFiles()));
            }
            getDataBinder(group.getNature()).writeAsString(form.getGroup(), group);
            request.setAttribute("group", group);
            request.setAttribute("isMemberGroup", isMemberGroup);
            request.setAttribute("isBrokerGroup", isBrokerGroup);
            request.setAttribute("isOperatorGroup", isOperatorGroup);

            if (isMemberGroup) {
                // Show scheduling options when there's a schedulable transfer type
                final TransferTypeQuery ttQuery = new TransferTypeQuery();
                ttQuery.setPageForCount();
                ttQuery.setContext(TransactionContext.PAYMENT);
                ttQuery.setGroup(group);
                ttQuery.setSchedulable(true);
                request.setAttribute("showScheduling", PageHelper.getTotalCount(transferTypeService.search(ttQuery)) > 0);

                // List channels
                request.setAttribute("channels", groupService.externalChannelsForMemberGroupEdition());

                RequestHelper.storeEnum(request, EmailValidation.class, "emailValidations");
            }

            if (context.isAdmin()) {
                AdminGroup adminGroup = context.getGroup();
                adminGroup = getFetchService().fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                if (getPermissionService().checkPermission("systemGroups", permissionByNature.get(group.getNature())) && (Group.Nature.ADMIN.equals(group.getNature()) || adminGroup.getManagesGroups().contains(group))) {
                    editable = true;
                }
            } else {
                // It's a member updating an operator group
                editable = getPermissionService().checkPermission("memberOperators", "manage");
            }
        }

        final boolean canManageFiles = context.isAdmin() ? getPermissionService().checkPermission("adminMemberGroups", "manageMemberCustomizedFiles") : true;
        request.setAttribute("isInsert", isInsert);
        request.setAttribute("editable", editable);
        request.setAttribute("canManageFiles", canManageFiles);
        RequestHelper.storeEnum(request, PasswordPolicy.class, "passwordPolicies");
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final Group group = readGroup(context);
        // Check if it's a insert operation
        if (group.isTransient()) {
            groupService.validateInsert(group);
        } else {
            if (group instanceof AdminGroup) {
                groupService.validateAdmin((AdminGroup) group);
            } else if (group instanceof OperatorGroup) {
                groupService.validateOperator((OperatorGroup) group);
            } else if (group instanceof BrokerGroup) {
                groupService.validateBroker((BrokerGroup) group);
            } else { // MemberGroup
                final EditGroupForm form = context.getForm();
                final MemberGroup memberGroup = (MemberGroup) group;
                if (form.isUseCustomSMSContextClass() && StringUtils.isEmpty(memberGroup.getMemberSettings().getSmsContextClassName())) {
                    throw new ValidationException("errors.required", context.message("group.settings.smsContextClassName"));
                }
                groupService.validateMember(memberGroup);
            }
        }
    }

    private void initBasic(final BeanBinder<? extends Group> groupBinder, final BeanBinder<? extends BasicGroupSettings> basicSettingsBinder) {
        groupBinder.registerBinder("basicSettings", basicSettingsBinder);

        groupBinder.registerBinder("id", PropertyBinder.instance(Long.class, "id", IdConverter.instance()));
        groupBinder.registerBinder("name", PropertyBinder.instance(String.class, "name"));
        groupBinder.registerBinder("description", PropertyBinder.instance(String.class, "description"));
        groupBinder.registerBinder("status", PropertyBinder.instance(Group.Status.class, "status"));
        groupBinder.registerBinder("loginPageName", PropertyBinder.instance(String.class, "loginPageName"));
        groupBinder.registerBinder("containerUrl", PropertyBinder.instance(String.class, "containerUrl"));

        basicSettingsBinder.registerBinder("passwordLength", DataBinderHelper.rangeConstraintBinder("passwordLength"));
        basicSettingsBinder.registerBinder("passwordPolicy", PropertyBinder.instance(PasswordPolicy.class, "passwordPolicy"));
        basicSettingsBinder.registerBinder("maxPasswordWrongTries", PropertyBinder.instance(Integer.TYPE, "maxPasswordWrongTries"));
        basicSettingsBinder.registerBinder("deactivationAfterMaxPasswordTries", DataBinderHelper.timePeriodBinder("deactivationAfterMaxPasswordTries"));
        basicSettingsBinder.registerBinder("passwordExpiresAfter", DataBinderHelper.timePeriodBinder("passwordExpiresAfter"));
        basicSettingsBinder.registerBinder("transactionPassword", PropertyBinder.instance(TransactionPassword.class, "transactionPassword"));
        basicSettingsBinder.registerBinder("transactionPasswordLength", PropertyBinder.instance(Integer.TYPE, "transactionPasswordLength"));
        basicSettingsBinder.registerBinder("maxTransactionPasswordWrongTries", PropertyBinder.instance(Integer.TYPE, "maxTransactionPasswordWrongTries"));
    }

    private void initBroker(final BeanBinder<? extends BrokerGroup> brokerGroupBinder) {
        brokerGroupBinder.registerBinder("possibleInitialGroups", SimpleCollectionBinder.instance(MemberGroup.class, "possibleInitialGroups"));
    }

    private void initMember(final BeanBinder<? extends MemberGroup> memberGroupBinder, final BeanBinder<? extends MemberGroupSettings> memberSettingsBinder) {
        memberGroupBinder.registerBinder("memberSettings", memberSettingsBinder);
        memberGroupBinder.registerBinder("active", PropertyBinder.instance(Boolean.TYPE, "active"));
        memberGroupBinder.registerBinder("initialGroup", PropertyBinder.instance(Boolean.TYPE, "initialGroup"));
        memberGroupBinder.registerBinder("initialGroupShow", PropertyBinder.instance(String.class, "initialGroupShow"));
        memberGroupBinder.registerBinder("registrationAgreement", PropertyBinder.instance(RegistrationAgreement.class, "registrationAgreement"));
        memberGroupBinder.registerBinder("defaultMailMessages", SimpleCollectionBinder.instance(Message.Type.class, "defaultMailMessages"));
        memberGroupBinder.registerBinder("smsMessages", SimpleCollectionBinder.instance(Message.Type.class, "smsMessages"));
        memberGroupBinder.registerBinder("defaultSmsMessages", SimpleCollectionBinder.instance(Message.Type.class, "defaultSmsMessages"));
        memberGroupBinder.registerBinder("channels", SimpleCollectionBinder.instance(Channel.class, "channels"));
        memberGroupBinder.registerBinder("defaultChannels", SimpleCollectionBinder.instance(Channel.class, "defaultChannels"));
        memberGroupBinder.registerBinder("cardType", PropertyBinder.instance(CardType.class, "cardType"));

        final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
        memberSettingsBinder.registerBinder("pinLength", DataBinderHelper.rangeConstraintBinder("pinLength"));
        memberSettingsBinder.registerBinder("maxPinWrongTries", PropertyBinder.instance(Integer.TYPE, "maxPinWrongTries"));
        memberSettingsBinder.registerBinder("pinBlockTimeAfterMaxTries", DataBinderHelper.timePeriodBinder("pinBlockTimeAfterMaxTries"));
        memberSettingsBinder.registerBinder("pinLength", DataBinderHelper.rangeConstraintBinder("pinLength"));
        memberSettingsBinder.registerBinder("smsChargeTransferType", PropertyBinder.instance(TransferType.class, "smsChargeTransferType"));
        memberSettingsBinder.registerBinder("smsChargeAmount", PropertyBinder.instance(BigDecimal.class, "smsChargeAmount", localSettings.getNumberConverter()));
        memberSettingsBinder.registerBinder("smsFree", PropertyBinder.instance(Integer.TYPE, "smsFree"));
        memberSettingsBinder.registerBinder("smsShowFreeThreshold", PropertyBinder.instance(Integer.TYPE, "smsShowFreeThreshold"));
        memberSettingsBinder.registerBinder("smsAdditionalCharged", PropertyBinder.instance(Integer.TYPE, "smsAdditionalCharged"));
        memberSettingsBinder.registerBinder("smsAdditionalChargedPeriod", DataBinderHelper.timePeriodBinder("smsAdditionalChargedPeriod"));
        memberSettingsBinder.registerBinder("smsContextClassName", PropertyBinder.instance(String.class, "smsContextClassName"));

        memberSettingsBinder.registerBinder("maxAdsPerMember", PropertyBinder.instance(Integer.TYPE, "maxAdsPerMember"));
        memberSettingsBinder.registerBinder("maxAdImagesPerMember", PropertyBinder.instance(Integer.TYPE, "maxAdImagesPerMember"));
        memberSettingsBinder.registerBinder("defaultAdPublicationTime", DataBinderHelper.timePeriodBinder("defaultAdPublicationTime"));
        memberSettingsBinder.registerBinder("maxAdPublicationTime", DataBinderHelper.timePeriodBinder("maxAdPublicationTime"));
        memberSettingsBinder.registerBinder("enablePermanentAds", PropertyBinder.instance(Boolean.TYPE, "enablePermanentAds"));
        memberSettingsBinder.registerBinder("externalAdPublication", PropertyBinder.instance(MemberGroupSettings.ExternalAdPublication.class, "externalAdPublication"));
        memberSettingsBinder.registerBinder("maxAdDescriptionSize", PropertyBinder.instance(Integer.TYPE, "maxAdDescriptionSize"));
        memberSettingsBinder.registerBinder("sendPasswordByEmail", PropertyBinder.instance(Boolean.TYPE, "sendPasswordByEmail"));
        memberSettingsBinder.registerBinder("emailValidation", PropertyBinder.instance(EmailValidation.class, "emailValidation"));
        memberSettingsBinder.registerBinder("maxImagesPerMember", PropertyBinder.instance(Integer.TYPE, "maxImagesPerMember"));
        memberSettingsBinder.registerBinder("viewLoansByGroup", PropertyBinder.instance(Boolean.TYPE, "viewLoansByGroup"));
        memberSettingsBinder.registerBinder("repayLoanByGroup", PropertyBinder.instance(Boolean.TYPE, "repayLoanByGroup"));
        memberSettingsBinder.registerBinder("maxSchedulingPayments", PropertyBinder.instance(Integer.TYPE, "maxSchedulingPayments"));
        memberSettingsBinder.registerBinder("maxSchedulingPeriod", DataBinderHelper.timePeriodBinder("maxSchedulingPeriod"));
        memberSettingsBinder.registerBinder("expireMembersAfter", DataBinderHelper.timePeriodBinder("expireMembersAfter"));
        memberSettingsBinder.registerBinder("groupAfterExpiration", PropertyBinder.instance(MemberGroup.class, "groupAfterExpiration"));

    }

    private void initOperator(final BeanBinder<? extends OperatorGroup> operatorGroupBinder) {
        // Bind max amount per day by transfer type map
        final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
        final PropertyBinder<TransferType> keyBinder = PropertyBinder.instance(TransferType.class, "transferTypeIds");
        final PropertyBinder<BigDecimal> valueBinder = PropertyBinder.instance(BigDecimal.class, "maxAmountPerDayByTT", localSettings.getNumberConverter());
        final MapBinder<TransferType, BigDecimal> maxAmountBinder = MapBinder.instance(keyBinder, valueBinder);
        operatorGroupBinder.registerBinder("maxAmountPerDayByTransferType", maxAmountBinder);
    }

    private Group readBaseGroup(final ActionContext context) {
        final EditGroupForm form = context.getForm();
        final long baseGroupId = form.getBaseGroupId();
        if (baseGroupId < 1) {
            return null;
        }
        return groupService.load(baseGroupId);
    }

    private Group readGroup(final ActionContext context) {
        final EditGroupForm form = context.getForm();
        final long id = form.getGroupId();
        Group.Nature nature;
        final boolean isInsert = (id <= 0L);
        if (isInsert) {
            nature = getGroupNature(context, false);
            // On insert, empty status means normal
            final String status = (String) form.getGroup("status");
            if (StringUtils.isEmpty(status)) {
                form.setGroup("status", Group.Status.NORMAL.name());
            }
        } else {
            nature = groupService.load(id).getNature();
        }
        return getDataBinder(nature).readFromString(form.getGroup());
    }

}
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
package nl.strohalm.cyclos.services.groups;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.accounts.MemberGroupAccountSettingsDAO;
import nl.strohalm.cyclos.dao.accounts.fee.account.AccountFeeDAO;
import nl.strohalm.cyclos.dao.accounts.fee.transaction.TransactionFeeDAO;
import nl.strohalm.cyclos.dao.accounts.transactions.PaymentFilterDAO;
import nl.strohalm.cyclos.dao.customizations.CustomFieldDAO;
import nl.strohalm.cyclos.dao.customizations.CustomizedFileDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.groups.GroupDAO;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.dao.members.MemberRecordTypeDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.Operation;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.cards.CardType;
import nl.strohalm.cyclos.entities.accounts.fees.account.AccountFee;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentFilter;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.fields.AdminCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings.PasswordPolicy;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.RegistrationAgreement;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.members.messages.MessageCategory;
import nl.strohalm.cyclos.entities.members.messages.Message.Type;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.AccountTypeService;
import nl.strohalm.cyclos.services.accounts.CreditLimitDTO;
import nl.strohalm.cyclos.services.accounts.MemberAccountHandler;
import nl.strohalm.cyclos.services.accounts.cards.CardTypeService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.services.elements.RegistrationAgreementService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.sms.ISmsContext;
import nl.strohalm.cyclos.services.sms.MemberGroupSmsContextImpl;
import nl.strohalm.cyclos.services.sms.exceptions.SmsContextInitializationException;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.RequiredValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Implementation for group service.
 * @author rafael
 * @author luis
 * @author Jefferson Magno
 */
public class GroupServiceImpl implements GroupService, ApplicationContextAware {

    private final class CustomSMSContextValidation implements PropertyValidation {

        private static final long serialVersionUID = 1L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final MemberGroup group = (MemberGroup) object;
            try {
                if (StringUtils.isNotEmpty(group.getMemberSettings().getSmsContextClassName())) {
                    instantiateCustomSmsContext(group);
                }
                return null;
            } catch (final Exception e) {
                LOG.error("Error instantiating custom SMS context", e);
                return new ValidationError("group.settings.smsCustomContextInvalid");
            }
        }

    }

    /**
     * A custom validation that process the given validation only when the member group is set to expire members after a given time period
     * @author luis
     */
    private final class ExpirationValidation implements PropertyValidation {

        private static final long        serialVersionUID = 7237205242667756245L;
        private final PropertyValidation validation;

        private ExpirationValidation(final PropertyValidation validation) {
            this.validation = validation;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final MemberGroup group = (MemberGroup) object;
            final TimePeriod expireMembersAfter = group.getMemberSettings().getExpireMembersAfter();
            if (expireMembersAfter != null && expireMembersAfter.getNumber() > 0) {
                return validation.validate(object, property, value);
            }
            return null;
        }
    }

    /**
     * Ensures that a password policy is consistent with the access settings
     * @author luis
     */
    private final class PasswordPolicyValidation implements PropertyValidation {

        private static final long serialVersionUID = 5513735809903251255L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final PasswordPolicy policy = (PasswordPolicy) value;
            if (policy == null) {
                return null;
            }
            final Group group = (Group) object;
            final AccessSettings accessSettings = settingsService.getAccessSettings();
            final boolean virtualKeyboard = accessSettings.isVirtualKeyboard();
            final boolean numericPassword = accessSettings.isNumericPassword();

            if (policy == PasswordPolicy.AVOID_OBVIOUS_LETTERS_NUMBERS_SPECIAL && virtualKeyboard) {
                // Special chars cannot be typed with the virtual keyboard
                return new ValidationError("group.error.passwordPolicySpecialVirtualKeyboard");
            } else if (group.getNature() != Group.Nature.ADMIN && policy.isForceCharacters() && numericPassword) {
                // Admin groups don't use this, but ensure for other groups that if numeric password, there are no characters enforcements
                return new ValidationError("group.error.passwordPolicyNumeric");
            }
            return null;
        }

    }

    private final class PasswordTrialsValidation implements PropertyValidation {
        private static final long serialVersionUID = -5445950747956604765L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final Group group = (Group) object;

            if (((Integer) value == 0) && (group.getBasicSettings().getMaxPasswordWrongTries() > 0)) {
                return new InvalidError();
            }
            return null;
        }
    }

    private final class PINBlockTimeValidation implements PropertyValidation {

        private static final long        serialVersionUID = 7237205242667756245L;
        private final PropertyValidation validation;

        private PINBlockTimeValidation(final PropertyValidation validation) {
            this.validation = validation;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final MemberGroup group = (MemberGroup) object;
            final TimePeriod expireMembersAfter = group.getMemberSettings().getPinBlockTimeAfterMaxTries();
            if (expireMembersAfter != null && expireMembersAfter.getNumber() > 0) {
                return validation.validate(object, property, value);
            }
            return null;
        }
    }

    private final class PinTrialsValidation implements PropertyValidation {
        private static final long serialVersionUID = -5445950747956604765L;

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final MemberGroup group = (MemberGroup) object;

            if (((Integer) value == 0) && (group.getMemberSettings().getMaxPinWrongTries() > 0)) {
                return new InvalidError();
            }
            return null;
        }
    }

    private static final Relationship[]   FETCH_TO_KEEP_DATA = { Group.Relationships.PERMISSIONS, Group.Relationships.TRANSFER_TYPES, Group.Relationships.CONVERSION_SIMULATION_TTS, BrokerGroup.Relationships.BROKER_CONVERSION_SIMULATION_TTS, Group.Relationships.DOCUMENTS, Group.Relationships.MESSAGE_CATEGORIES, BrokerGroup.Relationships.BROKER_DOCUMENTS, BrokerGroup.Relationships.BROKER_MEMBER_RECORD_TYPES, AdminGroup.Relationships.MANAGES_GROUPS, Group.Relationships.CHARGEBACK_TRANSFER_TYPES, AdminGroup.Relationships.TRANSFER_TYPES_AS_MEMBER, AdminGroup.Relationships.VIEW_INFORMATION_OF, AdminGroup.Relationships.VIEW_CONNECTED_ADMINS_OF, AdminGroup.Relationships.VIEW_ADMIN_RECORD_TYPES, MemberGroup.Relationships.CAN_VIEW_ADS_OF_GROUPS, MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS, MemberGroup.Relationships.MANAGED_BY_GROUPS, Group.Relationships.GUARANTEE_TYPES };
    private static final Relationship[]   FETCH_TO_REMOVE    = { Group.Relationships.PAYMENT_FILTERS, AdminGroup.Relationships.CONNECTED_ADMINS_VIEWED_BY, AdminGroup.Relationships.ADMIN_CUSTOM_FIELDS, MemberGroup.Relationships.ACCOUNT_FEES, MemberGroup.Relationships.MANAGED_BY_GROUPS, MemberGroup.Relationships.CUSTOM_FIELDS, MemberGroup.Relationships.FROM_TRANSACTION_FEES, MemberGroup.Relationships.TO_TRANSACTION_FEES, MemberGroup.Relationships.MEMBER_RECORD_TYPES };
    private static final Log              LOG                = LogFactory.getLog(GroupService.class);

    private AccountFeeDAO                 accountFeeDao;
    private AccountService                accountService;
    private AccountTypeService            accountTypeService;
    private CustomFieldDAO                customFieldDao;
    private CustomizedFileDAO             customizedFileDao;
    private GroupDAO                      groupDao;
    private ElementDAO                    elementDao;
    private ElementService                elementService;
    private MemberGroupAccountSettingsDAO memberGroupAccountSettingsDao;
    private MemberRecordTypeDAO           memberRecordTypeDao;
    private MemberService                 memberService;
    private ChannelService                channelService;
    private PaymentFilterDAO              paymentFilterDao;
    private TransferTypeService           transferTypeService;
    private TransactionFeeDAO             transactionFeeDao;
    private MemberAccountHandler          memberAccountHandler;
    private PermissionService             permissionService;
    private FetchService                  fetchService;
    private CardTypeService               cardTypeService;
    private CustomFieldService            customFieldService;
    private RegistrationAgreementService  registrationAgreementService;
    private ApplicationContext            applicationContext;
    private SettingsService               settingsService;
    private AccessService                 accessService;
    private Map<Long, ISmsContext>        cachedSmsContexts  = Collections.synchronizedMap(new HashMap<Long, ISmsContext>());

    public List<RegistrationAgreement> agreementsForGroupEdition() {
        return registrationAgreementService.listAll();
    }

    public List<CardType> cardTypesForGroupEdition() {
        return cardTypeService.listAll();
    }

    public boolean checkPermission(Group group, final TransferType transferType) {
        group = fetchService.fetch(group, Group.Relationships.TRANSFER_TYPES);
        final Collection<TransferType> transferTypes = group.getTransferTypes();
        return transferTypes != null && transferTypes.contains(transferType);
    }

    /**
     * Clear the sms context for the specified group
     * @param group
     */
    public void clearSmsContext(final MemberGroup group) {
        final ISmsContext ctx = cachedSmsContexts.remove(group.getId());
        if (ctx instanceof DisposableBean) {
            try {
                ((DisposableBean) ctx).destroy();
            } catch (final Throwable th) {
                LOG.error("Error cleaning sms context for group: " + group.getName() + "(" + ctx.getClass().getName() + ")", th);
            }
        }
    }

    /**
     * Clear the sms context cache
     */
    public void clearSmsContextCache() {
        synchronized (cachedSmsContexts) {
            for (final Map.Entry<Long, ISmsContext> entry : cachedSmsContexts.entrySet()) {
                final ISmsContext ctx = entry.getValue();
                try {
                    if (entry instanceof DisposableBean) {
                        ((DisposableBean) ctx).destroy();
                    }
                } catch (final Throwable th) {
                    LOG.error("Error cleaning sms context for group id: " + entry.getKey() + " (" + ctx.getClass().getName() + ")", th);
                }
            }
        }
        cachedSmsContexts.clear();
    }

    public List<Channel> externalChannelsForMemberGroupEdition() {
        return channelService.listExternal();
    }

    public Group findByLoginPageName(final String loginPageName) {
        return groupDao.findByLoginPageName(loginPageName);
    }

    public AccountService getAccountService() {
        return accountService;
    }

    public CustomFieldDAO getCustomFieldDao() {
        return customFieldDao;
    }

    public CustomizedFileDAO getCustomizedFileDao() {
        return customizedFileDao;
    }

    public ElementService getElementService() {
        return elementService;
    }

    public FetchService getFetchService() {
        return fetchService;
    }

    public GroupDAO getGroupDao() {
        return groupDao;
    }

    public MemberAccountHandler getMemberAccountHandler() {
        return memberAccountHandler;
    }

    public MemberGroupAccountSettingsDAO getMemberGroupAccountSettingsDao() {
        return memberGroupAccountSettingsDao;
    }

    public MemberRecordTypeDAO getMemberRecordTypeDao() {
        return memberRecordTypeDao;
    }

    public MemberService getMemberService() {
        return memberService;
    }

    public PaymentFilterDAO getPaymentFilterDao() {
        return paymentFilterDao;
    }

    public PermissionService getPermissionService() {
        return permissionService;
    }

    @SuppressWarnings("unchecked")
    public List<MemberGroup> getPossibleInitialGroups() {
        if (LoggedUser.isValid() && LoggedUser.isBroker()) {
            BrokerGroup brokerGroup = LoggedUser.group();
            brokerGroup = fetchService.fetch(brokerGroup, BrokerGroup.Relationships.POSSIBLE_INITIAL_GROUPS);
            return (List<MemberGroup>) brokerGroup.getPossibleInitialGroups();
        }
        final GroupQuery query = new GroupQuery();
        query.setNatures(Group.Nature.BROKER, Group.Nature.MEMBER);
        final List<MemberGroup> groups = (List<MemberGroup>) search(query);
        for (final Iterator iter = groups.iterator(); iter.hasNext();) {
            final MemberGroup group = (MemberGroup) iter.next();
            if (!group.isInitialGroup()) {
                iter.remove();
            }
        }
        return groups;
    }

    public List<? extends Group> getPossibleNewGroups(Group group) {
        group = fetchService.fetch(group);
        if (group.getStatus() == Group.Status.REMOVED) {
            return Collections.singletonList(group);
        }
        final GroupQuery query = new GroupQuery();
        if (group instanceof OperatorGroup) {
            query.setNatures(Group.Nature.OPERATOR);
            query.setMember(((OperatorGroup) group).getMember());
        } else {
            if (group.getNature() == Group.Nature.ADMIN) {
                query.setNatures(Group.Nature.ADMIN);
            } else {
                query.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
            }
        }
        final List<? extends Group> groups = search(query);
        groups.remove(group);
        return groups;
    }

    public ISmsContext getSmsContext(final MemberGroup group) throws SmsContextInitializationException {
        ISmsContext ctx = cachedSmsContexts.get(group.getId());
        if (ctx == null) {
            if (StringUtils.isEmpty(group.getMemberSettings().getSmsContextClassName())) {
                ctx = new MemberGroupSmsContextImpl();
            } else {
                ctx = instantiateCustomSmsContext(group);
            }
            cachedSmsContexts.put(group.getId(), ctx);
        }
        return ctx;
    }

    public TransferTypeService getTransferTypeService() {
        return transferTypeService;
    }

    public boolean hasGroupsWhichRequiresSpecialOnPassword() {
        final GroupQuery query = new GroupQuery();
        query.setStatus(Group.Status.NORMAL);
        for (final Group group : search(query)) {
            if (group.getBasicSettings().getPasswordPolicy() == PasswordPolicy.AVOID_OBVIOUS_LETTERS_NUMBERS_SPECIAL) {
                return true;
            }
        }
        return false;
    }

    public boolean hasMemberGroupsWhichEnforcesCharactersOnPassword() {
        final GroupQuery query = new GroupQuery();
        query.setStatus(Group.Status.NORMAL);
        query.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
        for (final Group group : search(query)) {
            if (group.getBasicSettings().getPasswordPolicy().isForceCharacters()) {
                return true;
            }
        }
        return false;
    }

    public MemberGroupAccountSettings insertAccountSettings(final MemberGroupAccountSettings settings) {
        // Check if the admin group has the permission to manage the member group
        final MemberGroup memberGroup = fetchService.fetch(settings.getGroup());
        AdminGroup adminGroup = LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
        if (!managesGroups.contains(memberGroup)) {
            throw new PermissionDeniedException();
        }

        validate(settings);
        // Check if the account type isn't already related to the group
        try {
            memberGroupAccountSettingsDao.load(settings.getGroup().getId(), settings.getAccountType().getId());
            throw new UnexpectedEntityException();
        } catch (final EntityNotFoundException e) {
            // Ok, the account is not related to the group, so we can go on
        }
        final MemberAccountType currentDefault = accountTypeService.getDefault(memberGroup);
        if (currentDefault == null) {
            // When there's no current default, set this one as default
            settings.setDefault(true);
        } else if (settings.isDefault()) {
            // When there was a default already and this one is marked as default, unmark the previous one
            final MemberGroupAccountSettings defaultSettings = memberGroupAccountSettingsDao.load(memberGroup.getId(), currentDefault.getId());
            defaultSettings.setDefault(false);
            memberGroupAccountSettingsDao.update(defaultSettings);
        }
        final MemberGroupAccountSettings saved = memberGroupAccountSettingsDao.insert(settings);
        final List<Member> members = memberService.listByGroup(settings.getGroup());
        for (final Member member : members) {
            memberAccountHandler.activate(member, saved.getAccountType());
        }

        // When inserting an account to an inactive group, activate it
        if (!memberGroup.isActive()) {
            memberGroup.setActive(true);
            groupDao.update(memberGroup);
            elementDao.activateMembersOfGroup(memberGroup);
        }
        return saved;
    }

    public AdminGroup insertAdmin(AdminGroup group, final AdminGroup baseGroup) {
        if (baseGroup != null) {
            // Copy settings from base group
            group = (AdminGroup) copyBaseGroupSettings(group, baseGroup);
            customFieldService.clearCache(CustomField.Nature.ADMIN);
        } else {
            group.setBasicSettings(new BasicGroupSettings());
        }

        // Validate and save
        validateInsert(group);
        group = (AdminGroup) save(group);

        // Copy inverse collections from base group
        if (baseGroup != null) {
            copyInverseCollections(group, baseGroup);
        }

        return group;
    }

    public BrokerGroup insertBroker(BrokerGroup group, final BrokerGroup baseGroup) {
        if (baseGroup != null) {
            // Copy settings from base group
            group = (BrokerGroup) copyBaseGroupSettings(group, baseGroup);
            customFieldService.clearCache(CustomField.Nature.MEMBER);
        } else {
            group.setBasicSettings(new BasicGroupSettings());
            group.setMemberSettings(new MemberGroupSettings());
        }

        // Validate and save
        validateInsert(group);
        group = (BrokerGroup) save(group);

        // Copy inverse collections from base group
        if (baseGroup != null) {
            copyInverseCollections(group, baseGroup);
        }

        // Add permission to admin group over the member group
        AdminGroup adminGroup = LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
        managesGroups.add(group);
        groupDao.update(adminGroup);

        return group;
    }

    public MemberGroup insertMember(MemberGroup group, final MemberGroup baseGroup) {
        if (baseGroup != null) {
            // Copy settings from base group
            group = (MemberGroup) copyBaseGroupSettings(group, baseGroup);
            customFieldService.clearCache(CustomField.Nature.MEMBER);
        } else {
            group.setBasicSettings(new BasicGroupSettings());
            group.setMemberSettings(new MemberGroupSettings());
        }

        // Validate and save
        validateInsert(group);
        group = (MemberGroup) save(group);

        // Add permission to admin group over the member group
        AdminGroup adminGroup = LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
        managesGroups.add(group);
        groupDao.update(adminGroup);

        // Copy inverse collections from base group
        if (baseGroup != null) {
            copyInverseCollections(group, baseGroup);
        }

        return group;
    }

    public OperatorGroup insertOperator(OperatorGroup group, final OperatorGroup baseGroup) {
        if (baseGroup != null) {
            // Copy settings from base group
            group = (OperatorGroup) copyBaseGroupSettings(group, baseGroup);
            customFieldService.clearCache(CustomField.Nature.OPERATOR);
        } else {
            group.setBasicSettings(new BasicGroupSettings());
        }

        // Validate and save
        validateInsert(group);
        group = (OperatorGroup) save(group);

        // Copy inverse collections from base group
        if (baseGroup != null) {
            copyInverseCollections(group, baseGroup);
        }

        return group;
    }

    public Group load(final Long id, final Relationship... fetch) {
        return groupDao.load(id, fetch);
    }

    public MemberGroupAccountSettings loadAccountSettings(final long groupId, final long accountTypeId, final Relationship... fetch) {
        return memberGroupAccountSettingsDao.load(groupId, accountTypeId, fetch);
    }

    public void removeAccountTypeRelationship(final MemberGroup group, final MemberAccountType type) {
        // Remove the account settings
        memberGroupAccountSettingsDao.delete(group.getId(), type.getId());

        // Deactivate all accounts
        final List<Member> activeMembers = memberService.listByGroup(group);
        for (final Member member : activeMembers) {
            memberAccountHandler.deactivate(member, type, true);
        }
    }

    public void removeAdmin(final Long id) {
        final Group group = load(id, FETCH_TO_REMOVE);
        if (!(group instanceof AdminGroup)) {
            throw new UnexpectedEntityException();
        }
        removeFromInverseCollections(group);
        remove(id);
    }

    public void removeBroker(final Long id) {
        final Group group = load(id, FETCH_TO_REMOVE);
        if (!(group instanceof BrokerGroup)) {
            throw new UnexpectedEntityException();
        }
        removeFromInverseCollections(group);
        remove(id);
    }

    public void removeMember(final Long id) {
        final Group group = load(id, FETCH_TO_REMOVE);
        if (!(group instanceof MemberGroup) || (group instanceof BrokerGroup)) {
            throw new UnexpectedEntityException();
        }
        removeFromInverseCollections(group);
        remove(id);
    }

    public void removeOperator(final Long id) {
        final Group group = load(id);
        if (!(group instanceof OperatorGroup)) {
            throw new UnexpectedEntityException();
        }
        remove(id);
    }

    public List<? extends Group> search(final GroupQuery query) {
        if (!query.isIgnoreManagedBy() && LoggedUser.isValid() && LoggedUser.isAdministrator()) {
            final AdminGroup adminGroup = LoggedUser.group();
            query.setManagedBy(adminGroup);
        }
        return groupDao.search(query);
    }

    public void setAccountFeeDao(final AccountFeeDAO accountFeeDao) {
        this.accountFeeDao = accountFeeDao;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setAccountTypeService(final AccountTypeService accountTypeService) {
        this.accountTypeService = accountTypeService;
    }

    public AdminGroup setAdminPermissions(final AdminGroupPermissionsDTO dto) {
        process(dto);
        return updateCollections(dto);
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public BrokerGroup setBrokerPermissions(final BrokerGroupPermissionsDTO dto) {
        process(dto);

        // Check view documents as broker
        final Collection<Operation> operations = dto.getOperations();
        final Operation viewDocuments = permissionService.loadOperation("brokerDocuments", "view");
        if (dto.getBrokerDocuments().isEmpty()) {
            operations.remove(viewDocuments);
        } else {
            operations.add(viewDocuments);
        }

        // Check make payment as member
        final Operation paymentAsMemberToMember = permissionService.loadOperation("brokerMemberPayments", "paymentAsMemberToMember");
        final Operation paymentAsMemberToSelf = permissionService.loadOperation("brokerMemberPayments", "paymentAsMemberToSelf");
        final Operation paymentAsMemberToSystem = permissionService.loadOperation("brokerMemberPayments", "paymentAsMemberToSystem");

        final Collection<TransferType> asMemberToMemberTTs = dto.getAsMemberToMemberTTs();
        if (asMemberToMemberTTs != null && !asMemberToMemberTTs.isEmpty()) {
            operations.add(paymentAsMemberToMember);
        } else {
            operations.remove(paymentAsMemberToMember);
        }

        final Collection<TransferType> asMemberToSelfTTs = dto.getAsMemberToSelfTTs();
        if (asMemberToSelfTTs != null && !asMemberToSelfTTs.isEmpty()) {
            operations.add(paymentAsMemberToSelf);
        } else {
            operations.remove(paymentAsMemberToSelf);
        }

        final Collection<TransferType> asMemberToSystemTTs = dto.getAsMemberToSystemTTs();
        if (asMemberToSystemTTs != null && !asMemberToSystemTTs.isEmpty()) {
            operations.add(paymentAsMemberToSystem);
        } else {
            operations.remove(paymentAsMemberToSystem);
        }


        final Operation showAccountInformationToBroker = permissionService.loadOperation("brokerReports", "showAccountInformation");
        if (dto.getBrokerCanViewInformationOf().isEmpty()) {
            operations.remove(showAccountInformationToBroker);
        } else {
            operations.add(showAccountInformationToBroker);
        }

        // Check view member records
        final Operation viewMemberRecords = permissionService.loadOperation("brokerMemberRecords", "view");
        if (dto.getBrokerMemberRecordTypes().isEmpty()) {
            operations.remove(viewMemberRecords);
        } else {
            operations.add(viewMemberRecords);
        }

        // Check create member records
        final Operation createMemberRecords = permissionService.loadOperation("brokerMemberRecords", "create");
        if (dto.getBrokerCreateMemberRecordTypes().isEmpty()) {
            operations.remove(createMemberRecords);
        } else {
            operations.add(createMemberRecords);
        }

        // Check modify member records
        final Operation modifyMemberRecords = permissionService.loadOperation("brokerMemberRecords", "modify");
        if (dto.getBrokerModifyMemberRecordTypes().isEmpty()) {
            operations.remove(modifyMemberRecords);
        } else {
            operations.add(modifyMemberRecords);
        }

        // Check delete member records
        final Operation deleteMemberRecords = permissionService.loadOperation("brokerMemberRecords", "delete");
        if (dto.getBrokerDeleteMemberRecordTypes().isEmpty()) {
            operations.remove(deleteMemberRecords);
        } else {
            operations.add(deleteMemberRecords);
        }

        return updateCollections(dto);
    }

    public void setCardTypeService(final CardTypeService cardTypeService) {
        this.cardTypeService = cardTypeService;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
    }

    public void setCustomFieldDao(final CustomFieldDAO customFieldDao) {
        this.customFieldDao = customFieldDao;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setCustomizedFileDao(final CustomizedFileDAO customizedFileDao) {
        this.customizedFileDao = customizedFileDao;
    }

    public void setElementDao(final ElementDAO elementDao) {
        this.elementDao = elementDao;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setGroupDao(final GroupDAO groupDao) {
        this.groupDao = groupDao;
    }

    public void setMemberAccountHandler(final MemberAccountHandler memberAccountHandler) {
        this.memberAccountHandler = memberAccountHandler;
    }

    public void setMemberGroupAccountSettingsDao(final MemberGroupAccountSettingsDAO memberGroupAccountSettingsDao) {
        this.memberGroupAccountSettingsDao = memberGroupAccountSettingsDao;
    }

    public MemberGroup setMemberPermissions(final MemberGroupPermissionsDTO<MemberGroup> dto) {
        process(dto);
        return updateCollections(dto);
    }

    public void setMemberRecordTypeDao(final MemberRecordTypeDAO memberRecordTypeDao) {
        this.memberRecordTypeDao = memberRecordTypeDao;
    }

    public void setMemberService(final MemberService memberService) {
        this.memberService = memberService;
    }

    public OperatorGroup setOperatorPermissions(final OperatorGroupPermissionsDTO dto) {
        process(dto);
        return updateCollections(dto);
    }

    public void setPaymentFilterDao(final PaymentFilterDAO paymentFilterDao) {
        this.paymentFilterDao = paymentFilterDao;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setRegistrationAgreementService(final RegistrationAgreementService registrationAgreementService) {
        this.registrationAgreementService = registrationAgreementService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionFeeDao(final TransactionFeeDAO transactionFeeDao) {
        this.transactionFeeDao = transactionFeeDao;
    }

    public void setTransferTypeService(final TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public MemberGroupAccountSettings updateAccountSettings(final MemberGroupAccountSettings settings, final boolean updateAccountLimits) {

        // Check if the admin group has the permission to manage the member group
        final MemberGroup memberGroup = settings.getGroup();
        AdminGroup adminGroup = LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
        final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
        if (!managesGroups.contains(memberGroup)) {
            throw new PermissionDeniedException();
        }

        validate(settings);

        final MemberAccountType currentDefault = accountTypeService.getDefault(memberGroup);
        if (currentDefault == null || currentDefault.equals(settings.getAccountType())) {
            // When there's no current default, or is this one, set as default
            settings.setDefault(true);
        } else if (settings.isDefault()) {
            // When there was a default already and this one is marked as default, unmark the previous one
            final MemberGroupAccountSettings defaultSettings = memberGroupAccountSettingsDao.load(memberGroup.getId(), currentDefault.getId());
            defaultSettings.setDefault(false);
            memberGroupAccountSettingsDao.update(defaultSettings);
        }

        final MemberGroupAccountSettings saved = memberGroupAccountSettingsDao.update(settings);

        // Check if we must update all account limits
        if (updateAccountLimits) {

            // The limits must be updated
            final CreditLimitDTO dto = new CreditLimitDTO();
            dto.setLimitPerType(Collections.singletonMap(saved.getAccountType(), saved.getDefaultCreditLimit()));
            dto.setUpperLimitPerType(Collections.singletonMap(saved.getAccountType(), saved.getDefaultUpperCreditLimit()));

            // Update each member account limit
            final List<Member> members = memberService.listByGroup(settings.getGroup());
            for (final Member member : members) {
                accountService.setCreditLimit(member, dto);
            }
        }
        return saved;
    }

    public AdminGroup updateAdmin(final AdminGroup group) {
        validateAdmin(group);
        return (AdminGroup) save(group);
    }

    public BrokerGroup updateBroker(final BrokerGroup group) {
        validateBroker(group);
        return (BrokerGroup) save(group);
    }

    public MemberGroup updateMember(MemberGroup group, final boolean forceMembersToAcceptAgreement) {
        validateMember(group);
        final MemberGroup current = (MemberGroup) load(group.getId());
        final RegistrationAgreement oldAgreement = fetchService.fetch(current.getRegistrationAgreement());
        boolean wasTPUsed;
        try {
            wasTPUsed = current.getBasicSettings().getTransactionPassword().isUsed();
        } catch (final Exception e) {
            wasTPUsed = false;
        }
        boolean isTPUsed;
        try {
            isTPUsed = group.getBasicSettings().getTransactionPassword().isUsed();
        } catch (final Exception e) {
            isTPUsed = false;
        }
        final boolean wasActive = current.isActive();
        group = (MemberGroup) save(group);

        // When the transaction password was not used and now is, or was used and now is not, update the accounts
        if ((wasTPUsed && !isTPUsed) || (!wasTPUsed && isTPUsed)) {
            group = fetchService.reload(group, MemberGroup.Relationships.ACCOUNT_SETTINGS);
            for (final MemberGroupAccountSettings mgas : group.getAccountSettings()) {
                mgas.setTransactionPasswordRequired(isTPUsed);
                memberGroupAccountSettingsDao.update(mgas);
            }
        }

        // When the group is becoming active, activate all members on it
        if (!wasActive && group.isActive()) {
            elementDao.activateMembersOfGroup(group);
        }

        // Check if the registration agreement has changed
        final RegistrationAgreement registrationAgreement = fetchService.fetch(group.getRegistrationAgreement());
        if (registrationAgreement != null && !registrationAgreement.equals(oldAgreement)) {
            // It did change. When not forcing members to accept it again, we should create all accepting logs
            if (!forceMembersToAcceptAgreement) {
                elementService.createAgreementForAllMembers(registrationAgreement, group);
            }
        }

        return group;
    }

    public OperatorGroup updateOperator(final OperatorGroup group) {
        group.setMember((Member) LoggedUser.element());
        validateOperator(group);
        return (OperatorGroup) save(group);
    }

    public boolean usesPin(MemberGroup group) {
        group = fetchService.fetch(group, MemberGroup.Relationships.CHANNELS);
        final Collection<Channel> channels = group.getChannels();
        for (final Channel channel : channels) {
            if (channel.getCredentials() == Credentials.PIN) {
                return true;
            }
        }
        return false;
    }

    public void validate(final MemberGroupAccountSettings settings) {
        getAccountSettingsValidator().validate(settings);
    }

    public void validateAdmin(final AdminGroup group) throws ValidationException {
        if (Group.Status.REMOVED.equals(group.getStatus())) {
            getRemovedValidator().validate(group);
        } else {
            getAdminValidator().validate(group);
        }
    }

    public void validateBroker(final BrokerGroup group) throws ValidationException {
        if (Group.Status.REMOVED.equals(group.getStatus())) {
            getRemovedValidator().validate(group);
        } else {
            getBrokerValidator().validate(group);
        }
    }

    public void validateInsert(final Group group) throws ValidationException {
        getInsertValidator().validate(group);
    }

    public void validateMember(final MemberGroup group) throws ValidationException {
        if (Group.Status.REMOVED.equals(group.getStatus())) {
            getRemovedValidator().validate(group);
        } else {
            getMemberValidator().validate(group);
        }
    }

    public void validateOperator(final OperatorGroup group) throws ValidationException {
        if (Group.Status.REMOVED.equals(group.getStatus())) {
            getRemovedValidator().validate(group);
        } else {
            getOperatorValidator().validate(group);
        }
    }

    private Group copyBaseGroupSettings(final Group group, Group baseGroup) {
        baseGroup = fetchService.fetch(baseGroup, Group.Relationships.PAYMENT_FILTERS, Group.Relationships.PERMISSIONS, Group.Relationships.TRANSFER_TYPES, Group.Relationships.DOCUMENTS, Group.Relationships.CUSTOMIZED_FILES, Group.Relationships.MESSAGE_CATEGORIES, AdminGroup.Relationships.TRANSFER_TYPES_AS_MEMBER, AdminGroup.Relationships.MANAGES_GROUPS, AdminGroup.Relationships.VIEW_INFORMATION_OF, AdminGroup.Relationships.VIEW_CONNECTED_ADMINS_OF, AdminGroup.Relationships.CONNECTED_ADMINS_VIEWED_BY, AdminGroup.Relationships.ADMIN_CUSTOM_FIELDS, MemberGroup.Relationships.ACCOUNT_SETTINGS, MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS, MemberGroup.Relationships.CAN_VIEW_ADS_OF_GROUPS, MemberGroup.Relationships.CAN_VIEW_INFORMATION_OF, MemberGroup.Relationships.ACCOUNT_FEES, MemberGroup.Relationships.MANAGED_BY_GROUPS, MemberGroup.Relationships.CUSTOM_FIELDS, MemberGroup.Relationships.FROM_TRANSACTION_FEES, MemberGroup.Relationships.TO_TRANSACTION_FEES, BrokerGroup.Relationships.TRANSFER_TYPES_AS_MEMBER, BrokerGroup.Relationships.BROKER_DOCUMENTS, BrokerGroup.Relationships.BROKER_CAN_VIEW_INFORMATION_OF, OperatorGroup.Relationships.MEMBER, OperatorGroup.Relationships.MAX_AMOUNT_PER_DAY_BY_TRANSFER_TYPE, OperatorGroup.Relationships.CAN_VIEW_INFORMATION_OF, Group.Relationships.GUARANTEE_TYPES, MemberGroup.Relationships.CAN_ISSUE_CERTIFICATION_TO_GROUPS, MemberGroup.Relationships.CAN_BUY_WITH_PAYMENT_OBLIGATIONS_FROM_GROUPS, MemberGroup.Relationships.CARD_TYPE);

        // Copy permissions
        final List<Operation> permissions = new ArrayList<Operation>(baseGroup.getPermissions());
        group.setPermissions(permissions);

        // Status
        group.setStatus(baseGroup.getStatus());

        // Transfer types
        final List<TransferType> transferTypes = new ArrayList<TransferType>(baseGroup.getTransferTypes());
        group.setTransferTypes(transferTypes);

        // Conversion Simulation TTs
        final List<TransferType> conversionSimulationTTs = new ArrayList<TransferType>(baseGroup.getConversionSimulationTTs());
        group.setConversionSimulationTTs(conversionSimulationTTs);

        // Documents
        final List<Document> documents = new ArrayList<Document>(baseGroup.getDocuments());
        group.setDocuments(documents);

        // Message categories
        final List<MessageCategory> messageCategories = new ArrayList<MessageCategory>(baseGroup.getMessageCategories());
        group.setMessageCategories(messageCategories);

        // Basic settings
        if (!(group instanceof OperatorGroup)) {
            group.setBasicSettings((BasicGroupSettings) baseGroup.getBasicSettings().clone());
        }

        // Guarantee types
        final List<GuaranteeType> guaranteeTypes = new ArrayList<GuaranteeType>(baseGroup.getGuaranteeTypes());
        group.setGuaranteeTypes(guaranteeTypes);

        // Chargeback transfer types
        final List<TransferType> chargebackTransferTypes = new ArrayList<TransferType>(baseGroup.getChargebackTransferTypes());
        group.setChargebackTransferTypes(chargebackTransferTypes);

        if (group instanceof AdminGroup) {
            final AdminGroup adminGroup = (AdminGroup) group;
            final AdminGroup baseAdminGroup = (AdminGroup) baseGroup;

            // Transfer types as member
            final List<TransferType> transferTypesAsMember = new ArrayList<TransferType>(baseAdminGroup.getTransferTypesAsMember());
            adminGroup.setTransferTypesAsMember(transferTypesAsMember);

            // Manages groups
            final List<MemberGroup> managesGroups = new ArrayList<MemberGroup>(baseAdminGroup.getManagesGroups());
            adminGroup.setManagesGroups(managesGroups);

            // View information of
            final List<SystemAccountType> viewInformationOf = new ArrayList<SystemAccountType>(baseAdminGroup.getViewInformationOf());
            adminGroup.setViewInformationOf(viewInformationOf);

            // View connected admins of
            final List<AdminGroup> viewConnectedAdminsOf = new ArrayList<AdminGroup>(baseAdminGroup.getViewConnectedAdminsOf());
            adminGroup.setViewConnectedAdminsOf(viewConnectedAdminsOf);

            // View member record types
            final List<MemberRecordType> viewMemberRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getViewMemberRecordTypes());
            adminGroup.setViewMemberRecordTypes(viewMemberRecordTypes);

            // Create member record types
            final List<MemberRecordType> createMemberRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getCreateMemberRecordTypes());
            adminGroup.setCreateMemberRecordTypes(createMemberRecordTypes);

            // Modify member record types
            final List<MemberRecordType> modifyMemberRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getModifyMemberRecordTypes());
            adminGroup.setModifyMemberRecordTypes(modifyMemberRecordTypes);

            // Delete member record types
            final List<MemberRecordType> deleteMemberRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getDeleteMemberRecordTypes());
            adminGroup.setDeleteMemberRecordTypes(deleteMemberRecordTypes);

            // View admin record types
            final List<MemberRecordType> viewAdminRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getViewAdminRecordTypes());
            adminGroup.setViewAdminRecordTypes(viewAdminRecordTypes);

            // Create admin record types
            final List<MemberRecordType> createAdminRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getCreateAdminRecordTypes());
            adminGroup.setCreateAdminRecordTypes(createAdminRecordTypes);

            // Modify admin record types
            final List<MemberRecordType> modifyAdminRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getModifyAdminRecordTypes());
            adminGroup.setModifyAdminRecordTypes(modifyAdminRecordTypes);

            // Delete admin record types
            final List<MemberRecordType> deleteAdminRecordTypes = new ArrayList<MemberRecordType>(baseAdminGroup.getDeleteAdminRecordTypes());
            adminGroup.setDeleteAdminRecordTypes(deleteAdminRecordTypes);
        }

        if (group instanceof MemberGroup) {
            final MemberGroup memberGroup = (MemberGroup) group;
            final MemberGroup baseMemberGroup = (MemberGroup) baseGroup;

            // Member group settings
            memberGroup.setMemberSettings((MemberGroupSettings) baseMemberGroup.getMemberSettings().clone());

            // Initial group & agreement
            memberGroup.setInitialGroup(baseMemberGroup.isInitialGroup());
            memberGroup.setRegistrationAgreement(baseMemberGroup.getRegistrationAgreement());

            // Active
            memberGroup.setActive(baseMemberGroup.isActive());

            // Can view profile of groups
            final List<MemberGroup> canViewProfileOfGroups = new ArrayList<MemberGroup>(baseMemberGroup.getCanViewProfileOfGroups());
            canViewProfileOfGroups.add(baseMemberGroup);
            memberGroup.setCanViewProfileOfGroups(canViewProfileOfGroups);

            // Can view ads of groups
            final List<MemberGroup> canViewAdsOfGroups = new ArrayList<MemberGroup>(baseMemberGroup.getCanViewAdsOfGroups());
            canViewAdsOfGroups.add(baseMemberGroup);
            memberGroup.setCanViewAdsOfGroups(canViewAdsOfGroups);

            // Can view information of
            final List<AccountType> canViewInformationOf = new ArrayList<AccountType>(baseMemberGroup.getCanViewInformationOf());
            memberGroup.setCanViewInformationOf(canViewInformationOf);

            // Default mail messages
            final List<Message.Type> defaultMailMessages = new ArrayList<Message.Type>(baseMemberGroup.getDefaultMailMessages());
            memberGroup.setDefaultMailMessages(defaultMailMessages);

            // SMS messages
            final List<Message.Type> smsMessages = new ArrayList<Message.Type>(baseMemberGroup.getSmsMessages());
            memberGroup.setSmsMessages(smsMessages);

            // Default SMS messages
            final List<Message.Type> defaultSmsMessages = new ArrayList<Message.Type>(baseMemberGroup.getDefaultSmsMessages());
            memberGroup.setDefaultSmsMessages(defaultSmsMessages);

            // Channels
            final List<Channel> channels = new ArrayList<Channel>(baseMemberGroup.getChannels());
            memberGroup.setChannels(channels);

            // Default channels
            final List<Channel> defaultChannels = new ArrayList<Channel>(baseMemberGroup.getDefaultChannels());
            memberGroup.setDefaultChannels(defaultChannels);

            // Request payment by channels
            final List<Channel> requestPaymentByChannels = new ArrayList<Channel>(baseMemberGroup.getRequestPaymentByChannels());
            memberGroup.setRequestPaymentByChannels(requestPaymentByChannels);

            // Can issue certification to groups
            final List<MemberGroup> canIssueCertificationToGroups = new ArrayList<MemberGroup>(baseMemberGroup.getCanIssueCertificationToGroups());
            memberGroup.setCanIssueCertificationToGroups(canIssueCertificationToGroups);

            // Can buy with payment obligations from groups
            final List<MemberGroup> canBuyWithPaymentObligationsFromGroups = new ArrayList<MemberGroup>(baseMemberGroup.getCanBuyWithPaymentObligationsFromGroups());
            memberGroup.setCanBuyWithPaymentObligationsFromGroups(canBuyWithPaymentObligationsFromGroups);

            // Card type
            final CardType cardType = baseMemberGroup.getCardType();
            memberGroup.setCardType(cardType);
        }

        if (group instanceof BrokerGroup) {
            final BrokerGroup brokerGroup = (BrokerGroup) group;
            final BrokerGroup baseBrokerGroup = (BrokerGroup) baseGroup;

            // Transfer types as member
            final List<TransferType> transferTypesAsMember = new ArrayList<TransferType>(baseBrokerGroup.getTransferTypesAsMember());
            brokerGroup.setTransferTypesAsMember(transferTypesAsMember);

            final List<TransferType> brokerConversionSimulationTTs = new ArrayList<TransferType>(baseBrokerGroup.getBrokerConversionSimulationTTs());
            brokerGroup.setBrokerConversionSimulationTTs(brokerConversionSimulationTTs);

            // Broker documents
            final List<Document> brokerDocuments = new ArrayList<Document>(baseBrokerGroup.getBrokerDocuments());
            brokerGroup.setBrokerDocuments(brokerDocuments);

            // Broker can view information of
            final List<AccountType> brokerCanViewInformationOf = new ArrayList<AccountType>(baseBrokerGroup.getBrokerCanViewInformationOf());
            brokerGroup.setBrokerCanViewInformationOf(brokerCanViewInformationOf);

            // Broker view member record types
            final List<MemberRecordType> brokerMemberRecordTypes = new ArrayList<MemberRecordType>(baseBrokerGroup.getBrokerMemberRecordTypes());
            brokerGroup.setBrokerMemberRecordTypes(brokerMemberRecordTypes);

            // Broker create member record types
            final List<MemberRecordType> brokerCreateMemberRecordTypes = new ArrayList<MemberRecordType>(baseBrokerGroup.getBrokerCreateMemberRecordTypes());
            brokerGroup.setBrokerCreateMemberRecordTypes(brokerCreateMemberRecordTypes);

            // Broker modify member record types
            final List<MemberRecordType> brokerModifyMemberRecordTypes = new ArrayList<MemberRecordType>(baseBrokerGroup.getBrokerModifyMemberRecordTypes());
            brokerGroup.setBrokerModifyMemberRecordTypes(brokerModifyMemberRecordTypes);

            // Broker delete member record types
            final List<MemberRecordType> brokerDeleteMemberRecordTypes = new ArrayList<MemberRecordType>(baseBrokerGroup.getBrokerDeleteMemberRecordTypes());
            brokerGroup.setBrokerDeleteMemberRecordTypes(brokerDeleteMemberRecordTypes);

            // Possible initial groups
            final List<MemberGroup> possibleInitialGroups = new ArrayList<MemberGroup>(baseBrokerGroup.getPossibleInitialGroups());
            brokerGroup.setPossibleInitialGroups(possibleInitialGroups);
        }

        if (group instanceof OperatorGroup) {
            final OperatorGroup operatorGroup = (OperatorGroup) group;
            final OperatorGroup baseOperatorGroup = (OperatorGroup) baseGroup;

            // Member
            operatorGroup.setMember(baseOperatorGroup.getMember());

            // Max amount per day by transfer type
            final Map<TransferType, BigDecimal> maxAmountPerDayByTransferType = new HashMap<TransferType, BigDecimal>(baseOperatorGroup.getMaxAmountPerDayByTransferType());
            operatorGroup.setMaxAmountPerDayByTransferType(maxAmountPerDayByTransferType);

            // Can view information of
            final List<AccountType> canViewInformationOf = new ArrayList<AccountType>(baseOperatorGroup.getCanViewInformationOf());
            operatorGroup.setCanViewInformationOf(canViewInformationOf);
        }

        return group;
    }

    @SuppressWarnings("unchecked")
    private Group copyInverseCollections(final Group group, final Group baseGroup) {

        // Payment filters
        final Collection<PaymentFilter> paymentFilters = baseGroup.getPaymentFilters();
        for (final PaymentFilter paymentFilter : paymentFilters) {
            final Collection<Group> groups = (Collection<Group>) paymentFilter.getGroups();
            groups.add(group);
            paymentFilterDao.update(paymentFilter);
        }

        // Customized files
        final Collection<CustomizedFile> customizedFiles = baseGroup.getCustomizedFiles();
        for (final CustomizedFile baseGroupCustomizedFile : customizedFiles) {
            final CustomizedFile customizedFile = (CustomizedFile) baseGroupCustomizedFile.clone();
            customizedFile.setId(null);
            customizedFile.setGroup(group);
            customizedFileDao.insert(customizedFile);
        }

        // Record types
        final Collection<MemberRecordType> recordTypes = baseGroup.getMemberRecordTypes();
        for (final MemberRecordType recordType : recordTypes) {
            recordType.getGroups().add(group);
            memberRecordTypeDao.update(recordType);
        }

        if (group instanceof AdminGroup) {
            final AdminGroup adminGroup = (AdminGroup) group;
            final AdminGroup baseAdminGroup = (AdminGroup) baseGroup;

            // Connected admins viewed by
            final Collection<AdminGroup> connectedAdminsViewedBy = baseAdminGroup.getConnectedAdminsViewedBy();
            for (final AdminGroup viewerAdminGroup : connectedAdminsViewedBy) {
                viewerAdminGroup.getViewConnectedAdminsOf().add(adminGroup);
                groupDao.update(viewerAdminGroup);
            }

            // Admin custom fields
            final Collection<AdminCustomField> adminCustomFields = baseAdminGroup.getAdminCustomFields();
            for (final AdminCustomField adminCustomField : adminCustomFields) {
                adminCustomField.getGroups().add(adminGroup);
                customFieldDao.update(adminCustomField);
            }
        } else if (group instanceof MemberGroup) {
            final MemberGroup memberGroup = (MemberGroup) group;
            final MemberGroup baseMemberGroup = (MemberGroup) baseGroup;

            // Managed by groups
            final Collection<AdminGroup> managedByGroups = baseMemberGroup.getManagedByGroups();
            for (final AdminGroup adminGroup : managedByGroups) {
                adminGroup.getManagesGroups().add(memberGroup);
                groupDao.update(adminGroup);
            }

            // Account settings
            final Collection<MemberGroupAccountSettings> baseMemberGroupAccountSettings = baseMemberGroup.getAccountSettings();
            for (final MemberGroupAccountSettings baseAccountSettings : baseMemberGroupAccountSettings) {
                final MemberGroupAccountSettings accountSettings = (MemberGroupAccountSettings) baseAccountSettings.clone();
                accountSettings.setId(null);
                accountSettings.setGroup(memberGroup);
                insertAccountSettings(accountSettings);
            }

            // Account fees
            final Collection<AccountFee> accountFees = baseMemberGroup.getAccountFees();
            for (final AccountFee accountFee : accountFees) {
                final Collection<MemberGroup> groups = accountFee.getGroups();
                groups.add(memberGroup);
                accountFeeDao.update(accountFee);
            }

            // Member custom fields and general remark custom fields)
            final Collection<CustomField> customFields = baseMemberGroup.getCustomFields();
            for (final CustomField customField : customFields) {
                // Get the groups using reflection
                final Collection<MemberGroup> groups = PropertyHelper.get(customField, "groups");
                groups.add(memberGroup);
                customFieldDao.update(customField);
            }

            // From transaction fees
            final Collection<TransactionFee> fromTransactionFees = baseMemberGroup.getFromTransactionFees();
            for (final TransactionFee transactionFee : fromTransactionFees) {
                transactionFee.getFromGroups().add(memberGroup);
                transactionFeeDao.update(transactionFee);
            }

            // To transaction fees
            final Collection<TransactionFee> toTransactionFees = baseMemberGroup.getToTransactionFees();
            for (final TransactionFee transactionFee : toTransactionFees) {
                transactionFee.getToGroups().add(memberGroup);
                transactionFeeDao.update(transactionFee);
            }

            // View profile of
            for (final MemberGroup other : memberGroup.getCanViewProfileOfGroups()) {
                other.getCanViewProfileOfGroups().add(memberGroup);
            }
            memberGroup.getCanViewProfileOfGroups().add(memberGroup);

            // View ads of
            for (final MemberGroup other : memberGroup.getCanViewAdsOfGroups()) {
                other.getCanViewAdsOfGroups().add(memberGroup);
            }
            memberGroup.getCanViewAdsOfGroups().add(memberGroup);

            // Group filters
            for (final GroupFilter groupFilter : baseMemberGroup.getGroupFilters()) {
                groupFilter.getGroups().add(memberGroup);
            }
            for (final GroupFilter groupFilter : baseMemberGroup.getCanViewGroupFilters()) {
                groupFilter.getViewableBy().add(memberGroup);
            }
        }

        return group;
    }

    // Note: if we put the setAccessService, Spring will fail to resolve dependencies, as cyclic dependencies would be needed.
    // So, the workaround was to get the bean the first time it is needed
    private AccessService getAccessService() {
        if (accessService == null) {
            accessService = applicationContext.getBean("accessService", AccessService.class);
        }
        return accessService;
    }

    private Validator getAccountSettingsValidator() {
        final Validator accountSettingsValidator = new Validator("account");
        accountSettingsValidator.property("group").displayName("group").required();
        accountSettingsValidator.property("accountType").displayName("account type").required();
        accountSettingsValidator.property("initialCredit").positive();
        accountSettingsValidator.property("initialCreditTransferType").add(new PropertyValidation() {
            private static final long serialVersionUID = 8284432136349418154L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                final MemberGroupAccountSettings mgas = (MemberGroupAccountSettings) object;
                final TransferType tt = fetchService.fetch((TransferType) value, TransferType.Relationships.FROM, TransferType.Relationships.TO);
                final BigDecimal initialCredit = mgas.getInitialCredit();
                // When there is initial credit, there must be a transfer type
                if (initialCredit != null && (initialCredit.compareTo(BigDecimal.ZERO) == 1) && tt == null) {
                    return new RequiredError();
                }
                // Must be from system to member
                if (tt != null && !(tt.isFromSystem() && !tt.isToSystem())) {
                    return new InvalidError();
                }
                return null;
            }
        });
        accountSettingsValidator.property("defaultCreditLimit").required().positive();
        accountSettingsValidator.property("defaultUpperCreditLimit").positive();
        accountSettingsValidator.property("lowUnits").positive();
        accountSettingsValidator.property("lowUnitsMessage").add(new PropertyValidation() {
            private static final long serialVersionUID = -6086632981851357180L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                final MemberGroupAccountSettings mgas = (MemberGroupAccountSettings) object;
                final BigDecimal lowUnits = mgas.getLowUnits();
                // When there are low units, the message is required
                if (lowUnits != null && (lowUnits.compareTo(BigDecimal.ZERO) == 1) && StringUtils.isEmpty(mgas.getLowUnitsMessage())) {
                    return new RequiredError();
                }
                return null;
            }
        });
        return accountSettingsValidator;
    }

    private Validator getAdminValidator() {
        final Validator adminValidator = new Validator("group");
        initBasic(adminValidator, true);
        return adminValidator;
    }

    private Validator getBrokerValidator() {
        final Validator brokerValidator = new Validator("group");
        initBasic(brokerValidator, true);
        initMember(brokerValidator);
        return brokerValidator;
    }

    private Validator getInsertValidator() {
        final Validator insertValidator = new Validator("group");
        insertValidator.property("name").required().maxLength(100);
        insertValidator.property("description").maxLength(2000);
        insertValidator.property("nature").required();
        insertValidator.property("status").required();
        return insertValidator;
    }

    private Validator getMemberValidator() {
        final Validator memberValidator = new Validator("group");
        initBasic(memberValidator, true);
        initMember(memberValidator);
        return memberValidator;
    }

    private Validator getOperatorValidator() {
        final Validator operatorValidator = new Validator("group");
        initBasic(operatorValidator, false);
        return operatorValidator;
    }

    private Validator getRemovedValidator() {
        final Validator removedValidator = new Validator("group");
        removedValidator.property("name").required().maxLength(100);
        removedValidator.property("description").maxLength(2000);
        return removedValidator;
    }

    private void initBasic(final Validator validator, final boolean addSettings) {
        validator.property("name").required().maxLength(100);
        validator.property("description").maxLength(2000);
        validator.property("loginPageName").maxLength(20);
        validator.property("containerUrl").maxLength(100).url();
        if (addSettings) {
            validator.property("basicSettings.passwordLength.min").key("group.settings.passwordLength.min").between(1, 32);
            validator.property("basicSettings.passwordLength.max").key("group.settings.passwordLength.max").between(1, 32);
            validator.property("basicSettings.maxPasswordWrongTries").key("group.settings.passwordTries.maximum").between(0, 99);
            validator.property("basicSettings.deactivationAfterMaxPasswordTries.number").key("group.settings.passwordTries.deactivationTime.number").between(0, 999);
            validator.property("basicSettings.deactivationAfterMaxPasswordTries.field").key("group.settings.passwordTries.deactivationTime.field").required();
            validator.property("basicSettings.passwordExpiresAfter.number").key("group.settings.passwordExpiresAfter.number").between(0, 999);
            validator.property("basicSettings.passwordExpiresAfter.field").key("group.settings.passwordExpiresAfter.field").required();
            validator.property("basicSettings.transactionPassword").key("group.settings.transactionPassword").required();
            validator.property("basicSettings.transactionPasswordLength").key("group.settings.transactionPassword.length").between(1, 32);
            validator.property("basicSettings.maxTransactionPasswordWrongTries").key("group.settings.maxTransactionPasswordWrongTries").between(0, 99);
            validator.property("basicSettings.deactivationAfterMaxPasswordTries.number").key("group.settings.passwordTries.deactivationTime.number").add(new PasswordTrialsValidation());
            validator.property("basicSettings.passwordPolicy").key("group.settings.passwordPolicy").add(new PasswordPolicyValidation());
        }
    }

    private void initMember(final Validator validator) {
        validator.property("memberSettings.defaultAdPublicationTime.number").key("group.settings.defaultAdPublicationTime.number").between(1, 999);
        validator.property("memberSettings.defaultAdPublicationTime.field").key("group.settings.defaultAdPublicationTime.field").required();
        validator.property("memberSettings.maxAdPublicationTime.number").key("group.settings.maxAdPublicationTime.number").between(1, 999);
        validator.property("memberSettings.maxAdPublicationTime.field").key("group.settings.maxAdPublicationTime.field").required();
        validator.property("memberSettings.maxAdDescriptionSize").key("group.settings.maxAdDescriptionSize").required().between(16, 16000);
        validator.property("memberSettings.maxAdsPerMember").key("group.settings.maxAdsPerMember").between(0, 999);
        validator.property("memberSettings.maxAdImagesPerMember").key("group.settings.maxAdImagesPerMember").between(0, 999);
        validator.property("memberSettings.maxImagesPerMember").key("group.settings.maxImagesPerMember").between(0, 999);
        validator.property("memberSettings.expireMembersAfter.number").key("group.settings.expireMembersAfter").between(0, 999);
        validator.property("memberSettings.expireMembersAfter.field").key("group.settings.expireMembersAfter").add(new ExpirationValidation(RequiredValidation.instance()));
        validator.property("memberSettings.groupAfterExpiration").key("group.settings.groupAfterExpiration").add(new ExpirationValidation(RequiredValidation.instance()));
        validator.property("memberSettings.pinBlockTimeAfterMaxTries.number").key("group.settings.pinBlockTimeAfterMaxTries.number").between(0, 999);
        validator.property("memberSettings.pinBlockTimeAfterMaxTries.field").key("group.settings.pinBlockTimeAfterMaxTries.field").add(new PINBlockTimeValidation(RequiredValidation.instance()));
        validator.property("memberSettings.smsContextClassName").key("group.settings.smsContextClassName").add(new CustomSMSContextValidation());
        validator.property("memberSettings.pinBlockTimeAfterMaxTries.number").key("group.settings.pinBlockTimeAfterMaxTries.number").add(new PinTrialsValidation());
    }

    private ISmsContext instantiateCustomSmsContext(final MemberGroup group) {
        final MemberGroupSettings memberSettings = group.getMemberSettings();
        final String className = memberSettings.getSmsContextClassName();
        try {
            final Class<?> ctxClass = Class.forName(className);
            if (!ISmsContext.class.isAssignableFrom(ctxClass)) {
                throw new SmsContextInitializationException(group, className, "The specified sms context class is not an implementation of: " + ISmsContext.class.getName());
            } else {
                final AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
                final ISmsContext context = (ISmsContext) factory.createBean(ctxClass, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
                factory.initializeBean(context, "SmsContext_" + group.getId());
                return context;
            }
        } catch (final ClassNotFoundException e) {
            throw new SmsContextInitializationException(group, className, "The specified sms context class was not found", e);
        } catch (final BeansException e) {
            throw new SmsContextInitializationException(group, className, "The specified sms context class couldn't be initialized (" + e.getMessage() + ")", e);
        }
    }

    /**
     * Process the dto before saving permissions. Basically, check each collection to set the specific permission.
     */
    private void process(final AdminGroupPermissionsDTO dto) {
        final Collection<Operation> operations = dto.getOperations();

        // Check view account information
        final Operation systemAccountInformation = permissionService.loadOperation("systemAccounts", "information");
        if (dto.getViewInformationOf() == null || dto.getViewInformationOf().isEmpty()) {
            operations.remove(systemAccountInformation);
        } else {
            operations.add(systemAccountInformation);
        }

        // Check view connected admins of
        final Operation viewConnectedAdminsOf = permissionService.loadOperation("systemStatus", "viewConnectedAdmins");
        if (dto.getViewConnectedAdminsOf() == null || dto.getViewConnectedAdminsOf().isEmpty()) {
            operations.remove(viewConnectedAdminsOf);
        } else {
            operations.add(viewConnectedAdminsOf);
        }

        // Check view documents
        final Operation documentDetails = permissionService.loadOperation("adminMemberDocuments", "details");
        if (CollectionUtils.isEmpty(dto.getDocuments())) {
            operations.remove(documentDetails);
        } else {
            operations.add(documentDetails);
        }

        // Check view messages
        final Operation viewMessages = permissionService.loadOperation("adminMemberMessages", "view");
        if (CollectionUtils.isEmpty(dto.getMessageCategories())) {
            operations.remove(viewMessages);
        } else {
            operations.add(viewMessages);
        }

        // Check guarantees types
        final Operation guaranteeTypes = permissionService.loadOperation("adminMemberGuarantees", "registerGuarantees");
        if (CollectionUtils.isEmpty(dto.getGuaranteeTypes())) {
            operations.remove(guaranteeTypes);
        } else {
            operations.add(guaranteeTypes);
        }

        // Check make payments
        final Operation systemPayments = permissionService.loadOperation("systemPayments", "payment");
        final Operation memberPayments = permissionService.loadOperation("adminMemberPayments", "payment");
        final Operation grantLoan = permissionService.loadOperation("adminMemberLoans", "grant");

        final Collection<TransferType> systemToSystemTTs = dto.getSystemToSystemTTs();
        if (systemToSystemTTs != null && !systemToSystemTTs.isEmpty()) {
            operations.add(systemPayments);
        } else {
            operations.remove(systemPayments);
        }

        final Collection<TransferType> systemToMemberTTs = dto.getSystemToMemberTTs();
        if (systemToMemberTTs != null && !systemToMemberTTs.isEmpty()) {
            operations.add(memberPayments);
        } else {
            operations.remove(memberPayments);
        }

        final Collection<TransferType> grantLoanTTs = dto.getGrantLoanTTs();
        if (grantLoanTTs != null && !grantLoanTTs.isEmpty()) {
            operations.add(grantLoan);
        } else {
            operations.remove(grantLoan);
        }

        // Check make payments as member
        final Operation paymentAsMemberToMember = permissionService.loadOperation("adminMemberPayments", "paymentAsMemberToMember");
        final Operation paymentAsMemberToSelf = permissionService.loadOperation("adminMemberPayments", "paymentAsMemberToSelf");
        final Operation paymentAsMemberToSystem = permissionService.loadOperation("adminMemberPayments", "paymentAsMemberToSystem");

        final Collection<TransferType> asMemberToMemberTTs = dto.getAsMemberToMemberTTs();
        if (asMemberToMemberTTs != null && !asMemberToMemberTTs.isEmpty()) {
            operations.add(paymentAsMemberToMember);
        } else {
            operations.remove(paymentAsMemberToMember);
        }

        final Collection<TransferType> asMemberToSelfTTs = dto.getAsMemberToSelfTTs();
        if (asMemberToSelfTTs != null && !asMemberToSelfTTs.isEmpty()) {
            operations.add(paymentAsMemberToSelf);
        } else {
            operations.remove(paymentAsMemberToSelf);
        }

        final Collection<TransferType> asMemberToSystemTTs = dto.getAsMemberToSystemTTs();
        if (asMemberToSystemTTs != null && !asMemberToSystemTTs.isEmpty()) {
            operations.add(paymentAsMemberToSystem);
        } else {
            operations.remove(paymentAsMemberToSystem);
        }

        // Check chargebacks
        final Operation chargebackSystem = permissionService.loadOperation("systemPayments", "chargeback");
        final Operation chargebackMember = permissionService.loadOperation("adminMemberPayments", "chargeback");

        final Collection<TransferType> systemChargebackTTs = dto.getSystemChargebackTTs();
        if (CollectionUtils.isNotEmpty(systemChargebackTTs)) {
            operations.add(chargebackSystem);
        } else {
            operations.remove(chargebackSystem);
        }

        final Collection<TransferType> memberChargebackTTs = dto.getMemberChargebackTTs();
        if (CollectionUtils.isNotEmpty(memberChargebackTTs)) {
            operations.add(chargebackMember);
        } else {
            operations.remove(chargebackMember);
        }


        // Check view member records
        final Operation viewMemberRecords = permissionService.loadOperation("adminMemberRecords", "view");
        if (CollectionUtils.isEmpty(dto.getViewMemberRecordTypes())) {
            operations.remove(viewMemberRecords);
        } else {
            operations.add(viewMemberRecords);
        }

        // Check create member records
        final Operation createMemberRecords = permissionService.loadOperation("adminMemberRecords", "create");
        if (CollectionUtils.isEmpty(dto.getCreateMemberRecordTypes())) {
            operations.remove(createMemberRecords);
        } else {
            operations.add(createMemberRecords);
        }

        // Check modify member records
        final Operation modifyMemberRecords = permissionService.loadOperation("adminMemberRecords", "modify");
        if (CollectionUtils.isEmpty(dto.getModifyMemberRecordTypes())) {
            operations.remove(modifyMemberRecords);
        } else {
            operations.add(modifyMemberRecords);
        }

        // Check delete member records
        final Operation deleteMemberRecords = permissionService.loadOperation("adminMemberRecords", "delete");
        if (CollectionUtils.isEmpty(dto.getDeleteMemberRecordTypes())) {
            operations.remove(deleteMemberRecords);
        } else {
            operations.add(deleteMemberRecords);
        }

        // Check view admin records
        final Operation viewAdminRecords = permissionService.loadOperation("adminAdminRecords", "view");
        if (CollectionUtils.isEmpty(dto.getViewAdminRecordTypes())) {
            operations.remove(viewAdminRecords);
        } else {
            operations.add(viewAdminRecords);
        }

        // Check create admin records
        final Operation createAdminRecords = permissionService.loadOperation("adminAdminRecords", "create");
        if (CollectionUtils.isEmpty(dto.getCreateAdminRecordTypes())) {
            operations.remove(createAdminRecords);
        } else {
            operations.add(createAdminRecords);
        }

        // Check modify admin records
        final Operation modifyAdminRecords = permissionService.loadOperation("adminAdminRecords", "modify");
        if (CollectionUtils.isEmpty(dto.getModifyAdminRecordTypes())) {
            operations.remove(modifyAdminRecords);
        } else {
            operations.add(modifyAdminRecords);
        }

        // Check delete admin records
        final Operation deleteAdminRecords = permissionService.loadOperation("adminAdminRecords", "delete");
        if (CollectionUtils.isEmpty(dto.getDeleteAdminRecordTypes())) {
            operations.remove(deleteAdminRecords);
        } else {
            operations.add(deleteAdminRecords);
        }
    }

    /**
     * Process the dto before saving permissions. Basically, check each collection to set the specific permission.
     */
    private void process(final MemberGroupPermissionsDTO<? extends MemberGroup> dto) {
        final Collection<Operation> operations = dto.getOperations();

        // Check view profile
        final Operation viewProfile = permissionService.loadOperation("memberProfile", "view");
        if (dto.getCanViewProfileOfGroups().isEmpty()) {
            operations.remove(viewProfile);
        } else {
            operations.add(viewProfile);
        }

        // Check account information
        final Operation showAccountInformation = permissionService.loadOperation("memberReports", "showAccountInformation");
        if (dto.getCanViewInformationOf().isEmpty()) {
            operations.remove(showAccountInformation);
        } else {
            operations.add(showAccountInformation);
        }

        // Check view ads
        final Operation viewAds = permissionService.loadOperation("memberAds", "view");
        if (dto.getCanViewAdsOfGroups().isEmpty()) {
            operations.remove(viewAds);
        } else {
            operations.add(viewAds);
        }

        // Check view documents
        final Operation viewDocuments = permissionService.loadOperation("memberDocuments", "view");
        if (dto.getDocuments().isEmpty()) {
            operations.remove(viewDocuments);
        } else {
            operations.add(viewDocuments);
        }

        // Check view messages
        final Operation sendToAdministration = permissionService.loadOperation("memberMessages", "sendToAdministration");
        if (CollectionUtils.isEmpty(dto.getMessageCategories())) {
            operations.remove(sendToAdministration);
        } else {
            operations.add(sendToAdministration);
        }

        // Check make payments
        final Operation systemPayments = permissionService.loadOperation("memberPayments", "paymentToSystem");
        final Operation memberPayments = permissionService.loadOperation("memberPayments", "paymentToMember");
        final Operation selfPayments = permissionService.loadOperation("memberPayments", "paymentToSelf");

        final Collection<TransferType> memberToMemberTTs = dto.getMemberToMemberTTs();
        if (memberToMemberTTs != null && !memberToMemberTTs.isEmpty()) {
            operations.add(memberPayments);
        } else {
            operations.remove(memberPayments);
        }

        final Collection<TransferType> memberToSystemTTs = dto.getMemberToSystemTTs();
        if (memberToSystemTTs != null && !memberToSystemTTs.isEmpty()) {
            operations.add(systemPayments);
        } else {
            operations.remove(systemPayments);
        }

        final Collection<TransferType> selfPaymentTTs = dto.getSelfPaymentTTs();
        if (selfPaymentTTs != null && !selfPaymentTTs.isEmpty()) {
            operations.add(selfPayments);
        } else {
            operations.remove(selfPayments);
        }

        final Operation requestPayments = permissionService.loadOperation("memberPayments", "request");
        final Collection<Channel> requestPaymentByChannels = dto.getRequestPaymentByChannels();
        if (requestPaymentByChannels != null && !requestPaymentByChannels.isEmpty()) {
            operations.add(requestPayments);
        } else {
            operations.remove(requestPayments);
        }


        // Check issue guarantees
        final Operation issueGuaranteesOp = permissionService.loadOperation("memberGuarantees", "issueGuarantees");
        if (CollectionUtils.isEmpty(dto.getGuaranteeTypes())) {
            operations.remove(issueGuaranteesOp);
        } else {
            operations.add(issueGuaranteesOp);
        }

        // Check issue certifications
        final Operation issueCertificationsOp = permissionService.loadOperation("memberGuarantees", "issueCertifications");
        if (CollectionUtils.isEmpty(dto.getCanIssueCertificationToGroups())) {
            operations.remove(issueCertificationsOp);
        } else {
            operations.add(issueCertificationsOp);
        }

        // Check buy with payment obligations
        final Operation buyWithPaymentObligationsOp = permissionService.loadOperation("memberGuarantees", "buyWithPaymentObligations");
        if (dto.getCanBuyWithPaymentObligationsFromGroups().isEmpty()) {
            operations.remove(buyWithPaymentObligationsOp);
        } else {
            operations.add(buyWithPaymentObligationsOp);
        }

        // Check chargebacks
        final Operation chargeback = permissionService.loadOperation("memberPayments", "chargeback");
        if (CollectionUtils.isNotEmpty(dto.getChargebackTTs())) {
            operations.add(chargeback);
        } else {
            operations.remove(chargeback);
        }
    }

    /**
     * Process the dto before saving permissions. Basically, check each collection to set the specific permission.
     */
    private void process(final OperatorGroupPermissionsDTO dto) {
        final Collection<Operation> operations = dto.getOperations();

        // Check make payments
        final Operation accountInformation = permissionService.loadOperation("operatorAccount", "accountInformation");

        // Check account information
        final Collection<AccountType> canViewInformationOf = dto.getCanViewInformationOf();
        if (CollectionUtils.isNotEmpty(canViewInformationOf)) {
            operations.add(accountInformation);
        } else {
            operations.remove(accountInformation);
        }


        // Check guarantees types
        final Operation guaranteeTypes = permissionService.loadOperation("operatorGuarantees", "issueGuarantees");
        if (CollectionUtils.isEmpty(dto.getGuaranteeTypes())) {
            operations.remove(guaranteeTypes);
        } else {
            operations.add(guaranteeTypes);
        }
    }

    private void remove(final long id) {
        groupDao.delete(id);
    }

    @SuppressWarnings("unchecked")
    private void removeFromInverseCollections(final Group group) {

        // Payment filters
        final Collection<PaymentFilter> paymentFilters = group.getPaymentFilters();
        for (final PaymentFilter paymentFilter : paymentFilters) {
            final Collection<Group> groups = (Collection<Group>) paymentFilter.getGroups();
            groups.remove(group);
            paymentFilterDao.update(paymentFilter);
        }

        // Record types
        final Collection<MemberRecordType> recordTypes = group.getMemberRecordTypes();
        for (final MemberRecordType recordType : recordTypes) {
            recordType.getGroups().remove(group);
            memberRecordTypeDao.update(recordType);
        }

        if (group instanceof AdminGroup) {
            final AdminGroup adminGroup = (AdminGroup) group;

            // Connected admins viewed by
            final Collection<AdminGroup> connectedAdminsViewedBy = adminGroup.getConnectedAdminsViewedBy();
            for (final AdminGroup viewerAdminGroup : connectedAdminsViewedBy) {
                viewerAdminGroup.getViewConnectedAdminsOf().remove(adminGroup);
                groupDao.update(viewerAdminGroup);
            }

            // Admin custom fields
            final Collection<AdminCustomField> adminCustomFields = adminGroup.getAdminCustomFields();
            for (final AdminCustomField adminCustomField : adminCustomFields) {
                adminCustomField.getGroups().remove(adminGroup);
                customFieldDao.update(adminCustomField);
            }
        }

        if (group instanceof MemberGroup) {
            final MemberGroup memberGroup = (MemberGroup) group;

            // Account fees
            final Collection<AccountFee> accountFees = memberGroup.getAccountFees();
            for (final AccountFee accountFee : accountFees) {
                final Collection<MemberGroup> groups = accountFee.getGroups();
                groups.remove(memberGroup);
                accountFeeDao.update(accountFee);
            }

            // Managed by groups
            final Collection<AdminGroup> managedByGroups = memberGroup.getManagedByGroups();
            for (final AdminGroup adminGroup : managedByGroups) {
                adminGroup.getManagesGroups().remove(memberGroup);
                groupDao.update(adminGroup);
            }

            // Member custom fields and general remark custom fields)
            final Collection<CustomField> customFields = memberGroup.getCustomFields();
            for (final CustomField customField : customFields) {
                // Get the groups using reflection
                final Collection<MemberGroup> groups = PropertyHelper.get(customField, "groups");
                groups.remove(memberGroup);
                customFieldDao.update(customField);
            }

            // From transaction fees
            final Collection<TransactionFee> fromTransactionFees = memberGroup.getFromTransactionFees();
            for (final TransactionFee transactionFee : fromTransactionFees) {
                transactionFee.getFromGroups().remove(memberGroup);
                transactionFeeDao.update(transactionFee);
            }

            // To transaction fees
            final Collection<TransactionFee> toTransactionFees = memberGroup.getToTransactionFees();
            for (final TransactionFee transactionFee : toTransactionFees) {
                transactionFee.getToGroups().remove(memberGroup);
                transactionFeeDao.update(transactionFee);
            }

        }

    }

    @SuppressWarnings("unchecked")
    private Group save(Group group) {
        if (group.isTransient()) {
            group = groupDao.insert(group);
        } else {
            // We must keep the many-to-many relationships, or they would be cleared...
            final Group currentGroup = load(group.getId(), FETCH_TO_KEEP_DATA);
            group.setDocuments(new ArrayList<Document>(currentGroup.getDocuments()));
            group.setPermissions(new ArrayList<Operation>(currentGroup.getPermissions()));
            group.setTransferTypes(new ArrayList<TransferType>(currentGroup.getTransferTypes()));
            group.setConversionSimulationTTs(new ArrayList<TransferType>(currentGroup.getConversionSimulationTTs()));
            group.setMessageCategories(new ArrayList<MessageCategory>(currentGroup.getMessageCategories()));
            group.setGuaranteeTypes(new ArrayList<GuaranteeType>(currentGroup.getGuaranteeTypes()));
            group.setChargebackTransferTypes(new ArrayList<TransferType>(currentGroup.getChargebackTransferTypes()));
            if (group instanceof AdminGroup) {
                final AdminGroup adminGroup = (AdminGroup) group;
                final AdminGroup currentAdminGroup = ((AdminGroup) currentGroup);
                adminGroup.setTransferTypesAsMember(new ArrayList<TransferType>(currentAdminGroup.getTransferTypesAsMember()));
                adminGroup.setManagesGroups(new ArrayList<MemberGroup>(currentAdminGroup.getManagesGroups()));
                adminGroup.setViewConnectedAdminsOf(new ArrayList<AdminGroup>(currentAdminGroup.getViewConnectedAdminsOf()));
                adminGroup.setViewInformationOf(new ArrayList<SystemAccountType>(currentAdminGroup.getViewInformationOf()));
                adminGroup.setViewAdminRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getViewAdminRecordTypes()));
                adminGroup.setCreateAdminRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getCreateAdminRecordTypes()));
                adminGroup.setModifyAdminRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getModifyAdminRecordTypes()));
                adminGroup.setDeleteAdminRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getDeleteAdminRecordTypes()));
                adminGroup.setViewMemberRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getViewMemberRecordTypes()));
                adminGroup.setCreateMemberRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getCreateMemberRecordTypes()));
                adminGroup.setModifyMemberRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getModifyMemberRecordTypes()));
                adminGroup.setDeleteMemberRecordTypes(new ArrayList<MemberRecordType>(currentAdminGroup.getDeleteMemberRecordTypes()));
            }
            if (group instanceof BrokerGroup) {
                final BrokerGroup brokerGroup = (BrokerGroup) group;
                final BrokerGroup currentBrokerGroup = (BrokerGroup) currentGroup;

                final List<Document> brokerDocuments = new ArrayList<Document>();
                if (currentBrokerGroup.getBrokerDocuments() != null) {
                    brokerDocuments.addAll(currentBrokerGroup.getBrokerDocuments());
                }
                brokerGroup.setBrokerDocuments(brokerDocuments);

                final List<AccountType> brokerCanViewInformationOf = new ArrayList<AccountType>();
                if (brokerGroup.getBrokerCanViewInformationOf() != null) {
                    brokerCanViewInformationOf.addAll(brokerGroup.getBrokerCanViewInformationOf());
                }
                brokerGroup.setBrokerCanViewInformationOf(brokerCanViewInformationOf);

                brokerGroup.setTransferTypesAsMember(new ArrayList<TransferType>(currentBrokerGroup.getTransferTypesAsMember()));
                brokerGroup.setBrokerConversionSimulationTTs(new ArrayList<TransferType>(currentBrokerGroup.getBrokerConversionSimulationTTs()));
                brokerGroup.setBrokerMemberRecordTypes(new ArrayList<MemberRecordType>(currentBrokerGroup.getBrokerMemberRecordTypes()));
                brokerGroup.setBrokerCreateMemberRecordTypes(new ArrayList<MemberRecordType>(currentBrokerGroup.getBrokerCreateMemberRecordTypes()));
                brokerGroup.setBrokerModifyMemberRecordTypes(new ArrayList<MemberRecordType>(currentBrokerGroup.getBrokerModifyMemberRecordTypes()));
                brokerGroup.setBrokerDeleteMemberRecordTypes(new ArrayList<MemberRecordType>(currentBrokerGroup.getBrokerDeleteMemberRecordTypes()));

                // "possibleInitialGroups" is updated at edit group screen, so it doesn't need to be copied
            }
            if (group instanceof MemberGroup) {
                final MemberGroup memberGroup = (MemberGroup) group;
                final MemberGroup currentMemberGroup = (MemberGroup) currentGroup;
                memberGroup.setAccountSettings(currentMemberGroup.getAccountSettings());

                // Ensure that no channel will be set by default if it's not accessible
                memberGroup.getDefaultChannels().retainAll(memberGroup.getChannels());

                // Ensure the removedChannels collection contains the channels which were removed
                final Collection<Channel> removedChannels = new HashSet<Channel>();
                removedChannels.addAll(currentMemberGroup.getChannels());
                removedChannels.removeAll(memberGroup.getChannels());

                final List<MemberGroup> viewProfile = new ArrayList<MemberGroup>();
                if (currentMemberGroup.getCanViewProfileOfGroups() != null) {
                    viewProfile.addAll(currentMemberGroup.getCanViewProfileOfGroups());
                }
                memberGroup.setCanViewProfileOfGroups(viewProfile);

                final List<AccountType> canViewInformationOf = new ArrayList<AccountType>();
                if (currentMemberGroup.getCanViewInformationOf() != null) {
                    canViewInformationOf.addAll(currentMemberGroup.getCanViewInformationOf());
                }
                memberGroup.setCanViewInformationOf(canViewInformationOf);

                final List<MemberGroup> viewAds = new ArrayList<MemberGroup>();
                if (currentMemberGroup.getCanViewAdsOfGroups() != null) {
                    viewAds.addAll(currentMemberGroup.getCanViewAdsOfGroups());
                }
                memberGroup.setCanViewAdsOfGroups(viewAds);

                final List<AdminGroup> managedByGroups = new ArrayList<AdminGroup>();
                if (currentMemberGroup.getManagedByGroups() != null) {
                    managedByGroups.addAll(currentMemberGroup.getManagedByGroups());
                }
                memberGroup.setManagedByGroups(managedByGroups);

                // "defaultMailMessages" is updated at edit group screen, so it doesn't need to be copied
                // "smsMessages" is updated at edit group screen, so it doesn't need to be copied
                // "defaultSmsMessages" is updated at edit group screen, so it doesn't need to be copied
                // "channels" is updated at edit group screen, so it doesn't need to be copied
                // "defaultChannels" is updated at edit group screen, so it doesn't need to be copied

                final List<Channel> requestPaymentByChannels = new ArrayList<Channel>();
                if (currentMemberGroup.getRequestPaymentByChannels() != null) {
                    requestPaymentByChannels.addAll(currentMemberGroup.getRequestPaymentByChannels());
                }
                memberGroup.setRequestPaymentByChannels(requestPaymentByChannels);

                final MemberGroupSettings memberSettings = memberGroup.getMemberSettings();
                memberSettings.setGroupAfterExpiration(fetchService.fetch(memberSettings.getGroupAfterExpiration()));

                // Update the basic settings of operator groups for members in this group
                final GroupQuery operatorQuery = new GroupQuery();
                operatorQuery.setNature(Group.Nature.OPERATOR);
                operatorQuery.fetch(RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
                final List<OperatorGroup> operatorGroups = (List<OperatorGroup>) groupDao.search(operatorQuery);
                for (final OperatorGroup operatorGroup : operatorGroups) {
                    if (operatorGroup.getMember().getGroup().equals(memberGroup)) {
                        groupDao.update(operatorGroup);
                    }
                }
                final List<MemberGroup> canIssueCertificationToGroups = new ArrayList<MemberGroup>();
                if (currentMemberGroup.getCanIssueCertificationToGroups() != null) {
                    canIssueCertificationToGroups.addAll(currentMemberGroup.getCanIssueCertificationToGroups());
                }
                memberGroup.setCanIssueCertificationToGroups(canIssueCertificationToGroups);

                final List<MemberGroup> canBuyWithPaymentObligationsFromGroups = new ArrayList<MemberGroup>();
                if (currentMemberGroup.getCanBuyWithPaymentObligationsFromGroups() != null) {
                    canBuyWithPaymentObligationsFromGroups.addAll(currentMemberGroup.getCanBuyWithPaymentObligationsFromGroups());
                }
                memberGroup.setCanBuyWithPaymentObligationsFromGroups(canBuyWithPaymentObligationsFromGroups);

                if (!StringUtils.equals(memberSettings.getSmsContextClassName(), currentMemberGroup.getMemberSettings().getSmsContextClassName())) {
                    clearSmsContext(memberGroup);
                }

                // Ensure the message notification types are not present on the group for SMS
                final Collection<Message.Type> smsMessages = memberGroup.getSmsMessages();
                if (smsMessages != null) {
                    smsMessages.remove(Message.Type.FROM_MEMBER);
                    smsMessages.remove(Message.Type.FROM_ADMIN_TO_GROUP);
                    smsMessages.remove(Message.Type.FROM_ADMIN_TO_MEMBER);
                }
                final Collection<Type> defaultSmsMessages = memberGroup.getDefaultSmsMessages();
                if (defaultSmsMessages != null) {
                    defaultSmsMessages.remove(Message.Type.FROM_MEMBER);
                    defaultSmsMessages.remove(Message.Type.FROM_ADMIN_TO_GROUP);
                    defaultSmsMessages.remove(Message.Type.FROM_ADMIN_TO_MEMBER);
                }

                // Remove from all members channels which are no longer accessible
                elementDao.removeChannelsFromMembers(memberGroup, removedChannels);
            }
            if (group instanceof OperatorGroup) {
                final OperatorGroup operatorGroup = (OperatorGroup) group;
                final OperatorGroup currentOperatorGroup = (OperatorGroup) currentGroup;

                // Check the account types
                final List<AccountType> canViewInformationOf = new ArrayList<AccountType>();
                if (currentOperatorGroup.getCanViewInformationOf() != null) {
                    canViewInformationOf.addAll(currentOperatorGroup.getCanViewInformationOf());
                }
                operatorGroup.setCanViewInformationOf(canViewInformationOf);
            }
            group = groupDao.update(group);

            // Ensure that logged users of this group will have updated references
            getAccessService().updateGroupReference(group);

            // Ensure the permissions cache for this group is reloaded
            permissionService.refreshCache(group);
        }
        return group;
    }

    @SuppressWarnings("unchecked")
    private <G extends Group> G updateCollections(final GroupPermissionsDTO<G> dto) {
        dto.setOperations(permissionService.getCachedOperations(dto.getOperations()));
        G group = fetchService.fetch(dto.getGroup());
        group.setPermissions(dto.getOperations());
        group.setDocuments(dto.getDocuments());
        group.setMessageCategories(dto.getMessageCategories());
        group.setGuaranteeTypes(dto.getGuaranteeTypes());
        final Collection<TransferType> conversionSimulationTTs = dto.getConversionSimulationTTs();
        group.setConversionSimulationTTs(conversionSimulationTTs);

        if (group instanceof MemberGroup) {
            final MemberGroup memberGroup = (MemberGroup) group;
            final MemberGroupPermissionsDTO<MemberGroup> memberDto = (MemberGroupPermissionsDTO<MemberGroup>) dto;
            memberGroup.setCanViewAdsOfGroups(memberDto.getCanViewAdsOfGroups());
            memberGroup.setCanViewProfileOfGroups(memberDto.getCanViewProfileOfGroups());
            memberGroup.setCanViewInformationOf(memberDto.getCanViewInformationOf());
            memberGroup.setRequestPaymentByChannels(memberDto.getRequestPaymentByChannels());
            memberGroup.setCanIssueCertificationToGroups(memberDto.getCanIssueCertificationToGroups());
            memberGroup.setCanBuyWithPaymentObligationsFromGroups(memberDto.getCanBuyWithPaymentObligationsFromGroups());
            memberGroup.setChargebackTransferTypes(memberDto.getChargebackTTs());

            final Collection<TransferType> memberToMemberTTs = memberDto.getMemberToMemberTTs();
            final Collection<TransferType> memberToSystemTTs = memberDto.getMemberToSystemTTs();
            final Collection<TransferType> selfPaymentTTs = memberDto.getSelfPaymentTTs();
            final Collection<TransferType> transferTypes = new ArrayList<TransferType>();
            if (memberToMemberTTs != null) {
                transferTypes.addAll(memberToMemberTTs);
            }
            if (memberToSystemTTs != null) {
                transferTypes.addAll(memberToSystemTTs);
            }
            if (selfPaymentTTs != null) {
                transferTypes.addAll(selfPaymentTTs);
            }
            memberGroup.setTransferTypes(transferTypes);

            if (group instanceof BrokerGroup) {
                final BrokerGroup brokerGroup = (BrokerGroup) group;
                final BrokerGroupPermissionsDTO brokerDto = (BrokerGroupPermissionsDTO) (GroupPermissionsDTO) dto;
                brokerGroup.setBrokerDocuments(brokerDto.getBrokerDocuments());

                final Collection<TransferType> asMemberToMemberTTs = brokerDto.getAsMemberToMemberTTs();
                final Collection<TransferType> asMemberToSelfTTs = brokerDto.getAsMemberToSelfTTs();
                final Collection<TransferType> asMemberToSystemTTs = brokerDto.getAsMemberToSystemTTs();
                final Collection<TransferType> transferTypesAsMember = new ArrayList<TransferType>();
                if (asMemberToMemberTTs != null) {
                    transferTypesAsMember.addAll(asMemberToMemberTTs);
                }
                if (asMemberToSelfTTs != null) {
                    transferTypesAsMember.addAll(asMemberToSelfTTs);
                }
                if (asMemberToSystemTTs != null) {
                    transferTypesAsMember.addAll(asMemberToSystemTTs);
                }
                brokerGroup.setTransferTypesAsMember(transferTypesAsMember);

                final Collection<TransferType> brokerConversionSimulationTTs = brokerDto.getBrokerConversionSimulationTTs();
                brokerGroup.setBrokerConversionSimulationTTs(brokerConversionSimulationTTs);

                brokerGroup.setBrokerCanViewInformationOf(brokerDto.getBrokerCanViewInformationOf());
                brokerGroup.setBrokerMemberRecordTypes(brokerDto.getBrokerMemberRecordTypes());
                brokerGroup.setBrokerCreateMemberRecordTypes(brokerDto.getBrokerCreateMemberRecordTypes());
                brokerGroup.setBrokerModifyMemberRecordTypes(brokerDto.getBrokerModifyMemberRecordTypes());
                brokerGroup.setBrokerDeleteMemberRecordTypes(brokerDto.getBrokerDeleteMemberRecordTypes());
            }
        } else if (group instanceof OperatorGroup) {
            final OperatorGroup operatorGroup = (OperatorGroup) group;
            final OperatorGroupPermissionsDTO operatorDto = (OperatorGroupPermissionsDTO) dto;

            operatorGroup.setCanViewInformationOf(operatorDto.getCanViewInformationOf());

            final Collection<TransferType> memberToMemberTTs = operatorDto.getMemberToMemberTTs();
            final Collection<TransferType> memberToSystemTTs = operatorDto.getMemberToSystemTTs();
            final Collection<TransferType> selfPaymentTTs = operatorDto.getSelfPaymentTTs();
            final Collection<TransferType> externalPaymentTTs = operatorDto.getExternalPaymentTTs();

            final Collection<TransferType> transferTypes = new ArrayList<TransferType>();
            if (memberToMemberTTs != null) {
                transferTypes.addAll(memberToMemberTTs);
            }
            if (memberToSystemTTs != null) {
                transferTypes.addAll(memberToSystemTTs);
            }
            if (selfPaymentTTs != null) {
                transferTypes.addAll(selfPaymentTTs);
            }
            if (externalPaymentTTs != null) {
                transferTypes.addAll(externalPaymentTTs);
            }
            operatorGroup.setTransferTypes(transferTypes);

        } else { // AdminGroup
            final AdminGroup adminGroup = (AdminGroup) group;
            final AdminGroupPermissionsDTO adminDto = (AdminGroupPermissionsDTO) (GroupPermissionsDTO) dto;

            final Collection<TransferType> grantLoanTTs = adminDto.getGrantLoanTTs();
            final Collection<TransferType> systemToMemberTTs = adminDto.getSystemToMemberTTs();
            final Collection<TransferType> systemToSystemTTs = adminDto.getSystemToSystemTTs();

            final Collection<TransferType> transferTypes = new ArrayList<TransferType>();
            if (grantLoanTTs != null) {
                transferTypes.addAll(grantLoanTTs);
            }
            if (systemToMemberTTs != null) {
                transferTypes.addAll(systemToMemberTTs);
            }
            if (systemToSystemTTs != null) {
                transferTypes.addAll(systemToSystemTTs);
            }
            adminGroup.setTransferTypes(transferTypes);

            final Collection<TransferType> asMemberToMemberTTs = adminDto.getAsMemberToMemberTTs();
            final Collection<TransferType> asMemberToSelfTTs = adminDto.getAsMemberToSelfTTs();
            final Collection<TransferType> asMemberToSystemTTs = adminDto.getAsMemberToSystemTTs();

            final Collection<TransferType> transferTypesAsMember = new ArrayList<TransferType>();
            if (asMemberToMemberTTs != null) {
                transferTypesAsMember.addAll(asMemberToMemberTTs);
            }
            if (asMemberToSelfTTs != null) {
                transferTypesAsMember.addAll(asMemberToSelfTTs);
            }
            if (asMemberToSystemTTs != null) {
                transferTypesAsMember.addAll(asMemberToSystemTTs);
            }
            adminGroup.setTransferTypesAsMember(transferTypesAsMember);

            final Collection<TransferType> chargebacks = new ArrayList<TransferType>();
            if (adminDto.getSystemChargebackTTs() != null) {
                chargebacks.addAll(adminDto.getSystemChargebackTTs());
            }
            if (adminDto.getMemberChargebackTTs() != null) {
                chargebacks.addAll(adminDto.getMemberChargebackTTs());
            }
            adminGroup.setChargebackTransferTypes(chargebacks);

            adminGroup.setViewInformationOf(adminDto.getViewInformationOf());
            adminGroup.setManagesGroups(adminDto.getManagesGroups());
            adminGroup.setViewConnectedAdminsOf(adminDto.getViewConnectedAdminsOf());
            adminGroup.setViewAdminRecordTypes(adminDto.getViewAdminRecordTypes());
            adminGroup.setCreateAdminRecordTypes(adminDto.getCreateAdminRecordTypes());
            adminGroup.setModifyAdminRecordTypes(adminDto.getModifyAdminRecordTypes());
            adminGroup.setDeleteAdminRecordTypes(adminDto.getDeleteAdminRecordTypes());
            adminGroup.setViewMemberRecordTypes(adminDto.getViewMemberRecordTypes());
            adminGroup.setCreateMemberRecordTypes(adminDto.getCreateMemberRecordTypes());
            adminGroup.setModifyMemberRecordTypes(adminDto.getModifyMemberRecordTypes());
            adminGroup.setDeleteMemberRecordTypes(adminDto.getDeleteMemberRecordTypes());
        }
        group = groupDao.update(group);
        permissionService.refreshCache(group);
        return group;
    }

}
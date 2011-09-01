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
package nl.strohalm.cyclos.services.elements;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import nl.strohalm.cyclos.dao.access.UserDAO;
import nl.strohalm.cyclos.dao.access.UsernameChangeLogDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.exceptions.QueryParseException;
import nl.strohalm.cyclos.dao.groups.GroupHistoryLogDAO;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.dao.members.NotificationPreferenceDAO;
import nl.strohalm.cyclos.dao.members.PendingMemberDAO;
import nl.strohalm.cyclos.dao.members.RegistrationAgreementLogDAO;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.access.UsernameChangeLog;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.MemberAccount;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.cards.Card;
import nl.strohalm.cyclos.entities.accounts.loans.Loan;
import nl.strohalm.cyclos.entities.accounts.loans.LoanParameters;
import nl.strohalm.cyclos.entities.accounts.loans.LoanQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.InvoiceQuery;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.GroupHistoryLog;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.AdminQuery;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.BrokeringQuery;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.ElementQuery;
import nl.strohalm.cyclos.entities.members.FullTextAdminQuery;
import nl.strohalm.cyclos.entities.members.FullTextElementQuery;
import nl.strohalm.cyclos.entities.members.FullTextMemberQuery;
import nl.strohalm.cyclos.entities.members.FullTextOperatorQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberQuery;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.OperatorQuery;
import nl.strohalm.cyclos.entities.members.PendingMember;
import nl.strohalm.cyclos.entities.members.PendingMemberQuery;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.entities.members.RegistrationAgreement;
import nl.strohalm.cyclos.entities.members.RegistrationAgreementLog;
import nl.strohalm.cyclos.entities.members.adInterests.AdInterestQuery;
import nl.strohalm.cyclos.entities.members.brokerings.Brokering;
import nl.strohalm.cyclos.entities.members.messages.Message.Type;
import nl.strohalm.cyclos.entities.members.preferences.NotificationPreference;
import nl.strohalm.cyclos.entities.members.remarks.GroupRemark;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.settings.AccessSettings.UsernameGeneration;
import nl.strohalm.cyclos.exceptions.MailSendingException;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.accounts.MemberAccountHandler;
import nl.strohalm.cyclos.services.accounts.cards.CardService;
import nl.strohalm.cyclos.services.accounts.pos.PosService;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.exceptions.MemberHasBalanceException;
import nl.strohalm.cyclos.services.elements.exceptions.MemberHasOpenInvoicesException;
import nl.strohalm.cyclos.services.elements.exceptions.MemberHasPendingLoansException;
import nl.strohalm.cyclos.services.elements.exceptions.MemberHasPendingScheduledPaymentsException;
import nl.strohalm.cyclos.services.elements.exceptions.RegistrationAgreementNotAcceptedException;
import nl.strohalm.cyclos.services.elements.exceptions.UsernameAlreadyInUseException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.preferences.PreferenceService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.transactions.InvoiceService;
import nl.strohalm.cyclos.services.transactions.LoanService;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.ScheduledPaymentService;
import nl.strohalm.cyclos.utils.CacheCleaner;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.HashHandler;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.MessageResolver;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RangeConstraint;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.StringHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.DelegatingValidator;
import nl.strohalm.cyclos.utils.validation.EmailValidation;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.LengthValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.UniqueError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.Validator.Property;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Implementation for element service
 * @author luis
 */
public class ElementServiceImpl implements ElementService {

    private static enum ActivationMail {
        IGNORE, THREADED, ONLINE
    }

    /**
     * Validates that the element's username is not already taken
     * @author luis
     */
    private class ExistingUsernameValidation implements GeneralValidation {
        private static final long serialVersionUID = -3358417537796698704L;

        public ValidationError validate(final Object object) {
            String username = null;
            if (object instanceof Element) {
                username = ((Element) object).getUsername();
            } else if (object instanceof PendingMember) {
                username = ((PendingMember) object).getUsername();
            }
            final Long id = ((Entity) object).getId();

            if (StringUtils.isEmpty(username)) {
                return null;
            }
            boolean existing = false;
            if (object instanceof Operator) {
                Member member;
                if (LoggedUser.isOperator()) { // an operator modifying his own profile
                    member = (Member) LoggedUser.accountOwner();
                } else {
                    member = LoggedUser.element();
                }

                try {
                    final OperatorUser existingOperator = userDao.loadOperator(member, username);
                    existing = !existingOperator.getId().equals(id);
                } catch (final EntityNotFoundException e) {
                    // not found. ok
                }
            } else {
                // Search in Elements
                try {
                    final User existingUser = userDao.load(username);
                    existing = !existingUser.getId().equals(id);
                } catch (final EntityNotFoundException e) {
                    // not found. ok
                }
                if (!existing) {
                    // Search in PendingMembers
                    try {
                        final PendingMember pendingMember = pendingMemberDao.loadByUsername(username);
                        if (object instanceof PendingMember && pendingMember.getId().equals(((PendingMember) object).getId())) {
                            // Updating the same pending member. Don't consider as existing
                            existing = false;
                        } else {
                            existing = true;
                        }
                    } catch (final EntityNotFoundException e) {
                        // not found. ok
                    }
                }
            }
            if (existing) {
                // If it got to this point, an user with the given username already exists
                return new ValidationError("createMember.error.usernameAlreadyInUse", username);
            }
            return null;
        }
    }

    private class PendingMemberEmailValidation implements PropertyValidation {
        private final PendingMember pendingMember;
        private static final long   serialVersionUID = 377970183876523686L;

        private PendingMemberEmailValidation(final PendingMember pendingMember) {
            this.pendingMember = pendingMember;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String email = (String) value;
            if (StringUtils.isNotEmpty(email) && pendingMemberDao.emailExists(pendingMember, email)) {
                return new UniqueError();
            }
            return null;
        }
    }

    private class UniqueEmailValidation implements PropertyValidation {
        private static final long serialVersionUID = -1170302387628372503L;
        private Long              userId;

        private UniqueEmailValidation(final Long userId) {
            this.userId = userId;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String email = (String) value;
            if (StringUtils.isEmpty(email)) {
                return null;
            }
            try {
                final Element loaded = elementDao.loadByEmail(email);
                if (userId == null) {
                    // member is new, not yet persisted element, so always wrong if the email already exists
                    return new UniqueError();
                }
                if (!loaded.getId().equals(userId)) {
                    return new UniqueError();
                }
            } catch (final EntityNotFoundException e) {
                // Ok, no one had that e-mail
            }
            return null;
        }
    }

    private MessageResolver             messageResolver = new MessageResolver.NoOpMessageResolver();
    private ElementDAO                  elementDao;
    private GroupHistoryLogDAO          groupHistoryLogDao;
    private NotificationPreferenceDAO   notificationPreferenceDao;
    private UserDAO                     userDao;
    private AccessService               accessService;
    private AccountService              accountService;
    private AdInterestService           adInterestService;
    private AdService                   adService;
    private BrokeringService            brokeringService;
    private CommissionService           commissionService;
    private ContactService              contactService;
    private CustomFieldService          customFieldService;
    private FetchService                fetchService;
    private GroupService                groupService;
    private InvoiceService              invoiceService;
    private PaymentService              paymentService;
    private LoanService                 loanService;
    private ScheduledPaymentService     scheduledPaymentService;
    private PreferenceService           preferenceService;
    private RemarkService               remarkService;
    private SettingsService             settingsService;
    private HashHandler                 hashHandler;
    private MailHandler                 mailHandler;
    private MemberAccountHandler        memberAccountHandler;
    private PendingMemberDAO            pendingMemberDao;
    private RegistrationAgreementLogDAO registrationAgreementLogDao;
    private CardService                 cardService;
    private PosService                  posService;
    private PermissionService           permissionService;
    private UsernameChangeLogDAO        usernameChangeLogDao;
    private boolean                     skipOrder;
    private boolean                     forceKeywords;

    public void acceptAgreement(final Member member, final RegistrationAgreement registrationAgreement, final String remoteAddress) {
        validateAgreement(member, registrationAgreement);

        final RegistrationAgreementLog log = new RegistrationAgreementLog();
        log.setMember(member);
        log.setRegistrationAgreement(registrationAgreement);
        log.setDate(Calendar.getInstance());
        log.setRemoteAddress(remoteAddress);
        registrationAgreementLogDao.insert(log);
    }

    public void addMissingEntitiesToIndex() {
        elementDao.addMissingEntitiesToIndex();
    }

    @SuppressWarnings("unchecked")
    public BulkMemberActionResultVO bulkChangeMemberGroup(final FullTextMemberQuery query, MemberGroup newGroup, final String comments) throws ValidationException {
        if (newGroup == null || newGroup.isTransient()) {
            throw new ValidationException();
        }
        if (StringUtils.isEmpty(comments)) {
            throw new ValidationException();
        }
        newGroup = fetchService.fetch(newGroup);

        int changed = 0;
        int unchanged = 0;
        final List<Member> members = (List<Member>) fullTextSearch(query);
        for (final Member member : members) {
            if (newGroup.equals(member.getGroup())) {
                unchanged++;
            } else {
                changeMemberGroup(member, newGroup, comments);
                changed++;
            }
            fetchService.clearCache();
        }
        return new BulkMemberActionResultVO(changed, unchanged);
    }

    public Administrator changeAdminGroup(Administrator admin, AdminGroup newGroup, final String comments) {
        newGroup = fetchService.fetch(newGroup);
        // Validate the arguments
        final Element loggedElement = LoggedUser.element();
        if (admin == null || newGroup == null || StringUtils.isEmpty(comments) || loggedElement.equals(admin)) {
            throw new ValidationException();
        }

        // Check the current group
        admin = (Administrator) load(admin.getId(), Element.Relationships.USER, Element.Relationships.GROUP);
        final AdminGroup oldGroup = admin.getAdminGroup();
        if (oldGroup.equals(newGroup) || oldGroup.getStatus() == Group.Status.REMOVED) {
            throw new ValidationException();
        }

        if (newGroup.getStatus() == Group.Status.REMOVED) {
            // Disconnect the user if he is logged in
            try {
                accessService.disconnectLoggedAdmin(admin.getAdminUser(), null);
            } catch (final NotConnectedException e) {
                // Ok - not logged in
            }
        }

        // Update the group
        admin.setGroup(newGroup);
        elementDao.update(admin);

        // Creates the group remark and updates group history logs
        createGroupRemark(admin, oldGroup, newGroup, comments);

        // Update the index
        elementDao.addToIndex(admin);

        return admin;
    }

    public Administrator changeAdminProfile(final Administrator admin) {
        return (Administrator) save(admin, null, WhenSaving.PROFILE, false, null);
    }

    public Member changeMemberGroup(Member member, MemberGroup newGroup, final String comments) throws MemberHasBalanceException, MemberHasOpenInvoicesException {
        // Validate the arguments
        if (member == null || newGroup == null || StringUtils.isEmpty(comments)) {
            throw new ValidationException();
        }

        // Check the current group
        member = (Member) load(member.getId(), Element.Relationships.USER, Element.Relationships.GROUP);
        final MemberGroup oldGroup = member.getMemberGroup();
        if (oldGroup.equals(newGroup) || oldGroup.getStatus() == Group.Status.REMOVED) {
            throw new ValidationException();
        }

        if (LoggedUser.isValid()) {
            // Check if the admin has permission to manage the target group
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
            if (!adminGroup.getManagesGroups().contains(newGroup)) {
                throw new PermissionDeniedException();
            }
        }

        // Check the new group
        newGroup = fetchService.fetch(newGroup);
        checkNewGroup(member, newGroup);

        if (newGroup.getStatus() == Group.Status.REMOVED) {
            // Disconnect the user if he is logged in
            try {
                accessService.disconnectLoggedMember(member.getMemberUser(), null);
            } catch (final NotConnectedException e) {
                // Ok - not logged in
            }

            // Remove all ads
            final AdQuery adQuery = new AdQuery();
            adQuery.setOwner(member);
            adService.remove(EntityHelper.toIds(adService.search(adQuery)));

            // Remove all ad interests
            final AdInterestQuery adInterestQuery = new AdInterestQuery();
            adInterestQuery.setOwner(member);
            adInterestService.remove(EntityHelper.toIds(adInterestService.search(adInterestQuery)));

            // Remove all notification preferences
            notificationPreferenceDao.delete(EntityHelper.toIds(notificationPreferenceDao.load(member)));

            // Remove all contacts
            contactService.remove(EntityHelper.toIds(contactService.list(member)));

            // Cancel all cards
            cardService.cancelAllMemberCards(member);

            // Unassign all pos
            posService.unassignAllMemberPos(member);
        }

        // Remove all brokerings if the member is no longer a broker
        if (oldGroup.isBroker() && (newGroup.getStatus() == Group.Status.REMOVED || !newGroup.isBroker())) {
            final BrokeringQuery brokeringQuery = new BrokeringQuery();
            brokeringQuery.setBroker(member);
            brokeringQuery.setResultType(ResultType.ITERATOR);
            final MessageSettings messageSettings = settingsService.getMessageSettings();
            String brokeringComments = messageSettings.getBrokerRemovedRemarkComments();
            brokeringComments = MessageProcessingHelper.processVariables(brokeringComments, member, settingsService.getLocalSettings());
            for (final Brokering brokering : brokeringService.search(brokeringQuery)) {
                brokeringService.remove(brokering, brokeringComments);
            }
        }

        // Update broker commissions
        if (oldGroup.isBroker() || newGroup.isBroker()) {
            commissionService.updateBrokerCommissions(member, oldGroup, newGroup);
        }

        // Update the group
        member.setGroup(newGroup);
        elementDao.update(member);
        final boolean wasInactive = !member.isActive();
        handleAccounts(member);
        if (wasInactive && member.isActive()) {
            sendActivationMailIfNeeded(ActivationMail.ONLINE, member);
        }

        // Creates the group remark and updates group history logs
        createGroupRemark(member, oldGroup, newGroup, comments);

        // Update the index
        elementDao.addToIndex(member);
        return member;
    }

    public Member changeMemberProfileByAdmin(final Member member) {
        return (Member) save(member, null, WhenSaving.PROFILE, false, null);
    }

    public Member changeMemberProfileByBroker(final Member member) {
        return (Member) save(member, null, WhenSaving.PROFILE, false, null);
    }

    public Member changeMemberProfileByWebService(final ServiceClient client, final Member member) {
        if (member.isTransient()) {
            throw new UnexpectedEntityException();
        }
        final Element current = load(member.getId());
        final Set<MemberGroup> manageGroups = fetchService.fetch(client, ServiceClient.Relationships.MANAGE_GROUPS).getManageGroups();
        if (!manageGroups.contains(current.getGroup())) {
            throw new PermissionDeniedException();
        }
        return (Member) save(member, ActivationMail.THREADED, WhenSaving.PROFILE, false, null);
    }

    public Administrator changeMyProfile(final Administrator admin) {
        final Element loggedElement = LoggedUser.element();
        if (!loggedElement.equals(admin)) {
            throw new UnexpectedEntityException();
        }
        return (Administrator) save(admin, null, WhenSaving.PROFILE, false, null);
    }

    public Member changeMyProfile(final Member member) {
        final Element loggedElement = LoggedUser.element();
        if (!loggedElement.equals(member)) {
            throw new UnexpectedEntityException();
        }
        return (Member) save(member, null, WhenSaving.PROFILE, false, null);
    }

    public Operator changeMyProfile(final Operator operator) throws UnexpectedEntityException {
        final Element loggedElement = LoggedUser.element();
        if (!loggedElement.equals(operator)) {
            throw new UnexpectedEntityException();
        }
        return (Operator) save(operator, null, WhenSaving.PROFILE, false, null);
    }

    public Operator changeOperatorGroup(Operator operator, OperatorGroup newGroup, final String comments) {
        newGroup = fetchService.fetch(newGroup);

        // Validate the arguments
        if (operator == null || newGroup == null || StringUtils.isEmpty(comments)) {
            throw new ValidationException();
        }

        // Check the current group
        operator = (Operator) load(operator.getId(), Element.Relationships.GROUP);
        final OperatorGroup oldGroup = operator.getOperatorGroup();
        if (oldGroup.equals(newGroup) || oldGroup.getStatus() == Group.Status.REMOVED) {
            throw new ValidationException();
        }

        if (newGroup.getStatus() == Group.Status.REMOVED) {
            // Disconnect the user if he is logged in
            try {
                accessService.disconnectLoggedOperator(operator.getOperatorUser(), null);
            } catch (final NotConnectedException e) {
                // Ok - not logged in
            }
        }

        // Update the group
        operator.setGroup(newGroup);
        elementDao.update(operator);

        // Creates the group remark and updates group history logs
        createGroupRemark(operator, oldGroup, newGroup, comments);

        // Update the index
        elementDao.addToIndex(operator);

        return operator;
    }

    public Operator changeOperatorProfile(final Operator operator) {
        final Member loggedMember = LoggedUser.element();
        try {
            final Operator current = (Operator) load(operator.getId(), Operator.Relationships.MEMBER);
            if (!loggedMember.equals(current.getMember())) {
                throw new Exception();
            }
        } catch (final Exception e) {
            throw new UnexpectedEntityException();
        }
        return (Operator) save(operator, null, WhenSaving.PROFILE, false, null);
    }

    public void createAgreementForAllMembers(final RegistrationAgreement registrationAgreement, final MemberGroup group) {
        final CacheCleaner cacheCleaner = new CacheCleaner(fetchService);
        final Iterator<Member> iterator = elementDao.resolveMemberWhichDidntAccept(group, registrationAgreement);
        final Calendar now = Calendar.getInstance();
        while (iterator.hasNext()) {
            final RegistrationAgreementLog log = new RegistrationAgreementLog();
            log.setMember(iterator.next());
            log.setRegistrationAgreement(registrationAgreement);
            log.setDate(now);
            registrationAgreementLogDao.insert(log);
            cacheCleaner.clearCache();
        }
    }

    @SuppressWarnings("unchecked")
    public List<? extends Element> fullTextSearch(final FullTextElementQuery query) {
        // Full text search may only be used when there are keywords. Otherwise, perform a normal search
        if (StringUtils.isEmpty(query.getKeywords())) {
            if (requiresKeywordsForSearch()) {
                throw new QueryParseException();
            }
            return search(query.toQuery());
        }

        // Since the full text does not index group filters, only groups, disassemble the group filters into groups
        if (query instanceof FullTextMemberQuery) {
            final FullTextMemberQuery memberQuery = (FullTextMemberQuery) query;
            final Collection<GroupFilter> groupFilters = fetchService.fetch(memberQuery.getGroupFilters(), GroupFilter.Relationships.GROUPS);
            if (CollectionUtils.isNotEmpty(groupFilters)) {
                final boolean hasGroupFilters = CollectionUtils.isNotEmpty(memberQuery.getGroupFilters());
                final boolean hasGroups = CollectionUtils.isNotEmpty(memberQuery.getGroups());
                final Set<Group> groupsFromFilters = new HashSet<Group>();
                if (hasGroupFilters) {
                    // Get all groups from selected group filters
                    for (final GroupFilter groupFilter : groupFilters) {
                        groupsFromFilters.addAll(groupFilter.getGroups());
                    }
                    if (hasGroups) {
                        // When there's both groups and group filters, use an intersection between them
                        memberQuery.setGroups(CollectionUtils.intersection(groupsFromFilters, memberQuery.getGroups()));
                    } else {
                        // Else, use only the groups from group filters
                        memberQuery.setGroups(groupsFromFilters);
                    }
                }
                memberQuery.setGroupFilters(null);
            }
        }
        if (!applyLoggedUserFilters(query)) {
            return Collections.emptyList();
        }
        query.setOrder(settingsService.getLocalSettings().getMemberResultDisplay());
        return elementDao.fullTextSearch(query);
    }

    public Calendar getFirstMemberActivationDate() {
        return elementDao.getFirstMemberActivationDate();
    }

    public IndexStatus getIndexStatus(final Class<? extends Element> entityType) {
        return elementDao.getIndexStatus(entityType);
    }

    public Member insertMember(final Member member, final boolean ignoreActivationMail, final boolean validatePassword) {
        final ActivationMail activationMail = ignoreActivationMail ? ActivationMail.IGNORE : ActivationMail.THREADED;
        return (Member) save(member, activationMail, WhenSaving.IMPORT, false, null);
    }

    public void invitePerson(final String email) {
        mailHandler.sendInvitation(LoggedUser.element(), email);
    }

    public Element load(final Long id, final Relationship... fetch) {
        final Element element = elementDao.load(id, fetch);
        checkAccessToMember(element);
        return element;
    }

    public Member loadByPrincipal(final PrincipalType principalType, String principal, final Relationship... fetch) {
        try {
            if (principal == null) {
                throw new NullPointerException();
            }
            final Principal principalEnum = principalType == null ? Principal.USER : principalType.getPrincipal();
            switch (principalEnum) {
                case USER:
                    final User user = loadUser(principal);
                    return (Member) fetchService.fetch(user.getElement(), fetch);
                case EMAIL:
                    return (Member) elementDao.loadByEmail(principal, fetch);
                case CUSTOM_FIELD:
                    final MemberCustomField customField = principalType.getCustomField();
                    if (StringUtils.isNotEmpty(customField.getPattern())) {
                        principal = StringHelper.removeMask(customField.getPattern(), principal, false);
                    }
                    return elementDao.loadByCustomField(customField, principal, fetch);
                case CARD:
                    // Use numbers only, avoid conflicts with formatting
                    final String cardNumber = StringHelper.onlyNumbers(principal);
                    final Card card = cardService.loadByNumber(new BigInteger(cardNumber), Card.Relationships.OWNER);
                    final Calendar expirationDate = DateHelper.truncateNextDay(card.getExpirationDate());
                    final boolean cardExpired = !Calendar.getInstance().getTime().before(expirationDate.getTime());
                    if (card.getStatus() != Card.Status.ACTIVE || cardExpired) {
                        throw new EntityNotFoundException("The card " + cardNumber + " is not active or has expired.");
                    }
                    return fetchService.fetch(card.getOwner(), fetch);
            }
            throw new EntityNotFoundException(Member.class);
        } catch (final EntityNotFoundException e) {
            throw e;
        } catch (final Exception e) {
            final EntityNotFoundException enfe = new EntityNotFoundException(Member.class);
            enfe.initCause(e);
            throw enfe;
        }
    }

    public OperatorUser loadOperatorUser(final Member member, final String operatorUsername, final Relationship... fetch) throws EntityNotFoundException {
        return userDao.loadOperator(member, operatorUsername, fetch);
    }

    public PendingMember loadPendingMember(final Long id, final Relationship... fetch) {
        final PendingMember pendingMember = pendingMemberDao.load(id, fetch);
        checkManagement(pendingMember);
        return pendingMember;
    }

    public PendingMember loadPendingMemberByKey(final String key, final Relationship... fetch) {
        return pendingMemberDao.loadByKey(key, fetch);
    }

    public User loadUser(final Long id, final Relationship... fetch) {
        final User user = userDao.load(id, fetch);
        checkAccessToMember(user.getElement());
        return user;
    }

    public User loadUser(final String username, final Relationship... fetch) {
        final User user = userDao.load(username, fetch);
        checkAccessToMember(user.getElement());
        return user;
    }

    public void optimizeIndex(final Class<? extends Element> entityType) {
        elementDao.optimizeIndex(entityType);
    }

    @SuppressWarnings("unchecked")
    public int processMembersExpirationForGroups(final Calendar time) {
        // Find on member groups...
        final GroupQuery query = new GroupQuery();
        query.setNatures(Group.Nature.MEMBER, Group.Nature.BROKER);
        int count = 0;
        final List<Group> groups = (List<Group>) groupService.search(query);
        final String message = messageResolver.message("changeGroup.member.expired");
        for (final Group group : groups) {
            final MemberGroup memberGroup = (MemberGroup) fetchService.fetch(group);
            final MemberGroupSettings memberSettings = memberGroup.getMemberSettings();
            final TimePeriod expireMembersAfter = memberSettings.getExpireMembersAfter();
            // ... those who expire members after a given time period ...
            if (expireMembersAfter == null || expireMembersAfter.getNumber() <= 0) {
                continue;
            }
            final Calendar limit = expireMembersAfter.remove(DateHelper.truncate(time));
            final List<Member> members = elementDao.listMembersRegisteredBeforeOnGroup(limit, memberGroup);
            final MemberGroup groupAfterExpiration = memberSettings.getGroupAfterExpiration();
            // ... then expire members on that group
            for (final Member member : members) {
                changeMemberGroup(member, groupAfterExpiration, message);
                count++;
            }
        }
        return count;
    }

    public RegisteredMember publicRegisterMember(final Member member, final String remoteAddress) {
        final MemberGroup group = fetchService.fetch(member.getMemberGroup());
        final List<? extends MemberGroup> possibleInitialGroups = groupService.getPossibleInitialGroups();
        if (!possibleInitialGroups.contains(group)) {
            throw new ValidationException();
        }
        return register(member, WhenSaving.PUBLIC, false, remoteAddress, null);
    }

    public void publicValidate(final Member member) throws ValidationException {
        validate(member, WhenSaving.PUBLIC, true);
    }

    public Member publicValidateRegistration(final String key) throws EntityNotFoundException, RegistrationAgreementNotAcceptedException {
        final PendingMember pendingMember = pendingMemberDao.loadByKey(key, PendingMember.Relationships.values());

        // Store the agreement data
        final RegistrationAgreement registrationAgreement = pendingMember.getRegistrationAgreement();
        final Calendar agreementDate = pendingMember.getRegistrationAgreementDate();
        final String remoteAddress = pendingMember.getRemoteAddress();

        // Only proceed if the agreement (if any) has already been accepted
        final MemberGroup group = pendingMember.getMemberGroup();
        if (group.getRegistrationAgreement() != null && !group.getRegistrationAgreement().equals(registrationAgreement)) {
            throw new RegistrationAgreementNotAcceptedException();
        }

        // Eagerly delete to avoid the username being reported as already used
        pendingMemberDao.delete(pendingMember.getId());

        // Translate the PendingMember into a Member
        Member member = new Member();
        PropertyHelper.copyProperties(pendingMember, member, "id", "memberGroup", "username", "password", "customValues");
        member.setGroup(pendingMember.getMemberGroup());
        final MemberUser user = new MemberUser();
        member.setUser(user);
        user.setSalt(pendingMember.getSalt());
        user.setUsername(pendingMember.getUsername());
        final String password = pendingMember.getPassword();
        if (StringUtils.isNotEmpty(password)) {
            user.setPassword(password);
            if (!pendingMember.isForceChangePassword()) {
                user.setPasswordDate(Calendar.getInstance());
            }
        }
        final Collection<MemberCustomFieldValue> customValues = new ArrayList<MemberCustomFieldValue>();
        for (final MemberCustomFieldValue current : pendingMember.getCustomValues()) {
            final MemberCustomFieldValue newValue = new MemberCustomFieldValue();
            newValue.setField(current.getField());
            newValue.setStringValue(current.getStringValue());
            newValue.setPossibleValue(current.getPossibleValue());
            newValue.setHidden(current.isHidden());
            customValues.add(newValue);
        }
        member.setCustomValues(customValues);
        member = (Member) save(member, ActivationMail.ONLINE, WhenSaving.EMAIL_VALIDATION, pendingMember.isForceChangePassword(), null);

        // Mark the agreement as accepted
        if (registrationAgreement != null) {
            final RegistrationAgreementLog log = new RegistrationAgreementLog();
            log.setMember(member);
            log.setRegistrationAgreement(registrationAgreement);
            log.setDate(agreementDate);
            log.setRemoteAddress(remoteAddress);
            registrationAgreementLogDao.insert(log);
        }

        return member;
    }

    public void purgeOldPublicMailValidations(final Calendar time) {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final TimePeriod timePeriod = localSettings.getDeletePendingRegistrationsAfter();
        if (timePeriod == null || timePeriod.getNumber() <= 0) {
            return;
        }
        pendingMemberDao.deleteBefore(timePeriod.remove(time));
    }

    public void rebuildIndex(final Class<? extends Element> entityType) {
        elementDao.rebuildIndex(entityType);
    }

    public Administrator registerAdmin(final Administrator admin, final boolean forceChangePassword) {
        if (!forceChangePassword) {
            admin.getUser().setPasswordDate(Calendar.getInstance());
        }
        return (Administrator) save(admin, ActivationMail.ONLINE, WhenSaving.ADMIN_BY_ADMIN, forceChangePassword, null);
    }

    public RegisteredMember registerMemberByAdmin(final Member member, final boolean forceChangePassword) {
        return register(member, WhenSaving.MEMBER_BY_ADMIN, forceChangePassword, null, null);
    }

    public RegisteredMember registerMemberByBroker(final Member member, final boolean forceChangePassword) {
        processBroker(member);
        if (!groupService.getPossibleInitialGroups().contains(member.getGroup())) {
            throw new ValidationException();
        }
        return register(member, WhenSaving.BY_BROKER, forceChangePassword, null, null);
    }

    public RegisteredMember registerMemberByWebService(ServiceClient client, final Member member, final String remoteAddress) {
        client = fetchService.fetch(client, ServiceClient.Relationships.MANAGE_GROUPS);
        final Set<MemberGroup> manageGroups = client.getManageGroups();
        if (manageGroups.isEmpty()) {
            throw new PermissionDeniedException();
        }
        MemberGroup group;
        try {
            group = (MemberGroup) fetchService.fetch(member.getGroup());
        } catch (final Exception e) {
            throw new EntityNotFoundException();
        }
        if (group == null) {
            group = manageGroups.iterator().next();
        }
        return register(member, WhenSaving.WEBSERVICE, false, remoteAddress, client);
    }

    public Operator registerOperator(final Operator operator) {
        final Member loggedMember = LoggedUser.element();
        operator.setMember(loggedMember);
        operator.getUser().setPasswordDate(Calendar.getInstance());
        return (Operator) save(operator, null, WhenSaving.OPERATOR, false, null);
    }

    public void removeAdmin(final Long id) {
        final Element element = load(id);
        if (!(element instanceof Administrator)) {
            throw new UnexpectedEntityException();
        }
        elementDao.delete(id);
        elementDao.removeFromIndex(element);
    }

    public void removeMember(final Long id) {
        final Element element = load(id, Element.Relationships.GROUP);
        if (!(element instanceof Member)) {
            throw new UnexpectedEntityException();
        }
        final Member member = (Member) element;
        if (member.getActivationDate() != null) {
            // Cannot permanently remove an active member
            throw new UnexpectedEntityException();
        }
        elementDao.delete(id);
        elementDao.removeFromIndex(member);
    }

    public void removeOperator(final Long id) {
        final Element element = load(id, Element.Relationships.GROUP);
        if (!(element instanceof Operator)) {
            throw new UnexpectedEntityException();
        }
        elementDao.delete(id);
        elementDao.removeFromIndex(element);
    }

    public int removePendingMembers(final Long... ids) {
        if (ids == null) {
            return 0;
        }
        for (final Long id : ids) {
            checkManagement(EntityHelper.reference(PendingMember.class, id));
        }
        return pendingMemberDao.delete(ids);
    }

    public boolean requiresKeywordsForSearch() {
        return LoggedUser.isValid() && !LoggedUser.isAdministrator() && forceKeywords;
    }

    public PendingMember resendEmail(final PendingMember pendingMember) throws MailSendingException {
        // Send the mail
        try {
            mailHandler.sendEmailValidation(pendingMember);
            pendingMember.setLastEmailDate(Calendar.getInstance());
            return pendingMemberDao.update(pendingMember);
        } finally {
            if (MailHandler.hasException()) {
                throw new MailSendingException("Email validation for: " + pendingMember.getName());
            }
        }
    }

    public List<? extends Element> search(final ElementQuery query) {
        if (!applyLoggedUserFilters(query)) {
            return Collections.emptyList();
        }
        query.fetch(Element.Relationships.USER);
        return elementDao.search(query);
    }

    public List<PendingMember> search(final PendingMemberQuery params) {
        Collection<MemberGroup> allowedGroups = null;
        if (LoggedUser.isValid()) {
            if (LoggedUser.isBroker()) {
                final Member loggedBroker = LoggedUser.element();
                params.setBroker(loggedBroker);

                final BrokerGroup group = LoggedUser.group();
                allowedGroups = fetchService.fetch(group, BrokerGroup.Relationships.POSSIBLE_INITIAL_GROUPS).getPossibleInitialGroups();
            } else {
                final AdminGroup group = LoggedUser.group();
                allowedGroups = fetchService.fetch(group, AdminGroup.Relationships.MANAGES_GROUPS).getManagesGroups();
            }
        }
        if (allowedGroups != null) {
            if (allowedGroups.isEmpty()) {
                // No allowed group
                return Collections.emptyList();
            }
            // Ensure only the allowed groups are returned
            final Collection<MemberGroup> groups = params.getGroups();
            if (CollectionUtils.isEmpty(groups)) {
                params.setGroups(allowedGroups);
            } else {
                for (final Iterator<MemberGroup> iterator = groups.iterator(); iterator.hasNext();) {
                    final MemberGroup memberGroup = iterator.next();
                    if (!allowedGroups.contains(memberGroup)) {
                        iterator.remove();
                    }
                }
            }
        }
        return pendingMemberDao.search(params);
    }

    public List<? extends Element> searchAtDate(final MemberQuery query, final Calendar date) {
        if (!applyLoggedUserFilters(query)) {
            return Collections.emptyList();
        }
        return elementDao.searchAtDate(query, date);
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setAdInterestService(final AdInterestService adInterestService) {
        this.adInterestService = adInterestService;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setBrokeringService(final BrokeringService brokeringService) {
        this.brokeringService = brokeringService;
    }

    public void setCardService(final CardService cardService) {
        this.cardService = cardService;
    }

    public void setCommissionService(final CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    public void setContactService(final ContactService contactService) {
        this.contactService = contactService;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setElementDao(final ElementDAO elementDAO) {
        elementDao = elementDAO;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setForceKeywords(final boolean forceKeywords) {
        this.forceKeywords = forceKeywords;
    }

    public void setGroupHistoryLogDao(final GroupHistoryLogDAO groupHistoryLogDao) {
        this.groupHistoryLogDao = groupHistoryLogDao;
    }

    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    public void setHashHandler(final HashHandler hashHandler) {
        this.hashHandler = hashHandler;
    }

    public void setInvoiceService(final InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    public void setLoanService(final LoanService loanService) {
        this.loanService = loanService;
    }

    public void setMailHandler(final MailHandler mailHandler) {
        this.mailHandler = mailHandler;
    }

    public void setMemberAccountHandler(final MemberAccountHandler memberAccountHandler) {
        this.memberAccountHandler = memberAccountHandler;
    }

    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void setNotificationPreferenceDao(final NotificationPreferenceDAO notificationPreferenceDao) {
        this.notificationPreferenceDao = notificationPreferenceDao;
    }

    public void setPaymentService(final PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setPendingMemberDao(final PendingMemberDAO pendingMemberDao) {
        this.pendingMemberDao = pendingMemberDao;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setPosService(final PosService posService) {
        this.posService = posService;
    }

    public void setPreferenceService(final PreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    public void setRegistrationAgreementAgreed(PendingMember pendingMember, final RegistrationAgreement registrationAgreement) {
        validateAgreement(pendingMember, registrationAgreement);
        pendingMember = fetchService.reload(pendingMember);
        pendingMember.setRegistrationAgreement(registrationAgreement);
        pendingMember.setRegistrationAgreementDate(Calendar.getInstance());
        pendingMemberDao.update(pendingMember);
    }

    public void setRegistrationAgreementLogDao(final RegistrationAgreementLogDAO registrationAgreementLogDao) {
        this.registrationAgreementLogDao = registrationAgreementLogDao;
    }

    public void setRemarkService(final RemarkService remarkService) {
        this.remarkService = remarkService;
    }

    public void setScheduledPaymentService(final ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setSkipOrder(final boolean skipOrder) {
        this.skipOrder = skipOrder;
    }

    public void setUserDao(final UserDAO userDAO) {
        userDao = userDAO;
    }

    public void setUsernameChangeLogDao(final UsernameChangeLogDAO usernameChangeLogDao) {
        this.usernameChangeLogDao = usernameChangeLogDao;
    }

    public boolean shallAcceptAgreement(Member member) {
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        final RegistrationAgreement registrationAgreement = member.getMemberGroup().getRegistrationAgreement();
        if (registrationAgreement == null) {
            return false;
        }
        final List<RegistrationAgreementLog> logs = registrationAgreementLogDao.listByMember(member);
        for (final RegistrationAgreementLog log : logs) {
            if (log.getRegistrationAgreement().equals(registrationAgreement)) {
                // Already accepted
                return false;
            }
        }
        // Not accepted yet
        return true;
    }

    public PendingMember update(PendingMember pendingMember) {
        if (pendingMember == null || pendingMember.isTransient()) {
            throw new UnexpectedEntityException();
        }
        checkManagement(pendingMember);
        validate(pendingMember);
        final Collection<MemberCustomFieldValue> customValues = pendingMember.getCustomValues();
        pendingMember = pendingMemberDao.update(pendingMember);
        pendingMember.setCustomValues(customValues);
        customFieldService.saveMemberValues(pendingMember);
        return pendingMember;
    }

    public void validate(final Element element, final WhenSaving when, final boolean manualPassword) throws ValidationException {
        validate(element, when, manualPassword, null);
    }

    public void validate(final PendingMember pendingMember) throws ValidationException {
        getValidator(pendingMember).validate(pendingMember);
    }

    private boolean applyLoggedUserFilters(final ElementQuery query) {
        if (LoggedUser.isValid()) {
            // Validate what's being searched
            final Element loggedElement = LoggedUser.element();
            if ((loggedElement instanceof Operator) && !(query instanceof MemberQuery)) {
                // An operator can only search members
                throw new PermissionDeniedException();
            }
            if (loggedElement instanceof Member) {
                if (query instanceof AdminQuery) {
                    // A member cannot search admins
                    throw new PermissionDeniedException();
                } else if (query instanceof OperatorQuery) {
                    ((OperatorQuery) query).setMember((Member) loggedElement);
                }
            }
            if ((loggedElement instanceof Administrator) && (query instanceof OperatorQuery)) {
                // An admin cannot search operators
                throw new PermissionDeniedException();
            }
            if (!(loggedElement instanceof Administrator)) {
                // Non-admins can only see enabled members
                query.setEnabled(true);
            }

            if (query instanceof MemberQuery) {
                if (LoggedUser.isAdministrator()) {
                    // The logged user is an admin
                    AdminGroup adminGroup = LoggedUser.group();
                    adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                    final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
                    if (CollectionUtils.isEmpty(managesGroups)) {
                        return false;
                    }
                    if (CollectionUtils.isEmpty(query.getGroups())) {
                        query.setGroups(managesGroups);
                    } else {
                        for (final Iterator<? extends Group> iter = query.getGroups().iterator(); iter.hasNext();) {
                            final Group group = iter.next();
                            if (!managesGroups.contains(group)) {
                                iter.remove();
                            }
                        }
                    }
                } else {
                    // If there's a member or operator logged on, ensure to constrain the groups he can view
                    MemberGroup group;
                    if (LoggedUser.isOperator()) {
                        final Operator operator = LoggedUser.element();
                        group = operator.getMember().getMemberGroup();
                    } else {
                        group = LoggedUser.group();
                    }
                    group = fetchService.fetch(group, MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS);
                    final Collection<MemberGroup> canViewProfileOfGroups = group.getCanViewProfileOfGroups();
                    if (CollectionUtils.isEmpty(canViewProfileOfGroups)) {
                        return false;
                    }
                    if (CollectionUtils.isEmpty(query.getGroups())) {
                        query.setGroups(canViewProfileOfGroups);
                    } else {
                        for (final Iterator<? extends Group> iter = query.getGroups().iterator(); iter.hasNext();) {
                            final Group currentGroup = iter.next();
                            if (!canViewProfileOfGroups.contains(currentGroup)) {
                                iter.remove();
                            }
                        }
                    }
                }
            }
        }
        if (query.getOrder() == null && !skipOrder) {
            query.setOrder(settingsService.getLocalSettings().getMemberResultDisplay());
        }
        return true;
    }

    private boolean applyLoggedUserFilters(final FullTextElementQuery query) {
        if (LoggedUser.isValid()) {
            // Validate what's being searched
            final Element loggedElement = LoggedUser.element();
            if ((loggedElement instanceof Operator) && !(query instanceof FullTextMemberQuery)) {
                // An operator can only search members
                throw new PermissionDeniedException();
            }
            if (loggedElement instanceof Member) {
                if (query instanceof FullTextAdminQuery) {
                    // A member cannot search admins
                    throw new PermissionDeniedException();
                } else if (query instanceof FullTextOperatorQuery) {
                    ((FullTextOperatorQuery) query).setMember((Member) loggedElement);
                }
            }
            if ((loggedElement instanceof Administrator) && (query instanceof FullTextOperatorQuery)) {
                // An admin cannot search operators
                throw new PermissionDeniedException();
            }
            if (!(loggedElement instanceof Administrator)) {
                // Non-admins can only see enabled members
                query.setEnabled(true);
            }

            if (query instanceof FullTextMemberQuery) {
                if (LoggedUser.isAdministrator()) {
                    // The logged user is an admin
                    AdminGroup adminGroup = LoggedUser.group();
                    adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                    final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
                    if (CollectionUtils.isEmpty(managesGroups)) {
                        return false;
                    }
                    if (CollectionUtils.isEmpty(query.getGroups())) {
                        query.setGroups(managesGroups);
                    } else {
                        for (final Iterator<? extends Group> iter = query.getGroups().iterator(); iter.hasNext();) {
                            final Group group = iter.next();
                            if (!managesGroups.contains(group)) {
                                iter.remove();
                            }
                        }
                    }
                } else {
                    // If there's a member or operator logged on, ensure to constrain the groups he can view
                    MemberGroup group;
                    if (LoggedUser.isOperator()) {
                        final Operator operator = LoggedUser.element();
                        group = operator.getMember().getMemberGroup();
                    } else {
                        group = LoggedUser.group();
                    }
                    group = fetchService.fetch(group, MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS);
                    final Collection<MemberGroup> canViewProfileOfGroups = group.getCanViewProfileOfGroups();
                    if (CollectionUtils.isEmpty(canViewProfileOfGroups)) {
                        return false;
                    }
                    if (CollectionUtils.isEmpty(query.getGroups())) {
                        query.setGroups(canViewProfileOfGroups);
                    } else {
                        for (final Iterator<? extends Group> iter = query.getGroups().iterator(); iter.hasNext();) {
                            final Group currentGroup = iter.next();
                            if (!canViewProfileOfGroups.contains(currentGroup)) {
                                iter.remove();
                            }
                        }
                    }
                }
            } else if (query instanceof FullTextOperatorQuery) {
                // Ensure the member will only see his operators
                final Member loggedMember = LoggedUser.element();
                final FullTextOperatorQuery oq = (FullTextOperatorQuery) query;
                oq.setMember(loggedMember);
            }
        }
        return true;
    }

    private void checkAccessToMember(final Element element) {
        if (element instanceof Member && LoggedUser.isValid()) {
            final Member member = fetchService.fetch((Member) element, Member.Relationships.BROKER);
            if ((LoggedUser.isMember() || LoggedUser.isOperator())) {
                final Member loggedMember = (Member) LoggedUser.accountOwner();
                if (!loggedMember.equals(member)) {
                    final MemberGroup group = fetchService.fetch(loggedMember.getMemberGroup(), MemberGroup.Relationships.CAN_VIEW_PROFILE_OF_GROUPS);
                    final Collection<MemberGroup> canViewMembersOfGroups = group.getCanViewProfileOfGroups();
                    if (!canViewMembersOfGroups.contains(element.getGroup()) && !loggedMember.equals(member.getBroker())) {
                        throw new PermissionDeniedException();
                    }
                }
            } else if (LoggedUser.isAdministrator()) {
                final AdminGroup group = LoggedUser.group();
                final Collection<MemberGroup> managesGroups = fetchService.fetch(group, AdminGroup.Relationships.MANAGES_GROUPS).getManagesGroups();
                if (!managesGroups.contains(member.getGroup())) {
                    throw new PermissionDeniedException();
                }
            }
        }
    }

    private void checkManagement(PendingMember pendingMember) {
        boolean valid = false;
        if (LoggedUser.isValid()) {
            pendingMember = fetchService.fetch(pendingMember);
            if (LoggedUser.isBroker()) {
                final Member loggedBroker = LoggedUser.element();
                valid = loggedBroker.equals(pendingMember.getBroker());
            } else {
                final AdminGroup group = LoggedUser.group();
                final Collection<MemberGroup> managesGroups = fetchService.reload(group, AdminGroup.Relationships.MANAGES_GROUPS).getManagesGroups();
                valid = managesGroups.contains(pendingMember.getMemberGroup());
            }
        }
        if (!valid) {
            throw new PermissionDeniedException();
        }
    }

    private void checkNewGroup(final Member member, final MemberGroup newGroup) {
        Collection<MemberAccountType> accountTypes = newGroup.getAccountTypes();
        if (accountTypes == null) {
            accountTypes = Collections.emptyList();
        }

        // Check if the member has any open loans
        final LoanQuery lQuery = new LoanQuery();
        lQuery.fetch(RelationshipHelper.nested(Loan.Relationships.TRANSFER, Transfer.Relationships.TYPE));
        lQuery.setStatus(Loan.Status.OPEN);
        lQuery.setMember(member);
        AccountType accType;
        for (final Loan loan : loanService.search(lQuery)) {
            final LoanParameters params = loan.getTransferType().getLoan();
            if (!accountTypes.contains(getFrom(params.getRepaymentType()))) {
                throw new MemberHasPendingLoansException(newGroup);
            }

            if (params.getType() == Loan.Type.WITH_INTEREST) {
                if ((accType = getFrom(params.getMonthlyInterestRepaymentType())) != null && !accountTypes.contains(accType)) {
                    throw new MemberHasPendingLoansException(newGroup);
                }
                if ((accType = getFrom(params.getGrantFeeRepaymentType())) != null && !accountTypes.contains(accType)) {
                    throw new MemberHasPendingLoansException(newGroup);
                }
                if ((accType = getFrom(params.getExpirationFeeRepaymentType())) != null && !accountTypes.contains(accType)) {
                    throw new MemberHasPendingLoansException(newGroup);
                }
                if ((accType = getFrom(params.getExpirationDailyInterestRepaymentType())) != null && !accountTypes.contains(accType)) {
                    throw new MemberHasPendingLoansException(newGroup);
                }
            }
        }

        // Check if the member has any open invoices
        final InvoiceQuery invoiceQuery = new InvoiceQuery();
        invoiceQuery.setDirection(InvoiceQuery.Direction.INCOMING);
        invoiceQuery.setOwner(member);
        invoiceQuery.setStatus(Invoice.Status.OPEN);
        for (final Invoice invoice : invoiceService.search(invoiceQuery)) {
            boolean found = false;
            final Iterator<MemberAccountType> accIt = accountTypes.iterator();
            while (!found && accIt.hasNext()) {
                accType = accIt.next();
                final Iterator<TransferType> ttIt = accType.getFromTransferTypes().iterator();
                while (!found && ttIt.hasNext()) {
                    final TransferType tt = ttIt.next();
                    if (tt.getTo().equals(invoice.getDestinationAccountType())) {
                        found = true;
                    }
                }
            }
            if (!found) {
                throw new MemberHasOpenInvoicesException(newGroup);
            }
        }

        invoiceQuery.setDirection(InvoiceQuery.Direction.OUTGOING);
        for (final Invoice invoice : invoiceService.search(invoiceQuery)) {
            if (!accountTypes.contains(invoice.getDestinationAccountType())) {
                throw new MemberHasOpenInvoicesException(newGroup);
            }
        }

        // Check the account balance
        final BigDecimal minimumPayment = paymentService.getMinimumPayment();
        for (final Account account : accountService.getAccounts(member)) {
            final BigDecimal balance = accountService.getBalance(new GetTransactionsDTO(account)).abs();
            if (!accountTypes.contains(account.getType()) && (balance.compareTo(minimumPayment) > 0)) {
                throw new MemberHasBalanceException(newGroup);
            }
        }

        // Check for scheduled payments
        if (scheduledPaymentService.hasUnrelatedPendingPayments(member, accountTypes)) {
            throw new MemberHasPendingScheduledPaymentsException(newGroup);
        }
    }

    private void createGroupHistoryLog(final Element element, final Group group, final Calendar start) {
        final GroupHistoryLog newGhl = new GroupHistoryLog();
        newGhl.setElement(element);
        newGhl.setGroup(group);
        newGhl.setPeriod(Period.begginingAt(start));
        groupHistoryLogDao.insert(newGhl);
    }

    /**
     * Create a remark for a group change
     */
    private void createGroupRemark(final Element member, final Group oldGroup, final Group newGroup, final String comments) {
        final Calendar now = Calendar.getInstance();
        final GroupRemark remark = new GroupRemark();
        if (LoggedUser.isValid()) {
            remark.setWriter(LoggedUser.element());
        }
        remark.setSubject(member);
        remark.setDate(now);
        remark.setOldGroup(oldGroup);
        remark.setNewGroup(newGroup);
        remark.setComments(comments);
        remarkService.save(remark);

        updateGroupHistoryLogs(member, newGroup, now);
    }

    /**
     * Creates a validator for the given group
     */
    private Validator createValidator(final Group group, final Element element, final WhenSaving when, final boolean manualPassword, final ServiceClient client) {
        final Element.Nature nature = group.getNature().getElementNature();
        final String baseName = nature.name().toLowerCase();
        return new DelegatingValidator(new DelegatingValidator.DelegateSource() {
            public Validator getValidator() {
                final AccessSettings accessSettings = settingsService.getAccessSettings();
                final LocalSettings localSettings = settingsService.getLocalSettings();
                final Validator validator = new Validator(baseName);
                validator.property("group").required();
                validator.property("name").required().maxLength(100);
                final boolean isMember = nature == Element.Nature.MEMBER;

                // Validate username
                if ((element.isTransient() && (!isMember || accessSettings.getUsernameGeneration() == UsernameGeneration.NONE)) || element.isPersistent()) {
                    final Property username = validator.property("username");
                    username.required();

                    // Checks that the username is not yet used
                    validator.general(new ExistingUsernameValidation());

                    final RangeConstraint usernameLength = accessSettings.getUsernameLength();
                    if (usernameLength != null) {
                        username.add(new LengthValidation(usernameLength)).regex(accessSettings.getUsernameRegex());
                    }
                }
                if (element.isTransient()) {
                    // When manual password or public registration, login password is always required
                    boolean loginPasswordRequired = manualPassword || when == WhenSaving.PUBLIC;
                    if (client != null) {
                        // For service clients, if the channel uses the login password, it is required as well
                        final Channel channel = client.getChannel();
                        if (channel.getCredentials() == Credentials.DEFAULT || channel.getCredentials() == Credentials.LOGIN_PASSWORD) {
                            loginPasswordRequired = true;
                        }
                    }
                    // Validate the password on insert
                    final Property password = validator.property("user.password").key("createMember.password");
                    if (loginPasswordRequired) {
                        password.required();
                    }
                    // We can only validate the password if it's not pre-hashed
                    if (!when.isPreHashed()) {
                        accessService.addLoginPasswordValidation(element, password);
                    }

                    // Validate the pin, if any
                    if (isMember) {
                        boolean pinRequired = false;
                        if (client != null) {
                            // For service clients, if the channel uses the login password, it is required as well
                            final Channel channel = client.getChannel();
                            if (channel.getCredentials() == Credentials.PIN) {
                                pinRequired = true;
                            }
                        }
                        final Property pin = validator.property("user.pin").key("channel.credentials.PIN");
                        if (pinRequired) {
                            pin.required();
                        }
                        if (!when.isPreHashed()) {
                            accessService.addPinValidation((Member) element, pin);
                        }
                    }
                }

                // Validate the email
                final Property email = validator.property("email");
                if (localSettings.isEmailRequired() && (nature != Element.Nature.OPERATOR)) { // Operator mail is never required
                    email.required();
                }
                email.add(EmailValidation.instance()).maxLength(100);
                if (localSettings.isEmailUnique()) {
                    email.add(new UniqueEmailValidation(element.getId()));
                }
                if (when == WhenSaving.PUBLIC) {
                    email.add(new PendingMemberEmailValidation(null));
                }

                // Custom fields
                validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
                    public Validator getValidator() {
                        switch (nature) {
                            case ADMIN:
                                return customFieldService.getAdminValueValidator((AdminGroup) group);
                            case MEMBER:
                                Member member = (Member) element;
                                MemberCustomField.Access access = null;
                                if (!LoggedUser.isValid()) {
                                    access = MemberCustomField.Access.REGISTRATION;
                                } else {
                                    member = fetchService.fetch(member, Member.Relationships.BROKER);
                                    final Element loggedElement = LoggedUser.element();
                                    if (loggedElement.equals(element)) {
                                        access = MemberCustomField.Access.MEMBER;
                                    } else if ((member == null && LoggedUser.isBroker()) || (member != null && loggedElement.equals(member.getBroker()))) {
                                        access = MemberCustomField.Access.BROKER;
                                    } else if (loggedElement instanceof Administrator) {
                                        access = MemberCustomField.Access.ADMIN;
                                    }
                                }
                                return customFieldService.getMemberValueValidator((MemberGroup) group, access);
                            case OPERATOR:
                                return customFieldService.getOperatorValueValidator(((OperatorGroup) group).getMember());
                        }
                        return null;
                    }
                }));
                return validator;
            }
        });
    }

    /**
     * Generate a member username
     */
    private String generateUsername(final int length) {
        String generated;
        boolean exists;
        do {
            // Generate a random number
            generated = RandomStringUtils.randomNumeric(length);
            if (generated.charAt(0) == '0') {
                // The first character cannot be zero
                generated = (new Random().nextInt(8) + 1) + generated.substring(1);
            }
            // Check if such username exists
            try {
                userDao.load(generated);
                exists = true;
            } catch (final EntityNotFoundException e) {
                exists = false;
            }
        } while (exists);
        return generated;
    }

    private AccountType getFrom(final TransferType tt) {
        return tt == null ? null : tt.getFrom();
    }

    private Validator getValidator(final PendingMember pendingMember) {
        final AccessSettings accessSettings = settingsService.getAccessSettings();
        final MemberGroup group = pendingMember.getMemberGroup();
        final Validator validator = new Validator("member");
        validator.property("name").required().maxLength(100);
        if (accessSettings.getUsernameGeneration() == UsernameGeneration.NONE) {
            validator.property("username").required().maxLength(64);
        }
        validator.property("email").required().maxLength(100).add(new PendingMemberEmailValidation(pendingMember));

        if (group != null) {
            validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
                public Validator getValidator() {
                    MemberCustomField.Access access = null;
                    if (!LoggedUser.isValid()) {
                        access = MemberCustomField.Access.REGISTRATION;
                    } else {
                        if (LoggedUser.element().equals(pendingMember.getBroker())) {
                            access = MemberCustomField.Access.BROKER;
                        } else {
                            access = MemberCustomField.Access.ADMIN;
                        }
                    }
                    return customFieldService.getMemberValueValidator(group, access);
                }
            }));
        }
        validator.general(new ExistingUsernameValidation());
        return validator;
    }

    /**
     * This method creates the accounts related to the member group, and marks those not related as inactive
     */
    @SuppressWarnings("unchecked")
    private void handleAccounts(Member member) {
        member = fetchService.fetch(member, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.ACCOUNT_SETTINGS));
        final MemberGroup group = member.getMemberGroup();
        final Collection<MemberGroupAccountSettings> accountSettings = group.getAccountSettings();
        final List<MemberAccount> accounts = (List<MemberAccount>) accountService.getAccounts(member, Account.Relationships.TYPE);
        // Mark as inactive the accounts no longer used
        for (final MemberAccount account : accounts) {
            if (!hasAccount(account, accountSettings)) {
                memberAccountHandler.deactivate(account, group.getStatus() == Group.Status.REMOVED);
            }
        }
        // Create the accounts the member does not yet has
        if (!CollectionUtils.isEmpty(accountSettings)) {
            for (final MemberGroupAccountSettings settings : accountSettings) {
                memberAccountHandler.activate(member, settings.getAccountType());
            }
        }
        // Activate members without accounts but in active groups
        if (member.getActivationDate() == null && group.isActive()) {
            member.setActivationDate(Calendar.getInstance());
        }
    }

    /**
     * Check if the specified account belongs to any of the accountSettings
     */
    private boolean hasAccount(final MemberAccount account, final Collection<MemberGroupAccountSettings> accountSettings) {
        for (final MemberGroupAccountSettings settings : accountSettings) {
            if (account.getType().equals(settings.getAccountType())) {
                return true;
            }
        }
        return false;
    }

    private void processBroker(final Member member) {
        // Set the broker
        member.setBroker((Member) LoggedUser.element());
    }

    private RegisteredMember register(Member member, final WhenSaving when, final boolean forceChangePassword, final String remoteAddress, final ServiceClient client) {
        final MemberGroup group = (MemberGroup) fetchService.fetch(member.getGroup());
        member.setGroup(group);

        RegisteredMember result;

        // Check the mail validation
        final MemberGroupSettings settings = group.getMemberSettings();
        final boolean validateEmail = settings.getEmailValidation() != null && settings.getEmailValidation().isApplied(when == WhenSaving.WEBSERVICE);
        if (validateEmail) {
            // It's enabled: Save a pending member
            final PendingMember pendingMember = new PendingMember();
            PropertyHelper.copyProperties(member, pendingMember);
            pendingMember.setCreationDate(Calendar.getInstance());
            pendingMember.setSalt(hashHandler.newSalt());
            pendingMember.setForceChangePassword(forceChangePassword);
            final User user = member.getUser();
            if (user != null) {
                pendingMember.setPassword(hashHandler.hash(pendingMember.getSalt(), user.getPassword()));
            }
            if (user instanceof MemberUser) {
                final MemberUser memberUser = (MemberUser) user;
                pendingMember.setPin(hashHandler.hash(pendingMember.getSalt(), memberUser.getPin()));
            }
            pendingMember.setValidationKey(RandomStringUtils.randomAlphanumeric(64));
            if (when == WhenSaving.PUBLIC) {
                // On public registrations, the license agreement has been accepted
                final RegistrationAgreement registrationAgreement = group.getRegistrationAgreement();
                if (registrationAgreement != null) {
                    pendingMember.setRegistrationAgreement(registrationAgreement);
                    pendingMember.setRegistrationAgreementDate(Calendar.getInstance());
                }
            }
            validate(pendingMember);
            result = pendingMemberDao.insert(pendingMember);

            customFieldService.saveMemberValues(result);

            resendEmail((PendingMember) result);
        } else {
            // Not enabled: save the member directly
            final ActivationMail activationMail = when == WhenSaving.WEBSERVICE ? ActivationMail.THREADED : ActivationMail.ONLINE;
            result = member = (Member) save(member, activationMail, when, forceChangePassword, client);

            // On the public registration, when there's an agreement, store it
            if (when == WhenSaving.PUBLIC) {
                member = fetchService.fetch(member, RelationshipHelper.nested(Element.Relationships.GROUP, MemberGroup.Relationships.REGISTRATION_AGREEMENT));
                final RegistrationAgreement registrationAgreement = member.getMemberGroup().getRegistrationAgreement();
                if (registrationAgreement != null) {
                    acceptAgreement(member, registrationAgreement, remoteAddress);
                }
            }
        }
        return result;
    }

    /**
     * Saves the given element
     * @param client TODO
     */
    private Element save(Element element, final ActivationMail activationMail, final WhenSaving when, final boolean forceChangePassword, final ServiceClient client) {
        validate(element, when, false, client);
        // Store the custom values on a saparate collection
        final Collection<?> values = (Collection<?>) PropertyHelper.get(element, "customValues");
        PropertyHelper.set(element, "customValues", null);

        final boolean isInsert = element.isTransient();
        if (isInsert) {
            // Check if we must generate a username for member
            final AccessSettings accessSettings = settingsService.getAccessSettings();
            final UsernameGeneration usernameGeneration = accessSettings.getUsernameGeneration();
            if (element instanceof Member && usernameGeneration != UsernameGeneration.NONE) {
                User user = element.getUser();
                // Assign a new user if none found
                if (user == null) {
                    user = new MemberUser();
                    element.setUser(user);
                }
                // Generate the username
                final String generated = generateUsername(accessSettings.getGeneratedUsernameLength());
                user.setUsername(generated);
            } else {
                // Check if the username already in use
                try {
                    final String username = element.getUsername();
                    if (element instanceof Operator) {
                        loadOperatorUser((Member) LoggedUser.element(), username);
                    } else {
                        loadUser(username);
                    }
                    throw new UsernameAlreadyInUseException(username);
                } catch (final EntityNotFoundException e) {
                    // Ok - not exists yet
                }
            }

            final User user = element.getUser();
            // Create a salt value
            if (user.getSalt() == null) {
                user.setSalt(hashHandler.newSalt());
            }
            // If a password exists, ensure it's hashed
            if (StringUtils.isNotEmpty(user.getPassword())) {
                // When the registration is not pre-hashed, hash the password
                if (!when.isPreHashed()) {
                    user.setPassword(hashHandler.hash(user.getSalt(), user.getPassword()));
                }
                // When not forcing to change (passwordDate == null), set a password date
                if (!forceChangePassword) {
                    user.setPasswordDate(Calendar.getInstance());
                }
            }
            // If a pin exists, ensure it's hashed
            if (user instanceof MemberUser) {
                final MemberUser memberUser = (MemberUser) user;
                if (StringUtils.isNotEmpty(memberUser.getPin()) && !when.isPreHashed()) {
                    memberUser.setPin(hashHandler.hash(user.getSalt(), memberUser.getPin()));
                }
            }

            // Insert
            Calendar creationDate = element.getCreationDate();
            if (creationDate == null) {
                creationDate = Calendar.getInstance();
                element.setCreationDate(creationDate);
            }
            element = elementDao.insert(element);
            if (element instanceof Member) {
                final Member member = (Member) element;

                // Handle the member accounts
                handleAccounts(member);

                // When the member has been activated, send the activation e-mail
                if (member.isActive()) {
                    sendActivationMailIfNeeded(activationMail, member);
                }

                // Fetch member group
                final MemberGroup group = fetchService.fetch(member.getMemberGroup(), MemberGroup.Relationships.CHANNELS, MemberGroup.Relationships.DEFAULT_MAIL_MESSAGES, MemberGroup.Relationships.SMS_MESSAGES, MemberGroup.Relationships.DEFAULT_SMS_MESSAGES);

                // Copy default channels access from to member
                final Collection<Channel> memberChannels = new ArrayList<Channel>(group.getDefaultChannels());
                member.setChannels(memberChannels);
                element = elementDao.update(member, false);

                // Insert the default notification preferences
                final List<NotificationPreference> preferences = new ArrayList<NotificationPreference>();
                final Collection<Type> defaultMailMessages = group.getDefaultMailMessages();
                final Collection<Type> smsMessages = group.getSmsMessages();
                final Collection<Type> defaultSmsMessages = group.getDefaultSmsMessages();
                for (final Type type : Type.values()) {
                    final NotificationPreference preference = new NotificationPreference();
                    preference.setEmail(defaultMailMessages.contains(type));
                    preference.setMessage(true);
                    if (smsMessages.contains(type) && defaultSmsMessages.contains(type)) {
                        preference.setSms(true);
                    }
                    preference.setMember(member);
                    preference.setType(type);
                    preferences.add(preference);
                }
                preferenceService.save(member, preferences);

                // Create the brokering when there is a broker set
                final Member broker = member.getBroker();
                if (broker != null) {
                    brokeringService.create(broker, member);
                }
            }

            // Create initial group history log
            createGroupHistoryLog(element, element.getGroup(), creationDate);
        } else {
            // Some properties cannot be saved using this method. Load the db state
            final Element saved = elementDao.load(element.getId());
            element.setCreationDate(saved.getCreationDate());
            element.setGroup(saved.getGroup());
            final User user = saved.getUser();
            if (element instanceof Member) {

                /*
                 * At this point if there is not a valid user, the update was invoked through an unrestricted web service client.
                 */
                final boolean isWebServiceInvocation = !LoggedUser.isValid();

                final Member member = (Member) element;

                // Check if the name has changed
                final String savedName = saved.getName();
                final String givenName = element.getName();
                if (!savedName.equals(givenName)) {
                    final boolean canChangeName = isWebServiceInvocation || permissionService.checkPermission("adminMembers", "changeName") || permissionService.checkPermission("brokerMembers", "changeName") || permissionService.checkPermission("memberProfile", "changeName");
                    if (!canChangeName) {
                        // No permissions. Ensure the name is not changed
                        member.setName(savedName);
                    }
                }

                // Check if the username has changed
                final String savedUsername = saved.getUsername();
                final String givenUsername = element.getUsername();
                boolean canChangeUsername;
                if (settingsService.getAccessSettings().getUsernameGeneration() == UsernameGeneration.NONE) {
                    canChangeUsername = isWebServiceInvocation || permissionService.checkPermission("adminMembers", "changeUsername") || permissionService.checkPermission("brokerMembers", "changeUsername") || permissionService.checkPermission("memberProfile", "changeUsername");
                } else {
                    // Even with permissions, when username is generated it cannot be changed
                    canChangeUsername = false;
                }
                if (!savedUsername.equals(givenUsername) && canChangeUsername) {
                    // Log the change
                    final UsernameChangeLog log = new UsernameChangeLog();
                    log.setDate(Calendar.getInstance());
                    log.setBy(LoggedUser.element());
                    log.setUser(user);
                    log.setPreviousUsername(savedUsername);
                    log.setNewUsername(givenUsername);
                    usernameChangeLogDao.insert(log);

                    // Save the username
                    user.setUsername(givenUsername);

                    // Set the owner name on each account
                    final List<? extends Account> accounts = accountService.getAccounts(member);
                    for (final Account account : accounts) {
                        account.setOwnerName(givenUsername);
                    }
                }
            } else if (element instanceof Operator) {
                if (LoggedUser.isMember()) {
                    // Save the username: a member always can change the operator's username
                    user.setUsername(element.getUsername());
                }
            }
            element.setUser(user);
            if (element instanceof Member) {
                final Member member = (Member) element;
                final Member savedMember = (Member) saved;
                member.setActivationDate(savedMember.getActivationDate());
                member.setBroker(savedMember.getBroker());
                member.setChannels(savedMember.getChannels());
            } else if (element instanceof Operator) {
                final Operator operator = (Operator) element;
                final Operator savedOperator = (Operator) saved;
                operator.setMember(savedOperator.getMember());
                if (!LoggedUser.isMember()) { // preserve the saved name: only the member can change the operator's name
                    operator.setName(savedOperator.getName());
                }
            }

            // Update
            element = elementDao.update(element);
        }

        // Save the custom fields
        PropertyHelper.set(element, "customValues", values);
        if (element instanceof Member) {
            customFieldService.saveMemberValues((Member) element);
        } else if (element instanceof Administrator) {
            customFieldService.saveAdminValues((Administrator) element);
        } else if (element instanceof Operator) {
            customFieldService.saveOperatorValues((Operator) element);
        }

        // Reindex the element
        elementDao.addToIndex(element);

        return element;
    }

    private void sendActivationMailIfNeeded(final ActivationMail activationMail, final Member member) {
        if (activationMail == ActivationMail.IGNORE || StringUtils.isEmpty(member.getEmail())) {
            return;
        }
        final MemberGroup group = member.getMemberGroup();
        final User user = member.getUser();
        // Check if the member is activated, and activation mail can be sent
        final boolean sendPasswordByEmail = group.getMemberSettings().isSendPasswordByEmail();
        String password = null;
        if (sendPasswordByEmail && StringUtils.isEmpty(user.getPassword())) {
            // Generate a password
            password = accessService.generatePassword(group);
            user.setPassword(hashHandler.hash(user.getSalt(), password));
            member.setUser(userDao.update(user));
            member.getMemberUser().setPasswordGenerated(true);
        }
        // Send activation mail
        mailHandler.sendActivation(activationMail == ActivationMail.THREADED, member, password);
    }

    /**
     * Updates end date of last group history log and create new group history log
     */
    private void updateGroupHistoryLogs(final Element element, final Group newGroup, final Calendar date) {
        // Update end date of last group history log
        final GroupHistoryLog lastGhl = groupHistoryLogDao.getLastGroupHistoryLog(element);
        if (lastGhl != null) {
            lastGhl.getPeriod().setEnd(date);
            groupHistoryLogDao.update(lastGhl);
        }

        // Create new group history log
        createGroupHistoryLog(element, newGroup, date);
    }

    @SuppressWarnings("unchecked")
    private void validate(final Element element, final WhenSaving when, final boolean manualPassword, final ServiceClient client) throws ValidationException {
        if (element.isTransient() && LoggedUser.isValid() && LoggedUser.isBroker() && (element instanceof Member)) {
            processBroker((Member) element);
        }

        Group group = element.getGroup();
        // We need a group in order to validate
        if (group == null || group.isTransient()) {
            if (element.isTransient()) {
                // Cannot validate a new member without a group
                throw new ValidationException("group", "member.group", new RequiredError());
            } else {
                // If no new group is supplied, just keep the old group
                final Element loaded = load(element.getId(), Element.Relationships.GROUP);
                group = loaded.getGroup();
                // Put the old group back on the element
                element.setGroup(group);
            }
        } else {
            group = fetchService.fetch(group);
        }
        if (element instanceof Member && element.isPersistent()) {
            // We must fill in the custom values by their default values if the member cannot edit it, so validation won't fail
            final Member member = (Member) element;
            final Collection<MemberCustomFieldValue> customValues = member.getCustomValues();
            List<MemberCustomField> fields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
            fields = CustomFieldHelper.onlyForGroup(fields, (MemberGroup) group);
            final Member current = (Member) load(element.getId(), Member.Relationships.CUSTOM_VALUES);
            final Collection<MemberCustomFieldValue> currentValues = current.getCustomValues();
            final Element loggedElement = LoggedUser.isValid() ? LoggedUser.element() : null;
            final boolean byOwner = loggedElement != null && loggedElement.equals(current);
            boolean byBroker = false;
            if (loggedElement != null && LoggedUser.isBroker()) {
                byBroker = loggedElement.equals(current.getBroker());
            }
            final Group loggedGroup = LoggedUser.isValid() ? LoggedUser.group() : null;
            for (final MemberCustomField field : fields) {
                if (loggedGroup != null && !field.getUpdateAccess().granted(loggedGroup, byOwner, byBroker, false)) {
                    final MemberCustomFieldValue currentValue = CustomFieldHelper.findByField(field, currentValues);
                    final MemberCustomFieldValue value = CustomFieldHelper.findByField(field, customValues);
                    if (value != null) {
                        customValues.remove(value);
                    }
                    if (currentValue != null) {
                        customValues.add(currentValue);
                    }
                }
            }
        }
        createValidator(group, element, when, manualPassword, client).validate(element);
    }

    private void validateAgreement(final RegisteredMember registeredMember, final RegistrationAgreement registrationAgreement) {
        final MemberGroup group = fetchService.fetch(registeredMember.getMemberGroup(), MemberGroup.Relationships.REGISTRATION_AGREEMENT);
        final RegistrationAgreement groupAgreement = group.getRegistrationAgreement();
        if (groupAgreement == null || !groupAgreement.equals(registrationAgreement)) {
            throw new ValidationException();
        }
    }

}
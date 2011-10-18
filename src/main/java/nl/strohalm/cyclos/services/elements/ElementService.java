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

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Administrator;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.ElementQuery;
import nl.strohalm.cyclos.entities.members.FullTextElementQuery;
import nl.strohalm.cyclos.entities.members.FullTextMemberQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.MemberQuery;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.PendingMember;
import nl.strohalm.cyclos.entities.members.PendingMemberQuery;
import nl.strohalm.cyclos.entities.members.RegisteredMember;
import nl.strohalm.cyclos.entities.members.RegistrationAgreement;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.exceptions.MailSendingException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.elements.exceptions.MemberHasBalanceException;
import nl.strohalm.cyclos.services.elements.exceptions.MemberHasOpenInvoicesException;
import nl.strohalm.cyclos.services.elements.exceptions.NoInitialGroupException;
import nl.strohalm.cyclos.services.elements.exceptions.RegistrationAgreementNotAcceptedException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for elements. Elements are used to control global member related operations. Members and Administrators descend from the same
 * Element entity.
 * @author rafael
 */
public interface ElementService extends Service {

    /**
     * Used for a member to accept to accept a registration agreement
     */
    void acceptAgreement(Member member, RegistrationAgreement registrationAgreement, String remoteAddress);

    /**
     * Adds to index entities which have not yet been indexed
     */
    void addMissingEntitiesToIndex();

    /**
     * Changes the group of all members returned by the query. For members that are already on the selected group, nothing is done
     */
    @AdminAction(@Permission(module = "adminMemberBulkActions", operation = "changeGroup"))
    @IgnoreMember
    BulkMemberActionResultVO bulkChangeMemberGroup(FullTextMemberQuery query, MemberGroup newGroup, String comments) throws ValidationException;

    /**
     * Changes the group of the specified administrator
     * @throws ValidationException When any of the arguments are null or empty, when the new group is the same as the old or when the admin is removed
     */
    @AdminAction(@Permission(module = "adminAdmins", operation = "changeGroup"))
    Administrator changeAdminGroup(Administrator admin, AdminGroup newGroup, String comments) throws ValidationException;

    /**
     * Change the profile of an administrator - not the logged one
     */
    @AdminAction(@Permission(module = "adminAdmins", operation = "changeProfile"))
    Administrator changeAdminProfile(Administrator admin);

    /**
     * Changes the group of the specified member. If the new group has status of removed:
     * <ul>
     * <li>Only let the group be changed if there are no accounts with balance and no open invoices</li>
     * <li>All contacts must be removed</li>
     * <li>If the member is a broker, all brokered members should have their broker changed to null, generating a remark with
     * messageSettings.brokerRemovedMessage as comments</li>
     * </ul>
     * @throws MemberHasBalanceException When the new group is removed and the member has balance on any of his accounts
     * @throws MemberHasOpenInvoicesException When the new group is removed and the member has open incoming invoices
     * @throws ValidationException When any of the arguments are null or empty, when the new group is the same as the old or when the member is
     * removed
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "changeGroup"))
    @PathToMember("")
    Member changeMemberGroup(Member member, MemberGroup newGroup, String comments) throws MemberHasBalanceException, MemberHasOpenInvoicesException, ValidationException;

    /**
     * An administrator changing a member's profile
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "changeProfile"))
    @PathToMember("")
    Member changeMemberProfileByAdmin(Member member);

    /**
     * An administrator changing a member's profile
     * @throws UnexpectedEntityException When the member is not brokered by the logged broker
     */
    @BrokerAction(@Permission(module = "brokerMembers", operation = "changeProfile"))
    @PathToMember("")
    Member changeMemberProfileByBroker(Member member) throws UnexpectedEntityException;

    /**
     * Updates a member profile by web services
     */
    Member changeMemberProfileByWebService(ServiceClient client, Member member);

    /**
     * Change the profile of the logged administrator
     * @throws UnexpectedEntityException When the given admin is not the logged user
     */
    @AdminAction
    @IgnoreMember
    Administrator changeMyProfile(Administrator admin) throws UnexpectedEntityException;

    /**
     * A member changing his own profile
     * @throws UnexpectedEntityException When the given member is not the logged user
     */
    @MemberAction
    @PathToMember("")
    Member changeMyProfile(Member member) throws UnexpectedEntityException;

    /**
     * An operator changing his own profile
     * @throws UnexpectedEntityException When the given operator is not the logged user
     */
    @OperatorAction
    @IgnoreMember
    Operator changeMyProfile(Operator operator) throws UnexpectedEntityException;

    /**
     * Changes the group of the specified operator
     * @throws ValidationException When any of the arguments are null or empty, when the new group is the same as the old or when the operator is
     * removed
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("member")
    Operator changeOperatorGroup(Operator operator, OperatorGroup newGroup, String comments) throws ValidationException;

    /**
     * A member changing his operator's profile
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("member")
    Operator changeOperatorProfile(Operator operator);

    /**
     * Marks all members in the given group as if they had accepted the given registration agreement
     */
    void createAgreementForAllMembers(RegistrationAgreement registrationAgreement, MemberGroup group);

    /**
     * Search the existing elements using a full text search, based on the given query parameters
     * @return a list of elements
     */
    List<? extends Element> fullTextSearch(FullTextElementQuery query);

    /**
     * Returns the date of the first member activation of the system
     */
    Calendar getFirstMemberActivationDate();

    /**
     * Returns the index sattus for the given entity
     */
    IndexStatus getIndexStatus(Class<? extends Element> entityType);

    /**
     * Creates a member, through internal procedures (like imports)
     */
    Member insertMember(Member member, boolean ignoreActivationMail, boolean validatePassword);

    /**
     * Sends an invitational e-mail to some person.
     */
    @AdminAction(@Permission(module = "basic", operation = "inviteMember"))
    @MemberAction(@Permission(module = "basic", operation = "inviteMember"))
    void invitePerson(String email);

    /**
     * Loads the element, fetching the specified relationships
     * @throws EntityNotFoundException When the given id does not exist
     */
    Element load(Long id, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads a member based on the given principal
     */
    Member loadByPrincipal(PrincipalType principalType, String principal, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads an operator user, fetching the specified relationships
     * @throws EntityNotFoundException When the given member and operatorUsername combination does not exist
     */
    OperatorUser loadOperatorUser(Member member, String operatorUsername, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads a PendingMember by id
     */
    PendingMember loadPendingMember(Long id, Relationship... fetch);

    /**
     * Loads a PendingMember given the activation key
     */
    PendingMember loadPendingMemberByKey(String key, Relationship... fetch);

    /**
     * Loads the user, fetching the specified relationships
     * @throws EntityNotFoundException When the given id does not exist
     */
    User loadUser(Long id, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads the user by username, fetching the specified relationships May only return users of type AdminUser or MemberUser. Operators have the
     * {@link #loadOperatorUser(Member, String, Relationship[])}
     * @throws EntityNotFoundException When the given username does not exist
     */
    User loadUser(String username, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Optimizes the full-text indexes for the given entity type
     */
    void optimizeIndex(Class<? extends Element> entityType);

    /**
     * Changes the group for all members which has stayed for more than a given time period on a group
     */
    int processMembersExpirationForGroups(Calendar time);

    /**
     * Public member registration
     * @throws NoInitialGroupException When there are no groups which are marked as initial
     */
    RegisteredMember publicRegisterMember(Member member, String remoteAddress) throws NoInitialGroupException;

    /**
     * On public member creation, first sets default group of member and validates the specified element
     * @throws ValidationException if validation fails.
     */
    void publicValidate(Member member) throws ValidationException;

    /**
     * Confirms the validation key
     * @throws EntityNotFoundException When there is no pending validation for the given key
     * @throws RegistrationAgreementNotAcceptedException When the user should accept an agreement before validating the registration
     */
    Member publicValidateRegistration(String key) throws EntityNotFoundException, RegistrationAgreementNotAcceptedException;

    /**
     * Purges old pending public registrations (whose member haven't activated them after some time)
     */
    void purgeOldPublicMailValidations(Calendar time);

    /**
     * Rebuild the index for full text searches for the given entity type
     */
    void rebuildIndex(Class<? extends Element> entityType);

    /**
     * Register a new administrator
     */
    @AdminAction(@Permission(module = "adminAdmins", operation = "register"))
    @PathToMember("")
    Administrator registerAdmin(Administrator admin, boolean forceChangePassword);

    /**
     * An administrator registering a new member
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "register"))
    @PathToMember("")
    RegisteredMember registerMemberByAdmin(Member member, boolean forceChangePassword);

    /**
     * A broker registering a new member
     * @throws NoInitialGroupException When the broker group settings don't specify an initial member group
     */
    @BrokerAction(@Permission(module = "brokerMembers", operation = "register"))
    @IgnoreMember
    RegisteredMember registerMemberByBroker(Member member, boolean forceChangePassword) throws NoInitialGroupException;

    /**
     * Register a member by Web Services
     */
    RegisteredMember registerMemberByWebService(ServiceClient client, Member member, String remoteAddress);

    /**
     * Used by a member to register an operator
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @IgnoreMember
    Operator registerOperator(Operator operator);

    /**
     * Removes the specified administrator (only if he didn't write any remarks).
     * @throws UnexpectedEntityException When the given id is not of an admin
     */
    @AdminAction(@Permission(module = "adminAdmins", operation = "remove"))
    void removeAdmin(Long id) throws UnexpectedEntityException;

    /**
     * Removes the specified member (only if he/she is not active)
     * @throws UnexpectedEntityException When the given id is not of a member, or is an active member
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "remove"))
    @RelatedEntity(Member.class)
    @PathToMember("")
    void removeMember(Long id) throws UnexpectedEntityException;

    /**
     * Removes the specified operator
     * @throws UnexpectedEntityException When the given id is not of an operator
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @RelatedEntity(Operator.class)
    @PathToMember("member")
    void removeOperator(Long id) throws UnexpectedEntityException;

    /**
     * Removes the given pending members
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "managePending"))
    @BrokerAction(@Permission(module = "brokerMembers", operation = "managePending"))
    @IgnoreMember
    int removePendingMembers(Long... ids);

    /**
     * Returns whether the current user is forced to use keywords for searching members
     */
    boolean requiresKeywordsForSearch();

    /**
     * Re-sends the activation e-mail
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "managePending"))
    @BrokerAction(@Permission(module = "brokerMembers", operation = "managePending"))
    @IgnoreMember
    PendingMember resendEmail(PendingMember pendingMember) throws MailSendingException;

    /**
     * Search the existing elements based on the ElementQuery object
     * @return a list of elements
     */
    List<? extends Element> search(ElementQuery query);

    /**
     * Searches for pending members
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "managePending"))
    @BrokerAction(@Permission(module = "brokerMembers", operation = "managePending"))
    @IgnoreMember
    List<PendingMember> search(PendingMemberQuery params);

    /**
     * Search the existing elements based on the ElementQuery object and a date
     * @return a list of elements
     */
    List<? extends Element> searchAtDate(MemberQuery query, Calendar date);

    /**
     * Updates the PendingMember, setting the given agreement as agreed
     */
    @DontEnforcePermission(traceable = true, value = "There is no permission to check")
    void setRegistrationAgreementAgreed(PendingMember pendingMember, RegistrationAgreement registrationAgreement);

    /**
     * Returns if there is a pending agreement to be accepted
     */
    @DontEnforcePermission(traceable = false, value = "There is no permission to check")
    boolean shallAcceptAgreement(Member member);

    /**
     * Updates the pending member
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "managePending"))
    @BrokerAction(@Permission(module = "brokerMembers", operation = "managePending"))
    @IgnoreMember
    PendingMember update(PendingMember pendingMember);

    /**
     * Validates the specified element
     */
    void validate(Element element, WhenSaving when, boolean manualPassword) throws ValidationException;

    /**
     * Validates the given PendingMember
     */
    void validate(PendingMember pendingMember) throws ValidationException;
}
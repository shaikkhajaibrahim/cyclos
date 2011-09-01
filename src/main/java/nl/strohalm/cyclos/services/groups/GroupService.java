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

import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.MemberAccountType;
import nl.strohalm.cyclos.entities.accounts.MemberGroupAccountSettings;
import nl.strohalm.cyclos.entities.accounts.cards.CardType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.RegistrationAgreement;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.sms.ISmsContext;
import nl.strohalm.cyclos.services.sms.exceptions.SmsContextInitializationException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for groups. The service is use to control group operations like: list, validate, remove, check permissions etc.
 * @author rafael
 * @author luis
 */
public interface GroupService extends Service {
    /**
     * Returns a list with all registration agreements to be showed in the group edition
     */
    @AdminAction( {
            @Permission(module = "systemGroups", operation = "manageAdmin"),
            @Permission(module = "systemGroups", operation = "manageBroker"),
            @Permission(module = "systemGroups", operation = "manageMember") })
    List<RegistrationAgreement> agreementsForGroupEdition();

    /**
     * Returns a list with all card types to be showed in the group edition
     */
    @AdminAction( {
            @Permission(module = "systemGroups", operation = "manageAdmin"),
            @Permission(module = "systemGroups", operation = "manageBroker"),
            @Permission(module = "systemGroups", operation = "manageMember") })
    List<CardType> cardTypesForGroupEdition();

    /**
     * Check permission of the group to use the transfer type
     */
    boolean checkPermission(Group group, TransferType transferType);

    /**
     * Returns a list with all external channels to be showed in the (member) group edition
     */
    @AdminAction( { @Permission(module = "systemGroups", operation = "manageMember") })
    List<Channel> externalChannelsForMemberGroupEdition();

    /**
     * Finds a group by it's login page name
     */
    Group findByLoginPageName(String loginPageName);

    /**
     * Lists the possible initial member groups
     */
    List<? extends MemberGroup> getPossibleInitialGroups();

    /**
     * Lists the possible new groups for elements on the given group, ordering by nature (admin / member then broker) and name. If the group is
     * removed, it can't be changed, so, the list will contain only it. Otherwise, returns all other groups, according to the group nature.
     */
    List<? extends Group> getPossibleNewGroups(Group currentGroup);

    /**
     * Returns the sms context defined for the specified group
     */
    ISmsContext getSmsContext(MemberGroup group) throws SmsContextInitializationException;

    /**
     * Checks if there is at least one group requiring special characters on the password
     */
    boolean hasGroupsWhichRequiresSpecialOnPassword();

    /**
     * Checks whether there is at least one member / broker group which enforces letters or special characters on the password
     */
    boolean hasMemberGroupsWhichEnforcesCharactersOnPassword();

    /**
     * Inserts an instance of group settings
     * @throws UnexpectedEntityException When the account is already related to the group
     */
    @AdminAction(@Permission(module = "adminMemberGroups", operation = "manageAccountSettings"))
    @IgnoreMember
    MemberGroupAccountSettings insertAccountSettings(MemberGroupAccountSettings settings) throws UnexpectedEntityException;

    /**
     * Inserts the specified admin group
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageAdmin"))
    AdminGroup insertAdmin(AdminGroup group, AdminGroup baseGroup);

    /**
     * Inserts the specified broker group
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageBroker"))
    BrokerGroup insertBroker(BrokerGroup group, BrokerGroup baseGroup);

    /**
     * Inserts the specified member group
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageMember"))
    MemberGroup insertMember(MemberGroup group, MemberGroup baseGroup);

    /**
     * Inserts the specified operator group (by member)
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("member")
    OperatorGroup insertOperator(OperatorGroup group, OperatorGroup baseGroup);

    /**
     * Loads the element, fetching the specified relationships
     * @return The element loaded
     */
    Group load(Long id, Relationship... fetch);

    /**
     * Loads an instance of group settings
     */
    MemberGroupAccountSettings loadAccountSettings(long groupId, long accountTypeId, Relationship... fetch);

    /**
     * Removes a relationship between a member group and a member accountType. All accounts of group members should be marked as inactive.
     */
    @AdminAction(@Permission(module = "adminMemberGroups", operation = "manageAccountSettings"))
    @IgnoreMember
    void removeAccountTypeRelationship(MemberGroup group, MemberAccountType type);

    /**
     * Removes the specified admin group
     * @throws EntityNotFoundException The specified group does not exists
     * @throws UnexpectedEntityException The specified group is not an AdminGroup
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageAdmin"))
    void removeAdmin(Long id) throws EntityNotFoundException, UnexpectedEntityException;

    /**
     * Removes the specified broker group
     * @throws EntityNotFoundException The specified group does not exists
     * @throws UnexpectedEntityException The specified group is not an BrokerGroup
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageBroker"))
    void removeBroker(Long id) throws EntityNotFoundException, UnexpectedEntityException;

    /**
     * Removes the specified member group
     * @throws EntityNotFoundException The specified group does not exists
     * @throws UnexpectedEntityException The specified group is not a MemberGroup
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageMember"))
    void removeMember(Long id) throws EntityNotFoundException, UnexpectedEntityException;

    /**
     * Removes the specified operator group (by member)
     * @throws EntityNotFoundException The specified group does not exists
     * @throws UnexpectedEntityException The specified group is not a OperatorGroup
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @RelatedEntity(OperatorGroup.class)
    @PathToMember("member")
    void removeOperator(Long id) throws EntityNotFoundException, UnexpectedEntityException;

    /**
     * Searches for groups that matches the give query
     */
    List<? extends Group> search(GroupQuery query);

    /**
     * Set the admin group permissions
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageAdmin"))
    AdminGroup setAdminPermissions(AdminGroupPermissionsDTO dto);

    /**
     * Set the brokergroup permissions
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageBroker"))
    BrokerGroup setBrokerPermissions(BrokerGroupPermissionsDTO dto);

    /**
     * Set the member group permissions
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageMember"))
    MemberGroup setMemberPermissions(MemberGroupPermissionsDTO<MemberGroup> dto);

    /**
     * Set the operator group permissions
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("group.member")
    OperatorGroup setOperatorPermissions(OperatorGroupPermissionsDTO dto);

    /**
     * Updates an instance of group settings
     */
    @AdminAction(@Permission(module = "adminMemberGroups", operation = "manageAccountSettings"))
    @IgnoreMember
    MemberGroupAccountSettings updateAccountSettings(MemberGroupAccountSettings settings, boolean updateAccountLimits);

    /**
     * Saves the specified admin group
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageAdmin"))
    AdminGroup updateAdmin(AdminGroup group);

    /**
     * Saves the specified broker group
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageBroker"))
    BrokerGroup updateBroker(BrokerGroup group);

    /**
     * Saves the specified member group
     */
    @AdminAction(@Permission(module = "systemGroups", operation = "manageMember"))
    MemberGroup updateMember(MemberGroup group, boolean forceMembersToAcceptAgreement);

    /**
     * Saves the specified operator group (by member)
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("member")
    OperatorGroup updateOperator(OperatorGroup group);

    /**
     * Check if the member group have access to a channel that uses pin
     */
    boolean usesPin(MemberGroup group);

    /**
     * Validates the group settings
     */
    void validate(MemberGroupAccountSettings settings);

    /**
     * Validates the specified admin group
     * @throws ValidationException if validation fails.
     */
    void validateAdmin(AdminGroup group) throws ValidationException;

    /**
     * Validates the specified broker group
     * @throws ValidationException if validation fails.
     */
    void validateBroker(BrokerGroup group) throws ValidationException;

    /**
     * Validates the group on insert operations
     */
    void validateInsert(Group group);

    /**
     * Validates the specified member group
     * @throws ValidationException if validation fails.
     */
    void validateMember(MemberGroup group) throws ValidationException;

    /**
     * Validates the specified operator group
     * @throws ValidationException if validation fails.
     */
    void validateOperator(OperatorGroup group) throws ValidationException;

}
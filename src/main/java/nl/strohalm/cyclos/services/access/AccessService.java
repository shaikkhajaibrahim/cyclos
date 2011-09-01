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
package nl.strohalm.cyclos.services.access;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.CredentialsAlreadyUsedException;
import nl.strohalm.cyclos.services.access.exceptions.InactiveMemberException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCardException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;
import nl.strohalm.cyclos.services.access.exceptions.SessionAlreadyInUseException;
import nl.strohalm.cyclos.services.access.exceptions.UserNotFoundException;
import nl.strohalm.cyclos.services.elements.ResetTransactionPasswordDTO;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator.Property;

/**
 * Service interface for login/logout and related operations such as searches for login, connected users, login
 * @author luis
 */
public interface AccessService extends Service {

    /**
     * It checks if the logged user can change the password of the specified user.
     * @throws PermissionDeniedException if the user doesn't have permissions
     */
    @DontEnforcePermission(traceable = false, value = "The implementation of this method must carry out with the permissions control")
    public void canChangePassword(User user);

    /**
     * Notifies the system of a permission denied exception. The system keeps track of how many have arisen and blocks the user if it detects that
     * there have been too many since the last login.
     * @return true if the user's login has been blocked.
     */
    public boolean notifyPermissionDeniedException();

    /**
     * Adds the login password validation to the given property
     */
    void addLoginPasswordValidation(Element element, Property property);

    /**
     * Appends a validation for pin on the given property
     */
    void addPinValidation(Member member, Property pin);

    /**
     * Changes the password of the given admin user
     */
    @AdminAction(@Permission(module = "adminAdminAccess", operation = "changePassword"))
    AdminUser changeAdminPassword(ChangeLoginPasswordDTO params);

    /**
     * Change the channels that the member have access to
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "changeChannelsAccess"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "changeChannelsAccess"))
    @PathToMember("")
    Member changeMemberChannelsAccess(final Member member, final Collection<Channel> channels, boolean verifySmsChannel);

    /**
     * Changes a member credential by web services
     */
    void changeMemberCredentialsByWebService(MemberUser memberUser, ServiceClient client, String newCredentials) throws CredentialsAlreadyUsedException;

    /**
     * Changes the password of the given member user
     * @throws CredentialsAlreadyUsedException When the given new password was already used for that user in past
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "changePassword"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "changePassword"))
    @PathToMember("user.element")
    MemberUser changeMemberPassword(ChangeLoginPasswordDTO params) throws CredentialsAlreadyUsedException;

    /**
     * Changes the pin of the given member user
     * @throws CredentialsAlreadyUsedException When the given new password was already used for that user in past
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "changePin"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "changePin"))
    @PathToMember("user.element")
    MemberUser changeMemberPin(ChangePinDTO params) throws CredentialsAlreadyUsedException;

    /**
     * Change the channels that the logged member have access to
     */
    @MemberAction
    @IgnoreMember
    Member changeMyChannelsAccess(final Collection<Channel> channels, boolean verifySmsChannel);

    /**
     * Changes the password of the logged user
     * @throws InvalidCredentialsException When the current password is invalid
     * @throws BlockedCredentialsException When the old password was invalid a few times, it becomes blocked
     * @throws CredentialsAlreadyUsedException When the given new password was already used for that user in past
     */
    @DontEnforcePermission(traceable = true)
    User changeMyPassword(ChangeLoginPasswordDTO params, String remoteAddress) throws InvalidCredentialsException, BlockedCredentialsException, CredentialsAlreadyUsedException;

    /**
     * Changes the pin of the logged user
     * @throws InvalidCredentialsException When the given pin is invalid
     * @throws BlockedCredentialsException When the transaction password was invalid a few times, it becomes blocked
     * @throws CredentialsAlreadyUsedException When the given new password was already used for that user in past
     */
    @MemberAction
    @IgnoreMember
    MemberUser changeMyPin(ChangePinDTO params, final String remoteAddress) throws InvalidCredentialsException, BlockedCredentialsException, CredentialsAlreadyUsedException;

    /**
     * Changes the password of the given operator user
     * @throws CredentialsAlreadyUsedException When the given new password was already used for that user in past
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("user.element.member")
    OperatorUser changeOperatorPassword(ChangeLoginPasswordDTO params) throws CredentialsAlreadyUsedException;

    /**
     * Checks for credentials for the given member, in the given channel
     */
    void checkCredentials(Channel channel, MemberUser user, String credentials, String remoteAddress, Member relatedMember) throws InvalidCredentialsException, BlockedCredentialsException, InvalidCardException;

    /**
     * Checks for credentials for the given principal, in the given channel
     */
    MemberUser checkCredentials(String channel, PrincipalType principalType, String principal, String credentials, String remoteAddress, Member relatedMember) throws UserNotFoundException, InvalidCardException, InvalidCredentialsException, BlockedCredentialsException, InvalidCardException;

    /**
     * Checks the password for the given user
     * @throws UserNotFoundException Invalid username
     * @throws InvalidCredentialsException Invalid password
     * @throws BlockedCredentialsException The user is blocked by exceding wrong login attempts
     */
    User checkPassword(String member, String username, String password, String remoteAddress) throws UserNotFoundException, InvalidCredentialsException, BlockedCredentialsException;

    /**
     * Checks whether the given transaction password is valid or not for the logged user. When the number of wrong tries is greater that the element's
     * group basicSettings.maxTransactionPasswordWrongTries, the transaction password must be blocked and a member alert must be generated
     * @throws InvalidCredentialsException Wrong transaction password
     * @throws BlockedCredentialsException The user has missed the password too many times
     */
    User checkTransactionPassword(String memberUsername, String username, String transactionPassword, String remoteAddress) throws UserNotFoundException, InvalidCredentialsException, BlockedCredentialsException;

    /**
     * Disconnects all users, except the one currently logged-in
     */
    @AdminAction(@Permission(module = "systemTasks", operation = "onlineState"))
    void disconnectAllButLogged();

    /**
     * Disconnects a logged admin. When the session is used, disconnects that session. Otherwise, all sessions
     * @throws NotConnectedException When there's no admin connected with the given session id
     */
    @AdminAction(@Permission(module = "adminAdminAccess", operation = "disconnect"))
    AdminUser disconnectLoggedAdmin(AdminUser user, String sessionId) throws NotConnectedException;

    /**
     * Disconnects a logged member. When the session is used, disconnects that session. Otherwise, all sessions
     * @throws NotConnectedException When there's no member connected with the given session id
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "disconnect"))
    @PathToMember("element")
    MemberUser disconnectLoggedMember(MemberUser user, String sessionId) throws NotConnectedException;

    /**
     * Disconnects a operator. When the session is used, disconnects that session. Otherwise, all sessions
     * @throws NotConnectedException When there's no operator connected with the given session id
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "disconnectOperator"))
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("element.member")
    OperatorUser disconnectLoggedOperator(OperatorUser user, String sessionId) throws NotConnectedException;

    /**
     * Generates a new password, according to the given group settings
     */
    String generatePassword(Group group);

    /**
     * Generates a transaction password for the logged user
     * @throws UnexpectedEntityException When the logged user's group doesn't uses transaction password
     * @return The plain generated transaction password
     */
    String generateTransactionPassword() throws UnexpectedEntityException;

    /**
     * Returns the user logged in the given session
     * @param sessionId Id of the session
     * @return The user logged in
     * @throws NotConnectedException No user logged under this session id
     */
    @DontEnforcePermission(traceable = false)
    User getLoggedUser(String sessionId) throws NotConnectedException;

    /**
     * Check if the login password of a given member has expired
     */
    boolean hasPasswordExpired(User user);

    /**
     * Returns whether the security code for the given card is blocked
     */
    boolean isCardSecurityCodeBlocked(BigInteger cardNumber);

    /**
     * Check if the channel is enabled for the member
     */
    boolean isChannelEnabledForMember(String channelInternalName, Member member);

    /**
     * Returns the session id for the given user if it is logged
     * @param user The user
     * @return The session id if the user logged in, null otherwise
     * @throws NotConnectedException The given user is not logged in
     */
    boolean isLoggedIn(User user) throws NotConnectedException;

    /**
     * Checks if the given user's login is blocked. The user's login could be blocked because he missed several times the password or he got too many
     * access denied exceptions.
     */
    boolean isLoginBlocked(User user);

    /**
     * Returns whether the given credential is obvious
     */
    boolean isObviousCredential(final Element element, final String credential);

    /**
     * Checks whether the given member's pin is blocked for exceeding wrong tries
     */
    boolean isPinBlocked(MemberUser user);

    /**
     * Lists the connected operators for the logged member
     */
    @MemberAction
    @IgnoreMember
    List<UserLoginDTO> listConnectedOperators();

    /**
     * Lists the connected users, according to the given natures
     */
    List<UserLoginDTO> listConnectedUsers(Collection<Group.Nature> natures);

    /**
     * Logs the specified user in the system
     * @param user The user to login
     * @param password The MD5 hashed password
     * @param remoteAddress The IP address for the requesting login
     * @param sessionId The session id for the user
     * @return the user logged in
     * @throws UserNotFoundException Invalid username
     * @throws InvalidCredentialsException Invalid password
     * @throws BlockedCredentialsException When the user is blocked by reaching the max login tries
     * @throws SessionAlreadyInUseException The given session id is already in use
     */
    User login(User user, String password, String channel, String remoteAddress, String sessionId) throws UserNotFoundException, InvalidCredentialsException, BlockedCredentialsException, SessionAlreadyInUseException;

    /**
     * Logs out of the system the user corresponding to the given <br>
     * session id and removes it from the logged users list
     * @param sessionId Id of the session to be logged out
     */
    User logout(String sessionId);

    /**
     * Immediately reenables an Admin to login after wrong password tries
     */
    @AdminAction(@Permission(module = "adminAdminAccess", operation = "enableLogin"))
    void reenableAdminLogin(AdminUser user);

    /**
     * Immediately reenables a Member to login after wrong password tries
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "enableLogin"))
    @PathToMember("element")
    void reenableMemberLogin(MemberUser user);

    /**
     * Immediately reenables an Operator to login after wrong password tries
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("element.member")
    void reenableOperatorLogin(OperatorUser user);

    /**
     * Resets the transaction password of the specified admin user
     */
    @AdminAction(@Permission(module = "adminAdminAccess", operation = "transactionPassword"))
    AdminUser resetAdminTransactionPassword(ResetTransactionPasswordDTO dto);

    /**
     * Resets the transaction password of the specified member user
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "transactionPassword"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "transactionPassword"))
    @PathToMember("user.element")
    MemberUser resetMemberTransactionPassword(ResetTransactionPasswordDTO dto);

    /**
     * Resets the transaction password of the specified operator user
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("user.element.member")
    OperatorUser resetOperatorTransactionPassword(ResetTransactionPasswordDTO dto);

    /**
     * Reset an user's password, sending it by mail
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "resetPassword"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "resetPassword"))
    @PathToMember("element")
    void resetPassword(MemberUser user);

    /**
     * Unblocks the security code of given card
     */
    @AdminAction(@Permission(module = "adminMemberCards", operation = "unblockSecurityCode"))
    @BrokerAction(@Permission(module = "brokerCards", operation = "unblockSecurityCode"))
    @PathToMember("owner")
    void unblockCardSecurityCode(BigInteger cardNumber);

    /**
     * Unblocks the own member's pin
     */
    @MemberAction(@Permission(module = "memberAccess", operation = "unblockPin"))
    @IgnoreMember
    void unblockMyPin();

    /**
     * Unblocks the given member's pin
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "unblockPin"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "unblockPin"))
    @PathToMember("element")
    void unblockPin(MemberUser user);

    /**
     * Updates all logged user's of the given group to have the group reference set to exactly the given reference. This avoids problems where some
     * settings are not immediately visible for logged users
     */
    void updateGroupReference(Group group);

    /**
     * Validates the password change for an admin
     */
    @AdminAction(@Permission(module = "adminAdminAccess", operation = "changePassword"))
    void validateChangeAdminPassword(ChangeLoginPasswordDTO params) throws ValidationException;

    /**
     * Validates the password change for a member
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "changePassword"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "changePassword"))
    @PathToMember("user.element")
    void validateChangeMemberPassword(ChangeLoginPasswordDTO params) throws ValidationException;

    /**
     * Validates the parameters of a pin change
     */
    @AdminAction(@Permission(module = "adminMemberAccess", operation = "changePin"))
    @BrokerAction(@Permission(module = "brokerMemberAccess", operation = "changePin"))
    @PathToMember("user.element")
    void validateChangeMemberPin(ChangePinDTO params) throws ValidationException;

    /**
     * Validates the password change for the logged user
     */
    void validateChangeMyPassword(ChangeLoginPasswordDTO params) throws ValidationException;

    /**
     * Validates the params to change logged user's pin
     */
    @MemberAction
    @IgnoreMember
    void validateChangeMyPin(ChangePinDTO params) throws ValidationException;

    /**
     * Validates the password change for an operator
     */
    @MemberAction(@Permission(module = "memberOperators", operation = "manage"))
    @PathToMember("user.element.member")
    void validateChangeOperatorPassword(ChangeLoginPasswordDTO params) throws ValidationException;

    /**
     * Checks if the login can be performed for the given username and address
     * @throws UserNotFoundException When the given user does not exist
     * @throws InactiveMemberException When the member has no active password
     * @throws PermissionDeniedException When the member does not have permission to login
     */
    User verifyLogin(String member, String username, String remoteAddress) throws UserNotFoundException, InactiveMemberException, PermissionDeniedException;
}
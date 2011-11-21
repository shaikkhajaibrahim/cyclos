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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.store.chm.ConcurrentHashMap;
import nl.strohalm.cyclos.dao.access.LoginHistoryDAO;
import nl.strohalm.cyclos.dao.access.PasswordHistoryLogDAO;
import nl.strohalm.cyclos.dao.access.UserDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.dao.members.ElementDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.AdminUser;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.LoginHistoryLog;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.PasswordHistoryLog;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.access.PasswordHistoryLog.PasswordType;
import nl.strohalm.cyclos.entities.access.User.TransactionPasswordStatus;
import nl.strohalm.cyclos.entities.accounts.cards.Card;
import nl.strohalm.cyclos.entities.accounts.cards.CardType;
import nl.strohalm.cyclos.entities.alerts.MemberAlert;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Type;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.groups.BasicGroupSettings.PasswordPolicy;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.exceptions.MailSendingException;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.CredentialsAlreadyUsedException;
import nl.strohalm.cyclos.services.access.exceptions.InactiveMemberException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCardException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidUserForChannelException;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;
import nl.strohalm.cyclos.services.access.exceptions.SessionAlreadyInUseException;
import nl.strohalm.cyclos.services.access.exceptions.SystemOfflineException;
import nl.strohalm.cyclos.services.access.exceptions.UserNotFoundException;
import nl.strohalm.cyclos.services.accounts.cards.CardService;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.application.ApplicationService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.ResetTransactionPasswordDTO;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.HashHandler;
import nl.strohalm.cyclos.utils.InvalidTriesTracer;
import nl.strohalm.cyclos.utils.MailHandler;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.RangeConstraint;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.StringHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.access.PermissionRequestorImpl;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.LengthValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;
import nl.strohalm.cyclos.utils.validation.Validator.Property;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation class for access services Service.
 * @author rafael
 * @author luis
 */
public class AccessServiceImpl implements AccessService {

    private final class LoginPasswordValidation implements PropertyValidation {
        private final Element     element;
        private static final long serialVersionUID = -4369049571487478881L;

        private LoginPasswordValidation(final Element element) {
            this.element = element;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String loginPassword = (String) value;
            final AccessSettings accessSettings = settingsService.getAccessSettings();
            final boolean numeric = accessSettings.isNumericPassword();
            return resolveValidationError(true, numeric, element, object, property, loginPassword);
        }
    }

    private final class PinValidation implements PropertyValidation {
        private final Member      member;
        private static final long serialVersionUID = -4369049571487478881L;

        private PinValidation(final Member member) {
            this.member = member;
        }

        public ValidationError validate(final Object object, final Object property, final Object value) {
            final String loginPassword = (String) value;
            // pin is always numeric
            return resolveValidationError(false, true, member, object, property, loginPassword);
        }
    }

    private static final Relationship                FETCH                       = RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP);

    private AlertService                             alertService;
    private FetchService                             fetchService;
    private ElementService                           elementService;
    private PermissionService                        permissionService;
    private SettingsService                          settingsService;
    private ElementDAO                               elementDao;
    private LoginHistoryDAO                          loginHistoryDao;
    private PasswordHistoryLogDAO                    passwordHistoryLogDao;
    private UserDAO                                  userDao;
    private final ConcurrentMap<String, Integer>     failedUsernamesByAddress    = new ConcurrentHashMap<String, Integer>();
    private final ConcurrentMap<String, Integer>     failedLogins                = new ConcurrentHashMap<String, Integer>();
    private final ConcurrentMap<String, Integer>     permissionDeniedCounter     = new ConcurrentHashMap<String, Integer>();
    private final ConcurrentMap<String, Calendar>    blockedUsers                = new ConcurrentHashMap<String, Calendar>();
    private final ConcurrentMap<String, Integer>     failedTransactionPasswords  = new ConcurrentHashMap<String, Integer>();
    private final ConcurrentMap<String, SessionDTO>  sessions                    = new ConcurrentHashMap<String, SessionDTO>();
    private final ConcurrentMap<User, UserLoginDTO>  loginsByUser                = new ConcurrentHashMap<User, UserLoginDTO>();
    private final ConcurrentMap<Group, Set<Element>> loggedElementsByGroup       = new ConcurrentHashMap<Group, Set<Element>>();
    private MailHandler                              mailHandler;
    private LoggingHandler                           loggingHandler;
    private HashHandler                              hashHandler;
    private ChannelService                           channelService;
    private ApplicationService                       applicationService;
    private CardService                              cardService;
    private InvalidTriesTracer                       pinTriesTracer              = new InvalidTriesTracer();
    private InvalidTriesTracer                       cardSecurityCodeTriesTracer = new InvalidTriesTracer();

    private static final Log                         LOG                         = LogFactory.getLog(AccessServiceImpl.class);
    private TransactionTemplate                      transactionTemplateNewTx;

    public void addLoginPasswordValidation(final Element element, final Property property) {
        property.add(new LoginPasswordValidation(element));
    }

    public void addPinValidation(final Member member, final Property property) {
        property.add(new PinValidation(member));
    }

    public void canChangePassword(User user) {
        if (LoggedUser.user().equals(user)) { // the logged user always can change his own password
            return;
        }

        final PermissionRequestorImpl permissionRequestor = new PermissionRequestorImpl();
        switch (user.getElement().getNature()) {
            case ADMIN:
                permissionRequestor.adminPermissions("adminAdminAccess", "changePassword");
                break;
            case MEMBER:
                user = fetchService.fetch(user, User.Relationships.ELEMENT);
                permissionRequestor.adminPermissions("adminMemberAccess", "changePassword");
                permissionRequestor.brokerPermissions("brokerMemberAccess", "changePassword");
                permissionRequestor.manages((Member) user.getElement());
                break;
            case OPERATOR:
                user = fetchService.fetch(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Operator.Relationships.MEMBER));
                permissionRequestor.memberPermissions("memberOperators", "manage");
                permissionRequestor.manages(((Operator) user.getElement()).getMember());
                break;
            default:
                throw new PermissionDeniedException();
        }

        if (!permissionService.checkPermissions(permissionRequestor)) {
            throw new PermissionDeniedException();
        }
    }

    public AdminUser changeAdminPassword(final ChangeLoginPasswordDTO params) {
        validateChangeAdminPassword(params);
        final AdminUser user = (AdminUser) fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        if (LoggedUser.user().equals(user)) {
            throw new PermissionDeniedException();
        }
        return (AdminUser) changePassword(user, params.getNewPassword(), params.isForceChange());
    }

    public Member changeMemberChannelsAccess(final Member member, final Collection<Channel> channels, final boolean verifySmsChannel) {
        return changeChannelsAccess(member, channels, verifySmsChannel);
    }

    public void changeMemberCredentialsByWebService(final MemberUser user, ServiceClient client, final String newCredentials) throws CredentialsAlreadyUsedException {
        client = fetchService.fetch(client, ServiceClient.Relationships.CHANNEL);
        switch (client.getChannel().getCredentials()) {
            case LOGIN_PASSWORD:
                changePassword(user, null, newCredentials, false);
                break;
            case PIN:
                changePin(user, newCredentials);
                break;
        }
    }

    public MemberUser changeMemberPassword(final ChangeLoginPasswordDTO params) throws CredentialsAlreadyUsedException {
        validateChangeMemberPassword(params);
        final MemberUser user = (MemberUser) fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        return (MemberUser) changePassword(user, params.getNewPassword(), params.isForceChange());
    }

    public MemberUser changeMemberPin(final ChangePinDTO params) throws CredentialsAlreadyUsedException {
        validateChangeMemberPin(params);
        final MemberUser user = fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        if (LoggedUser.user().equals(user)) {
            throw new PermissionDeniedException();
        }
        return changePin(user, params.getNewPin());
    }

    public Member changeMyChannelsAccess(final Collection<Channel> channels, final boolean verifySmsChannel) {
        final Member member = LoggedUser.element();
        return changeChannelsAccess(member, channels, verifySmsChannel);
    }

    public User changeMyPassword(final ChangeLoginPasswordDTO params, final String remoteAddress) throws InvalidCredentialsException, BlockedCredentialsException, CredentialsAlreadyUsedException {
        validateChangeMyPassword(params);
        final User loggedUser = LoggedUser.user();

        // We'll only check the old password if it's not expired
        final boolean isExpired = hasPasswordExpired(loggedUser);
        if (!isExpired) {
            // Check the current password
            final Element loggedElement = LoggedUser.element();
            String member = null;
            if (LoggedUser.isOperator()) {
                final Operator operator = LoggedUser.element();
                member = operator.getMember().getUsername();
            }
            checkPassword(member, loggedElement.getUsername(), params.getOldPassword(), remoteAddress);
        }

        return changePassword(LoggedUser.user(), params.getNewPassword(), false);
    }

    public MemberUser changeMyPin(final ChangePinDTO params, final String remoteAddress) throws InvalidCredentialsException, BlockedCredentialsException, CredentialsAlreadyUsedException {
        validateChangeMyPin(params);

        // Check whether to enforce the login or transaction password
        final MemberUser loggedUser = LoggedUser.user();
        final Member loggedMember = (Member) fetchService.fetch(loggedUser.getElement(), Element.Relationships.GROUP);
        final boolean usesTransactionPassword = loggedMember.getMemberGroup().getBasicSettings().getTransactionPassword().isUsed();

        // If the password (or transaction password) is incorrect an exception is thrown
        if (usesTransactionPassword) {
            checkTransactionPassword(null, loggedMember.getUsername(), params.getCredentials(), remoteAddress);
        } else {
            checkPassword(null, loggedMember.getUsername(), params.getCredentials(), remoteAddress);
        }

        return changePin(loggedUser, params.getNewPin());
    }

    public OperatorUser changeOperatorPassword(final ChangeLoginPasswordDTO params) {
        validateChangeOperatorPassword(params);
        final OperatorUser user = (OperatorUser) fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        return (OperatorUser) changePassword(user, params.getNewPassword(), params.isForceChange());
    }

    public void checkCredentials(Channel channel, MemberUser user, final String credentials, final String remoteAddress, final Member relatedMember) {
        if (StringUtils.isEmpty(credentials)) {
            throw new InvalidCredentialsException(channel.getCredentials(), user);
        }
        channel = fetchService.fetch(channel, Channel.Relationships.PRINCIPALS);

        user = fetchService.fetch(user);
        switch (channel.getCredentials()) {
            case DEFAULT:
            case LOGIN_PASSWORD:
                checkPassword(user, credentials, remoteAddress);
                break;
            case PIN:
                checkPin(user, credentials, channel.getInternalName(), relatedMember);
                break;
            case TRANSACTION_PASSWORD:
                checkTransactionPassword(null, user.getUsername(), credentials, remoteAddress);
                break;
            case CARD_SECURITY_CODE:
                final Card card = cardService.getActiveCard(user.getMember());
                checkCardSecurityCode(card.getCardNumber(), credentials, channel.getInternalName());
                break;
        }
    }

    public MemberUser checkCredentials(final String channelName, final PrincipalType principalType, final String principal, final String credentials, final String remoteAddress, final Member relatedMember) throws UserNotFoundException, InvalidCardException, InvalidCredentialsException, BlockedCredentialsException, InvalidCardException {
        final Channel channel = channelService.loadByInternalName(channelName);
        if (!channel.allows(principalType)) {
            throw new ValidationException();
        }

        // Load the member by principal
        Member member;
        try {
            member = elementService.loadByPrincipal(principalType, principal, FETCH);
        } catch (final EntityNotFoundException e) {
            if (Card.class.equals(e.getEntityType())) {
                throw new InvalidCardException();
            }
            throw new UserNotFoundException(principal);
        }
        final MemberUser user = member.getMemberUser();

        // Check the credentials
        checkCredentials(channel, user, credentials, remoteAddress, relatedMember);

        return user;
    }

    public User checkPassword(final String member, final String username, final String plainPassword, final String remoteAddress) {
        final User user = loadUser(member, username);
        return checkPassword(user, plainPassword, remoteAddress);
    }

    public User checkTransactionPassword(final String memberUsername, final String username, final String plainTransactionPassword, final String remoteAddress) {
        final User user = loadUser(memberUsername, username);
        return checkTransactionPassword(user, plainTransactionPassword, remoteAddress);
    }

    public void disconnectAllButLogged() {
        final User loggedUser = LoggedUser.user();
        // The map is not iterated directly to avoid ConcurrentModificationExcetion's
        final Collection<User> loggedUsers = new LinkedList<User>(loginsByUser.keySet());
        for (final User user : loggedUsers) {
            if (!loggedUser.equals(user)) {
                disconnect(user, null);
            }
        }
    }

    public AdminUser disconnectLoggedAdmin(final AdminUser user, final String sessionId) throws NotConnectedException {
        return disconnect(user, sessionId);
    }

    public MemberUser disconnectLoggedMember(final MemberUser user, final String sessionId) throws NotConnectedException {
        return disconnect(user, sessionId);
    }

    public OperatorUser disconnectLoggedOperator(final OperatorUser user, final String sessionId) throws NotConnectedException {
        return disconnect(user, sessionId);
    }

    public String generatePassword(Group group) {
        if (group instanceof OperatorGroup) {
            group = fetchService.fetch(group, RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
        }
        final Integer min = group.getBasicSettings().getPasswordLength().getMin();
        final boolean onlyNumbers = settingsService.getAccessSettings().isNumericPassword();
        return RandomStringUtils.random(min == null ? 4 : min, !onlyNumbers, true).toLowerCase();
    }

    public String generateTransactionPassword() {

        User user = LoggedUser.user();

        // Load operator´s member and group of operator´s member
        if (user instanceof OperatorUser) {
            Element element = user.getElement();
            element = fetchService.fetch(element, RelationshipHelper.nested(Operator.Relationships.MEMBER, Element.Relationships.GROUP));
        }

        // If the transaction password is not used for the logged user, return null
        if (!requestTransactionPassword(user)) {
            throw new UnexpectedEntityException();
        }

        // If there is already a password, return null
        final String current = user.getTransactionPassword();
        if (current != null) {
            return null;
        }

        // Get the chars
        final AccessSettings accessSettings = settingsService.getAccessSettings();
        final String chars = accessSettings.getTransactionPasswordChars();

        // Get the password length
        Group group = user.getElement().getGroup();
        if (group instanceof OperatorGroup) {
            group = fetchService.fetch(group, RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
        }
        final BasicGroupSettings basicSettings = group.getBasicSettings();
        final int length = basicSettings.getTransactionPasswordLength();

        // Generate a new one, and store it
        final StringBuilder buffer = new StringBuilder(length);
        final Random rnd = new Random();
        for (int i = 0; i < length; i++) {
            buffer.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        final String transactionPassword = buffer.toString();
        user.setTransactionPassword(hashHandler.hash(user.getSalt(), transactionPassword));
        user.setTransactionPasswordStatus(TransactionPasswordStatus.ACTIVE);
        user = userDao.update(user);
        return transactionPassword;
    }

    public User getLoggedUser(final String sessionId) {
        final SessionDTO session = sessions.get(sessionId);
        if (session == null) {
            throw new NotConnectedException();
        }
        return session.getUser();
    }

    public boolean hasPasswordExpired(User user) {
        user = fetchService.fetch(user, FETCH);
        Group group = user.getElement().getGroup();
        if (group instanceof OperatorGroup) {
            group = fetchService.fetch(group, RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
        }
        final TimePeriod exp = group.getBasicSettings().getPasswordExpiresAfter();
        final Calendar passwordDate = user.getPasswordDate();
        if (passwordDate == null) {
            return true;
        }
        if (exp != null && exp.getNumber() > 0 && passwordDate != null) {
            final Calendar expiresAt = exp.remove(Calendar.getInstance());
            return expiresAt.after(passwordDate);
        }
        return false;
    }

    public boolean isCardSecurityCodeBlocked(final BigInteger cardNumber) {
        try {
            cardService.loadByNumber(cardNumber, Card.Relationships.CARD_TYPE);
        } catch (final Exception e) {
            return false;
        }
        return cardSecurityCodeTriesTracer.isBlocked(cardNumber.toString());
    }

    public boolean isChannelEnabledForMember(final String channelInternalName, Member member) {
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        final Channel channel = channelService.loadByInternalName(channelInternalName);

        switch (channel.getCredentials()) {
            case TRANSACTION_PASSWORD:
                // Check if the member's group uses transaction password.
                if (!member.getMemberGroup().getBasicSettings().getTransactionPassword().isUsed()) {
                    LOG.warn("The member's group doesn't use transaction password,  member: " + member);
                    return false;
                }
                break;
            case CARD_SECURITY_CODE:
                // Check if the member's group has a Card Type
                if (member.getMemberGroup().getCardType() == null) {
                    LOG.warn("The member's group doesn't have a card type,  member: " + member);
                    return false;
                }
                break;
        }

        // If the group doesn't have access to the channel, the member can't access it too
        if (!isChannelEnabledForGroup(channelInternalName, member.getMemberGroup())) {
            return false;
        }
        // The access to the web channel can not be customized by the member
        if (channelInternalName.equals(Channel.WEB)) {
            return true;
        }
        // Check the member access customization
        for (final Channel ch : member.getChannels()) {
            if (ch.getInternalName().equals(channelInternalName)) {
                return true;
            }
        }
        return false;
    }

    public boolean isLoggedIn(final User user) {
        return loginsByUser.get(user) != null;
    }

    public boolean isLoginBlocked(final User user) {
        final String key = key(user);

        if (blockedUsers.containsKey(key)) {
            final Calendar mayLoginAt = blockedUsers.get(key);
            if (Calendar.getInstance().after(mayLoginAt)) {
                return false;
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean isObviousCredential(Element element, final String credential) {

        // Ensure not to fetch the element for new records
        if (element.isPersistent()) {
            element = fetchService.fetch(element, Element.Relationships.USER, Member.Relationships.CUSTOM_VALUES);
        }

        // If the credential is equals to the username, it is obvious
        if (credential.equalsIgnoreCase(element.getUsername())) {
            return true;
        }

        // If the credential is equals to any word of the full name, it is obvious
        final String[] nameParts = StringUtils.split(element.getName(), " .,/-\\");
        for (final String part : nameParts) {
            if (credential.equalsIgnoreCase(part)) {
                return true;
            }
        }

        // If the credential is equals to the user part of the e-mail, it is obvious
        final String email = element.getEmail();
        if (StringUtils.isNotEmpty(email) && email.contains("@")) {
            final String mailUser = StringUtils.split(email, "@", 1)[0];
            if (credential.equalsIgnoreCase(mailUser)) {
                return true;
            }
        }

        // If the credential matches custom field values, it is obvious
        final Collection<CustomFieldValue> customValues = PropertyHelper.get(element, "customValues");
        final LocalSettings localSettings = settingsService.getLocalSettings();
        for (final CustomFieldValue fieldValue : customValues) {
            final Type type = fieldValue.getField().getType();
            final String stringValue = fieldValue.getStringValue();
            if (StringUtils.isEmpty(stringValue)) {
                continue;
            }
            switch (type) {
                case DATE:
                    // The credential cannot be the same as a date
                    final String unmasked = StringHelper.removeMask(localSettings.getDatePattern().getPattern(), stringValue);
                    if (credential.equals(unmasked)) {
                        return true;
                    }
                    break;
                case INTEGER:
                    // The credential cannot be equal to an integer
                    if (credential.equals(stringValue)) {
                        return true;
                    }
                    break;
                case STRING:
                    // The credential cannot be contained in a string
                    if (stringValue.contains(credential)) {
                        return true;
                    }
                    break;
            }
        }

        // When all chars have the same distance, it's obvious (things like 1234, 7654, abcdef and so on...)
        if (credential.length() > 1) {
            final Set<Integer> diffs = new HashSet<Integer>();
            for (int i = 1, len = credential.length(); i < len; i++) {
                final char current = credential.charAt(i);
                final char previous = credential.charAt(i - 1);
                diffs.add(current - previous);
                if (diffs.size() > 1) {
                    // More than 1 difference means it's not obvious
                    break;
                }
            }
            if (diffs.size() == 1) {
                return true;
            }
        }

        return false;
    }

    public boolean isPinBlocked(final MemberUser user) {
        return pinTriesTracer.isBlocked(user.getUsername());
    }

    @SuppressWarnings("unchecked")
    public List<UserLoginDTO> listConnectedOperators() {
        // Filter the logged users
        final List<UserLoginDTO> list = new ArrayList<UserLoginDTO>();
        final Collection<UserLoginDTO> values = loginsByUser.values();
        final Member loggedMember = LoggedUser.element();
        for (final UserLoginDTO dto : values) {
            final User user = dto.getUser();
            if (!(user instanceof OperatorUser)) {
                continue;
            }
            final Operator operator = ((OperatorUser) user).getOperator();
            if (!operator.getMember().equals(loggedMember)) {
                continue;
            }
            list.add(dto);
        }

        // Sort the in-memory collection
        Collections.sort(list, new BeanComparator("user.element.name"));
        return list;
    }

    @SuppressWarnings("unchecked")
    public List<UserLoginDTO> listConnectedUsers(final Collection<Group.Nature> natures) {

        AdminGroup adminGroup = LoggedUser.group();
        adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS, AdminGroup.Relationships.VIEW_CONNECTED_ADMINS_OF);
        final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
        final Collection<AdminGroup> viewConnectedAdminsOf = adminGroup.getViewConnectedAdminsOf();

        // Filter the logged users
        final List<UserLoginDTO> list = new ArrayList<UserLoginDTO>();
        final Collection<UserLoginDTO> values = loginsByUser.values();
        for (final UserLoginDTO dto : values) {
            final User user = dto.getUser();
            final Group group = user.getElement().getGroup();
            // Test the nature
            if (natures != null && !natures.isEmpty() && !natures.contains(group.getNature())) {
                continue;
            }
            // Test if the admin has permission to see the user
            if (group instanceof AdminGroup) {
                // Admins are filtered by the viewConnectedAdminsOf collection
                if (viewConnectedAdminsOf == null || !viewConnectedAdminsOf.contains(group)) {
                    continue;
                }
            } else {
                MemberGroup managedGroup;
                if (group instanceof OperatorGroup) {
                    managedGroup = fetchService.fetch(((OperatorGroup) group).getMember()).getMemberGroup();
                } else {
                    managedGroup = (MemberGroup) group;
                }
                if (managesGroups == null || !managesGroups.contains(managedGroup)) {
                    continue;
                }
            }
            list.add(dto);
        }

        // Sort the in-memory collection
        Collections.sort(list, new BeanComparator("user.element.name"));
        return list;
    }

    public User login(final User user, final String plainCredentials, final String channelName, final String remoteAddress, final String sessionId) throws UserNotFoundException, InvalidCredentialsException, BlockedCredentialsException, SessionAlreadyInUseException {
        if (user == null) {
            throw new UnexpectedEntityException();
        }
        final String key = key(user);

        // Check if the session id is already in use
        final SessionDTO session = sessions.get(sessionId);
        if (session != null) {
            // If the same logged user under the same session id, return ok
            if (user.equals(session.getUser()) && remoteAddress.equals(session.getRemoteAddress())) {
                return user;
            }
            throw new SessionAlreadyInUseException(key);
        }
        final Channel channel = channelService.loadByInternalName(channelName);

        // When the system is offline, only allow login users with manage online state permission
        if (!applicationService.isOnline()) {
            if (!permissionService.checkPermission(user.getElement().getGroup(), "systemTasks", "onlineState")) {
                throw new SystemOfflineException();
            }
        }

        final boolean isMainWebChannel = Channel.WEB.equals(channel.getInternalName());
        if (user instanceof MemberUser) {
            // For members, check the channel's credentials
            checkCredentials(channel, (MemberUser) user, plainCredentials, remoteAddress, null);
            // Also, ensure the channel is enabled for the member (if not the main web channel)
            if (!isMainWebChannel && !isChannelEnabledForMember(channelName, ((MemberUser) user).getMember())) {
                throw new InvalidUserForChannelException(user.getUsername());
            }
        } else {
            // For admins or operators, always check the login
            if (channel != null && !isMainWebChannel) {
                // Also, they can only login in the main web channel
                throw new PermissionDeniedException();
            }
            checkPassword(user, plainCredentials, remoteAddress);
        }

        // Store the login data
        storeLogin(key, user, remoteAddress, sessionId);

        return user;
    }

    public User logout(final String sessionId) {
        final SessionDTO session = sessions.get(sessionId);
        if (session != null) {
            return storeLogout(session);
        }
        return null;
    }

    public boolean notifyPermissionDeniedException() {
        final User user = fetchService.fetch(LoggedUser.user(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        final String key = key(user);
        final BasicGroupSettings basicSettings = user.getElement().getGroup().getBasicSettings();

        // Check if maxTries has been reached
        final int maxTries = basicSettings.getMaxPasswordWrongTries();
        if (maxTries > 0) {
            int tries = 0;
            if (permissionDeniedCounter.containsKey(key)) {
                tries = permissionDeniedCounter.get(key);
            }
            tries++;
            permissionDeniedCounter.put(key, tries);
            if (tries >= maxTries) {
                // Block the user. Calculate when the user will be able to login again
                final Calendar mayLoginAt = basicSettings.getDeactivationAfterMaxPasswordTries().add(Calendar.getInstance());
                blockedUsers.put(key, mayLoginAt);
                // Send an alert
                insertAlert(user, tries);
                return true;
            }
        }
        return false;
    }

    public void reenableAdminLogin(final AdminUser user) {
        reenableUserLogin(user);
    }

    public void reenableMemberLogin(final MemberUser user) {
        reenableUserLogin(user);
    }

    public void reenableOperatorLogin(final OperatorUser user) {
        reenableUserLogin(user);
    }

    public AdminUser resetAdminTransactionPassword(final ResetTransactionPasswordDTO dto) {
        return resetTransactionPassword(AdminUser.class, dto);
    }

    public MemberUser resetMemberTransactionPassword(final ResetTransactionPasswordDTO dto) {
        return resetTransactionPassword(MemberUser.class, dto);
    }

    public OperatorUser resetOperatorTransactionPassword(final ResetTransactionPasswordDTO dto) {
        return resetTransactionPassword(OperatorUser.class, dto);
    }

    public void resetPasswordOnly(ChangeLoginPasswordDTO params){
        final MemberUser memberUser = (MemberUser) fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        final MemberGroup group = memberUser.getMember().getMemberGroup();
        String newPassword = generatePassword(group);
        params.setForceChange(true);
        params.setNewPassword(newPassword);
        params.setNewPasswordConfirmation(newPassword);
        changeMemberPassword(params);
    }


    public void resetPassword(MemberUser user) throws MailSendingException {
        // Check if password will be sent by mail
        user = fetchService.fetch(user, FETCH);
        final MemberGroup group = user.getMember().getMemberGroup();
        final boolean sendPasswordByEmail = group.getMemberSettings().isSendPasswordByEmail();

        String newPassword = null;
        if (sendPasswordByEmail) {
            // If send by mail, generate a new password
            newPassword = generatePassword(group);
        }

        // Update the user
        user.setPassword(hashHandler.hash(user.getSalt(), newPassword));
        user.setPasswordDate(null);
        userDao.update(user);

        if (sendPasswordByEmail) {
            // Send the password by mail
            mailHandler.sendResetPassword(user.getMember(), newPassword);
        }

    }

    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    public void setApplicationService(final ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setCardService(final CardService cardService) {
        this.cardService = cardService;
    }

    public void setChannelService(final ChannelService channelService) {
        this.channelService = channelService;
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

    public void setHashHandler(final HashHandler hashHandler) {
        this.hashHandler = hashHandler;
    }

    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    public void setLoginHistoryDao(final LoginHistoryDAO loginHistoryDao) {
        this.loginHistoryDao = loginHistoryDao;
    }

    public void setMailHandler(final MailHandler mailHandler) {
        this.mailHandler = mailHandler;
    }

    public void setPasswordHistoryLogDao(final PasswordHistoryLogDAO passwordHistoryLogDao) {
        this.passwordHistoryLogDao = passwordHistoryLogDao;
    }

    public void setPermissionService(final PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionTemplateNewTx(final TransactionTemplate transactionTemplateNewTx) {
        this.transactionTemplateNewTx = transactionTemplateNewTx;
    }

    public void setUserDao(final UserDAO userDao) {
        this.userDao = userDao;
    }

    public void unblockCardSecurityCode(final BigInteger cardNumber) {
        cardSecurityCodeTriesTracer.unblock(cardNumber.toString());
    }

    public void unblockMyPin() {
        final MemberUser user = LoggedUser.user();
        unblockPin(user);
    }

    public void unblockPin(final MemberUser user) {
        pinTriesTracer.unblock(user.getUsername());
    }

    public void updateGroupReference(final Group group) {
        final Set<Element> elements = loggedElementsByGroup.get(group);
        if (elements != null) {
            for (final Element element : elements) {
                element.setGroup(group);
            }
        }
    }

    public void validateChangeAdminPassword(final ChangeLoginPasswordDTO params) throws ValidationException {
        validateChangePassword(params, false);
    }

    public void validateChangeMemberPassword(final ChangeLoginPasswordDTO params) throws ValidationException {
        validateChangePassword(params, false);
    }

    public void validateChangeMemberPin(final ChangePinDTO params) {
        validateChangePin(params, false);
    }

    public void validateChangeMyPassword(final ChangeLoginPasswordDTO params) throws ValidationException {
        final User user = LoggedUser.user();
        params.setUser(user);
        validateChangePassword(params, true);
    }

    public void validateChangeMyPin(final ChangePinDTO params) {
        final MemberUser user = LoggedUser.user();
        params.setUser(user);
        validateChangePin(params, true);
    }

    public void validateChangeOperatorPassword(final ChangeLoginPasswordDTO params) throws ValidationException {
        validateChangePassword(params, false);
    }

    public ValidationError validateLoginPassword(final Element element, final String loginPassword) {
        return new LoginPasswordValidation(element).validate(element.getUser(), "password", loginPassword);
    }

    public User verifyLogin(final String member, final String username, final String remoteAddress) throws UserNotFoundException, InactiveMemberException, PermissionDeniedException {
        try {
            final User user = loadUser(member, username);

            // Check if there's an active password
            if (StringUtils.isEmpty(user.getPassword())) {
                throw new InactiveMemberException(username);
            }

            // Check if the member has permission to login
            if (!permissionService.checkPermission(user.getElement().getGroup(), "basic", "login")) {
                throw new PermissionDeniedException();
            }

            failedUsernamesByAddress.remove(remoteAddress);
            return user;
        } catch (final EntityNotFoundException e) {
            // Record the incorrect attempt
            recordIncorrectUser(member, username, remoteAddress);
            throw new AssertionError("Should not reach here - recordIncorrectUser should throw an exception");
        }
    }

    private Member changeChannelsAccess(Member member, final Collection<Channel> channels, final boolean verifySmsChannel) {
        member = fetchService.fetch(member, Member.Relationships.CHANNELS);
        final Channel smsChannel = channelService.getSmsChannel();
        // When SMS channel is enabled, it is not set directly by this method. So, we need to ensure it remains related to the member after saving
        if (verifySmsChannel && smsChannel != null && member.getChannels().contains(smsChannel)) {
            channels.add(smsChannel);
        }
        member.setChannels(channels);
        return elementDao.update(member);
    }

    private <U extends User> U changePassword(U user, final String oldPassword, final String plainNewPassword, final boolean forceChange) {
        user = fetchService.reload(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));

        // Before changing, ensure that the new password is valid
        final ValidationError validationResult = new LoginPasswordValidation(user.getElement()).validate(user, "password", plainNewPassword);
        if (validationResult != null) {
            throw new ValidationException("password", "channel.credentials.LOGIN_PASSWORD", validationResult);
        }

        final String currentPassword = user.getPassword();
        final String hashedOldPassword = hashHandler.hash(user.getSalt(), oldPassword);
        if (StringUtils.isNotEmpty(oldPassword) && !hashedOldPassword.equalsIgnoreCase(currentPassword)) {
            throw new InvalidCredentialsException(Credentials.LOGIN_PASSWORD, user);
        }

        final String newPassword = hashHandler.hash(user.getSalt(), plainNewPassword);

        final PasswordPolicy passwordPolicy = user.getElement().getGroup().getBasicSettings().getPasswordPolicy();
        if (passwordPolicy != PasswordPolicy.NONE && !forceChange) {
            // Check if it was already in use when there's a password policy
            //do not cjeck if password forced to be changed on next login
            if (StringUtils.trimToEmpty(currentPassword).equalsIgnoreCase(newPassword) || passwordHistoryLogDao.wasAlreadyUsed(user, PasswordType.LOGIN, newPassword)) {
                throw new CredentialsAlreadyUsedException(Credentials.LOGIN_PASSWORD, user);
            }
        }

        // Ensure the login password is not equals to the pin or transaction password
        if (newPassword.equalsIgnoreCase(user.getTransactionPassword()) || (user instanceof MemberUser && newPassword.equalsIgnoreCase(((MemberUser) user).getPin()))) {
            throw new ValidationException("changePassword.error.sameAsTransactionPasswordOrPin");
        }

        user.setPassword(newPassword);
        user.setPasswordDate(forceChange ? null : Calendar.getInstance());
        // Ensure that the returning user will have the ELEMENT and GROUP fetched
        user = fetchService.fetch(userDao.update(user), FETCH);
        if (user instanceof OperatorUser) {
            // When an operator, also ensure that the member will have the ELEMENT and GROUP fetched
            final Operator operator = ((OperatorUser) user).getOperator();
            final Member member = fetchService.fetch(operator.getMember(), Element.Relationships.USER, Element.Relationships.GROUP);
            operator.setMember(member);
        }

        // Log the password history
        if (StringUtils.isNotEmpty(currentPassword)) {
            final PasswordHistoryLog log = new PasswordHistoryLog();
            log.setDate(Calendar.getInstance());
            log.setUser(user);
            log.setType(PasswordType.LOGIN);
            log.setPassword(currentPassword);
            passwordHistoryLogDao.insert(log);
        }

        return user;
    }

    private User changePassword(User user, final String plainPassword, final boolean forceChange) {
        // Before changing, ensure that the new password is valid
        final ValidationError validationResult = new LoginPasswordValidation(user.getElement()).validate(user, "password", plainPassword);
        if (validationResult != null) {
            throw new ValidationException("password", "channel.credentials.LOGIN_PASSWORD", validationResult);
        }

        final String password = hashHandler.hash(user.getSalt(), plainPassword);
        user = fetchService.fetch(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        final String currentPassword = user.getPassword();

        final PasswordPolicy passwordPolicy = user.getElement().getGroup().getBasicSettings().getPasswordPolicy();
        if (passwordPolicy != null && passwordPolicy != PasswordPolicy.NONE) {
            // Check if it was already in use when there's a password policy
            if (StringUtils.trimToEmpty(currentPassword).equalsIgnoreCase(password) || passwordHistoryLogDao.wasAlreadyUsed(user, PasswordType.LOGIN, password)) {
                throw new CredentialsAlreadyUsedException(Credentials.LOGIN_PASSWORD, user);
            }
        }

        // Ensure the login password is not equals to the pin or transaction password
        final String pin = user instanceof MemberUser ? ((MemberUser) user).getPin() : null;
        if (password.equalsIgnoreCase(pin) || password.equalsIgnoreCase(user.getTransactionPassword())) {
            throw new ValidationException("changePassword.error.sameAsTransactionPasswordOrPin");
        }

        // Set the new password
        user.setPassword(password);
        if (forceChange) {
            user.setPasswordDate(null);
        } else {
            user.setPasswordDate(Calendar.getInstance());
        }

        // Log the password history
        if (StringUtils.isNotEmpty(currentPassword)) {
            final PasswordHistoryLog log = new PasswordHistoryLog();
            log.setDate(Calendar.getInstance());
            log.setUser(user);
            log.setType(PasswordType.LOGIN);
            log.setPassword(currentPassword);
            passwordHistoryLogDao.insert(log);
        }

        return user;
    }

    private MemberUser changePin(MemberUser user, final String plainPin) {

        // Before changing, ensure that the new pin is valid
        final ValidationError validationResult = new PinValidation(user.getMember()).validate(user, "pin", plainPin);
        if (validationResult != null) {
            throw new ValidationException("pin", "channel.credentials.PIN", validationResult);
        }

        final String pin = hashHandler.hash(user.getSalt(), plainPin);
        user = fetchService.fetch(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        final String currentPin = user.getPin();

        final PasswordPolicy passwordPolicy = user.getElement().getGroup().getBasicSettings().getPasswordPolicy();
        if (passwordPolicy != null && passwordPolicy != PasswordPolicy.NONE) {
            // Check if it was already in use when there's a password policy
            if (StringUtils.trimToEmpty(currentPin).equalsIgnoreCase(pin) || passwordHistoryLogDao.wasAlreadyUsed(user, PasswordType.PIN, pin)) {
                throw new CredentialsAlreadyUsedException(Credentials.PIN, user);
            }
        }

        // Ensure the login password is not equals to the pin or transaction password
        if (pin.equalsIgnoreCase(user.getPassword()) || pin.equalsIgnoreCase(user.getTransactionPassword())) {
            throw new ValidationException("changePin.error.sameAsLoginOrTransactionPassword");
        }

        // Set the new pin
        user.setPin(pin);

        // Log the pin history
        if (StringUtils.isNotEmpty(currentPin)) {
            final PasswordHistoryLog log = new PasswordHistoryLog();
            log.setDate(Calendar.getInstance());
            log.setUser(user);
            log.setType(PasswordType.PIN);
            log.setPassword(currentPin);
            passwordHistoryLogDao.insert(log);
        }

        return user;
    }

    private Card checkCardSecurityCode(final BigInteger cardNumber, String securityCode, final String channel) {
        Card card;
        try {
            card = cardService.loadByNumber(cardNumber, RelationshipHelper.nested(Card.Relationships.OWNER, Element.Relationships.USER), Card.Relationships.CARD_TYPE);
            if (card.getStatus() != Card.Status.ACTIVE) {
                // Ensure the card is active
                throw new Exception();
            }
        } catch (final Exception e) {
            throw new InvalidCardException();
        }

        // If the securiry code is manual, it is stored hashed, so we must hash the input as well
        if (!card.getCardType().isShowCardSecurityCode()) {
            securityCode = hashHandler.hash(card.getOwner().getUser().getSalt(), securityCode);
        }

        final CardType cardType = card.getCardType();
        // Check if not already blocked
        if (cardSecurityCodeTriesTracer.isBlocked(cardNumber.toString())) {
            throw new BlockedCredentialsException(Credentials.CARD_SECURITY_CODE, card.getOwner().getUser());
        } else {
            final String storedCode = card.getCardSecurityCode();
            if (StringUtils.isEmpty(storedCode) || !securityCode.equals(storedCode)) {
                final int maxTries = cardType.getMaxSecurityCodeTries();
                final TimePeriod blockTimeAfterMaxTries = cardType.getSecurityCodeBlockTime();
                final InvalidTriesTracer.TryStatus status = cardSecurityCodeTriesTracer.trace(cardNumber.toString(), maxTries, blockTimeAfterMaxTries);
                switch (status) {
                    case MAX_TRIES_REACHED:
                        // Create a member alert
                        final String formattedCardNumber = card.getCardType().getCardNumberConverter().toString(cardNumber);
                        alertService.create(card.getOwner(), MemberAlert.Alerts.CARD_SECURITY_CODE_BLOCKED_BY_TRIES, maxTries, formattedCardNumber);
                        // fall down
                    case NO_MORE_TRIES:
                        // MessageAspect will send a personal message to the member
                        throw new BlockedCredentialsException(Credentials.CARD_SECURITY_CODE, card.getOwner().getUser());
                    case TRIES_ALLOWED:
                        throw new InvalidCredentialsException(Credentials.CARD_SECURITY_CODE, card.getOwner().getUser());
                }
            } else {
                cardSecurityCodeTriesTracer.unblock(cardNumber.toString());
            }
        }
        return card;
    }

    private User checkPassword(User user, final String plainPassword, final String remoteAddress) {
        user = fetchService.fetch(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        final String key = key(user);

        // Check if the user is blocked by tries
        if (blockedUsers.containsKey(key)) {
            final Calendar mayLoginAt = blockedUsers.get(key);
            if (!Calendar.getInstance().after(mayLoginAt)) {
                throw new BlockedCredentialsException(Credentials.LOGIN_PASSWORD, user);
            } else {
                failedLogins.remove(key);
                blockedUsers.remove(key);
            }
        }

        // Check the password
        final String password = hashHandler.hash(user.getSalt(), plainPassword);
        final String userPassword = user.getPassword();
        if (userPassword == null || !userPassword.equalsIgnoreCase(StringUtils.trimToNull(password))) {
            final BasicGroupSettings basicSettings = user.getElement().getGroup().getBasicSettings();

            // Check if maxTries has been reached
            final int maxTries = basicSettings.getMaxPasswordWrongTries();
            if (maxTries > 0) {
                int tries = 0;
                if (failedLogins.containsKey(key)) {
                    tries = failedLogins.get(key);
                }
                tries++;
                failedLogins.put(key, tries);
                if (tries >= maxTries) {
                    // Block the user. Calculate when the user will be able to login again
                    final Calendar mayLoginAt = basicSettings.getDeactivationAfterMaxPasswordTries().add(Calendar.getInstance());
                    blockedUsers.put(key, mayLoginAt);
                    // Send an alert
                    if (user instanceof AdminUser) {
                        alertService.create(SystemAlert.Alerts.ADMIN_LOGIN_BLOCKED_BY_TRIES, user.getUsername(), tries, remoteAddress);
                    } else if (user instanceof MemberUser) {
                        alertService.create(((MemberUser) user).getMember(), MemberAlert.Alerts.LOGIN_BLOCKED_BY_TRIES, tries, remoteAddress);
                        // MessageAspect will send a personal message to the member
                    }
                    throw new BlockedCredentialsException(Credentials.LOGIN_PASSWORD, user);
                }
            }
            throw new InvalidCredentialsException(Credentials.LOGIN_PASSWORD, user);
        }

        // Everything ok - ublock and return the user
        blockedUsers.remove(key);
        failedLogins.remove(key);

        return user;
    }

    private MemberUser checkPin(MemberUser user, final String plainPin, final String channel, final Member relatedMember) {

        user = fetchService.fetch(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));

        final Member member = user.getMember();

        // Check if not already blocked
        final String username = user.getUsername();
        if (pinTriesTracer.isBlocked(username)) {
            throw new BlockedCredentialsException(Credentials.PIN, user);
        } else {
            final String pin = hashHandler.hash(user.getSalt(), plainPin);
            // Check the pin
            final String userPin = user.getPin();
            if (userPin == null || !userPin.equalsIgnoreCase(StringUtils.trimToNull(pin))) {
                final MemberGroupSettings memberSettings = member.getMemberGroup().getMemberSettings();
                final int maxTries = memberSettings.getMaxPinWrongTries();
                final TimePeriod blockTimeAfterMaxTries = memberSettings.getPinBlockTimeAfterMaxTries();
                final InvalidTriesTracer.TryStatus status = pinTriesTracer.trace(username, maxTries, blockTimeAfterMaxTries);
                switch (status) {
                    case MAX_TRIES_REACHED:
                        final String relatedUsername = relatedMember == null ? "" : relatedMember.getUsername();
                        alertService.create(member, MemberAlert.Alerts.PIN_BLOCKED_BY_TRIES, maxTries, channel, relatedUsername);
                        // fall down
                    case NO_MORE_TRIES:
                        // MessageAspect will send a personal message to the member
                        throw new BlockedCredentialsException(Credentials.PIN, user);
                    case TRIES_ALLOWED:
                        throw new InvalidCredentialsException(Credentials.PIN, user);
                }
            } else {
                pinTriesTracer.unblock(username);
            }
        }

        return user;
    }

    private User checkTransactionPassword(User user, final String plainTransactionPassword, final String remoteAddress) {
        user = fetchService.fetch(user, RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        if (user.getTransactionPasswordStatus().equals(MemberUser.TransactionPasswordStatus.BLOCKED)) {
            throw new BlockedCredentialsException(Credentials.TRANSACTION_PASSWORD, user);
        }

        final String key = key(user);

        final String transactionPassword = hashHandler.hash(user.getSalt(), plainTransactionPassword == null ? null : plainTransactionPassword.toUpperCase());
        if (StringUtils.trimToEmpty(user.getTransactionPassword()).equalsIgnoreCase(StringUtils.trimToEmpty(transactionPassword))) {
            failedTransactionPasswords.remove(key);
            return user;
        } else {
            final Group group = user.getElement().getGroup();
            final BasicGroupSettings settings = group.getBasicSettings();
            final int maxTries = settings.getMaxTransactionPasswordWrongTries();
            int tries = 0;
            if (failedTransactionPasswords.containsKey(key)) {
                tries = failedTransactionPasswords.get(key);
            }
            tries++;
            if (tries >= maxTries) {
                // Block the transaction password
                final ResetTransactionPasswordDTO dto = new ResetTransactionPasswordDTO();
                dto.setAllowGeneration(false);
                dto.setUser(user);
                resetTransactionPassword(null, dto);

                // Send an alert
                if (user instanceof AdminUser) {
                    alertService.create(SystemAlert.Alerts.ADMIN_TRANSACTION_PASSWORD_BLOCKED_BY_TRIES, user.getUsername(), tries, remoteAddress);
                } else {
                    Member m;
                    if (user instanceof MemberUser) {
                        m = ((MemberUser) user).getMember();
                    } else {
                        m = ((OperatorUser) user).getOperator().getMember();
                    }
                    alertService.create(m, MemberAlert.Alerts.TRANSACTION_PASSWORD_BLOCKED_BY_TRIES, tries, remoteAddress);
                    // MessageAspect will send a personal message to the member
                }

                // Remove from failed list
                failedTransactionPasswords.remove(key);
                throw new BlockedCredentialsException(Credentials.TRANSACTION_PASSWORD, user);
            } else {
                failedTransactionPasswords.put(key, tries);
            }
            throw new InvalidCredentialsException(Credentials.TRANSACTION_PASSWORD, user);
        }
    }

    @SuppressWarnings("unchecked")
    private <U extends User> U disconnect(final User user, final String sessionId) throws NotConnectedException {
        final UserLoginDTO dto = loginsByUser.get(user);
        if (dto == null || !user.equals(dto.getUser())) {
            throw new NotConnectedException();
        }
        if (sessionId == null) {
            return (U) storeLogout(dto);
        } else {
            final SessionDTO session = dto.getSession(sessionId);
            if (session == null) {
                throw new NotConnectedException();
            }
            return (U) storeLogout(session);
        }
    }

    private void insertAlert(final User user, final int tries) {

        try {
            transactionTemplateNewTx.execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus status) {
                    if (user instanceof AdminUser) {
                        alertService.create(SystemAlert.Alerts.ADMIN_LOGIN_BLOCKED_BY_PERMISSION_DENIEDS, user.getUsername(), tries, LoggedUser.remoteAddress());
                    } else if (user instanceof MemberUser) {
                        alertService.create(((MemberUser) user).getMember(), MemberAlert.Alerts.LOGIN_BLOCKED_BY_PERMISSION_DENIEDS, tries, LoggedUser.remoteAddress());
                    }
                };
            });
        } catch (final Exception e) {
            LOG.warn("Error while creating an alert", e);
        }
    }

    private boolean isChannelEnabledForGroup(final String channelInternalName, MemberGroup memberGroup) {
        memberGroup = fetchService.fetch(memberGroup, MemberGroup.Relationships.CHANNELS);
        for (final Channel channel : memberGroup.getChannels()) {
            if (channel.getInternalName().equals(channelInternalName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a unique key for the given member name and username
     */
    private String key(final String member, final String username) {
        String key = username;
        if (StringUtils.isNotEmpty(member)) {
            key = member + " / " + key;
        }
        return key;
    }

    /**
     * Returns a unique key for the given user
     */
    private String key(final User user) {
        String member = null;
        final String username = user.getUsername();
        if (user instanceof OperatorUser) {
            member = ((OperatorUser) user).getOperator().getMember().getUsername();
        }
        return key(member, username);
    }

    private User loadUser(final String member, final String username) {
        if (StringUtils.isEmpty(member)) {
            // Normal user login
            final User user = elementService.loadUser(username, FETCH);
            if (user instanceof OperatorUser) {
                // Cannot login as operator without giving a member username too
                throw new EntityNotFoundException(User.class);
            }
            return user;
        } else {
            // Member's operator login - First load the member
            final User loadedMember = elementService.loadUser(member, FETCH);
            MemberUser memberUser;
            try {
                memberUser = (MemberUser) loadedMember;
            } catch (final ClassCastException e) {
                throw new EntityNotFoundException(MemberUser.class);
            }
            final Member m = memberUser.getMember();
            // Then the operator
            final OperatorUser user = elementService.loadOperatorUser(m, username, FETCH);
            // Assign the already fetched member to the operator
            user.getOperator().setMember(m);
            return user;
        }
    }

    private void recordIncorrectUser(final String member, final String username, final String remoteAddress) throws UserNotFoundException {
        // Store the attempt
        int tries;
        try {
            tries = failedUsernamesByAddress.get(remoteAddress);
        } catch (final NullPointerException npe) {
            tries = 0;
        }
        tries++;
        failedUsernamesByAddress.put(remoteAddress, tries);

        // Check if an alert will be sent
        final int maxTries = settingsService.getAlertSettings().getAmountIncorrectLogin();
        if (maxTries > 0 && tries >= maxTries) {
            alertService.create(SystemAlert.Alerts.MAX_INCORRECT_LOGIN_ATTEMPTS, maxTries, remoteAddress);
            failedUsernamesByAddress.remove(remoteAddress);
        }
        throw new UserNotFoundException(key(member, username));
    }

    private void reenableUserLogin(final User user) {
        final String key = key(user);
        failedLogins.remove(key);
        blockedUsers.remove(key);
    }

    private boolean requestTransactionPassword(User user) {
        user = fetchService.fetch(user, FETCH);
        Group group = user.getElement().getGroup();
        if (group instanceof OperatorGroup) {
            group = fetchService.fetch(group, RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
        }
        final BasicGroupSettings settings = group.getBasicSettings();
        return settings.getTransactionPassword().isUsed();
    }

    @SuppressWarnings("unchecked")
    private <U extends User> U resetTransactionPassword(final Class<U> type, final ResetTransactionPasswordDTO dto) {
        User user = dto.getUser();
        if (type != null) {
            user = fetchService.reload(user);
            if (!type.isInstance(user)) {
                throw new UnexpectedEntityException();
            }
        }
        user.setTransactionPassword(null);
        user.setTransactionPasswordStatus(dto.isAllowGeneration() ? TransactionPasswordStatus.PENDING : TransactionPasswordStatus.BLOCKED);
        return (U) userDao.update(user);
    }

    private ValidationError resolveValidationError(final boolean loginPassword, final boolean numeric, final Element element, final Object object, final Object property, final String credential) {
        if (StringUtils.isEmpty(credential)) {
            return null;
        }

        final Group group = fetchService.fetch(element.getGroup());
        final BasicGroupSettings settings = group.getBasicSettings();
        RangeConstraint length;
        if (loginPassword) {
            length = settings.getPasswordLength();
        } else {
            final MemberGroup memberGroup = (MemberGroup) group;
            length = memberGroup.getMemberSettings().getPinLength();
        }

        // Validate the password length
        final ValidationError lengthResult = new LengthValidation(length).validate(object, property, credential);
        if (lengthResult != null) {
            return lengthResult;
        }

        final String keyPrefix = loginPassword ? "changePassword.error." : "changePin.error.";

        // Check for characters
        if (numeric && !(group instanceof AdminGroup)) {
            // Must be numeric
            if (!StringUtils.isNumeric(credential)) {
                return new ValidationError(keyPrefix + "mustBeNumeric");
            }
        }

        if (loginPassword && !numeric) {
            // When the virtual keyboard is enabled, make sure that the login password has no special characters
            final AccessSettings accessSettings = settingsService.getAccessSettings();
            if (accessSettings.isVirtualKeyboard() && StringHelper.hasSpecial(credential)) {
                return new ValidationError("changePassword.error.mustContainOnlyLettersOrNumbers");
            }
        }

        // Validate the password policy
        final PasswordPolicy policy = settings.getPasswordPolicy();
        if (policy == null || policy == PasswordPolicy.NONE) {
            // Nothing to enforce
            return null;
        }

        // Check for characters
        if (loginPassword && !numeric) {
            // Keys are hard coded with changePassword.error because pin is always numeric
            switch (policy) {
                case AVOID_OBVIOUS_LETTERS_NUMBERS:
                    // Must include letters and numbers
                    if (!StringHelper.hasDigits(credential) || !StringHelper.hasLetters(credential)) {
                        return new ValidationError("changePassword.error.mustIncludeLettersNumbers");
                    }
                    break;
                case AVOID_OBVIOUS_LETTERS_NUMBERS_SPECIAL:
                    // Must include letters, numbers and special characters
                    if (!StringHelper.hasDigits(credential) || !StringHelper.hasLetters(credential) || !StringHelper.hasSpecial(credential)) {
                        return new ValidationError("changePassword.error.mustIncludeLettersNumbersSpecial");
                    }
                    break;
            }
        }

        // Check for obvious password
        if (isObviousCredential(element, credential)) {
            return new ValidationError(keyPrefix + "obvious");
        }

        return null;
    }

    private UserLoginDTO storeLogin(final String key, final User user, final String remoteAddress, final String sessionId) {
        final Calendar now = Calendar.getInstance();

        // Store the session
        UserLoginDTO userLoginDTO = loginsByUser.get(user);
        if (userLoginDTO == null) {
            userLoginDTO = new UserLoginDTO(user);
            loginsByUser.put(user, userLoginDTO);
        }
        final SessionDTO sessionDTO = new SessionDTO(sessionId, now, remoteAddress);
        userLoginDTO.addSession(sessionDTO);
        sessions.put(sessionId, sessionDTO);

        // Index logged element by group
        final Element element = user.getElement();
        final Group group = element.getGroup();
        Set<Element> elements = loggedElementsByGroup.get(group);
        if (elements == null) {
            elements = Collections.synchronizedSet(new HashSet<Element>());
            loggedElementsByGroup.put(group, elements);
        }
        elements.add(element);

        // Initialize the login data
        LoggedUser.init(user);
        loggingHandler.traceLogin(remoteAddress, user, sessionId);

        // Initialize the permission denied counter
        permissionDeniedCounter.remove(key);

        // Save an entry in the login history
        final LoginHistoryLog loginHistoryLog = new LoginHistoryLog();
        loginHistoryLog.setUser(user);
        loginHistoryLog.setDate(now);
        loginHistoryLog.setRemoteAddress(remoteAddress);
        loginHistoryDao.insert(loginHistoryLog);

        return userLoginDTO;
    }

    private User storeLogout(final SessionDTO dto) {
        final UserLoginDTO login = dto.getLogin();
        if (login == null) {
            return null;
        }
        // If is the last session, logout the user
        if (login.getSessions() != null && login.getSessions().size() == 1) {
            return storeLogout(login);
        }

        // Remove the session
        final String sessionId = dto.getSessionId();
        sessions.remove(sessionId);
        login.removeSession(sessionId);

        // Trace to the log file
        final User user = login.getUser();
        loggingHandler.traceLogout(dto.getRemoteAddress(), user, sessionId);

        return user;
    }

    private User storeLogout(final UserLoginDTO dto) {

        User user = dto.getUser();

        // Remove each session
        final Collection<SessionDTO> sessions = new ArrayList<SessionDTO>(dto.getSessions());
        Calendar lastLogin = null;
        for (final SessionDTO session : sessions) {
            // Find the last login
            final Calendar loginDate = session.getLoginDate();
            if (lastLogin == null || lastLogin.before(loginDate)) {
                lastLogin = loginDate;
            }

            // Remove this session
            final String sessionId = session.getSessionId();
            this.sessions.remove(sessionId);
            dto.removeSession(sessionId);

            // Trace to the log file
            loggingHandler.traceLogout(session.getRemoteAddress(), user, sessionId);
        }

        // Logout the user
        loginsByUser.remove(user);

        // Remove the user from the group indexing
        final Element element = user.getElement();
        final Set<Element> elements = loggedElementsByGroup.get(element.getGroup());
        if (elements != null) {
            elements.remove(element);
        }

        // Store the last login
        if (lastLogin != null) {
            user = fetchService.reload(user);
            user.setLastLogin(lastLogin);
            user = userDao.update(user);
        }

        // Clean up the LoggedUser
        if (LoggedUser.isValid() && user.equals(LoggedUser.user())) {
            LoggedUser.cleanup();
        }

        return user;
    }

    private void validateChangePassword(final ChangeLoginPasswordDTO params, final boolean myPassword) {
        final Validator validator = new Validator("changePassword");
        if (myPassword) {
            // The old password is required if it is not expired
            if (!hasPasswordExpired(LoggedUser.user())) {
                validator.property("oldPassword").required();
            }
        } else {
            validator.property("user").required();
        }
        final User user = fetchService.fetch(params.getUser(), RelationshipHelper.nested(User.Relationships.ELEMENT, Element.Relationships.GROUP));
        if (user != null) {
            validator.property("newPassword").required().add(new LoginPasswordValidation(user.getElement()));
            validator.property("newPasswordConfirmation").required();
        }
        validator.general(new GeneralValidation() {
            private static final long serialVersionUID = -4110708889147050967L;

            public ValidationError validate(final Object object) {
                final ChangeLoginPasswordDTO params = (ChangeLoginPasswordDTO) object;
                final String newPassword = params.getNewPassword();
                final String newPasswordConfirmation = params.getNewPasswordConfirmation();
                if (StringUtils.isNotEmpty(newPassword) && StringUtils.isNotEmpty(newPasswordConfirmation) && !newPassword.equals(newPasswordConfirmation)) {
                    return new ValidationError("errors.passwords");
                }
                return null;
            }
        });
        validator.validate(params);
    }

    private void validateChangePin(final ChangePinDTO params, final boolean myPin) {
        final Validator validator = new Validator("changePin");
        if (myPin) {
            final MemberGroup group = LoggedUser.group();
            final boolean isTP = group.getBasicSettings().getTransactionPassword().isUsed();
            final String key = isTP ? "channel.credentials.TRANSACTION_PASSWORD" : "channel.credentials.LOGIN_PASSWORD";
            validator.property("credentials").key(key).required();
        } else {
            validator.property("user").required();
        }
        final MemberUser user = fetchService.fetch(params.getUser(), User.Relationships.ELEMENT);
        if (user != null) {
            validator.property("newPin").required().add(new PinValidation(user.getMember()));
            validator.property("newPinConfirmation").required();
        }
        validator.general(new GeneralValidation() {
            private static final long serialVersionUID = -4110708889147050967L;

            public ValidationError validate(final Object object) {
                final ChangePinDTO params = (ChangePinDTO) object;
                final String newPin = params.getNewPin();
                final String newPinConfirmation = params.getNewPinConfirmation();
                if (StringUtils.isNotEmpty(newPin) && StringUtils.isNotEmpty(newPinConfirmation) && !newPin.equals(newPinConfirmation)) {
                    return new ValidationError("changePin.error.pinsAreNotEqual");
                }
                return null;
            }
        });
        validator.validate(params);
    }

}
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
package nl.strohalm.cyclos.utils;

import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.OperatorUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.accounts.loans.LoanGroupQuery;
import nl.strohalm.cyclos.entities.accounts.pos.MemberPos;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DocumentQuery;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Reference.Nature;
import nl.strohalm.cyclos.entities.members.records.MemberRecordType;
import nl.strohalm.cyclos.entities.members.records.MemberRecordTypeQuery;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.exceptions.AccessDeniedException;
import nl.strohalm.cyclos.exceptions.LoggedOutException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.ChannelService;
import nl.strohalm.cyclos.services.access.exceptions.LoginException;
import nl.strohalm.cyclos.services.access.exceptions.NotConnectedException;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService;
import nl.strohalm.cyclos.services.customization.DocumentService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberRecordTypeService;
import nl.strohalm.cyclos.services.elements.ReferenceService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.loangroups.LoanGroupService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.query.PageHelper;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Helper class for login
 * @author luis
 */
public class LoginHelper {

    /**
     * Returns the currently logged user
     */
    public static User getLoggedUser(final HttpServletRequest request) {
        final HttpSession session = request.getSession(false);
        return (User) (session == null ? null : session.getAttribute("loggedUser"));
    }

    /**
     * Perform the login itself
     */
    public static User login(final Class<? extends User> requiredUserClass, final String principalTypeString, final String memberUsername, final String principal, final String password, final String channel, final HttpServletRequest request) throws LoginException {
        final HttpSession session = request.getSession();
        final boolean isPosWeb = Boolean.TRUE.equals(session.getAttribute("isPosWeb"));
        final WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext());
        final ChannelService channelService = SpringHelper.bean(applicationContext, "channelService");
        final ElementService elementService = SpringHelper.bean(applicationContext, "elementService");
        final AccessService accessService = SpringHelper.bean(applicationContext, "accessService");
        final GroupService groupService = SpringHelper.bean(applicationContext, "groupService");
        final FetchService fetchService = SpringHelper.bean(applicationContext, "fetchService");
        final AccountService accountService = SpringHelper.bean(applicationContext, "accountService");
        final PermissionService permissionService = SpringHelper.bean(applicationContext, "permissionService");
        final DocumentService documentService = SpringHelper.bean(applicationContext, "documentService");
        final LoanGroupService loanGroupService = SpringHelper.bean(applicationContext, "loanGroupService");
        final ReferenceService referenceService = SpringHelper.bean(applicationContext, "referenceService");
        final MemberRecordTypeService memberRecordTypeService = SpringHelper.bean(applicationContext, "memberRecordTypeService");
        final GuaranteeService guaranteeService = SpringHelper.bean(applicationContext, "guaranteeService");
        final AccessSettings accessSettings = SettingsHelper.getAccessSettings(request);
        final String remoteAddress = request.getRemoteAddr();

        final PrincipalType principalType = channelService.resolvePrincipalType(channel, principalTypeString);

        // Validate the user
        String usernameToVerify = principal;
        if (principalType.getPrincipal() != Principal.USER) {
            try {
                Member member;
                member = elementService.loadByPrincipal(principalType, principal, Element.Relationships.USER, Element.Relationships.GROUP);
                usernameToVerify = member.getUsername();
            } catch (final EntityNotFoundException e) {
                usernameToVerify = "";
            }
        }
        final User user = accessService.verifyLogin(memberUsername, usernameToVerify, remoteAddress);
        if (!requiredUserClass.isInstance(user)) {
            throw new AccessDeniedException();
        }

        // Find the user nature
        final Group group = user.getElement().getGroup();
        final boolean isAdmin = group instanceof AdminGroup;
        final boolean isMember = group instanceof MemberGroup;
        final boolean isBroker = group instanceof BrokerGroup;
        final boolean isOperator = group instanceof OperatorGroup;

        // Check if the administrator is allowed to login
        if (isAdmin) {
            if (!new WhitelistValidator(accessSettings.getAdministrationWhitelist()).isAllowed(request)) {
                throw new AccessDeniedException();
            }
        }

        // Login the user
        accessService.login(user, password, channel, remoteAddress, session.getId());

        // Apply the session timeout
        final TimePeriod timeout = isPosWeb ? accessSettings.getPoswebTimeout() : isMember ? accessSettings.getMemberTimeout() : accessSettings.getAdminTimeout();
        int timeoutSeconds = (int) timeout.getValueIn(TimePeriod.Field.SECONDS);
        if (timeoutSeconds <= 0) {
            timeoutSeconds = -1;
        }
        session.setMaxInactiveInterval(timeoutSeconds);

        // If is a member, determine if the member has accounts, documents, loan groups and memberPos
        boolean hasAccounts = false;
        boolean singleAccount = false;
        boolean hasDocuments = false;
        boolean hasLoanGroups = false;
        boolean hasGeneralReferences = false;
        boolean hasTransactionFeedbacks = false;
        boolean hasPin = false;
        boolean hasExternalChannels = false;
        boolean hasCards = false;
        boolean hasPos = false;
        if (isMember || isOperator) {
            Member member;
            if (isMember) {
                member = ((MemberUser) user).getMember();

                // Get the accessible channels
                final MemberGroup memberGroup = fetchService.fetch(member.getMemberGroup(), MemberGroup.Relationships.CHANNELS);
                hasPin = groupService.usesPin(memberGroup);
                for (final Channel current : memberGroup.getChannels()) {
                    if (!Channel.WEB.equals(current.getInternalName())) {
                        hasExternalChannels = true;
                        break;
                    }
                }

                if (!member.getPosDevices().isEmpty()) {
                    hasPos = true;
                    if (member.getPosDevices().size() == 1) {
                        final Collection<MemberPos> memberPos = member.getPosDevices();
                        for (final MemberPos mpos : memberPos) {
                            session.setAttribute("uniqueMemberPosId ", mpos.getPos().getId());
                        }

                    }
                }

            } else {
                member = ((OperatorUser) user).getOperator().getMember();
            }
            // Fetch broker
            member = fetchService.fetch(member, Member.Relationships.BROKER);
            final MemberGroup memberGroup = member.getMemberGroup();

            // Check if the member has accounts
            final List<? extends Account> accounts = accountService.getAccounts(member);
            hasAccounts = !accounts.isEmpty();
            singleAccount = accounts.size() == 1;
            // Check if the member has documents
            if (permissionService.checkPermission(memberGroup, "memberDocuments", "view")) {
                hasDocuments = true;
            } else {
                final DocumentQuery documentQuery = new DocumentQuery();
                documentQuery.setNature(Document.Nature.MEMBER);
                documentQuery.setMember(member);
                documentQuery.setPageForCount();
                hasDocuments = (PageHelper.getTotalCount(documentService.search(documentQuery)) > 0);
            }
            if (isMember) {
                // Check if the member has loan groups
                final LoanGroupQuery lgq = new LoanGroupQuery();
                lgq.setPageForCount();
                lgq.setMember(member);
                hasLoanGroups = PageHelper.getTotalCount(loanGroupService.searchForMember(lgq)) > 0;
            }
            // Check if the user has references
            final Collection<Nature> referenceNatures = referenceService.getNaturesByGroup(memberGroup);
            hasGeneralReferences = referenceNatures.contains(Nature.GENERAL);
            hasTransactionFeedbacks = referenceNatures.contains(Nature.TRANSACTION);

            // Check if the user can have guarantees
            try {
                final Collection<GuaranteeType.Model> guaranteeModels = guaranteeService.getRelatedGuaranteeModels(member);
                session.setAttribute("loggedMemberHasGuarantees", guaranteeModels.size() > 0);
            } catch (final Exception e) {
                // Ignore
            }

            // Check if the user has cards
            hasCards = member.getCards().isEmpty() ? false : true;
        }

        if (isAdmin || isBroker) {
            // Retrieve the member record types the logged user can see on the menu
            final MemberRecordTypeQuery query = new MemberRecordTypeQuery();
            if (isAdmin) {
                query.setViewableByAdminGroup((AdminGroup) group);
            } else {
                query.setViewableByBrokerGroup((BrokerGroup) group);
            }
            query.setShowMenuItem(true);
            final List<MemberRecordType> types = memberRecordTypeService.search(query);
            session.setAttribute("memberRecordTypesInMenu", types);
        }

        final String actionPrefix = "/" + (isAdmin ? "admin" : isMember ? "member" : "operator");

        // Set the session attributes
        session.setAttribute("loggedUser", user);
        session.setAttribute("loggedElement", user.getElement());
        session.setAttribute("isAdmin", isAdmin);
        session.setAttribute("isMember", isMember);
        session.setAttribute("isBroker", isBroker);
        session.setAttribute("isOperator", isOperator);
        session.setAttribute("isBuyer", guaranteeService.isBuyer());
        session.setAttribute("isSeller", guaranteeService.isSeller());
        session.setAttribute("isIssuer", guaranteeService.isIssuer());
        session.setAttribute("loggedMemberHasAccounts", hasAccounts);
        session.setAttribute("loggedMemberHasSingleAccount", singleAccount);
        session.setAttribute("loggedMemberHasDocuments", hasDocuments);
        session.setAttribute("loggedMemberHasLoanGroups", hasLoanGroups);
        session.setAttribute("loggedMemberHasGeneralReferences", hasGeneralReferences);
        session.setAttribute("loggedMemberHasTransactionFeedbacks", hasTransactionFeedbacks);
        session.setAttribute("hasPin", hasPin);
        session.setAttribute("hasCards", hasCards);
        session.setAttribute("hasPos", hasPos);
        session.setAttribute("hasExternalChannels", hasExternalChannels);
        session.setAttribute("actionPrefix", actionPrefix);
        session.setAttribute("pathPrefix", "/do" + actionPrefix);
        session.setAttribute("navigation", new Navigation(session));

        // Return the logged user
        return user;
    }

    /**
     * Returns the currently logged user, ensuring there is one
     */
    public static User validateLoggedUser(final HttpServletRequest request) {
        final HttpSession session = request.getSession();

        final WebApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(session.getServletContext());
        final AccessService accessService = (AccessService) applicationContext.getBean("accessService");

        // Find the logged user
        final User user = getLoggedUser(request);
        if (user == null) {
            throw new LoggedOutException();
        }
        // Find the registered logged user for the session id
        User serviceUser;
        try {
            serviceUser = accessService.getLoggedUser(session.getId());
        } catch (final NotConnectedException e) {
            throw new LoggedOutException();
        }
        // The web container session indicates there is an user, but there's no tracked session: invalidate the session's user
        if (user != null && serviceUser == null) {
            session.removeAttribute("loggedUser");
            throw new LoggedOutException();
        } else {
            // Ensure they match
            final boolean valid = user != null && user.equals(serviceUser);
            if (!valid) {
                session.invalidate();
                throw new AccessDeniedException();
            }
        }
        return user;
    }
}

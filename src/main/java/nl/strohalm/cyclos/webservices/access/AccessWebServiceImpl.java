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
package nl.strohalm.cyclos.webservices.access;

import javax.jws.WebService;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.CredentialsAlreadyUsedException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.webservices.PrincipalParameters;
import nl.strohalm.cyclos.webservices.WebServiceContext;
import nl.strohalm.cyclos.webservices.utils.server.ChannelHelper;
import nl.strohalm.cyclos.webservices.utils.server.MemberHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Default implementation for AccessWebService
 * 
 * @author luis
 */
@WebService(name = "access", serviceName = "access")
public class AccessWebServiceImpl implements AccessWebService {

    private SettingsService settingsService;
    private ChannelHelper   channelHelper;
    private ElementService  elementService;
    private AccessService   accessService;
    private MemberHelper    memberHelper;

    public ChangeCredentialsStatus changeCredentials(final ChangeCredentialsParameters params) {

        // Get and validate the parameters
        final String principal = params == null ? null : StringUtils.trimToNull(params.getPrincipal());
        final String oldCredentials = params == null ? null : StringUtils.trimToNull(params.getOldCredentials());
        final String newCredentials = params == null ? null : StringUtils.trimToNull(params.getNewCredentials());
        if (principal == null || oldCredentials == null || newCredentials == null) {
            throw new ValidationException();
        }

        final Channel channel = WebServiceContext.getChannel();
        // Only login password and pin can be changed from here
        final Credentials credentials = channel.getCredentials();
        if (credentials != Credentials.LOGIN_PASSWORD && credentials != Credentials.PIN) {
            throw new PermissionDeniedException();
        }
        final PrincipalType principalType = channelHelper.resolvePrincipalType(params.getPrincipalType());

        // Load the member
        Member member;
        try {
            member = elementService.loadByPrincipal(principalType, principal, Element.Relationships.GROUP, Element.Relationships.USER);
        } catch (final Exception e) {
            return ChangeCredentialsStatus.MEMBER_NOT_FOUND;
        }

        // Check the current credentials
        try {
            accessService.checkCredentials(channel, member.getMemberUser(), oldCredentials, WebServiceContext.getRequest().getRemoteAddr(), WebServiceContext.getMember());
        } catch (final InvalidCredentialsException e) {
            return ChangeCredentialsStatus.INVALID_CREDENTIALS;
        } catch (final BlockedCredentialsException e) {
            return ChangeCredentialsStatus.BLOCKED_CREDENTIALS;
        }

        // The login password is numeric depending on settings. Others, are always numeric
        boolean numericPassword;
        if (credentials == Credentials.LOGIN_PASSWORD) {
            numericPassword = settingsService.getAccessSettings().isNumericPassword();
        } else {
            numericPassword = true;
        }
        if (numericPassword && !StringUtils.isNumeric(newCredentials)) {
            return ChangeCredentialsStatus.INVALID_CHARACTERS;
        }

        // Change the password
        try {
            accessService.changeMemberCredentialsByWebService(member.getMemberUser(), WebServiceContext.getClient(), newCredentials);
        } catch (final ValidationException e) {
            if (CollectionUtils.isNotEmpty(e.getGeneralErrors())) {
                // Actually, the only possible general error is that it is the same as another credential.
                // In this case, we return CREDENTIALS_ALREADY_USED
                return ChangeCredentialsStatus.CREDENTIALS_ALREADY_USED;
            }
            // There is a property error. Let's scrap it to determine which is it
            try {
                final ValidationError error = e.getErrorsByProperty().values().iterator().next().iterator().next();
                final String key = error.getKey();
                if (key.endsWith("obvious")) {
                    // The password is too simple
                    return ChangeCredentialsStatus.TOO_SIMPLE;
                } else {
                    // Either must be numeric / must contain letters and numbers / must contain letters, numbers and special
                    throw new Exception();
                }
            } catch (final Exception e1) {
                // If there is some kind of unexpected validation result, just return as invalid
                return ChangeCredentialsStatus.INVALID_CHARACTERS;
            }
        } catch (final CredentialsAlreadyUsedException e) {
            return ChangeCredentialsStatus.CREDENTIALS_ALREADY_USED;
        }
        return ChangeCredentialsStatus.SUCCESS;
    }

    public CredentialsStatus checkCredentials(final CheckCredentialsParameters params) {
        final Member member = WebServiceContext.getMember();
        if (member != null) {
            throw new PermissionDeniedException();
        }
        try {
            final Channel channel = WebServiceContext.getChannel();
            final String channelName = channel.getInternalName();
            final PrincipalType principalType = channelHelper.resolvePrincipalType(params.getPrincipalType());
            String credentials = params.getCredentials();
            if (channel.getCredentials() == Credentials.TRANSACTION_PASSWORD) {
                credentials = credentials.toUpperCase();
            }
            final String remoteAddr = WebServiceContext.getRequest().getRemoteAddr();
            accessService.checkCredentials(channelName, principalType, params.getPrincipal(), credentials, remoteAddr, null);
            return CredentialsStatus.VALID;
        } catch (final BlockedCredentialsException e) {
            return CredentialsStatus.BLOCKED;
        } catch (final Exception e) {
            return CredentialsStatus.INVALID;
        }
    }

    public boolean isChannelEnabledForMember(final PrincipalParameters params) {
        Member member = WebServiceContext.getMember();
        if (member != null) {
            throw new PermissionDeniedException();
        }
        final PrincipalType principalType = channelHelper.resolvePrincipalType(params.getPrincipalType());
        member = elementService.loadByPrincipal(principalType, params.getPrincipal());
        return memberHelper.isChannelEnabledForMember(member);
    }

    public void setAccessService(final AccessService accessService) {
        this.accessService = accessService;
    }

    public void setChannelHelper(final ChannelHelper channelHelper) {
        this.channelHelper = channelHelper;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

}

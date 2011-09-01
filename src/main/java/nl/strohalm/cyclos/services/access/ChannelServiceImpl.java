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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nl.strohalm.cyclos.dao.access.ChannelDAO;
import nl.strohalm.cyclos.dao.access.ChannelPrincipalDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.ChannelPrincipal;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.access.Channel.Principal;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Implementation for channel service
 * @author luis
 */
public class ChannelServiceImpl implements ChannelService, InitializingBean {

    private FetchService         fetchService;
    private Map<String, Channel> cachedChannels = new ConcurrentHashMap<String, Channel>();
    private ChannelDAO           channelDao;
    private ChannelPrincipalDAO  channelPrincipalDao;
    private CustomFieldService   customFieldService;
    private SettingsService      settingsService;
    private TransactionTemplate  transactionTemplate;

    public void afterPropertiesSet() throws Exception {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus arg0) {
                refreshCache();
            }
        });
    }

    public boolean allowsPaymentRequest(final String channel) {
        return !isBuiltin(channel);
    }

    public Set<Credentials> getPossibleCredentials(final Channel channel) {
        final String internalName = channel.getInternalName();
        if (Channel.WEB.equals(internalName)) {
            // Main web allows only default
            return EnumSet.of(Credentials.DEFAULT);
        } else if (Arrays.asList(Channel.WAP1, Channel.WAP2, Channel.WEBSHOP).contains(internalName)) {
            // Wap1/2, and WebShop disallows card security code
            return EnumSet.of(Credentials.DEFAULT, Credentials.LOGIN_PASSWORD, Credentials.TRANSACTION_PASSWORD, Credentials.PIN);
        } else {
            // The others don't allow default, as it must be a single shot, and default has 2 passwords
            return EnumSet.of(Credentials.LOGIN_PASSWORD, Credentials.TRANSACTION_PASSWORD, Credentials.PIN, Credentials.CARD_SECURITY_CODE);
        }
    }

    public Channel getSmsChannel() {
        final String name = settingsService.getLocalSettings().getSmsChannelName();
        return StringUtils.isEmpty(name) ? null : loadByInternalName(name);
    }

    public boolean isBuiltin(final String channel) {
        try {
            return Channel.listBuiltin().contains(channel);
        } catch (final RuntimeException e) {
            return false;
        }
    }

    public List<Channel> list() {
        if (cachedChannels.isEmpty()) {
            refreshCache();
        }
        return new ArrayList<Channel>(cachedChannels.values());
    }

    public List<Channel> listBuiltin() {
        return filterChannels(true);
    }

    public List<Channel> listExternal() {
        final List<Channel> channels = list();
        channels.remove(loadByInternalName(Channel.WEB));
        return channels;
    }

    public List<Channel> listNonBuiltin() {
        return filterChannels(false);
    }

    public List<Channel> listSupportingPaymentRequest() {
        final List<Channel> channels = new ArrayList<Channel>();
        for (final Channel channel : list()) {
            if (channel.isPaymentRequestSupported()) {
                channels.add(channel);
            }
        }
        return channels;
    }

    public Channel load(final Long id) throws EntityNotFoundException {
        return channelDao.load(id);
    }

    public Channel loadByInternalName(final String name) throws EntityNotFoundException {
        if (cachedChannels.isEmpty()) {
            refreshCache();
        }
        final Channel channel = cachedChannels.get(name);
        if (channel == null) {
            throw new EntityNotFoundException(Channel.class);
        }
        return channel;
    }

    @SuppressWarnings("unchecked")
    public List<MemberCustomField> possibleCustomFieldsAsPrincipal() {
        final List<MemberCustomField> allMemberFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        final List<MemberCustomField> possible = new ArrayList<MemberCustomField>();
        for (final MemberCustomField field : allMemberFields) {
            if (field.getType() == CustomField.Type.STRING && field.getValidation().isUnique()) {
                possible.add(field);
            }
        }
        return possible;
    }

    public int remove(final Long... ids) {
        invalidateCache();
        return channelDao.delete(ids);
    }

    public PrincipalType resolvePrincipalType(final String principalTypeString) {
        PrincipalType principalType = null;
        try {
            // Try by principal enum
            final Principal principal = Principal.valueOf(principalTypeString);
            if (principal != Principal.CUSTOM_FIELD) {
                // Custom fields should be resolved by their internal name
                principalType = new PrincipalType(principal);
            }
        } catch (final Exception e) {
            // Try by custom field
            final List<MemberCustomField> possibleFields = possibleCustomFieldsAsPrincipal();
            final MemberCustomField customField = CustomFieldHelper.findByInternalName(possibleFields, principalTypeString);
            if (customField != null) {
                principalType = new PrincipalType(customField);
            }
        }
        return principalType;
    }

    public PrincipalType resolvePrincipalType(final String channelName, final String principalTypeString) {
        final Channel channel = loadByInternalName(channelName);
        PrincipalType principalType = resolvePrincipalType(principalTypeString);
        // Ensure the principal is supported by the channel
        if (!channel.getPrincipalTypes().contains(principalType)) {
            principalType = channel.getDefaultPrincipalType();
        }
        return principalType == null ? Channel.DEFAULT_PRINCIPAL_TYPE : principalType;
    }

    public Channel save(Channel channel) {
        validate(channel);
        invalidateCache();

        // Ensure there is a default
        final Collection<ChannelPrincipal> principals = channel.getPrincipals();
        boolean hasDefault = false;
        ChannelPrincipal user = null;
        for (final ChannelPrincipal channelPrincipal : principals) {
            if (channelPrincipal.getPrincipal() == Principal.USER) {
                user = channelPrincipal;
            }
            if (channelPrincipal.isDefault()) {
                hasDefault = true;
            }
        }
        if (!hasDefault) {
            // When no default, set preferentially the USER, or the first one
            if (user != null) {
                user.setDefault(true);
            } else {
                principals.iterator().next().setDefault(true);
            }
        }
        // Ensure that the web channel has USER set, otherwise, admins wouldn't login
        if (Channel.WEB.equals(channel.getInternalName())) {
            if (user == null) {
                user = new ChannelPrincipal();
                user.setChannel(channel);
                user.setPrincipal(Principal.USER);
                principals.add(user);
            }
        }

        // Ensure there's a valid credentials
        final Set<Credentials> possibleCredentials = getPossibleCredentials(channel);
        if (!possibleCredentials.contains(channel.getCredentials())) {
            channel.setCredentials(possibleCredentials.iterator().next());
        }

        if (channel.isTransient()) {
            channel = channelDao.insert(channel);
        } else {
            // Load the current data to ensure that restrictions are enforced
            final Channel current = load(channel.getId());

            // Ensure the internal name does not changes
            final String internalName = current.getInternalName();
            if (!allowsPaymentRequest(internalName)) {
                // If payment request is not allowed, ensure the WS url is null
                channel.setPaymentRequestWebServiceUrl(null);
            }
            channel.setPrincipals(null);
            channel = channelDao.update(channel);
            channelPrincipalDao.deleteAllFrom(channel);
        }

        // Insert the channel principals
        for (final ChannelPrincipal channelPrincipal : principals) {
            channelPrincipalDao.insert(channelPrincipal);
        }

        return channel;
    }

    public void setChannelDao(final ChannelDAO channelDao) {
        this.channelDao = channelDao;
    }

    public void setChannelPrincipalDao(final ChannelPrincipalDAO channelPrincipalDao) {
        this.channelPrincipalDao = channelPrincipalDao;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void setTransactionTemplate(final TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void validate(final Channel channel) throws ValidationException {
        final Validator validator = new Validator("channel");
        validator.property("internalName").required().maxLength(50);
        validator.property("displayName").required().maxLength(100);
        validator.property("principals").required().add(new PropertyValidation() {
            private static final long serialVersionUID = -2500914238715839704L;

            @SuppressWarnings("unchecked")
            public ValidationError validate(final Object object, final Object property, final Object value) {
                final Collection<ChannelPrincipal> principals = (Collection<ChannelPrincipal>) value;
                if (CollectionUtils.isEmpty(principals)) {
                    // Will already fail validation by the required
                    return null;
                }
                for (final ChannelPrincipal channelPrincipal : principals) {
                    if (channelPrincipal.getPrincipal() == null) {
                        return new RequiredError();
                    } else if (channelPrincipal.getPrincipal() == Principal.CUSTOM_FIELD) {
                        final MemberCustomField customField = fetchService.fetch(channelPrincipal.getCustomField());
                        if (customField == null) {
                            return new RequiredError();
                        }
                        if (customField.getType() != CustomField.Type.STRING) {
                            return new InvalidError();
                        }
                        if (!customField.getValidation().isUnique()) {
                            return new InvalidError();
                        }
                    }
                }
                return null;
            }
        });
        final Set<Credentials> possibleCredentials = getPossibleCredentials(channel);
        if (channel.getDefaultPrincipalType().getPrincipal() != Principal.CARD) {
            // Ensure card security code is only possible if principal could be card
            possibleCredentials.remove(Credentials.CARD_SECURITY_CODE);
        }
        if (possibleCredentials.size() > 1) {
            // Only required if there's choice
            validator.property("credentials").required().anyOf(possibleCredentials);
        }
        validator.validate(channel);
    }

    /**
     * 
     * @param builtin if it's false then the built-in are removed
     * @return
     */
    private List<Channel> filterChannels(final boolean builtin) {
        final List<Channel> channels = list();
        // Remove those which are built-in
        for (final Iterator<Channel> iterator = channels.iterator(); iterator.hasNext();) {
            final Channel channel = iterator.next();
            if (builtin && !isBuiltin(channel.getInternalName()) || !builtin && isBuiltin(channel.getInternalName())) {
                iterator.remove();
            }
        }
        return channels;
    }

    private void invalidateCache() {
        cachedChannels.clear();
    }

    private void refreshCache() {
        synchronized (cachedChannels) {
            if (cachedChannels.isEmpty()) {
                for (Channel channel : channelDao.listAll()) {
                    channel = fetchService.fetch(channel, Channel.Relationships.PRINCIPALS);
                    for (final ChannelPrincipal principal : channel.getPrincipals()) {
                        principal.setCustomField(fetchService.fetch(principal.getCustomField()));
                    }
                    cachedChannels.put(channel.getInternalName(), channel);
                }
            }
        }
    }

}

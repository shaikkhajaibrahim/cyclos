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
package nl.strohalm.cyclos.services.settings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.settings.SettingDAO;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.entities.settings.AlertSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LogSettings;
import nl.strohalm.cyclos.entities.settings.MailSettings;
import nl.strohalm.cyclos.entities.settings.MailTranslation;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.settings.Setting;
import nl.strohalm.cyclos.entities.settings.Setting.Type;
import nl.strohalm.cyclos.entities.settings.events.AccessSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.AccessSettingsEvent;
import nl.strohalm.cyclos.entities.settings.events.AlertSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.AlertSettingsEvent;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.entities.settings.events.LogSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LogSettingsEvent;
import nl.strohalm.cyclos.entities.settings.events.MailSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.MailSettingsEvent;
import nl.strohalm.cyclos.entities.settings.events.MailTranslationChangeListener;
import nl.strohalm.cyclos.entities.settings.events.MailTranslationEvent;
import nl.strohalm.cyclos.entities.settings.events.MessageSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.MessageSettingsEvent;
import nl.strohalm.cyclos.entities.settings.events.SettingsChangeListener;
import nl.strohalm.cyclos.scheduling.SchedulingHandler;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.exceptions.SelectedSettingTypeNotInFileException;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.XmlHelper;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation class for settings service
 * @author luis
 * @author Jefferson Magno
 */
public class SettingsServiceImpl implements SettingsService {

    private SettingDAO                               settingDao;

    // Handlers for each settings type
    private Map<Setting.Type, SettingsHandler<?>>    handlersMap;
    private SettingsHandler<AccessSettings>          accessSettingsHandler;
    private SettingsHandler<AlertSettings>           alertSettingsHandler;
    private SettingsHandler<LocalSettings>           localSettingsHandler;
    private SettingsHandler<LogSettings>             logSettingsHandler;
    private SettingsHandler<MailSettings>            mailSettingsHandler;
    private SettingsHandler<MailTranslation>         mailTranslationHandler;
    private SettingsHandler<MessageSettings>         messageSettingsHandler;

    // Scheduling handler notified when scheduled tasks time changes
    private SchedulingHandler                        schedulingHandler;

    // Registered event listeners
    private final Set<AccessSettingsChangeListener>  accessSettingsListeners  = new HashSet<AccessSettingsChangeListener>();
    private final Set<AlertSettingsChangeListener>   alertSettingsListeners   = new HashSet<AlertSettingsChangeListener>();
    private final Set<LocalSettingsChangeListener>   localSettingsListeners   = new HashSet<LocalSettingsChangeListener>();
    private final Set<LogSettingsChangeListener>     logSettingsListeners     = new HashSet<LogSettingsChangeListener>();
    private final Set<MailSettingsChangeListener>    mailSettingsListeners    = new HashSet<MailSettingsChangeListener>();
    private final Set<MailTranslationChangeListener> mailTranslationListeners = new HashSet<MailTranslationChangeListener>();
    private final Set<MessageSettingsChangeListener> messageSettingsListeners = new HashSet<MessageSettingsChangeListener>();

    private final String                             ROOT_ELEMENT             = "cyclos-settings";
    private final String                             SETTINGS_ELEMENT         = "settings";
    private final String                             SETTING_ELEMENT          = "setting";

    // Settings that cannot be imported / exported
    private List<String>                             ignoreSettings;

    private FetchService                             fetchService;

    public void addListener(final SettingsChangeListener listener) {
        if (listener instanceof AccessSettingsChangeListener) {
            accessSettingsListeners.add((AccessSettingsChangeListener) listener);
        } else if (listener instanceof AlertSettingsChangeListener) {
            alertSettingsListeners.add((AlertSettingsChangeListener) listener);
        } else if (listener instanceof LocalSettingsChangeListener) {
            localSettingsListeners.add((LocalSettingsChangeListener) listener);
        } else if (listener instanceof LogSettingsChangeListener) {
            logSettingsListeners.add((LogSettingsChangeListener) listener);
        } else if (listener instanceof MailSettingsChangeListener) {
            mailSettingsListeners.add((MailSettingsChangeListener) listener);
        } else if (listener instanceof MailTranslationChangeListener) {
            mailTranslationListeners.add((MailTranslationChangeListener) listener);
        } else if (listener instanceof MessageSettingsChangeListener) {
            messageSettingsListeners.add((MessageSettingsChangeListener) listener);
        }
    }

    public String exportToXml(final Collection<Type> types) {
        final LocalSettings localSettings = getLocalSettings();
        final StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"").append(localSettings.getCharset()).append("\"?>\n");
        xml.append('<').append(ROOT_ELEMENT).append(">\n");
        for (final Type type : types) {
            appendSettingType(xml, type);
        }
        xml.append("</").append(ROOT_ELEMENT).append(">\n");
        return xml.toString();
    }

    public AccessSettings getAccessSettings() {
        return accessSettingsHandler.get();
    }

    public AlertSettings getAlertSettings() {
        return alertSettingsHandler.get();
    }

    public LocalSettings getLocalSettings() {
        return localSettingsHandler.get();
    }

    public LogSettings getLogSettings() {
        return logSettingsHandler.get();
    }

    public MailSettings getMailSettings() {
        return mailSettingsHandler.get();
    }

    public MailTranslation getMailTranslation() {
        return mailTranslationHandler.get();
    }

    public MessageSettings getMessageSettings() {
        return messageSettingsHandler.get();
    }

    public MemberCustomField getSmsCustomField() {
        final Long id = getLocalSettings().getSmsCustomFieldId();
        if (id == null) {
            return null;
        }
        final MemberCustomField reference = EntityHelper.reference(MemberCustomField.class, id);
        return fetchService.fetch(reference);
    }

    public List<?> importFromXml(final String xml, final Collection<Setting.Type> types) {
        final Document doc = XmlHelper.readDocument(xml);
        final Element root = doc.getDocumentElement();
        final List<Element> settingTypesNodes = XmlHelper.getChilden(root, SETTINGS_ELEMENT);
        final Set<Setting.Type> typesImported = new HashSet<Setting.Type>();
        final List<Object> settings = new ArrayList<Object>();
        for (final Element settingTypeNode : settingTypesNodes) {
            final String settingTypeName = settingTypeNode.getAttribute("type");
            final Setting.Type type = CoercionHelper.coerce(Setting.Type.class, settingTypeName);
            if (!types.contains(type)) {
                continue;
            }
            final Map<String, String> values = new HashMap<String, String>();
            final List<Element> settingsNodes = XmlHelper.getChilden(settingTypeNode, SETTING_ELEMENT);
            for (final Element settingNode : settingsNodes) {
                final String settingName = settingNode.getAttribute("name");
                // Setting in ignore list, don't import it
                if (type == Setting.Type.LOCAL && ignoreSettings.contains(settingName)) {
                    continue;
                }
                String settingValue;
                try {
                    settingValue = StringUtils.trimToNull(settingNode.getChildNodes().item(0).getNodeValue());
                } catch (final Exception e) {
                    settingValue = null;
                }
                values.put(settingName, settingValue);
            }
            final SettingsHandler<?> settingsHandler = handlersMap.get(type);
            final Object setting = settingsHandler.importFrom(values);
            typesImported.add(type);
            settings.add(setting);
            notifyListener(settings);
        }
        final List<Setting.Type> notImportedTypes = new ArrayList<Setting.Type>();
        for (final Setting.Type type : types) {
            if (!typesImported.contains(type)) {
                notImportedTypes.add(type);
            }
        }
        if (CollectionUtils.isNotEmpty(notImportedTypes)) {
            throw new SelectedSettingTypeNotInFileException(notImportedTypes);
        }

        return settings;
    }

    public void importNew() {
        settingDao.importNew(getLocalSettings().getLocale());
    }

    public void initialize() {
        handlersMap = new HashMap<Setting.Type, SettingsHandler<?>>();
        handlersMap.put(Setting.Type.ACCESS, accessSettingsHandler);
        handlersMap.put(Setting.Type.ALERT, alertSettingsHandler);
        handlersMap.put(Setting.Type.LOCAL, localSettingsHandler);
        handlersMap.put(Setting.Type.LOG, logSettingsHandler);
        handlersMap.put(Setting.Type.MAIL, mailSettingsHandler);
        handlersMap.put(Setting.Type.MAIL_TRANSLATION, mailTranslationHandler);
        handlersMap.put(Setting.Type.MESSAGE, messageSettingsHandler);

        ignoreSettings = new ArrayList<String>();
        ignoreSettings.add("applicationName");
        ignoreSettings.add("language");
    }

    public void reloadTranslation() {
        // Delete all mail and message translations
        settingDao.deleteByType(Setting.Type.MAIL_TRANSLATION, Setting.Type.MESSAGE);
        // Delete some local settings which are also 'translatable'
        for (final Setting setting : settingDao.listByType(Setting.Type.LOCAL)) {
            final String name = setting.getName();
            if ("applicationUsername".equals(name) || "chargebackDescription".equals(name)) {
                settingDao.delete(setting.getId());
            }
        }
        // Now import them all again, with the current language
        importNew();

        // Then, refresh the handler's state
        localSettingsHandler.refresh();
        mailTranslationHandler.refresh();
        messageSettingsHandler.refresh();
    }

    public void removeListener(final SettingsChangeListener listener) {
        if (listener instanceof AccessSettingsChangeListener) {
            accessSettingsListeners.remove(listener);
        } else if (listener instanceof AlertSettingsChangeListener) {
            alertSettingsListeners.remove(listener);
        } else if (listener instanceof LocalSettingsChangeListener) {
            localSettingsListeners.remove(listener);
        } else if (listener instanceof LogSettingsChangeListener) {
            logSettingsListeners.remove(listener);
        } else if (listener instanceof MailSettingsChangeListener) {
            mailSettingsListeners.remove(listener);
        } else if (listener instanceof MailTranslationChangeListener) {
            mailTranslationListeners.remove(listener);
        } else if (listener instanceof MessageSettingsChangeListener) {
            messageSettingsListeners.remove(listener);
        }
    }

    public AccessSettings save(final AccessSettings settings) {
        final AccessSettings updated = accessSettingsHandler.update(settings);
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public AlertSettings save(final AlertSettings settings) {
        final AlertSettings updated = alertSettingsHandler.update(settings);
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public LocalSettings save(final LocalSettings settings) {
        final LocalSettings current = getLocalSettings();
        final LocalSettings updated = localSettingsHandler.update(settings);
        // If the scheduling time changed, we must notify the scheduled tasks
        if ((current.getSchedulingHour() != updated.getSchedulingHour()) || (current.getSchedulingMinute() != updated.getSchedulingMinute())) {
            schedulingHandler.updateTime();
        }
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public LogSettings save(final LogSettings settings) {
        final LogSettings updated = logSettingsHandler.update(settings);
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public MailSettings save(final MailSettings settings) {
        final MailSettings updated = mailSettingsHandler.update(settings);
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public MailTranslation save(final MailTranslation settings) {
        final MailTranslation updated = mailTranslationHandler.update(settings);
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public MessageSettings save(final MessageSettings settings) {
        final MessageSettings updated = messageSettingsHandler.update(settings);
        // Notify registered listeners
        notifyListener(settings);
        return updated;
    }

    public void setAccessSettingsHandler(final SettingsHandler<AccessSettings> handler) {
        accessSettingsHandler = handler;
    }

    public void setAlertSettingsHandler(final SettingsHandler<AlertSettings> handler) {
        alertSettingsHandler = handler;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setLocalSettingsHandler(final SettingsHandler<LocalSettings> handler) {
        localSettingsHandler = handler;
    }

    public void setLogSettingsHandler(final SettingsHandler<LogSettings> handler) {
        logSettingsHandler = handler;
    }

    public void setMailSettingsHandler(final MailSettingsHandler handler) {
        mailSettingsHandler = handler;
    }

    public void setMailTranslationHandler(final SettingsHandler<MailTranslation> mailTranslationHandler) {
        this.mailTranslationHandler = mailTranslationHandler;
    }

    public void setMessageSettingsHandler(final MessageSettingsHandler handler) {
        messageSettingsHandler = handler;
    }

    public void setSchedulingHandler(final SchedulingHandler schedulingHandler) {
        this.schedulingHandler = schedulingHandler;
    }

    public void setSettingDao(final SettingDAO settingDao) {
        this.settingDao = settingDao;
    }

    public void validate(final AccessSettings settings) {
        accessSettingsHandler.validate(settings);
    }

    public void validate(final AlertSettings settings) {
        alertSettingsHandler.validate(settings);
    }

    public void validate(final LocalSettings settings) {
        localSettingsHandler.validate(settings);
    }

    public void validate(final LogSettings settings) {
        logSettingsHandler.validate(settings);
    }

    public void validate(final MailSettings settings) {
        mailSettingsHandler.validate(settings);
    }

    public void validate(final MailTranslation settings) {
        mailTranslationHandler.validate(settings);
    }

    public void validate(final MessageSettings settings) {
        messageSettingsHandler.validate(settings);
    }

    private void appendSetting(final StringBuilder xml, final Setting setting) {
        final String indent2Levels = StringUtils.repeat("    ", 2);
        final String indent3Levels = StringUtils.repeat("    ", 3);
        xml.append(indent2Levels).append(String.format("<setting name=\"%s\" >\n", setting.getName()));
        xml.append(indent3Levels).append(StringEscapeUtils.escapeXml(setting.getValue()));
        xml.append(indent2Levels).append("</setting>\n");
    }

    private void appendSettingType(final StringBuilder xml, final Setting.Type type) {
        final String indent = StringUtils.repeat("    ", 1);
        xml.append(String.format("%s<settings type=\"%s\" >\n", indent, type.getValue()));
        final SettingsHandler<?> handler = handlersMap.get(type);
        final List<Setting> settings = handler.listSettings();
        for (final Setting setting : settings) {
            // Setting in ignore list, don't export it
            if (type == Setting.Type.LOCAL && ignoreSettings.contains(setting.getName())) {
                continue;
            }
            appendSetting(xml, setting);
        }
        xml.append(indent).append("</settings>\n");
    }

    private void notifyListener(final Object settings) {
        if (settings instanceof AccessSettings) {
            final AccessSettings accessSettings = (AccessSettings) settings;
            final AccessSettingsEvent event = new AccessSettingsEvent(accessSettings);
            for (final AccessSettingsChangeListener listener : accessSettingsListeners) {
                listener.onAccessSettingsUpdate(event);
            }
        } else if (settings instanceof AlertSettings) {
            final AlertSettings alertSettings = (AlertSettings) settings;
            final AlertSettingsEvent event = new AlertSettingsEvent(alertSettings);
            for (final AlertSettingsChangeListener listener : alertSettingsListeners) {
                listener.onAlertSettingsUpdate(event);
            }
        } else if (settings instanceof LocalSettings) {
            final LocalSettings localSettings = (LocalSettings) settings;
            final LocalSettingsEvent event = new LocalSettingsEvent(localSettings);
            for (final LocalSettingsChangeListener listener : localSettingsListeners) {
                listener.onLocalSettingsUpdate(event);
            }
        } else if (settings instanceof LogSettings) {
            final LogSettings logSettings = (LogSettings) settings;
            final LogSettingsEvent event = new LogSettingsEvent(logSettings);
            for (final LogSettingsChangeListener listener : logSettingsListeners) {
                listener.onLogSettingsUpdate(event);
            }
        } else if (settings instanceof MailSettings) {
            final MailSettings mailSettings = (MailSettings) settings;
            final MailSettingsEvent event = new MailSettingsEvent(mailSettings);
            for (final MailSettingsChangeListener listener : mailSettingsListeners) {
                listener.onMailSettingsUpdate(event);
            }
        } else if (settings instanceof MailTranslation) {
            final MailTranslation mailTranslation = (MailTranslation) settings;
            final MailTranslationEvent event = new MailTranslationEvent(mailTranslation);
            for (final MailTranslationChangeListener listener : mailTranslationListeners) {
                listener.onMailSettingsUpdate(event);
            }
        } else if (settings instanceof MessageSettings) {
            final MessageSettings messageSettings = (MessageSettings) settings;
            final MessageSettingsEvent event = new MessageSettingsEvent(messageSettings);
            for (final MessageSettingsChangeListener listener : messageSettingsListeners) {
                listener.onMessageSettingsUpdate(event);
            }
        }
    }

}
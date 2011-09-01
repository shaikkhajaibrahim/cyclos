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
package nl.strohalm.cyclos.struts;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.customization.TranslationMessageService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.SpringHelper;
import nl.strohalm.cyclos.utils.conversion.LocaleConverter;

import org.apache.commons.beanutils.MethodUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.Globals;
import org.apache.struts.util.MessageResources;

/**
 * Customized message resources used to get data from the service
 * @author luis
 */
public class CyclosMessageResources extends MessageResources {

    private static final long serialVersionUID = 6706539088478972L;

    /**
     * Creates and initializes an instance for the given servlet context
     */
    public static CyclosMessageResources initialize(final ServletContext context) {
        // Initialize the instance
        final CyclosMessageResources resources = new CyclosMessageResources(context);
        SpringHelper.injectBeans(context, resources);

        // Read the messages of the language, creating missing keys
        final Properties properties = readProperties(context);
        resources.translationMessageService.initialize(properties);

        // Load the messages
        resources.reload();

        // Store the resources on the context, so Struts will find it
        context.setAttribute(Globals.MESSAGES_KEY, resources);
        return resources;
    }

    /**
     * Read the properties from file
     */
    public static Properties readProperties(final ServletContext context) {
        final SettingsService settingsService = SpringHelper.bean(context, "settingsService");
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String language = LocaleConverter.instance().toString(localSettings.getLocale());
        return readProperties(context, language);
    }

    /**
     * Read the properties from file, using the specified message
     */
    public static Properties readProperties(final ServletContext context, final String language) {
        final SettingsService settingsService = SpringHelper.bean(context, "settingsService");
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final String propertiesName = "ApplicationResources_" + language + ".properties";
        InputStream in = context.getResourceAsStream("/WEB-INF/classes/" + propertiesName);
        if (in == null) {
            in = CyclosMessageResources.class.getResourceAsStream("/" + propertiesName);
        }
        final Properties properties = new Properties();
        try {
            try {
                // The Properties.load(Reader) only exists on Java 6
                final Reader reader = new InputStreamReader(in, localSettings.getCharset());
                MethodUtils.invokeMethod(properties, "load", reader);
            } catch (final NoSuchMethodException e) {
                // Fallback to Properties.load(InputStream)
                properties.load(in);
            }
        } catch (final Exception e) {
            // Ignore
        }
        return properties;
    }

    /**
     * Return the instance for the given servlet context
     */
    public static CyclosMessageResources retrieve(final ServletContext context) {
        try {
            return (CyclosMessageResources) context.getAttribute(Globals.MESSAGES_KEY);
        } catch (final Exception e) {
            return null;
        }
    }

    private TranslationMessageService translationMessageService;
    private Properties                properties;
    private ServletContext            servletContext;

    public CyclosMessageResources(final ServletContext servletContext) {
        super(null, null);
        this.servletContext = servletContext;
    }

    @Override
    public String getMessage(final Locale locale, final String key) {
        String message = properties.getProperty(key);
        if (message == null) {
            message = "???" + key + "???";
        }
        return message;
    }

    public Properties getProperties() {
        if (properties == null) {
            reload();
        }
        return properties;
    }

    public void reload() {
        synchronized (this) {
            properties = readProperties(servletContext, "en_US");
            final Properties languageProperties = translationMessageService.getAsProperties();
            for (final Map.Entry<Object, Object> entry : languageProperties.entrySet()) {
                final String key = (String) entry.getKey();
                final String value = (String) entry.getValue();
                if (StringUtils.isNotEmpty(value)) {
                    properties.put(key, value);
                }
            }
            formats.clear();
        }
    }

    @Inject
    public void setTranslationMessageService(final TranslationMessageService translationMessageService) {
        this.translationMessageService = translationMessageService;
    }
}

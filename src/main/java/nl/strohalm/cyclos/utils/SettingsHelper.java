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

import java.util.Locale;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.entities.settings.AccessSettings;
import nl.strohalm.cyclos.entities.settings.AlertSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.LogSettings;
import nl.strohalm.cyclos.entities.settings.MailSettings;
import nl.strohalm.cyclos.entities.settings.MailTranslation;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.settings.LocalSettings.DatePattern;
import nl.strohalm.cyclos.utils.conversion.LocaleConverter;

import org.apache.commons.lang.StringUtils;

/**
 * Helper class for storing and retrieving settings from the web context
 * @author luis
 */
public abstract class SettingsHelper {

    public static final String ACCESS_KEY           = "accessSettings";
    public static final String ALERT_KEY            = "alertSettings";
    public static final String LOCAL_KEY            = "localSettings";
    public static final String LOG_KEY              = "logSettings";
    public static final String MAIL_KEY             = "mailSettings";
    public static final String MAIL_TRANSLATION_KEY = "mailTranslation";
    public static final String MESSAGE_KEY          = "messageSettings";

    public static AccessSettings getAccessSettings(final HttpServletRequest request) {
        return getAccessSettings(request.getSession().getServletContext());
    }

    public static AccessSettings getAccessSettings(final ServletContext context) {
        return (AccessSettings) context.getAttribute(ACCESS_KEY);
    }

    public static AlertSettings getAlertSettings(final HttpServletRequest request) {
        return getAlertSettings(request.getSession().getServletContext());
    }

    public static AlertSettings getAlertSettings(final ServletContext context) {
        return (AlertSettings) context.getAttribute(ALERT_KEY);
    }

    public static String getDatePatternDescription(final ServletContext context, final DatePattern datePattern) {
        final String day = MessageHelper.message(context, "global.datePattern.day");
        final String month = MessageHelper.message(context, "global.datePattern.month");
        final String year = MessageHelper.message(context, "global.datePattern.year");
        String[] parts = null;
        switch (datePattern) {
            case DD_MM_YYYY_SLASH:
            case DD_MM_YYYY_PERIOD:
            case DD_MM_YYYY_DASH:
                parts = new String[] { day, month, year };
                break;
            case MM_DD_YYYY_SLASH:
            case MM_DD_YYYY_DASH:
            case MM_DD_YYYY_PERIOD:
                parts = new String[] { month, day, year };
                break;
            case YYYY_MM_DD_SLASH:
            case YYYY_MM_DD_DASH:
            case YYYY_MM_DD_PERIOD:
                parts = new String[] { year, month, day };
                break;
        }
        return StringUtils.join(parts, datePattern.getSeparator());
    }

    public static LocalSettings getLocalSettings(final HttpServletRequest request) {
        return getLocalSettings(request.getSession().getServletContext());
    }

    public static LocalSettings getLocalSettings(final ServletContext context) {
        return (LocalSettings) context.getAttribute(LOCAL_KEY);
    }

    public static LogSettings getLogSettings(final ServletContext context) {
        return (LogSettings) context.getAttribute(LOG_KEY);
    }

    public static MailSettings getMailSettings(final HttpServletRequest request) {
        return getMailSettings(request.getSession().getServletContext());
    }

    public static MailSettings getMailSettings(final ServletContext context) {
        return (MailSettings) context.getAttribute(MAIL_KEY);
    }

    public static MailTranslation getMailTranslation(final HttpServletRequest request) {
        return getMailTranslation(request.getSession().getServletContext());
    }

    public static MailTranslation getMailTranslation(final ServletContext context) {
        return (MailTranslation) context.getAttribute(MAIL_TRANSLATION_KEY);
    }

    public static MessageSettings getMessageSettings(final HttpServletRequest request) {
        return getMessageSettings(request.getSession().getServletContext());
    }

    public static MessageSettings getMessageSettings(final ServletContext context) {
        return (MessageSettings) context.getAttribute(MESSAGE_KEY);
    }

    public static void storeAccessSettings(final ServletContext context, final AccessSettings settings) {
        context.setAttribute(ACCESS_KEY, settings);
    }

    public static void storeAlertSettings(final ServletContext context, final AlertSettings settings) {
        context.setAttribute(ALERT_KEY, settings);
    }

    public static void storeLocalSettings(final ServletContext context, final LocalSettings settings) {
        context.setAttribute(LOCAL_KEY, settings);

        // Store the locale
        final Locale locale = settings.getLocale();
        context.setAttribute("language", LocaleConverter.instance().toString(locale));

        // Store the localized date and time patterns
        final DatePattern datePattern = settings.getDatePattern();
        final String day = MessageHelper.message(context, "global.datePattern.day");
        final String month = MessageHelper.message(context, "global.datePattern.month");
        final String year = MessageHelper.message(context, "global.datePattern.year");
        String[] parts = null;
        switch (datePattern) {
            case DD_MM_YYYY_SLASH:
            case DD_MM_YYYY_PERIOD:
            case DD_MM_YYYY_DASH:
                parts = new String[] { day, month, year };
                break;
            case MM_DD_YYYY_SLASH:
            case MM_DD_YYYY_DASH:
            case MM_DD_YYYY_PERIOD:
                parts = new String[] { month, day, year };
                break;
            case YYYY_MM_DD_SLASH:
            case YYYY_MM_DD_DASH:
            case YYYY_MM_DD_PERIOD:
                parts = new String[] { year, month, day };
                break;
        }
        final String datePatternString = StringUtils.join(parts, "/");
        context.setAttribute("datePattern", datePatternString);

        final String hour = MessageHelper.message(context, "global.datePattern.hour");
        final String minute = MessageHelper.message(context, "global.datePattern.minute");
        final String second = MessageHelper.message(context, "global.datePattern.second");
        final String timePatternString = StringUtils.join(new String[] { hour, minute, second }, ":");
        context.setAttribute("dateTimePattern", datePatternString + " " + timePatternString);
    }

    public static void storeLogSettings(final ServletContext context, final LogSettings settings) {
        context.setAttribute(LOG_KEY, settings);
    }

    public static void storeMailSettings(final ServletContext context, final MailSettings settings) {
        context.setAttribute(MAIL_KEY, settings);
    }

    public static void storeMailTranslation(final ServletContext context, final MailTranslation settings) {
        context.setAttribute(MAIL_TRANSLATION_KEY, settings);
    }

    public static void storeMessageSettings(final ServletContext context, final MessageSettings settings) {
        context.setAttribute(MESSAGE_KEY, settings);
    }

    public static void storeSetting(final ServletContext context, final Object settings) {
        if (settings instanceof AccessSettings) {
            storeAccessSettings(context, (AccessSettings) settings);
        } else if (settings instanceof AlertSettings) {
            storeAlertSettings(context, (AlertSettings) settings);
        } else if (settings instanceof LocalSettings) {
            storeLocalSettings(context, (LocalSettings) settings);
        } else if (settings instanceof LogSettings) {
            storeLogSettings(context, (LogSettings) settings);
        } else if (settings instanceof MailSettings) {
            storeMailSettings(context, (MailSettings) settings);
        } else if (settings instanceof MailTranslation) {
            storeMailTranslation(context, (MailTranslation) settings);
        } else if (settings instanceof MessageSettings) {
            storeMessageSettings(context, (MessageSettings) settings);
        }
    }

}

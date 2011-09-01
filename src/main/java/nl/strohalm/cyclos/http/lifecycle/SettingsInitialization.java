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
package nl.strohalm.cyclos.http.lifecycle;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.SettingsHelper;

/**
 * Initializes the settings
 * @author luis
 */
public class SettingsInitialization implements ContextInitialization {

    private SettingsService settingsService;

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public void init(final ServletContext context) {

        // Import new settings
        settingsService.importNew();

        // Store the settings on the servlet context
        SettingsHelper.storeAccessSettings(context, settingsService.getAccessSettings());
        SettingsHelper.storeAlertSettings(context, settingsService.getAlertSettings());
        SettingsHelper.storeLocalSettings(context, settingsService.getLocalSettings());
        SettingsHelper.storeLogSettings(context, settingsService.getLogSettings());
        SettingsHelper.storeMailSettings(context, settingsService.getMailSettings());
        SettingsHelper.storeMailTranslation(context, settingsService.getMailTranslation());
        SettingsHelper.storeMessageSettings(context, settingsService.getMessageSettings());
    }

    @Inject
    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}

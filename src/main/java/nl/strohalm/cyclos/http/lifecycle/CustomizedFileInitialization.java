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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.alerts.SystemAlert;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFileQuery;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.alerts.AlertService;
import nl.strohalm.cyclos.services.customization.CustomizedFileService;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Initializes the customized files
 * @author luis
 */
public class CustomizedFileInitialization implements ContextInitialization {

    private static final Log      LOG = LogFactory.getLog(CustomizedFileInitialization.class);
    private CustomizedFileService customizedFileService;
    private AlertService          alertService;

    public AlertService getAlertService() {
        return alertService;
    }

    public CustomizedFileService getCustomizedFileService() {
        return customizedFileService;
    }

    public void init(final ServletContext context) {
        // First, clear the customized css files to ensure proper migration from previous versions when css files were not customized
        final File customizedStylesDir = new File(context.getRealPath(CustomizationHelper.customizedPathFor(context, CustomizedFile.Type.STYLE)));
        for (final File css : customizedStylesDir.listFiles((FilenameFilter) new SuffixFileFilter(".css"))) {
            css.delete();
        }

        final LocalSettings localSettings = SettingsHelper.getLocalSettings(context);
        final CustomizedFileQuery query = new CustomizedFileQuery();
        query.fetch(CustomizedFile.Relationships.GROUP);
        query.setAll(true);
        final List<CustomizedFile> files = customizedFileService.search(query);
        for (final CustomizedFile customizedFile : files) {
            final CustomizedFile.Type type = customizedFile.getType();
            final String name = customizedFile.getName();
            final File physicalFile = CustomizationHelper.customizedFileOf(context, customizedFile);
            final File originalFile = CustomizationHelper.originalFileOf(context, type, name);

            try {
                // No conflicts are checked for style sheet files
                if (type != CustomizedFile.Type.STYLE) {
                    final boolean wasConflict = customizedFile.isConflict();

                    // Check if the file contents has changed since the customization
                    String originalFileContents = null;
                    if (originalFile.exists()) {
                        originalFileContents = FileUtils.readFileToString(originalFile);
                        if (originalFileContents.length() == 0) {
                            originalFileContents = null;
                        }
                    }
                    // Check if the file is now on conflict (or the new contents has changed)
                    boolean contentsChanged;
                    boolean newContentsChanged;
                    if (type == CustomizedFile.Type.APPLICATION_PAGE) {
                        contentsChanged = !StringUtils.trimToEmpty(originalFileContents).equals(StringUtils.trimToEmpty(customizedFile.getOriginalContents())) && !StringUtils.trimToEmpty(originalFileContents).equals(StringUtils.trimToEmpty(customizedFile.getContents()));
                        newContentsChanged = contentsChanged && !StringUtils.trimToEmpty(originalFileContents).equals(StringUtils.trimToEmpty(customizedFile.getNewContents()));
                    } else {
                        contentsChanged = !StringUtils.trimToEmpty(originalFileContents).equals(StringUtils.trimToEmpty(customizedFile.getOriginalContents()));
                        newContentsChanged = !StringUtils.trimToEmpty(originalFileContents).equals(StringUtils.trimToEmpty(customizedFile.getNewContents()));
                    }

                    if (!wasConflict && contentsChanged) {
                        // Save the new contents, marking the file as conflicts
                        customizedFile.setNewContents(originalFileContents);
                        customizedFileService.save(customizedFile);

                        // Generate an alert if the file is customized for the whole system
                        if (customizedFile.getGroup() == null && customizedFile.getGroupFilter() == null) {
                            SystemAlert.Alerts alertType = null;
                            switch (type) {
                                case APPLICATION_PAGE:
                                    alertType = SystemAlert.Alerts.NEW_VERSION_OF_APPLICATION_PAGE;
                                    break;
                                case HELP:
                                    alertType = SystemAlert.Alerts.NEW_VERSION_OF_HELP_FILE;
                                    break;
                                case STATIC_FILE:
                                    alertType = SystemAlert.Alerts.NEW_VERSION_OF_STATIC_FILE;
                                    break;
                            }
                            alertService.create(alertType, customizedFile.getName());
                        }
                    } else if (wasConflict && newContentsChanged) {
                        // The file has changed again. Update the new contents
                        customizedFile.setNewContents(originalFileContents);
                        customizedFileService.save(customizedFile);
                    }
                }

                // Check if we must update an style file
                final long lastModified = customizedFile.getLastModified() == null ? System.currentTimeMillis() : customizedFile.getLastModified().getTimeInMillis();
                if (!physicalFile.exists() || physicalFile.lastModified() != lastModified) {
                    physicalFile.getParentFile().mkdirs();
                    FileUtils.writeStringToFile(physicalFile, customizedFile.getContents(), localSettings.getCharset());
                    physicalFile.setLastModified(lastModified);
                }
            } catch (final IOException e) {
                LOG.warn("Error handling customized file: " + physicalFile.getAbsolutePath(), e);
            }
        }
        // We must copy all non-customized style sheets to the customized dir, so there will be no problems for locating images
        final File originalDir = new File(context.getRealPath(CustomizationHelper.originalPathFor(context, CustomizedFile.Type.STYLE)));
        for (final File original : originalDir.listFiles()) {
            final File customized = new File(customizedStylesDir, original.getName());
            if (!customized.exists()) {
                try {
                    FileUtils.copyFile(original, customized);
                } catch (final IOException e) {
                    LOG.warn("Error copying style sheet file: " + customized.getAbsolutePath(), e);
                }
            }
        }
    }

    @Inject
    public void setAlertService(final AlertService alertService) {
        this.alertService = alertService;
    }

    @Inject
    public void setCustomizedFileService(final CustomizedFileService customizedFileService) {
        this.customizedFileService = customizedFileService;
    }

}
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

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.customization.binaryfiles.BinaryFile;
import nl.strohalm.cyclos.entities.customization.documents.DynamicDocument;
import nl.strohalm.cyclos.entities.customization.documents.StaticDocument;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFileQuery;
import nl.strohalm.cyclos.entities.customization.files.CustomizedFile.Type;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.OperatorGroup;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.access.LoggedUser;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class for customizations
 * @author luis
 */
public final class CustomizationHelper {

    public static class CustomizationData {
        private CustomizationLevel level;
        private Long               id;

        public CustomizationData(final CustomizationLevel level) {
            this.level = level;
        }

        public CustomizationData(final CustomizationLevel level, final Long id) {
            this(level);
            this.id = id;
        }

        public Long getId() {
            return id;
        }

        public CustomizationLevel getLevel() {
            return level;
        }
    }

    public static enum CustomizationLevel {
        GROUP, GROUP_FILTER, GLOBAL, NONE
    }

    public static List<String>        OPERATOR_SPECIFIC_FILES = Collections.unmodifiableList(Arrays.asList("posweb_header.jsp", "posweb_footer.jsp"));
    public static final String        APPLICATION_PAGES_PATH  = "/pages/";
    public static final String        STATIC_FILES_PATH       = "/pages/general/static_files/";
    public static final String        STYLE_PATH              = "/pages/styles/";
    public static final String        HELP_PATH               = "/pages/general/translation_files/helps/";
    public static final String        DOCUMENT_PATH           = "/pages/documents/";
    private static final List<String> EXCLUDED_DIRS           = Arrays.asList(new String[] { "/general/translation_files/", "/scripts/", "/styles/" });
    private static final List<String> LOGIN_CUSTOMIZED_FILES  = Arrays.asList(new String[] { "login.css", "login.jsp", "top.jsp" });

    private static final Log          LOG                     = LogFactory.getLog(CustomizationHelper.class);

    /**
     * Returns the real file for the given customized file
     */
    public static File customizedFileOf(final ServletContext context, final CustomizedFile file) {
        Group group = file.getGroup();
        final GroupFilter groupFilter = file.getGroupFilter();
        final Type type = file.getType();
        String customizedPath;
        if (group == null && groupFilter == null) {
            customizedPath = customizedPathFor(context, type);
        } else if (group != null) {
            final FetchService fetchService = SpringHelper.bean(context, "fetchService");
            group = fetchService.fetch(group);
            final boolean forceSameGroup = (group instanceof OperatorGroup && OPERATOR_SPECIFIC_FILES.contains(file.getName()));
            customizedPath = customizedPathFor(context, type, group, forceSameGroup);
        } else {
            customizedPath = customizedPathFor(context, type, groupFilter);
        }
        final File dir = new File(context.getRealPath(customizedPath));
        final String fileName = resolveName(context, file);
        return new File(dir, fileName);
    }

    /**
     * Returns the real file for the given customized file
     */
    public static File customizedFileOf(final ServletContext context, final CustomizedFile.Type type, final String name) {
        final CustomizedFile file = new CustomizedFile();
        file.setType(type);
        file.setName(name);
        return customizedFileOf(context, file);
    }

    /**
     * Returns the root path for the customized file type
     */
    public static String customizedPathFor(final ServletContext context, final CustomizedFile.Type type) {
        String path;
        switch (type) {
            case STATIC_FILE:
                path = STATIC_FILES_PATH + "customized/";
                break;
            case HELP:
                path = HELP_PATH;
                break;
            case STYLE:
                path = STYLE_PATH;
                break;
            case APPLICATION_PAGE:
                path = APPLICATION_PAGES_PATH;
                break;
            default:
                throw new IllegalArgumentException("Unknown file type: " + type);
        }
        return path;
    }

    /**
     * Returns the root path for the customized file type
     */
    public static String customizedPathFor(final ServletContext context, final CustomizedFile.Type type, final Group group, final boolean forceSameGroup) {
        // Style sheets use the same path as the original file
        if (group != null && type != CustomizedFile.Type.STYLE) {
            final String pathPart = pathPart(context, group, forceSameGroup);
            return customizedPathFor(context, type) + pathPart + "/";
        }
        return customizedPathFor(context, type);
    }

    /**
     * Returns the root path for the customized file type
     */
    public static String customizedPathFor(final ServletContext context, final CustomizedFile.Type type, final GroupFilter groupFilter) {
        // Style sheets use the same path as the original file
        if (groupFilter != null && type != CustomizedFile.Type.STYLE) {
            final String pathPart = pathPart(context, groupFilter);
            return customizedPathFor(context, type) + pathPart + "/";
        }
        return customizedPathFor(context, type);
    }

    /**
     * Returns the real directory for documents
     */
    public static File documentDir(final ServletContext context) {
        return new File(context.getRealPath(DOCUMENT_PATH));
    }

    /**
     * Returns the real document file for a given document
     */
    public static File documentFile(final ServletContext context, final DynamicDocument document) {
        return new File(documentDir(context), "document_" + document.getId() + ".jsp");
    }

    /**
     * Finds the customization level and the related id for the given parameters, trying files on the following order:
     * <ol>
     * <li>Customized for the given group (if any)</li>
     * <li>Customized for the group filters of the given group (if the group is the same as the logged member's group)</li>
     * <li>Customized for the given group filter (if any)</li>
     * <li>Customized globally</li>
     * <li>The original file (with no customizations)</li>
     * </ol>
     */
    public static CustomizationData findCustomizationOf(final ServletContext context, final Type type, Group group, final GroupFilter groupFilter, final String name) {
        final CustomizedFile file = new CustomizedFile();
        file.setType(type);
        file.setName(name);

        if (group == null && LoggedUser.isValid()) {
            group = LoggedUser.group();
        }

        // Try customized for group
        if (group != null) {
            file.setGroup(group);
            // Try directly for group
            String customizedPath = customizedPathFor(context, type, group, true);
            String dir = context.getRealPath(customizedPath);
            String fileName = resolveName(context, file);
            File physicalFile = new File(dir, fileName);
            if (physicalFile.exists()) {
                return new CustomizationData(CustomizationLevel.GROUP, group.getId());
            }

            // For operator group, try by member group
            final FetchService fetchService = SpringHelper.bean(context, "fetchService");
            try {
                group = fetchService.fetch(group);
                if (group instanceof OperatorGroup) {
                    customizedPath = customizedPathFor(context, type, group, false);
                    dir = context.getRealPath(customizedPath);
                    fileName = resolveName(context, file);
                    physicalFile = new File(dir, fileName);
                    if (physicalFile.exists()) {
                        // Check if the file found uses the operator or member group
                        final String groupIdPart = "group_" + group.getId();
                        if (!fileName.contains(groupIdPart) && !dir.contains(groupIdPart)) {
                            // It is the member group
                            group = ((OperatorGroup) group).getMember().getGroup();
                        }
                        return new CustomizationData(CustomizationLevel.GROUP, group.getId());
                    }
                }
            } catch (final EntityNotFoundException e) {
                // Ignore
            }

            // Try by group filters of the given group (when is the same group as the logged user or no user logged in)
            if (!LoggedUser.isValid() || LoggedUser.group().equals(group)) {
                file.setGroup(null);
                try {
                    Group groupForFilters = group;
                    if (group instanceof OperatorGroup) {
                        groupForFilters = ((OperatorGroup) group).getMember().getGroup();
                    }
                    final Collection<GroupFilter> groupFilters = fetchService.fetch(groupForFilters, Group.Relationships.GROUP_FILTERS).getGroupFilters();
                    for (final GroupFilter current : groupFilters) {
                        file.setGroupFilter(current);
                        customizedPath = customizedPathFor(context, type, current);
                        dir = context.getRealPath(customizedPath);
                        fileName = resolveName(context, file);
                        physicalFile = new File(dir, fileName);
                        if (physicalFile.exists()) {
                            return new CustomizationData(CustomizationLevel.GROUP_FILTER, current.getId());
                        }
                    }
                } catch (final EntityNotFoundException e) {
                    // Ignore
                }
            }
        }
        // Try directly for group filter
        if (groupFilter != null) {
            file.setGroupFilter(groupFilter);
            final File groupFilterFile = customizedFileOf(context, file);
            if (groupFilterFile.exists()) {
                return new CustomizationData(CustomizationLevel.GROUP_FILTER, groupFilter.getId());
            }
        }

        // Try customized globally
        final String globallyCustomizedPath = customizedPathFor(context, type) + name;
        final File physicalFile = new File(context.getRealPath(globallyCustomizedPath));
        if (physicalFile.exists()) {
            return new CustomizationData(CustomizationLevel.GLOBAL);
        }

        // Return the original
        return new CustomizationData(CustomizationLevel.NONE);
    }

    public static File findFileOf(final ServletContext context, final CustomizedFile.Type type, final Group group, final GroupFilter groupFilter, final String name) {
        final String path = findPathOf(context, type, group, groupFilter, name);
        return new File(context.getRealPath(path));
    }

    public static File findFileOf(final ServletContext context, final CustomizedFile.Type type, final Group group, final String name) {
        return findFileOf(context, type, group, null, name);
    }

    public static String findPathOf(final ServletContext context, final CustomizedFile.Type type, final Group group, final String name) {
        return findPathOf(context, type, group, null, name);
    }

    /**
     * Returns the path of a customized file.
     */
    public static String findPathOf(final ServletContext context, final Type type, final Group group, final GroupFilter groupFilter, final String name) {
        final CustomizationData customization = findCustomizationOf(context, type, group, groupFilter, name);
        return pathOf(context, type, name, customization);
    }

    /**
     * Returns the real form file for a given document
     */
    public static File formFile(final ServletContext context, final DynamicDocument document) {
        return new File(documentDir(context), "form_" + document.getId() + ".jsp");
    }

    public static List<File> getDirectoryContents(final String path, final ServletContext servletContext) {
        final String rootPath = APPLICATION_PAGES_PATH + ("/".equals(path) ? "" : path);
        final File rootDir = new File(servletContext.getRealPath(rootPath));
        final List<File> allDirectories = Arrays.asList(rootDir.listFiles((FileFilter) DirectoryFileFilter.DIRECTORY));
        final List<File> filteredDirectories = new ArrayList<File>();
        for (final File currentFile : allDirectories) {
            final String fullPath = path + currentFile.getName() + "/";
            if (!EXCLUDED_DIRS.contains(fullPath)) {
                filteredDirectories.add(currentFile);
            }
        }
        final List<File> files = Arrays.asList(rootDir.listFiles(CustomizedFile.Type.APPLICATION_PAGE.getFilter()));
        Collections.sort(filteredDirectories);
        Collections.sort(files);
        final List<File> filesAndDirs = new ArrayList<File>();
        filesAndDirs.addAll(filteredDirectories);
        filesAndDirs.addAll(files);
        return filesAndDirs;
    }

    /**
     * Checks whether any of the the given files is related to the login page
     */
    public static boolean isAnyFileRelatedToLoginPage(final Collection<CustomizedFile> files) {
        for (final CustomizedFile file : files) {
            if (isRelatedToLoginPage(file)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the given file is related to the login page
     */
    public static boolean isRelatedToLoginPage(final CustomizedFile file) {
        return LOGIN_CUSTOMIZED_FILES.contains(file.getName());
    }

    /**
     * Lists files for the customized file type
     */
    public static List<File> listByType(final ServletContext context, final CustomizedFile.Type type) {
        final File dir = new File(context.getRealPath(originalPathFor(context, type)));
        return new ArrayList<File>(Arrays.asList(dir.listFiles(type.getFilter())));
    }

    /**
     * Returns a collection of file names of files that are not already customized
     */
    public static List<String> onlyNotAlreadyCustomized(final ServletContext context, final CustomizedFile.Type type, final List<CustomizedFile> customizedFiles) {
        final List<String> notYetCustomized = new ArrayList<String>();
        final List<File> files = CustomizationHelper.listByType(context, type);
        final CustomizedFileQuery query = new CustomizedFileQuery();
        query.setType(type);
        for (final File file : files) {
            boolean alreadyCustomized = false;
            for (final CustomizedFile customizedFile : customizedFiles) {
                if (customizedFile.getName().equals(file.getName())) {
                    alreadyCustomized = true;
                    break;
                }
            }
            if (!alreadyCustomized) {
                notYetCustomized.add(file.getName());
            }
        }
        Collections.sort(notYetCustomized);
        return notYetCustomized;
    }

    /**
     * Returns the real file for the given original file
     */
    public static File originalFileOf(final ServletContext context, final CustomizedFile.Type type, final String name) {
        final File dir = new File(context.getRealPath(originalPathFor(context, type)));
        return new File(dir, name);
    }

    /**
     * Returns the root path for the customized file type
     */
    public static String originalPathFor(final ServletContext context, final CustomizedFile.Type type) {
        String path;
        switch (type) {
            case STATIC_FILE:
                path = STATIC_FILES_PATH;
                break;
            case HELP:
                path = HELP_PATH + context.getAttribute("language") + "/";
                break;
            case STYLE:
                path = STYLE_PATH + "original/";
                break;
            case APPLICATION_PAGE:
                path = APPLICATION_PAGES_PATH;
                break;
            default:
                throw new IllegalArgumentException("Unknown file type: " + type);
        }
        return path;
    }

    /**
     * Returns the path for the given customization
     */
    public static String pathOf(final ServletContext context, final Type type, final String name, final CustomizationData customization) {
        final CustomizedFile file = new CustomizedFile();
        file.setType(type);
        file.setName(name);

        String path;
        switch (customization.getLevel()) {
            case NONE:
                path = originalPathFor(context, type);
                break;
            case GLOBAL:
                path = customizedPathFor(context, type);
                break;
            case GROUP:
                final Group group = EntityHelper.reference(Group.class, customization.getId());
                file.setGroup(group);
                path = customizedPathFor(context, type, group, true);
                break;
            case GROUP_FILTER:
                final GroupFilter groupFilter = EntityHelper.reference(GroupFilter.class, customization.getId());
                file.setGroupFilter(groupFilter);
                path = customizedPathFor(context, type, groupFilter);
                break;
            default:
                return null;
        }
        return path + resolveName(context, file);
    }

    /**
     * Returns the real file for a given static document
     */
    public static File staticFile(final ServletContext context, final StaticDocument document) {
        return new File(documentDir(context), document.getId().toString());
    }

    /**
     * Update a binary file on the disk
     */
    public static void updateBinaryFile(final ServletContext context, final java.io.File file, final BinaryFile binaryFile) {
        final Long lastModified = binaryFile.getLastModified() == null ? System.currentTimeMillis() : binaryFile.getLastModified().getTimeInMillis();
        if (!file.exists() || file.lastModified() != lastModified) {
            try {
                file.getParentFile().mkdirs();
                IOUtils.copy(binaryFile.getContents().getBinaryStream(), new FileOutputStream(file));
                file.setLastModified(lastModified);
            } catch (final Exception e) {
                LOG.warn("Error writing file: " + file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Update a customized file
     */
    public static void updateFile(final ServletContext context, final java.io.File file, final nl.strohalm.cyclos.entities.customization.files.File customizedFile) {
        final Long lastModified = customizedFile.getLastModified() == null ? System.currentTimeMillis() : customizedFile.getLastModified().getTimeInMillis();
        if (!file.exists() || file.lastModified() != lastModified) {
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(context);
            try {
                file.getParentFile().mkdirs();
                FileUtils.writeStringToFile(file, customizedFile.getContents(), localSettings.getCharset());
                file.setLastModified(lastModified);
            } catch (final IOException e) {
                LOG.warn("Error writing file: " + file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Returns the additional path part for the given customized group
     */
    private static String pathPart(final ServletContext context, final Group group, final boolean forceSameGroup) {
        if (group == null) {
            return null;
        }
        Long groupId;
        if (!forceSameGroup && (group instanceof OperatorGroup)) {
            final FetchService fetchService = SpringHelper.bean(context, "fetchService");
            final OperatorGroup og = fetchService.fetch(((OperatorGroup) group), RelationshipHelper.nested(OperatorGroup.Relationships.MEMBER, Element.Relationships.GROUP));
            groupId = og.getMember().getGroup().getId();
        } else {
            groupId = group.getId();
        }
        return "group_" + groupId.toString();
    }

    /**
     * Returns the additional path part for the given group filter
     */
    private static String pathPart(final ServletContext context, final GroupFilter groupFilter) {
        if (groupFilter == null) {
            return null;
        }
        return "group_filter_" + groupFilter.getId();
    }

    /**
     * Returns the name of the style sheet for a given group
     */
    private static String resolveName(final ServletContext context, final CustomizedFile file) {
        final Type type = file.getType();
        String name = file.getName();
        final Group group = file.getGroup();
        final GroupFilter groupFilter = file.getGroupFilter();
        // Only style sheets may change the name when customized for groups or group filters. Other types change the path when customized
        if (type != CustomizedFile.Type.STYLE || (group == null && groupFilter == null)) {
            return name;
        }
        String filename;
        String extension;
        final int pos = name.lastIndexOf('.');
        if (pos < 0) {
            filename = name;
            extension = "";
        } else {
            filename = name.substring(0, pos);
            extension = name.substring(pos + 1);
        }
        // Find the path part to append on the name
        String pathPart;
        if (group != null) {
            pathPart = pathPart(context, group, false);
        } else {
            pathPart = pathPart(context, groupFilter);
        }
        name = filename + "_" + pathPart + (StringUtils.isEmpty(extension) ? "" : "." + extension);
        return name;
    }

}
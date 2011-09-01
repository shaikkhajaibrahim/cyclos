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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.entities.customization.images.Image;

import org.apache.commons.io.IOUtils;

/**
 * Helper class for images
 * @author luis
 */
public final class WebImageHelper {

    public static final String SYSTEM_IMAGES_PATH     = "/pages/images";
    public static final String SYSTEM_THUMBNAILS_PATH = "/pages/images/thumbnails";
    public static final String SYSTEM_IMAGES_MAP_KEY  = "systemImages";
    public static final String CUSTOM_IMAGES_PATH     = "/pages/images/custom";
    public static final String CUSTOM_THUMBNAILS_PATH = "/pages/images/custom/thumbnails";
    public static final String STYLE_IMAGES_PATH      = "/pages/styles";
    public static final String STYLE_THUMBNAILS_PATH  = "/pages/styles/thumbnails";
    public static final String NEW_SYSTEM_IMAGES_PATH = "/WEB-INF/images/system";
    public static final String NEW_STYLE_IMAGES_PATH  = "/WEB-INF/images/style";

    /**
     * Return the real path for a given image nature
     */
    public static File imagePath(final Image.Nature nature, final ServletContext context) {
        String path = null;
        switch (nature) {
            case SYSTEM:
                path = WebImageHelper.SYSTEM_IMAGES_PATH;
                break;
            case CUSTOM:
                path = WebImageHelper.CUSTOM_IMAGES_PATH;
                break;
            case STYLE:
                path = WebImageHelper.STYLE_IMAGES_PATH;
                break;
            default:
                return null;
        }
        return new File(context.getRealPath(path));
    }

    /**
     * Returns the new style image files
     */
    public static File[] newStyleImages(final ServletContext context) {
        final File dir = new File(context.getRealPath(NEW_STYLE_IMAGES_PATH));
        return dir.listFiles();
    }

    /**
     * Returns the new system image files
     */
    public static File[] newSystemImages(final ServletContext context) {
        final File dir = new File(context.getRealPath(NEW_SYSTEM_IMAGES_PATH));
        return dir.listFiles();
    }

    /**
     * Overwrite an image, even if it's updated
     */
    public static void overwrite(final ServletContext context, final Image image) {
        final Image.Nature nature = image.getNature();
        if (!updateNature(nature)) {
            return;
        }

        updateImage(context, false, image, imagePath(nature, context), true);
        updateImage(context, true, image, thumbnailPath(nature, context), true);
    }

    /**
     * Return the real path for thumbnails of a given image nature
     */
    public static File thumbnailPath(final Image.Nature nature, final ServletContext context) {
        String path = null;
        switch (nature) {
            case SYSTEM:
                path = WebImageHelper.SYSTEM_THUMBNAILS_PATH;
                break;
            case CUSTOM:
                path = WebImageHelper.CUSTOM_THUMBNAILS_PATH;
                break;
            case STYLE:
                path = WebImageHelper.STYLE_THUMBNAILS_PATH;
                break;
            default:
                return null;
        }
        return new File(context.getRealPath(path));
    }

    /**
     * Update an image
     */
    public static void update(final ServletContext context, final Image image) {
        final Image.Nature nature = image.getNature();
        if (!updateNature(nature)) {
            return;
        }

        updateImage(context, false, image, imagePath(nature, context), false);
        updateImage(context, true, image, thumbnailPath(nature, context), false);
    }

    /**
     * Update an image or thumbnail
     * @param overrite
     */
    private static void updateImage(final ServletContext context, final boolean isThumbnail, final Image image, final File dir, final boolean overrite) {
        // Update nothing if no path is given
        if (dir == null) {
            return;
        }
        InputStream in = null;
        OutputStream out = null;
        final File file = new File(dir, image.getName());
        try {
            dir.mkdirs();
            final long lastModified = image.getLastModified() == null ? System.currentTimeMillis() : image.getLastModified().getTimeInMillis();
            if (overrite || file.lastModified() != lastModified) {
                out = new FileOutputStream(file);
                in = (isThumbnail ? image.getThumbnail() : image.getImage()).getBinaryStream();
                IOUtils.copy(in, out);
                file.setLastModified(lastModified);
            }
        } catch (final IOException e) {
            context.log("Error writing image file " + file, e);
        } catch (final SQLException e) {
            context.log("Error writing image file " + file + " from db", e);
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(out);
        }
    }

    /**
     * Determine if the given image nature will be updated on the file system
     */
    public static boolean updateNature(final Image.Nature nature) {
        // Only natures without owner
        return nature != null && nature.getOwnerType() == null;
    }
}

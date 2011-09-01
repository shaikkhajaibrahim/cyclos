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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.customization.images.Image;
import nl.strohalm.cyclos.entities.customization.images.Image.Nature;
import nl.strohalm.cyclos.services.customization.ImageService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.WebImageHelper;
import nl.strohalm.cyclos.utils.ImageHelper.ImageType;

/**
 * Initializes the images
 * @author luis
 */
public class ImageInitialization implements ContextInitialization {

    private ImageService imageService;
    private FetchService fetchService;

    public void init(final ServletContext context) {
        importImagesIfNecessary(context);
        updateImages(context);
    }

    @Inject
    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    @Inject
    public void setImageService(final ImageService imageService) {
        this.imageService = imageService;
    }

    private void importImagesIfNecessary(final ServletContext context) {
        final boolean noImages = !imageService.hasSystemImages();

        // Import new system images
        write(WebImageHelper.newSystemImages(context), Nature.SYSTEM);

        // Only on the first time the application is initialized (no system images) we import the default style images
        if (noImages) {
            write(WebImageHelper.newStyleImages(context), Nature.STYLE);
        }
    }

    /**
     * Updates the local images
     */
    private void updateImages(final ServletContext context) {
        // Initialize the custom and stylesheet images
        final List<Image> images = new ArrayList<Image>();
        images.addAll(imageService.listByNature(Image.Nature.SYSTEM));
        images.addAll(imageService.listByNature(Image.Nature.CUSTOM));
        images.addAll(imageService.listByNature(Image.Nature.STYLE));
        for (final Image image : images) {
            WebImageHelper.update(context, fetchService.reload(image));
        }
    }

    /**
     * Write images to the database
     */
    private void write(final File[] newImages, final Nature nature) {
        for (final File file : newImages) {
            final String fileName = file.getName();
            ImageType type;
            try {
                type = ImageType.getByFileName(fileName);
            } catch (final Exception e) {
                // Go on... probably a file that was not an image was among the files...
                continue;
            }

            // Check if the image is in the database
            boolean shouldUpdate = false;
            try {
                imageService.load(nature, fileName);
            } catch (final EntityNotFoundException e) {
                // Should update on DB - does not exists yet
                shouldUpdate = true;
            }

            // Insert the image on the database if needed
            if (shouldUpdate) {
                try {
                    imageService.save(nature, type, fileName, new FileInputStream(file));
                } catch (final FileNotFoundException e) {
                    throw new IllegalStateException("File not found?!? " + e);
                }
            }
        }
    }
}

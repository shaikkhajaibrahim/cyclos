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
package nl.strohalm.cyclos.services.customization;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import nl.strohalm.cyclos.dao.customizations.ImageDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.customization.images.AdImage;
import nl.strohalm.cyclos.entities.customization.images.CustomImage;
import nl.strohalm.cyclos.entities.customization.images.Image;
import nl.strohalm.cyclos.entities.customization.images.ImageCaptionDTO;
import nl.strohalm.cyclos.entities.customization.images.ImageDetailsDTO;
import nl.strohalm.cyclos.entities.customization.images.MemberImage;
import nl.strohalm.cyclos.entities.customization.images.OwneredImage;
import nl.strohalm.cyclos.entities.customization.images.StyleImage;
import nl.strohalm.cyclos.entities.customization.images.SystemImage;
import nl.strohalm.cyclos.entities.customization.images.Image.Nature;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.customization.exceptions.ImageException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.Dimensions;
import nl.strohalm.cyclos.utils.ImageHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.ImageHelper.ImageType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;

/**
 * Image service implementation
 * @author luis
 */
public class ImageServiceImpl implements ImageService {

    private static final int MAX_IMAGE_SIZE_MULTIPLIER = 5;

    private FetchService     fetchService;
    private ImageDAO         imageDao;
    private SettingsService  settingsService;

    public boolean hasSystemImages() {
        return !imageDao.listByNature(Nature.SYSTEM).isEmpty();
    }

    public List<? extends Image> listByNature(final Nature nature) {
        return imageDao.listByNature(nature);
    }

    public List<? extends OwneredImage> listByOwner(final Entity owner) {
        return imageDao.listByOwner(owner);
    }

    public Image load(final Long id, final Relationship... fetch) {
        return imageDao.load(id, fetch);
    }

    public Image load(final Nature nature, final String name) {
        // Nonsense for images with owner
        if (nature.getOwnerProperty() != null) {
            throw new EntityNotFoundException(Image.class);
        }
        return imageDao.load(nature, name);
    }

    public int remove(final Long... ids) {
        return imageDao.delete(ids);
    }

    public int removeAdImages(final Long... ids) throws UnexpectedEntityException {
        return doRemoveAdImages(ids);
    }

    public int removeMemberImages(final Long... ids) throws UnexpectedEntityException {
        return doRemoveMemberImages(ids);
    }

    public int removeMyAdImages(final Long... ids) throws UnexpectedEntityException {
        return doRemoveAdImages(ids);
    }

    public int removeMyImages(final Long... ids) throws UnexpectedEntityException {
        return doRemoveMemberImages(ids);
    }

    public int removeSystem(final Long... ids) throws UnexpectedEntityException {
        for (final Long id : ids) {
            final Image image = load(id);
            if (image instanceof OwneredImage) {
                throw new UnexpectedEntityException();
            }
        }
        return imageDao.delete(ids);
    }

    public Image save(final Nature nature, final ImageType type, final String name, final InputStream in) {
        switch (nature) {
            case SYSTEM:
                return saveSystemImage(type, name, in);
            case STYLE:
                return saveStyleImage(type, name, in);
            case CUSTOM:
                return saveCustomImage(type, name, in);
        }
        throw new IllegalArgumentException("Invalid nature: " + nature);
    }

    public AdImage saveAdImage(final Ad ad, final String caption, final ImageType type, final String name, final InputStream in) {
        return doSaveAdImage(ad, caption, type, name, in);
    }

    public CustomImage saveCustomImage(final ImageType type, final String name, final InputStream in) {
        CustomImage image;
        try {
            image = (CustomImage) load(Image.Nature.CUSTOM, name);
        } catch (final EntityNotFoundException e) {
            image = new CustomImage();
        }
        image = save(image, in, type.getContentType(), name);
        // We need this reload in order to be able to immediately read the blobs
        fetchService.reload(image);
        return image;
    }

    public void saveDetails(final ImageDetailsDTO details) {
        final Entity owner = details.getImageOwner();
        int order = 0;
        for (final ImageCaptionDTO dto : details.getDetails()) {
            final OwneredImage image = imageDao.load(dto.getId());
            if (!image.getOwner().equals(owner)) {
                throw new PermissionDeniedException();
            }
            image.setOrder(order++);
            image.setCaption(dto.getCaption());
            imageDao.update(image);
        }
    }

    public MemberImage saveMemberImage(final Member member, final String caption, final ImageType type, final String name, final InputStream in) {
        return doSaveMemberImage(member, caption, type, name, in);
    }

    public AdImage saveMyAdImage(final Ad ad, final String caption, final ImageType type, final String name, final InputStream in) {
        return doSaveAdImage(ad, caption, type, name, in);
    }

    public MemberImage saveMyImage(final Member member, final String caption, final ImageType type, final String name, final InputStream in) {
        return doSaveMemberImage(member, caption, type, name, in);
    }

    public StyleImage saveStyleImage(final ImageType type, final String name, final InputStream in) {
        StyleImage image;
        try {
            image = (StyleImage) load(Image.Nature.STYLE, name);
        } catch (final EntityNotFoundException e) {
            image = new StyleImage();
        }
        image = save(image, in, type.getContentType(), name);
        // We need this reload in order to be able to immediately read the blobs
        fetchService.reload(image);
        return image;
    }

    public SystemImage saveSystemImage(final ImageType type, final String name, final InputStream in) {
        SystemImage image;
        try {
            image = (SystemImage) load(Image.Nature.SYSTEM, name);
        } catch (final EntityNotFoundException e) {
            image = new SystemImage();
        }
        image = save(image, in, type.getContentType(), name);
        // We need this reload in order to be able to immediately read the blobs
        fetchService.reload(image);
        return image;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setImageDao(final ImageDAO imageDao) {
        this.imageDao = imageDao;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    private int doRemoveAdImages(final Long... ids) {
        for (final Long id : ids) {
            final Image image = load(id);
            if (!(image instanceof AdImage)) {
                throw new UnexpectedEntityException();
            }
        }
        return removeOwnered(ids);
    }

    private int doRemoveMemberImages(final Long... ids) {
        for (final Long id : ids) {
            final Image image = load(id);
            if (!(image instanceof MemberImage)) {
                throw new UnexpectedEntityException();
            }
        }
        return removeOwnered(ids);
    }

    private AdImage doSaveAdImage(Ad ad, final String caption, final ImageType type, final String name, final InputStream in) {
        ad = fetchService.fetch(ad, RelationshipHelper.nested(Ad.Relationships.OWNER, Element.Relationships.GROUP));
        final int maxImages = ad.getOwner().getMemberGroup().getMemberSettings().getMaxAdImagesPerMember();
        final int count = imageDao.countAdImages(ad);
        if (count >= maxImages) {
            throw new PermissionDeniedException();
        }
        final AdImage image = new AdImage();
        image.setAd(ad);
        image.setCaption(caption);
        image.setOrder(count + 1);
        return save(image, in, type.getContentType(), name);
    }

    private MemberImage doSaveMemberImage(Member member, final String caption, final ImageType type, final String name, final InputStream in) {
        member = fetchService.fetch(member, Element.Relationships.GROUP);
        final int maxImages = member.getMemberGroup().getMemberSettings().getMaxImagesPerMember();
        final int count = imageDao.countMemberImages(member);
        if (count >= maxImages) {
            throw new PermissionDeniedException();
        }
        final MemberImage image = new MemberImage();
        image.setMember(member);
        image.setCaption(caption);
        image.setOrder(count + 1);
        return save(image, in, type.getContentType(), name);
    }

    /**
     * Generates a thumbnail of the original image
     * @return The generated temporary files
     */
    private Set<File> generateBlobs(final Image image, final InputStream in) throws IOException {
        final LocalSettings localSettings = settingsService.getLocalSettings();

        // Generate a temporary file
        final File originalFile = File.createTempFile("cyclos", "image");
        // Store the stream to the file
        IOUtils.copy(in, new FileOutputStream(originalFile));

        final ImageType type = ImageType.getByContentType(image.getContentType());
        File imageFile = null;
        File thumbnailFile = null;

        // When the runtime cannot handle the given image type, we'd better not try!!!
        if (type.isResizeSupported()) {
            // Get the limits
            final Dimensions maxImageDimensions = localSettings.getMaxImageDimensions();
            final Dimensions maxThumbnailDimensions = localSettings.getMaxThumbnailDimensions();

            // Read the image properties
            final BufferedImage bufImage = ImageIO.read(originalFile);
            final Dimensions originalDimensions = new Dimensions(bufImage.getWidth(), bufImage.getHeight());
            if (originalDimensions.isGreaterThan(new Dimensions(maxImageDimensions.getWidth() * MAX_IMAGE_SIZE_MULTIPLIER, maxImageDimensions.getHeight() * MAX_IMAGE_SIZE_MULTIPLIER))) {
                throw new ImageException(ImageException.INVALID_DIMENSION);
            }

            // Image (except style sheet) needs resizing
            if (image.getNature() != Image.Nature.STYLE) {
                if (originalDimensions.isGreaterThan(maxImageDimensions)) {
                    imageFile = ImageHelper.resizeGivenMaxDimensions(bufImage, type.getContentType(), maxImageDimensions);
                }
            }
            // Thumbnail needs resizing
            if (originalDimensions.isGreaterThan(maxThumbnailDimensions)) {
                thumbnailFile = ImageHelper.resizeGivenMaxDimensions(bufImage, type.getContentType(), maxThumbnailDimensions);
            }
        }

        // When no resize was done, use the same original file
        if (imageFile == null) {
            imageFile = originalFile;
        }
        if (thumbnailFile == null) {
            thumbnailFile = originalFile;
        }

        // Set the blobs
        final int imageSize = (int) imageFile.length();
        image.setImage(imageDao.createBlob(new FileInputStream(imageFile), imageSize));
        image.setImageSize(imageSize);
        final int thumbnailSize = (int) thumbnailFile.length();
        image.setThumbnail(imageDao.createBlob(new FileInputStream(thumbnailFile), thumbnailSize));
        image.setThumbnailSize(thumbnailSize);

        // Return the open files
        return new HashSet<File>(Arrays.asList(originalFile, imageFile, thumbnailFile));
    }

    private int removeOwnered(final Long... ids) throws UnexpectedEntityException {
        return imageDao.delete(ids);
    }

    private <I extends Image> I save(I image, final InputStream in, final String contentType, final String name) {
        image.setContentType(contentType);
        image.setName(name);
        image.setLastModified(Calendar.getInstance());
        Set<File> openFiles = null;
        try {
            try {
                openFiles = generateBlobs(image, in);
            } catch (final ImageException e) {
                throw e;
            } catch (final Throwable e) {
                throw new ImageException(e);
            }
            if (image.isTransient()) {
                image = imageDao.insert(image, true);
            } else {
                image = imageDao.update(image, true);
            }
            return image;
        } finally {
            if (CollectionUtils.isNotEmpty(openFiles)) {
                // Ensure open files will be removed after the transaction commits
                for (final File file : openFiles) {
                    file.delete();
                }
            }
        }
    }
}
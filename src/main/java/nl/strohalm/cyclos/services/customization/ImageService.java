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

import java.io.InputStream;
import java.util.List;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.customization.images.AdImage;
import nl.strohalm.cyclos.entities.customization.images.CustomImage;
import nl.strohalm.cyclos.entities.customization.images.Image;
import nl.strohalm.cyclos.entities.customization.images.ImageDetailsDTO;
import nl.strohalm.cyclos.entities.customization.images.MemberImage;
import nl.strohalm.cyclos.entities.customization.images.OwneredImage;
import nl.strohalm.cyclos.entities.customization.images.StyleImage;
import nl.strohalm.cyclos.entities.customization.images.SystemImage;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.ImageHelper.ImageType;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;

/**
 * Service interface for customized images (system, advertisement, profile)
 * @author luis
 */
public interface ImageService extends Service {

    /**
     * Returns if there are any system images
     */
    boolean hasSystemImages();

    /**
     * Lists the images of the given nature
     */
    List<? extends Image> listByNature(Image.Nature nature);

    /**
     * Lists the images belonging to the specified owner
     */
    List<? extends OwneredImage> listByOwner(Entity owner);

    /**
     * Loads an image by nature and name
     */
    Image load(Image.Nature nature, String name);

    /**
     * Loads the image by id with the specified fetch
     */
    Image load(Long id, Relationship... fetch);

    /**
     * Removes images without checking permissions. Should be called from internal procedures
     */
    int remove(Long... ids);

    /**
     * Removes the specified AD images that belongs to a related member.
     * @return The number of removed images
     * @throws UnexpectedEntityException When one of the given ids is not of an AD image
     */
    @AdminAction(@Permission(module = "adminMemberAds", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerAds", operation = "manage"))
    @RelatedEntity(AdImage.class)
    @PathToMember("ad.owner")
    int removeAdImages(Long... ids) throws UnexpectedEntityException;

    /**
     * Removes the specified MEMBER images that belongs to related members.
     * @return The number of removed images
     * @throws UnexpectedEntityException When one of the given ids is not of a MEMBER image
     */
    @AdminAction(@Permission(module = "adminMembers", operation = "changeProfile"))
    @BrokerAction(@Permission(module = "brokerMembers", operation = "changeProfile"))
    @RelatedEntity(MemberImage.class)
    @PathToMember("member")
    int removeMemberImages(Long... ids) throws UnexpectedEntityException;

    /**
     * Removes the specified AD images that belongs to the logged member.
     * @return The number of removed images
     * @throws UnexpectedEntityException When one of the given ids is not of an AD image
     */
    @MemberAction(@Permission(module = "memberAds", operation = "publish"))
    @OperatorAction(@Permission(module = "operatorAds", operation = "publish"))
    @RelatedEntity(AdImage.class)
    @PathToMember("ad.owner")
    int removeMyAdImages(Long... ids) throws UnexpectedEntityException;

    /**
     * Removes the specified MEMBER images that belongs to the logged member.
     * @return The number of removed images
     * @throws UnexpectedEntityException When one of the given ids is not of a MEMBER image
     */
    @MemberAction
    @RelatedEntity(MemberImage.class)
    @PathToMember("member")
    int removeMyImages(Long... ids) throws UnexpectedEntityException;

    /**
     * Removes the specified images. They must be either SYSTEM, CUSTOM or STYLE
     * @return The number of removed images
     * @throws UnexpectedEntityException When one of the given ids is not of a system image
     */
    @AdminAction(@Permission(module = "systemCustomImages", operation = "manage"))
    int removeSystem(Long... ids) throws UnexpectedEntityException;

    /**
     * Saves an image with no permission check. Should be called on system initialization to import images on first execution
     */
    Image save(Image.Nature nature, ImageType type, String name, InputStream in);

    /**
     * Saves the specified ad image for a related member
     */
    @AdminAction(@Permission(module = "adminMemberAds", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerAds", operation = "manage"))
    @PathToMember("owner")
    AdImage saveAdImage(Ad ad, String caption, ImageType type, String name, InputStream in);

    /**
     * Saves the specified custom image
     */
    @AdminAction(@Permission(module = "systemCustomImages", operation = "manage"))
    CustomImage saveCustomImage(ImageType type, String name, InputStream in);

    /**
     * Saves the image details: order and captions
     */
    void saveDetails(ImageDetailsDTO details);

    /**
     * Saves the specified member image for a related member
     */
    @BrokerAction(@Permission(module = "brokerMembers", operation = "changeProfile"))
    @AdminAction(@Permission(module = "adminMembers", operation = "changeProfile"))
    @PathToMember("")
    MemberImage saveMemberImage(Member member, String caption, ImageType type, String name, InputStream in);

    /**
     * Saves the specified ad image for the logged member
     */
    @MemberAction(@Permission(module = "memberAds", operation = "publish"))
    @OperatorAction(@Permission(module = "operatorAds", operation = "publish"))
    @PathToMember("owner")
    AdImage saveMyAdImage(Ad ad, String caption, ImageType type, String name, InputStream in);

    /**
     * Saves the specified member image for the logged member
     */
    @MemberAction
    @PathToMember("")
    MemberImage saveMyImage(Member member, String caption, ImageType type, String name, InputStream in);

    /**
     * Saves the specified stylesheet image
     */
    @AdminAction(@Permission(module = "systemCustomImages", operation = "manage"))
    StyleImage saveStyleImage(ImageType type, String name, InputStream in);

    /**
     * Saves the specified system image
     */
    @AdminAction(@Permission(module = "systemCustomImages", operation = "manage"))
    SystemImage saveSystemImage(ImageType type, String name, InputStream in);

}
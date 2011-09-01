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
package nl.strohalm.cyclos.services.ads;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.ads.AdDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.ads.FullTextAdQuery;
import nl.strohalm.cyclos.entities.ads.Ad.TradeType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * Service interface for Advertisements used to control ad operations like add, remove, expiration date, number of ads by member.
 * @author rafael
 */
@RelatedEntity(Ad.class)
@PathToMember("owner")
public interface AdService extends Service {

    /**
     * Count for active ads with AdCategory = adCategoryId and TradeType = type and external publication = true. Used in WS.
     */
    @SystemAction
    public int countExternalAds(Long adCategoryId, TradeType type);

    /**
     * @see AdDAO#fullTextSearch(FullTextAdQuery)
     */
    public List<Ad> fullTextSearch(FullTextAdQuery query) throws DaoException;

    /**
     * Adds to index entities which have not yet been indexed
     */
    void addMissingEntitiesToIndex();

    /**
     * Returns the index status
     */
    IndexStatus getIndexStatus();

    /**
     * Returns the number of ads of a given status
     * @param status The status of the ads
     * @return the number of ads
     */
    int getNumberOfAds(Ad.Status status);

    /**
     * Returns the number of ads of a given member by date
     * @param date
     * @param member
     */
    Map<Ad.Status, Integer> getNumberOfAds(Calendar date, Member member);

    /**
     * Returns the number of ads of a given member by status (Ad.Status)
     * @param member the owner of the ads
     * @return number of ads by status
     */
    Map<Ad.Status, Integer> getNumberOfAds(Member member);

    /**
     * Loads the ad, fetching the specified relationships
     * @return The ads loaded
     */
    Ad load(Long id, Relationship... fetch);

    /**
     * Marks this ad as members already notified
     */
    void markMembersNotified(Ad ad);

    /**
     * Generates an alert for each ad expired today
     */
    List<Ad> notifyExpiredAds(Calendar time);

    /**
     * Optimizes the full-text indexes
     */
    void optimizeIndex();

    /**
     * Build the index for full text searches
     */
    void rebuildIndex();

    /**
     * Remove the given advertisements with no permission check. Should be called from internal procedures
     */
    int remove(Long[] ids);

    /**
     * Removes the specified ads
     * @return The number of removed ads
     */
    @AdminAction(@Permission(module = "adminMemberAds", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerAds", operation = "manage"))
    int removeMemberAd(Long... ids);

    /**
     * Removes the specified ads
     * @return The number of removed ads
     */
    @MemberAction(@Permission(module = "memberAds", operation = "publish"))
    @OperatorAction(@Permission(module = "operatorAds", operation = "publish"))
    int removeMyAd(Long... ids);

    /**
     * Saves the ad without checking permissions. Should be called from internal procedures
     */
    @SystemAction
    Ad save(Ad ad);

    /**
     * Saves the specified ad
     */
    @AdminAction(@Permission(module = "adminMemberAds", operation = "manage"))
    @BrokerAction(@Permission(module = "brokerAds", operation = "manage"))
    Ad saveMemberAd(Ad ad);

    /**
     * Saves the specified ad
     */
    @MemberAction(@Permission(module = "memberAds", operation = "publish"))
    @OperatorAction(@Permission(module = "operatorAds", operation = "publish"))
    Ad saveMyAd(Ad ad);

    /**
     * Search the existing ads based on the AdQuery object
     * @return a list of ads
     */
    List<Ad> search(AdQuery queryParameters);

    /**
     * Validates the specified ad
     * @throws ValidationException if validation fails.
     */
    @DontEnforcePermission(traceable = true)
    void validate(Ad ad) throws ValidationException;

}
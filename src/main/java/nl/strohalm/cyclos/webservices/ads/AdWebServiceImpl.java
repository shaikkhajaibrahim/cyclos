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
package nl.strohalm.cyclos.webservices.ads;

import java.util.List;

import javax.jws.WebService;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.ads.AdCategoryService;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.webservices.model.AdCategoryVO;
import nl.strohalm.cyclos.webservices.model.AdVO;
import nl.strohalm.cyclos.webservices.model.DetailedAdCategoryVO;
import nl.strohalm.cyclos.webservices.utils.server.AdHelper;

/**
 * Web service implementation
 * @author luis
 */
@WebService(name = "ads", serviceName = "ads")
public class AdWebServiceImpl implements AdWebService {

    private static final Relationship[] FETCH = { RelationshipHelper.nested(Ad.Relationships.OWNER, Element.Relationships.GROUP), Ad.Relationships.CATEGORY, Ad.Relationships.CUSTOM_VALUES, Ad.Relationships.IMAGES };
    private AdService                   adService;
    private AdHelper                    adHelper;
    private AdCategoryService           adCategoryService;
    private CustomFieldService          customFieldService;

    public AdResultPage fullTextSearch(final FullTextAdSearchParameters params) {
        if (params == null) {
            return null;
        }
        final List<Ad> ads = adService.fullTextSearch(adHelper.toFullTextQuery(params));
        return adHelper.toResultPage(params, ads);
    }

    public AdHelper getAdHelper() {
        return adHelper;
    }

    public AdService getAdService() {
        return adService;
    }

    public AdCategoryVO[] listCategories() {
        final List<AdCategory> list = adCategoryService.listLeaf();
        final AdCategoryVO[] vos = new AdCategoryVO[list.size()];
        for (int i = 0; i < vos.length; i++) {
            vos[i] = adHelper.toVO(list.get(i));
        }
        return vos;
    }

    public DetailedAdCategoryVO[] listCategoryTree() {
        final List<AdCategory> rootList = adCategoryService.listRoot();
        final DetailedAdCategoryVO[] vos = new DetailedAdCategoryVO[rootList.size()];
        for (int i = 0; i < rootList.size(); i++) {
            final AdCategory adCategory = rootList.get(i);
            vos[i] = adHelper.toDetailedVO(adCategory);
        }
        return vos;
    }

    @SuppressWarnings("unchecked")
    public AdVO load(final long id) {
        try {
            List<AdCustomField> customFields = (List<AdCustomField>) customFieldService.listByNature(CustomField.Nature.AD);
            customFields = CustomFieldHelper.onlyVisible(customFields);
            final Ad ad = adService.load(id, FETCH);
            return adHelper.toVO(ad, customFields);
        } catch (final PermissionDeniedException e) {
            return null;
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }

    public AdCategoryVO loadCategory(final long id) {
        try {
            return adHelper.toVO(adCategoryService.load(id));
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }

    public AdResultPage search(final AdSearchParameters params) {
        if (params == null) {
            return null;
        }
        final List<Ad> ads = adService.search(adHelper.toQuery(params));
        return adHelper.toResultPage(params, ads);
    }

    public void setAdCategoryService(final AdCategoryService adCategoryService) {
        this.adCategoryService = adCategoryService;
    }

    public void setAdHelper(final AdHelper adHelper) {
        this.adHelper = adHelper;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

}
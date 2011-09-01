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
package nl.strohalm.cyclos.webservices.utils.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.ads.AbstractAdQuery;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.ads.FullTextAdQuery;
import nl.strohalm.cyclos.entities.ads.Ad.TradeType;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomField;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.ads.AdService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.query.Page;
import nl.strohalm.cyclos.webservices.ads.AbstractAdSearchParameters;
import nl.strohalm.cyclos.webservices.ads.AdResultPage;
import nl.strohalm.cyclos.webservices.ads.AdSearchParameters;
import nl.strohalm.cyclos.webservices.ads.FullTextAdSearchParameters;
import nl.strohalm.cyclos.webservices.ads.AbstractAdSearchParameters.AdVOStatus;
import nl.strohalm.cyclos.webservices.ads.AbstractAdSearchParameters.AdVOTradeType;
import nl.strohalm.cyclos.webservices.model.AdCategoryVO;
import nl.strohalm.cyclos.webservices.model.AdVO;
import nl.strohalm.cyclos.webservices.model.DetailedAdCategoryVO;
import nl.strohalm.cyclos.webservices.model.FieldValueVO;
import nl.strohalm.cyclos.webservices.model.TimePeriodVO;

/**
 * Utility class for ads
 * @author luis
 */
public class AdHelper {
    private CurrencyHelper     currencyHelper;
    private FieldHelper        fieldHelper;
    private ImageHelper        imageHelper;
    private MemberHelper       memberHelper;
    private SettingsService    settingsService;
    private QueryHelper        queryHelper;
    private CustomFieldService customFieldService;
    private AdService          adService;

    public AdService getAdService() {
        return adService;
    }

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public FieldHelper getFieldHelper() {
        return fieldHelper;
    }

    public ImageHelper getImageHelper() {
        return imageHelper;
    }

    public MemberHelper getMemberHelper() {
        return memberHelper;
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setCurrencyHelper(final CurrencyHelper currencyHelper) {
        this.currencyHelper = currencyHelper;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFieldHelper(final FieldHelper fieldHelper) {
        this.fieldHelper = fieldHelper;
    }

    public void setImageHelper(final ImageHelper imageHelper) {
        this.imageHelper = imageHelper;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setQueryHelper(final QueryHelper queryHelper) {
        this.queryHelper = queryHelper;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public DetailedAdCategoryVO toDetailedVO(final AdCategory category) {
        if (category == null) {
            return null;
        }
        final DetailedAdCategoryVO vo = new DetailedAdCategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setFullName(category.getFullName());
        vo.setLevel(category.getLevel());

        // count only ads with external publication = true
        vo.setCountOffer(adService.countExternalAds(category.getId(), TradeType.OFFER));
        vo.setCountSearch(adService.countExternalAds(category.getId(), TradeType.SEARCH));

        final Collection<AdCategory> childrenList = category.getChildren();
        final List<DetailedAdCategoryVO> children = new ArrayList<DetailedAdCategoryVO>();

        for (final AdCategory child : childrenList) {
            children.add(toDetailedVO(child));
        }
        vo.setChildren(children);
        return vo;
    }

    public FullTextAdQuery toFullTextQuery(final FullTextAdSearchParameters params) {
        if (params == null) {
            return null;
        }
        final FullTextAdQuery query = new FullTextAdQuery();
        fill(params, query);
        return query;
    }

    public AdQuery toQuery(final AdSearchParameters params) {
        if (params == null) {
            return null;
        }
        final AdQuery query = new AdQuery();
        fill(params, query);
        query.setRandomOrder(params.isRandomOrder());
        return query;
    }

    /**
     * Converts a list or page of ads into an AdResultPage
     */
    @SuppressWarnings("unchecked")
    public AdResultPage toResultPage(final AbstractAdSearchParameters params, final List<Ad> ads) {
        final AdResultPage page = new AdResultPage();
        if (ads instanceof Page) {
            final Page<Ad> adPage = (Page<Ad>) ads;
            page.setCurrentPage(adPage.getCurrentPage());
            page.setTotalCount(adPage.getTotalCount());
        }
        final List<AdVO> vos = new ArrayList<AdVO>();
        List<AdCustomField> adFields = null;
        List<MemberCustomField> memberFields = null;
        if (params.isShowAdFields()) {
            adFields = (List<AdCustomField>) customFieldService.listByNature(CustomField.Nature.AD);
        }
        if (params.isShowMemberFields()) {
            memberFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        }
        for (int i = 0; i < ads.size(); i++) {
            final AdVO vo = toVO(ads.get(i), adFields, memberFields);
            if (vo != null) {
                vos.add(vo);
            }
        }
        page.setAds(vos);
        return page;
    }

    /**
     * Convert an advertisement to VO
     */
    public AdVO toVO(final Ad ad) {
        return toVO(ad, null);
    }

    /**
     * Convert an advertisement to VO, filling the given ad custom fields
     */
    public AdVO toVO(final Ad ad, final List<AdCustomField> fields) {
        return toVO(ad, fields, null);
    }

    /**
     * Convert an advertisement to VO, filling the given ad and owner custom fields
     */
    public AdVO toVO(final Ad ad, final List<AdCustomField> adFields, final List<MemberCustomField> memberFields) {
        if (ad == null) {
            return null;
        }
        final LocalSettings localSettings = settingsService.getLocalSettings();

        final AdVO vo = new AdVO();
        vo.setId(ad.getId());
        vo.setCategory(toVO(ad.getCategory()));
        vo.setTitle(ad.getTitle());
        vo.setDescription(ad.getDescription());
        final Currency currency = ad.getCurrency();
        vo.setCurrency(currencyHelper.toVO(currency));
        vo.setPrice(ad.getPrice());
        if (currency == null) {
            vo.setFormattedPrice(localSettings.getNumberConverter().toString(ad.getPrice()));
        } else {
            vo.setFormattedPrice(localSettings.getUnitsConverter(currency.getPattern()).toString(ad.getPrice()));
        }
        vo.setPermanent(ad.isPermanent());
        vo.setSearching(ad.getTradeType() == Ad.TradeType.SEARCH);
        vo.setHtml(ad.isHtml());
        final Period publicationPeriod = ad.getPublicationPeriod();
        if (publicationPeriod != null) {
            final Calendar begin = publicationPeriod.getBegin();
            final Calendar end = publicationPeriod.getEnd();
            vo.setPublicationStart(begin);
            vo.setFormattedPublicationStart(localSettings.getRawDateConverter().toString(begin));
            vo.setPublicationEnd(end);
            vo.setFormattedPublicationEnd(localSettings.getRawDateConverter().toString(end));
        }
        vo.setOwner(memberHelper.toVO(ad.getOwner(), memberFields, false));
        vo.setImages(imageHelper.toVOs(ad.getImages()));
        if (adFields != null) {
            vo.setFields(fieldHelper.toList(adFields, ad.getCustomValues()));
        }
        return vo;
    }

    /**
     * Convert a category to VO
     */
    public AdCategoryVO toVO(final AdCategory category) {
        if (category == null) {
            return null;
        }
        final AdCategoryVO vo = new AdCategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getFullName());
        return vo;
    }

    private void fill(final AbstractAdSearchParameters params, final AbstractAdQuery query) {
        query.fetch(Ad.Relationships.OWNER, Ad.Relationships.CUSTOM_VALUES, Ad.Relationships.IMAGES, Ad.Relationships.CATEGORY);
        queryHelper.fill(params, query);
        query.setExternalPublication(true);
        AdVOStatus status = params.getStatus();
        if (status == null) {
            status = AdVOStatus.ACTIVE;
        }
        switch (status) {
            case PERMANENT:
                query.setStatus(Ad.Status.PERMANENT);
                break;
            case SCHEDULED:
                query.setStatus(Ad.Status.SCHEDULED);
                break;
            case EXPIRED:
                query.setStatus(Ad.Status.EXPIRED);
                break;
            default:
                query.setStatus(Ad.Status.ACTIVE);
                break;
        }
        query.setCategory(params.getCategoryId() == null ? null : EntityHelper.reference(AdCategory.class, params.getCategoryId()));
        query.setKeywords(params.getKeywords());
        query.setInitialPrice(params.getInitialPrice());
        query.setFinalPrice(params.getFinalPrice());
        final TimePeriodVO since = params.getSince();
        if (since != null && since.getNumber() != null && since.getField() != null) {
            final TimePeriod timePeriod = new TimePeriod(since.getNumber(), TimePeriod.Field.valueOf(since.getField().name()));
            query.setSince(timePeriod);
        }
        final AdVOTradeType tradeType = params.getTradeType();
        if (tradeType != null) {
            switch (tradeType) {
                case OFFER:
                    query.setTradeType(Ad.TradeType.OFFER);
                    break;
                case SEARCH:
                    query.setTradeType(Ad.TradeType.SEARCH);
                    break;
            }
        }
        query.setOwner(CoercionHelper.coerce(Member.class, params.getMemberId()));
        final MemberGroup[] groups = EntityHelper.references(MemberGroup.class, params.getMemberGroupIds());
        if (groups == null || groups.length > 0) {
            query.setGroups(Arrays.asList(groups));
        }
        final List<FieldValueVO> adFields = params.getAdFields();
        if (adFields != null && adFields.size() > 0) {
            query.setAdValues(fieldHelper.<AdCustomFieldValue> toValueCollection(CustomField.Nature.AD, adFields));
        }
        final List<FieldValueVO> memberFields = params.getMemberFields();
        if (memberFields != null && memberFields.size() > 0) {
            query.setMemberValues(fieldHelper.<MemberCustomFieldValue> toValueCollection(CustomField.Nature.MEMBER, memberFields));
        }
        query.setWithImagesOnly(params.isWithImagesOnly());
    }
}

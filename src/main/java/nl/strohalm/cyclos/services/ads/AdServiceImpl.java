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
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.ads.AdDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.IndexStatus;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.AbstractAdQuery;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.ads.FullTextAdQuery;
import nl.strohalm.cyclos.entities.ads.Ad.Status;
import nl.strohalm.cyclos.entities.ads.Ad.TradeType;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings;
import nl.strohalm.cyclos.entities.groups.MemberGroupSettings.ExternalAdPublication;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.Operator;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.AdInterestService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.CurrentTransactionData;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.RangeConstraint;
import nl.strohalm.cyclos.utils.StringHelper;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.access.LoggedUser;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.DelegatingValidator;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.InvalidError;
import nl.strohalm.cyclos.utils.validation.LengthValidation;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.RequiredValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Implementation class for the Advertisement service interface
 * @author rafael
 * @author luis
 */
public class AdServiceImpl implements AdService, ApplicationContextAware {

    /**
     * Validates max description size
     * @author Jefferson Magno
     */
    public class MaxAdDescriptionSizeValidation implements PropertyValidation {
        private static final long serialVersionUID = -5580051445666373995L;

        public ValidationError validate(final Object object, final Object name, final Object value) {
            final Ad ad = (Ad) object;
            Element element = fetchService.fetch(ad.getOwner(), Element.Relationships.GROUP);
            if (element == null) {
                if (!LoggedUser.isValid()) {
                    return null;
                }
                element = LoggedUser.element();
            }
            final Group group = element.getGroup();
            if (group instanceof MemberGroup) {
                final MemberGroup memberGroup = fetchService.fetch((MemberGroup) group);
                final int maxAdDescriptionSize = memberGroup.getMemberSettings().getMaxAdDescriptionSize();
                String description = value == null ? null : value.toString();
                if (ad.isHtml()) {
                    description = StringHelper.removeMarkupTagsAndUnescapeEntities(description);
                }
                return new LengthValidation(RangeConstraint.to(maxAdDescriptionSize)).validate(object, name, description);
            }
            return null;
        }
    }

    /**
     * Validates max publication period
     * @author luis
     */
    public class MaxPublicationTimeValidation implements GeneralValidation {
        private static final long serialVersionUID = 1616929350799341483L;

        public ValidationError validate(final Object object) {
            final Ad ad = (Ad) object;
            Element element = fetchService.fetch(ad.getOwner(), Element.Relationships.GROUP);
            if (element == null) {
                if (!LoggedUser.isValid()) {
                    return null;
                }
                element = LoggedUser.element();
            }
            final Group group = fetchService.fetch(element.getGroup());
            Calendar begin = null;
            Calendar end = null;
            try {
                begin = ad.getPublicationPeriod().getBegin();
                end = ad.getPublicationPeriod().getEnd();
            } catch (final NullPointerException e) {
            }
            // Check max publication time
            if (begin != null && end != null && !ad.isPermanent() && (group instanceof MemberGroup)) {
                final TimePeriod maxAdPublicationTime = ((MemberGroup) group).getMemberSettings().getMaxAdPublicationTime();
                final Calendar maxEnd = maxAdPublicationTime.add(begin);
                if (end.after(maxEnd)) {
                    return new ValidationError("ad.error.maxPublicationTimeExceeded");
                }
            }
            return null;
        }
    }

    /**
     * Validates an ad publication period
     * @author luis
     */
    public class PublicationPeriodValidation implements PropertyValidation {
        private static final long serialVersionUID = -6352683891570105522L;

        public ValidationError validate(final Object object, final Object name, final Object value) {
            final Ad ad = (Ad) object;
            if (!ad.isPermanent()) {
                final ValidationError required = RequiredValidation.instance().validate(object, name, value);
                if (required != null) {
                    return required;
                }
                if (name.toString().endsWith(".end") && ad.getPublicationPeriod() != null) {
                    final Calendar beginDate = ad.getPublicationPeriod().getBegin();
                    final Calendar endDate = (Calendar) value;
                    // Check if end is after begin
                    if (beginDate != null && endDate != null && !endDate.after(beginDate)) {
                        return new InvalidError();
                    }
                }
            }
            return null;
        }
    }

    private AdDAO              adDao;
    private CustomFieldService customFieldService;
    private FetchService       fetchService;
    private AdCategoryService  adCategoryService;
    private boolean            skipOrder;
    private ApplicationContext applicationContext;

    public void addMissingEntitiesToIndex() {
        adDao.addMissingEntitiesToIndex();
    }

    public int countExternalAds(final Long adCategoryId, final TradeType type) {
        final AdQuery query = new AdQuery();
        query.setStatus(Ad.Status.ACTIVE);
        query.setTradeType(type);
        query.setCategory(EntityHelper.reference(AdCategory.class, adCategoryId));
        query.setExternalPublication(true);
        query.setPageForCount();
        return PageHelper.getTotalCount(search(query));
    }

    public List<Ad> fullTextSearch(final FullTextAdQuery query) throws DaoException {
        if (StringUtils.isEmpty(query.getKeywords())) {
            // Without keywords, do a normal search
            return search(query.toAdQuery());
        }

        if (query.getCategory() != null && !adCategoryService.getActiveCategoriesId().contains(query.getCategory().getId())) {
            return Collections.emptyList();
        }

        if (query.getCategory() == null) {
            query.setCategoriesIds(adCategoryService.getActiveCategoriesId());
        } else {
            query.setCategoriesIds(new LinkedList<Long>());
            query.getCategoriesIds().add(query.getCategory().getId());
        }
        Collection<GroupFilter> groupFilters = query.getGroupFilters();
        if (CollectionUtils.isNotEmpty(groupFilters)) {
            // The full text search cannot handle group filters directly. Groups must be assigned
            groupFilters = fetchService.fetch(groupFilters, GroupFilter.Relationships.GROUPS);
            final Set<MemberGroup> groups = new HashSet<MemberGroup>();
            if (query.getGroups() != null) {
                groups.addAll(query.getGroups());
            }
            for (final GroupFilter groupFilter : groupFilters) {
                groups.addAll(groupFilter.getGroups());
            }
            query.setGroupFilters(null);
            query.setGroups(groups);
        }
        if (!applyLoggedUserFilters(query)) {
            return Collections.emptyList();
        }
        return adDao.fullTextSearch(query);
    }

    public IndexStatus getIndexStatus() {
        return adDao.getIndexStatus(Ad.class);
    }

    public Map<Ad.Status, Integer> getNumberOfAds(final Calendar date, final Member member) {
        final Map<Ad.Status, Integer> numberOfAds = new EnumMap<Ad.Status, Integer>(Ad.Status.class);
        final AdQuery query = new AdQuery();
        query.setOwner(member);
        query.setPageForCount();
        // date is for history
        if (date != null) {
            query.setHistoryDate(date);
            query.setIncludeDeleted(true);
        }
        for (final Ad.Status status : Ad.Status.values()) {
            query.setStatus(status);
            final int totalCount = PageHelper.getTotalCount(search(query));
            numberOfAds.put(status, totalCount);
        }
        return numberOfAds;
    }

    public Map<Ad.Status, Integer> getNumberOfAds(final Member member) {
        return getNumberOfAds(null, member);
    }

    public int getNumberOfAds(final Status status) {
        final AdQuery query = new AdQuery();
        query.setStatus(status);
        query.setPageForCount();
        if (LoggedUser.isValid()) {
            AdminGroup adminGroup = LoggedUser.group();
            adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
            query.setGroups(adminGroup.getManagesGroups());
        }
        return PageHelper.getTotalCount(search(query));
    }

    public Ad load(final Long id, final Relationship... fetch) {
        final Ad ad = adDao.load(id, fetch);
        if (LoggedUser.isValid() && !ad.getOwner().equals(LoggedUser.accountOwner()) && (LoggedUser.isMember() || LoggedUser.isOperator())) {
            final Member member = (Member) LoggedUser.accountOwner();
            final MemberGroup group = fetchService.fetch(member.getMemberGroup(), MemberGroup.Relationships.CAN_VIEW_ADS_OF_GROUPS);
            final Collection<MemberGroup> canViewAdsOfGroups = group.getCanViewAdsOfGroups();
            if (!canViewAdsOfGroups.contains(ad.getOwner().getGroup())) {
                throw new PermissionDeniedException();
            }
        }
        return ad;
    }

    public void markMembersNotified(Ad ad) {
        ad = load(ad.getId());
        ad.setMembersNotified(true);
        adDao.update(ad);
    }

    public List<Ad> notifyExpiredAds(final Calendar time) {
        final AdQuery searchParams = new AdQuery();
        searchParams.setEndDate(time);
        return search(searchParams);
    }

    public void optimizeIndex() {
        adDao.optimizeIndex(Ad.class);
    }

    public void rebuildIndex() {
        adDao.rebuildIndex(Ad.class);
    }

    public int remove(final Long[] ids) {
        return doRemove(ids);
    }

    public int removeMemberAd(final Long... ids) {
        return doRemove(ids);
    }

    public int removeMyAd(final Long... ids) {
        return doRemove(ids);
    }

    public Ad save(final Ad ad) {
        return doSave(ad);
    }

    public Ad saveMemberAd(final Ad ad) {
        return doSave(ad);
    }

    public Ad saveMyAd(final Ad ad) {
        return doSave(ad);
    }

    public List<Ad> search(final AdQuery query) {
        if (!applyLoggedUserFilters(query)) {
            return Collections.emptyList();
        }
        query.setSkipOrder(skipOrder);
        return adDao.search(query);
    }

    public void setAdCategoryService(final AdCategoryService adCategoryService) {
        this.adCategoryService = adCategoryService;
    }

    public void setAdDao(final AdDAO adDao) {
        this.adDao = adDao;
    }

    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setSkipOrder(final boolean skipOrder) {
        this.skipOrder = skipOrder;
    }

    public void validate(final Ad ad) throws ValidationException {
        getValidator().validate(ad);
    }

    private boolean applyLoggedUserFilters(final AbstractAdQuery query) {
        if (LoggedUser.isValid()) {
            if (LoggedUser.isAdministrator()) {
                // The logged user is an admin
                AdminGroup adminGroup = LoggedUser.group();
                adminGroup = fetchService.fetch(adminGroup, AdminGroup.Relationships.MANAGES_GROUPS);
                final Collection<MemberGroup> managesGroups = adminGroup.getManagesGroups();
                if (CollectionUtils.isEmpty(managesGroups)) {
                    return false;
                }
                if (CollectionUtils.isEmpty(query.getGroups())) {
                    query.setGroups(managesGroups);
                } else {
                    for (final Iterator<? extends Group> iter = query.getGroups().iterator(); iter.hasNext();) {
                        final Group group = iter.next();
                        if (!managesGroups.contains(group)) {
                            iter.remove();
                        }
                    }
                }
            } else {
                // If it´s a member viewing his/her own ads or it´s an operator viewing his/her member's ads
                if (query.isMyAds()) {
                    query.setOwner((Member) LoggedUser.accountOwner());
                    return true;
                }
                // If there's a member logged on, ensure to constrain the groups he can view
                MemberGroup group;
                if (LoggedUser.isOperator()) {
                    final Operator operator = LoggedUser.element();
                    group = operator.getMember().getMemberGroup();
                } else {
                    group = LoggedUser.group();
                }
                group = fetchService.fetch(group, MemberGroup.Relationships.CAN_VIEW_ADS_OF_GROUPS);
                final Collection<MemberGroup> canViewAdsOfGroups = group.getCanViewAdsOfGroups();
                if (CollectionUtils.isEmpty(canViewAdsOfGroups)) {
                    return false;
                }
                if (CollectionUtils.isEmpty(query.getGroups())) {
                    query.setGroups(canViewAdsOfGroups);
                } else {
                    for (final Iterator<? extends Group> iter = query.getGroups().iterator(); iter.hasNext();) {
                        final Group currentGroup = iter.next();
                        if (!canViewAdsOfGroups.contains(currentGroup)) {
                            iter.remove();
                        }
                    }
                }
            }
        }
        return true;
    }

    private int doRemove(final Long... ids) {
        // Update the categories counters
        for (final Long id : ids) {
            final Ad ad = load(id, Ad.Relationships.CATEGORY);
            if (ad.getStatus().isActive()) {
                adCategoryService.addCounter(ad.getCategory(), ad.getTradeType(), -1);
            }
        }

        // Physically remove
        final int count = adDao.delete(ids);

        // Update the search index
        adDao.removeFromIndex(Ad.class, ids);
        return count;
    }

    private Ad doSave(Ad ad) {
        // If the price is null, set currency to null too
        if (ad.getPrice() == null) {
            ad.setCurrency(null);
        }
        // Validates whether the Ad is valid or not
        validate(ad);

        // Store the custom values out of the ad
        final Collection<AdCustomFieldValue> customValues = ad.getCustomValues();
        ad.setCustomValues(null);

        // Check for limited input
        final Member owner = fetchService.fetch(ad.getOwner(), Element.Relationships.GROUP);
        final MemberGroup group = owner.getMemberGroup();
        final MemberGroupSettings memberSettings = group.getMemberSettings();
        if (!memberSettings.isEnablePermanentAds()) {
            ad.setPermanent(false);
        }
        final ExternalAdPublication externalAdPublication = memberSettings.getExternalAdPublication();
        if (externalAdPublication == ExternalAdPublication.DISABLED) {
            ad.setExternalPublication(false);
        } else if (externalAdPublication == ExternalAdPublication.ENABLED) {
            ad.setExternalPublication(true);
        }

        AdCategory subtractCounter = null;
        AdCategory addCounter = null;
        final boolean isInsert = ad.isTransient();
        if (isInsert) {
            final int maxAds = owner.getMemberGroup().getMemberSettings().getMaxAdsPerMember();

            // Check if the member can publish more advertisements
            final AdQuery adQuery = new AdQuery();
            adQuery.setPageForCount();
            adQuery.setOwner(ad.getOwner());
            final int currentAds = PageHelper.getTotalCount(adDao.search(adQuery));
            if (currentAds >= maxAds) {
                throw new ValidationException("ad.error.maxAds", ad.getOwner().getUsername());
            }

            final Calendar now = Calendar.getInstance();
            if (ad.isPermanent()) {
                final Period p = new Period(now, null);
                ad.setPublicationPeriod(p);
            }
            ad.setCreationDate(now);
            ad = adDao.insert(ad);
            if (ad.getStatus().isActive()) {
                addCounter = ad.getCategory();
            }
        } else {
            final Ad old = load(ad.getId());
            if (old.getStatus().isActive()) {
                subtractCounter = old.getCategory();
            }
            if (ad.isPermanent() || !ad.isMembersNotified()) {
                // Ensure the publication period remains the same when marked as permanent
                if (ad.isPermanent()) {
                    final Period p = old.getPublicationPeriod().clone();
                    p.setEnd(null);
                    ad.setPublicationPeriod(p);
                }
                // Ensure the members notified remains the same
                ad.setMembersNotified(old.isMembersNotified());
            }
            ad = adDao.update(ad);
            if (ad.getStatus().isActive()) {
                addCounter = ad.getCategory();
            }
        }

        // Save the custom field values
        ad.setCustomValues(customValues);
        customFieldService.saveAdValues(ad);

        // Update the full text index
        adDao.addToIndex(ad);

        // Update the category counters
        if (subtractCounter != null && !subtractCounter.equals(addCounter)) {
            adCategoryService.addCounter(subtractCounter, ad.getTradeType(), -1);
        }
        if (addCounter != null && !addCounter.equals(subtractCounter)) {
            adCategoryService.addCounter(addCounter, ad.getTradeType(), +1);
        }

        // Notify interested members
        if (ad.getStatus().isActive() && !ad.isMembersNotified()) {
            // Do it only after the transaction commit, to avoid 2 threads using the same ad
            final Ad toNotify = ad;
            CurrentTransactionData.addTransactionCommitListener(new CurrentTransactionData.TransactionCommitListener() {
                public void onTransactionCommit() {
                    // We cannot rely on injection as it causes a cycle, because AdInterestService also depends on this AdService
                    final AdInterestService adInterestService = (AdInterestService) applicationContext.getBean("adInterestService");
                    adInterestService.notifyInterestedMembers(toNotify);
                }
            });
        }

        return ad;
    }

    private Validator getValidator() {
        final Validator validator = new Validator("ad");
        validator.general(new MaxPublicationTimeValidation());
        validator.property("title").required().maxLength(100);
        validator.property("description").required().add(new MaxAdDescriptionSizeValidation());
        validator.property("price").positiveNonZero();
        validator.property("category").required();
        validator.property("tradeType").required();
        validator.property("owner").required();
        validator.property("publicationPeriod.begin").add(new PublicationPeriodValidation());
        validator.property("publicationPeriod.end").add(new PublicationPeriodValidation());

        // Custom fields
        validator.chained(new DelegatingValidator(new DelegatingValidator.DelegateSource() {
            public Validator getValidator() {
                return customFieldService.getAdValueValidator();
            }
        }));
        return validator;
    }

}
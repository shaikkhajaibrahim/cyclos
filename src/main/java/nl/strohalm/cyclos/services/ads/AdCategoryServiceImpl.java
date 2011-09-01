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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.dao.ads.AdCategoryDAO;
import nl.strohalm.cyclos.dao.ads.AdDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.ads.AdCategoryQuery;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.ads.Ad.TradeType;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.XmlHelper;
import nl.strohalm.cyclos.utils.query.PageHelper;
import nl.strohalm.cyclos.utils.validation.GeneralValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation class for the Advertisement service interface
 * @author rafael
 * @author luis
 * @author Lucas Geiss
 */
public class AdCategoryServiceImpl implements AdCategoryService {

    private final String     ROOT_ELEMENT     = "ad-categories";
    private final String     CATEGORY_ELEMENT = "ad-category";
    private final String     NAME_ATTRIBUTE   = "name";

    private AdService        adService;
    private SettingsService  settingsService;
    private List<AdCategory> rootCache;
    private List<AdCategory> leafCache;
    private AdCategoryDAO    adCategoryDao;
    private AdDAO            adDao;
    private FetchService     fetchService;

    public AdCategory addCounter(AdCategory category, final TradeType tradeType, final int count) {
        category = fetchService.fetch(category, RelationshipHelper.nested(AdCategory.MAX_LEVEL, AdCategory.Relationships.PARENT));
        AdCategory current = category;
        while (current != null) {
            if (tradeType == TradeType.OFFER) {
                current.setCountOffer(Math.max(0, current.getCountOffer() + count));
            } else {
                current.setCountSearch(Math.max(0, current.getCountSearch() + count));
            }
            adCategoryDao.update(current);

            if ((current != category) && !current.isActive()) {
                current = null;
            } else {
                current = current.getParent();
            }
        }
        invalidateCache();
        return category;
    }

    public String exportToXml() {
        final LocalSettings localSettings = settingsService.getLocalSettings();
        final StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"").append(localSettings.getCharset()).append("\"?>\n");
        xml.append('<').append(ROOT_ELEMENT).append(">\n");
        final List<AdCategory> categories = listRoot();
        for (final AdCategory adCategory : categories) {
            appendXml(xml, adCategory);
        }
        xml.append("</").append(ROOT_ELEMENT).append(">\n");
        return xml.toString();
    }

    public List<Long> getActiveCategoriesId() {
        return adCategoryDao.getActiveCategoriesId();
    }

    public void importFromXml(final String xml) {
        final Document doc = XmlHelper.readDocument(xml);
        final Element root = doc.getDocumentElement();
        // Find the order where the new root categories will start
        int rootOrder = 0;
        final List<AdCategory> rootCategories = listRoot();
        for (final AdCategory adCategory : rootCategories) {
            final Integer order = adCategory.getOrder();
            if (order != null && order > rootOrder) {
                rootOrder = order;
            }
        }
        // Insert the root categories
        final List<Element> childen = XmlHelper.getChilden(root, CATEGORY_ELEMENT);
        for (final Element elem : childen) {
            importCategory(null, ++rootOrder, elem);
        }
        invalidateCache();
    }

    public synchronized List<AdCategory> listLeaf() {
        refreshCacheIfNeeded();
        return leafCache;
    }

    public synchronized List<AdCategory> listRoot() {
        refreshCacheIfNeeded();
        return rootCache;
    }

    public AdCategory load(final Long id, final Relationship... fetch) {
        return adCategoryDao.load(id, fetch);
    }

    public void recalculateAllCounters() {
        final Iterator<AdCategory> categories = adCategoryDao.iterateAll();

        while (categories.hasNext()) {
            final AdCategory category = categories.next();
            recalculateCounters(category, false);
        }

        invalidateCache();
    }

    public int remove(final Long... ids) {
        invalidateCache();
        final AdQuery adQuery = new AdQuery();
        adQuery.setPageForCount();
        for (final Long id : ids) {
            adQuery.setCategory(EntityHelper.reference(AdCategory.class, id));
            if (PageHelper.getTotalCount(adService.search(adQuery)) > 0) {
                throw new DaoException(new DataIntegrityViolationException("category"));
            }
        }
        return adCategoryDao.delete(ids);
    }

    public AdCategory save(final AdCategory category) {
        // Validates whether the ad category is valid or not
        validate(category);
        AdCategory current = null;
        if (category.isTransient()) {
            current = adCategoryDao.insert(category);
            if (category.getParent() != null) {
                category.getParent().getChildren().add(current);
            }
        } else {
            current = adCategoryDao.load(category.getId(), AdCategory.Relationships.CHILDREN);

            // Only the name and active status can be updated
            current.setName(category.getName());

            // When the category is deactivated, we should also deactivate all children
            final boolean deactivated = (current.isActive() && !category.isActive());
            if (deactivated) {
                // this method calls the adCategoryDao.update(current) so the above changes (like name) are also stored.
                deactivateRecursively(current);
                AdCategory parent = current.getParent();
                while (parent != null) {
                    recalculateCounters(parent, false);
                    parent = parent.getParent();
                }
            } else {
                final boolean changedActive = current.isActive() != category.isActive();
                if (changedActive) {
                    current.setActive(category.isActive());
                }
                current = adCategoryDao.update(current);
                if (changedActive) {
                    AdCategory parent = current.getParent();
                    while (parent != null) {
                        recalculateCounters(parent, false);
                        parent = parent.getParent();
                    }
                    recalculateCounters(current, true);
                }
            }
        }
        invalidateCache();
        return current;
    }

    public List<AdCategory> search(final AdCategoryQuery query) {
        return adCategoryDao.search(query);
    }

    public List<AdCategory> searchLeafAdCategories(final AdCategoryQuery query) {
        return adCategoryDao.searchLeafAdCategories(query);
    }

    public void setAdCategoryDao(final AdCategoryDAO adCategoryDao) {
        this.adCategoryDao = adCategoryDao;
    }

    public void setAdDao(final AdDAO adDao) {
        this.adDao = adDao;
    }

    public void setAdService(final AdService adService) {
        this.adService = adService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    /**
     * Set ad category order
     */
    public void setOrder(final Long[] ids) {
        int index = 0;
        for (final Long id : ids) {
            final AdCategory adCategory = load(id);
            adCategory.setOrder(++index);
            adCategoryDao.update(adCategory);
        }
        invalidateCache();
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void validate(final AdCategory category) throws ValidationException {
        getValidator().validate(category);
    }

    private void appendXml(final StringBuilder xml, final AdCategory adCategory) {
        final String indent = StringUtils.repeat("    ", adCategory.getLevel());
        xml.append(String.format("%s<%s %s=\"%s\"", indent, CATEGORY_ELEMENT, NAME_ATTRIBUTE, StringEscapeUtils.escapeXml(adCategory.getName())));
        final Collection<AdCategory> children = adCategory.getChildren();
        if (CollectionUtils.isEmpty(children)) {
            xml.append(" />\n");
        } else {
            xml.append(">\n");
            for (final AdCategory child : children) {
                appendXml(xml, child);
            }
            xml.append(indent).append("</").append(CATEGORY_ELEMENT).append(">\n");
        }
    }

    private void deactivateRecursively(final AdCategory adCategory) {
        adCategory.setActive(false);
        adCategory.setCountOffer(0);
        adCategory.setCountSearch(0);
        adCategoryDao.update(adCategory);
        // Recursively deactivate children
        for (final AdCategory child : adCategory.getChildren()) {
            deactivateRecursively(child);
        }
    }

    /**
     * Fecth all children of the given category
     */
    private void fetchChildren(final AdCategory category) {
        final AdCategory fetched = fetchService.fetch(category, AdCategory.Relationships.CHILDREN);
        final Collection<AdCategory> children = fetched.getChildren();
        if (children == null) {
            return;
        }
        category.setChildren(children);
        for (final Iterator<AdCategory> iterator = children.iterator(); iterator.hasNext();) {
            final AdCategory current = iterator.next();
            if (current.isActive()) {
                fetchChildren(current);
            } else {
                iterator.remove();
            }
        }
    }

    private Validator getValidator() {
        final Validator validator = new Validator("adCategory");
        validator.property("name").required().maxLength(100);
        validator.general(new GeneralValidation() {
            private static final long serialVersionUID = -8975710041548036332L;

            public ValidationError validate(final Object object) {
                final AdCategory category = (AdCategory) object;
                if (category.isActive()) {
                    // Ensure that an active category has no inactive parents
                    AdCategory current = fetchService.fetch(category.getParent(), RelationshipHelper.nested(AdCategory.MAX_LEVEL, AdCategory.Relationships.PARENT));
                    while (current != null) {
                        if (!current.isActive()) {
                            return new ValidationError("adCategory.error.cantActivateCategoryWithInactiveParent");
                        }
                        current = current.getParent();
                    }
                }
                return null;
            }
        });
        return validator;
    }

    private AdCategory importCategory(final AdCategory parent, final int order, final Element elem) {
        Collection<AdCategory> toCheck = null;
        if (parent == null) {
            toCheck = listRoot();
        } else {
            final AdCategory cat = load(parent.getId(), AdCategory.Relationships.CHILDREN);
            toCheck = cat.getChildren();
        }

        if (toCheck != null) {
            for (final AdCategory cat : toCheck) {
                if (cat.getName().equals(StringUtils.trimToNull(elem.getAttribute(NAME_ATTRIBUTE))) && cat.isActive()) {
                    return null;
                }
            }
        }

        AdCategory category = new AdCategory();
        category.setName(StringUtils.trimToNull(elem.getAttribute(NAME_ATTRIBUTE)));
        category.setParent(parent);
        category.setOrder(order);
        category.setActive(true);
        category = adCategoryDao.insert(category);
        int childOrder = 0;
        final List<AdCategory> children = new ArrayList<AdCategory>();
        final List<Element> childCategories = XmlHelper.getChilden(elem, CATEGORY_ELEMENT);
        for (final Element child : childCategories) {
            final AdCategory cat = importCategory(category, ++childOrder, child);
            if (cat != null) {
                children.add(cat);
            }
        }
        category.setChildren(children);
        return category;
    }

    /**
     * Invalidates the cache when some data is changed
     */
    private synchronized void invalidateCache() {
        rootCache = null;
        leafCache = null;
    }

    private void recalculateCounters(final AdCategory category, final boolean recursive) {
        final AdQuery query = new AdQuery();
        query.setStatus(Ad.Status.ACTIVE);
        query.setPageForCount();
        query.setCategory(category);

        // Get the offer count
        query.setTradeType(TradeType.OFFER);
        category.setCountOffer(PageHelper.getTotalCount(adDao.search(query)));

        // Get the search count
        query.setTradeType(TradeType.SEARCH);
        category.setCountSearch(PageHelper.getTotalCount(adDao.search(query)));

        adCategoryDao.update(category);

        if (recursive) {
            for (final AdCategory child : category.getChildren()) {
                recalculateCounters(child, false);
            }
        }
    }

    /**
     * Reads all categories from the database, storing them on memory
     */
    @SuppressWarnings("unchecked")
    private synchronized void refreshCache() {
        final AdCategoryQuery query = new AdCategoryQuery();
        rootCache = adCategoryDao.search(query);

        for (final AdCategory category : rootCache) {
            fetchChildren(category);
        }

        leafCache = adCategoryDao.searchLeafAdCategories(query);
        Collections.sort(leafCache, new BeanComparator("fullName"));
    }

    /**
     * Refresh the cache on the first search
     */
    private synchronized void refreshCacheIfNeeded() {
        if (rootCache == null || leafCache == null) {
            refreshCache();
        }
    }
}
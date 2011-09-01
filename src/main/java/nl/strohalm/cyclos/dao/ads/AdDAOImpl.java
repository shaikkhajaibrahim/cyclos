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
package nl.strohalm.cyclos.dao.ads;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.IndexedDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.dao.exceptions.QueryParseException;
import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.entities.ads.FullTextAdQuery;
import nl.strohalm.cyclos.entities.ads.Ad.Status;
import nl.strohalm.cyclos.entities.customization.fields.AdCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.customization.images.AdImage;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.TimePeriod;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.hibernate.HibernateCustomFieldHandler;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;
import nl.strohalm.cyclos.utils.lucene.Filters;
import nl.strohalm.cyclos.utils.lucene.IndexHandler;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;

/**
 * Implementation class for ad DAO
 * @author rafael
 */
public class AdDAOImpl extends IndexedDAOImpl<Ad> implements AdDAO {

    private static final String[]       FIELDS_FULL_TEXT = { "title", "description", "customValues", "owner.name", "owner.email", "owner.username", "owner.customValues" };
    private HibernateCustomFieldHandler hibernateCustomFieldHandler;

    public AdDAOImpl() {
        super(Ad.class);
    }

    @Override
    public int delete(final boolean flush, final Long... ids) {
        if (ids != null && ids.length > 0) {
            final Map<String, Object> namedParameters = new HashMap<String, Object>();
            namedParameters.put("ids", Arrays.asList(ids));
            executeUpdate("delete from " + AdCustomFieldValue.class.getName() + " v where v.ad.id in (:ids)", namedParameters);
            executeUpdate("delete from " + AdImage.class.getName() + " ai where ai.ad.id in (:ids)", namedParameters);
            final Integer results = CoercionHelper.coerce(Integer.TYPE, executeUpdate("update Ad ad set ad.deleteDate = current_date(), ad.description = null where ad.id in (:ids)", namedParameters));
            if (flush) {
                flush();
            }
            return results;
        } else {
            return 0;
        }
    }

    public List<Ad> fullTextSearch(final FullTextAdQuery adQuery) throws DaoException {
        final String keywords = adQuery.getKeywords();
        final Calendar today = DateHelper.truncate(Calendar.getInstance());
        // When searching by keywords, use the full-text query
        final Query query;
        try {
            query = getQueryParser().parse(keywords);
        } catch (final ParseException e) {
            throw new QueryParseException(e);
        }

        final Filters filters = new Filters();
        filters.addTerms("id", adQuery.getId());
        filters.addTerms("membersNotified", adQuery.getMembersNotified());
        filters.addTerms("category", adQuery.getCategoriesIds());
        filters.addTerms("currency", adQuery.getCurrency());
        filters.addTerms("externalPublication", adQuery.getExternalPublication());
        filters.addRange("price", adQuery.getInitialPrice(), adQuery.getFinalPrice());
        final TimePeriod since = adQuery.getSince();
        if (since != null && since.isValid()) {
            final Calendar sinceDate = since.remove(today);
            filters.addRange("publicationBegin", null, sinceDate);
        }
        filters.addTerms("owner", adQuery.getOwner());
        filters.addTerms("owner.group", adQuery.getGroups());
        filters.addTerms("tradeType", adQuery.getTradeType());
        if (CollectionUtils.isNotEmpty(adQuery.getAdValues())) {
            for (final AdCustomFieldValue fieldValue : adQuery.getAdValues()) {
                final CustomField field = fieldValue.getField();
                filters.addTerms("customValues." + field.getId(), fieldValue.getValue());
            }
        }
        if (CollectionUtils.isNotEmpty(adQuery.getMemberValues())) {
            for (final MemberCustomFieldValue fieldValue : adQuery.getMemberValues()) {
                final CustomField field = fieldValue.getField();
                filters.addTerms("owner.customValues." + field.getId(), fieldValue.getValue());
            }
        }
        if (adQuery.isWithImagesOnly()) {
            filters.addTerms("hasImages", true);
        }
        // Status
        final Status status = adQuery.getStatus();
        if (status != null) {
            final Filter isPermanent = Filters.terms("permanent", true);
            final Filter isNotPermanent = Filters.terms("permanent", false);
            Filter endRange;
            Filter beginRange;
            switch (status) {
                case PERMANENT:
                    // permanent = true
                    filters.add(isPermanent);
                    break;
                case ACTIVE:
                    // permanent = true or (end > today and begin <= today) // neither begin / end are null
                    beginRange = Filters.range("publicationBegin", null, today);
                    endRange = Filters.range("publicationEnd", today, null);
                    filters.add(Filters.or(isPermanent, Filters.and(endRange, beginRange)));
                    break;
                case SCHEDULED:
                    // permanent = false and begin >= today
                    beginRange = Filters.range("publicationBegin", today, null);
                    filters.add(Filters.and(isNotPermanent, beginRange));
                    break;
                case EXPIRED:
                    // permanent = false and end <= today
                    endRange = Filters.range("publicationEnd", null, today, false, false);
                    filters.add(Filters.and(isNotPermanent, endRange));
                    break;
            }
        }
        // Execute the query
        return list(Ad.class, adQuery, query, filters);
    }

    public Integer getNumberOfAds(final Calendar date, final Collection<? extends Group> groups, final Ad.Status status) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder("select count(ad.id) from Ad ad where 1=1");
        hql.append(" and (ad.deleteDate is null or ad.deleteDate > :date)");
        if (!CollectionUtils.isEmpty(groups)) {
            hql.append(" and ad.owner.group in (:groups) ");
            namedParameters.put("groups", groups);
        }
        switch (status) {
            case ACTIVE:
                hql.append(" and ad.publicationPeriod.begin <= :date");
                hql.append(" and (ad.permanent = true or ad.publicationPeriod.end > :date)");
                break;
            case EXPIRED:
                HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.publicationPeriod.begin", "<=", date);
                HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.publicationPeriod.end", "<=", date);
                break;
            case SCHEDULED:
                HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.creationDate", "<=", date);
                HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.publicationPeriod.begin", ">", date);
                break;
        }
        namedParameters.put("date", date);
        return uniqueResult(hql.toString(), namedParameters);
    }

    public Integer getNumberOfCreatedAds(final Period period, final Collection<? extends Group> groups) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder("select count(ad.id) from Ad ad where 1=1");
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "ad.creationDate", period);
        if (!CollectionUtils.isEmpty(groups)) {
            hql.append(" and ad.owner.group in (:groups) ");
            namedParameters.put("groups", groups);
        }
        return uniqueResult(hql.toString(), namedParameters);
    }

    public List<Ad> search(final AdQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append(" select ad");
        hql.append(" from Ad ad inner join ad.owner m left join ad.category c1 left join c1.parent c2 left join c2.parent c3 ");
        hibernateCustomFieldHandler.appendJoins(hql, "ad.customValues", query.getAdValues());
        hibernateCustomFieldHandler.appendJoins(hql, "m.customValues", query.getMemberValues());
        HibernateHelper.appendJoinFetch(hql, getEntityType(), "ad", query.getFetch());
        hql.append(" where 1=1");
        if (query.getCategory() != null) {
            hql.append(" and (c1 = :adCategory or c2 = :adCategory or c3 = :adCategory)");
            namedParameters.put("adCategory", query.getCategory());
        }
        if (!query.isIncludeDeleted()) {
            hql.append(" and ad.deleteDate is null ");
        }

        HibernateHelper.addParameterToQuery(hql, namedParameters, "ad.category.active", true);
        HibernateHelper.addParameterToQuery(hql, namedParameters, "ad.id", query.getId());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "ad.membersNotified", query.getMembersNotified());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "ad.externalPublication", query.getExternalPublication());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "m", query.getOwner());
        // Group filters are handled at service level
        HibernateHelper.addInParameterToQuery(hql, namedParameters, "m.group", query.getGroups());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "ad.tradeType", query.getTradeType());
        HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.price", ">=", query.getInitialPrice());
        HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.price", "<=", query.getFinalPrice());
        HibernateHelper.addParameterToQuery(hql, namedParameters, "ad.currency", query.getCurrency());
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "ad.publicationPeriod.begin", Period.day(query.getBeginDate()));
        HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "ad.publicationPeriod.end", Period.day(query.getEndDate()));

        final Calendar now = Calendar.getInstance();
        // Since
        if (query.getSince() != null && query.getSince().getNumber() > 0) {
            final Calendar since = DateHelper.truncate(query.getSince().remove(now));
            HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.publicationPeriod.begin", ">=", since);
        }

        // With Images Only
        if (query.isWithImagesOnly()) {
            hql.append(" and exists (select img.id from AdImage img where img.ad = ad) ");
        }

        // Check the history date
        final Calendar historyDate = (Calendar) ObjectUtils.defaultIfNull(query.getHistoryDate(), Calendar.getInstance());
        HibernateHelper.addParameterToQueryOperator(hql, namedParameters, "ad.creationDate", "<=", historyDate);
        hql.append(" and (ad.deleteDate is null or ad.deleteDate >= :historyDate)");
        namedParameters.put("historyDate", historyDate);

        // Status
        if (query.getStatus() != null) {
            switch (query.getStatus()) {
                case PERMANENT:
                    hql.append(" and ad.permanent = true ");
                    break;
                case ACTIVE:
                    hql.append(" and (ad.permanent = true or ((ad.publicationPeriod.end is null or ad.publicationPeriod.end >= :historyDate) and ad.publicationPeriod.begin <= :historyDate)) ");
                    break;
                case SCHEDULED:
                    hql.append(" and (ad.permanent is null or ad.permanent = false) and ad.publicationPeriod.begin > :historyDate ");
                    break;
                case EXPIRED:
                    hql.append(" and (ad.permanent is null or ad.permanent = false) and ad.publicationPeriod.end < :historyDate ");
                    break;
            }
        }

        // Keywords
        if (StringUtils.isNotEmpty(query.getKeywords())) {
            hql.append(" and ((ad.title like :keywords) or (ad.description like :keywords))");
            namedParameters.put("keywords", "%" + query.getKeywords() + "%");
        }
        // Custom Values
        hibernateCustomFieldHandler.appendConditions(hql, namedParameters, query.getAdValues());
        hibernateCustomFieldHandler.appendConditions(hql, namedParameters, query.getMemberValues());

        // Handle order
        if (query.isRandomOrder()) {
            HibernateHelper.appendOrder(hql, "rand()");
        } else {
            HibernateHelper.appendOrder(hql, "ad.publicationPeriod.begin desc");
        }
        return list(query, hql.toString(), namedParameters);
    }

    public void setHibernateCustomFieldHandler(final HibernateCustomFieldHandler hibernateCustomFieldHandler) {
        this.hibernateCustomFieldHandler = hibernateCustomFieldHandler;
    }

    @Override
    protected void appendAllIndexedEntitiesQuery(final Class<? extends Ad> type, final StringBuilder hql, final String alias, final Map<String, Object> params) {
        hql.append(" and ").append(alias).append(".deleteDate is null");
    }

    private MultiFieldQueryParser getQueryParser() {
        final Map<String, Float> boosts = new HashMap<String, Float>();
        boosts.put("title", 2.5F);
        boosts.put("description", 2F);
        boosts.put("owner.name", 1.5F);
        boosts.put("owner.username", 1.3F);
        return new MultiFieldQueryParser(IndexHandler.LUCENE_VERSION, FIELDS_FULL_TEXT, getAnalyzer(), boosts);
    }
}
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.entities.ads.AdCategoryQuery;
import nl.strohalm.cyclos.entities.ads.AdQuery;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;

/**
 * Implementation class for ad category DAO
 * @author rafael
 * @author Lucas Geiss
 */
public class AdCategoryDAOImpl extends BaseDAOImpl<AdCategory> implements AdCategoryDAO {

    public AdCategoryDAOImpl() {
        super(AdCategory.class);
    }

    public List<Long> getActiveCategoriesId() {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = new StringBuilder();
        hql.append("select cat.id from AdCategory cat");
        hql.append(" where 1=1");

        HibernateHelper.addParameterToQuery(hql, namedParameters, "cat.active", true);

        return list(new AdQuery(), hql.toString(), namedParameters);
    }

    @SuppressWarnings("unchecked")
    public Iterator<AdCategory> iterateAll() {
        return getHibernateTemplate().iterate("from " + getEntityType().getName());
    }

    public List<AdCategory> search(final AdCategoryQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final Set<Relationship> fetch = query.getFetch();
        final StringBuilder hql = HibernateHelper.getInitialQuery(getEntityType(), "c", fetch);
        if (!query.isReturnDisabled()) {
            hql.append(" and c.active = true ");
            hql.append(" and not exists (select p.id from AdCategory p where c.parent = p and p.active <> true) ");
            hql.append(" and not exists (select p.id from AdCategory p where c.parent.parent = p and p.active <> true) ");
        }
        if (query.getParent() != null) {
            HibernateHelper.addParameterToQuery(hql, namedParameters, "c.parent", query.getParent());
        } else {
            hql.append(" and c.parent is null ");
        }
        String[] order;
        if (query.isOrderAlphabetically()) {
            order = new String[] { "c.name" };
        } else {
            order = new String[] { "c.order", "c.name" };
        }
        HibernateHelper.appendOrder(hql, order);
        return list(query, hql.toString(), namedParameters);
    }

    public List<AdCategory> searchLeafAdCategories(final AdCategoryQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final Set<Relationship> fetch = query.getFetch();
        final StringBuilder hql = HibernateHelper.getInitialQuery(getEntityType(), "c", fetch);
        if (!query.isReturnDisabled()) {
            hql.append(" and c.active = true ");
            hql.append(" and not exists (select p.id from AdCategory p where c.parent = p and p.active <> true) ");
            hql.append(" and not exists (select p.id from AdCategory p where c.parent.parent = p and p.active <> true) ");
        }
        hql.append(" and not exists (select c1.id from AdCategory c1 where c1.parent = c) ");
        String[] order;
        if (query.isOrderAlphabetically()) {
            order = new String[] { "c.name" };
        } else {
            order = new String[] { "c.order", "c.name" };
        }
        HibernateHelper.appendOrder(hql, order);
        return list(query, hql.toString(), namedParameters);
    }

}
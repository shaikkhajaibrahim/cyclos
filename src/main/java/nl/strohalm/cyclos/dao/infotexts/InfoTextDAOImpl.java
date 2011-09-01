/*
    This file is part of Cyclos <http://project.cyclos.org>

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.dao.infotexts;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.entities.infotexts.InfoText;
import nl.strohalm.cyclos.entities.infotexts.InfoTextQuery;
import nl.strohalm.cyclos.utils.DateHelper;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;

import org.apache.commons.lang.StringUtils;

public class InfoTextDAOImpl extends BaseDAOImpl<InfoText> implements InfoTextDAO {

    public InfoTextDAOImpl() {
        super(InfoText.class);
    }

    public List<InfoText> search(final InfoTextQuery query) {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = HibernateHelper.getInitialQuery(InfoText.class, "info", query.getFetch());

        final String alias = StringUtils.trimToNull(query.getAlias());
        if (alias != null) {
            hql.append(" and :alias in elements(info.aliases) ");
            namedParameters.put("alias", alias);
        }

        if (query.getKeywords() != null) {
            hql.append(" and (info.subject like :keywords or info.body like :keywords) ");
            namedParameters.put("keywords", "%" + query.getKeywords().toUpperCase() + "%");
        }

        if (query.isWithBodyOnly()) {
            hql.append(" and length(info.body) > 0");
        }

        if (query.isOnlyActive()) {
            HibernateHelper.addParameterToQuery(hql, namedParameters, "info.enabled", Boolean.TRUE);
            final Period period = Period.day(Calendar.getInstance());
            hql.append(" and (info.validity.begin is null or info.validity.begin < :_end_) and (info.validity.end is null or info.validity.end >= :_begin_)");
            namedParameters.put("_begin_", DateHelper.getBeginForParameter(period));
            namedParameters.put("_end_", DateHelper.getEndForParameter(period));
        } else {
            HibernateHelper.addParameterToQuery(hql, namedParameters, "info.enabled", query.getEnabled());
            HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "info.validity.begin", query.getStartIn());
            HibernateHelper.addPeriodParameterToQuery(hql, namedParameters, "info.validity.end", query.getEndIn());
        }

        return list(query, hql.toString(), namedParameters);
    }
}

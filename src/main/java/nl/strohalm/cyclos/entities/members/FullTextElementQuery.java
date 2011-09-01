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
package nl.strohalm.cyclos.entities.members;

import java.util.Collection;

import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.settings.LocalSettings.MemberResultDisplay;
import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.query.QueryParameters;

/**
 * Query parameters for a full text query for elements
 * @author luis
 */
public abstract class FullTextElementQuery extends QueryParameters {
    private static final long serialVersionUID = 5541605137638733118L;
    private String                                 keywords;
    private Collection<? extends Group>            groups;
    private Boolean                                enabled;
    private Collection<? extends CustomFieldValue> customValues;
    private MemberResultDisplay                    order;

    public Collection<? extends CustomFieldValue> getCustomValues() {
        return customValues;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Collection<? extends Group> getGroups() {
        return groups;
    }

    public String getKeywords() {
        return keywords;
    }

    public abstract Element.Nature getNature();

    public MemberResultDisplay getOrder() {
        return order;
    }

    public abstract Class<? extends ElementQuery> getQueryClass();

    public void setCustomValues(final Collection<? extends CustomFieldValue> customValues) {
        this.customValues = customValues;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    public void setGroups(final Collection<? extends Group> groups) {
        this.groups = groups;
    }

    public void setKeywords(final String keywords) {
        this.keywords = keywords;
    }

    public void setOrder(final MemberResultDisplay order) {
        this.order = order;
    }

    public ElementQuery toQuery() {
        final ElementQuery query = ClassHelper.instantiate(getQueryClass());
        PropertyHelper.copyProperties(this, query);
        return query;
    }
}

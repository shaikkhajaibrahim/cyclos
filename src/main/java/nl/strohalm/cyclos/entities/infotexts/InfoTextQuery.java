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
package nl.strohalm.cyclos.entities.infotexts;

import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.query.QueryParameters;

public class InfoTextQuery extends QueryParameters {

    private static final long serialVersionUID = -3579899318396793812L;
    private String            keywords;
    private Period            startIn;
    private Period            endIn;
    private boolean           onlyActive;
    private Boolean           enabled;
    private String            alias;
    private boolean           withBodyOnly;

    public String getAlias() {
        return alias;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public Period getEndIn() {
        return endIn;
    }

    public String getKeywords() {
        return keywords;
    }

    public Period getStartIn() {
        return startIn;
    }

    public boolean isOnlyActive() {
        return onlyActive;
    }

    public boolean isWithBodyOnly() {
        return withBodyOnly;
    }

    public void setAlias(final String alias) {
        this.alias = alias;
    }

    public void setEnabled(final Boolean enabled) {
        this.enabled = enabled;
    }

    public void setEndIn(final Period endIn) {
        this.endIn = endIn;
    }

    public void setKeywords(final String keywords) {
        this.keywords = keywords;
    }

    public void setOnlyActive(final boolean onlyActive) {
        this.onlyActive = onlyActive;
    }

    public void setStartIn(final Period startIn) {
        this.startIn = startIn;
    }

    public void setWithBodyOnly(final boolean withBodyOnly) {
        this.withBodyOnly = withBodyOnly;
    }
}

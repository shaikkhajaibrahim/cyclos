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
package nl.strohalm.cyclos.services.infotexts;

import java.util.List;

import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.infotexts.InfoText;
import nl.strohalm.cyclos.entities.infotexts.InfoTextQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

public interface InfoTextService extends Service {
    public static final String INFO_TEXT_EMPTY_PROPERTY     = "infoText.empty.subject";
    public static final String INFO_TEXT_NOT_MATCH_PROPERTY = "infoText.nomatch.subject";

    /**
     * Gets the info text's subject associated with the alias parameter<br>
     * If the info text couldn't be found then returns the internationalized value for the key <code><b>infoText.nomatch.subject</b></code>.<br>
     * If the alias parameter is null then returns the internationalized value for the key <code><b>infoText.empty.subject</b></code>.
     * @param alias the info texts's alias to search for
     * @return the info text's subject according to the specified alias<br>
     */
    @SystemAction
    String getInfoTextSubject(String alias);

    @AdminAction(@Permission(module = "systemInfoTexts", operation = "view"))
    InfoText load(long id, Relationship... fetch) throws EntityNotFoundException;

    /**
     * Loads an info text by alias without permission checks
     */
    @SystemAction
    InfoText loadByAliasForWebServices(String alias) throws EntityNotFoundException;

    /**
     * Loads an info text without permission checks
     */
    @SystemAction
    InfoText loadForWebServices(long id) throws EntityNotFoundException;

    /**
     * Removes the info texts with the given ids.
     */
    @AdminAction(@Permission(module = "systemInfoTexts", operation = "manage"))
    int remove(Long... ids);

    @AdminAction(@Permission(module = "systemInfoTexts", operation = "manage"))
    InfoText save(InfoText infoText);

    @AdminAction(@Permission(module = "systemInfoTexts", operation = "view"))
    List<InfoText> search(InfoTextQuery query);

    /**
     * Searches for matching info texts without permission checks
     */
    @SystemAction
    List<InfoText> searchForWebServices(InfoTextQuery query);

    @AdminAction(@Permission(module = "systemInfoTexts", operation = "manage"))
    void validate(final InfoText infoText);
}

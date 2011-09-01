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
package nl.strohalm.cyclos.services.accounts.external;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.external.ExternalAccount;
import nl.strohalm.cyclos.entities.accounts.external.ExternalAccountDetailsVO;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for External Accounts
 * @author Lucas Geiss
 */
public interface ExternalAccountService extends Service {

    /**
     * Overview external accounts
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "details"))
    public List<ExternalAccountDetailsVO> externalAccountOverview();

    /**
     * Loads a ExternalAccount by id
     */
    @AdminAction( {
            @Permission(module = "systemExternalAccounts", operation = "view"),
            @Permission(module = "systemExternalAccounts", operation = "details") })
    ExternalAccount load(Long id, Relationship... fetch);

    /**
     * Removes the specified External Account
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "manage"))
    int remove(Long... ids);

    /**
     * Saves the External Account
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "manage"))
    ExternalAccount save(ExternalAccount externalAccount);

    /**
     * Search External Accounts related system account type
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "view"))
    List<ExternalAccount> search();

    /**
     * Validate the specified External Account
     */
    @DontEnforcePermission(traceable = true)
    void validate(ExternalAccount externalAccount);

}
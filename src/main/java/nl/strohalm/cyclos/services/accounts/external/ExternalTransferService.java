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

import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransfer;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransferAction;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransferImport;
import nl.strohalm.cyclos.entities.accounts.external.ExternalTransferQuery;
import nl.strohalm.cyclos.exceptions.UnexpectedEntityException;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;
import nl.strohalm.cyclos.utils.transactionimport.TransactionImportDTO;

/**
 * Service interface for handling external transfers
 * @author luis, Jefferson Magno
 */
public interface ExternalTransferService extends Service {

    /**
     * Imports a transfer, according to the given parameters
     */
    @SystemAction
    ExternalTransfer importNew(ExternalTransferImport transferImport, TransactionImportDTO dto);

    /**
     * Loads an external transfer by id
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "details"))
    ExternalTransfer load(Long id, Relationship... fetch);

    /**
     * Perform a given action on multiple external transfers
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    void performAction(ExternalTransferAction action, Long... ids);

    /**
     * Process the given external transfers
     * @throws UnexpectedEntityException When one of the the processing parameters are inconsistent
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "processPayment"))
    int process(Collection<ProcessExternalTransferDTO> dtos) throws UnexpectedEntityException;

    /**
     * Saves the external transfer
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "manage"))
    ExternalTransfer save(ExternalTransfer fieldMapping);

    /**
     * Searches for external transfers
     */
    @AdminAction(@Permission(module = "systemExternalAccounts", operation = "details"))
    List<ExternalTransfer> search(ExternalTransferQuery query);

    /**
     * Validate the specified external transfer
     */
    @DontEnforcePermission(traceable = true)
    void validate(ExternalTransfer externalTransfer);
}

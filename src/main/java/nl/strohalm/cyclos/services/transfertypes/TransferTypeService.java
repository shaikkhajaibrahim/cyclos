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
package nl.strohalm.cyclos.services.transfertypes;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Service interface for transfer types
 * @author luis
 */
public interface TransferTypeService extends Service {

    /**
     * Returns a list with all channels to be showed in the transfer type edition<br>
     * This kind of method is necessary only by security purposes because it ensure the right access control
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    List<Channel> channelsForTransferTypeEdition();

    /**
     * gets a list with all possible conversion TT's available for any member account and any currency. A TT is considered a conversion if it goes
     * from member to an unlimited system account.
     */
    // TODO RATES 5: SET PERMISSIONS
    List<TransferType> getConversionTTs();

    /**
     * gets a list with all possible converstion TT's for the specified accountType
     */
    // TODO RATES 5 : permissions?
    List<TransferType> getConversionTTs(AccountType fromAccountType);

    /**
     * gets a list with all possible conversion TT's available for any member account of the specified currency. A TT is considered a conversion if it
     * goes from member to an unlimited system account.
     */
    // TODO RATES 5: SET PERMISSIONS
    List<TransferType> getConversionTTs(Currency currency);

    /**
     * get a list with transferTypes having A-rated Transaction Fees
     */
    @AdminAction(@Permission(module = "systemReports", operation = "aRateConfigSimulation"))
    List<TransferType> listARatedTTs();

    /**
     * Loads the specified transfer type, fetching the specified relationships
     */
    TransferType load(Long id, Relationship... fetch);

    /**
     * Removes the specified transfer types
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    int remove(Long... ids);

    /**
     * Saves the transfer type, returning the resulting object
     */
    @AdminAction(@Permission(module = "systemAccounts", operation = "manage"))
    TransferType save(TransferType transferType);

    /**
     * Search the existing transfer types
     */
    List<TransferType> search(TransferTypeQuery query);

    /**
     * Validate the specified transfer type
     */
    void validate(TransferType transferType);
}

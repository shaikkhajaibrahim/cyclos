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
package nl.strohalm.cyclos.services.services;

import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.services.ServiceClient;
import nl.strohalm.cyclos.entities.services.ServiceClientQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

/**
 * Service interface for service clients
 * @author luis
 */
public interface ServiceClientService extends Service {

    /**
     * Removes all hosts with the given ids
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "manage"))
    int delete(Long... ids);

    /**
     * Finds a service client for the given address, with the given username and password
     */
    @SystemAction
    ServiceClient findByAddressAndCredentials(String address, String username, String password);

    /**
     * Lists the channels which may be used in a service client
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "view"))
    List<Channel> listPossibleChannels();

    /**
     * Lists the possible transfer for doPayment with the given client
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "view"))
    List<TransferType> listPossibleDoPaymentTypes(ServiceClient client);

    /**
     * Lists the possible transfer for receivePayment with the given client
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "view"))
    List<TransferType> listPossibleReceivePaymentTypes(ServiceClient client);

    /**
     * Loads a service client with the specified fetch
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "view"))
    ServiceClient load(Long id, Relationship... fetch);

    /**
     * Save a host
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "manage"))
    ServiceClient save(ServiceClient host);

    /**
     * Searches for service clients
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "view"))
    @IgnoreMember
    List<ServiceClient> search(ServiceClientQuery query);

    /**
     * Validates a service client
     */
    @AdminAction(@Permission(module = "systemServiceClients", operation = "manage"))
    void validate(ServiceClient client);
}

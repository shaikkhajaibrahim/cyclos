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
package mp.platform.cyclone.webservices.webshop;

import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.entities.accounts.transactions.WebShopTicket;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.transactions.TicketService;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.model.WebShopTicketVO;
import mp.platform.cyclone.webservices.utils.WebServiceHelper;
import mp.platform.cyclone.webservices.utils.server.TicketHelper;

/**
 * Webshop web service implementation
 * @author luis
 */
@WebService(name = "webshop", serviceName = "webshop")
public class WebShopWebServiceImpl implements WebShopWebService {

    private TicketService    ticketService;
    private TicketHelper     ticketHelper;
    private WebServiceHelper webServiceHelper;

    public String generate(final GenerateWebShopTicketParams params) {
        try {
            final HttpServletRequest request = WebServiceContext.getRequest();
            // the toTicket() checks the member restriction too
            final WebShopTicket ticket = ticketHelper.toTicket(params);
            ticket.setMemberAddress(request.getRemoteAddr());
            final WebShopTicket object = ticketService.generate(ticket);
            return object.getTicket();
        } catch (final Exception e) {
            webServiceHelper.error(e);
            return null;
        }
    }

    public WebShopTicketVO get(final String ticket) {
        try {
            final WebShopTicket object = (WebShopTicket) ticketService.load(ticket);

            // Check the member restriction
            final Member restricted = WebServiceContext.getMember();
            if (restricted != null && !restricted.equals(object.getTo())) {
                throw new Exception("Error getting webshop ticket: the target member ('to') of the webshop ticket is not the restricted one");
            }

            return ticketHelper.toVO(object);
        } catch (final Exception e) {
            webServiceHelper.error(e);
            return null;
        }
    }

    public TicketHelper getTicketHelper() {
        return ticketHelper;
    }

    public TicketService getTicketService() {
        return ticketService;
    }

    public void setTicketHelper(final TicketHelper ticketHelper) {
        this.ticketHelper = ticketHelper;
    }

    public void setTicketService(final TicketService ticketService) {
        this.ticketService = ticketService;
    }

    public void setWebServiceHelper(final WebServiceHelper webServiceHelper) {
        this.webServiceHelper = webServiceHelper;
    }
}

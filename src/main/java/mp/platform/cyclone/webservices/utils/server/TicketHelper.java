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
package mp.platform.cyclone.webservices.utils.server;

import java.util.List;

import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.PaymentRequestTicket;
import nl.strohalm.cyclos.entities.accounts.transactions.Ticket;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.WebShopTicket;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.model.PaymentRequestTicketVO;
import mp.platform.cyclone.webservices.model.TicketVO;
import mp.platform.cyclone.webservices.model.WebShopTicketVO;
import mp.platform.cyclone.webservices.webshop.GenerateWebShopTicketParams;

/**
 * Utility class for tickets
 * @author luis
 */
public class TicketHelper {

    private CustomFieldService customFieldService;
    private SettingsService    settingsService;
    private ElementService     elementService;
    private CurrencyHelper     currencyHelper;
    private MemberHelper       memberHelper;
    private FetchService       fetchService;

    public void setCurrencyHelper(final CurrencyHelper currencyHelper) {
        this.currencyHelper = currencyHelper;
    }

    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Convert a params object into a ticket
     */
    public WebShopTicket toTicket(final GenerateWebShopTicketParams params) {
        if (params == null) {
            return null;
        }
        final WebShopTicket ticket = new WebShopTicket();
        ticket.setAmount(params.getAmount());
        ticket.setCurrency(currencyHelper.resolve(params.getCurrency()));
        ticket.setClientAddress(params.getClientAddress());
        ticket.setDescription(params.getDescription());
        ticket.setReturnUrl(params.getReturnUrl());

        // Check the member restriction
        final Member restricted = WebServiceContext.getMember();
        if (restricted != null) {
            ticket.setTo(restricted);
        } else {
            try {
                final User user = elementService.loadUser(params.getToUsername(), User.Relationships.ELEMENT);
                if (user instanceof MemberUser) {
                    ticket.setTo(((MemberUser) user).getMember());
                } else {
                    throw new IllegalArgumentException("Invalid username: " + params.getToUsername() + ". It isn't an instance of MemberUser");
                }
            } catch (final Exception e) {
                throw new IllegalArgumentException("Invalid member: " + params.getToUsername());
            }
        }
        return ticket;
    }

    public PaymentRequestTicketVO toVO(final PaymentRequestTicket ticket) {
        if (ticket == null) {
            return null;
        }
        final PaymentRequestTicketVO vo = new PaymentRequestTicketVO();
        fill(ticket, vo, true);
        vo.setFromChannel(ticket.getFromChannel().getInternalName());
        vo.setToChannel(ticket.getToChannel().getInternalName());
        return vo;
    }

    public WebShopTicketVO toVO(final WebShopTicket ticket) {
        if (ticket == null) {
            return null;
        }
        final WebShopTicketVO vo = new WebShopTicketVO();
        fill(ticket, vo, false);
        vo.setMemberAddress(ticket.getMemberAddress());
        vo.setClientAddress(ticket.getClientAddress());
        vo.setReturnUrl(ticket.getReturnUrl());
        return vo;
    }

    @SuppressWarnings("unchecked")
    private void fill(final Ticket ticket, final TicketVO vo, final boolean onlyBasicCustomFields) {
        final Transfer transfer = ticket.getTransfer();
        Currency currency = ticket.getCurrency();
        if (currency == null && ticket.getTransferType() != null) {
            currency = ticket.getTransferType().getFrom().getCurrency();
        }
        final LocalSettings localSettings = settingsService.getLocalSettings();
        vo.setId(ticket.getId());
        vo.setTicket(ticket.getTicket());
        vo.setAwaitingAuthorization(transfer != null && transfer.getStatus() == Payment.Status.PENDING);
        vo.setOk(!vo.isAwaitingAuthorization() && ticket.getStatus() == Ticket.Status.OK);
        vo.setCancelled(ticket.getStatus() == Ticket.Status.CANCELLED);
        vo.setExpired(ticket.getStatus() == Ticket.Status.EXPIRED);
        vo.setPending(ticket.getStatus() == Ticket.Status.PENDING);

        final List<MemberCustomField> customFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        final Member from = fetchService.fetch(ticket.getFrom(), Element.Relationships.GROUP);
        if (from != null) {
            final List<MemberCustomField> fields = CustomFieldHelper.onlyForGroup(customFields, from.getMemberGroup());
            vo.setFromMember(memberHelper.toVO(from, CustomFieldHelper.onlyBasic(fields), false));
        }
        final Member to = fetchService.fetch(ticket.getTo(), Element.Relationships.GROUP);
        if (to != null) {
            final List<MemberCustomField> fields = CustomFieldHelper.onlyForGroup(customFields, to.getMemberGroup());
            vo.setToMember(memberHelper.toVO(to, CustomFieldHelper.onlyBasic(fields), false));
        }
        vo.setAmount(ticket.getAmount());
        if (currency == null) {
            vo.setFormattedAmount(localSettings.getNumberConverter().toString(ticket.getAmount()));
        } else {
            vo.setFormattedAmount(localSettings.getUnitsConverter(currency.getPattern()).toString(ticket.getAmount()));
        }
        vo.setDescription(ticket.getDescription());
        vo.setCreationDate(ticket.getCreationDate());
        vo.setFormattedCreationDate(localSettings.getDateTimeConverter().toString(ticket.getCreationDate()));
    }
}

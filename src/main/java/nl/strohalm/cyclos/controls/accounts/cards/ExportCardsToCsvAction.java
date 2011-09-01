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
/**
 * 
 */
package nl.strohalm.cyclos.controls.accounts.cards;

import java.util.List;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseCsvAction;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.cards.Card;
import nl.strohalm.cyclos.entities.accounts.cards.CardQuery;
import nl.strohalm.cyclos.entities.accounts.cards.CardType;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.accounts.cards.CardService;
import nl.strohalm.cyclos.services.accounts.cards.CardTypeService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;
import nl.strohalm.cyclos.utils.conversion.CustomFieldConverter;
import nl.strohalm.cyclos.utils.csv.CSVWriter;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;

/**
 * 
 * @author rodrigo
 */
@AdminAction( { @Permission(module = "adminMemberCards", operation = "view") })
@IgnoreMember
public class ExportCardsToCsvAction extends BaseCsvAction {

    private CardTypeService       cardTypeService;
    private DataBinder<CardQuery> dataBinder;
    protected CardService         cardService;
    protected CustomFieldService  customFieldService;
    private ElementService        elementService;
    protected GroupService        groupService;

    public CardService getCardService() {
        return cardService;
    }

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public DataBinder<CardQuery> getDataBinder() {
        if (dataBinder == null) {
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            final BeanBinder<CardQuery> binder = BeanBinder.instance(CardQuery.class);
            binder.registerBinder("groups", SimpleCollectionBinder.instance(Group.class, "groups"));
            binder.registerBinder("status", SimpleCollectionBinder.instance(Card.Status.class, "status"));
            binder.registerBinder("expiration", DataBinderHelper.periodBinder(localSettings, "expiration"));
            binder.registerBinder("member", PropertyBinder.instance(Member.class, "member"));
            binder.registerBinder("number", PropertyBinder.instance(Card.class, "number"));
            binder.registerBinder("cardType", PropertyBinder.instance(CardType.class, "cardType"));
            binder.registerBinder("customValues", SimpleCollectionBinder.instance(MemberCustomField.class, "customValues"));

            dataBinder = binder;
        }
        return dataBinder;
    }

    @Inject
    public void setCardService(final CardService cardService) {
        this.cardService = cardService;
    }

    @Inject
    public void setCardTypeService(final CardTypeService cardTypeService) {
        this.cardTypeService = cardTypeService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Inject
    public void setGroupService(final GroupService groupService) {
        this.groupService = groupService;
    }

    @Override
    protected List<Card> executeQuery(final ActionContext context) {
        final SearchCardsForm form = context.getForm();
        final CardQuery query = getDataBinder().readFromString(form.getQuery());
        query.setResultType(ResultType.ITERATOR);
        SearchCardsAction.prepareQuery(context, query, elementService, groupService);
        final List<Card> cards = cardService.search(query);
        return cards;
    }

    @Override
    protected String fileName(final ActionContext context) {
        final User loggedUser = context.getUser();
        return "cards_" + loggedUser.getUsername() + ".csv";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected CSVWriter initCsvWriter(final ActionContext context) {

        final SearchCardsForm form = context.getForm();
        CardType cardType = CoercionHelper.coerce(CardType.class, form.getQuery("cardType"));
        // if cardType is null is because there is only one CardType, and when it happens we doesn't show CardType filter
        if (cardType == null) {
            final List<CardType> cardTypes = cardTypeService.listAll();
            cardType = cardTypes.get(0);
        }
        cardType = getFetchService().fetch(cardType);

        final LocalSettings settings = getLocalSettings();
        final CSVWriter<Card> csv = CSVWriter.instance(Card.class, settings);

        csv.addColumn(context.message("card.member"), "owner.name");
        csv.addColumn(context.message("member.username"), "owner.username");
        csv.addColumn(context.message("card.number"), "cardNumber", cardType.getCardNumberConverter());
        csv.addColumn(context.message("card.expirationDate"), "expirationDate", settings.getDateConverter());
        csv.addColumn(context.message("card.group"), "owner.group.name");
        // Member custom field values
        final List<MemberCustomField> customFields = (List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER);
        for (final MemberCustomField field : customFields) {
            csv.addColumn(field.getName(), "owner.customValues", new CustomFieldConverter(field));
        }

        if (cardType.getCardSecurityCode().equals(CardType.CardSecurityCode.AUTOMATIC)) {
            csv.addColumn(context.message("cardType.cardSecurityCode"), "cardSecurityCode");
        }

        return csv;
    }

    @Override
    protected boolean useCsvWriterPerRequest() {
        return true;
    }

}

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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.annotations.RequestParameter;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.access.User.TransactionPasswordStatus;
import nl.strohalm.cyclos.entities.accounts.cards.Card;
import nl.strohalm.cyclos.entities.accounts.cards.CardLog;
import nl.strohalm.cyclos.entities.accounts.cards.CardQuery;
import nl.strohalm.cyclos.entities.accounts.cards.CardType;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.accounts.cards.CardService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.RangeConstraint;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.ActionHelper.ByElementExtractor;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.DataBinderHelper;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.binding.SimpleCollectionBinder;
import nl.strohalm.cyclos.utils.validation.ValidationException;

/**
 * 
 * @author rodrigo
 */
@AdminAction( { @Permission(module = "adminMemberCards", operation = "view") })
@MemberAction( { @Permission(module = "memberCards", operation = "view") })
@BrokerAction( { @Permission(module = "brokerCards", operation = "view") })
@RelatedEntity(Card.class)
@RequestParameter("cardId")
@PathToMember("owner")
public class CardDetailsAction extends BaseFormAction implements LocalSettingsChangeListener {

    private DataBinder<CardQuery> dataBinder;

    protected GroupService        groupService;
    protected CardService         cardService;
    protected ElementService      elementService;
    private ReadWriteLock         lock = new ReentrantReadWriteLock(true);

    public CardService getCardService() {
        return cardService;
    }

    public DataBinder<CardQuery> getDataBinder() {
        try {
            lock.readLock().lock();
            if (dataBinder == null) {
                final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
                final BeanBinder<CardQuery> binder = BeanBinder.instance(CardQuery.class);
                binder.registerBinder("groups", SimpleCollectionBinder.instance(Group.class, "groups"));
                binder.registerBinder("status", SimpleCollectionBinder.instance(Card.Status.class, "status"));
                binder.registerBinder("expiration", DataBinderHelper.periodBinder(localSettings, "expiration"));
                binder.registerBinder("member", PropertyBinder.instance(Member.class, "member"));
                binder.registerBinder("number", PropertyBinder.instance(Card.class, "number"));

                dataBinder = binder;
            }
            return dataBinder;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        try {
            lock.writeLock().lock();

            dataBinder = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Inject
    public void setCardService(final CardService cardService) {
        this.cardService = cardService;
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
    protected void prepareForm(final ActionContext context) {

        final CardForm form = context.getForm();
        final HttpServletRequest request = context.getRequest();
        final long cardId = form.getCardId();
        final String listOnly = form.getListOnly();

        boolean canBlock = false;
        boolean canUnblock = false;
        boolean canActivate = false;
        boolean canCancel = false;
        boolean canChangeCode = false;
        boolean canUnblockSecurityCode = false;
        boolean showUnblockSecurityCodeButton = false;
        final PermissionService pService = getPermissionService();

        Card card;
        try {
            if (cardId > 0) {
                card = cardService.load(cardId, Element.Relationships.USER);
                if ((!context.isBroker() && !context.isAdmin() && !card.getOwner().equals(context.getAccountOwner())) || (context.isBroker() && !context.isBrokerOf(card.getOwner()) && !card.getOwner().equals(context.getAccountOwner()))) {
                    throw new ValidationException();
                }
            } else {
                throw new ValidationException();
            }

            if (context.isAdmin()) {
                final String module = "adminMemberCards";
                switch (card.getStatus()) {
                    case ACTIVE:
                        canBlock = pService.checkPermission(module, "block");
                        break;
                    case BLOCKED:
                        canUnblock = pService.checkPermission(module, "unblock");
                        break;
                    case PENDING:
                        canActivate = pService.checkPermission(module, "unblock");
                        break;
                }
                canCancel = pService.checkPermission(module, "cancel");
                canChangeCode = pService.checkPermission(module, "changeCardSecurityCode");
                canUnblockSecurityCode = pService.checkPermission(module, "unblockSecurityCode");

            }
            if (context.isBrokerOf(card.getOwner())) {
                final String module = "brokerCards";
                switch (card.getStatus()) {
                    case ACTIVE:
                        canBlock = pService.checkPermission(module, "block");
                        break;
                    case BLOCKED:
                        canUnblock = pService.checkPermission(module, "unblock");
                        break;
                    case PENDING:
                        canActivate = pService.checkPermission(module, "unblock");
                        break;
                }
                canCancel = getPermissionService().checkPermission(module, "cancel");
                canChangeCode = getPermissionService().checkPermission(module, "changeCardSecurityCode");
                canUnblockSecurityCode = pService.checkPermission(module, "unblockSecurityCode");
            }
            if (context.isMember() && card.getOwner().equals((context.getAccountOwner()))) {
                final String module = "memberCards";
                switch (card.getStatus()) {
                    case ACTIVE:
                        canBlock = pService.checkPermission(module, "block");
                        break;
                    case BLOCKED:
                        canUnblock = pService.checkPermission(module, "unblock");
                        break;
                    case PENDING:
                        canActivate = pService.checkPermission(module, "unblock");
                        break;
                }
                canChangeCode = pService.checkPermission(module, "changeCardSecurityCode");
            }

            // can only change card code if has one
            if (card.getCardType().getCardSecurityCode() == CardType.CardSecurityCode.NOT_USED) {
                canChangeCode = false;
            }
            // if CardType.CardSecurityCode == CardType.CardSecurityCode.AUTOMATIC then must show cardSecurityCode on screen
            boolean isAutomaticCardCode = false;
            if (card.getCardType().getCardSecurityCode() == CardType.CardSecurityCode.AUTOMATIC) {
                isAutomaticCardCode = true;
                canChangeCode = false;
            }
            if (card.getStatus() == Card.Status.PENDING) {
                canChangeCode = false;
            }
            if (card.getStatus() == Card.Status.CANCELED || card.getStatus() == Card.Status.EXPIRED) {
                canCancel = false;
                canChangeCode = false;
            }

            // CARD SECURITY CODE BLOCKED
            final boolean isCardBlocked = getAccessService().isCardSecurityCodeBlocked(card.getCardNumber());

            if (isCardBlocked) {
                showUnblockSecurityCodeButton = canUnblockSecurityCode;
                canChangeCode = false;
            }

            final ByElementExtractor extractor = new ByElementExtractor() {
                public Element getByElement(final Entity entity) {
                    return getFetchService().fetch(((CardLog) entity).getBy());
                }
            };

            // Saves allowed operations in the request
            request.setAttribute("card", card);
            request.setAttribute("cardId", card.getId());
            request.setAttribute("canBlock", canBlock);
            request.setAttribute("canUnblock", canUnblock);
            request.setAttribute("canActivate", canActivate);
            request.setAttribute("canCancel", canCancel);
            request.setAttribute("canChangeCode", canChangeCode);
            request.setAttribute("isAutomaticCardCode", isAutomaticCardCode);
            request.setAttribute("isManualCardCode", card.getCardType().getCardSecurityCode() == CardType.CardSecurityCode.MANUAL);
            request.setAttribute("isAdmin", context.isAdmin());
            request.setAttribute("showUnblockSecurityCodeButton", showUnblockSecurityCodeButton);
            request.setAttribute("listOnly", listOnly);
            request.setAttribute("memberId", card.getOwner().getId());
            request.setAttribute("hasActiveCard", cardService.getActiveCard(card.getOwner()) == null ? false : true);
            request.setAttribute("isCardBlocked", isCardBlocked);
            request.setAttribute("showCardSecurityCode", card.getCardType().isShowCardSecurityCode());
            request.setAttribute("logsBy", ActionHelper.getByElements(context, card.getLogs(), extractor));

            // Check if the logged user uses a transaction password
            final boolean usesTransactionPassword = context.isTransactionPasswordEnabled();
            boolean transactionPasswordBlocked = false;
            boolean transactionPasswordPending = false;
            if (usesTransactionPassword) {
                final TransactionPasswordStatus transactionPasswordStatus = getFetchService().reload(context.getUser()).getTransactionPasswordStatus();
                if (transactionPasswordStatus == TransactionPasswordStatus.BLOCKED) {
                    transactionPasswordBlocked = true;
                } else if (transactionPasswordStatus.isGenerationAllowed()) {
                    transactionPasswordPending = true;
                }
            }
            request.setAttribute("usesTransactionPassword", usesTransactionPassword);
            request.setAttribute("transactionPasswordBlocked", transactionPasswordBlocked);
            request.setAttribute("transactionPasswordPending", transactionPasswordPending);

            final RangeConstraint range = card.getCardType().getCardSecurityCodeLength();
            request.setAttribute("minPasswordLength", range.getMin());
            request.setAttribute("maxPasswordLength", range.getMax());

        } catch (final Exception e) {
            throw new ValidationException();
        }
    }
}

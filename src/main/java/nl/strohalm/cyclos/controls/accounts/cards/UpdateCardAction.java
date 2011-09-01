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

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.annotations.RequestParameter;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.entities.access.Channel.Credentials;
import nl.strohalm.cyclos.entities.accounts.cards.Card;
import nl.strohalm.cyclos.entities.accounts.cards.Card.Relationships;
import nl.strohalm.cyclos.entities.accounts.cards.CardType.CardSecurityCode;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.access.exceptions.BlockedCredentialsException;
import nl.strohalm.cyclos.services.access.exceptions.InvalidCredentialsException;
import nl.strohalm.cyclos.services.accounts.cards.CardService;
import nl.strohalm.cyclos.services.permissions.PermissionService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.RequiredError;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;

/**
 * 
 * @author rodrigo
 */
@AdminAction
@MemberAction
@BrokerAction
@RelatedEntity(Card.class)
@RequestParameter("cardId")
@PathToMember("owner")
public class UpdateCardAction extends BaseFormAction {

    private CardService cardService;

    public CardService getCardService() {
        return cardService;
    }

    @Inject
    public void setCardService(final CardService cardService) {
        this.cardService = cardService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final CardForm form = context.getForm();
        final String operation = context.getRequest().getParameter("operation");
        String permission = operation;

        final PermissionService permissionService = getPermissionService();
        final String module = getModuleName(context.getGroup(), form.getMemberId() == 0);

        if (operation.equals("changeCardCode")) {
            permission = "changeCardSecurityCode";
        } else if (operation.equals("activate")) {
            permission = "unblock";
        }

        if (!permissionService.checkPermission(module, permission)) {
            throw new PermissionDeniedException();
        }
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final String remoteAddress = request.getRemoteAddr();
        final CardForm form = context.getForm();
        final String code = form.getSecurityCode();
        final String password = form.getPassword();
        final long cardId = form.getCardId();
        final Card card = cardService.load(cardId);
        try {
            final boolean usesTransactionPassword = context.isTransactionPasswordEnabled();
            if (usesTransactionPassword) {
                final Element element = context.getElement();
                getAccessService().checkTransactionPassword(null, element.getUsername(), password, remoteAddress);
            }
            final String operation = request.getParameter("operation");
            if (operation.equals("block")) {
                cardService.blockCard(card);
                context.sendMessage("card.blocked");
            } else if (operation.equals("unblock")) {
                cardService.unblockCard(card);
                context.sendMessage("card.unblocked");
            } else if (operation.equals("activate")) {
                cardService.activateCard(card, code);
                context.sendMessage("card.activated");
            } else if (operation.equals("cancel")) {
                cardService.cancelCard(card);
                context.sendMessage("card.canceled");
            } else if (operation.equals("changeCardCode")) {
                cardService.changeCardCode(card, code);
                context.sendMessage("card.cardCodeChanged");
            } else if (operation.equals("unblockSecurityCode")) {
                cardService.unblockSecurityCode(card);
                context.sendMessage("card.securityCodeUnblocked");
            } else {
                throw new ValidationException();
            }
            return ActionHelper.redirectWithParam(context.getRequest(), context.getSuccessForward(), "cardId", cardId);
        } catch (final InvalidCredentialsException e) {
            return context.sendError("card.updateCard.error.invalidTransactionPassword");
        } catch (final BlockedCredentialsException e) {
            if (e.getCredentialsType() == Credentials.TRANSACTION_PASSWORD) {
                context.getSession().setAttribute("returnTo", context.getPathPrefix() + "/manageExternalAcccess");
                return context.sendError("card.updateCard.error.blockedTransactionPassword");
            } else {
                request.getSession().invalidate();
                return context.sendError("card.updateCard.error.userBlocked");
            }
        }

    }

    @Override
    protected void validateForm(final ActionContext context) {
        final CardForm form = context.getForm();
        final boolean usesTransactionPassword = context.isTransactionPasswordEnabled();

        final ValidationException e = new ValidationException();
        e.setPropertyKey("securityCode", "card.changeCardCode.newCode1");
        e.setPropertyKey("securityCodeConfirmation", "card.changeCardCode.newCode2");
        e.setPropertyKey("login.transactionPassword", "login.transactionPassword");

        if (usesTransactionPassword) {
            context.validateTransactionPassword();
            if (StringUtils.isEmpty(form.getPassword())) {
                e.addPropertyError("login.transactionPassword", new RequiredError());
            }
        }

        final HttpServletRequest request = context.getRequest();
        final String operation = request.getParameter("operation");
        final Card card = cardService.load(form.getCardId(), Relationships.CARD_TYPE);
        final boolean cardWithManualCodeActivation = card.getCardType().getCardSecurityCode() == CardSecurityCode.MANUAL && operation.equals("activate");

        if (operation.equals("changeCardCode") || cardWithManualCodeActivation) {
            final String securityCode = form.getSecurityCode();
            if (StringUtils.isEmpty(securityCode)) {
                e.addPropertyError("securityCode", new RequiredError());
            }
            if (!card.getCardType().isShowCardSecurityCode()) {
                final String securityCodeConfirmation = form.getSecurityCodeConfirmation();
                if (StringUtils.isEmpty(securityCodeConfirmation)) {
                    e.addPropertyError("securityCodeConfirmation", new RequiredError());
                }
                if (!ObjectUtils.equals(securityCode, securityCodeConfirmation)) {
                    e.addGeneralError(new ValidationError("card.updateCard.cardCodesAreNotEqual"));
                }
            }
        }
        e.throwIfHasErrors();
    }

    private String getModuleName(final Group group, final boolean sameUser) {
        if (Group.Nature.ADMIN == group.getNature()) {
            return "adminMemberCards";
        } else if (Group.Nature.BROKER == group.getNature() && !sameUser) {
            return "brokerCards";
        } else { // This is a member.
            return "memberCards";
        }
    }
}

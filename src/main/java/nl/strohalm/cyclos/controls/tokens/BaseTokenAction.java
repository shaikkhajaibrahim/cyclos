/*
 *
 *    This file is part of Cyclos.
 *
 *    Cyclos is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    Cyclos is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Cyclos; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *
 */

package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.dao.tokens.exceptions.TokenNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.ApplicationException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.tokens.TokenService;
import nl.strohalm.cyclos.services.tokens.exceptions.BadStatusForRedeem;
import nl.strohalm.cyclos.services.tokens.exceptions.InvalidPinException;
import nl.strohalm.cyclos.services.tokens.exceptions.NoTransactionTypeException;
import nl.strohalm.cyclos.services.tokens.exceptions.RefundNonExpiredToken;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.utils.ActionHelper;
import org.apache.struts.action.ActionForward;

public abstract class BaseTokenAction extends BaseFormAction {

    TokenService tokenService;

    @Inject
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    ElementService elementService;

    @Inject
    public void setElementService(ElementService elementService) {
        this.elementService = elementService;
    }

    Member loadLoggedMember(ActionContext actionContext) {
        return elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), actionContext.getUser().getUsername());
    }

    @Override
    protected final ActionForward handleSubmit(ActionContext context) throws Exception {
        String errorKey;
        BaseTokenForm baseTokenForm = context.getForm();
        try {
            Member member = loadLoggedMember(context);
            return tokenSubmit(baseTokenForm, member, context);
        } catch (TokenNotFoundException e) {
            errorKey = "tokens.error.tokenNotFound";
        } catch (BadStatusForRedeem e) {
            errorKey = "tokens.error.badStatusForRedeem";
        } catch (NoTransactionTypeException e) {
            errorKey = "tokens.error.noTransactionType";
        } catch (RefundNonExpiredToken e) {
            errorKey = "tokens.error.refundNotExpired";
        } catch (NotEnoughCreditsException e) {
            errorKey = "tokens.error.notEnoughCredits";
        } catch (InvalidPinException e) {
            errorKey = "tokens.error.invalidPin";
        }

        return context.sendError(errorKey, baseTokenForm.getTokenId());
    }

    abstract ActionForward tokenSubmit(BaseTokenForm token, Member loggedMember, ActionContext actionContext);

}


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
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.tokens.TokenService;
import nl.strohalm.cyclos.utils.ActionHelper;
import org.apache.struts.action.ActionForward;

public class RedeemTokenAction extends BaseTokenAction {

    @Override
    ActionForward tokenSubmit(BaseTokenForm token, Member loggedMember, ActionContext context) {
        String pin = (String) token.getToken("pin");


        tokenService.redeemToken(loggedMember, token.getTokenId(), pin , settingService.getLocalSettings().getRefundTokenTransferType());
        context.sendMessage("tokens.tokenRedeemed", token.getTokenId());
        return context.getSuccessForward();
    }

}

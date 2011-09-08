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
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.tokens.TokenService;

public class RedeemTokenPinAction extends BaseFormAction {

    private TokenService tokenService;

    @Inject
    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    private ElementService elementService;

    @Inject
    public void setElementService(ElementService elementService) {
        this.elementService = elementService;
    }


    @Override
    protected void formAction(ActionContext context) throws Exception {

        Member member = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), context.getUser().getUsername());
        BaseTokenForm baseTokenForm = context.getForm();
        String tokenId = (String) baseTokenForm.getToken("tokenId");
        String pin = (String) baseTokenForm.getToken("pin");
        tokenService.redeemToken(member, tokenId, pin );
    }


}

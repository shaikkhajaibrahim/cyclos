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

package nl.strohalm.cyclos.webservices.tokens;

import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.tokens.TokenService;

import javax.jws.WebService;

@WebService(name = "tokens", serviceName = "tokens")
public class TokenWebServiceImpl implements TokenWebService {

    private TokenService tokenService;

    private ElementService elementService;

    @Override
    public String generateToken(GenerateTokenParameters generateTokenParameters) {

        GenerateTokenDTO generateTokenDTO = new GenerateTokenDTO();
        generateTokenDTO.setAmount(generateTokenParameters.getAmount());
        generateTokenDTO.setFrom(generateTokenParameters.getUsername());
        generateTokenDTO.setTokenSender(generateTokenParameters.getSenderMobile());
        return tokenService.generateToken(generateTokenDTO);

    }

    @Override
    public void redeemToken(RedeemTokenParameters redeemTokenParameters) {
        Member member = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), redeemTokenParameters.getUsername());
        tokenService.redeemToken(member, redeemTokenParameters.getTokenId(),redeemTokenParameters.getTokenPin());
    }

    @Override
    public void senderRedeemToken(RedeemTokenParameters redeemTokenParameters) {
        //FIXME:
        //authorization...
        Member member = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), redeemTokenParameters.getUsername());

        tokenService.senderRedeemToken(member, redeemTokenParameters.getTokenId());

    }

    @Override
    public void generatePin(GeneratePinParameters generatePinParameters) {
        //FIXME credentials??
        tokenService.generatePin(generatePinParameters.getTokenId());
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public void setElementService(ElementService elementService) {
        this.elementService = elementService;
    }
}

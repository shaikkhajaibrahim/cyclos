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

import nl.strohalm.cyclos.dao.tokens.exceptions.TokenNotFoundException;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.tokens.GenerateTokenDTO;
import nl.strohalm.cyclos.services.tokens.SenderRedeemTokenData;
import nl.strohalm.cyclos.services.tokens.TokenService;
import nl.strohalm.cyclos.services.tokens.exceptions.BadStatusForRedeem;
import nl.strohalm.cyclos.services.tokens.exceptions.NoTransactionTypeException;
import nl.strohalm.cyclos.services.transactions.exceptions.NotEnoughCreditsException;
import nl.strohalm.cyclos.webservices.WebServiceFaultsEnum;

import javax.jws.WebService;

@WebService(name = "tokens", serviceName = "tokens")
public class TokenWebServiceImpl implements TokenWebService {

    private TokenService tokenService;

    private ElementService elementService;

    @Override
    public String generateToken(GenerateTokenParameters generateTokenParameters) {

        try {
            GenerateTokenDTO generateTokenDTO = new GenerateTokenDTO();
            generateTokenDTO.setAmount(generateTokenParameters.getAmount());
            generateTokenDTO.setFrom(generateTokenParameters.getUsername());
            generateTokenDTO.setSenderMobilePhone(generateTokenParameters.getSenderMobile());
            return tokenService.generateToken(generateTokenDTO);
        } catch (NotEnoughCreditsException e) {
            throw WebServiceFaultsEnum.NOT_ENOUGH_CREDITS.getFault("Not enough credits for token generation");
        } catch (NoTransactionTypeException e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Cannot perform operation");
        }

    }

    @Override
    public void redeemToken(RedeemTokenParameters redeemTokenParameters) {
        try {
            Member member = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), redeemTokenParameters.getUsername());
            tokenService.redeemToken(member, redeemTokenParameters.getTokenId(), redeemTokenParameters.getPin());
        } catch (BadStatusForRedeem e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Token cannot be redeemed");
        } catch (TokenNotFoundException e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Token not found");
        } catch (NoTransactionTypeException e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Cannot perform operation");
        }
    }

    @Override
    public void senderRedeemToken(SenderRedeemTokenParameters redeemTokenParameters) {
        //FIXME:
        //authorization...
        try {
            Member member = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), redeemTokenParameters.getUsername());
            SenderRedeemTokenData senderRedeemTokenData = new SenderRedeemTokenData();
            senderRedeemTokenData.setPin(redeemTokenParameters.getPin());
            senderRedeemTokenData.setTransactionId(redeemTokenParameters.getReferenceNumber());
            tokenService.senderRedeemToken(member, senderRedeemTokenData);
        } catch (BadStatusForRedeem e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Token cannot be redeemed");
        } catch (TokenNotFoundException e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Token not found");
        } catch (NoTransactionTypeException e) {
            throw WebServiceFaultsEnum.INVALID_PARAMETERS.getFault("Cannot perform operation");
        }
    }

    public void setTokenService(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public void setElementService(ElementService elementService) {
        this.elementService = elementService;
    }
}

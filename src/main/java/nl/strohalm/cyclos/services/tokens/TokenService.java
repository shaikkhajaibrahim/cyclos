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
package nl.strohalm.cyclos.services.tokens;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.SystemAction;

import java.util.Calendar;
import java.util.List;

public interface TokenService extends Service {

    String generateToken(GenerateTokenDTO generateTokenDTO);

    //@BrokerAction
    void redeemToken(Member broker, String tokenId, String pin, Long transferTypeId);

    void senderRedeemToken(Member member, SenderRedeemTokenData senderRedeemTokenData);

   // @SystemAction
    void processExpiredTokens(Calendar time);

    List<Token> getUserTokens(User user);

    Token loadTokenById(String tokenId, Relationship... relationship);

    Token loadTokenByTransactionId(String tokenId);

   // @BrokerAction
    void refundToken(Member member, SenderRedeemTokenData senderRedeemTokenData);
}

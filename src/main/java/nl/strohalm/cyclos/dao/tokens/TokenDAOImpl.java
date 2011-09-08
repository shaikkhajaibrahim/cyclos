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

package nl.strohalm.cyclos.dao.tokens;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.tokens.TokenDAO;
import nl.strohalm.cyclos.entities.tokens.Status;
import nl.strohalm.cyclos.entities.tokens.Token;

import java.util.*;

public class TokenDAOImpl extends BaseDAOImpl<Token> implements TokenDAO {

    public TokenDAOImpl() {
        super(Token.class);
    }

    @Override
    public Token loadByTokenId(String tokenId) {
        return uniqueResult("from Token t where t.tokenId = :tokenId", Collections.singletonMap("tokenId", tokenId));
    }

    @Override
    public List<Token> getTokensToExpire(Calendar time) {
                final StringBuilder hql = new StringBuilder();
        hql.append(" from Token t ");
        hql.append("   where t.transferFrom.date <= :date ");
        hql.append("   and t.status = :status ");

        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("date", time);
        namedParameters.put("status", Status.ISSUED);

        return list(hql.toString(), namedParameters);
    }

    @Override
    public List<Token> getUserTokens(String userName) {
        final StringBuilder hql = new StringBuilder();
        hql.append(" from Token t where t.transferFrom.from.ownerName <= :username ");

        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        namedParameters.put("username", userName);

        return list(hql.toString(), namedParameters);
    }
}

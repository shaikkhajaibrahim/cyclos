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

package nl.strohalm.cyclos.services.tokens;

import nl.strohalm.cyclos.dao.tokens.TokenDAO;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.tokens.Status;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.tokens.exceptions.BadStatusForRedeem;
import nl.strohalm.cyclos.services.tokens.exceptions.InvalidPinException;
import nl.strohalm.cyclos.services.tokens.exceptions.RefundNonExpiredToken;
import nl.strohalm.cyclos.services.tokens.exceptions.WrongTokenStateForPinReset;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transactions.TransferDTO;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import java.util.Calendar;
import java.util.List;

public class TokenServiceImpl implements TokenService {

    private TokenDAO tokenDao;

    private PaymentService paymentService;
    private ElementService elementService;
    private TransferTypeService transferTypeService;
    private SettingsService settingsService;

    @Override
    public String generateToken(GenerateTokenDTO generateTokenDTO) {
        String tokenId = generateTokenID();
        boolean ifSendNotification = true;
        if (generateTokenDTO.getSenderMobilePhone() == null) {
            //FIXME what if username != mobile
            //sender is not set for user transaction and set if initiated by agent
            ifSendNotification = false; //notification send only to agent
            generateTokenDTO.setSenderMobilePhone(generateTokenDTO.getFrom());
        }
        Transfer transfer = transferToSuspenseAccount(generateTokenDTO, generateTokenDTO.getRecipientMobilePhone());
        Token token = createToken(generateTokenDTO, transfer, tokenId, ifSendNotification);
        generatePin(tokenId);
        return token.getTransferFrom().getTransactionNumber();
    }


    private Token createToken(GenerateTokenDTO generateTokenDTO, Transfer transfer, String tokenId, boolean ifSendNotification) {
        Token token = new Token();
        token.setTokenId(tokenId);
        token.setTransferFrom(transfer);
        token.setAmount(generateTokenDTO.getAmount());
        token.setStatus(Status.ISSUED);
        token.setSenderMobilePhone(generateTokenDTO.getSenderMobilePhone());
        token.setRecipientMobilePhone(generateTokenDTO.getRecipientMobilePhone());
        token.setIfSendNotification(ifSendNotification);
        return tokenDao.insert(token);
    }

    private Transfer transferToSuspenseAccount(GenerateTokenDTO generateTokenDTO, String recipient) {
        TransferDTO doPaymentDTO = new TransferDTO();

        doPaymentDTO.setAmount(generateTokenDTO.getAmount());
        doPaymentDTO.setChannel("");
        Member from = loadUser(generateTokenDTO.getFrom());
        doPaymentDTO.setFromOwner(from);

        Long tokenTypeId = generateTokenDTO.getTransferTypeId();
        TransferType tt = transferTypeService.load(tokenTypeId);
        doPaymentDTO.setTransferType(tt);

        doPaymentDTO.setToOwner(SystemAccountOwner.instance());
        doPaymentDTO.setContext(TransactionContext.AUTOMATIC);
        doPaymentDTO.setDescription("Creation of token for recipient "+recipient);
        doPaymentDTO.setChannel(Channel.POSWEB);

        return (Transfer) paymentService.insertWithoutNotification(doPaymentDTO);
    }

    private Member loadUser(String userName) {
        return elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), userName);
    }


    @Override
    public void redeemToken(Member broker, String tokenId, String pin, Long transferTypeId) {
        Token token = tokenDao.loadByTokenId(tokenId);
        validatePin(token, pin);
        Transfer transfer = redeemToken(token, transferTypeId, broker, Status.ISSUED, Status.REMITTED);
        token.setTransferTo(transfer);
        token.setStatus(Status.REMITTED);
        tokenDao.update(token);
    }

    private void validatePin(Token token, String pin) {
        if (!token.getPin().equals(pin)) {
            throw new InvalidPinException();
        }
    }

    @Override
    public void senderRedeemToken(Member member, SenderRedeemTokenData senderRedeemTokenData) {
        Token token = loadTokenByTransactionId(senderRedeemTokenData.getTransactionId());
        validatePin(token, senderRedeemTokenData.getPin());
        Long tokenTypeId = senderRedeemTokenData.getTransferTypeId();
        token.setTransferTo(redeemToken(token, tokenTypeId, member, Status.ISSUED, Status.SENDER_REMITTED));
        token.setStatus(Status.SENDER_REMITTED);
        tokenDao.update(token);
    }

    @Override
    public void resetPinToken(ResetPinTokenData resetPinTokenData) {
        Token token = loadTokenByTransactionId(resetPinTokenData.getTransactionId());
        if(token.getStatus() != Status.ISSUED){
           throw new WrongTokenStateForPinReset();
        }
        token.setPin(randomNumber(4));
        tokenDao.update(token);
    }

    private Transfer redeemToken(Token token, Long transferTypeId, AccountOwner to, Status neededOldStatus, Status newStatus) {
        if (token.getStatus() != neededOldStatus) {
            throw new BadStatusForRedeem();
        }
        token.setStatus(newStatus);
        tokenDao.update(token);

        TransferDTO doPaymentDTO = new TransferDTO();
        doPaymentDTO.setAmount(token.getAmount());
        doPaymentDTO.setFromOwner(SystemAccountOwner.instance());

        doPaymentDTO.setTransferType(
                transferTypeService.load(transferTypeId));

        doPaymentDTO.setToOwner(to);
        doPaymentDTO.setDescription("Redemption of token " + token.getTokenId());
        doPaymentDTO.setContext(TransactionContext.AUTOMATIC);
        return (Transfer) paymentService.insertWithoutNotification(doPaymentDTO);
    }

    private void generatePin(String tokenId) {
        Token token = tokenDao.loadByTokenId(tokenId);
        if (token.getStatus() != Status.ISSUED) {
            throw new BadStatusForRedeem();
        }
        if (token.getPin() == null) {
            String pin = createPin();
            token.setPin(pin);
            tokenDao.update(token);
        }
    }

    @Override
    public Token loadTokenById(String tokenId, Relationship... rel ) {
        return tokenDao.loadByTokenId(tokenId, rel);
    }

    @Override
    public void refundToken(Member member, SenderRedeemTokenData senderRedeemTokenData) {
        Token token = loadTokenByTransactionId(senderRedeemTokenData.getTransactionId());
        validatePin(token, senderRedeemTokenData.getPin());
        if (token.getStatus() != Status.EXPIRED) {
            throw new RefundNonExpiredToken();
        }
        redeemToken(token, senderRedeemTokenData.getTransferTypeId(), member, Status.EXPIRED, Status.REFUNDED);
        token.setStatus(Status.REFUNDED);
        tokenDao.update(token);
    }

    @Override
    public void processExpiredTokens(Calendar time) {
        Calendar timeToExpire = Calendar.getInstance();
        int tokenExpirationInDays = -1*settingsService.getLocalSettings().getTokenExpirationInDays();
        timeToExpire.setTime(DateUtils.addDays(time.getTime(), tokenExpirationInDays == 0 ? -30 : tokenExpirationInDays));
        for (Token token : tokenDao.getTokensToExpire(timeToExpire)) {
            token.setStatus(Status.EXPIRED);
            Long ttId = settingsService.getLocalSettings().getExpireTokenTransferType();
            token.setTransferTo(redeemToken(token, ttId, SystemAccountOwner.instance(), Status.ISSUED, Status.EXPIRED));
            tokenDao.update(token);
        }

    }

    String generateTokenID() {
        return randomNumber(12);
    }


    public String padTransactionId(String transactionId){
        return "0000000".substring(transactionId.length()) + transactionId;
    }

    @Override
    public Token loadTokenByTransactionId(String tokenId) {
        String paddedTransactionId = padTransactionId(tokenId);
        return tokenDao.loadByTransactionId(paddedTransactionId);
    }

    private String randomNumber(int length) {
        return StringUtils.rightPad("" +
                (long) (Math.random() * Math.pow(10,length+1)), length+1, "0").substring(1, length+1);
    }

    private String createPin() {
        return randomNumber(4);
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setElementService(ElementService elementService) {
        this.elementService = elementService;
    }

    public void setTokenDao(TokenDAO tokenDao) {
        this.tokenDao = tokenDao;
    }

    public void setTransferTypeService(TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    public List<Token> getUserTokens(User user) {
        return tokenDao.getUserTokens(user.getUsername());
    }
}

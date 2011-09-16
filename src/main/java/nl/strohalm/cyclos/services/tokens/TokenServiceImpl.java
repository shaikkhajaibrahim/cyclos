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
import nl.strohalm.cyclos.entities.access.Channel;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.AccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.tokens.Status;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.services.tokens.exceptions.BadStatusForRedeem;
import nl.strohalm.cyclos.services.tokens.exceptions.InvalidPinException;
import nl.strohalm.cyclos.services.tokens.exceptions.NoTransactionTypeException;
import nl.strohalm.cyclos.services.tokens.exceptions.RefundNonExpiredToken;
import nl.strohalm.cyclos.services.transactions.DoExternalPaymentDTO;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.sms.SmsSender;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import java.util.*;

public class TokenServiceImpl implements TokenService {

    public final static String CREATE_TOKEN_TRANSACTION_TYPE_NAME = "tokenCreation";

    public final static String REDEEM_TOKEN_TRANSACTION_TYPE_NAME = "tokenRedemption";

    public final static String SENDER_REDEEM_TOKEN_TRANSACTION_TYPE_NAME = "senderTokenRedemption";

    public final static String TOKEN_EXPIRATION_TRANSACTION_TYPE_NAME = "tokenExpiration";

    public final static String TOKEN_REFUND_TRANSACTION_TYPE_NAME = "tokenRefund";


    private TokenDAO tokenDao;

    private PaymentService paymentService;
    private ElementService elementService;
    private SmsSender smsSender;
    private TransferTypeService transferTypeService;
    private SettingsService settingsService;

    @Override
    public String generateToken(GenerateTokenDTO generateTokenDTO) {
        String tokenId = generateTokenID();
        if (generateTokenDTO.getSenderMobilePhone() == null) {
            //FIXME what if username != mobile
            generateTokenDTO.setSenderMobilePhone(generateTokenDTO.getFrom());
        }
        Transfer transfer = transferToSuspenseAccount(generateTokenDTO, generateTokenDTO.getRecipientMobilePhone());
        Token token = createToken(generateTokenDTO, transfer, tokenId);
        sendTokenIdBySms(token);
        generatePin(tokenId);
        return token.getTokenId();
    }


    private Token createToken(GenerateTokenDTO generateTokenDTO, Transfer transfer, String tokenId) {
        Token token = new Token();
        token.setTokenId(tokenId);
        token.setTransferFrom(transfer);
        token.setAmount(generateTokenDTO.getAmount());
        token.setStatus(Status.ISSUED);
        token.setSenderMobilePhone(generateTokenDTO.getSenderMobilePhone());
        token.setRecipientMobilePhone(generateTokenDTO.getRecipientMobilePhone());
        return tokenDao.insert(token);
    }

    private Transfer transferToSuspenseAccount(GenerateTokenDTO generateTokenDTO, String recipient) {
        DoExternalPaymentDTO doPaymentDTO = new DoExternalPaymentDTO();

        doPaymentDTO.setAmount(generateTokenDTO.getAmount());
        doPaymentDTO.setChannel("");
        Member from = loadUser(generateTokenDTO.getFrom());
        doPaymentDTO.setFrom(from);

        TransferTypeQuery ttq = new TransferTypeQuery();
        ttq.setName(CREATE_TOKEN_TRANSACTION_TYPE_NAME);
        List<TransferType> tts = transferTypeService.search(ttq);
        if (tts.isEmpty()) {
            throw new NoTransactionTypeException("No transaction type " + CREATE_TOKEN_TRANSACTION_TYPE_NAME);
        }
        doPaymentDTO.setTransferType(tts.get(0));

        doPaymentDTO.setTo(SystemAccountOwner.instance());
        doPaymentDTO.setContext(TransactionContext.AUTOMATIC);
        doPaymentDTO.setDescription("Creation of token for recipient "+recipient);
        //FIXME: some better channel? cannot be web, since we don't want to be accessible through normal payment page
        doPaymentDTO.setChannel(Channel.POSWEB);

        return (Transfer) paymentService.insertExternalPayment(doPaymentDTO);
    }

    private Member loadUser(String userName) {
        return elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), userName);
    }


    @Override
    public void redeemToken(Member broker, String tokenId, String pin) {
        Token token = tokenDao.loadByTokenId(tokenId);
        validatePin(token, pin);
        Transfer transfer = redeemToken(token, REDEEM_TOKEN_TRANSACTION_TYPE_NAME, broker, Status.ISSUED, Status.REMITTED);
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
        Token token = tokenDao.loadByTransactionId(senderRedeemTokenData.getTransactionId());
        validatePin(token, senderRedeemTokenData.getPin());
        token.setTransferTo(redeemToken(token, SENDER_REDEEM_TOKEN_TRANSACTION_TYPE_NAME, member, Status.ISSUED, Status.SENDER_REMITTED));
        token.setStatus(Status.SENDER_REMITTED);
        tokenDao.update(token);
    }

    private Transfer redeemToken(Token token, String transactionType, AccountOwner to, Status neededOldStatus, Status newStatus) {
        if (token.getStatus() != neededOldStatus) {
            throw new BadStatusForRedeem();
        }
        token.setStatus(newStatus);
        tokenDao.update(token);

        DoExternalPaymentDTO doPaymentDTO = new DoExternalPaymentDTO();
        doPaymentDTO.setAmount(token.getAmount());
        doPaymentDTO.setFrom(SystemAccountOwner.instance());

        TransferTypeQuery ttq = new TransferTypeQuery();
        ttq.setName(transactionType);
        List<TransferType> tts = transferTypeService.search(ttq);
        if (tts.isEmpty()) {
            throw new NoTransactionTypeException("No transaction type " + transactionType);
        }
        doPaymentDTO.setTransferType(tts.get(0));

        doPaymentDTO.setTo(to);
        //FIXME
        doPaymentDTO.setDescription("Redemption of token " + token.getTokenId());
        doPaymentDTO.setContext(TransactionContext.AUTOMATIC);
        return (Transfer) paymentService.insertExternalPayment(doPaymentDTO);
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
        sendPinBySms(token);

    }

    @Override
    public Token loadTokenById(String tokenId) {
        return tokenDao.loadByTokenId(tokenId);
    }

    @Override
    public void refundToken(Member member, SenderRedeemTokenData senderRedeemTokenData) {
        Token token = tokenDao.loadByTransactionId(senderRedeemTokenData.getTransactionId());
        validatePin(token, senderRedeemTokenData.getPin());
        if (token.getStatus() != Status.EXPIRED) {
            throw new RefundNonExpiredToken();
        }
        redeemToken(token, TOKEN_REFUND_TRANSACTION_TYPE_NAME, member, Status.EXPIRED, Status.REFUNDED);
        token.setStatus(Status.REFUNDED);
        tokenDao.update(token);
    }

    @Override
    public void processExpiredTokens(Calendar time) {
        Calendar timeToExpire = Calendar.getInstance();
        timeToExpire.setTime(DateUtils.addDays(time.getTime(), -1));
        for (Token token : tokenDao.getTokensToExpire(timeToExpire)) {
            token.setStatus(Status.EXPIRED);
            token.setTransferTo(redeemToken(token, TOKEN_EXPIRATION_TRANSACTION_TYPE_NAME, SystemAccountOwner.instance(), Status.ISSUED, Status.EXPIRED));
            tokenDao.update(token);
        }
        ;
    }

    String generateTokenID() {
        return randomNumber(12);
    }

    @Override
    public Token loadTokenByTransactionId(String tokenId) {
        return tokenDao.loadByTransactionId(tokenId);
    }

    private String randomNumber(int length) {
        return StringUtils.rightPad("" +
                (long) (Math.random() * Math.pow(10,length+1)), length+1, "0").substring(1, length+1);
    }

    private void sendPinBySms(Token token) {
        sendSms(token.getSenderMobilePhone(), token, getMessageSettings().getTokenPinGeneratedSms());
    }

    private void sendTokenIdBySms(Token token) {
        sendSms(token.getRecipientMobilePhone(), token, getMessageSettings().getTokenGeneratedSms());
    }

    private void sendSms(String smsRecipient, Token token, String smsTemplate) {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("tokenId", token.getTokenId());
        params.put("pin", token.getPin());
        params.put("transactionId", token.getTransferFrom().getTransactionNumber());
        final String sms = MessageProcessingHelper.processVariables(smsTemplate, params);
        smsSender.send(smsRecipient, sms);
    }

    private MessageSettings getMessageSettings() {
        return settingsService.getMessageSettings();
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

    public void setSmsSender(SmsSender smsSender) {
        this.smsSender = smsSender;
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

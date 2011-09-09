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
import nl.strohalm.cyclos.entities.tokens.Status;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.transactions.DoExternalPaymentDTO;
import nl.strohalm.cyclos.services.transactions.PaymentService;
import nl.strohalm.cyclos.services.transactions.TransactionContext;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.sms.SmsSender;
import org.apache.commons.lang.time.DateUtils;

import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class TokenServiceImpl implements TokenService {

    public final static String CREATE_TOKEN_TRANSACTION_TYPE_NAME = "tokenCreation";

    public final static String REDEEM_TOKEN_TRANSACTION_TYPE_NAME = "tokenRedemption";

    public final static String SENDER_REDEEM_TOKEN_TRANSACTION_TYPE_NAME = "senderTokenRedemption";

    public final static String TOKEN_EXPIRATION_TRANSACTION_TYPE_NAME = "tokenExpiration";

    private TokenDAO tokenDao;

    private PaymentService paymentService;
    private ElementService elementService;
    private SmsSender smsSender;
    private TransferTypeService transferTypeService;

    @Override
    public String generateToken(GenerateTokenDTO generateTokenDTO) {
        String voucherId = generateTokenID();
        if (generateTokenDTO.getTokenSender() == null) {
            //FIXME what if username != mobile
            generateTokenDTO.setTokenSender(generateTokenDTO.getFrom());
        }
        Transfer transfer = transferToSuspenseAccount(generateTokenDTO, voucherId);
        Token voucher = createToken(generateTokenDTO, transfer, voucherId);
        sendConfirmationSms(voucher);
        return voucher.getTokenId();
    }


    private Token createToken(GenerateTokenDTO generateTokenDTO, Transfer transfer, String voucherId) {
        Token token = new Token();
        token.setTokenId(voucherId);
        token.setTransferFrom(transfer);
        token.setAmount(generateTokenDTO.getAmount());
        token.setStatus(Status.ISSUED);
        token.setSenderMobilePhone(generateTokenDTO.getTokenSender());
        return tokenDao.insert(token);
    }

    private Transfer transferToSuspenseAccount(GenerateTokenDTO generateTokenDTO, String voucherId) {
        DoExternalPaymentDTO doPaymentDTO = new DoExternalPaymentDTO();

        doPaymentDTO.setAmount(generateTokenDTO.getAmount());

        Member from = loadUser(generateTokenDTO.getFrom());
        doPaymentDTO.setFrom(from);

        TransferTypeQuery ttq = new TransferTypeQuery();
        ttq.setName(CREATE_TOKEN_TRANSACTION_TYPE_NAME);
        List<TransferType> tts = transferTypeService.search(ttq);
        if (tts.isEmpty()) {
            throw new RuntimeException("No transaction type " + CREATE_TOKEN_TRANSACTION_TYPE_NAME);
        }
        doPaymentDTO.setTransferType(tts.get(0));

        doPaymentDTO.setTo(SystemAccountOwner.instance());
        doPaymentDTO.setContext(TransactionContext.PAYMENT);
        doPaymentDTO.setDescription("Creation of voucher " + voucherId);

        return (Transfer) paymentService.insertExternalPayment(doPaymentDTO);
    }

    private Member loadUser(String userName) {
        return elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), userName);
    }

    private void sendConfirmationSms(Token voucher) {
        System.out.println("Generated: " + voucher.getTokenId() + " ");
    }


    @Override
    public void redeemToken(Member broker, String tokenId, String pin) {
        Token voucher = tokenDao.loadByTokenId(tokenId);
        validatePin(voucher, pin);
        Transfer transfer = redeemToken(voucher, REDEEM_TOKEN_TRANSACTION_TYPE_NAME, broker);
        voucher.setTransferTo(transfer);
        voucher.setStatus(Status.REMITTED);
        tokenDao.update(voucher);
    }

    private void validatePin(Token token, String pin) {
        if (!token.getPin().equals(pin)) {
            throw new RuntimeException("Invalid PIN");
        }
    }

    @Override
    public void senderRedeemToken(Member member, String tokenId) {
        Token token = tokenDao.loadByTokenId(tokenId);
        token.setStatus(Status.SENDER_REMITTED);
        token.setTransferTo(redeemToken(token, SENDER_REDEEM_TOKEN_TRANSACTION_TYPE_NAME, member));
        tokenDao.update(token);
    }

    private Transfer redeemToken(Token token, String transactionType, AccountOwner to) {
        if (token.getStatus() != Status.ISSUED) {
            throw new RuntimeException("Bad status");
        }
        DoExternalPaymentDTO doPaymentDTO = new DoExternalPaymentDTO();

        doPaymentDTO.setAmount(token.getAmount());

        doPaymentDTO.setFrom(SystemAccountOwner.instance());

        TransferTypeQuery ttq = new TransferTypeQuery();
        ttq.setName(transactionType);
        List<TransferType> tts = transferTypeService.search(ttq);
        if (tts.isEmpty()) {
            throw new RuntimeException("No transaction type " + transactionType);
        }
        doPaymentDTO.setTransferType(tts.get(0));

        doPaymentDTO.setTo(to);
        //FIXME
        doPaymentDTO.setDescription("Redemption of token " + token.getTokenId());
        doPaymentDTO.setContext(TransactionContext.AUTOMATIC);
        return (Transfer) paymentService.insertExternalPayment(doPaymentDTO);
    }

    @Override
    public void generatePin(String tokenId) {
        Token voucher = tokenDao.loadByTokenId(tokenId);
        if (voucher.getStatus() != Status.ISSUED) {
            throw new RuntimeException("Bad status");
        }
        if (voucher.getPin() == null) {
            String pin = createPin();
            voucher.setPin(pin);
            tokenDao.update(voucher);
        }
        sendPinBySms(voucher);

    }

    @Override
    public Token loadTokenById(String tokenId) {
        return tokenDao.loadByTokenId(tokenId);
    }

    @Override
    public void processExpiredTokens(Calendar time) {
        Calendar timeToExpire = Calendar.getInstance();
        timeToExpire.setTime(DateUtils.addDays(time.getTime(), -1));
        for (Token token : tokenDao.getTokensToExpire(timeToExpire)) {
            token.setStatus(Status.EXPIRED);
            token.setTransferTo(redeemToken(token, TOKEN_EXPIRATION_TRANSACTION_TYPE_NAME, SystemAccountOwner.instance()));
            tokenDao.update(token);
        }
        ;
    }

    String generateTokenID() {
        return UUID.randomUUID().toString().replaceAll("-", "").substring(0, 10);
    }

    private void sendPinBySms(Token voucher) {
        //FIXME: also to nonmember!!
        Member user = loadUser(voucher.getTransferFrom().getActualFrom().getOwnerName());
        smsSender.send(user, "PIN " + voucher.getPin() + " was generated for token: " + voucher.getTokenId());
    }

    private String createPin() {
        String pin = ("" + (int) (Math.random() * 10000)).substring(0, 4);
        return pin;
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

    @Override
    public List<Token> getUserTokens(User user) {

        return tokenDao.getUserTokens(user.getUsername());
    }
}

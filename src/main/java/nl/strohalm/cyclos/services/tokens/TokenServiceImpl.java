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
import nl.strohalm.cyclos.entities.access.MemberUser;
import nl.strohalm.cyclos.entities.access.PrincipalType;
import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.SystemAccountOwner;
import nl.strohalm.cyclos.entities.accounts.SystemAccountType;
import nl.strohalm.cyclos.entities.accounts.transactions.Payment;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferType;
import nl.strohalm.cyclos.entities.accounts.transactions.TransferTypeQuery;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.groups.GroupFilter;
import nl.strohalm.cyclos.entities.groups.GroupQuery;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.tokens.Status;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.access.AccessService;
import nl.strohalm.cyclos.services.accounts.AccountDTO;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.services.elements.MemberService;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.groups.GroupService;
import nl.strohalm.cyclos.services.transactions.*;
import nl.strohalm.cyclos.services.transfertypes.TransferTypeService;
import nl.strohalm.cyclos.utils.sms.SmsSender;
import nl.strohalm.cyclos.webservices.sms.SendSmsParameters;
import nl.strohalm.cyclos.webservices.sms.SmsWebService;

import java.util.*;

public class TokenServiceImpl implements TokenService {

    public final static String CREATE_VOUCHER_TRANSACTION_TYPE_NAME = "voucherCreation";

    public final static String REDEEM_VOUCHER_TRANSACTION_TYPE_NAME = "voucherRedemption";

    private TokenDAO tokenDao;

    private PaymentService paymentService;
    private ElementService elementService;
   // private SmsWebService smsWebService;
    private TransferTypeService transferTypeService;

    @Override
    public String generateToken(GenerateTokenDTO generateTokenDTO) {
        Transfer transfer = transferToSuspenseAccount(generateTokenDTO);
        Token voucher = createVoucherAccount(generateTokenDTO, transfer);
        sendConfirmationSms(voucher);
        return voucher.getTokenId();
    }


    private Token createVoucherAccount(GenerateTokenDTO generateTokenDTO, Transfer transfer) {
        Token token = new Token();
        token.setTokenId(generateTokenID());
        token.setTransferFrom(transfer);
        token.setAmount(generateTokenDTO.getAmount());
        token.setStatus(Status.ISSUED);
        token.setSenderMobilePhone(generateTokenDTO.getTokenSender());
        return tokenDao.insert(token);
    }

    private Transfer transferToSuspenseAccount(GenerateTokenDTO generateTokenDTO) {
        DoExternalPaymentDTO doPaymentDTO = new DoExternalPaymentDTO();

        doPaymentDTO.setAmount(generateTokenDTO.getAmount());

        Member from = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), generateTokenDTO.getFrom());
        doPaymentDTO.setFrom(from);

        TransferTypeQuery ttq = new TransferTypeQuery();
        ttq.setName(CREATE_VOUCHER_TRANSACTION_TYPE_NAME);
        List<TransferType> tts = transferTypeService.search(ttq);
        if (tts.isEmpty()) {
            throw new RuntimeException("No transaction type "+CREATE_VOUCHER_TRANSACTION_TYPE_NAME);
        }
        doPaymentDTO.setTransferType(tts.get(0));

        doPaymentDTO.setTo(SystemAccountOwner.instance());

        doPaymentDTO.setContext(TransactionContext.PAYMENT);
        return (Transfer) paymentService.insertExternalPayment(doPaymentDTO);
    }

    private void sendConfirmationSms(Token voucher) {
        System.out.println("Generated: "+voucher.getTokenId()+" ");
    }


    @Override
    public void redeemToken(Member broker, String tokenId, String pin) {
        Member voucher = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), tokenId);
        validatePin(tokenId, pin);
        transferFromSuspenseAccount(broker, voucher);

    }

    private void validatePin(String tokenId, String pin) {
        Member voucher = elementService.loadByPrincipal(new PrincipalType(Channel.Principal.USER), tokenId);
        if (!voucher.getMemberUser().getPin().equals(pin)) {
           throw new RuntimeException("Invalid PIN");
        }
    }

    private void transferFromSuspenseAccount(Member broker, Member voucher) {
        //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public void senderRedeemToken(Member member, String tokenId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void generatePin(String tokenId) {
        Token voucher = tokenDao.loadByTokenId(tokenId);
        String pin = createPin();
        voucher.setPin(pin);
        tokenDao.update(voucher);
        sendPinBySms(voucher, pin);
    }

    String generateTokenID() {
        return UUID.randomUUID().toString().replaceAll("-","").substring(0,10);
    }

    private void sendPinBySms(Token voucher, String pin) {
        //FIXME
    }


    private String createPin() {
        String pin = (""+(int)(Math.random()*10000)).substring(0, 4);
        return pin;
    }

    public void setPaymentService(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void setElementService(ElementService elementService) {
        this.elementService = elementService;
    }


 //   public void setSmsWebService(SmsWebService smsWebService) {
 //       this.smsWebService = smsWebService;
 //   }

    public void setTokenDao(TokenDAO tokenDao) {
        this.tokenDao = tokenDao;
    }

    public void setTransferTypeService(TransferTypeService transferTypeService) {
        this.transferTypeService = transferTypeService;
    }
}

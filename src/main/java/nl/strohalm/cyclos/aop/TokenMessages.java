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
package nl.strohalm.cyclos.aop;

import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.entities.settings.MessageSettings;
import nl.strohalm.cyclos.entities.tokens.Token;
import nl.strohalm.cyclos.services.accounts.AccountService;
import nl.strohalm.cyclos.services.accounts.GetTransactionsDTO;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.MessageProcessingHelper;
import nl.strohalm.cyclos.utils.sms.SmsSender;

public class TokenMessages {

    private SettingsService settingsService;

    private SmsSender smsSender;

    private AccountService accountService;

    private MessageHelper messageHelper;

    public TokenMessages(SettingsService settingsService, SmsSender smsSender, MessageService messageService, AccountService accountService) {
        this.settingsService = settingsService;
        this.smsSender = smsSender;
        this.accountService = accountService;
        this.messageHelper = new MessageHelper(settingsService, messageService);
    }

    MessageSettings messageSettings() {
        return settingsService.getMessageSettings();
    }

    void sendRedeemTokenMessages(Member member, Token token) {
//        messageHelper.sendMemberMessage(messageSettings().getTokenRedemptionSubject(), messageSettings().getTokenRedemptionMessage(),
//        messageSettings().getTokenRedemptionSms(), member, Message.Type.TOKEN, token, accountService.getStatus(new GetTransactionsDTO(token.getTransferFrom().getFrom())));
        sendSms(token.getRecipientMobilePhone(), token, messageSettings().getTokenRedeemedRecipientSms(), false);
    }

    void sendGenerateTokenMessages(Token token) {
        sendPinBySms(token);
        sendTokenIdBySms(token);
    }

    void sendResetPinTokenMessages(Token token){
        sendSms(token.getSenderMobilePhone(), token, getMessageSettings().getTokenResetPinGeneratedSms(), true);
    }

    private void sendPinBySms(Token token) {
//1. BlueCash Voucher sent to 234XXXXXXXXXX. Transaction ID: XXXXXXXXXXXX, Amount: NX,XXX.XX Please send Voucher PIN XXXX to Recipient. Your balance is NX,XXX.XX
        //TODO: separate handling for user
        sendSms(token.getSenderMobilePhone(), token, getMessageSettings().getTokenPinGeneratedSms(), true);
    }

    private void sendTokenIdBySms(Token token) {
        sendSms(token.getRecipientMobilePhone(), token, getMessageSettings().getTokenGeneratedSms(), true);
    }

    private void sendSms(String smsRecipient, Token token, String smsTemplate, boolean sendingFailed) {
        if (settingsService.getLocalSettings().isSmsNotificationEnabled()) {
            final String sms = MessageProcessingHelper.processVariables(smsTemplate, token, settingsService.getLocalSettings());
            Member creator = (Member) token.getTransferFrom().getFromOwner();
            MessageSettings messageSettings = settingsService.getMessageSettings();
            if (sendingFailed) {
                smsSender.send(smsRecipient, sms, creator, messageSettings.getTokenSmsFailedSubject(), messageSettings.getTokenSmsFailedMessage(), token);
            } else {
                smsSender.send(smsRecipient, sms);
            }
        }
    }

    private MessageSettings getMessageSettings() {
        return settingsService.getMessageSettings();
    }

}

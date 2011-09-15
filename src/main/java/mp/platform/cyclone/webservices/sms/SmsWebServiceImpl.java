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
package mp.platform.cyclone.webservices.sms;

import javax.jws.WebService;

import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.sms.SmsLog;
import nl.strohalm.cyclos.entities.sms.SmsLog.ErrorType;
import nl.strohalm.cyclos.services.elements.MessageService;
import nl.strohalm.cyclos.services.elements.SendSmsDTO;
import nl.strohalm.cyclos.services.infotexts.InfoTextService;
import mp.platform.cyclone.webservices.WebServiceContext;
import mp.platform.cyclone.webservices.utils.WebServiceHelper;
import mp.platform.cyclone.webservices.utils.server.MemberHelper;

import org.apache.commons.lang.StringUtils;

/**
 * Implementation for {@link SmsWebService}
 * 
 * @author luis
 */
@WebService(name = "sms", serviceName = "sms")
public class SmsWebServiceImpl implements SmsWebService {

    private MessageService   messageService;
    private MemberHelper     memberHelper;
    private WebServiceHelper webServiceHelper;
    private InfoTextService  infoTextService;

    public SendSmsResult sendSms(final SendSmsParameters params) {
        if (params == null || (StringUtils.isEmpty(params.getText())) && !params.isInfoText()) {
            throw new IllegalArgumentException();
        }
        final Member restrictedMember = WebServiceContext.getMember();
        SendSmsStatus status = null;

        // Get the target member
        Member target = null;
        try {
            target = memberHelper.loadByPrincipal(params.getTargetPrincipalType(), params.getTargetPrincipal());
            // Ensure the target is found and not the restricted member, if any
            if (target == null || target.equals(restrictedMember)) {
                throw new Exception();
            }
            // Ensure the target participates on this channel
            if (!memberHelper.isChannelEnabledForMember(target)) {
                status = SendSmsStatus.CHANNEL_DISABLED_FOR_TARGET;
            }
        } catch (final Exception e) {
            status = SendSmsStatus.TARGET_NOT_FOUND;
        }

        // Get the member to be charged
        Member charged = null;
        if (status == null) {
            if (restrictedMember != null) {
                charged = restrictedMember;
            } else {
                try {
                    charged = memberHelper.loadByPrincipal(params.getToChargePrincipalType(), params.getToChargePrincipal());
                    if (charged == null) {
                        charged = target;
                    } else {
                        if (!memberHelper.isChannelEnabledForMember(charged)) {
                            status = SendSmsStatus.CHANNEL_DISABLED_FOR_CHARGED;
                        }
                    }
                } catch (final Exception e) {
                    status = SendSmsStatus.CHARGED_NOT_FOUND;
                }
            }
        }

        String textToSend = params.getText();
        if (status == null && params.isInfoText()) {
            textToSend = infoTextService.getInfoTextSubject(params.getText());
            if (textToSend == null) {
                webServiceHelper.trace("Info text's subject null for alias '" + params.getText() + "'");
                status = SendSmsStatus.INFO_TEXT_NOT_FOUND;
            }
        }
        // Send the message
        final SendSmsResult result = new SendSmsResult();
        if (status == null) {
            final SendSmsDTO send = new SendSmsDTO();
            send.setTargetMember(target);
            send.setChargedMember(charged);
            send.setText(textToSend);
            send.setSmsType(params.getSmsType());
            try {
                final SmsLog log = messageService.sendSms(send);
                if (log == null) {
                    throw new IllegalStateException("No SMS log returned from MessageService.sendSms()");
                }
                final ErrorType errorType = log.getErrorType();
                result.setSmsId(log.getId());

                if (errorType != null) {
                    if (errorType == ErrorType.SEND_ERROR) {
                        status = SendSmsStatus.SEND_ERROR;
                    } else {
                        status = SendSmsStatus.CHARGE_COULD_NOT_BE_DONE;
                    }
                } else {
                    status = SendSmsStatus.SUCCESS;
                }
            } catch (final Exception e) {
                webServiceHelper.error(e);
                status = SendSmsStatus.INTERNAL_ERROR;
            }
        }
        result.setStatus(status);
        return result;
    }

    public void setInfoTextService(final InfoTextService infoTextService) {
        this.infoTextService = infoTextService;
    }

    public void setMemberHelper(final MemberHelper memberHelper) {
        this.memberHelper = memberHelper;
    }

    public void setMessageService(final MessageService messageService) {
        this.messageService = messageService;
    }

    public void setWebServiceHelper(final WebServiceHelper webServiceHelper) {
        this.webServiceHelper = webServiceHelper;
    }

}

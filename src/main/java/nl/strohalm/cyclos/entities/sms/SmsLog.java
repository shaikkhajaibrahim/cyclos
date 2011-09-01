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
package nl.strohalm.cyclos.entities.sms;

import java.util.Calendar;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.members.messages.Message;
import nl.strohalm.cyclos.utils.FormatObject;
import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * A log of a sent SMS message
 * @author Jefferson Magno
 */
public class SmsLog extends Entity {

    /**
     * Possible error on sending an sms
     * 
     * @author luis
     */
    public static enum ErrorType implements StringValuedEnum {

        /** The member who should pay for the sms doesn't have enough credits */
        NOT_ENOUGH_FUNDS("NEF"),

        /** The member who should pay for the sms doesn't have sms or doesn't allow charge for it */
        ALLOW_CHARGING_DISABLED("ACD"),

        /** The additional messages is zero and the member has no free messages left */
        NO_SMS_LEFT("NL"),

        /** The destination member had no mobile phone set (according to localSettings.smsCustomField */
        NO_PHONE("NP"),

        /** An unknown error has ocurred while sending the sms */
        SEND_ERROR("SE");

        private final String value;

        private ErrorType(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public static enum Relationships implements Relationship {
        TARGET_MEMBER("targetMember"), CHARGED_MEMBER("chargedMember"), SMS_MAILING("smsMailing");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long serialVersionUID = -4331781757069220677L;
    private Member            targetMember;
    private Member            chargedMember;
    private Calendar          date;
    private Message.Type      messageType;
    private SmsMailing        smsMailing;
    private boolean           freeBaseUsed;
    private SmsType           smsType;
    private ErrorType         errorType;

    public Member getChargedMember() {
        return chargedMember;
    }

    public Calendar getDate() {
        return date;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public Message.Type getMessageType() {
        return messageType;
    }

    public SmsMailing getSmsMailing() {
        return smsMailing;
    }

    public SmsType getSmsType() {
        return smsType;
    }

    public SmsLogStatus getStatus() {
        if (errorType != null) {
            return SmsLogStatus.ERROR;
        }
        return SmsLogStatus.DELIVERED;
    }

    public Member getTargetMember() {
        return targetMember;
    }

    public SmsLogType getType() {
        if (smsMailing != null) {
            return SmsLogType.MAILING;
        } else if (messageType != null) {
            return SmsLogType.NOTIFICATION;
        } else if (smsType != null) {
            return SmsLogType.SMS_OPERATION;
        }
        return null;
    }

    public boolean isFree() {
        if (smsMailing != null && smsMailing.isFree()) {
            return true;
        }
        return freeBaseUsed;
    }

    public boolean isFreeBaseUsed() {
        return freeBaseUsed;
    }

    public boolean isSuccessful() {
        return errorType == null;
    }

    public void setChargedMember(final Member chargedMember) {
        this.chargedMember = chargedMember;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setErrorType(final ErrorType errorType) {
        this.errorType = errorType;
    }

    public void setFreeBaseUsed(final boolean freeBaseUsed) {
        this.freeBaseUsed = freeBaseUsed;
    }

    public void setMessageType(final Message.Type messageType) {
        this.messageType = messageType;
    }

    public void setSmsMailing(final SmsMailing smsMailing) {
        this.smsMailing = smsMailing;
    }

    public void setSmsType(final SmsType smsType) {
        this.smsType = smsType;
    }

    public void setTargetMember(final Member targetMember) {
        this.targetMember = targetMember;
    }

    @Override
    public String toString() {
        return "SmsLog [id=" + getId() + ", chargedMember=" + chargedMember + ", date=" + FormatObject.formatObject(date) + ", errorType=" + errorType + ", freeBaseUsed=" + freeBaseUsed + ", messageType=" + messageType + ", smsMailing=" + smsMailing + ", smsType=" + smsType + ", targetMember=" + targetMember + "]";
    }
}
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
package nl.strohalm.cyclos.webservices.sms;

import java.io.Serializable;

import nl.strohalm.cyclos.entities.sms.SmsType;

/**
 * Parameters used to send an SMS message
 * @author luis
 */
public class SendSmsParameters implements Serializable {

    private static final long serialVersionUID = -211018539906643986L;

    private SmsType           smsType;
    private String            targetPrincipal;
    private String            targetPrincipalType;
    private String            toChargePrincipal;
    private String            toChargePrincipalType;
    private String            text;
    private boolean           isInfoText;

    public SmsType getSmsType() {
        return smsType;
    }

    public String getTargetPrincipal() {
        return targetPrincipal;
    }

    public String getTargetPrincipalType() {
        return targetPrincipalType;
    }

    public String getText() {
        return text;
    }

    public String getToChargePrincipal() {
        return toChargePrincipal;
    }

    public String getToChargePrincipalType() {
        return toChargePrincipalType;
    }

    public boolean isInfoText() {
        return isInfoText;
    }

    public void setInfoText(final boolean isInfoText) {
        this.isInfoText = isInfoText;
    }

    public void setSmsType(final SmsType smsType) {
        this.smsType = smsType;
    }

    public void setTargetPrincipal(final String targetPrincipal) {
        this.targetPrincipal = targetPrincipal;
    }

    public void setTargetPrincipalType(final String targetPrincipalType) {
        this.targetPrincipalType = targetPrincipalType;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public void setToChargePrincipal(final String toChargePrincipal) {
        this.toChargePrincipal = toChargePrincipal;
    }

    public void setToChargePrincipalType(final String toChargePrincipalType) {
        this.toChargePrincipalType = toChargePrincipalType;
    }

    @Override
    public String toString() {
        return "SendSmsParameters [smsType=" + smsType + ", targetPrincipal=" + targetPrincipal + ", targetPrincipalType=" + targetPrincipalType + ", text=" + text + ", toChargePrincipal=" + toChargePrincipal + ", toChargePrincipalType=" + toChargePrincipalType + "]";
    }

}

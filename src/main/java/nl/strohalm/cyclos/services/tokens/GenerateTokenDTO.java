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

import java.math.BigDecimal;

public class GenerateTokenDTO {

    private BigDecimal amount;
    private String from;
    private String senderMobilePhone;
    private String recipientMobilePhone;
    private Long transferTypeId;

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSenderMobilePhone() {
        return senderMobilePhone;
    }

    public void setSenderMobilePhone(String senderMobilePhone) {
        this.senderMobilePhone = senderMobilePhone;
    }

    public String getRecipientMobilePhone() {
        return recipientMobilePhone;
    }

    public void setRecipientMobilePhone(String recipientMobilePhone) {
        this.recipientMobilePhone = recipientMobilePhone;
    }

    public Long getTransferTypeId() {
        return transferTypeId;
    }

    public void setTransferTypeId(Long transferTypeId) {
        this.transferTypeId = transferTypeId;
    }

}

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
package nl.strohalm.cyclos.webservices.payments;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Base parameters for payment-related operations
 * @author luis
 */
public abstract class AbstractPaymentParameters implements Serializable {

    private static final long serialVersionUID = 8695041183461321816L;
    private String            fromMemberPrincipalType;
    private String            fromMember;
    private String            toMemberPrincipalType;
    private String            toMember;
    private BigDecimal        amount;
    private String            description;
    private String            currency;
    private String            traceNumber;

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public String getFromMember() {
        return fromMember;
    }

    public String getFromMemberPrincipalType() {
        return fromMemberPrincipalType;
    }

    public String getToMember() {
        return toMember;
    }

    public String getToMemberPrincipalType() {
        return toMemberPrincipalType;
    }

    public String getTraceNumber() {
        return traceNumber;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setCurrency(final String currency) {
        this.currency = currency;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setFromMember(final String fromMember) {
        this.fromMember = fromMember;
    }

    public void setFromMemberPrincipalType(final String fromMemberPrincipalType) {
        this.fromMemberPrincipalType = fromMemberPrincipalType;
    }

    public void setToMember(final String toMember) {
        this.toMember = toMember;
    }

    public void setToMemberPrincipalType(final String toMemberPrincipalType) {
        this.toMemberPrincipalType = toMemberPrincipalType;
    }

    public void setTraceNumber(final String traceNumber) {
        this.traceNumber = traceNumber;
    }

    @Override
    public String toString() {
        return "AbstractPaymentParameters [amount=" + amount + ", currency=" + currency + ", description=" + description + ", fromMember=" + fromMember + ", fromMemberPrincipalType=" + fromMemberPrincipalType + ", toMember=" + toMember + ", toMemberPrincipalType=" + toMemberPrincipalType + ", traceNumber=" + traceNumber + "]";
    }

}
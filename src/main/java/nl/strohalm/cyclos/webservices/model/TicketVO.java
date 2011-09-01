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
package nl.strohalm.cyclos.webservices.model;

import java.math.BigDecimal;
import java.util.Calendar;

/**
 * Ticket for web services
 * @author luis
 */
public abstract class TicketVO extends WebServicesEntityVO {

    private static final long serialVersionUID = 7596721124085616450L;
    private String            ticket;
    private MemberVO          toMember;
    private MemberVO          fromMember;
    private BigDecimal        amount;
    private String            formattedAmount;
    private Calendar          creationDate;
    private String            formattedCreationDate;
    private String            description;
    private boolean           isOk;
    private boolean           isCancelled;
    private boolean           isPending;
    private boolean           isExpired;
    private boolean           isAwaitingAuthorization;

    public BigDecimal getAmount() {
        return amount;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getDescription() {
        return description;
    }

    public String getFormattedAmount() {
        return formattedAmount;
    }

    public String getFormattedCreationDate() {
        return formattedCreationDate;
    }

    public MemberVO getFromMember() {
        return fromMember;
    }

    public String getTicket() {
        return ticket;
    }

    public MemberVO getToMember() {
        return toMember;
    }

    public boolean isAwaitingAuthorization() {
        return isAwaitingAuthorization;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public boolean isOk() {
        return isOk;
    }

    public boolean isPending() {
        return isPending;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public void setAwaitingAuthorization(final boolean isAwaitingAuthorization) {
        this.isAwaitingAuthorization = isAwaitingAuthorization;
    }

    public void setCancelled(final boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    public void setCreationDate(final Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setExpired(final boolean isExpired) {
        this.isExpired = isExpired;
    }

    public void setFormattedAmount(final String formattedAmount) {
        this.formattedAmount = formattedAmount;
    }

    public void setFormattedCreationDate(final String formattedCreationDate) {
        this.formattedCreationDate = formattedCreationDate;
    }

    public void setFromMember(final MemberVO from) {
        fromMember = from;
    }

    public void setOk(final boolean isOk) {
        this.isOk = isOk;
    }

    public void setPending(final boolean isPending) {
        this.isPending = isPending;
    }

    public void setTicket(final String ticket) {
        this.ticket = ticket;
    }

    public void setToMember(final MemberVO toMember) {
        this.toMember = toMember;
    }

    @Override
    public String toString() {
        return "TicketVO[ticket=" + ticket + ", toMember=" + toMember + ", fromMember=" + fromMember + ", amount=" + amount + ", formattedAmount=" + formattedAmount + ", creationDate=" + creationDate + ", formattedCreationDate=" + formattedCreationDate + ", description=" + description + ", isOk=" + isOk + ", isCancelled=" + isCancelled + ", isPending=" + isPending + ", isExpired=" + isExpired + ", isAwaitingAuthorization=" + isAwaitingAuthorization + ")";

    }
}
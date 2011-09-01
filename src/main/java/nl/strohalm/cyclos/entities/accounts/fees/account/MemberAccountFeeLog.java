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
package nl.strohalm.cyclos.entities.accounts.fees.account;

import java.util.Calendar;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.transactions.Invoice;
import nl.strohalm.cyclos.entities.accounts.transactions.Transfer;
import nl.strohalm.cyclos.entities.members.Member;

/**
 * Relates a member to an account fee log
 * 
 * @author luis
 */
public class MemberAccountFeeLog extends Entity {

    public static enum Relationships implements Relationship {
        ACCOUNT_FEE_LOG("accountFeeLog"), MEMBER("member"), TRANSFER("transfer"), INVOICE("invoice");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long serialVersionUID = -3632964253062346212L;
    private Calendar          date;
    private Member            member;
    private AccountFeeLog     accountFeeLog;
    private Transfer          transfer;
    private Invoice           invoice;

    public AccountFeeLog getAccountFeeLog() {
        return accountFeeLog;
    }

    public Calendar getDate() {
        return date;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public Member getMember() {
        return member;
    }

    public Transfer getTransfer() {
        return transfer;
    }

    public void setAccountFeeLog(final AccountFeeLog accountFeeLog) {
        this.accountFeeLog = accountFeeLog;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setInvoice(final Invoice invoice) {
        this.invoice = invoice;
    }

    public void setMember(final Member member) {
        this.member = member;
    }

    public void setTransfer(final Transfer transfer) {
        this.transfer = transfer;
    }

    @Override
    public String toString() {
        return getId() + ", log: " + accountFeeLog + ", member: " + member;
    }

}

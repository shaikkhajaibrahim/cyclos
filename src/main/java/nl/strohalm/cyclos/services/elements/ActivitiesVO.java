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
package nl.strohalm.cyclos.services.elements;

import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.strohalm.cyclos.entities.ads.Ad;
import nl.strohalm.cyclos.entities.members.Reference;
import nl.strohalm.cyclos.services.transactions.TransactionSummaryVO;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * Return class for the statistical data regarding the activities of a specified user.
 * @author rafael
 */
public class ActivitiesVO extends DataObject {

    private static final long                serialVersionUID = -347229469387541880L;
    private Map<String, AccountActivitiesVO> accountActivities;
    private TransactionSummaryVO             incomingInvoices;
    private TransactionSummaryVO             outgoingInvoices;
    private int                              numberBrokeredMembers;
    private Calendar                         sinceActive;
    private Map<Reference.Level, Integer>    givenReferencesByLevel;
    private Map<Reference.Level, Integer>    receivedReferencesByLevel;
    private Map<Ad.Status, Integer>          adsByStatus;

    public void addAccountActivities(final String account, final AccountActivitiesVO vo) {
        if (accountActivities == null) {
            accountActivities = new LinkedHashMap<String, AccountActivitiesVO>();
        }
        accountActivities.put(account, vo);
    }

    public Map<String, AccountActivitiesVO> getAccountActivities() {
        return accountActivities;
    }

    public Map<Ad.Status, Integer> getAdsByStatus() {
        return adsByStatus;
    }

    public Map<Reference.Level, Integer> getGivenReferencesByLevel() {
        return givenReferencesByLevel;
    }

    public TransactionSummaryVO getIncomingInvoices() {
        if (incomingInvoices == null) {
            incomingInvoices = new TransactionSummaryVO();
        }
        return incomingInvoices;
    }

    public int getNumberBrokeredMembers() {
        return numberBrokeredMembers;
    }

    public TransactionSummaryVO getOutgoingInvoices() {
        if (outgoingInvoices == null) {
            outgoingInvoices = new TransactionSummaryVO();
        }
        return outgoingInvoices;
    }

    public Map<Reference.Level, Integer> getReceivedReferencesByLevel() {
        return receivedReferencesByLevel;
    }

    public Calendar getSinceActive() {
        return sinceActive;
    }

    public boolean isSingleAccount() {
        return accountActivities == null ? false : accountActivities.size() == 1;
    }

    public void setAccountActivities(final Map<String, AccountActivitiesVO> accountActivities) {
        this.accountActivities = accountActivities;
    }

    public void setAdsByStatus(final Map<Ad.Status, Integer> adsByStatus) {
        this.adsByStatus = adsByStatus;
    }

    public void setGivenReferencesByLevel(final Map<Reference.Level, Integer> givenReferencesByLevel) {
        this.givenReferencesByLevel = givenReferencesByLevel;
    }

    public void setIncomingInvoices(final TransactionSummaryVO incomingInvoices) {
        this.incomingInvoices = incomingInvoices;
    }

    public void setNumberBrokeredMembers(final int numberBrokeredMembers) {
        this.numberBrokeredMembers = numberBrokeredMembers;
    }

    public void setOutgoingInvoices(final TransactionSummaryVO invoices) {
        outgoingInvoices = invoices;
    }

    public void setReceivedReferencesByLevel(final Map<Reference.Level, Integer> receivedReferencesByLevel) {
        this.receivedReferencesByLevel = receivedReferencesByLevel;
    }

    public void setSinceActive(final Calendar sinceActive) {
        if (sinceActive != null) {
            this.sinceActive = sinceActive;
        }
    }
}

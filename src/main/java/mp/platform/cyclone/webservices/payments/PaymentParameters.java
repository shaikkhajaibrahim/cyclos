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
package mp.platform.cyclone.webservices.payments;

import java.util.List;

import nl.strohalm.cyclos.utils.ObjectHelper;
import mp.platform.cyclone.webservices.model.FieldValueVO;

/**
 * Parameters for making a new payment
 * @author luis
 */
public class PaymentParameters extends AbstractPaymentParameters {
    private static final long  serialVersionUID = 8364312939644892473L;
    private Long               transferTypeId;
    private Boolean            fromSystem;
    private Boolean            toSystem;
    private String             credentials;
    private List<FieldValueVO> customValues;
    private boolean            returnStatus;

    public String getCredentials() {
        return credentials;
    }

    public List<FieldValueVO> getCustomValues() {
        return customValues;
    }

    public Long getTransferTypeId() {
        return transferTypeId;
    }

    public boolean isFromSystem() {
        return ObjectHelper.valueOf(fromSystem);
    }

    public boolean isReturnStatus() {
        return returnStatus;
    }

    public boolean isToSystem() {
        return ObjectHelper.valueOf(toSystem);
    }

    public void setCredentials(final String credentials) {
        this.credentials = credentials;
    }

    public void setCustomValues(final List<FieldValueVO> customValues) {
        this.customValues = customValues;
    }

    public void setFromSystem(final boolean fromSystem) {
        this.fromSystem = fromSystem;
    }

    public void setReturnStatus(final boolean returnStatus) {
        this.returnStatus = returnStatus;
    }

    public void setToSystem(final boolean toSystem) {
        this.toSystem = toSystem;
    }

    public void setTransferTypeId(final Long transferTypeId) {
        this.transferTypeId = transferTypeId;
    }

    @Override
    public String toString() {
        return "PaymentParameters [credentials=****" + ", customValues=" + customValues + ", fromSystem=" + fromSystem + ", returnStatus=" + returnStatus + ", toSystem=" + toSystem + ", transferTypeId=" + transferTypeId + ", " + super.toString() + "]";
    }
}

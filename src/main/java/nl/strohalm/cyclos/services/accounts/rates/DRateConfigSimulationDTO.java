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
package nl.strohalm.cyclos.services.accounts.rates;

import java.math.BigDecimal;

import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * DTO for the simulation of the DRate configuration, showing the DRate curve in a graph.
 * @author Rinke
 * 
 */
public class DRateConfigSimulationDTO extends DataObject {

    private static final long serialVersionUID = 3305740610339929693L;

    private Currency          currency;
    private BigDecimal        interest;
    private BigDecimal        baseMalus;
    private BigDecimal        minimalD;
    private Integer           startD;
    private Integer           endD;

    public BigDecimal getBaseMalus() {
        return baseMalus;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Integer getEndD() {
        return endD;
    }

    public BigDecimal getInterest() {
        return interest;
    }

    public BigDecimal getMinimalD() {
        return minimalD;
    }

    public Integer getStartD() {
        return startD;
    }

    public void setBaseMalus(final BigDecimal baseMalus) {
        this.baseMalus = baseMalus;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setEndD(final Integer endD) {
        this.endD = endD;
    }

    public void setInterest(final BigDecimal interest) {
        this.interest = interest;
    }

    public void setMinimalD(final BigDecimal minimalD) {
        this.minimalD = minimalD;
    }

    public void setStartD(final Integer startD) {
        this.startD = startD;
    }

}

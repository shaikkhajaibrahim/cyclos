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
package nl.strohalm.cyclos.entities.accounts;

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;

/**
 * parent class for ARateParameters and DRateParameters. These classes store the parameters of rate configuration on the currency.
 * 
 * @author Rinke
 * 
 */
abstract public class RateParameters extends Entity {

    public static enum Relatonships implements Relationship {
        CURRENCY("currency");

        private final String name;

        private Relatonships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long serialVersionUID = 3436424608100999408L;

    private Currency          currency;
    private Calendar          date;
    private BigDecimal        initValue;
    private Calendar          initDate;
    private BigDecimal        creationValue;
    private Calendar          enabledSince;

    public RateParameters() {
    }

    public RateParameters(final RateParameters copyFrom) {
        date = Calendar.getInstance();
        initValue = copyFrom.initValue;
        initDate = (copyFrom.initDate == null) ? null : (Calendar) copyFrom.initDate.clone();
        creationValue = copyFrom.creationValue;
        enabledSince = (copyFrom.enabledSince == null) ? null : (Calendar) copyFrom.enabledSince.clone();
    }

    public BigDecimal getCreationValue() {
        return creationValue;
    }

    public Currency getCurrency() {
        return currency;
    }

    public Calendar getDate() {
        return date;
    }

    public Calendar getEnabledSince() {
        return enabledSince;
    }

    public Calendar getInitDate() {
        return initDate;
    }

    public BigDecimal getInitValue() {
        return initValue;
    }

    public void setCreationValue(final BigDecimal creationValue) {
        this.creationValue = creationValue;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setDate(final Calendar date) {
        this.date = date;
    }

    public void setEnabledSince(final Calendar enabledSince) {
        this.enabledSince = enabledSince;
    }

    public void setInitDate(final Calendar initDate) {
        this.initDate = initDate;
    }

    public void setInitValue(final BigDecimal initValue) {
        this.initValue = initValue;
    }

}

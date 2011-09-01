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
import java.util.Calendar;

import nl.strohalm.cyclos.entities.accounts.AccountStatus;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.utils.DataObject;

/**
 * Data Transfer Object for Rates (D-rate, A-rate). Class uses static methods to create instances, in stead of constructors. This is because the
 * static methods allow for more descriptive method names to create an instance for a specific purpose. If using constructors for this the class would
 * be packed with a lot of constructors which are not easily distinguished from each other.
 * 
 * @author Rinke
 */
public class RatesDTO extends DataObject {

    private static final long serialVersionUID = -6920158006595142222L;

    /**
     * pseudo-constructor used to store a result of an A-rate calculations. Just wraps the EmissionDate, but used for compatability with D-rate.
     * @param emissionDate - the emissionDate as calculated
     * @return an instance of RatesDTO containing the emissiondate
     */
    public static RatesDTO createARateInstance(final Calendar emissionDate) {
        final RatesDTO instance = new RatesDTO();
        instance.setEmissionDate(emissionDate);
        return instance;
    }

    /**
     * pseudo-constructor particularly used to store a result of a D-rate calculation. Returned instance contains the calculated D-rate and the date
     * to which this rate belongs.
     * @param processDate - the date to which this d-rate belongs.
     * @return an instance of RatesDTO containing the d-rate and the corresponding Calendar date.
     */
    public static RatesDTO createDRateInstance(final BigDecimal dRate, final Calendar processDate) {
        final RatesDTO instance = new RatesDTO();
        instance.setIncomingD(dRate);
        instance.setProcessDate(processDate);
        return instance;
    }

    /**
     * pseudo-constructor typically used to pass arguments into a merge calculation for the a-rate. Returned instance contains data of two set of
     * units which are to be merged, being the account as the first set, and an incoming amount with an emission date as the second set.
     * 
     * @param amount the incoming amount
     * @param emissionDate the emission date of the incoming amount.
     * @return an instance of RatesDTO containing all necessary data.
     */
    public static RatesDTO createDTOForARateMerge(final AccountStatus status, final BigDecimal amount, final Calendar emissionDate) {
        final RatesDTO instance = new RatesDTO();
        instance.setStatus(status);
        instance.setIncomingAmount(amount);
        instance.setEmissionDate(emissionDate);
        return instance;
    }

    /**
     * pseudo-constructor typically used to pass arguments into a merge calculation for the d-rate. Returned instance contains data of two set of
     * units which are to be merged, being the account as the first set, and an incoming amount with a d-rate as the second set.
     * @param incomingAmount
     * @param incomingD
     * @param processDate
     * @return an instance of RatesDTO containing all necessary data.
     */
    public static RatesDTO createDTOForDRateMerge(final AccountStatus status, final BigDecimal incomingAmount, final BigDecimal incomingD, final Calendar processDate) {
        final RatesDTO instance = new RatesDTO();
        instance.setStatus(status);
        instance.setIncomingAmount(incomingAmount);
        instance.setIncomingD(incomingD);
        instance.setProcessDate(processDate);
        return instance;
    }

    /**
     * pseudo-constructor for passing a set of units to an a-rate calculating method. Returned instance contains the amount, the emission date, and
     * the currency.
     * @param amount the amount of the set of units.
     * @param emissionDate the emissiondate belonging to this set of units.
     * @param currency the currency of the amount.
     * @return an instance of RatesDTO containing all necessary data.
     */
    public static RatesDTO createSetOfUnitsForARate(final BigDecimal amount, final Calendar emissionDate, final Currency currency) {
        final RatesDTO instance = new RatesDTO();
        instance.setIncomingAmount(amount);
        instance.setEmissionDate(emissionDate);
        instance.setCurrency(currency);
        return instance;
    }

    /**
     * Pseudo-constructor particularly used to pass a set of units to a d-rate calculating method. Returned instance contains the balance, the d-rate
     * and the currency.
     * @param amount the amount of the set of units
     * @param dRate the d-rate of this amount
     * @param currency the Currency of the amount.
     * @return an instance of RatesDTO containing all necessary data.
     */
    public static RatesDTO createSetOfUnitsForDRate(final BigDecimal amount, final BigDecimal dRate, final Currency currency) {
        final RatesDTO instance = new RatesDTO();
        instance.setIncomingAmount(amount);
        instance.setIncomingD(dRate);
        instance.setCurrency(currency);
        return instance;
    }

    private AccountStatus status;
    private BigDecimal    incomingAmount;
    private BigDecimal    incomingD;
    private Calendar      processDate;
    private Currency      currency;
    private Calendar      emissionDate;

    private RatesDTO() {
    }

    public BigDecimal getAmount() {
        return incomingAmount;
    }

    public Currency getCurrency() {
        return currency;
    }

    public BigDecimal getD() {
        return incomingD;
    }

    public Calendar getEmissionDate() {
        return emissionDate;
    }

    public Long getEmissionDateAsMillis() {
        return emissionDate.getTimeInMillis();
    }

    public BigDecimal getIncomingAmount() {
        return incomingAmount;
    }

    public BigDecimal getIncomingD() {
        return incomingD;
    }

    public Calendar getProcessDate() {
        return processDate;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setEmissionDate(final Calendar emissionDate) {
        this.emissionDate = emissionDate;
    }

    public void setIncomingAmount(final BigDecimal incomingAmount) {
        this.incomingAmount = incomingAmount;
    }

    public void setIncomingD(final BigDecimal incomingD) {
        this.incomingD = incomingD;
    }

    public void setProcessDate(final Calendar processDate) {
        this.processDate = processDate;
    }

    public void setStatus(final AccountStatus status) {
        this.status = status;
    }

}

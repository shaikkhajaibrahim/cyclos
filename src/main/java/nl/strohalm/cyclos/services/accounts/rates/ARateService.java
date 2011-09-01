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

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.TransactionFee;
import nl.strohalm.cyclos.entities.accounts.fees.transaction.SimpleTransactionFee.ARateRelation;
import nl.strohalm.cyclos.services.stats.StatisticalResultDTO;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.validation.Validator;

/**
 * Interface for all business logic for the A-Rates, which includes code for updating rates with time, code for merging rates, and code for applying
 * rates in conversions.
 * 
 * @author Rinke
 * 
 */
public interface ARateService extends RateService {

    /**
     * does the validation for the ARated fields, for as well TransactionFees, as ARatedFeeDTO (for A-rate configuration simulation).
     * @param validator
     * @param aRateRelation
     */
    Validator applyARateFieldsValidation(final Validator validator, final ARatedFeeDTO dto, final ARateRelation aRateRelation);

    /**
     * converts an Emissiondate to the aRate.
     */
    BigDecimal emissionDateToRate(final Calendar emissionDate, Calendar actualDate);

    /**
     * gets the data for producing a graph of the configuration curve for the A-rate.
     * @return a StatisticalResultDTO which must be used in the constructor of a StatisticalDataProducer. The statisticsResult.jsp automatically
     * handles a request attribute as a list of statisticalDataProducers, so all graphs are displayed.
     */
    @AdminAction(@Permission(module = "systemReports", operation = "aRateConfigSimulation"))
    StatisticalResultDTO getARateConfigGraph(final ARatedFeeDTO inputParameters);

    /**
     * gets the percentage of the fee with chargeType is A_RATED or MIXEX_A_D_RATES. Calculates this from the fee parameters and the rates, but gets
     * the rates from the given account and the date.
     * @return the percentage of the fee.
     */
    BigDecimal getARatedFeePercentage(final TransactionFee fee, final Account fromAccount, final Calendar date);

    /**
     * gets the precentage of the fee with chargeType A_RATED or MIXED_A_D_RATES. Just wraps the fee in a ARatedFeeDTO and calls
     * getARatedFeePercentage(aRatedFeeDTO, aRAte, dRate)
     * @param fee
     * @param aRate a BigDecimal being the A-rate to be used. May NOT be null.
     * @param dRate a BigDecimal being the D-rate to be used. May be null in case of MIXED_A_D_RATES; in that case the param is not used.
     * @return the percentage of the fee charge.
     */
    BigDecimal getARatedFeePercentage(final TransactionFee fee, final BigDecimal aRate, final BigDecimal dRate);

    /**
     * validate the ARateConfigSimulation
     */
    @DontEnforcePermission(traceable = true)
    void validate(ARatedFeeDTO dto, ARateRelation rateRelation);

}

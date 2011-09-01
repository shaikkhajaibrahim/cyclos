/*
   This file is part of Cyclos.
   Copyright: STRO Organization. All rights reserved. 
 */
package nl.strohalm.cyclos.services.accounts.rates;

import java.math.BigDecimal;
import java.util.Calendar;

import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.services.stats.StatisticalResultDTO;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.Permission;

/**
 * Interface for all business logic for the D-Rates, which includes code for updating rates with time, code for merging rates, and code for applying
 * rates in conversions.
 * 
 * @author Rinke
 * 
 */
public interface DRateService extends RateService {

    /**
     * Calculates the D-rated fee amount to be paid when a conversion transfer is done.
     * @param amount the amount to be converted (L, in the model description)
     * @param fromAccount the account from which this is converted. The D-rate of this account is used as the D-rate for the amount to be converted
     * @param date the process date for the conversion
     * @return the fee amount, that is: the transferAmount minus the conversion result. Returns null if no D-rate is enabled.
     */
    BigDecimal convertWithDRate(BigDecimal amount, Account fromAccount, Calendar date);

    /**
     * calculates the conversion result according to the D-rate, from the currency's parameters.
     * @param setOfUnits a RatesDTO containing balance, D-Rate and currency.
     * @return The amount in national currency after conversion. Returns null if d-rate is not enabled on the currency.
     */
    BigDecimal getDRateConversionResult(final RatesDTO setOfUnits);

    /**
     * gets the data for producing a graph of the configuration curve for the D-rate.
     * @return a StatisticalResultDTO which must be used in the constructor of a StatisticalDataProducer. The statisticsResult.jsp automatically
     * handles a request attribute as a list of statisticalDataProducers, so all graphs are displayed.
     */
    @AdminAction(@Permission(module = "systemReports", operation = "dRateConfigSimulation"))
    StatisticalResultDTO getRateConfigGraph(DRateConfigSimulationDTO input);

    /**
     * Validate the dto for d-rate configuration simulation
     * @param dto
     */
    @DontEnforcePermission(traceable = true)
    void validate(DRateConfigSimulationDTO dto);

}

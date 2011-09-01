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
package nl.strohalm.cyclos.controls.reports.simulations;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseFormAction;
import nl.strohalm.cyclos.controls.reports.statistics.graphs.StatisticalDataProducer;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.accounts.CurrencyService;
import nl.strohalm.cyclos.services.accounts.rates.DRateConfigSimulationDTO;
import nl.strohalm.cyclos.services.accounts.rates.DRateService;
import nl.strohalm.cyclos.services.stats.StatisticalResultDTO;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;

import org.apache.struts.action.ActionForward;

/**
 * Action for the Simulation of the DRate Configuration, showing the DRate curve in a graph.
 * @author Rinke
 * 
 */
public class DRateConfigSimulationAction extends BaseFormAction implements LocalSettingsChangeListener {

    private DataBinder<DRateConfigSimulationDTO> dataBinder;
    private DRateService                         dRateService;
    private CurrencyService                      currencyService;

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        dataBinder = null;
    }

    @Inject
    public void setCurrencyService(final CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    @Inject
    public void setdRateService(final DRateService dRateService) {
        this.dRateService = dRateService;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final RateConfigSimulationForm form = context.getForm();
        if (form.isReloadData()) {
            return handleDisplay(context);
        }
        prepareForm(context);
        final DRateConfigSimulationDTO dto = getDataBinder().readFromString(form.getSimulation());
        final List<StatisticalDataProducer> dataList = new ArrayList<StatisticalDataProducer>();
        final StatisticalResultDTO rawDataObject = dRateService.getRateConfigGraph(dto);
        final StatisticalDataProducer producer = new StatisticalDataProducer(rawDataObject, context);
        final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
        producer.setSettings(localSettings);
        dataList.add(producer);
        request.setAttribute("dataList", dataList);
        return context.getInputForward();
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final RateConfigSimulationForm form = context.getForm();
        final DRateConfigSimulationDTO dto = getDataBinder().readFromString(form.getSimulation());
        final List<Currency> ratedCurrencies = currencyService.listDRatedCurrencies();
        Currency currency = dto.getCurrency();
        final boolean firstTime = (currency == null);
        if (currency == null && ratedCurrencies.size() > 0) {
            currency = ratedCurrencies.get(0);
        }
        if (currency != null && (firstTime || form.isReloadData())) {
            currency = getFetchService().fetch(currency);
            final BigDecimal interest = currency.getdRateParameters().getInterest();
            final BigDecimal baseMalus = currency.getdRateParameters().getBaseMalus();
            final BigDecimal minimalD = currency.getMinimalD();
            dto.setCurrency(currency);
            dto.setInterest(interest);
            dto.setBaseMalus(baseMalus);
            dto.setMinimalD(minimalD);
            dto.setStartD(30);
            dto.setEndD(0);
        }
        getDataBinder().writeAsString(form.getSimulation(), dto);
        final HttpServletRequest request = context.getRequest();
        request.setAttribute("currencies", ratedCurrencies);
        if (ratedCurrencies.size() == 1) {
            request.setAttribute("singleCurrency", "true");
        }
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final RateConfigSimulationForm form = context.getForm();
        final DRateConfigSimulationDTO dto = getDataBinder().readFromString(form.getSimulation());
        dRateService.validate(dto);
    }

    private DataBinder<DRateConfigSimulationDTO> getDataBinder() {
        if (dataBinder == null) {
            final LocalSettings localSettings = SettingsHelper.getLocalSettings(getServlet().getServletContext());
            final BeanBinder<DRateConfigSimulationDTO> binder = BeanBinder.instance(DRateConfigSimulationDTO.class);
            binder.registerBinder("currency", PropertyBinder.instance(Currency.class, "currency"));
            binder.registerBinder("interest", PropertyBinder.instance(BigDecimal.class, "interest", localSettings.getHighPrecisionConverter()));
            binder.registerBinder("baseMalus", PropertyBinder.instance(BigDecimal.class, "baseMalus", localSettings.getNumberConverter()));
            binder.registerBinder("minimalD", PropertyBinder.instance(BigDecimal.class, "minimalD", localSettings.getNumberConverter()));
            binder.registerBinder("startD", PropertyBinder.instance(Integer.class, "startD"));
            binder.registerBinder("endD", PropertyBinder.instance(Integer.class, "endD"));
            dataBinder = binder;
        }
        return dataBinder;
    }

}

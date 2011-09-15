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
package mp.platform.cyclone.webservices.utils.server;

import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.services.accounts.CurrencyService;
import mp.platform.cyclone.webservices.model.CurrencyVO;

import org.apache.commons.lang.StringUtils;

/**
 * Utility class for currencies
 * @author luis
 */
public class CurrencyHelper {

    private CurrencyService currencyService;

    /**
     * Given a currency symbol or identifier as string, returns the {@link Currency} instance
     */
    public Currency resolve(final String string) {
        if (StringUtils.isNotEmpty(string)) {
            return currencyService.loadBySymbolOrId(string);
        }
        return null;
    }

    public void setCurrencyService(final CurrencyService currencyService) {
        this.currencyService = currencyService;
    }

    public CurrencyVO toVO(final Currency currency) {
        if (currency == null) {
            return null;
        }
        final CurrencyVO vo = new CurrencyVO();
        vo.setId(currency.getId());
        vo.setName(currency.getName());
        vo.setSymbol(currency.getSymbol());
        return vo;
    }
}

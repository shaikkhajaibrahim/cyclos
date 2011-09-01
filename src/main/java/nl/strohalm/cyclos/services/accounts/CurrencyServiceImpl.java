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
package nl.strohalm.cyclos.services.accounts;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.dao.accounts.ARateParametersDAO;
import nl.strohalm.cyclos.dao.accounts.CurrencyDAO;
import nl.strohalm.cyclos.dao.accounts.DRateParametersDAO;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.ARateParameters;
import nl.strohalm.cyclos.entities.accounts.Account;
import nl.strohalm.cyclos.entities.accounts.AccountType;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.DRateParameters;
import nl.strohalm.cyclos.entities.accounts.RateParameters;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.validation.PropertyValidation;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.Validator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;

/**
 * Service implementation for currencies.
 * 
 * @author luis
 * @author Rinke
 */
public class CurrencyServiceImpl implements CurrencyService {

    private List<Currency>     cachedCurrencies;
    private CurrencyDAO        currencyDao;
    private ARateParametersDAO aRateParametersDao;
    private DRateParametersDAO dRateParametersDao;
    private FetchService       fetchService;
    private SettingsService    settingsService;
    private AccountService     accountService;

    public List<Currency> listAll() {
        if (cachedCurrencies == null) {
            cachedCurrencies = currencyDao.listAll();
        }
        return new ArrayList<Currency>(cachedCurrencies);
    }

    public List<Currency> listByMember(final Member member) {
        final List<Currency> currencies = new ArrayList<Currency>();
        final List<? extends Account> accounts = accountService.getAccounts(member, RelationshipHelper.nested(Account.Relationships.TYPE, AccountType.Relationships.CURRENCY));
        for (final Account account : accounts) {
            final Currency currency = account.getType().getCurrency();
            if (!currencies.contains(currency)) {
                currencies.add(currency);
            }
        }
        return currencies;
    }

    public List<Currency> listByMemberGroup(final MemberGroup group) {
        List<Currency> currencies = currencyDao.listByMemberGroup(group);
        if (CollectionUtils.isEmpty(currencies)) {
            currencies = currencyDao.listAll();
        }
        return currencies;
    }

    public List<Currency> listDRatedCurrencies() {
        final List<Currency> currencies = currencyDao.listAll();
        final List<Currency> ratedCurrencies = new ArrayList<Currency>(currencies.size());
        for (final Currency currency : currencies) {
            if (currency.isEnableDRate()) {
                ratedCurrencies.add(currency);
            }
        }
        return ratedCurrencies;
    }

    public Currency load(final Long id, final Relationship... fetch) {
        return currencyDao.load(id, fetch);
    }

    public Currency loadBySymbolOrId(String symbolOrId) {
        symbolOrId = symbolOrId.trim();
        Long id;
        try {
            id = Long.parseLong(symbolOrId);
        } catch (final Exception e) {
            id = null;
        }
        for (final Currency currency : listAll()) {
            if (currency.getSymbol().equalsIgnoreCase(symbolOrId) || currency.getId().equals(id)) {
                return currency;
            }
        }
        throw new EntityNotFoundException(Currency.class);
    }

    public int remove(final Long... ids) {
        cachedCurrencies = null;
        return currencyDao.delete(ids);
    }

    public Currency save(Currency currency, final boolean enableARate, final boolean enableDRate) {
        validate(currency, enableARate, enableDRate);
        cachedCurrencies = null;
        boolean aRateChanged = false;
        boolean dRateChanged = false;
        // to be sure, set the reference to null if no rate enabled.
        if (!enableARate) {
            currency.setaRateParameters(null);
        }
        if (!enableDRate) {
            currency.setdRateParameters(null);
        }
        ARateParameters aRate = currency.getaRateParameters();
        DRateParameters dRate = currency.getdRateParameters();
        boolean newARate = true;
        boolean newDRate = true;
        if (currency.isTransient()) {
            currency.setaRateParameters(null);
            currency.setdRateParameters(null);
            currency = currencyDao.insert(currency, false);
            aRateChanged = dRateChanged = true;
        } else {
            final Currency old = load(currency.getId(), Currency.Relatonships.A_RATE_PARAMETERS, Currency.Relatonships.D_RATE_PARAMETERS);
            final ARateParameters oldARate = old.getaRateParameters();
            final DRateParameters oldDRate = old.getdRateParameters();
            aRateChanged = checkRateHasChanged(oldARate, aRate);
            dRateChanged = checkRateHasChanged(oldDRate, dRate);

            // if params have changed. A new record will be inserted
            if (aRateChanged) {
                currency.setaRateParameters(null);
            }
            if (dRateChanged) {
                currency.setdRateParameters(null);
            }

            currency = currencyDao.update(currency, false);
            // delete current Rate if no longer needed.
            // TODO RATES 9B: delete all rates for this currency
            if (aRateChanged && aRate == null && oldARate != null) {
                aRateParametersDao.delete(oldARate.getId());
            }
            if (dRateChanged && dRate == null && oldDRate != null) {
                dRateParametersDao.delete(oldDRate.getId());
            }

            newARate = aRateChanged && oldARate == null;
            newDRate = dRateChanged && oldDRate == null;
            if (!newARate && oldARate != null && aRate != null) {
                aRate.setEnabledSince(oldARate.getEnabledSince());
            }
            if (!newDRate && oldDRate != null && dRate != null) {
                dRate.setEnabledSince(oldDRate.getEnabledSince());
            }
        }

        final Calendar now = Calendar.getInstance();

        if (aRateChanged && aRate != null) {
            // Copy of the previous Rate, to avoid trying to insert an already persistent entity
            aRate = new ARateParameters(aRate);
            if (newARate) {
                aRate.setEnabledSince(now);
            }
            aRate.setCurrency(currency);
            aRate = aRateParametersDao.insert(aRate);
            currency.setaRateParameters(aRate);
            currencyDao.update(currency, false);
        }
        if (dRateChanged && dRate != null) {
            // Copy of the previous Rate, to avoid trying to insert an already persistent entity
            dRate = new DRateParameters(dRate);
            if (newDRate) {
                dRate.setEnabledSince(now);
            }
            dRate.setCurrency(currency);
            dRate = dRateParametersDao.insert(dRate);
            currency.setdRateParameters(dRate);
            currencyDao.update(currency, false);
        }

        // Now force flushing the 1st level cache
        fetchService.clearCache();

        return currency;
    }

    public void setAccountService(final AccountService accountService) {
        this.accountService = accountService;
    }

    public void setaRateParametersDao(final ARateParametersDAO aRateParametersDao) {
        this.aRateParametersDao = aRateParametersDao;
    }

    public void setCurrencyDao(final CurrencyDAO currencyDao) {
        this.currencyDao = currencyDao;
    }

    public void setdRateParametersDao(final DRateParametersDAO dRateParametersDao) {
        this.dRateParametersDao = dRateParametersDao;
    }

    public void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public void validate(final Currency currency, final boolean enableARate, final boolean enableDRate) {
        getValidator(enableARate, enableDRate).validate(currency);
    }

    private boolean checkRateHasChanged(final RateParameters oldRate, final RateParameters newRate) {
        if (newRate != null && oldRate == null) {
            return true;
        } else if (newRate == null && oldRate != null) {
            return true;
        } else if (newRate != null && oldRate != null) {
            final LocalSettings localSettings = settingsService.getLocalSettings();
            final EqualsBuilder eb = new EqualsBuilder();
            eb.append(localSettings.round(newRate.getInitValue()), localSettings.round(oldRate.getInitValue()));
            eb.append(newRate.getInitDate(), oldRate.getInitDate());
            eb.append(localSettings.round(newRate.getCreationValue()), localSettings.round(oldRate.getCreationValue()));
            if (oldRate instanceof DRateParameters && newRate instanceof DRateParameters) {
                final DRateParameters oldDRate = (DRateParameters) oldRate;
                final DRateParameters newDRate = (DRateParameters) newRate;
                eb.append(localSettings.roundHighPrecision(newDRate.getInterest()), localSettings.roundHighPrecision(oldDRate.getInterest()));
                eb.append(localSettings.round(newDRate.getBaseMalus()), localSettings.round(oldDRate.getBaseMalus()));
                eb.append(localSettings.round(newDRate.getMinimalD()), localSettings.round(oldDRate.getMinimalD()));
            }
            return !eb.isEquals();
        }
        return false;
    }

    private Validator getBaseValidator() {
        final Validator validator = new Validator("currency");
        validator.property("name").required().maxLength(100);
        validator.property("description").maxLength(2000);
        validator.property("symbol").required().maxLength(20);

        validator.property("pattern").required().maxLength(30).add(new PropertyValidation() {
            private static final long serialVersionUID = 455899399346626634L;

            public ValidationError validate(final Object object, final Object name, final Object value) {
                final String pattern = (String) value;
                // Check if units pattern contains #amount#
                if (!StringUtils.isEmpty(pattern) && !pattern.contains("#amount#")) {
                    return new ValidationError("currency.error.pattern");
                }
                return null;
            }
        });
        return validator;
    }

    private Validator getValidator(final boolean enableARate, final boolean enableDRate) {
        final Validator validator = getBaseValidator();
        if (enableARate) {
            setRateValidation(validator, "aRateParameters", "currency.aRate.");
        }
        if (enableDRate) {
            final String keyPrefix = "currency.dRate.";
            validator.property("dRateParameters.interest").key(keyPrefix + "interest").required().positive();
            validator.property("dRateParameters.baseMalus").key(keyPrefix + "baseMalus").required().positive();
            validator.property("dRateParameters.minimalD").key(keyPrefix + "minimalD").required();
            setRateValidation(validator, "dRateParameters", keyPrefix);
        }
        return validator;
    }

    private void setRateValidation(final Validator validator, final String propertyRateName, final String keyPrefix) {
        validator.property(propertyRateName + ".initValue").key(keyPrefix + "initValue").required();
        validator.property(propertyRateName + ".initDate").key(keyPrefix + "initDate").required();
        validator.property(propertyRateName + ".creationValue").key(keyPrefix + "creationValue").required();
    }
}

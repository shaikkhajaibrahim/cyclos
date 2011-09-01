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
import java.util.Collection;
import java.util.Map;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.utils.conversion.NumberConverter;

/**
 * A generic account
 * @author luis
 */
public abstract class Account extends Entity {

    public static enum Relationships implements Relationship {
        TYPE("type"), STATUSES("statuses");
        private final String name;

        private Relationships(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private static final long         serialVersionUID = 2606773131957831444L;
    private Calendar                  creationDate;
    private BigDecimal                creditLimit;
    private BigDecimal                upperCreditLimit;
    private AccountType               type;
    private String                    ownerName;
    private Collection<AccountStatus> statuses;

    public Calendar getCreationDate() {
        return creationDate;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public abstract AccountType.Nature getNature();

    public abstract AccountOwner getOwner();

    public String getOwnerName() {
        return ownerName;
    }

    public Collection<AccountStatus> getStatuses() {
        return statuses;
    }

    public AccountType getType() {
        return type;
    }

    public BigDecimal getUpperCreditLimit() {
        return upperCreditLimit;
    }

    public void setCreationDate(final Calendar creationDate) {
        this.creationDate = creationDate;
    }

    public void setCreditLimit(final BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public void setOwnerName(final String ownerName) {
        this.ownerName = ownerName;
    }

    public void setStatuses(final Collection<AccountStatus> statuses) {
        this.statuses = statuses;
    }

    public void setType(final AccountType type) {
        this.type = type;
    }

    public void setUpperCreditLimit(final BigDecimal upperCreditLimit) {
        this.upperCreditLimit = upperCreditLimit;
    }

    @Override
    protected void appendVariableValues(final Map<String, Object> variables, final LocalSettings localSettings) {
        NumberConverter<BigDecimal> converter;
        try {
            converter = localSettings.getUnitsConverter(getType().getCurrency().getPattern());
        } catch (final Exception e) {
            converter = localSettings.getNumberConverter();
        }
        if (creditLimit != null) {
            variables.put("creditLimit", converter.toString(creditLimit.negate()));
        }
        if (upperCreditLimit != null) {
            variables.put("upperCreditLimit", converter.toString(upperCreditLimit));
        }
    }

}
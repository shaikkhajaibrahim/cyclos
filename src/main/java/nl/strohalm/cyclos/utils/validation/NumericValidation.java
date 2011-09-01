/*
    This file is part of Cyclos <http://project.cyclos.org>

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.utils.validation;

import org.apache.commons.lang.StringUtils;

public class NumericValidation implements PropertyValidation {

    private static NumericValidation INSTANCE         = new NumericValidation();
    private static final long        serialVersionUID = -5841372577792943157L;

    public static NumericValidation instance() {
        return INSTANCE;
    }

    /**
     * This class could be used as a class validator in the custom field edition, then it must contains a public constructor
     */
    public NumericValidation() {

    }

    /**
     * Validates that the value only contains numeric chars
     * @see nl.strohalm.cyclos.utils.validation.PropertyValidation#validate(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public ValidationError validate(final Object object, final Object property, final Object value) {
        if (value != null && !StringUtils.isNumeric((String) value)) {
            return new ValidationError("errors.numeric");
        } else {
            return null;
        }
    }

}

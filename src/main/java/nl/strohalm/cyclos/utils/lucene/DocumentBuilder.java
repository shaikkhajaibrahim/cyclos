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
package nl.strohalm.cyclos.utils.lucene;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;

import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomField.Type;
import nl.strohalm.cyclos.utils.conversion.IdConverter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.hibernate.util.StringHelper;

/**
 * Helper to build a lucene {@link Document}
 * 
 * @author luis
 */
public class DocumentBuilder {

    private Document document;

    public DocumentBuilder() {
        document = new Document();
    }

    /**
     * Adds a decimal field
     */
    public DocumentBuilder add(final String name, final BigDecimal value) {
        return add(name, LuceneFormatter.format(value), false);
    }

    /**
     * Adds a boolean field
     */
    public DocumentBuilder add(final String name, final boolean value) {
        return add(name, String.valueOf(value), false);
    }

    /**
     * Adds an date field
     */
    public DocumentBuilder add(final String name, final Calendar value) {
        return add(name, LuceneFormatter.format(value), false);
    }

    /**
     * Adds custom field values
     */
    public DocumentBuilder add(final String name, final Collection<? extends CustomFieldValue> fieldValues) {
        if (CollectionUtils.isNotEmpty(fieldValues)) {
            for (final CustomFieldValue fieldValue : fieldValues) {
                String stringValue = fieldValue.getStringValue();
                final CustomFieldPossibleValue possibleValue = fieldValue.getPossibleValue();
                if (StringUtils.isEmpty(stringValue) && possibleValue == null) {
                    // No value for the current field
                    continue;
                }

                // Check the field type
                final CustomField field = fieldValue.getField();
                final Type type = field.getType();
                final boolean isString = type == CustomField.Type.STRING || type == CustomField.Type.URL;
                final boolean isEnumerated = type == CustomField.Type.ENUMERATED;

                // Include the general field
                if (isString) {
                    // Only analyze when there is no mask
                    final boolean analyzed = StringHelper.isEmpty(field.getPattern());
                    stringValue = nl.strohalm.cyclos.utils.StringHelper.removeMarkupTagsAndUnescapeEntities(stringValue);
                    add(name, stringValue, analyzed);
                } else if (isEnumerated) {
                    add(name, possibleValue.getValue());
                }

                // Include the specific field
                final String specificFieldName = name + "." + field.getId();
                if (isString) {
                    // Only analyze when there is no mask
                    final boolean analyzed = StringHelper.isEmpty(field.getPattern());
                    stringValue = nl.strohalm.cyclos.utils.StringHelper.removeMarkupTagsAndUnescapeEntities(stringValue);
                    add(specificFieldName, stringValue, analyzed);
                } else if (isEnumerated) {
                    add(specificFieldName, possibleValue);
                } else {
                    // Other types are not analyzed
                    add(specificFieldName, stringValue, false);
                }
            }
        }
        return this;
    }

    /**
     * Adds an entity field, which is stored as id
     */
    public DocumentBuilder add(final String name, final Entity entity) {
        if (entity != null) {
            add(name, IdConverter.instance().toString(entity.getId()), false);
        }
        return this;
    }

    /**
     * Adds an enumerated field
     */
    public DocumentBuilder add(final String name, final Enum<?> enumerated) {
        if (enumerated != null) {
            add(name, enumerated.name(), false);
        }
        return this;
    }

    /**
     * Adds an analyzed string field
     */
    public DocumentBuilder add(final String name, final String value) {
        return add(name, value, true);
    }

    /**
     * Adds an string field, which may be analyzer or not
     */
    public DocumentBuilder add(final String name, final String value, final boolean analyzed) {
        if (StringUtils.isNotEmpty(value)) {
            final Field field = new Field(name, value, Store.YES, analyzed ? Index.ANALYZED : Index.NOT_ANALYZED);
            document.add(field);
        }
        return this;
    }

    public Document getDocument() {
        return document;
    }
}

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
package nl.strohalm.cyclos.taglibs;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldPossibleValue;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.Validation;
import nl.strohalm.cyclos.utils.MessageHelper;
import nl.strohalm.cyclos.utils.RangeConstraint;
import nl.strohalm.cyclos.utils.StringHelper;
import nl.strohalm.cyclos.utils.conversion.IdConverter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Tag used to render a custom field
 * @author luis
 */
public class CustomFieldTag extends TagSupport {

    private static final long                      serialVersionUID = 1356009486707156305L;
    private boolean                                editable         = true;
    private boolean                                enabled          = true;
    private CustomField                            field;
    private String                                 fieldName;
    private String                                 styleId;
    private boolean                                search;
    private boolean                                textOnly;
    private String                                 size;
    private Object                                 value;
    private String                                 valueName;
    private Collection<? extends CustomFieldValue> collection;
    private boolean                                encodeHtml       = true;

    @Override
    public int doEndTag() throws JspException {
        try {
            if (field == null) {
                throw new JspException("Field is null on CustomFieldTag");
            }

            // Get the field value from collection if needed
            if (value == null && collection != null) {
                for (final CustomFieldValue customValue : collection) {
                    if (field.equals(customValue.getField())) {
                        value = customValue;
                        break;
                    }
                }
            }

            final StringBuilder sb = new StringBuilder();

            // There is a hidden that will store the field id
            if (StringUtils.isNotEmpty(fieldName)) {
                sb.append("<input type=\"hidden\" name=\"").append(fieldName).append("\" value=\"").append(field.getId()).append("\">");
            }

            final CustomField.Control control = field.getControl();

            if (editable && !textOnly) {
                if (field.getType() == CustomField.Type.BOOLEAN) {
                    if (search) {
                        // When a boolean is on search, we use a select with true and false values
                        renderSelect(sb);
                    } else {
                        // Boolean is always a check box. As it is not submitted when is not checked, we must use a hidden that will be submitted
                        renderCheckBox(sb);
                    }
                } else if (field.getType() == CustomField.Type.ENUMERATED) {
                    final Collection<CustomFieldPossibleValue> possibleValues = field.getPossibleValues();
                    if (CollectionUtils.isEmpty(possibleValues)) {
                        // If there are no possible values, render a hidden, so it is ensured that a value will be submitted
                        sb.append("<input type=\"hidden\" name=\"").append(valueName).append("\">");
                    } else {
                        if (control == CustomField.Control.RADIO && !search) {
                            // Render a radio group
                            renderRadioGroup(sb);
                        } else {
                            // Render a select box
                            renderSelect(sb);
                        }
                    }
                } else if (!search && field.getControl() == CustomField.Control.TEXTAREA) {
                    renderTextarea(sb);
                } else if (!search && field.getControl() == CustomField.Control.RICH_EDITOR) {
                    renderRichEditor(sb);
                } else {
                    renderText(sb);
                }
            } else {
                renderDisplay(sb);
            }
            pageContext.getOut().write(sb.toString());
        } catch (final IOException e) {
            throw new JspException(e);
        } finally {
            release();
        }
        return EVAL_PAGE;
    }

    public Collection<? extends CustomFieldValue> getCollection() {
        return collection;
    }

    public CustomField getField() {
        return field;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getSize() {
        return size;
    }

    public String getStyleId() {
        return styleId;
    }

    public Object getValue() {
        return value;
    }

    public String getValueName() {
        return valueName;
    }

    public boolean isEditable() {
        return editable;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isEncodeHtml() {
        return encodeHtml;
    }

    public boolean isSearch() {
        return search;
    }

    public boolean isTextOnly() {
        return textOnly;
    }

    @Override
    public void release() {
        super.release();
        field = null;
        fieldName = null;
        valueName = null;
        value = null;
        collection = null;
        enabled = true;
        editable = true;
        search = false;
        textOnly = false;
        encodeHtml = true;
        size = null;
        styleId = null;
    }

    public void setCollection(final Collection<? extends CustomFieldValue> collection) {
        this.collection = collection;
    }

    public void setEditable(final boolean editable) {
        this.editable = editable;
    }

    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }

    public void setEncodeHtml(final boolean encodeHtml) {
        this.encodeHtml = encodeHtml;
    }

    public void setField(final CustomField field) {
        this.field = field;
    }

    public void setFieldName(final String name) {
        fieldName = name;
    }

    public void setSearch(final boolean search) {
        this.search = search;
    }

    public void setSize(final String size) {
        this.size = size;
    }

    public void setStyleId(final String styleId) {
        this.styleId = styleId;
    }

    public void setTextOnly(final boolean textOnly) {
        this.textOnly = textOnly;
    }

    public void setValue(final Object value) {
        this.value = value;
    }

    public void setValueName(final String valueField) {
        valueName = valueField;
    }

    private String ensureHtml(final String string) {
        return StringUtils.replace(StringEscapeUtils.escapeHtml(string), "\n", "<br>");
    }

    private CustomFieldPossibleValue getPossibleValue() {
        if (value instanceof CustomFieldValue) {
            return ((CustomFieldValue) value).getPossibleValue();
        }
        return null;
    }

    /**
     * Return a style class for the current field size
     */
    private String getSizeClass() {
        if (StringUtils.isNotEmpty(size)) {
            return size;
        }
        final CustomField.Size size = field == null ? null : field.getSize();
        if (size == null || size == CustomField.Size.DEFAULT) {
            return "";
        }
        return size.name().toLowerCase();
    }

    private String getStringValue() {
        String string;
        if (value instanceof String) {
            string = (String) value;
        } else if (value instanceof CustomFieldValue) {
            string = ((CustomFieldValue) value).getValue();
        } else {
            string = null;
        }
        if (StringUtils.isNotEmpty(field.getPattern())) {
            string = StringHelper.applyMask(field.getPattern(), string);
        }
        return string == null ? "" : string;
    }

    /**
     * Returns a collection containing all possible values which are not disabled, however, keeping the current value even when disabled
     */
    private Collection<CustomFieldPossibleValue> removeDisabledValues(final CustomFieldPossibleValue selected, final Collection<CustomFieldPossibleValue> possibleValues) {
        final Collection<CustomFieldPossibleValue> result = new ArrayList<CustomFieldPossibleValue>();
        for (final CustomFieldPossibleValue possibleValue : possibleValues) {
            boolean useField = true;
            if (!possibleValue.isEnabled()) {
                if (selected == null || !selected.equals(possibleValue)) {
                    useField = false;
                }
            }
            if (useField) {
                result.add(possibleValue);
            }
        }
        return result;
    }

    private void renderCheckBox(final StringBuilder sb) throws JspException {
        final boolean value = Boolean.parseBoolean(getStringValue());
        sb.append("<input type=\"hidden\" id=\"_value_of_").append(field.getId()).append("\" name=\"").append(valueName).append("\" value=\"").append(value).append("\">");
        sb.append("<input type=\"checkbox\" id=\"_check_of_").append(field.getId()).append("\" class=\"checkbox\" onclick=\"if (!this.disabled){$('_value_of_").append(field.getId()).append("').value=String(this.checked);}\"");
        if (!enabled) {
            sb.append("disabled ");
        }
        if (value) {
            sb.append(" checked");
        }
        sb.append(">");
    }

    private void renderDisplay(final StringBuilder sb) throws JspException {
        boolean skipEncode = false;
        String value;
        switch (field.getType()) {
            case ENUMERATED:
                try {
                    value = getPossibleValue().getValue();
                } catch (final NullPointerException e) {
                    value = "";
                }
                break;
            case BOOLEAN:
                final String key = "true".equals(getStringValue()) ? "global.yes" : "global.no";
                value = MessageHelper.message(pageContext.getServletContext(), key);
                break;
            case URL:
                value = getStringValue();
                if (StringUtils.isNotEmpty(value)) {
                    if (!value.contains("://")) {
                        value = "http://" + value;
                    }
                    if (editable) {
                        value = "<a href=\"" + value + "\" class=\"default\" target=\"_blank\">" + value + "</a>";
                        skipEncode = true;
                    }
                }
                break;
            default:
                value = getStringValue();
                break;
        }
        if (!textOnly) {
            sb.append("<input type=\"text\" name=\"").append(valueName).append("\" value=\"");
        }
        value = StringUtils.trimToEmpty(value);

        if (!skipEncode && encodeHtml && field.getControl() != CustomField.Control.RICH_EDITOR) {
            value = StringUtils.replace(StringUtils.replace(StringUtils.replace(StringUtils.replace(value, "\"", "&quot;"), "<", "&lt;"), ">", "&gt;"), "\n", "<br>");
        }
        sb.append(value);
        if (!textOnly) {
            sb.append("\" class=\"" + getSizeClass() + " readonly InputBoxDisabled\" readonly>\n");
        }
    }

    private void renderRadioGroup(final StringBuilder sb) throws JspException {
        final CustomFieldPossibleValue value = getPossibleValue();
        sb.append("<table border='0' cellpadding='0' cellspacing='0' style='border-spacing:0px;'>\n");
        final boolean requiredField = field != null && field.getValidation().isRequired();
        sb.append("<tr>\n");
        int i = 0;
        int controlsPerRow = Integer.MAX_VALUE;
        if (field != null && field.getSize() != null) {
            switch (field.getSize()) {
                case TINY:
                    controlsPerRow = 1;
                    break;
                case SMALL:
                    controlsPerRow = 2;
                    break;
                case MEDIUM:
                    controlsPerRow = 3;
                    break;
                case LARGE:
                    controlsPerRow = 4;
                    break;
            }
        }
        String selectedValue = "";
        final String name = "_radio_" + field.getId();
        // Remove the disabled possible values, but keep the current value even if it's disabled
        final Collection<CustomFieldPossibleValue> possibleValues = removeDisabledValues(value, field.getPossibleValues());

        // Check whether the default value will be used
        boolean useDefault = !isSearch() && this.value == null;
        if (this.value instanceof CustomFieldValue) {
            // Use on new values only
            final CustomFieldValue fieldValue = (CustomFieldValue) this.value;
            useDefault = fieldValue.isTransient();
        }

        // Render each radio
        for (final CustomFieldPossibleValue possibleValue : possibleValues) {
            final String id = name + "_" + i;
            final String idAsString = String.valueOf(possibleValue.getId());
            final boolean checked = ObjectUtils.equals(value, possibleValue) || (useDefault && possibleValue.isDefaultValue());
            if (checked) {
                selectedValue = idAsString;
            }
            sb.append("<td valign='top' width='20px'><input type='radio' name='").append(name).append("' id=\"").append(id).append("\" class='radio' value='").append(idAsString).append("'").append(enabled ? "" : " disabled").append(checked ? " checked" : "");
            sb.append(" onclick=\"if (!this.disabled){$('_value_of_").append(field.getId()).append("').value=this.value;}\"");
            sb.append("></td>\n");
            final boolean isRequired = requiredField && i == possibleValues.size() - 1;
            sb.append("<td><label ");
            if (isRequired) {
                sb.append("class=\"required\" ");
            }
            sb.append(" for=\"").append(id).append("\">").append(possibleValue.getValue()).append("</label></td>\n");
            i++;
            if (i % controlsPerRow == 0) {
                sb.append("<tr>\n</tr>\n");
            }
        }
        sb.append("</tr>\n");
        sb.append("</table>\n");
        sb.append("<input type=\"hidden\" id=\"_value_of_").append(field.getId()).append("\" name=\"").append(valueName).append("\" value=\"" + selectedValue + "\">\n");
    }

    private void renderRichEditor(final StringBuilder sb) {
        final RichTextAreaTag tag = new RichTextAreaTag();
        tag.setStyleId("richText" + getField().getId());
        tag.setDisabled(!enabled);
        tag.setName(valueName);
        tag.setPageContext(pageContext);
        tag.setValue(getStringValue());
        try {
            tag.doStartTag();
            tag.doEndTag();
        } catch (final JspException e) {
            throw new RuntimeException(e);
        }
    }

    private void renderSelect(final StringBuilder sb) throws JspException {

        final boolean hasChildren = CollectionUtils.isNotEmpty(field.getChildren());
        final boolean hasParent = field.getParent() != null;

        // We show the empty label in 2 situations: search or required fields
        String emptyLabel = "";
        if (search || field.getValidation().isRequired()) {
            if (search) {
                emptyLabel = StringUtils.trimToEmpty(field.getAllSelectedLabel());
                if (StringUtils.isEmpty(emptyLabel)) {
                    emptyLabel = MessageHelper.message(pageContext.getServletContext(), "global.search.all");
                }
            } else {
                emptyLabel = MessageHelper.message(pageContext.getServletContext(), "global.select.empty");
            }
        }

        final CustomFieldPossibleValue selected = getPossibleValue();
        final Collection<CustomFieldPossibleValue> possibleValues = removeDisabledValues(selected, field.getPossibleValues());

        // We will render a MultiDropDown control if we are on search for enumerated fields, and the field has no relationships
        if (!hasChildren && !hasParent && search && field.getType() == CustomField.Type.ENUMERATED && field.getPossibleValues().size() > 2) {
            // MultiDropDown
            final StringWriter temp = new StringWriter();
            pageContext.pushBody(temp);
            final MultiDropDownTag multiSelect = new MultiDropDownTag();
            multiSelect.setPageContext(pageContext);
            multiSelect.setName(valueName);
            multiSelect.setSingleField(true);
            multiSelect.doStartTag();
            multiSelect.setEmptyLabel(emptyLabel);
            final OptionTag option = new OptionTag();
            final List<String> values = Arrays.asList(StringUtils.trimToEmpty(getStringValue()).split(","));
            for (final CustomFieldPossibleValue possibleValue : possibleValues) {
                final String idAsString = String.valueOf(possibleValue.getId());
                option.setParent(multiSelect);
                option.setPageContext(pageContext);
                option.setValue(idAsString);
                option.setText(possibleValue.getValue());
                option.setSelected(values.contains(idAsString));
                option.doStartTag();
                option.doEndTag();
            }
            multiSelect.doEndTag();
            pageContext.popBody();
            sb.append(temp.toString());
        } else {
            // Normal select or search for booleans
            final String id = "custom_field_select_" + field.getId();
            sb.append("<select id=\"").append(id).append("\"");
            sb.append(" fieldId=\"").append(field.getId()).append("\"");
            sb.append(" fieldName=\"").append(field.getInternalName()).append("\" name=\"").append(valueName).append('"');
            if (search) {
                sb.append(" fieldEmptyLabel=\"").append(StringEscapeUtils.escapeHtml(emptyLabel)).append('"');
            }
            final Validation validation = field.getValidation();
            final List<String> classes = new ArrayList<String>();
            if (validation != null && validation.isRequired() && !search) {
                classes.add("required");
            }
            if (!enabled) {
                classes.add("InputBoxDisabled");
                sb.append(" disabled");
            }
            sb.append(" class=\"").append(StringUtils.join(classes.iterator(), ' ')).append("\">\n");
            sb.append("<option value=''>");
            sb.append(StringEscapeUtils.escapeHtml(emptyLabel));
            sb.append("</option>\n");
            if (field.getType() == CustomField.Type.ENUMERATED) {
                // Render each possible value as an enumerated value
                CustomFieldPossibleValue value = selected;
                // When the value is not a PossibleValue, try to find it as String
                if (value == null) {
                    final String valueAsString = getStringValue();
                    if (StringUtils.isNotEmpty(valueAsString)) {
                        // The value as string is the possible value id
                        final Long possibleValueId = IdConverter.instance().valueOf(valueAsString);
                        value = (CustomFieldPossibleValue) CollectionUtils.find(field.getPossibleValues(), new Predicate() {
                            public boolean evaluate(final Object obj) {
                                final CustomFieldPossibleValue pv = (CustomFieldPossibleValue) obj;
                                return pv.getId().equals(possibleValueId);
                            }
                        });
                    }
                }

                // Check whether the default value will be used
                boolean useDefault = !isSearch() && this.value == null;
                if (this.value instanceof CustomFieldValue) {
                    // Use on new values only
                    final CustomFieldValue fieldValue = (CustomFieldValue) this.value;
                    useDefault = fieldValue.isTransient();
                }

                // When a field has children, store it's current value on the page scope, so that child fields can filter their possible values
                if (hasChildren) {
                    pageContext.setAttribute("valueForField_" + field.getId(), value);
                }

                // Retrieve the possible values
                final CustomField parentField = field.getParent();
                if (parentField != null) {
                    final CustomFieldPossibleValue parentValue = (CustomFieldPossibleValue) pageContext.getAttribute("valueForField_" + parentField.getId());
                    CollectionUtils.filter(possibleValues, new Predicate() {
                        public boolean evaluate(final Object object) {
                            final CustomFieldPossibleValue possibleValue = (CustomFieldPossibleValue) object;
                            return ObjectUtils.equals(parentValue, possibleValue.getParent());
                        }
                    });
                }

                // Write the options
                for (final CustomFieldPossibleValue possibleValue : possibleValues) {
                    final String idAsString = String.valueOf(possibleValue.getId());
                    sb.append("<option");
                    if (ObjectUtils.equals(value, possibleValue) || (useDefault && possibleValue.isDefaultValue())) {
                        sb.append(" selected");
                    }
                    sb.append(" value=\"").append(idAsString).append("\">");
                    sb.append(ensureHtml(possibleValue.getValue())).append("</option>\n");
                }
            } else {
                // Render the options for a boolean
                sb.append("<option value='true'");
                if (value != null && "true".equalsIgnoreCase(value.toString())) {
                    sb.append(" selected");
                }
                sb.append(">").append(MessageHelper.message(pageContext.getServletContext(), "global.yes")).append("</option>\n");
                sb.append("<option value='false'");
                if (value != null && "false".equalsIgnoreCase(value.toString())) {
                    sb.append(" selected");
                }
                sb.append(">").append(MessageHelper.message(pageContext.getServletContext(), "global.no")).append("</option>\n");
            }
            if (hasChildren) {
                sb.append("</select>\n");
                sb.append("<script>\n");
                sb.append("$('").append(id).append("').onchange = function() {\n");
                for (final CustomField child : field.getChildren()) {
                    final String childId = "custom_field_select_" + child.getId();
                    sb.append("updateCustomFieldChildValues('").append(id).append("', '").append(childId).append("');\n");
                }
                sb.append("");
                sb.append("}\n");
                sb.append("Event.observe(self, 'load', $('").append(id).append("').onchange);\n");
                sb.append("</script>\n");
            }
        }
    }

    private void renderText(final StringBuilder sb) throws JspException {
        final List<String> classNames = new ArrayList<String>();

        final Validation validation = field.getValidation();
        if (validation != null && validation.isRequired() && !search) {
            classNames.add("required");
        }

        final String pattern = StringUtils.trimToNull(field.getPattern());
        switch (field.getType()) {
            case DATE:
                classNames.add("date");
                break;
            case INTEGER:
                classNames.add("number");
                break;
            case BIG_DECIMAL:
                classNames.add("float");
                break;
            case STRING:
                if (pattern != null) {
                    classNames.add("pattern");
                }
                break;
        }

        final String sizeClass = getSizeClass();
        if (StringUtils.isNotEmpty(sizeClass)) {
            classNames.add(sizeClass);
        }
        final String value = getStringValue();
        sb.append("<input type=\"text\" fieldName=\"").append(field.getInternalName()).append("\" name=\"").append(valueName).append("\" value=\"").append(StringEscapeUtils.escapeHtml(value)).append('"');
        if (enabled) {
            classNames.add("InputBoxEnabled");
        } else {
            sb.append(" readonly");
            classNames.add("InputBoxDisabled");
        }
        if (StringUtils.isNotEmpty(styleId)) {
            sb.append(" id=\"").append(styleId).append('"');
        }
        if (pattern != null) {
            sb.append(" maskPattern=\"").append(StringEscapeUtils.escapeHtml(pattern)).append('"');
        }
        final RangeConstraint lengthConstraint = validation == null ? null : validation.getLengthConstraint();
        if (lengthConstraint != null && lengthConstraint.getMax() != null && lengthConstraint.getMax() > 0) {
            sb.append(" maxLength=\"").append(lengthConstraint.getMax()).append('"');
        }
        sb.append(" class=\"").append(StringUtils.join(classNames.iterator(), ' ')).append("\">\n");
    }

    private void renderTextarea(final StringBuilder sb) {
        final List<String> classNames = new ArrayList<String>();

        final Validation validation = field.getValidation();
        if (validation != null && validation.isRequired() && !search) {
            classNames.add("required");
        }

        sb.append("<textarea rows='5' fieldId='").append(field.getId()).append("' fieldName=\"").append(field.getInternalName()).append("\" name=\"").append(valueName).append('"');
        final RangeConstraint lengthConstraint = validation == null ? null : validation.getLengthConstraint();
        if (lengthConstraint != null && lengthConstraint.getMax() != null && lengthConstraint.getMax() > 0) {
            sb.append(" maxLength=\"").append(lengthConstraint.getMax()).append('"');
            classNames.add("maxLength");
        }
        if (enabled) {
            classNames.add("InputBoxEnabled");
        } else {
            classNames.add("InputBoxDisabled");
            sb.append(" readonly");
        }
        final String sizeClass = getSizeClass();
        if (StringUtils.isNotEmpty(sizeClass)) {
            classNames.add(sizeClass);
        }
        sb.append(" class=\"").append(StringUtils.join(classNames.iterator(), ' ')).append("\">").append(getStringValue()).append("</textarea>");
    }
}

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
package nl.strohalm.cyclos.utils.binding;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import nl.strohalm.cyclos.utils.ClassHelper;
import nl.strohalm.cyclos.utils.PropertyHelper;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A property binder that can convert a whole bean from / to a set of properties
 * @author luis
 */
public class BeanBinder<T> extends DataBinder<T> {

    private static final long serialVersionUID = 6669808609710330346L;

    public static <T> BeanBinder<T> instance(final Class<T> beanClass) {
        return instance(beanClass, null);
    }

    public static <T> BeanBinder<T> instance(final Class<T> beanClass, final String path) {
        final BeanBinder<T> binder = new BeanBinder<T>();
        binder.setType(beanClass);
        binder.setPath(path);
        return binder;
    }

    private Map<String, DataBinder<?>> mappings = new LinkedHashMap<String, DataBinder<?>>();

    public Map<String, DataBinder<?>> getMappings() {
        return mappings;
    }

    @Override
    public T read(final Object object) {
        if (object == null) {
            return null;
        }
        final T bean = ClassHelper.instantiate(getType());
        readInto(bean, object, false);
        return bean;
    }

    @Override
    public String readAsString(final Object object) {
        if (object == null) {
            return "null";
        }
        final Map<String, String> map = new HashMap<String, String>();
        writeAsString(map, object);
        final StringBuilder sb = new StringBuilder();
        sb.append('{');
        for (final Iterator<Map.Entry<String, String>> it = map.entrySet().iterator(); it.hasNext();) {
            final Entry<String, String> entry = it.next();
            final String property = StringEscapeUtils.escapeJavaScript(entry.getKey());
            final String value = StringEscapeUtils.escapeJavaScript(entry.getValue());
            sb.append('\'').append(property).append("':'").append(value == null ? "" : value).append('\'');
            if (it.hasNext()) {
                sb.append(',');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    @Override
    public T readFromString(final Object object) {
        if (object == null) {
            return null;
        }
        final T bean = ClassHelper.instantiate(getType());
        readInto(bean, object, true);
        return bean;
    }

    public void readInto(final Object bean, Object object, final boolean fromString) {
        object = PropertyHelper.get(object, getPath());
        for (final Map.Entry<String, DataBinder<?>> entry : mappings.entrySet()) {
            final String beanProperty = entry.getKey();
            final DataBinder<?> nestedBinder = entry.getValue();
            Object resolvedObject = object;
            if (StringUtils.isEmpty(nestedBinder.getPath())) {
                resolvedObject = PropertyHelper.get(object, nestedBinder.getPath());
            }
            final Object value = fromString ? nestedBinder.readFromString(resolvedObject) : nestedBinder.read(resolvedObject);
            PropertyHelper.set(bean, beanProperty, CoercionHelper.coerce(nestedBinder.getType(), value));
        }
    }

    public void registerBinder(final String property, final DataBinder<?> binder) {
        mappings.put(property, binder);
    }

    public void setMappings(final Map<String, DataBinder<?>> mappings) {
        this.mappings = mappings;
    }

    @Override
    public void write(final Object object, final T value) {
        if (object == null) {
            return;
        }
        doWrite(object, value, false);
    }

    @Override
    public void writeAsString(final Object object, Object value) {
        if (object == null) {
            return;
        }
        if ("".equals(value)) {
            value = null;
        }
        doWrite(object, value, true);
    }

    @SuppressWarnings("unchecked")
    private void doWrite(final Object bean, final Object value, final boolean asString) {
        // Resolve the bean
        Object resolved = PropertyHelper.get(bean, getPath());
        if (resolved == null) {
            // Was null - instantiate a new one
            resolved = ClassHelper.instantiate(getType());
            PropertyHelper.set(bean, getPath(), resolved);
        }

        for (final Map.Entry<String, DataBinder<?>> entry : mappings.entrySet()) {
            final String beanProperty = entry.getKey();
            final DataBinder<Object> nestedBinder = (DataBinder<Object>) entry.getValue();
            Object current = null;
            if (asString) {
                if (nestedBinder instanceof PropertyBinder) {
                    current = nestedBinder.readAsString(value);
                } else {
                    final Object nestedValue = PropertyHelper.get(value, beanProperty);
                    Object nestedBean = PropertyHelper.get(resolved, beanProperty);
                    if (nestedBean == null) {
                        nestedBean = nestedBinder instanceof BeanBinder ? new HashMap() : ClassHelper.instantiate(nestedBinder.getType());
                    }
                    if (nestedValue != null) {
                        nestedBinder.writeAsString(resolved, nestedValue);
                    }
                    current = nestedBean;
                }
            } else {
                current = nestedBinder.read(value);
            }
            PropertyHelper.set(resolved, beanProperty, current);
        }
    }
}

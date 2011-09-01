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
package nl.strohalm.cyclos.utils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.EntityReference;
import nl.strohalm.cyclos.entities.EntityWithCustomFields;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.proxy.HibernateProxy;

/**
 * Helper class for entities
 * @author luis
 */
public class EntityHelper {

    /**
     * A method handler for toString() on entity references
     * @author luis
     */
    private static final class ToStringMethodHandler implements MethodHandler {
        private final Class<? extends Entity> entityType;
        private final Long                    id;

        private ToStringMethodHandler(final Class<? extends Entity> entityType, final Long id) {
            this.entityType = entityType;
            this.id = id;
        }

        public Object invoke(final Object object, final Method thisMethod, final Method proceed, final Object[] args) throws Throwable {
            if (thisMethod.getName() == "toString" && args.length == 0) {
                return entityType.getSimpleName() + "#" + id;
            }
            return proceed.invoke(object, args);
        }
    }

    private static Long[]                                                              EMPTY_ARRAY             = new Long[0];
    private static Map<Class<? extends Entity>, SortedMap<String, PropertyDescriptor>> cachedPropertiesByClass = new HashMap<Class<? extends Entity>, SortedMap<String, PropertyDescriptor>>();
    private static Map<Class<? extends Entity>, Class<? extends Entity>>               cachedEntityTypes       = new HashMap<Class<? extends Entity>, Class<? extends Entity>>();

    /**
     * Returns a Map keyed by the field internal name of field values as string
     */
    public static Map<String, String> getFields(final EntityWithCustomFields<?, ?> entity) {
        final Map<String, String> values = new LinkedHashMap<String, String>();
        for (final CustomFieldValue value : entity.getCustomValues()) {
            values.put(value.getField().getInternalName(), value.getValue());
        }
        return values;
    }

    /**
     * Returns the real class of the given entity. If it is a proxy, return the entity class, not the proxy class
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Entity> getRealClass(final Entity entity) {
        final Class<? extends Entity> type = entity.getClass();
        if ((entity instanceof EntityReference) || (entity instanceof HibernateProxy)) {
            return (Class<? extends Entity>) type.getSuperclass();
        }
        return type;
    }

    /**
     * Returns the real root class of the given entity. If it is a proxy, return the entity class, not the proxy class. For example, if trying
     * getRootRealClass(MemberAccountProxy), the result will be Account.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends Entity> getRealRootClass(final Entity entity) {
        Class<? extends Entity> type = getRealClass(entity);
        while (!type.getSuperclass().equals(Entity.class)) {
            type = (Class<? extends Entity>) type.getSuperclass();
        }
        return type;
    }

    /**
     * Returns a Map keyed by the custom field of custom field values
     */
    @SuppressWarnings("unchecked")
    public static <CF extends CustomField, CFV extends CustomFieldValue> Map<CF, CFV> getValuesByField(final EntityWithCustomFields<CF, CFV> entity) {
        final Map<CF, CFV> values = new LinkedHashMap<CF, CFV>();
        for (final CFV value : entity.getCustomValues()) {
            values.put((CF) value.getField(), value);
        }
        return values;
    }

    /**
     * Returns a Map with basic properties for the given entity
     */
    public static Map<String, PropertyDescriptor> propertyDescriptorsFor(final Entity entity) {
        final Class<? extends Entity> clazz = getRealClass(entity);
        SortedMap<String, PropertyDescriptor> properties = cachedPropertiesByClass.get(clazz);
        if (properties == null) {
            properties = new TreeMap<String, PropertyDescriptor>();
            final PropertyDescriptor[] propertyDescriptors = PropertyUtils.getPropertyDescriptors(clazz);
            for (final PropertyDescriptor descriptor : propertyDescriptors) {
                final String name = descriptor.getName();
                boolean ok = name.equals("id");
                if (!ok) {
                    final Method readMethod = descriptor.getReadMethod();
                    if (readMethod != null) {
                        final Class<?> declaringClass = readMethod.getDeclaringClass();
                        ok = !declaringClass.equals(Entity.class) && !declaringClass.equals(EntityWithCustomFields.class);
                    }
                }
                if (ok) {
                    properties.put(name, descriptor);
                }
            }
            properties = Collections.unmodifiableSortedMap(properties);
            cachedPropertiesByClass.put(clazz, properties);
        }
        return properties;
    }

    /**
     * Returns a collection with basic property names for the given entity
     */
    public static Collection<String> propertyNamesFor(final Entity entity) {
        return propertyDescriptorsFor(entity).keySet();
    }

    /**
     * Returns a collection with basic property names for the given entity
     */
    public static Collection<String> propertyNamesFor(final EntityWithCustomFields<?, ?> entity) {
        return propertyNamesFor((Entity) entity);
    }

    /**
     * Returns a reference to the given entity type and the give id
     */
    @SuppressWarnings("unchecked")
    public static <E extends Entity> E reference(final Class<E> entityType, final Long id) {
        if (id == null || id.longValue() <= 0L) {
            return null;
        }

        final Class<? extends Entity> proxyClass = resolveEntityClass(entityType);
        final E proxy = (E) ClassHelper.instantiate(proxyClass);
        ((ProxyObject) proxy).setHandler(new ToStringMethodHandler(entityType, id));
        proxy.setId(id);
        return proxy;
    }

    /**
     * Returns a reference array to the given entity type and the give ids
     */
    @SuppressWarnings("unchecked")
    public static <E extends Entity> E[] references(final Class<E> entityType, final List<Long> ids) {
        if (ids == null || ids.size() == 0) {
            return (E[]) Array.newInstance(entityType, 0);
        }
        final E[] entities = (E[]) Array.newInstance(entityType, ids.size());
        for (int i = 0; i < ids.size(); i++) {
            final Long id = ids.get(i);
            entities[i] = reference(entityType, id);
        }
        return entities;
    }

    /**
     * Converts entities into an array of identifiers
     */
    public static Long[] toIds(final Collection<? extends Entity> entities) {
        if (entities == null || entities.isEmpty()) {
            return EMPTY_ARRAY;
        }
        final Long[] ids = new Long[entities.size()];
        int i = 0;
        for (final Entity entity : entities) {
            ids[i++] = entity.getId();
        }
        return ids;
    }

    /**
     * Converts entities into an array of identifiers
     */
    public static Long[] toIds(final Entity... entities) {
        if (entities == null || entities.length == 0) {
            return EMPTY_ARRAY;
        }
        return toIds(Arrays.asList(entities));
    }

    /**
     * Converts entities into a JSON representation of ids
     */
    public static String toIdsAsString(final Collection<? extends Entity> entities) {
        return '[' + StringUtils.join(toIds(entities), ',') + ']';
    }

    /**
     * Converts entities into a JSON representation of ids
     */
    public static String toIdsAsString(final Entity... entities) {
        return '[' + StringUtils.join(toIds(entities), ',') + ']';
    }

    @SuppressWarnings("unchecked")
    private synchronized static Class<? extends Entity> resolveEntityClass(final Class<? extends Entity> entityType) {
        Class<? extends Entity> proxyType = cachedEntityTypes.get(entityType);
        if (proxyType == null) {
            final ProxyFactory factory = new ProxyFactory();
            factory.setInterfaces(new Class[] { EntityReference.class });
            factory.setSuperclass(entityType);
            proxyType = factory.createClass();
            cachedEntityTypes.put(entityType, proxyType);
        }
        return proxyType;
    }

}

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.struts.CyclosMessageResources;
import nl.strohalm.cyclos.utils.conversion.BooleanConverter;
import nl.strohalm.cyclos.utils.conversion.MessageConverter;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.struts.util.MessageResources;

/**
 * Helper class for resource bundle access
 * @author luis
 */
public final class MessageHelper {

    /**
     * Retrieve a message for the first error on the validation exception
     */
    public static String firstErrorMessage(final HttpServletRequest request, final ValidationException e) {
        return firstErrorMessage(request.getSession().getServletContext(), e);
    }

    /**
     * Retrieve a message for the first error on the validation exception
     */
    public static String firstErrorMessage(final ServletContext context, final ValidationException e) {
        if (e == null || !e.hasErrors()) {
            return "";
        }
        final Collection<ValidationError> generalErrors = e.getGeneralErrors();
        ValidationError error;
        Object[] args;
        if (generalErrors != null && !generalErrors.isEmpty()) {
            error = generalErrors.iterator().next();
            args = error.getArguments().toArray();
        } else {
            final Map<String, Collection<ValidationError>> errorsByProperty = e.getErrorsByProperty();
            final Entry<String, Collection<ValidationError>> entry = errorsByProperty.entrySet().iterator().next();
            final String property = entry.getKey();
            final String propertyKey = e.getPropertyKey(property);
            error = entry.getValue().iterator().next();
            final List<Object> arguments = new ArrayList<Object>();
            arguments.add(StringUtils.isEmpty(propertyKey) ? property : message(context, propertyKey));
            if (error.getArguments() != null && !error.getArguments().isEmpty()) {
                arguments.addAll(error.getArguments());
            }
            args = arguments.toArray();
        }
        return message(context, error.getKey(), args);
    }

    public static BooleanConverter getBooleanConverter(final ServletContext context) {
        final MessageResolver messageResolver = SpringHelper.bean(context, "messageResolver");
        return new BooleanConverter(messageResolver.message("global.yes"), messageResolver.message("global.no"));
    }

    public static MessageConverter getMessageConverter(final ServletContext context, final String prefix) {
        final MessageResolver messageResolver = SpringHelper.bean(context, "messageResolver");
        return new MessageConverter(messageResolver, prefix);
    }

    /**
     * Retrieve a message from the request
     */
    public static String message(final HttpServletRequest request, final String key, final List<Object> args) {
        return message(request, key, CollectionUtils.isEmpty(args) ? null : args.toArray());
    }

    /**
     * Retrieve a message from the request
     */
    public static String message(final HttpServletRequest request, final String key, final Object... args) {
        return message(request.getSession().getServletContext(), key, args);
    }

    /**
     * Retrieve a message from the servlet context
     */
    public static String message(final ServletContext context, final String key, final List<Object> args) {
        return message(context, key, CollectionUtils.isEmpty(args) ? null : args.toArray());
    }

    /**
     * Retrieve a message from the servlet context
     */
    public static String message(final ServletContext context, final String key, final Object... args) {
        try {
            final MessageResources resources = CyclosMessageResources.retrieve(context);
            return resources.getMessage(key, args);
        } catch (final Exception e) {
            return "???" + key + "???";
        }
    }
}

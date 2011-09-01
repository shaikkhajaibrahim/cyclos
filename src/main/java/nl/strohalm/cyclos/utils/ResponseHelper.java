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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.utils.validation.ValidationError;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * Helper class for working with the response object
 * @author luis
 */
public abstract class ResponseHelper {

    public static enum Status {
        SUCCESS, ERROR;
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    /**
     * Set the specified response as a file download (attachment)
     */
    public static void setDownload(final HttpServletResponse response, final String fileName) {
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\";");
        // In order for IE to find the download, we must remove the cache headers >:-(
        response.setHeader("Pragma", "Public");
        response.setHeader("Cache-Control", "");
    }

    /**
     * Set the correct character encoding (according to the settings)
     */
    public static void setEncoding(final HttpServletRequest request, final HttpServletResponse response) {
        final LocalSettings localSettings = SettingsHelper.getLocalSettings(request);
        response.setCharacterEncoding(localSettings.getCharset());
    }

    /**
     * Set the specified response to open inline (no attachment)
     */
    public static void setInline(final HttpServletResponse response) {
        response.setHeader("Content-Disposition", "inline");
    }

    /**
     * Set the response header to use no cache
     */
    public static void setNoCache(final HttpServletResponse response) {
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store,max-age=0");
        response.setDateHeader("Expires", 1);
    }

    /**
     * Set the response as text/plain, with the correct character encoding (according to the settings) and no cache
     */
    public static void setTextNoCache(final HttpServletRequest request, final HttpServletResponse response) {
        response.setContentType("text/plain");
        setEncoding(request, response);
        setNoCache(response);
    }

    /**
     * Set the response as text/xml, with the correct character encoding (according to the settings) and no cache
     */
    public static void setXmlNoCache(final HttpServletRequest request, final HttpServletResponse response) {
        response.setContentType("text/xml");
        setEncoding(request, response);
        setNoCache(response);
    }

    /**
     * Writes a file contents to the response
     */
    public static void writeFile(final HttpServletResponse response, final File file) throws IOException {
        if (file.exists()) {
            response.setContentLength((int) file.length());
            response.setDateHeader("Last-Modified", file.lastModified());
            final InputStream in = new FileInputStream(file);
            try {
                IOUtils.copy(in, response.getOutputStream());
            } finally {
                IOUtils.closeQuietly(in);
            }
        }
    }

    /**
     * Write a JSON response
     */
    public static void writeJSON(final HttpServletResponse response, final String json) {
        try {
            response.getWriter().print("[" + json + "]");
        } catch (final Exception e) {
            throw new IllegalStateException("Error writing JSON string", e);
        }
    }

    /**
     * Write the status to the response as XML
     */
    public static void writeStatus(final HttpServletRequest request, final HttpServletResponse response, final Object status, final Map<String, ?> fields) {

        final LocalSettings localSettings = SettingsHelper.getLocalSettings(request);
        final StringBuilder sb = new StringBuilder();
        sb.append("<?xml version='1.0' encoding='").append(localSettings.getCharset()).append("'?>");
        sb.append("<status value='").append(status).append("'>");
        if (fields != null && !fields.isEmpty()) {
            for (final Map.Entry<String, ?> entry : fields.entrySet()) {
                final String tag = entry.getKey();
                sb.append('<').append(tag).append("><![CDATA[");
                sb.append(entry.getValue());
                sb.append("]]></").append(tag).append('>');
            }
        }
        sb.append("</status>");

        ResponseHelper.setXmlNoCache(request, response);

        try {
            response.getWriter().print(sb.toString());
        } catch (final IOException e1) {
            throw new IllegalStateException("Error writing status xml: " + e1);
        }
    }

    /**
     * Write the validation errors if there are any. When no errors are found, the response body will be empty.
     */
    public static void writeValidationErrors(final HttpServletRequest request, final HttpServletResponse response, final ValidationException e) {
        final Map<String, Object> fields = new LinkedHashMap<String, Object>();

        final StringBuilder sb = new StringBuilder();
        if (e.hasErrors()) {
            for (final ValidationError error : e.getGeneralErrors()) {
                sb.append(MessageHelper.message(request, error.getKey(), error.getArguments())).append('\n');
            }
            for (final Map.Entry<String, Collection<ValidationError>> entry : e.getErrorsByProperty().entrySet()) {
                final String property = entry.getKey();
                final String key = e.getPropertyKey(property);
                final String displayName = e.getPropertyDisplayName(property);
                String propertyMessage = property;
                if (key != null) {
                    propertyMessage = MessageHelper.message(request, key);
                } else if (displayName != null) {
                    propertyMessage = displayName;
                }

                for (final ValidationError error : entry.getValue()) {
                    final List<Object> args = new ArrayList<Object>();
                    args.add(propertyMessage);
                    if (error.getArguments() != null) {
                        args.addAll(error.getArguments());
                    }
                    sb.append(MessageHelper.message(request, error.getKey(), args.toArray())).append('\n');
                }
            }
        } else {
            sb.append(MessageHelper.message(request, "error.validation"));
        }
        fields.put("message", sb.toString());
        fields.put("properties", StringUtils.join(e.getErrorsByProperty().keySet().iterator(), ','));
        writeStatus(request, response, Status.ERROR, fields);
    }

    /**
     * Used when no validation error occured
     */
    public static void writeValidationSuccess(final HttpServletRequest request, final HttpServletResponse response) {
        writeStatus(request, response, Status.SUCCESS, null);
    }
}
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
package nl.strohalm.cyclos.controls;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.utils.CacheCleaner;
import nl.strohalm.cyclos.utils.ResponseHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.csv.CSVWriter;

import org.apache.struts.action.ActionServlet;

/**
 * Base class for CSV export actions
 * @author luis
 */
public abstract class BaseCsvAction extends BaseAjaxAction implements LocalSettingsChangeListener {

    private CSVWriter<?>  cachedCsvWriter = null;
    private LocalSettings localSettings;

    public CSVWriter<?> getCsvWriter() {
        return cachedCsvWriter;
    }

    public LocalSettings getLocalSettings() {
        return localSettings;
    }

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        localSettings = event.getSource();
        cachedCsvWriter = null;
    }

    @Override
    public void setServlet(final ActionServlet servlet) {
        super.setServlet(servlet);
        if (servlet != null) {
            localSettings = SettingsHelper.getLocalSettings(servlet.getServletContext());
        }
    }

    @Override
    protected ContentType contentType() {
        return ContentType.CSV;
    }

    protected abstract List<?> executeQuery(ActionContext context);

    protected abstract String fileName(ActionContext context);

    @Override
    protected void handleException(final HttpServletRequest request, final HttpServletResponse response, final Exception e) throws Exception {
        throw e;
    }

    protected abstract CSVWriter<?> initCsvWriter(ActionContext context);

    @Override
    @SuppressWarnings("unchecked")
    protected final void renderContent(final ActionContext context) throws Exception {
        final CSVWriter csvWriter = resolveCsvWriter(context);
        final HttpServletResponse response = context.getResponse();
        try {
            ResponseHelper.setDownload(response, fileName(context));
            final PrintWriter out = response.getWriter();
            csvWriter.writeHeader(out);

            final CacheCleaner cacheCleaner = new CacheCleaner(getFetchService());
            int remaining = localSettings.getMaxIteratorResults() == 0 ? -1 : localSettings.getMaxIteratorResults();
            for (final Object row : executeQuery(context)) {
                if ((remaining-- == 0) && shouldLimit()) {
                    out.print(context.message("reports.print.limitation", localSettings.getMaxIteratorResults()));
                    break;
                }
                csvWriter.writeRow(row, out);
                cacheCleaner.clearCache();
            }
        } catch (final Exception e) {
            // Restore the response headers to open on the browser
            try {
                response.resetBuffer();
                response.setContentType("text/html");
                ResponseHelper.setInline(response);
                throw e;
            } catch (final IllegalStateException e1) {
                e.printStackTrace();
            }
        }
    }

    protected boolean shouldLimit() {
        return true;
    }

    /**
     * Should be overriden to return true if the CsvWriter cannot be safely shared among requests
     */
    protected boolean useCsvWriterPerRequest() {
        return false;
    }

    /**
     * Returns the cached CsvWriter if is safe to use among different requests, or a new one on each call
     */
    private CSVWriter<?> resolveCsvWriter(final ActionContext context) {
        CSVWriter<?> csvWriter;
        if (useCsvWriterPerRequest()) {
            csvWriter = initCsvWriter(context);
        } else {
            if (cachedCsvWriter == null) {
                cachedCsvWriter = initCsvWriter(context);
            }
            csvWriter = cachedCsvWriter;
        }
        return csvWriter;
    }
}

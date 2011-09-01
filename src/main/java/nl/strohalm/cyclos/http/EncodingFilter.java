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
package nl.strohalm.cyclos.http;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsEvent;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.SettingsHelper;

import org.apache.commons.lang.StringUtils;

/**
 * Filter used to apply the correct character encoding
 * @author luis
 */
public class EncodingFilter extends OncePerRequestFilter implements LocalSettingsChangeListener {

    private ServletContext  context;
    private String          encoding;
    private SettingsService settingsService;

    @Override
    public void destroy() {
    }

    public SettingsService getSettingsService() {
        return settingsService;
    }

    @Override
    public void init(final FilterConfig config) throws ServletException {
        super.init(config);

        context = config.getServletContext();
        settingsService.addListener(this);
        update(SettingsHelper.getLocalSettings(context));
    }

    public void onLocalSettingsUpdate(final LocalSettingsEvent event) {
        update(event.getSource());
    }

    @Inject
    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Override
    protected void execute(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
        try {
            final String requestEncoding = request.getCharacterEncoding();
            if (StringUtils.isEmpty(requestEncoding)) {
                request.setCharacterEncoding(encoding);
            }
            response.setCharacterEncoding(encoding);
        } catch (final Exception e) {
        }
        chain.doFilter(request, response);
    }

    private void update(final LocalSettings localSettings) {
        if (localSettings == null) {
            encoding = "ISO-8859-1";
        } else {
            encoding = localSettings.getCharset();
        }
    }
}

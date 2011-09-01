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
package nl.strohalm.cyclos.struts;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.settings.events.LocalSettingsChangeListener;
import nl.strohalm.cyclos.services.settings.SettingsService;
import nl.strohalm.cyclos.utils.CurrentInvocationData;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.SpringHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.action.SecureTilesRequestProcessor;
import org.apache.struts.config.ForwardConfig;
import org.apache.struts.config.ModuleConfig;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Custom request processor
 * @author luis
 */
public class CyclosRequestProcessor extends SecureTilesRequestProcessor {

    private static final Log    LOG                      = LogFactory.getLog(CyclosRequestProcessor.class);

    private SettingsService     settingsService;

    private static final Object syncActionInitialization = new Object();

    public SettingsService getSettingsService() {
        return settingsService;
    }

    @Override
    public void init(final ActionServlet servlet, final ModuleConfig moduleConfig) throws ServletException {
        super.init(servlet, moduleConfig);
        SpringHelper.injectBeans(servlet.getServletContext(), this);

        final CyclosControllerConfig config = (CyclosControllerConfig) moduleConfig.getControllerConfig();
        settingsService.addListener(config);
        config.initialize(SettingsHelper.getLocalSettings(servlet.getServletContext()));
    }

    @Inject
    public void setSettingsService(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Override action creation to inject spring beans
     */
    @Override
    protected Action processActionCreate(final HttpServletRequest request, final HttpServletResponse response, final ActionMapping actionMapping) throws IOException {
        synchronized (syncActionInitialization) {
            Action action = (Action) actions.get(actionMapping.getType());
            if (action != null) {
                return action;
            } else {
                action = super.processActionCreate(request, response, actionMapping);

                if (action == null) {
                    return null;
                }

                // Register the action as listener
                if (action instanceof LocalSettingsChangeListener) {
                    settingsService.addListener((LocalSettingsChangeListener) action);
                }

                // Inject the required beans
                try {
                    SpringHelper.injectBeans(WebApplicationContextUtils.getWebApplicationContext(getServletContext()), action);
                } catch (final Exception e) {
                    LOG.error("Error injecting beans on " + action, e);
                }

                return action;
            }
        }
    }

    /**
     * Override form creation to remove the form from session if the request was triggered by the menu
     */
    @Override
    @SuppressWarnings("unchecked")
    protected ActionForm processActionForm(final HttpServletRequest request, final HttpServletResponse response, final ActionMapping actionMapping) {
        final HttpSession session = request.getSession();
        if (StringUtils.isEmpty(actionMapping.getName())) {
            return null;
        }
        if (RequestHelper.isFromMenu(request)) {
            session.removeAttribute(actionMapping.getName());
        }
        // Add form to session
        final ActionForm form = super.processActionForm(request, response, actionMapping);
        if ("session".equals(actionMapping.getScope())) {
            Set<String> sessionForms = (Set<String>) session.getAttribute("sessionForms");
            if (sessionForms == null) {
                sessionForms = new HashSet<String>();
                session.setAttribute("sessionForms", sessionForms);
            }
            sessionForms.add(actionMapping.getName());
        }
        return form;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected ActionForward processActionPerform(final HttpServletRequest request, final HttpServletResponse response, final Action action, final ActionForm form, final ActionMapping mapping) throws IOException, ServletException {
        // Clean previous session stored forms
        if (RequestHelper.isFromMenu(request)) {
            final HttpSession session = request.getSession();
            final Set<String> sessionForms = (Set<String>) session.getAttribute("sessionForms");
            if (sessionForms != null) {
                for (final String name : sessionForms) {
                    final ActionForm currentForm = (ActionForm) session.getAttribute(name);
                    if (currentForm != form) {
                        session.removeAttribute(name);
                    }
                }
            }
        }
        try {
            CurrentInvocationData.setSystemRequest(false);
            return super.processActionPerform(request, response, action, form, mapping);
        } finally {
            CurrentInvocationData.cleanup();
        }
    }

    @Override
    protected void processForwardConfig(final HttpServletRequest request, final HttpServletResponse response, final ForwardConfig forward) throws IOException, ServletException {
        try {
            super.processForwardConfig(request, response, forward);
        } catch (final IllegalStateException e) {
            LOG.warn("Error processing the forward to " + forward.getPath());
        }
    }

    @Override
    protected void processPopulate(final HttpServletRequest request, final HttpServletResponse response, final ActionForm form, final ActionMapping mapping) throws ServletException {
        try {
            super.processPopulate(request, response, form, mapping);
        } catch (final Exception e) {
            LOG.error("Error populating " + form + " in " + mapping.getPath(), e);
            request.getSession().setAttribute("errorKey", "error.validation");
            final RequestDispatcher rd = request.getRequestDispatcher("/do/error");
            try {
                rd.forward(request, response);
            } catch (final IOException e1) {
                LOG.error("Error while trying to forward to error page", e1);
            }
        }
    }
}

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.dao.exceptions.QueryParseException;
import nl.strohalm.cyclos.entities.settings.LocalSettings;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.fetch.FetchService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.ImageHelper;
import nl.strohalm.cyclos.utils.SettingsHelper;
import nl.strohalm.cyclos.utils.ImageHelper.ImageType;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.upload.MultipartRequestHandler;

/**
 * Base action where there is no logged user expected
 * @author luis
 */
public abstract class BasePublicAction extends Action {

    private final Log    LOG = LogFactory.getLog(BasePublicAction.class);
    private FetchService fetchService;

    /**
     * The Struts standard execute method is reserved, being the executeAction the one that subclasses must implement
     */
    @Override
    public final ActionForward execute(final ActionMapping mapping, final ActionForm actionForm, final HttpServletRequest request, final HttpServletResponse response) throws Exception {

        // Check for uploads that exceeded the max length
        final Boolean maxLengthExceeded = (Boolean) request.getAttribute(MultipartRequestHandler.ATTRIBUTE_MAX_LENGTH_EXCEEDED);
        if (maxLengthExceeded != null && maxLengthExceeded) {
            final LocalSettings settings = SettingsHelper.getLocalSettings(request);
            return ActionHelper.sendError(mapping, request, response, "error.maxUploadSizeExceeded", FileUtils.byteCountToDisplaySize(settings.getMaxUploadBytes()));
        }

        // Create an action context
        request.setAttribute("formAction", mapping.getPath());

        try {
            // Process the action
            return executeAction(mapping, actionForm, request, response);
        } catch (final PermissionDeniedException e) {
            return ActionHelper.sendError(mapping, request, response, "error.permissionDenied");
        } catch (final QueryParseException e) {
            return ActionHelper.sendError(mapping, request, response, "error.queryParse");
        } catch (final ImageHelper.UnknownImageTypeException e) {
            final String recognizedTypes = StringUtils.join(ImageType.values(), ", ");
            return ActionHelper.sendError(mapping, request, response, "error.unknownImageType", recognizedTypes);
        } catch (final ValidationException e) {
            return ActionHelper.handleValidationException(mapping, request, response, e);
        } catch (final Exception e) {
            ActionHelper.generateLog(request, getServlet().getServletContext(), e);
            LOG.error("Application error on " + getClass().getName(), e);
            return ActionHelper.sendError(mapping, request, response, null);
        }
    }

    public final FetchService getFetchService() {
        return fetchService;
    }

    @Inject
    public final void setFetchService(final FetchService fetchService) {
        this.fetchService = fetchService;
    }

    /**
     * This method must be implemented to perform the action itself
     */
    protected abstract ActionForward executeAction(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws Exception;
}

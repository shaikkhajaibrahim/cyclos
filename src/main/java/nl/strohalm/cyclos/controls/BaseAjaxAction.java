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

import nl.strohalm.cyclos.entities.access.User;
import nl.strohalm.cyclos.exceptions.LoggedOutException;
import nl.strohalm.cyclos.utils.ResponseHelper;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Abstract action to AJAX responses
 * @author luis
 */
public abstract class BaseAjaxAction extends BaseAction {

    /**
     * An enum with possible content types
     * @author luis
     */
    public static enum ContentType {
        CSV("application/msexcel"), JSON("text/json"), TEXT("text/plain"), HTML("text/html"), XML("text/xml");
        private final String contentType;

        private ContentType(final String contentType) {
            this.contentType = contentType;
        }

        public String getContentType() {
            return contentType;
        }

        public void processResponse(final HttpServletResponse response) {
            response.setContentType(contentType);
        }
    }

    /**
     * Return the response content type
     */
    protected abstract ContentType contentType();

    @Override
    protected final ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        // Prepare the response headers
        final ContentType contentType = contentType();
        contentType.processResponse(response);
        ResponseHelper.setEncoding(request, response);
        ResponseHelper.setNoCache(response);

        // Render the response
        try {
            renderContent(context);
            response.flushBuffer();
        } catch (final Exception e) {
            e.printStackTrace();
            handleException(request, response, e);
        }

        // The response is now complete - return null
        return null;
    }

    // Send the error to response
    protected void handleException(final HttpServletRequest request, final HttpServletResponse response, final Exception e) throws Exception {
        response.sendError(500, StringUtils.trimToEmpty(e.getMessage()));
    }

    /**
     * Render the AJAX result
     */
    protected abstract void renderContent(ActionContext context) throws Exception;

    /**
     * Ajax actions are never stored on navigation path
     */
    @Override
    protected boolean storePath(final ActionMapping actionMapping, final HttpServletRequest request) {
        return false;
    }

    @Override
    protected User validate(final HttpServletRequest request, final HttpServletResponse response, final ActionMapping actionMapping) throws Exception {
        try {
            return super.validate(request, response, actionMapping);
        } catch (final LoggedOutException e) {
            handleException(request, response, e);
            request.getSession().invalidate();
            return null;
        }
    }
}

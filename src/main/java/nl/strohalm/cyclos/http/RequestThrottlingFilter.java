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
import java.util.Properties;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.logging.LoggingHandler;

/**
 * This filter ensures a maximum number of concurrent requests are executed, so that the system is not overloaded
 * 
 * @author luis
 */
public class RequestThrottlingFilter extends OncePerRequestFilter {

    private static final String SOAP_FAULT = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><soap:Fault><faultcode xmlns:ns1=\"cyclos\">ns1:system-overloaded</faultcode><faultstring>System overloaded, retry in a few minutes.</faultstring></soap:Fault></soap:Body></soap:Envelope>";

    private Semaphore           semaphore;
    private int                 maxWaitSeconds;
    private int                 maxWaitSecondsWebServices;
    private LoggingHandler      loggingHandler;

    @Inject
    public void setCyclosProperties(final Properties properties) {
        int maxRequests;
        try {
            maxRequests = Integer.parseInt(properties.getProperty("cyclos.maxRequests"));
        } catch (final NumberFormatException e) {
            System.out.println("Invalid configuration property: cyclos.maxRequests. Assuming defaults");
            maxRequests = 100;
        }
        semaphore = new Semaphore(maxRequests, true);
        try {
            maxWaitSeconds = Integer.parseInt(properties.getProperty("cyclos.maxWaitSeconds"));
        } catch (final NumberFormatException e) {
            System.out.println("Invalid configuration property: cyclos.maxWaitSeconds. Assuming defaults");
            maxWaitSeconds = 100;
        }
        try {
            maxWaitSecondsWebServices = Integer.parseInt(properties.getProperty("cyclos.maxWaitSecondsWebServices"));
        } catch (final NumberFormatException e) {
            System.out.println("Invalid configuration property: cyclos.maxWaitSecondsWebServices. Assuming defaults");
            maxWaitSecondsWebServices = 100;
        }
    }

    @Inject
    public void setLoggingHandler(final LoggingHandler loggingHandler) {
        this.loggingHandler = loggingHandler;
    }

    @Override
    protected void execute(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain) throws IOException, ServletException {
        try {
            final boolean isWebService = RequestHelper.isWebService(request);
            if (!semaphore.tryAcquire(isWebService ? maxWaitSecondsWebServices : maxWaitSeconds, TimeUnit.SECONDS)) {
                // Could not acquire a permit. The system is overloaded
                loggingHandler.logRequestRejectedOnSystemOverloaded(request.getRequestURI(), request.getRemoteAddr());
                if (isWebService) {
                    // A web service request should be replied with a fault code SOAP xml
                    response.getWriter().write(SOAP_FAULT);
                } else {
                    // A regular request is redirected to the error page
                    request.getSession().setAttribute("errorKey", "error.systemOverloaded");
                    response.sendRedirect(request.getContextPath() + "/do/error");
                }
            } else {
                try {
                    // Proceed the filter chain
                    chain.doFilter(request, response);
                } finally {
                    semaphore.release();
                }
            }
        } catch (final InterruptedException e) {
            // The thread was interrupted while waiting for a permit. Just return
        }
    }
}

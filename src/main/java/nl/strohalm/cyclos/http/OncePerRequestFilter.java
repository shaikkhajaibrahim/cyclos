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

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.strohalm.cyclos.utils.SpringHelper;

/**
 * A filter which is guaranteed to execute only once per request. Subclasses should implement the execute method
 * @author luis
 */
public abstract class OncePerRequestFilter implements Filter {

    private static long    counter;
    private String         requestKey;
    protected FilterConfig config;

    public void destroy() {
    }

    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        // Check if the filter was already executed
        if (request.getAttribute(requestKey) != null) {
            // Already executed: just proceed the chain
            chain.doFilter(request, response);
        } else {
            // Not yet: set the control attribute and invoke the execute() method
            request.setAttribute(requestKey, Boolean.TRUE);
            execute(request, response, chain);
        }
    }

    public void init(final FilterConfig config) throws ServletException {
        this.config = config;
        final ServletContext servletContext = config.getServletContext();
        SpringHelper.injectBeans(servletContext, this);
        requestKey = "alreadyExecuted" + (counter++);
    }

    protected abstract void execute(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException;
}

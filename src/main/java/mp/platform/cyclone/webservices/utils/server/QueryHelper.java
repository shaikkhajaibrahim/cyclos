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
package mp.platform.cyclone.webservices.utils.server;

import nl.strohalm.cyclos.utils.query.PageParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import mp.platform.cyclone.webservices.utils.SearchParameters;

/**
 * Helper class for queries
 * @author luis
 */
public class QueryHelper {

    private static final int MAX_PAGE_SIZE = 100;

    public void fill(final SearchParameters searchParameters, final QueryParameters query) {
        if (query != null) {
            query.setResultType(ResultType.PAGE);
            // Ensure the page size is not greater than the maximum allowed
            int pageSize = searchParameters.getPageSize();
            if (pageSize > MAX_PAGE_SIZE) {
                pageSize = MAX_PAGE_SIZE;
            }
            query.setPageParameters(new PageParameters(pageSize, searchParameters.getCurrentPage()));
        }
    }

}

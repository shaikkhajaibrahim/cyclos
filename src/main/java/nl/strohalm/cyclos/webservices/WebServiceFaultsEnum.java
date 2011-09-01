/*
    This file is part of Cyclos <http://project.cyclos.org>

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.webservices;

import nl.strohalm.cyclos.webservices.payments.WebServiceFault;
import nl.strohalm.cyclos.webservices.utils.WebServiceHelper;

/**
 * All faults throws by the web services operations
 * @author ameyer
 */
public enum WebServiceFaultsEnum implements WebServiceFault {

    APPLICATION_OFFLINE("application-offline"),

    SECURE_ACCESS_REQUIRED("secure-access-required"),

    UNAUTHORIZED_ACCESS("unauthorized-access"),

    INVALID_PARAMETERS("invalid-parameter"),

    QUERY_PARSE_ERROR("query-parse-error"),

    UNEXPECTED_ERROR("unexpected-error"),

    INVALID_CREDENTIALS("invalid-credentials"),

    BLOCKED_CREDENTIALS("blocked-credentials"),

    INVALID_CHANNEL("invalid-channel"),

    MEMBER_NOT_FOUND("member-not-found"),

    CURRENTLY_UNAVAILABLE("currently-unavailable"),

    INACTIVE_POS("inactive-pos");

    protected String code;

    private WebServiceFaultsEnum(final String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Throw a SoapFault with this fault code
     */
    public void throwFault() {
        throw WebServiceHelper.fault(this);
    }

    /**
     * Throw a SoapFault with this fault code and the specified message as the cause
     */
    public void throwFault(final String serverDetailsMessage) {
        throw WebServiceHelper.fault(this, new Exception(serverDetailsMessage));
    }

    /**
     * Throw a SoapFault with this fault code and the specified Throwable as the cause
     */
    public void throwFault(final Throwable cause) {
        throw WebServiceHelper.fault(this, cause);
    }
}

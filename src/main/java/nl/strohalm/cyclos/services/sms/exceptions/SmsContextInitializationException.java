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
package nl.strohalm.cyclos.services.sms.exceptions;

import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.exceptions.ApplicationException;

public class SmsContextInitializationException extends ApplicationException {

    private static final long serialVersionUID = 1L;

    private String            smsContextClassName;
    private MemberGroup       group;

    public SmsContextInitializationException(final MemberGroup group, final String smsContextClassName, final String message) {
        this(group, smsContextClassName, message, null);
    }

    public SmsContextInitializationException(final MemberGroup group, final String smsContextClassName, final String message, final Throwable cause) {
        super(message, cause);
        this.smsContextClassName = smsContextClassName;
        this.group = group;
    }

    @Override
    public String getMessage() {
        return "Group: " + group.getName() + ". Context impl: " + smsContextClassName + ". Error: " + super.getMessage();
    }

    public String getSmsContextClassName() {
        return smsContextClassName;
    }
}

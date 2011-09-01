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
package nl.strohalm.cyclos.entities.alerts;

import nl.strohalm.cyclos.utils.EnumHelper;
import nl.strohalm.cyclos.utils.StringValuedEnum;

/**
 * An alert sent to system
 * @author luis
 */
public class SystemAlert extends Alert {
    /**
     * Contains the possible system alerts
     * @author luis
     */
    public static enum Alerts implements StringValuedEnum, AlertType {
        /**
         * Alert when the application has been restarted, or started for the first time. No arguments
         */
        APPLICATION_RESTARTED,

        /**
         * Alert when the application has being shutdown. No arguments
         */
        APPLICATION_SHUTDOWN,

        /**
         * Alert when a null rate on a system account was encountered. Arguments: 0: the null db field; 1: system account name.
         */
        NULL_RATE,

        /**
         * Alert when an account fee has started running. Arguments: 0: The account fee name
         */
        ACCOUNT_FEE_RUNNING,

        /**
         * Alert when an account fee has been manually cancelled. Arguments: 0: The account fee name
         */
        ACCOUNT_FEE_CANCELLED,

        /**
         * Alert when an account fee has failed. Arguments: 0: The account fee name
         */
        ACCOUNT_FEE_FAILED,

        /**
         * Alert when an account fee recovery log was created. Arguments: 0: The account fee name
         */
        ACCOUNT_FEE_RECOVERED,

        /**
         * Alert when an account fee has successfully finished. Arguments: 0: The account fee name
         */
        ACCOUNT_FEE_FINISHED,

        /**
         * Alert someone tries for a given number of tries (alertSettings.amountIncorrectLogin) to login with an invalid username Arguments: 0: The
         * number of missed times 1: The IP that generated the request
         */
        MAX_INCORRECT_LOGIN_ATTEMPTS,

        /**
         * Alert when an administrator login is temporarily blocked by reaching the maximum login attempts. Arguments: 0: The administrator username
         * 1: The number of tries 2: The IP that generated the request
         */
        ADMIN_LOGIN_BLOCKED_BY_TRIES,

        /**
         * Alert when an administrator had it's login temporarily blocked by too many permission denied exceptions. Arguments: 0: The number of
         * permission denied exceptions 1: The IP address that sent the request
         */
        ADMIN_LOGIN_BLOCKED_BY_PERMISSION_DENIEDS,

        /**
         * Alert when an administrator transaction password is blocked by reaching the maximum attempts. Arguments: 0: The administrator username 1:
         * The number of tries 2: The IP that generated the request
         */
        ADMIN_TRANSACTION_PASSWORD_BLOCKED_BY_TRIES,

        /**
         * Alert when there is a new original version of an application page that was customized Arguments: 0: the relative path (from /pages) of the
         * application page
         */
        NEW_VERSION_OF_APPLICATION_PAGE,

        /**
         * Alert when there is a new original version of a static file that was customized Arguments: 0: the name of the static file
         */
        NEW_VERSION_OF_STATIC_FILE,

        /**
         * Alert when there is a new original version of a help file that was customized Arguments: 0: the name of the help file
         */
        NEW_VERSION_OF_HELP_FILE,

        /**
         * Alert when an index rebuild has started. Arguments: 0: The index type
         */
        INDEX_REBUILD_START,

        /**
         * Alert when an index rebuild has ended. Arguments: 0: The index type
         */
        INDEX_REBUILD_END,

        /**
         * Alert when an index has been optimized. Arguments: 0: The index type
         */
        INDEX_OPTIMIZED,

        /**
         * The account status processing has failed. Arguments: 0: the payment date, 1: from account owner, 2: to account owner, 3: the payment amount
         */
        ERROR_PROCESSING_ACCOUNT_STATUS,

        ;

        private final String name;

        private Alerts() {
            name = "alert.system." + EnumHelper.capitalizeName(this);
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return name;
        }
    }

    private static final long serialVersionUID = -4680889167594248176L;

}
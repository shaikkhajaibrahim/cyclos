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
package nl.strohalm.cyclos;

import java.io.IOException;
import java.util.Properties;

/**
 * Returns the cyclos properties
 * 
 * @author luis
 */
public class CyclosConfiguration {

    public static final String CYCLOS_PROPERTIES_FILE = "/cyclos.properties";

    public static Properties getCyclosProperties() throws IOException {
        final Properties properties = new Properties();
        properties.put("cyclos.maxRequests", "50");
        properties.put("cyclos.maxWaitSeconds", "30");
        properties.put("cyclos.maxWaitSecondsWebServices", "10");
        properties.put("cyclos.maxMailSenderThreads", "5");
        properties.put("cyclos.maxSmsSenderThreads", "50");
        properties.put("cyclos.maxPaymentRequestSenderThreads", "50");
        properties.put("cyclos.forceKeywordsOnMemberSearch", "false");
        properties.put("cyclos.disableOrderOnMemberSearch", "false");
        properties.put("cyclos.disableOrderOnAdSearch", "false");
        properties.load(CyclosConfiguration.class.getResourceAsStream(CYCLOS_PROPERTIES_FILE));
        return properties;
    }
}
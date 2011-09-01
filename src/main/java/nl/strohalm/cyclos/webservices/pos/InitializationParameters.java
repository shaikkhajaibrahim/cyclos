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
package nl.strohalm.cyclos.webservices.pos;

/**
 * This class encapsulates all the parameters of the getInitializationData WS operation.<br>
 * We don't use the PosPinParameters class as the container parameter object<br>
 * because we need differentiate these classes in the {@link nl.strohalm.cyclos.webservices.PosInterceptor} to avoid id and pin controls.
 * @author ameyer
 */
public class InitializationParameters extends PosPinParameters {

}

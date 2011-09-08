/*
 *
 *    This file is part of Cyclos.
 *
 *    Cyclos is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    Cyclos is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Cyclos; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *
 */

package nl.strohalm.cyclos.controls.tokens;

import nl.strohalm.cyclos.controls.BaseBindingForm;

import java.util.Map;

public class BaseTokenForm extends BaseBindingForm {

    public Map<String, Object> getToken() {
        return values;
    }

    public Object getToken(final String key) {
        return values.get(key);
    }


    public void setToken(final Map<String, Object> map) {
        values = map;
    }

    public void setToken(final String key, final Object value) {
        values.put(key, value);
    }


}

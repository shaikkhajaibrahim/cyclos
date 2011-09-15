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
package mp.platform.cyclone.webservices.ads;

import nl.strohalm.cyclos.utils.ObjectHelper;

/**
 * Parameters for normal ad search via web services
 * @author luis
 */
public class AdSearchParameters extends AbstractAdSearchParameters {
    private static final long serialVersionUID = -4673987206616189023L;
    private Boolean           randomOrder;

    public boolean isRandomOrder() {
        return ObjectHelper.valueOf(randomOrder);
    }

    public void setRandomOrder(final boolean randomOrder) {
        this.randomOrder = randomOrder;
    }

    @Override
    public String toString() {
        return "AdSearchParameters [randomOrder=" + randomOrder + ", " + super.toString() + "]";
    }
}
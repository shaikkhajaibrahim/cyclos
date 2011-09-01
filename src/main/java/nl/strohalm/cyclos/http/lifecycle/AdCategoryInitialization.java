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
package nl.strohalm.cyclos.http.lifecycle;

import java.util.List;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.ads.AdCategory;
import nl.strohalm.cyclos.services.ads.AdCategoryService;
import nl.strohalm.cyclos.utils.conversion.CoercionHelper;

/**
 * Initialization for ad categories
 * @author luis
 */
public class AdCategoryInitialization implements ContextInitialization {

    private AdCategoryService adCategoryService;

    public void init(final ServletContext context) {
        boolean allZeros = true;
        final List<AdCategory> root = adCategoryService.listRoot();
        for (final AdCategory adCategory : root) {
            final int offer = CoercionHelper.coerce(Integer.TYPE, adCategory.getCountOffer());
            final int search = CoercionHelper.coerce(Integer.TYPE, adCategory.getCountSearch());
            if (offer > 0 || search > 0) {
                allZeros = false;
                break;
            }
        }
        if (allZeros) {
            adCategoryService.recalculateAllCounters();
        }
    }

    @Inject
    public void setAdCategoryService(final AdCategoryService adCategoryService) {
        this.adCategoryService = adCategoryService;
    }

}

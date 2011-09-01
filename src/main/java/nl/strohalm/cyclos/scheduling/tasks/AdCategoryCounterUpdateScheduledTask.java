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
package nl.strohalm.cyclos.scheduling.tasks;

import java.util.Calendar;

import nl.strohalm.cyclos.services.ads.AdCategoryService;

/**
 * Updates the offer and search ad counters for all categories
 * @author luis
 */
public class AdCategoryCounterUpdateScheduledTask extends BaseScheduledTask {

    private AdCategoryService adCategoryService;

    public AdCategoryCounterUpdateScheduledTask() {
        super("Update ad category counters", false);
    }

    public void setAdCategoryService(final AdCategoryService adCategoryService) {
        this.adCategoryService = adCategoryService;
    }

    @Override
    protected void doRun(final Calendar time) {
        adCategoryService.recalculateAllCounters();
    }

}

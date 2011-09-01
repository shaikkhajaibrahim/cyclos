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

import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService;

/**
 * Initialization for Guarantees. It updates the guarantee's status
 * @see nl.strohalm.cyclos.services.accounts.guarantees.GuaranteeService#guaranteesToProcess(Calendar)
 * @see nl.strohalm.cyclos.scheduling.tasks.GuaranteeScheduledTask
 * @author ameyer
 * 
 */
public class GuaranteeInitialization implements ContextInitialization {
    private GuaranteeService guaranteeService;

    public void init(final ServletContext context) {
        final Calendar time = Calendar.getInstance();
        final List<Guarantee> guarantees = guaranteeService.guaranteesToProcess(time);
        for (final Guarantee guarantee : guarantees) {
            guaranteeService.processGuarantee(guarantee, time);
        }
    }

    @Inject
    public void setGuaranteeService(final GuaranteeService guaranteeService) {
        this.guaranteeService = guaranteeService;
    }
}

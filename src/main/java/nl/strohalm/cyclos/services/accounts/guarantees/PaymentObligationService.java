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

package nl.strohalm.cyclos.services.accounts.guarantees;

import java.util.Calendar;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligation;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligationLog;
import nl.strohalm.cyclos.entities.accounts.guarantees.PaymentObligationQuery;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.OperatorAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.access.SystemAction;

@RelatedEntity(PaymentObligation.class)
@PathToMember("buyer")
public interface PaymentObligationService extends Service {

    @DontEnforcePermission(traceable = false)
    public boolean canChangeStatus(final PaymentObligation paymentObligation, final PaymentObligation.Status newStatus);

    @DontEnforcePermission(traceable = false)
    public boolean canDelete(PaymentObligation paymentObligation);

    @MemberAction(@Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"))
    @OperatorAction(@Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"))
    public void changeStatus(final Long paymentObligationId, final PaymentObligation.Status newStatus);

    /**
     * @param dto
     * @return the payment obligation's ids of these that exceed the payment obligation period of the guarantee type associated to the (there is only
     * one) active issuer's certificacion. the periods' bounds are: inclusive, exclusive
     */
    @MemberAction(@Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations"))
    @OperatorAction(@Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations"))
    @IgnoreMember
    public Long[] checkPaymentObligationPeriod(PaymentObligationPackDTO dto);

    /**
     * @return the status showed in the search payment obligation page allowed to the logged user (e.g. in case of seller group this method restricts
     * the states)
     */
    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "viewPaymentObligations"))
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @IgnoreMember
    public PaymentObligation.Status[] getStatusToFilter();

    /**
     * Loads the Payment Obligation, fetching the specified relationships.
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public PaymentObligation load(Long id, Relationship... fetch);

    /**
     * Used from scheduled task: it changes the payment obligation's status
     */
    @SystemAction
    public void processPaymentObligations(Calendar time);

    @MemberAction(@Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations"))
    @OperatorAction(@Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations"))
    @IgnoreMember
    public void reject(final Long paymentObligationId);

    @MemberAction(@Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"))
    @OperatorAction(@Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"))
    public int remove(Long paymentObligationId);

    /**
     * Saves the payment obligation
     */
    @MemberAction(@Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"))
    @OperatorAction(@Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"))
    public PaymentObligation save(PaymentObligation paymentObligation);

    /**
     * Saves the payment obligation. Used only by other services
     */
    @SystemAction
    public PaymentObligation save(PaymentObligation paymentObligation, boolean validateBeforeSave);

    /**
     * Saves the payment obligation log
     */
    @SystemAction
    public PaymentObligationLog saveLog(PaymentObligationLog paymentObligationLog);

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "viewPaymentObligations"))
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @IgnoreMember
    public List<PaymentObligation> search(PaymentObligationQuery queryParameters);

    @DontEnforcePermission(traceable = true)
    public void validate(PaymentObligation paymentObligation);
}

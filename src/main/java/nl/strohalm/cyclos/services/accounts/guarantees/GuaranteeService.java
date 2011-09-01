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

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.guarantees.Certification;
import nl.strohalm.cyclos.entities.accounts.guarantees.Guarantee;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeQuery;
import nl.strohalm.cyclos.entities.accounts.guarantees.GuaranteeType;
import nl.strohalm.cyclos.entities.groups.MemberGroup;
import nl.strohalm.cyclos.entities.members.Member;
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
import nl.strohalm.cyclos.utils.query.PageParameters;

@RelatedEntity(Guarantee.class)
@PathToMember("buyer")
public interface GuaranteeService extends Service {

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "acceptGuaranteesAsMember"))
    public Guarantee acceptGuaranteeAsMember(Guarantee guarantee, final boolean automaticLoanAuthorization);

    /**
     * It calculates the fee's value according to the specified DTO parameters. Called by CalculateGuaranteeFeeAjaxAction
     */
    public BigDecimal calculateFee(GuaranteeFeeCalculationDTO dto);

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "cancelGuaranteesAsMember"))
    public Guarantee cancelGuaranteeAsMember(Guarantee guarantee);

    @DontEnforcePermission(traceable = false)
    public boolean canChangeStatus(final Guarantee guarantee, final Guarantee.Status newStatus);

    @DontEnforcePermission(traceable = false)
    public boolean canRemoveGuarantee(final Guarantee guarantee);

    @MemberAction(@Permission(module = "memberGuarantees", operation = "issueGuarantees"))
    @OperatorAction(@Permission(module = "operatorGuarantees", operation = "issueGuarantees"))
    @PathToMember("issuer")
    public Guarantee changeStatus(final Guarantee guarantee, final Guarantee.Status newStatus);

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "cancelGuaranteesAsMember"))
    public Guarantee denyGuaranteeAsMember(Guarantee guarantee);

    /**
     * It returns the associated buyers according to the logged user. If the user is an administrator then it returns all buyer groups. If the user is
     * an issuer it returns all groups according to the 'issueCertifications' permission. Otherwise it returns all groups which contains the logged
     * user in its 'buyWithPaymentObligations' collection permission.
     */
    @AdminAction( { @Permission(module = "adminMemberGuarantees", operation = "viewPaymentObligations"), @Permission(module = "adminMemberGuarantees", operation = "viewCertifications"), @Permission(module = "adminMemberGuarantees", operation = "viewGuarantees") })
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "issueGuarantees"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "operatorGuarantees", operation = "issueGuarantees"), @Permission(module = "operatorGuarantees", operation = "sellWithPaymentObligations") })
    @IgnoreMember
    public Collection<? extends MemberGroup> getBuyers();

    /**
     * Returns guarantees matching the given criteria
     */
    @AdminAction( { @Permission(module = "adminMemberGuarantees", operation = "viewCertifications") })
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "issueCertifications"), @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "operatorGuarantees", operation = "issueCertifications"), @Permission(module = "operatorGuarantees", operation = "buyWithPaymentObligations") })
    @IgnoreMember
    public List<Guarantee> getGuarantees(Certification certification, PageParameters pageParameters, List<Guarantee.Status> statusList);

    /**
     * It returns the associated issuers according to the logged user. If the user is an administrator then it returns all issuer groups. If the user
     * is a buyer then returns all groups which contains the logged user in its 'issueCertifications' collection permission. Otherwise (if the logged
     * user is a seller) return all issuers according to all its buyers. There is only a "@MemberAction" annotation without permissions because all
     * the members could view the issuers.
     */
    @AdminAction( { @Permission(module = "adminMemberGuarantees", operation = "viewPaymentObligations"), @Permission(module = "adminMemberGuarantees", operation = "viewCertifications"), @Permission(module = "adminMemberGuarantees", operation = "viewGuarantees") })
    @MemberAction
    @OperatorAction
    @IgnoreMember
    public Collection<? extends MemberGroup> getIssuers();

    /**
     * It returns the issuer groups which has permission over the specified guarantee type.
     */
    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "registerGuarantees"))
    @IgnoreMember
    public Collection<? extends MemberGroup> getIssuers(GuaranteeType guaranteeType);

    /**
     * Lists guarantee's models which has this member as buyer, seller or issuer without take into account any other property.
     */
    @MemberAction
    @OperatorAction
    @IgnoreMember
    public Collection<GuaranteeType.Model> getRelatedGuaranteeModels(final Member member);

    /**
     * It returns the associated seller groups according to the logged user. If the user is an administrator then return all seller groups. If the
     * user is an issuer return all sellers according to all (buyer) groups associated to its 'issueCertifications' permission. Otherwise returns all
     * groups according to its 'buyWithPaymentObligations' permission
     */
    @AdminAction( { @Permission(module = "adminMemberGuarantees", operation = "viewPaymentObligations"), @Permission(module = "adminMemberGuarantees", operation = "viewCertifications"), @Permission(module = "adminMemberGuarantees", operation = "viewGuarantees") })
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "issueGuarantees"), @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "operatorGuarantees", operation = "issueGuarantees"), @Permission(module = "operatorGuarantees", operation = "buyWithPaymentObligations") })
    @IgnoreMember
    public Collection<? extends MemberGroup> getSellers();

    /**
     * Used from scheduled task: it searches for the guarantees to change its status This method is used in conjunction with the processGuarantees
     * method
     */
    @SystemAction
    public List<Guarantee> guaranteesToProcess(Calendar time);

    /**
     * It checks if the logged user belongs to a buyer group
     * @return true only if the logged user's group has the 'buyWithPaymentObligations' permission enabled.
     */
    @DontEnforcePermission(traceable = false)
    public boolean isBuyer();

    /**
     * It checks if the logged user belongs to an issuer group
     * @return true only if the logged user's group has the 'issueGuarantees' permission enabled
     */
    @DontEnforcePermission(traceable = false)
    public boolean isIssuer();

    /**
     * It checks if the logged user belongs to an seller group
     * @return true only if the logged user's group has the 'sellWithPaymentObligations' permission enabled
     */
    @DontEnforcePermission(traceable = false)
    public boolean isSeller();

    /**
     * It loads the guarantee with the specified id, fetching the specified relationships<br>
     * <b>Security note:</b> The implementation of this method must carry out the permissions control.
     */
    @DontEnforcePermission(traceable = true)
    public Guarantee load(Long id, Relationship... fetch);

    /**
     * Used from scheduled task: it changes the guarantee's status
     * @see #guaranteesToProcess(Calendar)
     */
    @SystemAction
    public Guarantee processGuarantee(Guarantee guarantee, Calendar time);

    /**
     * Used from scheduled task: It generates a new loan for each guarantee which status is ACCEPTED and become valid before or at the time parameter
     * @param time the times used as the current time
     */
    @SystemAction
    public void processGuaranteeLoans(Calendar time);

    /**
     * Registers a new guarantee
     */
    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "registerGuarantees"))
    public Guarantee registerGuarantee(Guarantee guarantee);

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "registerGuarantees"))
    public int remove(Long guaranteeId);

    /**
     * It creates a new guarantee from a pack of payment obligations
     * @param pack a pack of payment obligations
     * @return the new created guarantee
     */
    @MemberAction(@Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations"))
    @OperatorAction(@Permission(module = "operatorGuarantees", operation = "sellWithPaymentObligations"))
    @IgnoreMember
    public Guarantee requestGuarantee(PaymentObligationPackDTO pack);

    /**
     * It searches for Guarantees
     * @return the list of guarantees according to the query parameters
     */
    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "viewGuarantees"))
    @MemberAction
    @OperatorAction
    @IgnoreMember
    public List<Guarantee> search(GuaranteeQuery queryParameters);

    /**
     * It makes guarantee's validation
     */
    @DontEnforcePermission(traceable = true)
    public void validate(Guarantee guarantee);

}
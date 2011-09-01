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
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.accounts.Currency;
import nl.strohalm.cyclos.entities.accounts.guarantees.Certification;
import nl.strohalm.cyclos.entities.accounts.guarantees.CertificationQuery;
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

@RelatedEntity(Certification.class)
@PathToMember("issuer")
public interface CertificationService extends Service {

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "cancelCertificationsAsMember"))
    public void cancelCertificationAsMember(Long certificationId);

    @DontEnforcePermission(traceable = false)
    public boolean canChangeStatus(final Certification certification, final Certification.Status newStatus);

    @DontEnforcePermission(traceable = false)
    public boolean canDelete(Certification certification);

    @MemberAction(@Permission(module = "memberGuarantees", operation = "issueCertifications"))
    @OperatorAction(@Permission(module = "operatorGuarantees", operation = "issueCertifications"))
    public void changeStatus(final Long certificationId, final Certification.Status newStatus);

    /**
     * 
     * @param currency
     * @param buyer
     * @param issuer
     * @return the unique active certification to the tuple (buyer, issuer, currency)
     */
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "issueCertifications"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "operatorGuarantees", operation = "issueCertifications"), @Permission(module = "operatorGuarantees", operation = "sellWithPaymentObligations") })
    @IgnoreMember
    public Certification getActiveCertification(final Currency currency, final Member buyer, final Member issuer);

    /**
     * @return the members (Issuers) who as an (unique) active certification issued to the specified buyer
     */
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations"), @Permission(module = "memberGuarantees", operation = "sellWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "operatorGuarantees", operation = "buyWithPaymentObligations"), @Permission(module = "operatorGuarantees", operation = "sellWithPaymentObligations") })
    @IgnoreMember
    public List<Member> getCertificationIssuersForBuyer(Member buyer, Currency currency);

    /**
     * @param includePendingGuarantees if true include the pending guarantees in the used amount
     * @return the used certification amount according to the associated guarantees
     */
    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "viewCertifications"))
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "operatorGuarantees", operation = "buyWithPaymentObligations") })
    @IgnoreMember
    public BigDecimal getUsedAmount(Certification certification, boolean includePendingGuarantees);

    /**
     * Loads the Certification, fetching the specified relationships.
     */
    @DontEnforcePermission(traceable = true, value = "The implementation of this method must carry out the permissions control")
    public Certification load(Long id, Relationship... fetch);

    /**
     * Used from scheduled task and intercepted by AOP: it changes the cerification's status
     * @param taskTime
     */
    @SystemAction
    public List<Certification> processCertifications(Calendar taskTime);

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "cancelCertificationsAsMember"))
    public int remove(Long certificationId);

    @MemberAction(@Permission(module = "memberGuarantees", operation = "issueCertifications"))
    public Certification save(Certification certification);

    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "viewCertifications"))
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "issueCertifications"), @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "memberGuarantees", operation = "issueCertifications"), @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @IgnoreMember
    public List<Certification> search(CertificationQuery queryParameters);

    /**
     * 
     * @param queryParameters
     * @return the list of certifications with additional data: the certifications's used amount
     */
    @AdminAction(@Permission(module = "adminMemberGuarantees", operation = "viewCertifications"))
    @MemberAction( { @Permission(module = "memberGuarantees", operation = "issueCertifications"), @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @OperatorAction( { @Permission(module = "memberGuarantees", operation = "issueCertifications"), @Permission(module = "memberGuarantees", operation = "buyWithPaymentObligations") })
    @IgnoreMember
    public List<CertificationDTO> searchWithUsedAmount(final CertificationQuery queryParameters);

    @DontEnforcePermission(traceable = true)
    public void validate(Certification certification);

}

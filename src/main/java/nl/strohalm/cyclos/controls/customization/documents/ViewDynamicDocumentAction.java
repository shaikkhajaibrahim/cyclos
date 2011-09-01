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
package nl.strohalm.cyclos.controls.customization.documents;

import java.util.Calendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.annotations.RequestParameter;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.customization.documents.DynamicDocument;
import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.services.customization.DocumentService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.CustomFieldHelper;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.EntityWithCustomFieldsWrapper;
import nl.strohalm.cyclos.utils.RequestHelper;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Form used to view a custom document
 * @author luis
 */

@AdminAction( { @Permission(module = "adminMemberDocuments", operation = "details") })
@MemberAction( { @Permission(module = "memberDocuments", operation = "view") })
@BrokerAction( { @Permission(module = "brokerDocuments", operation = "view") })
@RequestParameter("memberId")
@RelatedEntity(Member.class)
@PathToMember("")
public class ViewDynamicDocumentAction extends BaseAction {

    private ElementService     elementService;
    private DocumentService    documentService;
    private CustomFieldService customFieldService;

    public CustomFieldService getCustomFieldService() {
        return customFieldService;
    }

    public DocumentService getDocumentService() {
        return documentService;
    }

    public ElementService getElementService() {
        return elementService;
    }

    @Inject
    public void setCustomFieldService(final CustomFieldService customFieldService) {
        this.customFieldService = customFieldService;
    }

    @Inject
    public void setDocumentService(final DocumentService documentService) {
        this.documentService = documentService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final ViewDocumentForm form = context.getForm();
        final long documentId = form.getDocumentId();
        boolean isValidDocument = false;
        DynamicDocument document;
        if (documentId > 0L) {
            document = (DynamicDocument) documentService.load(documentId);
            final Group group = getFetchService().fetch(context.getGroup(), Group.Relationships.DOCUMENTS);

            if (context.isAdmin()) {
                isValidDocument = group.getDocuments().contains(document);
            } else {
                if (context.isMember()) {
                    isValidDocument = group.getDocuments().contains(document);
                }

                if (context.isBroker() && !isValidDocument) {
                    final BrokerGroup brokerGroup = (BrokerGroup) getFetchService().fetch(group, BrokerGroup.Relationships.BROKER_DOCUMENTS);
                    isValidDocument = brokerGroup.getBrokerDocuments().contains(document);
                }
            }
        }

        if (!isValidDocument) {
            throw new PermissionDeniedException();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final ViewDocumentForm form = context.getForm();
        final long memberId = form.getMemberId();
        final long documentId = form.getDocumentId();
        if (memberId <= 0L || documentId <= 0L) {
            throw new ValidationException();
        }
        Member member;
        DynamicDocument document;
        try {
            member = (Member) elementService.load(memberId, Element.Relationships.USER, Element.Relationships.GROUP, Member.Relationships.BROKER, Member.Relationships.CUSTOM_VALUES);
            document = (DynamicDocument) documentService.load(documentId);
        } catch (final Exception e) {
            throw new ValidationException();
        }
        if (document.isHasFormPage()) {
            final String formPageName = CustomizationHelper.formFile(getServlet().getServletContext(), document).getName();
            request.setAttribute("formPage", CustomizationHelper.DOCUMENT_PATH + formPageName);
        }
        final String documentPageName = CustomizationHelper.documentFile(getServlet().getServletContext(), document).getName();
        request.setAttribute("documentPage", CustomizationHelper.DOCUMENT_PATH + documentPageName);

        request.setAttribute("document", document);

        // Set the member inside a wrapper, allowing access to custom fields the same way as properties
        final List<MemberCustomField> customFields = CustomFieldHelper.onlyForGroup((List<MemberCustomField>) customFieldService.listByNature(CustomField.Nature.MEMBER), member.getMemberGroup());
        request.setAttribute("member", new EntityWithCustomFieldsWrapper(member, customFields));
        request.setAttribute("now", Calendar.getInstance());

        if (document.isHasFormPage() && RequestHelper.isGet(request)) {
            return context.getInputForward();
        } else {
            return context.getSuccessForward();
        }
    }
}

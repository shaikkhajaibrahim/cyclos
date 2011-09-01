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

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DynamicDocument;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.DocumentService;
import nl.strohalm.cyclos.utils.CustomizationHelper;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

@AdminAction( { @Permission(module = "adminMemberDocuments", operation = "manageDynamic") })
@IgnoreMember
public class PreviewDynamicDocumentAction extends BaseAction {

    private DocumentService documentService;

    @Inject
    public void setDocumentService(final DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * This methods is checking that group permission has been asigned to the member/admin who is trying to see the document
     * @see nl.strohalm.cyclos.controls.BaseAction#checkPermission(nl.strohalm.cyclos.controls.ActionContext)
     */
    @Override
    protected void checkPermission(final ActionContext context) {
        final PreviewDocumentForm form = context.getForm();
        final long documentId = form.getDocumentId();
        if (documentId > 0L) {
            final DynamicDocument document = (DynamicDocument) documentService.load(documentId, Document.Relationships.GROUPS);
            final AdminGroup group = (AdminGroup) getFetchService().fetch(context.getGroup(), Group.Relationships.DOCUMENTS);
            if (!group.getDocuments().contains(document)) {
                throw new PermissionDeniedException();
            }
        }
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final PreviewDocumentForm form = context.getForm();
        final long documentId = form.getDocumentId();

        DynamicDocument document;
        try {
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

        return context.getSuccessForward();
    }
}
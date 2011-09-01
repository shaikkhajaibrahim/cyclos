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
package nl.strohalm.cyclos.controls.members.documents;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.controls.BaseAction;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DocumentQuery;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group.Status;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.customization.DocumentService;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;

/**
 * Action used to select a document to print
 * @author luis
 * @author Jefferson Magno
 */
public class SelectDocumentAction extends BaseAction {

    private DocumentService documentService;
    private ElementService  elementService;

    @Inject
    public void setDocumentService(final DocumentService documentService) {
        this.documentService = documentService;
    }

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected ActionForward executeAction(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final SelectDocumentForm form = context.getForm();
        boolean myDocuments = false;
        boolean adminCanManage = false;
        boolean byBroker = false;
        boolean brokerCanManageMemberDocuments = false;
        long memberId = form.getMemberId();
        Member member = null;
        if (memberId > 0L) {
            try {
                member = (Member) elementService.load(memberId, Element.Relationships.USER);
                if (memberId == context.getElement().getId()) {
                    myDocuments = true;
                } else {
                    byBroker = context.isBrokerOf(member);
                    adminCanManage = getPermissionService().checkPermission("adminMemberDocuments", "manageMember");
                }
            } catch (final Exception e) {
                // Just ignore it
            }
        } else {
            if (context.isAdmin() || context.isOperator()) {
                throw new ValidationException();
            }
            member = context.getElement();
            memberId = member.getId();
            myDocuments = true;
        }
        if (member == null) {
            throw new ValidationException();
        }
        List<Document> documents;
        final DocumentQuery documentQuery = new DocumentQuery();
        documentQuery.setMember(member);
        if (byBroker) {
            brokerCanManageMemberDocuments = getPermissionService().checkPermission("brokerDocuments", "viewMember") || getPermissionService().checkPermission("brokerDocuments", "manageMember");
            documentQuery.setBrokerCanManageMemberDocuments(brokerCanManageMemberDocuments);
            documentQuery.setBrokerGroup((BrokerGroup) context.getGroup());
        } else {
            documentQuery.setGroup(context.getGroup());
        }
        documents = documentService.search(documentQuery);
        request.setAttribute("member", member);
        request.setAttribute("documents", documents);
        request.setAttribute("myDocuments", myDocuments);
        request.setAttribute("byBroker", byBroker);
        request.setAttribute("adminCanManage", adminCanManage);
        request.setAttribute("removed", member.getGroup().getStatus() == Status.REMOVED);
        return context.getInputForward();
    }

}
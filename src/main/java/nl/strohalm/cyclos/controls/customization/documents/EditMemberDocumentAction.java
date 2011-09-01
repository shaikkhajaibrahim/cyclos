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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.MemberDocument;
import nl.strohalm.cyclos.entities.customization.documents.StaticDocument;
import nl.strohalm.cyclos.entities.groups.AdminGroup;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.services.customization.exceptions.CannotUploadFileException;
import nl.strohalm.cyclos.services.elements.ElementService;
import nl.strohalm.cyclos.utils.ActionHelper;
import nl.strohalm.cyclos.utils.EntityHelper;
import nl.strohalm.cyclos.utils.RelationshipHelper;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.binding.BeanBinder;
import nl.strohalm.cyclos.utils.binding.DataBinder;
import nl.strohalm.cyclos.utils.binding.PropertyBinder;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.struts.action.ActionForward;
import org.apache.struts.upload.FormFile;

/**
 * Action used to edit a member static document
 * @author Jefferson Magno
 */
@AdminAction( { @Permission(module = "adminMemberDocuments", operation = "manageMember") })
@BrokerAction( { @Permission(module = "brokerDocuments", operation = "manageMember") })
@IgnoreMember
public class EditMemberDocumentAction extends EditStaticDocumentAction {

    private DataBinder<MemberDocument> dataBinder;
    private ElementService             elementService;

    @Inject
    public void setElementService(final ElementService elementService) {
        this.elementService = elementService;
    }

    @Override
    protected void checkPermission(final ActionContext context) {
        final EditMemberDocumentForm form = context.getForm();
        final long documentId = form.getDocumentId();
        final long memberId = form.getMemberId();
        MemberDocument document;
        Member member = null;

        if (documentId > 0L) {
            document = (MemberDocument) documentService.load(documentId, RelationshipHelper.nested(MemberDocument.Relationships.MEMBER, Member.Relationships.BROKER));
            final MemberDocument.Visibility visibility = document.getVisibility();
            if (context.isBroker() && visibility == MemberDocument.Visibility.ADMIN) {
                throw new PermissionDeniedException();
            }
            member = document.getMember();
        } else if (memberId > 0) {
            member = (Member) elementService.load(memberId, Member.Relationships.BROKER);
        }

        if (member != null) {
            if (context.isAdmin()) {
                final AdminGroup group = (AdminGroup) getFetchService().fetch(context.getGroup(), AdminGroup.Relationships.MANAGES_GROUPS);
                if (!group.getManagesGroups().contains(member.getGroup())) {
                    throw new PermissionDeniedException();
                }
            } else {
                if (!context.getElement().equals(member.getBroker())) {
                    throw new PermissionDeniedException();
                }
            }
        }
    }

    @Override
    protected Class<? extends StaticDocument> getEntityType() {
        return MemberDocument.class;
    }

    @Override
    protected ActionForward handleSubmit(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final EditStaticDocumentForm form = context.getForm();
        MemberDocument document = getDataBinder().readFromString(form.getDocument());
        final boolean isInsert = document.getId() == null;
        try {
            final FormFile upload = form.getUpload();
            document = (MemberDocument) documentService.saveMember(document, upload.getInputStream(), upload.getFileSize(), upload.getFileName(), upload.getContentType());

            context.sendMessage(isInsert ? "document.inserted" : "document.modified");
            request.setAttribute("document", document);
            return ActionHelper.redirectWithParam(request, context.getSuccessForward(), "documentId", document.getId());
        } catch (final IOException e) {
            throw new CannotUploadFileException(e);
        }
    }

    @Override
    protected void prepareForm(final ActionContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        final EditMemberDocumentForm form = context.getForm();
        Member member;
        final long documentId = form.getDocumentId();
        MemberDocument document;
        if (documentId > 0L) {
            document = (MemberDocument) documentService.load(documentId);
            member = document.getMember();
        } else {
            final long memberId = form.getMemberId();
            if (memberId < 1) {
                throw new ValidationException();
            }
            member = EntityHelper.reference(Member.class, memberId);
            document = new MemberDocument();
            document.setMember(member);
        }
        final boolean byBroker = context.isBrokerOf(member) && getPermissionService().checkPermission("brokerDocuments", "manageMember");
        final boolean adminCanManage = context.isAdmin() && getPermissionService().checkPermission("adminMemberDocuments", "manageMember");

        getDataBinder().writeAsString(form.getDocument(), document);
        request.setAttribute("member", member);
        request.setAttribute("document", document);
        request.setAttribute("byBroker", byBroker);
        request.setAttribute("adminCanManage", adminCanManage);
        final List<MemberDocument.Visibility> visibilities = new ArrayList<MemberDocument.Visibility>();
        visibilities.add(MemberDocument.Visibility.BROKER);
        visibilities.add(MemberDocument.Visibility.MEMBER);
        if (!byBroker) {
            visibilities.add(MemberDocument.Visibility.ADMIN);
        }
        request.setAttribute("visibilities", visibilities);
    }

    @Override
    protected void validateForm(final ActionContext context) {
        final EditStaticDocumentForm form = context.getForm();
        final Document document = getDataBinder().readFromString(form.getDocument());
        documentService.validate(document, false);
    }

    private DataBinder<MemberDocument> getDataBinder() {
        if (dataBinder == null) {
            final BeanBinder<MemberDocument> beanBinder = EditStaticDocumentAction.getDataBinder(MemberDocument.class);
            beanBinder.registerBinder("member", PropertyBinder.instance(Member.class, "member"));
            beanBinder.registerBinder("visibility", PropertyBinder.instance(MemberDocument.Visibility.class, "visibility"));
            dataBinder = beanBinder;
        }
        return dataBinder;
    }
}
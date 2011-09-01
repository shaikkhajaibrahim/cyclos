/*
    This file is part of Cyclos <http://project.cyclos.org>

    Cyclos is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Cyclos is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Cyclos. If not, see <http://www.gnu.org/licenses/>.

 */
package nl.strohalm.cyclos.controls.customization.documents;

import nl.strohalm.cyclos.annotations.RequestParameter;
import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.customization.documents.MemberDocument;
import nl.strohalm.cyclos.entities.groups.Group.Nature;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.MemberAction;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;

@AdminAction( { @Permission(module = "adminMemberDocuments", operation = "manageMember") })
@MemberAction
@BrokerAction( { @Permission(module = "brokerDocuments", operation = "viewMember") })
@RelatedEntity(MemberDocument.class)
@PathToMember("member")
@RequestParameter("documentId")
public class ViewMemberDocumentAction extends AbstractViewDocumentAction {

    @Override
    protected void checkPermission(final ActionContext context) {
        final PreviewDocumentForm form = context.getForm();
        final long documentId = form.getDocumentId();
        MemberDocument document;
        if (documentId > 0L) {
            document = (MemberDocument) documentService.load(documentId);
            final MemberDocument.Visibility visibility = document.getVisibility();

            if (((context.getGroup().getNature() == Nature.MEMBER) && visibility != MemberDocument.Visibility.MEMBER) || (context.isBroker() && visibility == MemberDocument.Visibility.ADMIN)) {
                throw new PermissionDeniedException();
            }
        }
    }
}

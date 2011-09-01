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

import nl.strohalm.cyclos.controls.ActionContext;
import nl.strohalm.cyclos.entities.customization.documents.StaticDocument;
import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.exceptions.PermissionDeniedException;

public abstract class ViewStaticDocumentAction extends AbstractViewDocumentAction {

    /**
     * This methods is checking that group permission has been asigned to the member/admin who is trying to see the document
     * @see nl.strohalm.cyclos.controls.BaseAction#checkPermission(nl.strohalm.cyclos.controls.ActionContext)
     */
    @Override
    protected void checkPermission(final ActionContext context) {
        final PreviewDocumentForm form = context.getForm();
        final long documentId = form.getDocumentId();
        boolean isValidDocument = false;
        StaticDocument document;
        if (documentId > 0L) {
            document = (StaticDocument) documentService.load(documentId);
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
}

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

import java.io.File;
import java.util.List;

import javax.servlet.ServletContext;

import nl.strohalm.cyclos.annotations.Inject;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DocumentPage;
import nl.strohalm.cyclos.entities.customization.documents.DocumentQuery;
import nl.strohalm.cyclos.entities.customization.documents.DynamicDocument;
import nl.strohalm.cyclos.services.customization.DocumentService;
import nl.strohalm.cyclos.utils.CustomizationHelper;

/**
 * Initializes the custom documents
 * @author luis
 */
public class DocumentInitialization implements ContextInitialization {

    private DocumentService documentService;

    public DocumentService getDocumentService() {
        return documentService;
    }

    public void init(final ServletContext context) {
        final DocumentQuery documentQuery = new DocumentQuery();
        documentQuery.setNature(Document.Nature.DYNAMIC);
        documentQuery.fetch(DynamicDocument.Relationships.FORM_PAGE, DynamicDocument.Relationships.DOCUMENT_PAGE);
        final List<Document> documents = documentService.search(documentQuery);
        for (final Document document : documents) {
            final DynamicDocument dynamicDocument = (DynamicDocument) document;
            if (dynamicDocument.isHasFormPage()) {
                final File formFile = CustomizationHelper.formFile(context, dynamicDocument);
                final DocumentPage formPage = dynamicDocument.getFormPage();
                CustomizationHelper.updateFile(context, formFile, formPage);
            }
            final File documentFile = CustomizationHelper.documentFile(context, dynamicDocument);
            final DocumentPage documentPage = dynamicDocument.getDocumentPage();
            CustomizationHelper.updateFile(context, documentFile, documentPage);
        }
    }

    @Inject
    public void setDocumentService(final DocumentService documentService) {
        this.documentService = documentService;
    }

}
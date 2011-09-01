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
package nl.strohalm.cyclos.services.customization;

import java.io.InputStream;
import java.util.List;

import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DocumentQuery;
import nl.strohalm.cyclos.entities.customization.documents.DynamicDocument;
import nl.strohalm.cyclos.entities.customization.documents.MemberDocument;
import nl.strohalm.cyclos.entities.customization.documents.StaticDocument;
import nl.strohalm.cyclos.services.Service;
import nl.strohalm.cyclos.utils.access.AdminAction;
import nl.strohalm.cyclos.utils.access.BrokerAction;
import nl.strohalm.cyclos.utils.access.DontEnforcePermission;
import nl.strohalm.cyclos.utils.access.IgnoreMember;
import nl.strohalm.cyclos.utils.access.PathToMember;
import nl.strohalm.cyclos.utils.access.Permission;
import nl.strohalm.cyclos.utils.access.RelatedEntity;

/**
 * Service interface for document management (save, load, edit)
 * @author luis
 * @author Jefferson Magno
 */
public interface DocumentService extends Service {

    /**
     * Loads the document with the specified fetch.
     */
    @DontEnforcePermission(traceable = true, value = "Each client of this method must implement the corresponding permission's control")
    Document load(Long id, Relationship... fetch);

    /**
     * Remove the specified dynamic documents, returning the number of removed documents
     */
    @AdminAction(@Permission(module = "adminMemberDocuments", operation = "manageDynamic"))
    @IgnoreMember
    int removeDynamic(Long... ids);

    /**
     * Remove the specified member documents, returning the number of removed documents
     */
    @AdminAction(@Permission(module = "adminMemberDocuments", operation = "manageMember"))
    @BrokerAction(@Permission(module = "brokerDocuments", operation = "manageMember"))
    @RelatedEntity(MemberDocument.class)
    @PathToMember("member")
    int removeMember(Long... ids);

    /**
     * Remove the specified static documents, returning the number of removed documents
     */
    @AdminAction(@Permission(module = "adminMemberDocuments", operation = "manageStatic"))
    @IgnoreMember
    int removeStatic(Long... ids);

    /**
     * Save a dynamic document
     */
    @AdminAction(@Permission(module = "adminMemberDocuments", operation = "manageDynamic"))
    @IgnoreMember
    Document saveDynamic(DynamicDocument document);

    /**
     * Save a member document
     */
    @AdminAction(@Permission(module = "adminMemberDocuments", operation = "manageMember"))
    @BrokerAction(@Permission(module = "brokerDocuments", operation = "manageMember"))
    @PathToMember("member")
    Document saveMember(MemberDocument document, InputStream stream, int size, String fileName, String contentType);

    /**
     * Save a static document
     */
    @AdminAction(@Permission(module = "adminMemberDocuments", operation = "manageStatic"))
    @IgnoreMember
    Document saveStatic(StaticDocument document, InputStream stream, int size, String fileName, String contentType);

    /**
     * Search documents
     */
    @DontEnforcePermission(traceable = true, value = "Each client of this method must implement the corresponding permission's control or set the properly query")
    List<Document> search(DocumentQuery documentQuery);

    /**
     * Validates a document
     */
    @DontEnforcePermission(traceable = true)
    void validate(Document document);

    /**
     * Validates a document
     */
    @DontEnforcePermission(traceable = true)
    void validate(Document document, boolean validateBinaryFile);

}
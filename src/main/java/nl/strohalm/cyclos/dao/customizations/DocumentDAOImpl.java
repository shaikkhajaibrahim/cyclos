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
package nl.strohalm.cyclos.dao.customizations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.strohalm.cyclos.dao.BaseDAOImpl;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.customization.documents.Document;
import nl.strohalm.cyclos.entities.customization.documents.DocumentQuery;
import nl.strohalm.cyclos.entities.customization.documents.MemberDocument;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.utils.hibernate.HibernateHelper;

/**
 * Implementation for DocumentDAO
 * @author luis
 */
public class DocumentDAOImpl extends BaseDAOImpl<Document> implements DocumentDAO {

    public DocumentDAOImpl() {
        super(Document.class);
    }

    public List<Document> search(final DocumentQuery query) throws DaoException {
        final Map<String, Object> namedParameters = new HashMap<String, Object>();
        final StringBuilder hql = HibernateHelper.getInitialQuery(getEntityType(), "doc", query.getFetch());

        // Save named parameters values
        namedParameters.put("dynamicType", Document.Nature.DYNAMIC.getValue());
        namedParameters.put("memberType", Document.Nature.MEMBER.getValue());
        namedParameters.put("staticType", Document.Nature.STATIC.getValue());
        namedParameters.put("memberVisibility", MemberDocument.Visibility.MEMBER.getValue());
        namedParameters.put("brokerVisibility", MemberDocument.Visibility.BROKER.getValue());

        // Nature
        if (query.getNature() != null) {
            HibernateHelper.addParameterToQuery(hql, namedParameters, "doc.class", query.getNature().getValue());
        }

        // Groups (member searching for his/her own documents)
        if (query.getGroup() != null) {
            hql.append(" and (doc.class = :memberType or exists (select g.id from Group g where doc in elements(g.documents) and g = :group)) ");
            namedParameters.put("group", query.getGroup());
        }

        // Broker groups (broker searching for documents of his/her members)
        if (query.getBrokerGroup() != null) {
            hql.append(" and ( ");
            if (query.isBrokerCanManageMemberDocuments()) {
                hql.append(" (doc.class = :memberType) or ");
            }
            hql.append(" exists (select g.id from BrokerGroup g where doc in elements(g.brokerDocuments) and g = :brokerGroup) ");
            hql.append(" ) ");
            namedParameters.put("brokerGroup", query.getBrokerGroup());
        }

        // Member
        if (query.getMember() != null) {
            final Member member = getFetchDao().fetch(query.getMember(), Member.Relationships.BROKER);
            hql.append(" and (doc.class in (:dynamicType, :staticType) or ");
            hql.append(" exists (select id from MemberDocument md where md = doc and md.member = :member ");
            // Viewer
            if (query.getViewer() instanceof Member) {
                // Member (or broker) searching his own documents
                if (query.getViewer().equals(member)) {
                    hql.append(" and md.visibility = :memberVisibility ");
                }
                // Broker searching member documents
                if (member.getBroker() != null && member.getBroker().equals(query.getViewer())) {
                    hql.append(" and md.visibility in (:memberVisibility, :brokerVisibility) ");
                }
            }
            hql.append(" )) ");
            namedParameters.put("member", query.getMember());
        } else {
            hql.append(" and doc.class <> :memberType ");
        }

        HibernateHelper.appendOrder(hql, "doc.name");
        return list(query, hql.toString(), namedParameters);
    }

}
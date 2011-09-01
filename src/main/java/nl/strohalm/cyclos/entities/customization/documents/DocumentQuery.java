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
package nl.strohalm.cyclos.entities.customization.documents;

import nl.strohalm.cyclos.entities.groups.BrokerGroup;
import nl.strohalm.cyclos.entities.groups.Group;
import nl.strohalm.cyclos.entities.members.Element;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.utils.query.QueryParameters;

/**
 * Query parameters for documents
 * @author luis
 * @author Jefferson Magno
 */
public class DocumentQuery extends QueryParameters {

    private static final long serialVersionUID               = 3786759993229052921L;
    private boolean           brokerCanManageMemberDocuments = false;
    private Group             group;
    private BrokerGroup       brokerGroup;
    private Member            member;
    private Document.Nature   nature;
    private Element           viewer;

    public BrokerGroup getBrokerGroup() {
        return brokerGroup;
    }

    public Group getGroup() {
        return group;
    }

    public Member getMember() {
        return member;
    }

    public Document.Nature getNature() {
        return nature;
    }

    public Element getViewer() {
        return viewer;
    }

    public boolean isBrokerCanManageMemberDocuments() {
        return brokerCanManageMemberDocuments;
    }

    public void setBrokerCanManageMemberDocuments(final boolean brokerCanManageMemberDocuments) {
        this.brokerCanManageMemberDocuments = brokerCanManageMemberDocuments;
    }

    public void setBrokerGroup(final BrokerGroup brokerGroup) {
        this.brokerGroup = brokerGroup;
    }

    public void setGroup(final Group group) {
        this.group = group;
    }

    public void setMember(final Member member) {
        this.member = member;
    }

    public void setNature(final Document.Nature nature) {
        this.nature = nature;
    }

    public void setViewer(final Element viewer) {
        this.viewer = viewer;
    }

}
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
package nl.strohalm.cyclos.webservices.members;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.webservices.model.MemberVO;
import nl.strohalm.cyclos.webservices.utils.ResultPage;

/**
 * Page results for members
 * @author luis
 */
public class MemberResultPage extends ResultPage<MemberVO> {
    private static final long serialVersionUID = -186613342878700230L;
    private List<MemberVO>    members;

    public MemberResultPage() {
    }

    public MemberResultPage(final int currentPage, final int totalCount, final List<MemberVO> members) {
        super(currentPage, totalCount);
        this.members = members;
    }

    public List<MemberVO> getMembers() {
        return members;
    }

    @Override
    public Iterator<MemberVO> iterator() {
        if (members == null) {
            return Collections.<MemberVO> emptyList().iterator();
        }
        return members.iterator();
    }

    public void setMembers(final List<MemberVO> members) {
        this.members = members;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        boolean first = true;
        buffer.append("MemberResultPage [");

        for (final MemberVO vo : members) {
            if (!first) {
                buffer.append(", ");
            }
            buffer.append(vo.toString());
            first = false;
        }

        buffer.append("]");

        return buffer.toString();

    }

}

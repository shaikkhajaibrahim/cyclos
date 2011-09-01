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
package nl.strohalm.cyclos.webservices.infotexts;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.webservices.model.InfoTextVO;
import nl.strohalm.cyclos.webservices.utils.ResultPage;

/**
 * Page results for info texts
 * @author luis
 */
public class InfoTextResultPage extends ResultPage<InfoTextVO> {
    private static final long serialVersionUID = 2676760669726197474L;
    private List<InfoTextVO>  infoTexts;

    public InfoTextResultPage() {
    }

    public InfoTextResultPage(final int currentPage, final int totalCount, final List<InfoTextVO> infoTexts) {
        super(currentPage, totalCount);
        this.infoTexts = infoTexts;
    }

    public List<InfoTextVO> getInfoTexts() {
        return infoTexts;
    }

    @Override
    public Iterator<InfoTextVO> iterator() {
        if (infoTexts == null) {
            return Collections.<InfoTextVO> emptyList().iterator();
        }
        return infoTexts.iterator();
    }

    public void setInfoTexts(final List<InfoTextVO> infoTexts) {
        this.infoTexts = infoTexts;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        boolean first = true;
        buffer.append("InfoTextResultPage[");

        for (final InfoTextVO vo : infoTexts) {
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

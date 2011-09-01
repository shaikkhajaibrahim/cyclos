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
package nl.strohalm.cyclos.webservices.ads;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.webservices.model.AdVO;
import nl.strohalm.cyclos.webservices.utils.ResultPage;

/**
 * Page results for ads
 * @author luis
 */
public class AdResultPage extends ResultPage<AdVO> {
    private static final long serialVersionUID = -186613342878700230L;
    private List<AdVO>        ads;

    public AdResultPage() {
    }

    public AdResultPage(final int currentPage, final int totalCount, final List<AdVO> ads) {
        super(currentPage, totalCount);
        this.ads = ads;
    }

    public List<AdVO> getAds() {
        return ads;
    }

    @Override
    public Iterator<AdVO> iterator() {
        if (ads == null) {
            return Collections.<AdVO> emptyList().iterator();
        }
        return ads.iterator();
    }

    public void setAds(final List<AdVO> ads) {
        this.ads = ads;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        boolean first = true;
        buffer.append("AdResultPage[");

        for (final AdVO vo : ads) {
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

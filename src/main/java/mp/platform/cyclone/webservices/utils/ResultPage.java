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
package mp.platform.cyclone.webservices.utils;

import java.io.Serializable;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import nl.strohalm.cyclos.utils.ObjectHelper;

/**
 * Contains a page of results
 * @author luis
 */
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class ResultPage<T> implements Serializable, Iterable<T> {
    private static final long serialVersionUID = -7387157723771363227L;
    private Integer           currentPage;
    private Integer           totalCount;

    public ResultPage() {
    }

    public ResultPage(final int currentPage, final int totalCount) {
        this.currentPage = currentPage;
        this.totalCount = totalCount;
    }

    public int getCurrentPage() {
        return ObjectHelper.valueOf(currentPage);
    }

    public int getTotalCount() {
        return ObjectHelper.valueOf(totalCount);
    }

    public abstract Iterator<T> iterator();

    public void setCurrentPage(final int currentPage) {
        this.currentPage = currentPage;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

}

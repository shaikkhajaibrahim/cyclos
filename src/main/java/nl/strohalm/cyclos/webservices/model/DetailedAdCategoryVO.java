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
package nl.strohalm.cyclos.webservices.model;

import java.util.List;

import javax.xml.bind.annotation.XmlType;

/**
 * Detailed data for ad category for web services
 * @author luis
 */
@XmlType(name = "detailedAdCategory")
public class DetailedAdCategoryVO extends AdCategoryVO {
    private static final long          serialVersionUID = -477774908557924729L;
    private String                     fullName;
    private List<DetailedAdCategoryVO> children;
    private int                        level;
    private int                        countOffer;
    private int                        countSearch;

    public List<DetailedAdCategoryVO> getChildren() {
        return children;
    }

    public int getCountOffer() {
        return countOffer;
    }

    public int getCountSearch() {
        return countSearch;
    }

    public String getFullName() {
        return fullName;
    }

    public int getLevel() {
        return level;
    }

    public void setChildren(final List<DetailedAdCategoryVO> children) {
        this.children = children;
    }

    public void setCountOffer(final int offerCount) {
        countOffer = offerCount;
    }

    public void setCountSearch(final int searchCount) {
        countSearch = searchCount;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public void setLevel(final int level) {
        this.level = level;
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("DetailedAdCategoryVO(fullName=" + fullName + ", level=" + level + ", countOffer=" + countOffer + ", countSearch=" + countSearch);
        if (children == null) {
            buffer.append("childrens=0)");
        } else {
            buffer.append("childrens=" + children.size() + ")");
        }
        return buffer.toString();
    }
}

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
package mp.platform.cyclone.webservices.members;

import java.util.List;

import nl.strohalm.cyclos.utils.ObjectHelper;
import mp.platform.cyclone.webservices.model.FieldValueVO;
import mp.platform.cyclone.webservices.utils.SearchParameters;

/**
 * Base parameters for searching members via web services
 * @author luis
 */
public abstract class AbstractMemberSearchParameters extends SearchParameters {
    private static final long  serialVersionUID = 5544458121666295428L;
    private List<FieldValueVO> fields;
    private Boolean            showCustomFields;
    private Boolean            showImages;
    private Boolean            withImagesOnly;
    private List<Long>         groupFilterIds;
    private List<Long>         groupIds;

    public List<FieldValueVO> getFields() {
        return fields;
    }

    public List<Long> getGroupFilterIds() {
        return groupFilterIds;
    }

    public List<Long> getGroupIds() {
        return groupIds;
    }

    public boolean isShowCustomFields() {
        return ObjectHelper.valueOf(showCustomFields);
    }

    public boolean isShowImages() {
        return ObjectHelper.valueOf(showImages);
    }

    public boolean isWithImagesOnly() {
        return ObjectHelper.valueOf(withImagesOnly);
    }

    public void setFields(final List<FieldValueVO> memberFields) {
        fields = memberFields;
    }

    public void setGroupFilterIds(final List<Long> groupFilterIds) {
        this.groupFilterIds = groupFilterIds;
    }

    public void setGroupIds(final List<Long> groupIds) {
        this.groupIds = groupIds;
    }

    public void setShowCustomFields(final boolean showCustomFields) {
        this.showCustomFields = showCustomFields;
    }

    public void setShowImages(final boolean showImages) {
        this.showImages = showImages;
    }

    public void setWithImagesOnly(final boolean withImagesOnly) {
        this.withImagesOnly = withImagesOnly;
    }

    @Override
    public String toString() {
        return "AbstractMemberSearchParameters [fields=" + fields + ", groupFilterIds=" + groupFilterIds + ", groupIds=" + groupIds + ", showCustomFields=" + showCustomFields + ", showImages=" + showImages + ", withImagesOnly=" + withImagesOnly + ", " + super.toString() + "]";
    }
}

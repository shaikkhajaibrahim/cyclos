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
package mp.platform.cyclone.webservices.model;

import javax.xml.bind.annotation.XmlType;

/**
 * Custom field data for web services
 * @author luis
 */
@XmlType(name = "field")
public class FieldVO extends WebServicesEntityVO {

    /**
     * A custom field data type
     * @author luis
     */
    @XmlType(name = "type")
    public static enum FieldVOType {
        STRING, ENUMERATED, INTEGER, FLOAT, DATE, BOOLEAN, URL;
    }

    private static final long serialVersionUID = -1218562552352420468L;
    private String            displayName;
    private String            internalName;
    private FieldVOType       type;

    public String getDisplayName() {
        return displayName;
    }

    public String getInternalName() {
        return internalName;
    }

    public FieldVOType getType() {
        return type;
    }

    public boolean isEnumerated() {
        return type == FieldVOType.ENUMERATED;
    }

    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    public void setEnumerated(final boolean enumerated) {
        // Ignored. This setter exists due to a bug on XFire
    }

    public void setInternalName(final String internalName) {
        this.internalName = internalName;
    }

    public void setType(final FieldVOType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "FieldVO(displayName=" + displayName + ", internalName=" + internalName + "type=" + type + ")";
    }
}

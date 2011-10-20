/*
 *
 *    This file is part of Cyclos.
 *
 *    Cyclos is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    Cyclos is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with Cyclos; if not, write to the Free Software
 *    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 *  
 */

package nl.strohalm.cyclos.controls.members;

import nl.strohalm.cyclos.entities.customization.fields.CustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.services.customization.CustomFieldService;
import nl.strohalm.cyclos.utils.SettingsHelper;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

public class MemberUtils {

    public static void setFullNameIfNeeded(Member member, HttpServletRequest httpServletRequest, CustomFieldService customFieldService) {
            String fullNameExpression = SettingsHelper.getLocalSettings(httpServletRequest).getFullNameExpression();
            if (!StringUtils.isEmpty(fullNameExpression)) {
                String fullName = prepareFullName(member.getCustomValues(), fullNameExpression, customFieldService);
                member.setName(fullName);
            }
        }

    private static String prepareFullName(Collection<MemberCustomFieldValue> fields,
                                          String fullNameExpression, CustomFieldService customFieldService) {
        String fullName = fullNameExpression;
        for (MemberCustomFieldValue value : fields) {
            CustomField cf = customFieldService.load(value.getField().getId());
            String fieldValue = value.getValue() == null ? "" : value.getValue();
            fullName = fullName.replaceAll("#"+cf.getInternalName()+"#",fieldValue);
        }
        return fullName.replaceAll("#[^#]*#", "");
    }
    
}

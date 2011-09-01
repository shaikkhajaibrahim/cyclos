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
package nl.strohalm.cyclos.dao.members;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import nl.strohalm.cyclos.entities.customization.fields.CustomFieldValue;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomField;
import nl.strohalm.cyclos.entities.customization.fields.MemberCustomFieldValue;
import nl.strohalm.cyclos.entities.members.Member;
import nl.strohalm.cyclos.utils.lucene.DocumentBuilder;

import org.apache.lucene.document.Document;

/**
 * Maps {@link Member}s to lucene {@link Document}s
 * 
 * @author luis
 */
public class MemberDocumentMapper extends ElementDocumentMapper<Member> {

    @Override
    protected boolean indexEmail(final Member member) {
        return super.indexEmail(member) && !member.isHideEmail();
    }

    @Override
    protected void process(final DocumentBuilder document, final Member member) {
        final boolean hasImages = ((Number) getSession().createFilter(member.getImages(), "select count(*)").uniqueResult()).intValue() > 0;

        super.process(document, member);
        document.add("activationDate", member.getActivationDate());
        document.add("broker", member.getBroker());
        document.add("hasImages", hasImages);
    }

    @Override
    protected Collection<? extends CustomFieldValue> resolveFieldValues(final Member member) {
        final Collection<MemberCustomFieldValue> fieldValues = new ArrayList<MemberCustomFieldValue>(member.getCustomValues());
        for (final Iterator<MemberCustomFieldValue> iterator = fieldValues.iterator(); iterator.hasNext();) {
            final MemberCustomFieldValue fieldValue = iterator.next();
            final MemberCustomField field = fetch(fieldValue.getField());
            if (fieldValue.isHidden()) {
                // Remove hidden fields
                iterator.remove();
            } else if (field.getIndexing() == MemberCustomField.Indexing.NONE) {
                // Remove field which are not indexed
                iterator.remove();
            }
        }
        return fieldValues;
    }

}

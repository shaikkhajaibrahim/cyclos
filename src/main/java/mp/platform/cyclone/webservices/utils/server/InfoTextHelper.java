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
package mp.platform.cyclone.webservices.utils.server;

import java.util.ArrayList;
import java.util.List;

import nl.strohalm.cyclos.entities.infotexts.InfoText;
import nl.strohalm.cyclos.entities.infotexts.InfoTextQuery;
import nl.strohalm.cyclos.utils.Period;
import nl.strohalm.cyclos.utils.query.Page;
import mp.platform.cyclone.webservices.infotexts.InfoTextResultPage;
import mp.platform.cyclone.webservices.infotexts.InfoTextSearchParameters;
import mp.platform.cyclone.webservices.model.InfoTextVO;

/**
 * Contains utility methods to handle custom fields
 * @author luis
 */
public class InfoTextHelper {

    private QueryHelper queryHelper;

    public void setQueryHelper(final QueryHelper queryHelper) {
        this.queryHelper = queryHelper;
    }

    public InfoTextQuery toQuery(final InfoTextSearchParameters params) {
        if (params == null) {
            return null;
        }
        final InfoTextQuery query = new InfoTextQuery();
        queryHelper.fill(params, query);
        query.setAlias(params.getAlias());
        query.setKeywords(params.getKeywords());
        return query;
    }

    public InfoTextResultPage toResultPage(final List<InfoText> list) {
        final InfoTextResultPage page = new InfoTextResultPage();
        if (list instanceof Page<?>) {
            final Page<InfoText> infoTextPage = (Page<InfoText>) list;
            page.setCurrentPage(infoTextPage.getCurrentPage());
            page.setTotalCount(infoTextPage.getTotalCount());
        }
        final List<InfoTextVO> vos = new ArrayList<InfoTextVO>();
        for (int i = 0; i < list.size(); i++) {
            final InfoTextVO vo = toVO(list.get(i));
            if (vo != null) {
                vos.add(vo);
            }
        }
        page.setInfoTexts(vos);
        return page;
    }

    public InfoTextVO toVO(final InfoText infoText) {
        final InfoTextVO vo = new InfoTextVO();
        vo.setId(infoText.getId());
        vo.setSubject(infoText.getSubject());
        vo.setBody(infoText.getBody());
        final Period validity = infoText.getValidity();
        vo.setValidFrom(validity == null ? null : validity.getBegin());
        vo.setValidTo(validity == null ? null : validity.getEnd());
        return vo;
    }

}

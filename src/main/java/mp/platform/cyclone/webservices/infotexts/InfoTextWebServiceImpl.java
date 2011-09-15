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
package mp.platform.cyclone.webservices.infotexts;

import java.util.List;

import javax.jws.WebService;

import nl.strohalm.cyclos.entities.infotexts.InfoText;
import nl.strohalm.cyclos.entities.infotexts.InfoTextQuery;
import nl.strohalm.cyclos.services.infotexts.InfoTextService;
import mp.platform.cyclone.webservices.model.InfoTextVO;
import mp.platform.cyclone.webservices.utils.server.InfoTextHelper;

/**
 * Implementation for info text web service
 * @author luis
 */
@WebService(name = "infoTexts", serviceName = "infoTexts")
public class InfoTextWebServiceImpl implements InfoTextWebService {

    private InfoTextHelper  infoTextHelper;
    private InfoTextService infoTextService;

    public InfoTextVO loadByAlias(final String alias) {
        final InfoText infoText = infoTextService.loadByAliasForWebServices(alias);
        return infoTextHelper.toVO(infoText);
    }

    public InfoTextVO loadById(final Long id) {
        final InfoText infoText = infoTextService.loadForWebServices(id);
        return infoTextHelper.toVO(infoText);
    }

    public InfoTextResultPage search(final InfoTextSearchParameters params) {
        InfoTextQuery query = infoTextHelper.toQuery(params);
        if (query == null) {
            query = new InfoTextQuery();
        }
        final List<InfoText> list = infoTextService.searchForWebServices(query);
        return infoTextHelper.toResultPage(list);
    }

    public void setInfoTextHelper(final InfoTextHelper infoTextHelper) {
        this.infoTextHelper = infoTextHelper;
    }

    public void setInfoTextService(final InfoTextService infoTextService) {
        this.infoTextService = infoTextService;
    }

}

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
package nl.strohalm.cyclos.services.fetch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.EntityReference;
import nl.strohalm.cyclos.entities.Relationship;

/**
 * Implementation for fetch service.
 * @author luis
 */
public class FetchServiceImpl implements FetchService {

    private FetchDAO fetchDao;

    public void clearCache() {
        fetchDao.clearCache();
    }

    public <E extends Entity> Collection<E> fetch(final Collection<E> entities, final Relationship... fetch) {
        if (entities == null) {
            return null;
        }
        final Collection<E> toReturn = new ArrayList<E>();
        for (E entity : entities) {
            entity = fetch(entity, fetch);
            toReturn.add(entity);
        }
        return toReturn;
    }

    public <E extends Entity> E fetch(final E entity, final Relationship... fetch) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return fetchDao.fetch(entity, fetch);
    }

    public FetchDAO getFetchDao() {
        return fetchDao;
    }

    public boolean isInitialized(final Object value) {
        if (value == null) {
            return false;
        }
        if (!(value instanceof Entity) && !(value instanceof Collection<?>) && !(value instanceof Map<?, ?>)) {
            return true;
        }
        if (value instanceof EntityReference) {
            return false;
        }
        return fetchDao.isInitialized(value);
    }

    public <E extends Entity> E reload(final E entity, final Relationship... fetch) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return fetchDao.reload(entity, fetch);
    }

    public void removeFromCache(final Entity entity) {
        fetchDao.removeFromCache(entity);
    }

    public void setFetchDao(final FetchDAO fetchDAO) {
        fetchDao = fetchDAO;
    }
}

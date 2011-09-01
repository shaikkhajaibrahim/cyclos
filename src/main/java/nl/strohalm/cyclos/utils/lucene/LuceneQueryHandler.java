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
package nl.strohalm.cyclos.utils.lucene;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.dao.exceptions.EntityNotFoundException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.utils.DataIteratorHelper;
import nl.strohalm.cyclos.utils.query.IteratorListImpl;
import nl.strohalm.cyclos.utils.query.PageImpl;
import nl.strohalm.cyclos.utils.query.PageParameters;
import nl.strohalm.cyclos.utils.query.QueryParameters.ResultType;
import nl.strohalm.cyclos.utils.validation.ValidationException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;

/**
 * Handler for entity queries using Lucene
 * @author luis
 */
public class LuceneQueryHandler {

    private IndexHandler indexHandler;
    private FetchDAO     fetchDao;

    /**
     * Executes a lucene query
     */
    public <E extends Entity & Indexable> List<E> executeQuery(final Class<E> entityType, final Query query, Filter filter, final ResultType resultType, final PageParameters pageParameters, final Relationship... fetch) {
        if (filter instanceof Filters && !((Filters) filter).isValid()) {
            filter = null;
        }
        switch (resultType) {
            case ITERATOR:
                return iterator(entityType, query, filter, pageParameters, fetch);
            default:
                return listOrPage(entityType, query, filter, resultType, pageParameters, fetch);
        }
    }

    public void setFetchDao(final FetchDAO fetchDao) {
        this.fetchDao = fetchDao;
    }

    public void setIndexHandler(final IndexHandler indexHandler) {
        this.indexHandler = indexHandler;
    }

    private <E extends Entity & Indexable> List<E> iterator(final Class<E> entityType, final Query query, final Filter filter, final PageParameters pageParameters, final Relationship... fetch) {
        IndexSearcher searcher = null;
        // Prepare the parameters
        IndexReader reader;
        try {
            reader = indexHandler.getReader(entityType);
        } catch (final DaoException e) {
            // Probably index files don't exist
            return new IteratorListImpl<E>(Collections.<E> emptyList().iterator());
        }
        final int firstResult = pageParameters == null ? 0 : pageParameters.getFirstResult();
        int maxResults = pageParameters == null ? 0 : pageParameters.getMaxResults();
        if (maxResults == 0) {
            maxResults = -1;
        }

        try {
            // Run the search
            searcher = new IndexSearcher(reader);
            final IteratorHitCollector hitCollector = new IteratorHitCollector(firstResult, maxResults);
            searcher.search(query, filter, hitCollector);

            // Open the iterator
            final Iterator<E> iterator = hitCollector.toIterator(reader, entityType, fetchDao, fetch);
            DataIteratorHelper.registerOpen(iterator);

            // Wrap the iterator
            return new IteratorListImpl<E>(iterator);

        } catch (final Exception e) {
            throw new DaoException(e);
        } finally {
            try {
                searcher.close();
            } catch (final Exception e) {
                // Silently ignore
            }
        }
    }

    private <E extends Entity & Indexable> List<E> listOrPage(final Class<E> entityType, final Query query, final Filter filter, final ResultType resultType, final PageParameters pageParameters, final Relationship... fetch) {
        IndexSearcher searcher = null;
        // Prepare the parameters
        IndexReader reader;
        try {
            reader = indexHandler.getReader(entityType);
        } catch (final DaoException e) {
            // Probably index files don't exist
            return Collections.emptyList();
        }
        final int firstResult = pageParameters == null ? 0 : pageParameters.getFirstResult();
        final int maxResults = pageParameters == null ? Integer.MAX_VALUE : pageParameters.getMaxResults();
        try {
            // Run the search
            searcher = new IndexSearcher(reader);
            final ListHitCollector hitCollector = new ListHitCollector(firstResult, maxResults);
            searcher.search(query, filter, hitCollector);
            List<E> list = hitCollector.toList(reader, entityType, fetchDao, fetch);

            // When result type is page, get the additional data
            if (resultType == ResultType.PAGE) {
                list = new PageImpl<E>(pageParameters, hitCollector.getTotalDocuments(), list);
            }
            return list;
        } catch (final EntityNotFoundException e) {
            throw new ValidationException("general.error.indexedRecordNotFound");
        } catch (final Exception e) {
            throw new DaoException(e);
        } finally {
            // Close resources
            try {
                searcher.close();
            } catch (final Exception e) {
                // Silently ignore
            }
            try {
                reader.close();
            } catch (final Exception e) {
                // Silently ignore
            }
        }
    }

}

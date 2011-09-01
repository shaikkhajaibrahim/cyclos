package nl.strohalm.cyclos.utils.lucene;

import java.util.ArrayList;
import java.util.List;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.dao.exceptions.DaoException;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.utils.EntityHelper;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;

/**
 * A {@link Collector} which fills a list
 * @author luis
 */
public class ListHitCollector extends AbstractHitCollector {

    public ListHitCollector(final int firstResult, final int maxResults) {
        super(firstResult, maxResults);
    }

    /**
     * Transforms the collected hits into a list of entities
     */
    public <E extends Entity & Indexable> List<E> toList(final IndexReader reader, final Class<E> entityType, final FetchDAO fetchDao, final Relationship... fetch) {
        final List<Integer> documents = getDocuments();
        final List<E> list = new ArrayList<E>(documents.size());
        for (final Integer documentNumber : documents) {
            Document document;
            try {
                document = reader.document(documentNumber);
            } catch (final Exception e) {
                throw new DaoException(e);
            }
            final Long id = Long.parseLong(document.get("id"));
            final E entity = EntityHelper.reference(entityType, id);
            list.add(fetchDao.fetch(entity, fetch));
        }
        return list;
    }
}
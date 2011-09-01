package nl.strohalm.cyclos.utils.lucene;

import java.util.Iterator;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Indexable;
import nl.strohalm.cyclos.entities.Relationship;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;

/**
 * A {@link Collector} which uses an iterator
 * @author luis
 */
public class IteratorHitCollector extends AbstractHitCollector {

    public IteratorHitCollector(final int firstResult, final int maxResults) {
        super(firstResult, maxResults);
    }

    /**
     * Transforms the collected hits into an iterator
     */
    public <E extends Entity & Indexable> Iterator<E> toIterator(final IndexReader reader, final Class<E> entityType, final FetchDAO fetchDao, final Relationship... fetch) {
        final Iterator<Integer> documentIterator = getDocuments().iterator();
        return new IndexIterator<E>(entityType, documentIterator, reader, fetchDao, fetch);
    }

}
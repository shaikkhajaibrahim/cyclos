package nl.strohalm.cyclos.utils.lucene;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Base implementation for {@link Collector}s
 * @author luis
 */
public abstract class AbstractHitCollector extends Collector {
    private int           firstResult;
    private int           maxResults;
    private List<Integer> documents = new ArrayList<Integer>();
    private int           current   = -1;
    private int           base      = 0;

    public AbstractHitCollector(final int firstResult, final int maxResults) {
        this.firstResult = firstResult;
        this.maxResults = firstResult + maxResults;
    }

    @Override
    public boolean acceptsDocsOutOfOrder() {
        return false;
    }

    @Override
    public void collect(final int doc) {
        current++;
        if (current < firstResult || (maxResults > 0 && current >= maxResults)) {
            return;
        }
        documents.add(doc + base);
    }

    /**
     * Return the document indexes
     */
    public List<Integer> getDocuments() {
        return documents;
    }

    /**
     * Returns the total documents
     */
    public int getTotalDocuments() {
        return current + 1;
    }

    @Override
    public void setNextReader(final IndexReader reader, final int docBase) throws IOException {
        base = docBase;
    }

    @Override
    public void setScorer(final Scorer scorer) throws IOException {
    }
}
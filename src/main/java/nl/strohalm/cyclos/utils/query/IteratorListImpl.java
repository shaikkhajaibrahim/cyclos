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
package nl.strohalm.cyclos.utils.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import nl.strohalm.cyclos.dao.FetchDAO;
import nl.strohalm.cyclos.entities.Entity;
import nl.strohalm.cyclos.entities.Relationship;
import nl.strohalm.cyclos.utils.DataIteratorHelper;

/**
 * Basic implementation for iterator lists, using a delegate iterator
 * @author luis
 */
public class IteratorListImpl<E> implements IteratorList<E> {

    protected Iterator<E>  iterator;
    private final boolean  empty;
    private FetchDAO       fetchDao;
    private Relationship[] fetch;

    public IteratorListImpl(final Iterator<E> iterator) {
        this.iterator = iterator;
        this.empty = !iterator.hasNext();
    }

    public IteratorListImpl(final Iterator<E> iterator, final FetchDAO fetchDao, final Relationship... fetch) {
        this(iterator);
        this.fetchDao = fetchDao;
        this.fetch = fetch;
    }

    public boolean add(final E o) {
        throw new UnsupportedOperationException();
    }

    public void add(final int index, final E element) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(final int index, final Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(final Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public E get(final int index) {
        throw new UnsupportedOperationException();
    }

    public Iterator<E> getIterator() {
        return iterator;
    }

    public boolean hasNext() {
        final boolean hasNext = iterator.hasNext();

        if (!hasNext) {
            // Instantly close the underlying iterator after the last result
            DataIteratorHelper.close(iterator);
        }

        return hasNext;
    }

    public int indexOf(final Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return empty;
    }

    public Iterator<E> iterator() {
        return this;
    }

    public int lastIndexOf(final Object o) {
        throw new UnsupportedOperationException();
    }

    public ListIterator<E> listIterator() {
        throw new UnsupportedOperationException();
    }

    public ListIterator<E> listIterator(final int index) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    public E next() {
        final E next = iterator.next();
        if (fetchDao != null) {
            return (E) fetchDao.fetch((Entity) next, fetch);
        } else {
            return next;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }

    public E remove(final int index) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public E set(final int index, final E element) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public List<E> subList(final int fromIndex, final int toIndex) {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        final List<E> list = new LinkedList<E>();
        for (final E e : this) {
            list.add(e);
        }
        return list.toArray();
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(final T[] a) {
        final List<T> list = new LinkedList<T>();
        for (final E e : this) {
            list.add((T) e);
        }
        return list.toArray(a);
    }
}

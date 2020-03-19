/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.util;

import com.cburch.draw.model.CanvasObject;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class MatchingSet<E extends CanvasObject> extends AbstractSet<E> {

    private HashSet<Member<E>> set;

    public MatchingSet() {
        set = new HashSet<>();
    }

    public MatchingSet(Collection<E> initialContents) {
        set = new HashSet<>(initialContents.size());
        for (E value : initialContents) {
            set.add(new Member<>(value));
        }
    }

    @Override
    public boolean add(E value) {
        return set.add(new Member<>(value));
    }

    @Override
    public boolean remove(Object value) {
        @SuppressWarnings("unchecked")
        E eValue = (E) value;
        return set.remove(new Member<>(eValue));
    }

    @Override
    public boolean contains(Object value) {
        @SuppressWarnings("unchecked")
        E eValue = (E) value;
        return set.contains(new Member<>(eValue));
    }

    @Override
    public Iterator<E> iterator() {
        return new MatchIterator<>(set.iterator());
    }

    @Override
    public int size() {
        return set.size();
    }

    private static class Member<E extends CanvasObject> {

        E value;

        private Member(E value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object other) {
            @SuppressWarnings("unchecked")
            Member<E> that = (Member<E>) other;
            return this.value.matches(that.value);
        }

        @Override
        public int hashCode() {
            return value.matchesHashCode();
        }
    }

    private static class MatchIterator<E extends CanvasObject> implements Iterator<E> {

        private Iterator<Member<E>> iterator;

        private MatchIterator(Iterator<Member<E>> iterator) {
            this.iterator = iterator;
        }

        public boolean hasNext() {
            return iterator.hasNext();
        }

        public E next() {
            return iterator.next().value;
        }

        public void remove() {
            iterator.remove();
        }

    }

}

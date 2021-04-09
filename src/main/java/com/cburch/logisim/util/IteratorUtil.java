/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorUtil {

    public static Iterator<?> EMPTY_ITERATOR = new EmptyIterator<>();

    public static <E> Iterator<E> emptyIterator() {
        return new EmptyIterator<>();
    }

    public static <E> Iterator<E> createUnitIterator(E data) {
        return new UnitIterator<>(data);
    }

    public static <E> Iterator<E> createArrayIterator(E[] data) {
        return new ArrayIterator<>(data);
    }

    public static <E> Iterator<E> createJoinedIterator(Iterator<? extends E> i0,
        Iterator<? extends E> i1) {
        if (!i0.hasNext()) {
            @SuppressWarnings("unchecked")
            Iterator<E> ret = (Iterator<E>) i1;
            return ret;
        } else if (!i1.hasNext()) {
            @SuppressWarnings("unchecked")
            Iterator<E> ret = (Iterator<E>) i0;
            return ret;
        } else {
            return new IteratorUnion<>(i0, i1);
        }
    }

    private static class EmptyIterator<E> implements Iterator<E> {

        private EmptyIterator() {
        }

        public E next() {
            throw new NoSuchElementException();
        }

        public boolean hasNext() {
            return false;
        }

        public void remove() {
            throw new UnsupportedOperationException("EmptyIterator.remove");
        }
    }

    private static class UnitIterator<E> implements Iterator<E> {

        private final E data;
        private boolean taken = false;

        private UnitIterator(E data) {
            this.data = data;
        }

        public E next() {
            if (taken) {
                throw new NoSuchElementException();
            }
            taken = true;
            return data;
        }

        public boolean hasNext() {
            return !taken;
        }

        public void remove() {
            throw new UnsupportedOperationException("UnitIterator.remove");
        }
    }

    private static class ArrayIterator<E> implements Iterator<E> {

        private final E[] data;
        private int i = -1;

        private ArrayIterator(E[] data) {
            this.data = data;
        }

        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            i++;
            return data[i];
        }

        public boolean hasNext() {
            return i + 1 < data.length;
        }

        public void remove() {
            throw new UnsupportedOperationException("ArrayIterator.remove");
        }
    }

    private static class IteratorUnion<E> implements Iterator<E> {

        Iterator<? extends E> current;
        Iterator<? extends E> next;

        private IteratorUnion(Iterator<? extends E> current, Iterator<? extends E> next) {
            this.current = current;
            this.next = next;
        }

        public E next() {
            if (!current.hasNext()) {
                if (next == null) {
                    throw new NoSuchElementException();
                }
                current = next;
                if (!current.hasNext()) {
                    throw new NoSuchElementException();
                }
            }
            return current.next();
        }

        public boolean hasNext() {
            return current.hasNext() || (next != null && next.hasNext());
        }

        public void remove() {
            current.remove();
        }
    }

}

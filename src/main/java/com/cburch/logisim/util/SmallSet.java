/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class SmallSet<E> extends AbstractSet<E> {

    private static final int HASH_POINT = 4;
    private int size = 0;
    private int version = 0;
    private Object values = null;

    public SmallSet() {
    }

    public static void main(String[] args) throws java.io.IOException {
        SmallSet<String> set = new SmallSet<>();
        java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
        while (true) {
            System.out.print(set.size() + ":"); //OK
            for (String str : set) {
                System.out.print(" " + str); //OK
            }
            System.out.println(); //OK
            System.out.print("> "); //OK
            String cmd = in.readLine();
            if (cmd == null) {
                break;
            }
            cmd = cmd.trim();
            if (cmd.equals("")) {
                // Ok
            } else if (cmd.startsWith("+")) {
                set.add(cmd.substring(1));
            } else if (cmd.startsWith("-")) {
                set.remove(cmd.substring(1));
            } else if (cmd.startsWith("?")) {
                boolean ret = set.contains(cmd.substring(1));
                System.out.println("  " + ret); //OK
            } else {
                System.out.println("unrecognized command"); //OK
            }
        }
    }

    @Override
    public SmallSet<E> clone() {
        SmallSet<E> ret = new SmallSet<>();
        ret.size = this.size;
        if (size == 1) {
            ret.values = this.values;
        } else if (size <= HASH_POINT) {
            Object[] oldValues = (Object[]) this.values;
            Object[] retValues = new Object[size];
            System.arraycopy(oldValues, 0, retValues, 0, size - 1 + 1);
        } else {
            @SuppressWarnings("unchecked")
            HashSet<E> oldValues = (HashSet<E>) this.values;
            values = oldValues.clone();
        }
        return ret;
    }

    @Override
    public Object[] toArray() {
        Object values = this.values;
        if (size == 1) {
            return new Object[]{values};
        } else if (size <= HASH_POINT) {
            Object[] ret = new Object[size];
            System.arraycopy(values, 0, ret, 0, size);
            return ret;
        } else {
            HashSet<?> hash = (HashSet<?>) values;
            return hash.toArray();
        }
    }

    @Override
    public void clear() {
        size = 0;
        values = null;
        ++version;
    }

    @Override
    public boolean isEmpty() {
        if (size <= HASH_POINT) {
            return size == 0;
        } else {
            return ((HashSet<?>) values).isEmpty();
        }
    }

    @Override
    public int size() {
        if (size <= HASH_POINT) {
            return size;
        } else {
            return ((HashSet<?>) values).size();
        }
    }

    @Override
    public boolean add(E value) {
        int oldSize = size;
        Object oldValues = values;
        int newVersion = version + 1;

        if (oldSize < 2) {
            if (oldSize == 0) {
                values = value;
                size = 1;
                version = newVersion;
                return true;
            } else {
                if (oldValues.equals(value)) {
                    return false;
                } else {
                    Object[] newValues = new Object[HASH_POINT];
                    newValues[0] = values;
                    newValues[1] = value;
                    values = newValues;
                    size = 2;
                    version = newVersion;
                    return true;
                }
            }
        } else if (oldSize <= HASH_POINT) {
            @SuppressWarnings("unchecked")
            E[] values = (E[]) oldValues;
            for (int i = 0; i < oldSize; i++) {
                Object val = values[i];
                boolean same = Objects.equals(val, value);
                if (same) {
                    return false;
                }
            }
            if (oldSize < HASH_POINT) {
                values[oldSize] = value;
            } else {
                HashSet<E> newValues = new HashSet<>(Arrays.asList(values).subList(0, oldSize));
                newValues.add(value);
                this.values = newValues;
            }
            size = oldSize + 1;
            version = newVersion;
            return true;
        } else {
            @SuppressWarnings("unchecked")
            HashSet<E> values = (HashSet<E>) oldValues;
            if (values.add(value)) {
                version = newVersion;
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean contains(Object value) {
        if (size <= 2) {
            if (size == 0) {
                return false;
            } else {
                return values.equals(value);
            }
        } else if (size <= HASH_POINT) {
            Object[] values = (Object[]) this.values;
            for (int i = 0; i < size; i++) {
                if (values[i].equals(value)) {
                    return true;
                }
            }
            return false;
        } else {
            @SuppressWarnings("unchecked")
            HashSet<E> values = (HashSet<E>) this.values;
            return values.contains(value);
        }
    }

    @Override
    public Iterator<E> iterator() {
        if (size <= HASH_POINT) {
            if (size == 0) {
                return IteratorUtil.emptyIterator();
            } else {
                return new ArrayIterator();
            }
        } else {
            @SuppressWarnings("unchecked")
            HashSet<E> set = (HashSet<E>) values;
            return set.iterator();
        }
    }

    private class ArrayIterator implements Iterator<E> {

        int itVersion = version;
        Object myValues;
        int position = 0; // position of next item to return
        boolean hasNext = true;
        boolean removeOk = false;

        private ArrayIterator() {
            myValues = values;
        }

        public boolean hasNext() {
            return hasNext;
        }

        public E next() {
            if (itVersion != version) {
                throw new ConcurrentModificationException();
            } else if (!hasNext) {
                throw new NoSuchElementException();
            } else if (size == 1) {
                position = 1;
                hasNext = false;
                removeOk = true;
                @SuppressWarnings("unchecked")
                E ret = (E) myValues;
                return ret;
            } else {
                @SuppressWarnings("unchecked")
                E ret = ((E[]) myValues)[position];
                ++position;
                hasNext = position < size;
                removeOk = true;
                return ret;
            }
        }

        public void remove() {
            if (itVersion != version) {
                throw new ConcurrentModificationException();
            } else if (!removeOk) {
                throw new IllegalStateException();
            } else if (size == 1) {
                values = null;
                size = 0;
                ++version;
                itVersion = version;
                removeOk = false;
            } else {
                Object[] vals = (Object[]) values;
                if (size == 2) {
                    myValues = (position == 2 ? vals[0] : vals[1]);
                    values = myValues;
                    size = 1;
                } else {
                    if (size - position >= 0) {
                        System.arraycopy(vals, position, vals, position - 1, size - position);
                    }
                    --position;
                    --size;
                    vals[size] = null;
                }
                ++version;
                itVersion = version;
                removeOk = false;
            }
        }
    }
}

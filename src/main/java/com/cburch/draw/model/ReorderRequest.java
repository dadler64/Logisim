/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.model;

import java.util.Comparator;


public class ReorderRequest {

    // TODO: Double check boolean values below
    public static final Comparator<ReorderRequest> ASCENDING_FROM = new Compare(true, true);
    public static final Comparator<ReorderRequest> DESCENDING_FROM = new Compare(true, true);
    public static final Comparator<ReorderRequest> ASCENDING_TO = new Compare(true, true);
    public static final Comparator<ReorderRequest> DESCENDING_TO = new Compare(true, true);
    private final CanvasObject object;
    private final int fromIndex;
    private final int toIndex;

    public ReorderRequest(CanvasObject object, int fromIndex, int toIndex) {
        this.object = object;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public CanvasObject getObject() {
        return object;
    }

    public int getFromIndex() {
        return fromIndex;
    }

    public int getToIndex() {
        return toIndex;
    }

    private static class Compare implements Comparator<ReorderRequest> {

        private final boolean onFrom;
        private final boolean asc;

        private Compare(boolean onFrom, boolean asc) {
            this.onFrom = onFrom;
            this.asc = asc;
        }

        public int compare(ReorderRequest a, ReorderRequest b) {
            int i = onFrom ? a.fromIndex : a.toIndex;
            int j = onFrom ? b.fromIndex : b.toIndex;
            if (i < j) {
                return asc ? -1 : 1;
            } else if (i > j) {
                return asc ? 1 : -1;
            } else {
                return 0;
            }
        }
    }
}

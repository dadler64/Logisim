/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;

class RomContentsListener implements HexModelListener {

    Project project;
    boolean enabled = true;

    RomContentsListener(Project project) {
        this.project = project;
    }

    void setEnabled(boolean value) {
        enabled = value;
    }

    public void metaInfoChanged(HexModel source) {
        // ignore - this can only come from an already-registered
        // action
    }

    public void bytesChanged(HexModel source, long start, long numBytes, int[] oldValues) {
        if (enabled && project != null && oldValues != null) {
            // this change needs to be logged in the undo log
            int[] newValues = new int[oldValues.length];
            for (int i = 0; i < newValues.length; i++) {
                newValues[i] = source.get(start + i);
            }
            project.doAction(new Change(this, (MemContents) source, start, oldValues, newValues));
        }
    }

    private static class Change extends Action {

        private final RomContentsListener source;
        private final MemContents contents;
        private final long start;
        private final int[] oldValues;
        private final int[] newValues;
        private boolean completed = true;

        Change(RomContentsListener source, MemContents contents, long start, int[] oldValues, int[] newValues) {
            this.source = source;
            this.contents = contents;
            this.start = start;
            this.oldValues = oldValues;
            this.newValues = newValues;
        }

        @Override
        public String getName() {
            return Strings.get("romChangeAction");
        }

        @Override
        public void doIt(Project project) {
            if (!completed) {
                completed = true;
                try {
                    source.setEnabled(false);
                    contents.set(start, newValues);
                } finally {
                    source.setEnabled(true);
                }
            }
        }

        @Override
        public void undo(Project project) {
            if (completed) {
                completed = false;
                try {
                    source.setEnabled(false);
                    contents.set(start, oldValues);
                } finally {
                    source.setEnabled(true);
                }
            }
        }

        @Override
        public boolean shouldAppendTo(Action other) {
            if (other instanceof Change) {
                Change change = (Change) other;
                long oEnd = change.start + change.newValues.length;
                long end = start + newValues.length;
                if (oEnd >= start && end >= change.start) {
                    return true;
                }
            }
            return super.shouldAppendTo(other);
        }

        @Override
        public Action append(Action other) {
            if (other instanceof Change) {
                Change change = (Change) other;
                long oEnd = change.start + change.newValues.length;
                long end = start + newValues.length;
                if (oEnd >= start && end >= change.start) {
                    long nStart = Math.min(start, change.start);
                    long nEnd = Math.max(end, oEnd);
                    int[] nOld = new int[(int) (nEnd - nStart)];
                    int[] nNew = new int[(int) (nEnd - nStart)];
                    System.arraycopy(change.oldValues, 0, nOld, (int) (change.start - nStart), change.oldValues.length);
                    System.arraycopy(oldValues, 0, nOld, (int) (start - nStart), oldValues.length);
                    System.arraycopy(newValues, 0, nNew, (int) (start - nStart), newValues.length);
                    System.arraycopy(change.newValues, 0, nNew, (int) (change.start - nStart), change.newValues.length);
                    return new Change(source, contents, nStart, nOld, nNew);
                }
            }
            return super.append(other);
        }
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.undo;

import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.LinkedList;

public class UndoLog {

    private static final int MAX_UNDO_SIZE = 64;

    private final EventSourceWeakSupport<UndoLogListener> listeners;
    private final LinkedList<Action> undoLog;
    private final LinkedList<Action> redoLog;
    private int modCount;

    public UndoLog() {
        this.listeners = new EventSourceWeakSupport<>();
        this.undoLog = new LinkedList<>();
        this.redoLog = new LinkedList<>();
        this.modCount = 0;
    }

    //
    // listening methods
    //
    public void addProjectListener(UndoLogListener listener) {
        listeners.add(listener);
    }

    public void removeProjectListener(UndoLogListener listener) {
        listeners.remove(listener);
    }

    private void fireEvent(int action, Action actionObject) {
        UndoLogEvent event = null;
        for (UndoLogListener listener : listeners) {
            if (event == null) {
                event = new UndoLogEvent(this, action, actionObject);
            }
            listener.undoLogChanged(event);
        }
    }

    //
    // accessor methods
    //
    public Action getUndoAction() {
        if (undoLog.size() == 0) {
            return null;
        } else {
            return undoLog.getLast();
        }
    }

    public Action getRedoAction() {
        if (redoLog.size() == 0) {
            return null;
        } else {
            return redoLog.getLast();
        }
    }

    public boolean isModified() {
        return modCount != 0;
    }

    //
    // mutator methods
    //
    public void doAction(Action action) {
        if (action == null) {
            return;
        }
        action.doIt();
        logAction(action);
    }

    private void logAction(Action action) {
        redoLog.clear();
        if (!undoLog.isEmpty()) {
            Action previousAction = undoLog.getLast();
            if (action.shouldAppendTo(previousAction)) {
                if (previousAction.isModification()) {
                    --modCount;
                }
                Action joinedAction = previousAction.append(action);
                if (joinedAction == null) {
                    fireEvent(UndoLogEvent.ACTION_DONE, action);
                    return;
                }
                action = joinedAction;
            }
            while (undoLog.size() > MAX_UNDO_SIZE) {
                undoLog.removeFirst();
            }
        }
        undoLog.add(action);
        if (action.isModification()) {
            ++modCount;
        }
        fireEvent(UndoLogEvent.ACTION_DONE, action);
    }

    public void undoAction() {
        if (undoLog.size() > 0) {
            Action action = undoLog.removeLast();
            if (action.isModification()) {
                --modCount;
            }
            action.undo();
            redoLog.add(action);
            fireEvent(UndoLogEvent.ACTION_UNDONE, action);
        }
    }

    public void redoAction() {
        if (redoLog.size() > 0) {
            Action action = redoLog.removeLast();
            if (action.isModification()) {
                ++modCount;
            }
            action.doIt();
            undoLog.add(action);
            fireEvent(UndoLogEvent.ACTION_DONE, action);
        }
    }

    public void clearModified() {
        modCount = 0;
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.io.File;
import java.util.HashMap;
import javax.swing.JFrame;

class Model {

    private final EventSourceWeakSupport<ModelListener> listeners;
    private final Selection selection;
    private final HashMap<SelectionItem, ValueLog> log;
    private boolean fileEnabled = false;
    private File file = null;
    private boolean fileHeader = true;
    private boolean selected = false;
    private LogThread logger = null;

    public Model(CircuitState circuitState) {
        listeners = new EventSourceWeakSupport<>();
        selection = new Selection(circuitState, this);
        log = new HashMap<>();
    }

    public boolean isSelected() {
        return selected;
    }

    public void addModelListener(ModelListener l) {
        listeners.add(l);
    }

    public void removeModelListener(ModelListener l) {
        listeners.remove(l);
    }

    public CircuitState getCircuitState() {
        return selection.getCircuitState();
    }

    public Selection getSelection() {
        return selection;
    }

    public ValueLog getValueLog(SelectionItem item) {
        ValueLog valueLog = log.get(item);
        if (valueLog == null && selection.indexOf(item) >= 0) {
            valueLog = new ValueLog();
            log.put(item, valueLog);
        }
        return valueLog;
    }

    public boolean isFileEnabled() {
        return fileEnabled;
    }

    public void setFileEnabled(boolean value) {
        if (fileEnabled == value) {
            return;
        }
        fileEnabled = value;
        fireFilePropertyChanged(new ModelEvent());
    }

    public File getFile() {
        return file;
    }

    public void setFile(File value) {
        if (file == null ? value == null : file.equals(value)) {
            return;
        }
        file = value;
        fileEnabled = file != null;
        fireFilePropertyChanged(new ModelEvent());
    }

    public boolean getFileHeader() {
        return fileHeader;
    }

    public void setFileHeader(boolean value) {
        if (fileHeader == value) {
            return;
        }
        fileHeader = value;
        fireFilePropertyChanged(new ModelEvent());
    }

    public void propagationCompleted() {
        CircuitState circuitState = getCircuitState();
        Value[] values = new Value[selection.size()];
        boolean changed = false;
        for (int i = selection.size() - 1; i >= 0; i--) {
            SelectionItem item = selection.get(i);
            values[i] = item.fetchValue(circuitState);
            if (!changed) {
                Value v = getValueLog(item).getLast();
                changed = v == null ? values[i] != null : !v.equals(values[i]);
            }
        }
        if (changed) {
            for (int i = selection.size() - 1; i >= 0; i--) {
                SelectionItem item = selection.get(i);
                getValueLog(item).append(values[i]);
            }
            fireEntryAdded(new ModelEvent(), values);
        }
    }

    public void setSelected(JFrame frame, boolean value) {
        if (selected == value) {
            return;
        }
        selected = value;
        if (selected) {
            logger = new LogThread(this);
            logger.start();
        } else {
            if (logger != null) {
                logger.cancel();
            }
            logger = null;
            fileEnabled = false;
        }
        fireFilePropertyChanged(new ModelEvent());
    }

    void fireSelectionChanged(ModelEvent e) {
        log.keySet().removeIf(i -> selection.indexOf(i) < 0);

        for (ModelListener l : listeners) {
            l.selectionChanged(e);
        }
    }

    private void fireEntryAdded(ModelEvent e, Value[] values) {
        for (ModelListener l : listeners) {
            l.entryAdded(e, values);
        }
    }

    private void fireFilePropertyChanged(ModelEvent e) {
        for (ModelListener l : listeners) {
            l.filePropertyChanged(e);
        }
    }
}

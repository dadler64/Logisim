/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.log;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Value;
import javax.swing.AbstractListModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

class SelectionList extends JList<Object> {

    private Selection selection;

    public SelectionList() {
        selection = null;
        setModel(new Model());
        setCellRenderer(new MyCellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    public void setSelection(Selection value) {
        if (selection != value) {
            Model model = (Model) getModel();
            if (selection != null) {
                selection.removeModelListener(model);
            }
            selection = value;
            if (selection != null) {
                selection.addModelListener(model);
            }
            model.selectionChanged(null);
        }
    }

    public void localeChanged() {
        repaint();
    }

    private static class MyCellRenderer extends DefaultListCellRenderer {

        @Override
        public java.awt.Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean hasFocus) {
            java.awt.Component component = super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            if (component instanceof JLabel && value instanceof SelectionItem) {
                JLabel label = (JLabel) component;
                SelectionItem item = (SelectionItem) value;
                Component comp = item.getComponent();
                label.setIcon(new ComponentIcon(comp));
                label.setText(item + " - " + item.getRadix());
            }
            return component;
        }
    }

    private class Model extends AbstractListModel<Object> implements ModelListener {

        public int getSize() {
            return selection == null ? 0 : selection.size();
        }

        public Object getElementAt(int index) {
            return selection.get(index);
        }

        public void selectionChanged(ModelEvent event) {
            fireContentsChanged(this, 0, getSize());
        }

        public void entryAdded(ModelEvent event, Value[] values) {
        }

        public void filePropertyChanged(ModelEvent event) {
        }
    }
}

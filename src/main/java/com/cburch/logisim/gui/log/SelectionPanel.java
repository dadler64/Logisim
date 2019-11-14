/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.log;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

class SelectionPanel extends LogPanel {

    private Listener listener = new Listener();
    private ComponentSelector selector;
    private JButton addToolButton;
    private JButton changeBaseButton;
    private JButton moveUpButton;
    private JButton moveDownButton;
    private JButton removeButton;
    private SelectionList list;

    public SelectionPanel(LogFrame window) {
        super(window);
        selector = new ComponentSelector(getModel());
        addToolButton = new JButton();
        changeBaseButton = new JButton();
        moveUpButton = new JButton();
        moveDownButton = new JButton();
        removeButton = new JButton();
        list = new SelectionList();
        list.setSelection(getSelection());

        JPanel panel = new JPanel(new GridLayout(5, 1));
        panel.add(addToolButton);
        panel.add(changeBaseButton);
        panel.add(moveUpButton);
        panel.add(moveDownButton);
        panel.add(removeButton);

        addToolButton.addActionListener(listener);
        changeBaseButton.addActionListener(listener);
        moveUpButton.addActionListener(listener);
        moveDownButton.addActionListener(listener);
        removeButton.addActionListener(listener);
        selector.addMouseListener(listener);
        selector.addTreeSelectionListener(listener);
        list.addListSelectionListener(listener);
        listener.computeEnabled();

        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        setLayout(layout);
        JScrollPane explorerPane = new JScrollPane(selector,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        JScrollPane listPane = new JScrollPane(list,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        layout.setConstraints(explorerPane, constraints);
        add(explorerPane);
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.NORTH;
        constraints.weightx = 0.0;
        layout.setConstraints(panel, constraints);
        add(panel);
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        layout.setConstraints(listPane, constraints);
        add(listPane);
    }

    @Override
    public String getTitle() {
        return Strings.get("selectionTab");
    }

    @Override
    public String getHelpText() {
        return Strings.get("selectionHelp");
    }

    @Override
    public void localeChanged() {
        addToolButton.setText(Strings.get("selectionAdd"));
        changeBaseButton.setText(Strings.get("selectionChangeBase"));
        moveUpButton.setText(Strings.get("selectionMoveUp"));
        moveDownButton.setText(Strings.get("selectionMoveDown"));
        removeButton.setText(Strings.get("selectionRemove"));
        selector.localeChanged();
        list.localeChanged();
    }

    @Override
    public void modelChanged(Model oldModel, Model newModel) {
        if (getModel() == null) {
            selector.setLogModel(newModel);
            list.setSelection(null);
        } else {
            selector.setLogModel(newModel);
            list.setSelection(getSelection());
        }
        listener.computeEnabled();
    }

    private class Listener extends MouseAdapter
            implements ActionListener, TreeSelectionListener,
            ListSelectionListener {

        @Override
        public void mouseClicked(MouseEvent mouseEvent) {
            if (mouseEvent.getClickCount() == 2) {
                TreePath path = selector.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
                if (path != null && listener != null) {
                    doAdd(selector.getSelectedItems());
                }
            }
        }

        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source.equals(addToolButton)) {
                doAdd(selector.getSelectedItems());
            } else if (source.equals(changeBaseButton)) {
                SelectionItem value = (SelectionItem) list.getSelectedValue();
                if (value != null) {
                    int radix = value.getRadix();
                    switch (radix) {
                        case 2:
                            value.setRadix(10);
                            break;
                        case 10:
                            value.setRadix(16);
                            break;
                        default:
                            value.setRadix(2);
                    }
                }
            } else if (source.equals(moveUpButton)) {
                doMove(-1);
            } else if (source.equals(moveDownButton)) {
                doMove(1);
            } else if (source.equals(removeButton)) {
                Selection selection = getSelection();
                @SuppressWarnings("deprecation")
                Object[] objectsToRemove = list.getSelectedValues();
                boolean changed = false;
                for (Object object : objectsToRemove) {
                    int index = selection.indexOf((SelectionItem) object);
                    if (index >= 0) {
                        selection.remove(index);
                        changed = true;
                    }
                }
                if (changed) {
                    list.clearSelection();
                }
            }
        }

        public void valueChanged(TreeSelectionEvent event) {
            computeEnabled();
        }

        public void valueChanged(ListSelectionEvent event) {
            computeEnabled();
        }

        private void computeEnabled() {
            int index = list.getSelectedIndex();
            addToolButton.setEnabled(selector.hasSelectedItems());
            changeBaseButton.setEnabled(index >= 0);
            moveUpButton.setEnabled(index > 0);
            moveDownButton.setEnabled(index >= 0 && index < list.getModel().getSize() - 1);
            removeButton.setEnabled(index >= 0);
        }

        private void doAdd(List<SelectionItem> selectedItems) {
            if (selectedItems != null && selectedItems.size() > 0) {
                SelectionItem last = null;
                for (SelectionItem item : selectedItems) {
                    getSelection().add(item);
                    last = item;
                }
                list.setSelectedValue(last, true);
            }
        }

        private void doMove(int delta) {
            Selection selection = getSelection();
            int oldIndex = list.getSelectedIndex();
            int newIndex = oldIndex + delta;
            if (oldIndex >= 0 && newIndex >= 0 && newIndex < selection.size()) {
                selection.move(oldIndex, newIndex);
                list.setSelectedIndex(newIndex);
            }
        }
    }
}

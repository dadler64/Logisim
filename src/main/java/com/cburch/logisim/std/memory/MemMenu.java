/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

class MemMenu implements ActionListener, MenuExtender {

    private final Mem factory;
    private final Instance instance;
    private Project project;
    private Frame frame;
    private CircuitState circuitState;
    private JMenuItem editItem;
    private JMenuItem clearItem;
    private JMenuItem loadItem;
    private JMenuItem saveItem;

    MemMenu(Mem factory, Instance instance) {
        this.factory = factory;
        this.instance = instance;
    }

    public void configureMenu(JPopupMenu menu, Project project) {
        this.project = project;
        this.frame = project.getFrame();
        this.circuitState = project.getCircuitState();

        Object attrs = instance.getAttributeSet();
        if (attrs instanceof RomAttributes) {
            ((RomAttributes) attrs).setProject(project);
        }

        boolean enabled = circuitState != null;
        editItem = createItem(enabled, Strings.get("ramEditMenuItem"));
        clearItem = createItem(enabled, Strings.get("ramClearMenuItem"));
        loadItem = createItem(enabled, Strings.get("ramLoadMenuItem"));
        saveItem = createItem(enabled, Strings.get("ramSaveMenuItem"));

        menu.addSeparator();
        menu.add(editItem);
        menu.add(clearItem);
        menu.add(loadItem);
        menu.add(saveItem);
    }

    private JMenuItem createItem(boolean enabled, String label) {
        JMenuItem menuItem = new JMenuItem(label);
        menuItem.setEnabled(enabled);
        menuItem.addActionListener(this);
        return menuItem;
    }

    public void actionPerformed(ActionEvent evt) {
        Object source = evt.getSource();
        if (source == editItem) {
            doEdit();
        } else if (source == clearItem) {
            doClear();
        } else if (source == loadItem) {
            doLoad();
        } else if (source == saveItem) {
            doSave();
        }
    }

    private void doEdit() {
        MemState state = factory.getState(instance, circuitState);
        if (state == null) {
            return;
        }
        HexFrame frame = factory.getHexFrame(project, instance, circuitState);
        frame.setVisible(true);
        frame.toFront();
    }

    private void doClear() {
        MemState state = factory.getState(instance, circuitState);
        boolean isAllZero = state.getContents().isClear();
        if (isAllZero) {
            return;
        }

        int choice = JOptionPane.showConfirmDialog(frame,
            Strings.get("ramConfirmClearMsg"),
            Strings.get("ramConfirmClearTitle"),
            JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            state.getContents().clear();
        }
    }

    private void doLoad() {
        JFileChooser chooser = project.createChooser();
        File oldSelected = factory.getCurrentImage(instance);
        if (oldSelected != null) {
            chooser.setSelectedFile(oldSelected);
        }
        chooser.setDialogTitle(Strings.get("ramLoadDialogTitle"));
        int choice = chooser.showOpenDialog(frame);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                factory.loadImage(circuitState.getInstanceState(instance), f);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame,
                    e.getMessage(),
                    Strings.get("ramLoadErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void doSave() {
        MemState state = factory.getState(instance, circuitState);

        JFileChooser chooser = project.createChooser();
        File oldSelected = factory.getCurrentImage(instance);
        if (oldSelected != null) {
            chooser.setSelectedFile(oldSelected);
        }
        chooser.setDialogTitle(Strings.get("ramSaveDialogTitle"));
        int choice = chooser.showSaveDialog(frame);
        if (choice == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                HexFile.save(f, state.getContents());
                factory.setCurrentImage(instance, f);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame,
                    e.getMessage(),
                    Strings.get("ramSaveErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

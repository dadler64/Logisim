/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class LogisimMenuBar extends JMenuBar {

    public static final LogisimMenuItem PRINT = new LogisimMenuItem("Print");
    public static final LogisimMenuItem EXPORT_IMAGE = new LogisimMenuItem("ExportImage");
    public static final LogisimMenuItem CUT = new LogisimMenuItem("Cut");
    public static final LogisimMenuItem COPY = new LogisimMenuItem("Copy");
    public static final LogisimMenuItem PASTE = new LogisimMenuItem("Paste");
    public static final LogisimMenuItem DELETE = new LogisimMenuItem("Delete");
    public static final LogisimMenuItem DUPLICATE = new LogisimMenuItem("Duplicate");
    public static final LogisimMenuItem SELECT_ALL = new LogisimMenuItem("SelectAll");
    public static final LogisimMenuItem RAISE = new LogisimMenuItem("Raise");
    public static final LogisimMenuItem LOWER = new LogisimMenuItem("Lower");
    public static final LogisimMenuItem RAISE_TOP = new LogisimMenuItem("RaiseTop");
    public static final LogisimMenuItem LOWER_BOTTOM = new LogisimMenuItem("LowerBottom");
    public static final LogisimMenuItem ADD_CONTROL = new LogisimMenuItem("AddControl");
    public static final LogisimMenuItem REMOVE_CONTROL = new LogisimMenuItem("RemoveControl");

    public static final LogisimMenuItem ADD_CIRCUIT = new LogisimMenuItem("AddCircuit");
    public static final LogisimMenuItem MOVE_CIRCUIT_UP = new LogisimMenuItem("MoveCircuitUp");
    public static final LogisimMenuItem MOVE_CIRCUIT_DOWN = new LogisimMenuItem("MoveCircuitDown");
    public static final LogisimMenuItem SET_MAIN_CIRCUIT = new LogisimMenuItem("SetMainCircuit");
    public static final LogisimMenuItem REMOVE_CIRCUIT = new LogisimMenuItem("RemoveCircuit");
    public static final LogisimMenuItem EDIT_LAYOUT = new LogisimMenuItem("EditLayout");
    public static final LogisimMenuItem EDIT_APPEARANCE = new LogisimMenuItem("EditAppearance");
    public static final LogisimMenuItem VIEW_TOOLBOX = new LogisimMenuItem("ViewToolbox");
    public static final LogisimMenuItem VIEW_SIMULATION = new LogisimMenuItem("ViewSimulation");
    public static final LogisimMenuItem REVERT_APPEARANCE = new LogisimMenuItem("RevertAppearance");
    public static final LogisimMenuItem ANALYZE_CIRCUIT = new LogisimMenuItem("AnalyzeCircuit");
    public static final LogisimMenuItem CIRCUIT_STATS = new LogisimMenuItem("GetCircuitStatistics");

    public static final LogisimMenuItem SIMULATE_ENABLE = new LogisimMenuItem("SimulateEnable");
    public static final LogisimMenuItem SIMULATE_STEP = new LogisimMenuItem("SimulateStep");
    public static final LogisimMenuItem TICK_ENABLE = new LogisimMenuItem("TickEnable");
    public static final LogisimMenuItem TICK_STEP = new LogisimMenuItem("TickStep");
    private final JFrame parent;
    private final MyListener listener;
    private final Project project;
    private final HashMap<LogisimMenuItem, MenuItem> menuItems = new HashMap<>();
    private final ArrayList<ChangeListener> enableListeners;
    private final MenuFile menuFile;
    private final MenuEdit menuEdit;
    private final MenuProject menuProject;
    private final MenuSimulate menuSimulate;
    private final MenuHelp menuHelp;
    private SimulateListener simulateListener = null;

    public LogisimMenuBar(JFrame parent, Project project) {
        this.parent = parent;
        this.listener = new MyListener();
        this.project = project;
        this.enableListeners = new ArrayList<>();

        add(menuFile = new MenuFile(this));
        add(menuEdit = new MenuEdit(this));
        add(menuProject = new MenuProject(this));
        add(menuSimulate = new MenuSimulate(this));
        add(new WindowMenu(parent));
        add(menuHelp = new MenuHelp(this));

        LocaleManager.addLocaleListener(listener);
        listener.localeChanged();
    }

    public void setEnabled(LogisimMenuItem logisimMenuItem, boolean value) {
        MenuItem item = menuItems.get(logisimMenuItem);
        if (item != null) {
            item.setEnabled(value);
        }
    }

    public void addActionListener(LogisimMenuItem logisimMenuItem, ActionListener l) {
        MenuItem item = menuItems.get(logisimMenuItem);
        if (item != null) {
            item.addActionListener(l);
        }
    }

    public void removeActionListener(LogisimMenuItem logisimMenuItem, ActionListener l) {
        MenuItem item = menuItems.get(logisimMenuItem);
        if (item != null) {
            item.removeActionListener(l);
        }
    }

    public void addEnableListener(ChangeListener l) {
        enableListeners.add(l);
    }

    public void removeEnableListener(ChangeListener l) {
        enableListeners.remove(l);
    }

    void fireEnableChanged() {
        ChangeEvent event = new ChangeEvent(this);
        for (ChangeListener listener : enableListeners) {
            listener.stateChanged(event);
        }
    }

    public void setSimulateListener(SimulateListener l) {
        simulateListener = l;
    }

    public void setCircuitState(Simulator simulator, CircuitState state) {
        menuSimulate.setCurrentState(simulator, state);
    }

    public Project getMenuProject() {
        return project;
    }

    JFrame getParentWindow() {
        return parent;
    }

    void registerItem(LogisimMenuItem logisimMenuItem, MenuItem menuItem) {
        menuItems.put(logisimMenuItem, menuItem);
    }

    void fireStateChanged(Simulator simulator, CircuitState state) {
        if (simulateListener != null) {
            simulateListener.stateChangeRequested(simulator, state);
        }
    }

    public void doAction(LogisimMenuItem logisimMenuItem) {
        MenuItem menuItem = menuItems.get(logisimMenuItem);
        menuItem.actionPerformed(new ActionEvent(menuItem, ActionEvent.ACTION_PERFORMED, logisimMenuItem.toString()));
    }

    public boolean isEnabled(LogisimMenuItem logisimMenuItem) {
        MenuItem menuItem = menuItems.get(logisimMenuItem);
        return menuItem != null && menuItem.isEnabled();
    }

    private class MyListener implements LocaleListener {

        public void localeChanged() {
            menuFile.localeChanged();
            menuEdit.localeChanged();
            menuProject.localeChanged();
            menuSimulate.localeChanged();
            menuHelp.localeChanged();
        }
    }
}

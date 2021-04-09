/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SimulatorEvent;
import com.cburch.logisim.circuit.SimulatorListener;
import com.cburch.logisim.gui.log.LogFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

class MenuSimulate extends Menu {

    private final LogisimMenuBar menubar;
    private final MyListener myListener = new MyListener();
    private final MenuItemCheckImpl run;
    private final JMenuItem reset = new JMenuItem();
    private final MenuItemImpl step;
    private final MenuItemCheckImpl ticksEnabled;
    private final MenuItemImpl tickOnce;
    private final JMenu tickFreq = new JMenu();
    private final TickFrequencyChoice[] tickFrequencies = {
        new TickFrequencyChoice(4096),
        new TickFrequencyChoice(2048),
        new TickFrequencyChoice(1024),
        new TickFrequencyChoice(512),
        new TickFrequencyChoice(256),
        new TickFrequencyChoice(128),
        new TickFrequencyChoice(64),
        new TickFrequencyChoice(32),
        new TickFrequencyChoice(16),
        new TickFrequencyChoice(8),
        new TickFrequencyChoice(4),
        new TickFrequencyChoice(2),
        new TickFrequencyChoice(1),
        new TickFrequencyChoice(0.5),
        new TickFrequencyChoice(0.25),
    };
    private final JMenu downStateMenu = new JMenu();
    private final ArrayList<CircuitStateMenuItem> downStateItems = new ArrayList<>();
    private final JMenu upStateMenu = new JMenu();
    private final ArrayList<CircuitStateMenuItem> upStateItems = new ArrayList<>();
    private final JMenuItem log = new JMenuItem();
    private CircuitState currentState = null;
    private CircuitState bottomState = null;
    private Simulator currentSim = null;

    public MenuSimulate(LogisimMenuBar menubar) {
        this.menubar = menubar;

        run = new MenuItemCheckImpl(this, LogisimMenuBar.SIMULATE_ENABLE);
        step = new MenuItemImpl(this, LogisimMenuBar.SIMULATE_STEP);
        ticksEnabled = new MenuItemCheckImpl(this, LogisimMenuBar.TICK_ENABLE);
        tickOnce = new MenuItemImpl(this, LogisimMenuBar.TICK_STEP);

        menubar.registerItem(LogisimMenuBar.SIMULATE_ENABLE, run);
        menubar.registerItem(LogisimMenuBar.SIMULATE_STEP, step);
        menubar.registerItem(LogisimMenuBar.TICK_ENABLE, ticksEnabled);
        menubar.registerItem(LogisimMenuBar.TICK_STEP, tickOnce);

        int menuMask = getToolkit().getMenuShortcutKeyMaskEx();
        run.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_E, menuMask));
        reset.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_R, menuMask));
        step.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_I, menuMask));
        tickOnce.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_T, menuMask));
        ticksEnabled.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_K, menuMask));

        ButtonGroup buttonGroup = new ButtonGroup();
        for (TickFrequencyChoice tickFrequency : tickFrequencies) {
            buttonGroup.add(tickFrequency);
            tickFreq.add(tickFrequency);
        }

        add(run);
        add(reset);
        add(step);
        addSeparator();
        add(upStateMenu);
        add(downStateMenu);
        addSeparator();
        add(tickOnce);
        add(ticksEnabled);
        add(tickFreq);
        addSeparator();
        add(log);

        setEnabled(false);
        run.setEnabled(false);
        reset.setEnabled(false);
        step.setEnabled(false);
        upStateMenu.setEnabled(false);
        downStateMenu.setEnabled(false);
        tickOnce.setEnabled(false);
        ticksEnabled.setEnabled(false);
        tickFreq.setEnabled(false);

        run.addChangeListener(myListener);
        menubar.addActionListener(LogisimMenuBar.SIMULATE_ENABLE, myListener);
        menubar.addActionListener(LogisimMenuBar.SIMULATE_STEP, myListener);
        menubar.addActionListener(LogisimMenuBar.TICK_ENABLE, myListener);
        menubar.addActionListener(LogisimMenuBar.TICK_STEP, myListener);
        // run.addActionListener(myListener);
        reset.addActionListener(myListener);
        // step.addActionListener(myListener);
        // tickOnce.addActionListener(myListener);
        // ticksEnabled.addActionListener(myListener);
        log.addActionListener(myListener);

        computeEnabled();
    }

    public void localeChanged() {
        this.setText(Strings.get("simulateMenu"));
        run.setText(Strings.get("simulateRunItem"));
        reset.setText(Strings.get("simulateResetItem"));
        step.setText(Strings.get("simulateStepItem"));
        tickOnce.setText(Strings.get("simulateTickOnceItem"));
        ticksEnabled.setText(Strings.get("simulateTickItem"));
        tickFreq.setText(Strings.get("simulateTickFreqMenu"));
        for (TickFrequencyChoice tickFrequency : tickFrequencies) {
            tickFrequency.localeChanged();
        }
        downStateMenu.setText(Strings.get("simulateDownStateMenu"));
        upStateMenu.setText(Strings.get("simulateUpStateMenu"));
        log.setText(Strings.get("simulateLogItem"));
    }

    public void setCurrentState(Simulator simulator, CircuitState state) {
        if (currentState == state) {
            return;
        }
        Simulator oldSimulator = currentSim;
        CircuitState oldState = currentState;
        currentSim = simulator;
        currentState = state;
        if (bottomState == null) {
            bottomState = currentState;
        } else if (currentState == null) {
            bottomState = null;
        } else {
            CircuitState circuitState = bottomState;
            while (circuitState != null && circuitState != currentState) {
                circuitState = circuitState.getParentState();
            }
            if (circuitState == null) {
                bottomState = currentState;
            }
        }

        boolean oldPresent = oldState != null;
        boolean present = currentState != null;
        if (oldPresent != present) {
            computeEnabled();
        }

        if (currentSim != oldSimulator) {
            double freqency = currentSim == null ? 1.0 : currentSim.getTickFrequency();
            for (TickFrequencyChoice tickFrequency : tickFrequencies) {
                tickFrequency.setSelected(Math.abs(tickFrequency.frequency - freqency) < 0.001);
            }

            if (oldSimulator != null) {
                oldSimulator.removeSimulatorListener(myListener);
            }
            if (currentSim != null) {
                currentSim.addSimulatorListener(myListener);
            }
            myListener.simulatorStateChanged(new SimulatorEvent(simulator));
        }

        clearItems(downStateItems);
        CircuitState circuitState = bottomState;
        while (circuitState != null && circuitState != currentState) {
            downStateItems.add(new CircuitStateMenuItem(circuitState));
            circuitState = circuitState.getParentState();
        }
        if (circuitState != null) {
            circuitState = circuitState.getParentState();
        }
        clearItems(upStateItems);
        while (circuitState != null) {
            upStateItems.add(0, new CircuitStateMenuItem(circuitState));
            circuitState = circuitState.getParentState();
        }
        recreateStateMenus();
    }

    private void clearItems(ArrayList<CircuitStateMenuItem> items) {
        for (CircuitStateMenuItem item : items) {
            item.unregister();
        }
        items.clear();
    }

    private void recreateStateMenus() {
        recreateStateMenu(downStateMenu, downStateItems, KeyEvent.VK_RIGHT);
        recreateStateMenu(upStateMenu, upStateItems, KeyEvent.VK_LEFT);
    }

    private void recreateStateMenu(JMenu menu, ArrayList<CircuitStateMenuItem> items, int code) {
        menu.removeAll();
        menu.setEnabled(items.size() > 0);
        boolean first = true;
        int mask = getToolkit().getMenuShortcutKeyMaskEx();
        for (int i = items.size() - 1; i >= 0; i--) {
            JMenuItem item = items.get(i);
            menu.add(item);
            if (first) {
                item.setAccelerator(KeyStroke.getKeyStroke(code, mask));
                first = false;
            } else {
                item.setAccelerator(null);
            }
        }
    }

    @Override
    void computeEnabled() {
        boolean present = currentState != null;
        Simulator sim = this.currentSim;
        boolean simRunning = sim != null && sim.isRunning();
        setEnabled(present);
        run.setEnabled(present);
        reset.setEnabled(present);
        step.setEnabled(present && !simRunning);
        upStateMenu.setEnabled(present);
        downStateMenu.setEnabled(present);
        tickOnce.setEnabled(present);
        ticksEnabled.setEnabled(present && simRunning);
        tickFreq.setEnabled(present);
        menubar.fireEnableChanged();
    }

    private class TickFrequencyChoice extends JRadioButtonMenuItem
        implements ActionListener {

        private final double frequency;

        private TickFrequencyChoice(double value) {
            frequency = value;
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent event) {
            if (currentSim != null) {
                currentSim.setTickFrequency(frequency);
            }
        }

        public void localeChanged() {
            double frequency = this.frequency;
            if (frequency < 1000) {
                String hzStr;
                if (Math.abs(frequency - Math.round(frequency)) < 0.0001) {
                    hzStr = "" + (int) Math.round(frequency);
                } else {
                    hzStr = "" + frequency;
                }
                setText(StringUtil.format(Strings.get("simulateTickFreqItem"), hzStr));
            } else {
                String kHzStr;
                double kHzFrequency = Math.round(frequency / 100) / 10.0;
                if (kHzFrequency == Math.round(kHzFrequency)) {
                    kHzStr = "" + (int) kHzFrequency;
                } else {
                    kHzStr = "" + kHzFrequency;
                }
                setText(StringUtil.format(Strings.get("simulateTickKFreqItem"), kHzStr));
            }
        }
    }

    private class CircuitStateMenuItem extends JMenuItem implements CircuitListener, ActionListener {

        private final CircuitState circuitState;

        private CircuitStateMenuItem(CircuitState circuitState) {
            this.circuitState = circuitState;

            Circuit circuit = circuitState.getCircuit();
            circuit.addCircuitListener(this);
            this.setText(circuit.getName());
            addActionListener(this);
        }

        private void unregister() {
            Circuit circuit = circuitState.getCircuit();
            circuit.removeCircuitListener(this);
        }

        public void circuitChanged(CircuitEvent event) {
            if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
                this.setText(circuitState.getCircuit().getName());
            }
        }

        public void actionPerformed(ActionEvent event) {
            menubar.fireStateChanged(currentSim, circuitState);
        }
    }

    private class MyListener implements ActionListener, SimulatorListener, ChangeListener {

        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            Project project = menubar.getMenuProject();
            Simulator simulator = project == null ? null : project.getSimulator();
            if (source.equals(run) || source.equals(LogisimMenuBar.SIMULATE_ENABLE)) {
                if (simulator != null) {
                    simulator.setIsRunning(!simulator.isRunning());
                    project.repaintCanvas();
                }
            } else if (source.equals(reset)) {
                if (simulator != null) {
                    simulator.requestReset();
                }
            } else if (source.equals(step) || source.equals(LogisimMenuBar.SIMULATE_STEP)) {
                if (simulator != null) {
                    simulator.step();
                }
            } else if (source.equals(tickOnce) || source.equals(LogisimMenuBar.TICK_STEP)) {
                if (simulator != null) {
                    simulator.tick();
                }
            } else if (source.equals(ticksEnabled) || source.equals(LogisimMenuBar.TICK_ENABLE)) {
                if (simulator != null) {
                    simulator.setIsTicking(!simulator.isTicking());
                }
            } else if (source.equals(log)) {
                assert menubar.getMenuProject() != null;
                LogFrame frame = menubar.getMenuProject().getLogFrame(true);
                frame.setVisible(true);
            }
        }

        public void propagationCompleted(SimulatorEvent event) {
        }

        public void tickCompleted(SimulatorEvent event) {
        }

        public void simulatorStateChanged(SimulatorEvent event) {
            Simulator simulator = event.getSource();
            if (simulator != currentSim) {
                return;
            }
            computeEnabled();
            run.setSelected(simulator.isRunning());
            ticksEnabled.setSelected(simulator.isTicking());
            double frequency = simulator.getTickFrequency();
            for (TickFrequencyChoice item : tickFrequencies) {
                item.setSelected(frequency == item.frequency);
            }
        }

        public void stateChanged(ChangeEvent event) {
            step.setEnabled(run.isEnabled() && !run.isSelected());
        }
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.opts;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.util.TableLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

class SimulateOptions extends OptionsPanel {

    private final MyListener myListener = new MyListener();
    private final JLabel simLimitLabel = new JLabel();
    private final JComboBox<Integer> simLimit = new JComboBox<>(new Integer[]{
        200,
        500,
        1000,
        2000,
        5000,
        10000,
        20000,
        50000,
    });
    private final JCheckBox simRandomness = new JCheckBox();
    private final JLabel gateUndefinedLabel = new JLabel();
    private final JComboBox<Object> gateUndefined = new JComboBox<>(new Object[]{
        new ComboOption(Options.GATE_UNDEFINED_IGNORE),
        new ComboOption(Options.GATE_UNDEFINED_ERROR)
    });

    public SimulateOptions(OptionsFrame window) {
        super(window);

        JPanel simLimitPanel = new JPanel();
        simLimitPanel.add(simLimitLabel);
        simLimitPanel.add(simLimit);
        simLimit.addActionListener(myListener);

        JPanel gateUndefinedPanel = new JPanel();
        gateUndefinedPanel.add(gateUndefinedLabel);
        gateUndefinedPanel.add(gateUndefined);
        gateUndefined.addActionListener(myListener);

        simRandomness.addActionListener(myListener);

        setLayout(new TableLayout(1));
        add(simLimitPanel);
        add(gateUndefinedPanel);
        add(simRandomness);

        window.getOptions().getAttributeSet().addAttributeListener(myListener);
        AttributeSet attrs = getOptions().getAttributeSet();
        myListener.loadSimLimit(attrs.getValue(Options.SIMULATOR_LIMIT_ATTRIBUTE));
        myListener.loadGateUndefined(attrs.getValue(Options.ATTR_GATE_UNDEFINED));
        myListener.loadSimRandomness(attrs.getValue(Options.SIMULATOR_RANDOM_ATTRIBUTE));
    }

    @Override
    public String getTitle() {
        return Strings.get("simulateTitle");
    }

    @Override
    public String getHelpText() {
        return Strings.get("simulateHelp");
    }

    @Override
    public void localeChanged() {
        simLimitLabel.setText(Strings.get("simulateLimit"));
        gateUndefinedLabel.setText(Strings.get("gateUndefined"));
        simRandomness.setText(Strings.get("simulateRandomness"));
    }

    private class MyListener implements ActionListener, AttributeListener {

        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source == simLimit) {
                Integer opt = (Integer) simLimit.getSelectedItem();
                if (opt != null) {
                    AttributeSet attrs = getOptions().getAttributeSet();
                    getProject().doAction(OptionsActions.setAttribute(attrs,
                        Options.SIMULATOR_LIMIT_ATTRIBUTE, opt));
                }
            } else if (source == simRandomness) {
                AttributeSet attrs = getOptions().getAttributeSet();
                Object val = simRandomness.isSelected() ? Options.SIMULATOR_RANDOM_DEFAULT
                    : Integer.valueOf(0);
                getProject().doAction(OptionsActions.setAttribute(attrs,
                    Options.SIMULATOR_RANDOM_ATTRIBUTE, val));
            } else if (source == gateUndefined) {
                ComboOption opt = (ComboOption) gateUndefined.getSelectedItem();
                if (opt != null) {
                    AttributeSet attrs = getOptions().getAttributeSet();
                    getProject().doAction(OptionsActions.setAttribute(attrs,
                        Options.ATTR_GATE_UNDEFINED, opt.getValue()));
                }
            }
        }

        public void attributeListChanged(AttributeEvent e) {
        }

        public void attributeValueChanged(AttributeEvent e) {
            Attribute<?> attr = e.getAttribute();
            Object val = e.getValue();
            if (attr == Options.SIMULATOR_LIMIT_ATTRIBUTE) {
                loadSimLimit((Integer) val);
            } else if (attr == Options.SIMULATOR_RANDOM_ATTRIBUTE) {
                loadSimRandomness((Integer) val);
            }
        }

        private void loadSimLimit(Integer val) {
            int value = val;
            ComboBoxModel<Integer> model = simLimit.getModel();
            for (int i = 0; i < model.getSize(); i++) {
                Integer opt = model.getElementAt(i);
                if (opt == value) {
                    simLimit.setSelectedItem(opt);
                }
            }
        }

        private void loadGateUndefined(Object val) {
            ComboOption.setSelected(gateUndefined, val);
        }

        private void loadSimRandomness(Integer val) {
            simRandomness.setSelected(val > 0);
        }
    }
}

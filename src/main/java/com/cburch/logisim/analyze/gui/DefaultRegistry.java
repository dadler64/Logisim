/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.gui;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JRootPane;

class DefaultRegistry {

    private final JRootPane rootPane;

    public DefaultRegistry(JRootPane rootPane) {
        this.rootPane = rootPane;
        rootPane.setDefaultButton(null);
    }

    public void registerDefaultButton(JComponent component, JButton button) {
        component.addFocusListener(new MyListener(button));
    }

    private class MyListener implements FocusListener {

        private final JButton defaultButton;

        private MyListener(JButton defaultButton) {
            this.defaultButton = defaultButton;
        }

        public void focusGained(FocusEvent event) {
            rootPane.setDefaultButton(defaultButton);
        }

        public void focusLost(FocusEvent event) {
            JButton currentDefault = rootPane.getDefaultButton();
            if (currentDefault.equals(defaultButton)) {
                rootPane.setDefaultButton(null);
            }
        }
    }
}

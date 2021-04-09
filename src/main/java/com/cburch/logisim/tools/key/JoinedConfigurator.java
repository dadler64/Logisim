/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.key;

public class JoinedConfigurator implements KeyConfigurator, Cloneable {

    private KeyConfigurator[] handlers;

    private JoinedConfigurator(KeyConfigurator[] handlers) {
        this.handlers = handlers;
    }

    public static JoinedConfigurator create(KeyConfigurator a, KeyConfigurator b) {
        return new JoinedConfigurator(new KeyConfigurator[]{a, b});
    }

    public static JoinedConfigurator create(KeyConfigurator[] configs) {
        return new JoinedConfigurator(configs);
    }

    @Override
    public JoinedConfigurator clone() {
        JoinedConfigurator configurator;
        try {
            configurator = (JoinedConfigurator) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
        int len = this.handlers.length;
        configurator.handlers = new KeyConfigurator[len];
        for (int i = 0; i < len; i++) {
            configurator.handlers[i] = this.handlers[i].clone();
        }
        return configurator;
    }

    public KeyConfigurationResult keyEventReceived(KeyConfigurationEvent event) {
        KeyConfigurator[] handlers = this.handlers;
        if (event.isConsumed()) {
            return null;
        }
        for (KeyConfigurator handler : handlers) {
            KeyConfigurationResult result = handler.keyEventReceived(event);
            if (result != null || event.isConsumed()) {
                return result;
            }
        }
        return null;
    }
}

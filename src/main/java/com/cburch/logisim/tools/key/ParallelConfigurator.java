/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import java.util.HashMap;

public class ParallelConfigurator implements KeyConfigurator, Cloneable {

    private KeyConfigurator[] handlers;

    private ParallelConfigurator(KeyConfigurator[] handlers) {
        this.handlers = handlers;
    }

    public static ParallelConfigurator create(KeyConfigurator a, KeyConfigurator b) {
        return new ParallelConfigurator(new KeyConfigurator[]{a, b});
    }

    public static ParallelConfigurator create(KeyConfigurator[] configs) {
        return new ParallelConfigurator(configs);
    }

    @Override
    public ParallelConfigurator clone() {
        ParallelConfigurator configurator;
        try {
            configurator = (ParallelConfigurator) super.clone();
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
        KeyConfigurationResult first = null;
        HashMap<Attribute<?>, Object> map = null;
        for (KeyConfigurator handler : handlers) {
            KeyConfigurationResult result = handler.keyEventReceived(event);
            if (result != null) {
                if (first == null) {
                    first = result;
                } else if (map == null) {
                    map = new HashMap<>(first.getAttributeValues());
                    map.putAll(result.getAttributeValues());
                } else {
                    map.putAll(result.getAttributeValues());
                }
            }
        }
        if (map != null) {
            return new KeyConfigurationResult(event, map);
        } else {
            return first;
        }
    }
}

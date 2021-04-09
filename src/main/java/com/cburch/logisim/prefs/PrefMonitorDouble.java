/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.prefs;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

class PrefMonitorDouble extends AbstractPrefMonitor<Double> {

    private final double defaultValue;
    private double value;

    PrefMonitorDouble(String name, double defaultValue) {
        super(name);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        Preferences preferences = AppPreferences.getPreferences();
        set(preferences.getDouble(name, defaultValue));
        preferences.addPreferenceChangeListener(this);
    }

    public Double get() {
        return value;
    }

    public void set(Double newValue) {
        double newVal = newValue;
        if (value != newVal) {
            AppPreferences.getPreferences().putDouble(getIdentifier(), newVal);
        }
    }

    public void preferenceChange(PreferenceChangeEvent event) {
        Preferences preferences = event.getNode();
        String prop = event.getKey();
        String name = getIdentifier();
        if (prop.equals(name)) {
            double oldValue = value;
            double newValue = preferences.getDouble(name, defaultValue);
            if (newValue != oldValue) {
                value = newValue;
                AppPreferences.firePropertyChange(name,
                    oldValue, newValue);
            }
        }
    }
}

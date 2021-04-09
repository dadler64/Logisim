/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.prefs;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

class PrefMonitorInt extends AbstractPrefMonitor<Integer> {

    private final int defaultValue;
    private int value;

    PrefMonitorInt(String name, int defaultValue) {
        super(name);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        Preferences preferences = AppPreferences.getPreferences();
        set(preferences.getInt(name, defaultValue));
        preferences.addPreferenceChangeListener(this);
    }

    public Integer get() {
        return value;
    }

    public void set(Integer newValue) {
        int newVal = newValue;
        if (value != newVal) {
            AppPreferences.getPreferences().putInt(getIdentifier(), newVal);
        }
    }

    public void preferenceChange(PreferenceChangeEvent event) {
        Preferences preferences = event.getNode();
        String prop = event.getKey();
        String name = getIdentifier();
        if (prop.equals(name)) {
            int oldValue = value;
            int newValue = preferences.getInt(name, defaultValue);
            if (newValue != oldValue) {
                value = newValue;
                AppPreferences.firePropertyChange(name,
                    oldValue, newValue);
            }
        }
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.prefs;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

class PrefMonitorBoolean extends AbstractPrefMonitor<Boolean> {

    private boolean defaultValue;
    private boolean value;

    PrefMonitorBoolean(String name, boolean defaultValue) {
        super(name);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
        Preferences preferences = AppPreferences.getPreferences();
        set(preferences.getBoolean(name, defaultValue));
        preferences.addPreferenceChangeListener(this);
    }

    public Boolean get() {
        return value;
    }

    @Override
    public boolean getBoolean() {
        return value;
    }

    public void set(Boolean newValue) {
        if (value != newValue) {
            AppPreferences.getPreferences().putBoolean(getIdentifier(), newValue);
        }
    }

    public void preferenceChange(PreferenceChangeEvent event) {
        Preferences preferences = event.getNode();
        String property = event.getKey();
        String name = getIdentifier();
        if (property.equals(name)) {
            boolean oldValue = value;
            boolean newValue = preferences.getBoolean(name, defaultValue);
            if (newValue != oldValue) {
                value = newValue;
                AppPreferences.firePropertyChange(name, oldValue, newValue);
            }
        }
    }
}

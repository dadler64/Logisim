/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.prefs;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.Preferences;

class PrefMonitorStringOpts extends AbstractPrefMonitor<String> {

    private final String[] opts;
    private final String defaultValue;
    private String value;

    PrefMonitorStringOpts(String name, String[] opts, String defaultValue) {
        super(name);
        this.opts = opts;
        this.value = opts[0];
        this.defaultValue = defaultValue;
        Preferences preferences = AppPreferences.getPreferences();
        set(preferences.get(name, defaultValue));
        preferences.addPreferenceChangeListener(this);
    }

    private static boolean isSame(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }

    public String get() {
        return value;
    }

    public void set(String newValue) {
        String oldValue = value;
        if (!isSame(oldValue, newValue)) {
            AppPreferences.getPreferences().put(getIdentifier(), newValue);
        }
    }

    public void preferenceChange(PreferenceChangeEvent event) {
        Preferences preferences = event.getNode();
        String prop = event.getKey();
        String name = getIdentifier();
        if (prop.equals(name)) {
            String oldValue = value;
            String newValue = preferences.get(name, defaultValue);
            if (!isSame(oldValue, newValue)) {
                String chosen = null;
                for (String s : opts) {
                    if (isSame(s, newValue)) {
                        chosen = s;
                        break;
                    }
                }
                if (chosen == null) {
                    chosen = defaultValue;
                }
                value = chosen;
                AppPreferences.firePropertyChange(name, oldValue, chosen);
            }
        }
    }
}

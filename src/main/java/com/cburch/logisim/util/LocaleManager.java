/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import com.adlerd.logger.Logger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class LocaleManager {

    // static members
    private static final String SETTINGS_NAME = "settings";
    private static final ArrayList<LocaleManager> managers = new ArrayList<>();
    private static final ArrayList<LocaleListener> listeners = new ArrayList<>();
    private static boolean replaceAccents = false;
    private static HashMap<Character, String> repl = null;
    private static Locale curLocale = null;
    // instance members
    private final String directoryName;
    private final String fileStart;
    private ResourceBundle settings = null;
    private ResourceBundle locale = null;
    private ResourceBundle defaultLocale = null;

    public LocaleManager(String directoryName, String fileStart) {
        this.directoryName = directoryName;
        this.fileStart = fileStart;
        loadDefault();
        managers.add(this);
    }

    public static Locale getLocale() {
        Locale locale = curLocale;
        if (locale == null) {
            locale = Locale.getDefault();
            curLocale = locale;
        }
        return locale;
    }

    public static void setLocale(Locale locale) {
        Locale curLocale = getLocale();
        if (!locale.equals(curLocale)) {
            Locale[] options = Strings.getLocaleManager().getLocaleOptions();
            Locale select = null;
            Locale backup = null;
            String localeLanguage = locale.getLanguage();
            for (Locale option : options) {
                if (select == null && option.equals(locale)) {
                    select = option;
                }
                if (backup == null && option.getLanguage().equals(localeLanguage)) {
                    backup = option;
                }
            }
            if (select == null) {
                if (backup == null) {
                    select = new Locale("en");
                } else {
                    select = backup;
                }
            }

            LocaleManager.curLocale = select;
            Locale.setDefault(select);
            for (LocaleManager man : managers) {
                man.loadDefault();
            }
            repl = replaceAccents ? fetchReplaceAccents() : null;
            fireLocaleChanged();
        }
    }

    public static boolean canReplaceAccents() {
        return fetchReplaceAccents() != null;
    }

    public static void setReplaceAccents(boolean value) {
        HashMap<Character, String> newRepl = value ? fetchReplaceAccents() : null;
        replaceAccents = value;
        repl = newRepl;
        fireLocaleChanged();
    }

    private static HashMap<Character, String> fetchReplaceAccents() {
        HashMap<Character, String> ret = null;
        String value;
        try {
            value = Strings.source.locale.getString("accentReplacements");
        } catch (MissingResourceException e) {
            Logger.debugln(e.getMessage());
            return null;
        }
        StringTokenizer tokenizer = new StringTokenizer(value, "/");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            char c = '\0';
            String s = null;
            if (token.length() == 1) {
                c = token.charAt(0);
                s = "";
            } else if (token.length() >= 2 && token.charAt(1) == ' ') {
                c = token.charAt(0);
                s = token.substring(2).trim();
            }
            if (s != null) {
                if (ret == null) {
                    ret = new HashMap<>();
                }
                ret.put(c, s);
            }
        }
        return ret;
    }

    public static void addLocaleListener(LocaleListener l) {
        listeners.add(l);
    }

    public static void removeLocaleListener(LocaleListener l) {
        listeners.remove(l);
    }

    private static void fireLocaleChanged() {
        for (LocaleListener listener : listeners) {
            listener.localeChanged();
        }
    }

    private static String replaceAccents(String source, HashMap<Character, String> repl) {
        // find first non-standard character - so we can avoid the replacement process if possible
        int i = 0;
        int n = source.length();
        for (; i < n; i++) {
            char ci = source.charAt(i);
            if (ci < 32 || ci >= 127) {
                break;
            }
        }
        if (i == n) {
            return source;
        }

        // ok, we'll have to consider replacing accents
        char[] characters = source.toCharArray();
        StringBuilder builder = new StringBuilder(source.substring(0, i));
        for (int j = i; j < characters.length; j++) {
            char cj = characters[j];
            if (cj < 32 || cj >= 127) {
                String out = repl.get(cj);
                if (out != null) {
                    builder.append(out);
                } else {
                    builder.append(cj);
                }
            } else {
                builder.append(cj);
            }
        }
        return builder.toString();
    }

    private void loadDefault() {
        if (settings == null) {
            try {
                settings = ResourceBundle.getBundle(directoryName + "/" + SETTINGS_NAME);
            } catch (java.util.MissingResourceException ignored) {
                // Ignored
            }
        }

        try {
            loadLocale(Locale.getDefault());
            if (locale != null) {
                return;
            }
        } catch (java.util.MissingResourceException ignored) {
            // Ignored
        }
        try {
            loadLocale(Locale.ENGLISH);
            if (locale != null) {
                return;
            }
        } catch (java.util.MissingResourceException ignored) {
            // Ignored
        }
        Locale[] choices = getLocaleOptions();
        if (choices != null && choices.length > 0) {
            loadLocale(choices[0]);
        }
        if (locale != null) {
            return;
        }
        throw new RuntimeException("No locale bundles are available");
    }

    private void loadLocale(Locale locale) {
        String bundleName = directoryName + "/" + locale.getLanguage() + "/" + fileStart;
        this.locale = ResourceBundle.getBundle(bundleName, locale);
    }

    public String get(String key) {
        String ret;
        try {
            ret = locale.getString(key);
        } catch (MissingResourceException e) {
            ResourceBundle backup = defaultLocale;
            if (backup == null) {
                Locale backup_loc = Locale.US;
                backup = ResourceBundle.getBundle(directoryName + "/en/" + fileStart, backup_loc);
                defaultLocale = backup;
            }
            try {
                ret = backup.getString(key);
            } catch (MissingResourceException e2) {
                ret = key;
            }
        }
        HashMap<Character, String> repl = LocaleManager.repl;
        if (repl != null) {
            ret = replaceAccents(ret, repl);
        }
        return ret;
    }

    public StringGetter getter(String key) {
        return new LocaleGetter(this, key);
    }

    public StringGetter getter(String key, String arg) {
        return StringUtil.formatter(getter(key), arg);
    }

    public StringGetter getter(String key, StringGetter arg) {
        return StringUtil.formatter(getter(key), arg);
    }

    public Locale[] getLocaleOptions() {
        String locales = null;
        try {
            if (settings != null) {
                locales = settings.getString("locales");
            }
        } catch (java.util.MissingResourceException ignored) {
            // Ignored
        }
        if (locales == null) {
            return new Locale[]{};
        }

        ArrayList<Locale> localeList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(locales);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String language;
            String country;
            if (token.length() >= 2) {
                language = token.substring(0, 2);
                country = (token.length() >= 5 ? token.substring(3, 5) : null);
            } else {
                language = null;
                country = null;
            }
            if (language != null) {
                Locale loc = country == null ? new Locale(language) : new Locale(language, country);
                localeList.add(loc);
            }
        }

        return localeList.toArray(new Locale[0]);
    }

    public JComponent createLocaleSelector() {
        Locale[] locales = getLocaleOptions();
        if (locales == null || locales.length == 0) {
            Locale locale = getLocale();
            if (locale == null) {
                locale = new Locale("en");
            }
            locales = new Locale[]{locale};
        }
        return new JScrollPane(new LocaleSelector(locales));
    }

    private static class LocaleGetter implements StringGetter {

        private final LocaleManager source;
        private final String key;

        LocaleGetter(LocaleManager source, String key) {
            this.source = source;
            this.key = key;
        }

        public String get() {
            return source.get(key);
        }

        @Override
        public String toString() {
            return get();
        }
    }
}

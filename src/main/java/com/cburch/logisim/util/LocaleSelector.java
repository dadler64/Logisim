/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LocaleSelector.LocaleOption;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

class LocaleSelector extends JList<LocaleOption> implements LocaleListener, ListSelectionListener {

    private final LocaleOption[] items;

    LocaleSelector(Locale[] locales) {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultListModel<LocaleOption> model = new DefaultListModel<>();
        items = new LocaleOption[locales.length];
        for (int i = 0; i < locales.length; i++) {
            items[i] = new LocaleOption(locales[i]);
            model.addElement(items[i]);
        }
        setModel(model);
        setVisibleRowCount(Math.min(items.length, 8));
        LocaleManager.addLocaleListener(this);
        localeChanged();
        addListSelectionListener(this);
    }

    public void localeChanged() {
        Locale current = LocaleManager.getLocale();
        LocaleOption option = null;
        for (LocaleOption item : items) {
            item.update(current);
            if (current.equals(item.locale)) {
                option = item;
            }
        }
        if (option != null) {
            setSelectedValue(option, true);
        }
    }

    public void valueChanged(ListSelectionEvent event) {
        LocaleOption option = getSelectedValue();
        if (option != null) {
            SwingUtilities.invokeLater(option);
        }
    }

    public static class LocaleOption implements Runnable {

        private final Locale locale;
        private String text;

        LocaleOption(Locale locale) {
            this.locale = locale;
            update(locale);
        }

        @Override
        public String toString() {
            return text;
        }

        void update(Locale current) {
            if (current != null && current.equals(locale)) {
                text = locale.getDisplayName(locale);
            } else {
                text = locale.getDisplayName(locale) + " / " + locale.getDisplayName(current);
            }
        }

        public void run() {
            if (!LocaleManager.getLocale().equals(locale)) {
                LocaleManager.setLocale(locale);
                AppPreferences.LOCALE.set(locale.getLanguage());
            }
        }
    }
}

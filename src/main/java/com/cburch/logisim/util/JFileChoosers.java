/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import com.adlerd.logger.Logger;
import com.cburch.logisim.prefs.AppPreferences;
import java.io.File;
import java.io.IOException;
import javax.swing.JFileChooser;

public class JFileChoosers {

    private static final String[] PROP_NAMES = {null, "user.home", "user.dir", "java.home", "java.io.tmpdir"};
    private static String currentDirectory = "";

    private JFileChoosers() {
    }

    public static String getCurrentDirectory() {
        return currentDirectory;
    }

    public static JFileChooser create() {
        RuntimeException first = null;
        for (String prop : PROP_NAMES) {
            try {
                String dirname;
                if (prop == null) {
                    dirname = currentDirectory;
                    if (dirname.equals("")) {
                        dirname = AppPreferences.DIALOG_DIRECTORY.get();
                    }
                } else {
                    dirname = System.getProperty(prop);
                }
                if (dirname.equals("")) {
                    return new LogisimFileChooser();
                } else {
                    File dir = new File(dirname);
                    if (dir.canRead()) {
                        return new LogisimFileChooser(dir);
                    }
                }
            } catch (RuntimeException t) {
                if (first == null) {
                    first = t;
                }
                Throwable u = t.getCause();
                if (!(u instanceof IOException)) {
                    throw t;
                }
            }
        }
        throw first;
    }

    public static JFileChooser createAt(File openDirectory) {
        if (openDirectory == null) {
            return create();
        } else {
            try {
                return new LogisimFileChooser(openDirectory);
            } catch (RuntimeException t) {
                if (t.getCause() instanceof IOException) {
                    try {
                        return create();
                    } catch (RuntimeException u) {
                        Logger.debugln(u.getMessage());
                    }
                }
                throw t;
            }
        }
    }

    public static JFileChooser createSelected(File selected) {
        if (selected == null) {
            return create();
        } else {
            JFileChooser fileChooser = createAt(selected.getParentFile());
            fileChooser.setSelectedFile(selected);
            return fileChooser;
        }
    }

    /* A user reported that JFileChooser's constructor sometimes resulted in
     * IOExceptions when Logisim is installed under a system administrator
     * account and then is attempted to run as a regular user. This class is
     * an attempt to be a bit more robust about which directory the JFileChooser
     * opens up under. (23 Feb 2010) */
    private static class LogisimFileChooser extends JFileChooser {

        private LogisimFileChooser() {
            super();
        }

        private LogisimFileChooser(File initSelected) {
            super(initSelected);
        }

        @Override
        public File getSelectedFile() {
            File currentDirectory = getCurrentDirectory();
            if (currentDirectory != null) {
                JFileChoosers.currentDirectory = currentDirectory.toString();
            }
            return super.getSelectedFile();
        }
    }
}

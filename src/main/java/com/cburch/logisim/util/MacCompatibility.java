/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import com.adlerd.logger.Logger;
import java.io.File;
import java.io.IOException;
import javax.swing.JMenuBar;
import net.roydesign.mac.MRJAdapter;

public class MacCompatibility {

    public static final double mrjVersion;

    static {
        double versionValue;
        try {
            versionValue = MRJAdapter.mrjVersion;
        } catch (Throwable t) {
            versionValue = 0.0;
        }
        mrjVersion = versionValue;
    }

    private MacCompatibility() {
    }

    public static boolean isAboutAutomaticallyPresent() {
        try {
            return MRJAdapter.isAboutAutomaticallyPresent();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isPreferencesAutomaticallyPresent() {
        try {
            return MRJAdapter.isPreferencesAutomaticallyPresent();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isQuitAutomaticallyPresent() {
        try {
            return MRJAdapter.isQuitAutomaticallyPresent();
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isSwingUsingScreenMenuBar() {
        try {
            return MRJAdapter.isSwingUsingScreenMenuBar();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void setFramelessJMenuBar(JMenuBar menuBar) {
        try {
            MRJAdapter.setFramelessJMenuBar(menuBar);
        } catch (Throwable t) {
            Logger.debugln(t.getMessage());
        }
    }

    public static void setFileCreatorAndType(File destination, String app, String type) throws IOException {
        IOException ioExcept = null;
        try {
            try {
                MRJAdapter.setFileCreatorAndType(destination, app, type);
            } catch (IOException e) {
                ioExcept = e;
            }
        } catch (Throwable t) {
            Logger.debugln(t.getMessage());
        }
        if (ioExcept != null) {
            throw ioExcept;
        }
    }

}

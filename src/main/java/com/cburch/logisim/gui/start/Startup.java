/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.start;

import static com.adlerd.logger.Logger.errorln;
import static com.adlerd.logger.Logger.infoln;
import static com.adlerd.logger.Logger.outputln;

import com.adlerd.logger.Logger;
import com.cburch.logisim.Main;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.gui.main.Print;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.gui.menu.WindowManagers;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.StringUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Startup {

    private static Startup startupTemp = null;
    // based on command line
    private final ArrayList<File> filesToOpen = new ArrayList<>();
    private final boolean isTty;
    private final ArrayList<File> filesToPrint = new ArrayList<>();
    private final HashMap<File, File> substitutions = new HashMap<>();
    private boolean templateEmpty = false;
    private boolean templatePlain = false;
    private boolean showSplash;
    private File loadFile;
    private boolean initialized = false;
    private int ttyFormat = 0;
    // from other sources
    private SplashScreen monitor = null;
    private File templateFile = null;

    private Startup(boolean isTty) {
        this.isTty = isTty;
        this.showSplash = !isTty;
    }

    static void doOpen(File file) {
        if (startupTemp != null) {
            startupTemp.doOpenFile(file);
        }
    }

    static void doPrint(File file) {
        if (startupTemp != null) {
            startupTemp.doPrintFile(file);
        }
    }

    private static void registerHandler() {
        try {
            Class<?> needed1 = Class.forName("com.apple.eawt.Application");
            if (needed1 == null) {
                return;
            }
            Class<?> needed2 = Class.forName("com.apple.eawt.ApplicationAdapter");
            if (needed2 == null) {
                return;
            }
            MacOsAdapter.register();
            MacOsAdapter.addListeners(true);
        } catch (ClassNotFoundException ignored) {
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                MacOsAdapter.addListeners(false);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private static void setLocale(String lang) {
        Locale[] options = Strings.getLocaleOptions();
        for (Locale option : options) {
            if (lang.equals(option.toString())) {
                LocaleManager.setLocale(option);
                return;
            }
        }
        errorln(Strings.get("invalidLocaleError")); //OK
        errorln(Strings.get("invalidLocaleOptionsHeader")); //OK
        for (Locale option : options) {
            errorln("   " + option.toString()); //OK
        }
        System.exit(-1);
    }

    public static Startup parseArgs(String[] args) {
        // see whether we'll be using any graphics
        boolean isTty = false;
        boolean isClearPreferences = false;
        for (String arg : args) {
            if (arg.equals("-tty")) {
                isTty = true;
            } else if (arg.equals("-clearprefs") || arg.equals("-clearprops")) {
                isClearPreferences = true;
            }
        }

        if (!isTty) {
            // we're using the GUI: Set up the Look&Feel to match the platform
            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "Logisim");
            System.setProperty("apple.laf.useScreenMenuBar", "true");

            LocaleManager.setReplaceAccents(false);

            // Initialize graphics acceleration if appropriate
            AppPreferences.handleGraphicsAcceleration();
        }

        Startup startup = new Startup(isTty);
        startupTemp = startup;
        if (!isTty) {
            registerHandler();
        }

        if (isClearPreferences) {
            AppPreferences.clear();
        }

        String theme;
        String os = System.getProperty("os.name").split(" ")[0]; // Get the first word of the os.name string

        switch (os) {
            case "Mac":
//                theme = "javax.swing.plaf.nimbus.NimbusLookAndFeel";
                theme = UIManager.getCrossPlatformLookAndFeelClassName();
                break;
            case "Linux":
                theme = UIManager.getCrossPlatformLookAndFeelClassName();
                break;
            case "Windows":
                theme = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
                break;
            default:
                errorln("Unknown theme specified for os: " + os);
                theme = UIManager.getSystemLookAndFeelClassName();
        }

        try {
            UIManager.setLookAndFeel(theme);
        } catch (ClassNotFoundException e) {
            errorln("Couldn't find class for specified look and feel:" + theme);
            errorln("Did you include the L&F library in the class path?");
            errorln("Using the default look and feel.");
        } catch (UnsupportedLookAndFeelException e) {
            errorln("Can't use the specified look and feel (" + theme + ") on this platform.");
            errorln("Using the default look and feel.");
        } catch (Exception e) {
            errorln("Couldn't get specified look and feel (" + theme + "), for some reason.");
            errorln("Using the default look and feel.");
            e.printStackTrace(System.err);
        }
        // parse arguments
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-tty")) {
                if (i + 1 < args.length) {
                    i++;
                    String[] formats = args[i].split(",");
                    if (formats.length == 0) {
                        errorln(Strings.get("ttyFormatError")); //OK
                    }
                    for (String format : formats) {
                        String fmt = format.trim();
                        switch (fmt) {
                            case "table":
                                startup.ttyFormat |= TtyInterface.FORMAT_TABLE;
                                break;
                            case "speed":
                                startup.ttyFormat |= TtyInterface.FORMAT_SPEED;
                                break;
                            case "tty":
                                startup.ttyFormat |= TtyInterface.FORMAT_TTY;
                                break;
                            case "halt":
                                startup.ttyFormat |= TtyInterface.FORMAT_HALT;
                                break;
                            case "stats":
                                startup.ttyFormat |= TtyInterface.FORMAT_STATISTICS;
                                break;
                            default:
                                errorln(Strings.get("ttyFormatError")); //OK
                                break;
                        }
                    }
                } else {
                    errorln(Strings.get("ttyFormatError")); //OK
                    return null;
                }
            } else if (arg.equals("-sub")) {
                if (i + 2 < args.length) {
                    File a = new File(args[i + 1]);
                    File b = new File(args[i + 2]);
                    if (startup.substitutions.containsKey(a)) {
                        errorln(Strings.get("argDuplicateSubstitutionError")); //OK
                        return null;
                    } else {
                        startup.substitutions.put(a, b);
                        i += 2;
                    }
                } else {
                    errorln(Strings.get("argTwoSubstitutionError")); //OK
                    return null;
                }
            } else if (arg.equals("-load")) {
                if (i + 1 < args.length) {
                    i++;
                    if (startup.loadFile != null) {
                        errorln(Strings.get("loadMultipleError")); //OK
                    }
                    startup.loadFile = new File(args[i]);
                } else {
                    errorln(Strings.get("loadNeedsFileError")); //OK
                    return null;
                }
            } else if (arg.equals("-empty")) {
                if (startup.templateFile != null || startup.templateEmpty || startup.templatePlain) {
                    errorln(Strings.get("argOneTemplateError")); //OK
                    return null;
                }
                startup.templateEmpty = true;
            } else if (arg.equals("-plain")) {
                if (startup.templateFile != null || startup.templateEmpty || startup.templatePlain) {
                    errorln(Strings.get("argOneTemplateError")); //OK
                    return null;
                }
                startup.templatePlain = true;
            } else if (arg.equals("-version")) {
                infoln(Main.VERSION_NAME); //OK
                return null;
            } else if (arg.equals("-gates")) {
                i++;
                if (i >= args.length) {
                    printUsage();
                }
                String a = args[i];
                if (a.equals("shaped")) {
                    AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_SHAPED);
                } else if (a.equals("rectangular")) {
                    AppPreferences.GATE_SHAPE.set(AppPreferences.SHAPE_RECTANGULAR);
                } else {
                    errorln(Strings.get("argGatesOptionError")); //OK
                    System.exit(-1);
                }
            } else if (arg.equals("-locale")) {
                i++;
                if (i >= args.length) {
                    printUsage();
                }
                setLocale(args[i]);
            } else if (arg.equals("-accents")) {
                i++;
                if (i >= args.length) {
                    printUsage();
                }
                String a = args[i];
                if (a.equals("yes")) {
                    AppPreferences.ACCENTS_REPLACE.setBoolean(false);
                } else if (a.equals("no")) {
                    AppPreferences.ACCENTS_REPLACE.setBoolean(true);
                } else {
                    errorln(Strings.get("argAccentsOptionError")); //OK
                    System.exit(-1);
                }
            } else if (arg.equals("-template")) {
                if (startup.templateFile != null || startup.templateEmpty || startup.templatePlain) {
                    errorln(Strings.get("argOneTemplateError")); //OK
                    return null;
                }
                i++;
                if (i >= args.length) {
                    printUsage();
                }
                startup.templateFile = new File(args[i]);
                if (!startup.templateFile.exists()) {
                    errorln(StringUtil.format( //OK
                        Strings.get("templateMissingError"), args[i]));
                } else if (!startup.templateFile.canRead()) {
                    errorln(StringUtil.format( //OK
                        Strings.get("templateCannotReadError"), args[i]));
                }
            } else if (arg.equals("-nosplash")) {
                startup.showSplash = false;
//			} else if (arg.equals("-clearprefs")) {
                // already handled above
            } else if (arg.charAt(0) == '-') {
                printUsage();
                return null;
            } else {
                startup.filesToOpen.add(new File(arg));
            }
        }
        if (startup.isTty && startup.filesToOpen.isEmpty()) {
            errorln(Strings.get("ttyNeedsFileError")); //OK
            return null;
        }
        if (startup.loadFile != null && !startup.isTty) {
            errorln(Strings.get("loadNeedsTtyError")); //OK
            return null;
        }
        return startup;
    }

    private static void printUsage() {
        outputln(StringUtil.format(Strings.get("argUsage"), Startup.class.getName())); //OK
        outputln(""); //OK
        outputln(Strings.get("argOptionHeader")); //OK
        outputln("   " + Strings.get("argAccentsOption")); //OK
        outputln("   " + Strings.get("argClearOption")); //OK
        outputln("   " + Strings.get("argEmptyOption")); //OK
        outputln("   " + Strings.get("argGatesOption")); //OK
        outputln("   " + Strings.get("argHelpOption")); //OK
        outputln("   " + Strings.get("argLoadOption")); //OK
        outputln("   " + Strings.get("argLocaleOption")); //OK
        outputln("   " + Strings.get("argNoSplashOption")); //OK
        outputln("   " + Strings.get("argPlainOption")); //OK
        outputln("   " + Strings.get("argSubOption")); //OK
        outputln("   " + Strings.get("argTemplateOption")); //OK
        outputln("   " + Strings.get("argTtyOption")); //OK
        outputln("   " + Strings.get("argVersionOption")); //OK
        System.exit(-1);
    }

    private void doOpenFile(File file) {
        if (initialized) {
            ProjectActions.doOpen(null, null, file);
        } else {
            filesToOpen.add(file);
        }
    }

    private void doPrintFile(File file) {
        if (initialized) {
            Project toPrint = ProjectActions.doOpen(null, null, file);
            Print.doPrint(toPrint);
            if (toPrint != null) {
                toPrint.getFrame().dispose();
            } else {
                errorln("'toPrint' == null");
            }
        } else {
            filesToPrint.add(file);
        }
    }

    List<File> getFilesToOpen() {
        return filesToOpen;
    }

    File getLoadFile() {
        return loadFile;
    }

    int getTtyFormat() {
        return ttyFormat;
    }

    Map<File, File> getSubstitutions() {
        return Collections.unmodifiableMap(substitutions);
    }

    public void run() {
        if (isTty) {
            try {
                TtyInterface.run(this);
                return;
            } catch (Throwable t) {
                t.printStackTrace();
                System.exit(-1);
                return;
            }
        }

        // kick off the progress monitor
        // (The values used for progress values are based on a single run where
        // I loaded a large file.)
        if (showSplash) {
            try {
                monitor = new SplashScreen();
                monitor.setVisible(true);
            } catch (Throwable t) {
                monitor = null;
                showSplash = false;
            }
        }

        // pre-load the two basic component libraries, just so that the time
        // taken is shown separately in the progress bar.
        if (showSplash) {
            monitor.setProgress(SplashScreen.LIBRARIES);
        }
        Loader templateLoader = new Loader(monitor);
        int count = templateLoader.getBuiltin().getLibrary("Base").getTools().size()
            + templateLoader.getBuiltin().getLibrary("Gates").getTools().size();
        if (count < 0) {
            // this will never happen, but the optimizer doesn't know that...
            Logger.errorln("FATAL ERROR - no components", true, -1); //OK
        }

        // load in template
        loadTemplate(templateLoader, templateFile, templateEmpty);

        // now that the splash screen is almost gone, we do some last-minute
        // interface initialization
        if (showSplash) {
            monitor.setProgress(SplashScreen.GUI_INIT);
        }
        WindowManagers.initialize();
        if (MacCompatibility.isSwingUsingScreenMenuBar()) {
            MacCompatibility.setFramelessJMenuBar(new LogisimMenuBar(null, null));
        } else {
            new LogisimMenuBar(null, null);
            // most of the time occupied here will be in loading menus, which
            // will occur eventually anyway; we might as well do it when the
            // monitor says we are
        }

        // if user has double-clicked a file to open, we'll
        // use that as the file to open now.
        initialized = true;

        // load file
        if (filesToOpen.isEmpty()) {
            ProjectActions.doNew(monitor, true);
            if (showSplash) {
                monitor.close();
            }
        } else {
            boolean first = true;
            for (File fileToOpen : filesToOpen) {
                try {
                    ProjectActions.doOpen(monitor, fileToOpen, substitutions);
                } catch (LoadFailedException ex) {
                    errorln(fileToOpen.getName() + ": " + ex.getMessage()); //OK
                    System.exit(-1);
                }
                if (first) {
                    first = false;
                    if (showSplash) {
                        monitor.close();
                    }
                    monitor = null;
                }
            }
        }

        for (File fileToPrint : filesToPrint) {
            doPrintFile(fileToPrint);
        }
    }

    private void loadTemplate(Loader loader, File templateFile,
        boolean templateEmpty) {
        if (showSplash) {
            monitor.setProgress(SplashScreen.TEMPLATE_OPEN);
        }
        if (templateFile != null) {
            AppPreferences.setTemplateFile(templateFile);
            AppPreferences.setTemplateType(AppPreferences.TEMPLATE_CUSTOM);
        } else if (templateEmpty) {
            AppPreferences.setTemplateType(AppPreferences.TEMPLATE_EMPTY);
        } else if (templatePlain) {
            AppPreferences.setTemplateType(AppPreferences.TEMPLATE_PLAIN);
        }
    }
}

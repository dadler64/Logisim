/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.proj;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadFailedException;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.start.SplashScreen;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.StringUtil;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class ProjectActions {

    private ProjectActions() {
    }

    public static Project doNew(SplashScreen monitor) {
        return doNew(monitor, false);
    }

    public static Project doNew(SplashScreen monitor, boolean isStartupScreen) {
        if (monitor != null) {
            monitor.setProgress(SplashScreen.FILE_CREATE);
        }
        Loader loader = new Loader(monitor);
        InputStream templReader = AppPreferences.getTemplate().createStream();
        LogisimFile file = null;
        try {
            file = loader.openLogisimFile(templReader);
        } catch (IOException ex) {
            displayException(monitor, ex);
        } catch (LoadFailedException ex) {
            displayException(monitor, ex);
        } finally {
            try {
                templReader.close();
            } catch (IOException e) {
            }
        }
        if (file == null) {
            file = createEmptyFile(loader);
        }
        return completeProject(monitor, loader, file, isStartupScreen);
    }

    private static void displayException(Component parent, Exception ex) {
        String msg = StringUtil.format(Strings.get("templateOpenError"),
                ex.toString());
        String ttl = Strings.get("templateOpenErrorTitle");
        JOptionPane.showMessageDialog(parent, msg, ttl, JOptionPane.ERROR_MESSAGE);
    }

    private static LogisimFile createEmptyFile(Loader loader) {
        InputStream templReader = AppPreferences.getEmptyTemplate().createStream();
        LogisimFile file;
        try {
            file = loader.openLogisimFile(templReader);
        } catch (Throwable t) {
            file = LogisimFile.createNew(loader);
            file.addCircuit(new Circuit("main"));
        } finally {
            try {
                templReader.close();
            } catch (IOException e) {
            }
        }
        return file;
    }

    private static Project completeProject(SplashScreen monitor, Loader loader,
            LogisimFile file, boolean isStartup) {
        if (monitor != null) {
            monitor.setProgress(SplashScreen.PROJECT_CREATE);
        }
        Project ret = new Project(file);

        if (monitor != null) {
            monitor.setProgress(SplashScreen.FRAME_CREATE);
        }
        SwingUtilities.invokeLater(new CreateFrame(loader, ret, isStartup));
        return ret;
    }

    public static LogisimFile createNewFile(Project baseProject) {
        Loader loader = new Loader(baseProject == null ? null : baseProject.getFrame());
        InputStream templReader = AppPreferences.getTemplate().createStream();
        LogisimFile file;
        try {
            file = loader.openLogisimFile(templReader);
        } catch (IOException ex) {
            displayException(baseProject.getFrame(), ex);
            file = createEmptyFile(loader);
        } catch (LoadFailedException ex) {
            if (!ex.isShown()) {
                displayException(baseProject.getFrame(), ex);
            }
            file = createEmptyFile(loader);
        } finally {
            try {
                templReader.close();
            } catch (IOException e) {
            }
        }
        return file;
    }

    private static Frame createFrame(Project sourceProject, Project newProject) {
        if (sourceProject != null) {
            Frame frame = sourceProject.getFrame();
            if (frame != null) {
                frame.savePreferences();
            }
        }
        Frame newFrame = new Frame(newProject);
        newProject.setFrame(newFrame);
        return newFrame;
    }

    public static Project doNew(Project baseProject) {
        LogisimFile file = createNewFile(baseProject);
        Project newProj = new Project(file);
        Frame frame = createFrame(baseProject, newProj);
        frame.setVisible(true);
        frame.getCanvas().requestFocus();
        newProj.getLogisimFile().getLoader().setParent(frame);
        return newProj;
    }

    public static Project doOpen(SplashScreen monitor, File source,
            Map<File, File> substitutions) throws LoadFailedException {
        if (monitor != null) {
            monitor.setProgress(SplashScreen.FILE_LOAD);
        }
        Loader loader = new Loader(monitor);
        LogisimFile file = loader.openLogisimFile(source, substitutions);
        AppPreferences.updateRecentFile(source);

        return completeProject(monitor, loader, file, false);
    }

    public static void doOpen(Component parent, Project baseProject) {
        JFileChooser chooser;
        if (baseProject != null) {
            Loader oldLoader = baseProject.getLogisimFile().getLoader();
            chooser = oldLoader.createChooser();
            if (oldLoader.getMainFile() != null) {
                chooser.setSelectedFile(oldLoader.getMainFile());
            }
        } else {
            chooser = JFileChoosers.create();
        }
        chooser.setFileFilter(Loader.LOGISIM_FILTER);

        int returnVal = chooser.showOpenDialog(parent);
        if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File selected = chooser.getSelectedFile();
        if (selected != null) {
            doOpen(parent, baseProject, selected);
        }
    }

    public static Project doOpen(Component parent,
            Project baseProject, File f) {
        Project proj = Projects.findProjectFor(f);
        Loader loader = null;
        if (proj != null) {
            proj.getFrame().toFront();
            loader = proj.getLogisimFile().getLoader();
            if (proj.isFileDirty()) {
                String message = StringUtil.format(Strings.get("openAlreadyMessage"),
                        proj.getLogisimFile().getName());
                String[] options = {
                        Strings.get("openAlreadyLoseChangesOption"),
                        Strings.get("openAlreadyNewWindowOption"),
                        Strings.get("openAlreadyCancelOption"),
                };
                int result = JOptionPane.showOptionDialog(proj.getFrame(),
                        message, Strings.get("openAlreadyTitle"), 0,
                        JOptionPane.QUESTION_MESSAGE, null,
                        options, options[2]);
                if (result == 0) {
                    // keep proj as is, so that load happens into the window
                } else if (result == 1) {
                    proj = null; // we'll create a new project
                } else {
                    return proj;
                }
            }
        }

        if (proj == null && baseProject != null && baseProject.isStartupScreen()) {
            proj = baseProject;
            proj.setStartupScreen(false);
            loader = baseProject.getLogisimFile().getLoader();
        } else {
            loader = new Loader(baseProject == null ? parent : baseProject.getFrame());
        }

        try {
            LogisimFile lib = loader.openLogisimFile(f);
            AppPreferences.updateRecentFile(f);
            if (lib == null) {
                return null;
            }
            if (proj == null) {
                proj = new Project(lib);
            } else {
                proj.setLogisimFile(lib);
            }
        } catch (LoadFailedException ex) {
            if (!ex.isShown()) {
                JOptionPane.showMessageDialog(parent,
                        StringUtil.format(Strings.get("fileOpenError"),
                                ex.toString()),
                        Strings.get("fileOpenErrorTitle"),
                        JOptionPane.ERROR_MESSAGE);
            }
            return null;
        }

        Frame frame = proj.getFrame();
        if (frame == null) {
            frame = createFrame(baseProject, proj);
        }
        frame.setVisible(true);
        frame.toFront();
        frame.getCanvas().requestFocus();
        proj.getLogisimFile().getLoader().setParent(frame);
        return proj;
    }

    // returns true if save is completed
    public static boolean doSaveAs(Project project) {
        Loader loader = project.getLogisimFile().getLoader();
        JFileChooser chooser = loader.createChooser();
        chooser.setFileFilter(Loader.LOGISIM_FILTER);
        if (loader.getMainFile() != null) {
            chooser.setSelectedFile(loader.getMainFile());
        }
        int showDialog = chooser.showSaveDialog(project.getFrame());    // This shows the dialog to save your file
        if (showDialog != JFileChooser.APPROVE_OPTION) {
            return false;
        }

        File selectedFile = chooser.getSelectedFile();
        String circuitExtension = Loader.LOGISIM_EXTENSION;
        if (!selectedFile.getName().endsWith(circuitExtension)) { // See if the file has a '.circ' extension
            String old = selectedFile.getName();
            int dotIndex = old.lastIndexOf('.');
            if (dotIndex < 0 || !Pattern.matches("\\.\\p{L}{2,}[0-9]?", old.substring(dotIndex))) {
                selectedFile = new File(selectedFile.getParentFile(), old + circuitExtension);
            } else {
                String extension = old.substring(dotIndex);
                String ttl = Strings.get("replaceExtensionTitle");
                String message = Strings.get("replaceExtensionMessage", extension);
                Object[] options = {
                        Strings.get("replaceExtensionReplaceOpt", extension),
                        Strings.get("replaceExtensionAddOpt", circuitExtension),
                        Strings.get("replaceExtensionKeepOpt")
                };
                JOptionPane dialog = new JOptionPane(message);
                dialog.setMessageType(JOptionPane.QUESTION_MESSAGE);
                dialog.setOptions(options);
                dialog.createDialog(project.getFrame(), ttl).setVisible(true);

                Object result = dialog.getValue();
                if (result == options[0]) {
                    String name = old.substring(0, dotIndex) + circuitExtension;
                    selectedFile = new File(selectedFile.getParentFile(), name);
                } else if (result == options[1]) {
                    selectedFile = new File(selectedFile.getParentFile(), old + circuitExtension);
                }
            }
        }

        if (selectedFile.exists()) {
            int confirm = JOptionPane.showConfirmDialog(project.getFrame(),
                    Strings.get("confirmOverwriteMessage"),
                    Strings.get("confirmOverwriteTitle"),
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        return doSave(project, selectedFile);
    }

    public static boolean doSave(Project project) {
        Loader loader = project.getLogisimFile().getLoader();
        File mainFile = loader.getMainFile(); // TODO: Is this showing up as null?
        if (mainFile == null) {
            return doSaveAs(project);
        } else {
            return doSave(project, mainFile);
        }
    }

    private static boolean doSave(Project project, File file) {
        Loader loader = project.getLogisimFile().getLoader();
        Tool oldTool = project.getTool();
        project.setTool(null);
        boolean isSaved = loader.save(project.getLogisimFile(), file);
        if (isSaved) {
            AppPreferences.updateRecentFile(file);
            project.setFileAsClean();
        }
        project.setTool(oldTool);
        return isSaved;
    }

    public static void doQuit() {
        Frame top = Projects.getTopFrame();
        top.savePreferences();

        for (Project project : new ArrayList<>(Projects.getOpenProjects())) {
            if (!project.confirmClose(Strings.get("confirmQuitTitle"))) {
                return;
            }
        }
        System.exit(0);
    }

    private static class CreateFrame implements Runnable {

        private Loader loader;
        private Project project;
        private boolean isStartupScreen;

        private CreateFrame(Loader loader, Project project, boolean isStartup) {
            this.loader = loader;
            this.project = project;
            this.isStartupScreen = isStartup;
        }

        public void run() {
            Frame frame = createFrame(null, project);
            frame.setVisible(true);
            frame.toFront();
            frame.getCanvas().requestFocus();
            loader.setParent(frame);
            if (isStartupScreen) {
                project.setStartupScreen(true);
            }
        }
    }
}

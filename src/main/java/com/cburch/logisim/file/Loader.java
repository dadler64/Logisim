/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.logisim.std.Builtin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.MacCompatibility;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.ZipClassLoader;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.filechooser.FileFilter;

public class Loader implements LibraryLoader {

    public static final String LOGISIM_EXTENSION = ".circ";
    public static final FileFilter LOGISIM_FILTER = new LogisimFileFilter();
    public static final FileFilter JAR_FILTER = new JarFileFilter();
    // fixed
    private Component parent;
    private Builtin builtin = new Builtin();
    // to be cleared with each new file
    private File mainFile = null;
    private Stack<File> filesOpening = new Stack<>();
    private Map<File, File> substitutions = new HashMap<>();

    public Loader(Component parent) {
        this.parent = parent;
        clear();
    }

    private static File determineBackupName(File baseFile) {
        File directory = baseFile.getParentFile();
        String fileName = baseFile.getName();
        if (fileName.endsWith(LOGISIM_EXTENSION)) {
            fileName = fileName.substring(0, fileName.length() - LOGISIM_EXTENSION.length());
        }
        for (int i = 1; i <= 20; i++) {
            String extension = i == 1 ? ".bak" : (".bak" + i);
            File candidate = new File(directory, fileName + extension);
            if (!candidate.exists()) {
                return candidate;
            }
        }
        return null;
    }

    private static void recoverBackup(File backup, File destination) {
        if (backup != null && backup.exists()) {
            if (destination.exists()) {
                destination.delete();
            }
            backup.renameTo(destination);
        }
    }

    public Builtin getBuiltin() {
        return builtin;
    }

    public void setParent(Component value) {
        parent = value;
    }

    private File getSubstitution(File source) {
        File returnFile = substitutions.get(source);
        return returnFile == null ? source : returnFile;
    }

    //
    // file chooser related methods
    //
    public File getMainFile() {
        return mainFile;
    }

    private void setMainFile(File file) {
        mainFile = file;
    }

    public JFileChooser createChooser() {
        return JFileChoosers.createAt(getCurrentDirectory());
    }

    // used here and in LibraryManager only
    File getCurrentDirectory() {
        File file;
        if (!filesOpening.empty()) {
            file = filesOpening.peek();
        } else {
            file = mainFile;
        }
        return file == null ? null : file.getParentFile();
    }

    //
    // more substantive methods accessed from outside this package
    //
    public void clear() {
        filesOpening.clear();
        mainFile = null;
    }

    public LogisimFile openLogisimFile(File file, Map<File, File> substitutions)
            throws LoadFailedException {
        this.substitutions = substitutions;
        try {
            return openLogisimFile(file);
        } finally {
            this.substitutions = Collections.emptyMap();
        }
    }

    public LogisimFile openLogisimFile(File file) throws LoadFailedException {
        try {
            LogisimFile logisimFile = loadLogisimFile(file);
            if (logisimFile != null) {
                setMainFile(file);
            }
            showMessages(logisimFile);
            return logisimFile;
        } catch (LoaderException loaderException) {
            throw new LoadFailedException(loaderException.getMessage(), loaderException.isShown());
        }
    }

    public LogisimFile openLogisimFile(InputStream reader) throws LoadFailedException, IOException {
        LogisimFile logisimFile;
        try {
            logisimFile = LogisimFile.load(reader, this);
        } catch (LoaderException loaderException) {
            return null;
        }
        showMessages(logisimFile);
        return logisimFile;
    }

    public Library loadLogisimLibrary(File file) {
        File actualFile = getSubstitution(file);
        LoadedLibrary loadedLibrary = LibraryManager.instance.loadLogisimLibrary(this, actualFile);
        if (loadedLibrary != null) {
            LogisimFile returnFile = (LogisimFile) loadedLibrary.getLibrary();
            showMessages(returnFile);
        }
        return loadedLibrary;
    }

    public Library loadJarLibrary(File file, String className) {
        File actualFile = getSubstitution(file);
        return LibraryManager.instance.loadJarLibrary(this, actualFile, className);
    }

    public void reload(LoadedLibrary library) {
        LibraryManager.instance.reload(this, library);
    }

    public boolean save(LogisimFile file, File destination) {
        Library referenceLibrary = LibraryManager.instance.findReference(file, destination);
        if (referenceLibrary != null) {
            JOptionPane.showMessageDialog(parent,
                    StringUtil.format(Strings.get("fileCircularError"), referenceLibrary.getDisplayName()),
                    Strings.get("fileSaveErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        File backupFile = determineBackupName(destination);
        boolean backupCreated = backupFile != null && destination.renameTo(backupFile);

        FileOutputStream outputStream = null;
        try {
            try {
                MacCompatibility.setFileCreatorAndType(destination, "LGSM", "circ");
            } catch (IOException ignored) { }

            outputStream = new FileOutputStream(destination);
            file.write(outputStream, this);
            file.setName(toProjectName(destination));

            File oldFile = getMainFile();
            setMainFile(destination);
            LibraryManager.instance.fileSaved(this, destination, oldFile, file);
        } catch (IOException ioException) {
            if (backupCreated) {
                recoverBackup(backupFile, destination);
            }
            if (destination.exists() && destination.length() == 0) {
                destination.delete();
            }
            JOptionPane.showMessageDialog(parent,
                    StringUtil.format(Strings.get("fileSaveError"),
                    ioException.toString()),
                    Strings.get("fileSaveErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ioException) {
                    if (backupCreated) {
                        recoverBackup(backupFile, destination);
                    }
                    if (destination.exists() && destination.length() == 0) {
                        destination.delete();
                    }
                    JOptionPane.showMessageDialog(parent,
                            StringUtil.format(Strings.get("fileSaveCloseError"),
                            ioException.toString()),
                            Strings.get("fileSaveErrorTitle"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        }

        if (!destination.exists() || destination.length() == 0) {
            if (backupCreated && backupFile != null && backupFile.exists()) {
                recoverBackup(backupFile, destination);
            } else {
                destination.delete();
            }
            JOptionPane.showMessageDialog(parent,
                    Strings.get("fileSaveZeroError"),
                    Strings.get("fileSaveErrorTitle"),
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (backupCreated && backupFile.exists()) {
            backupFile.delete();
        }
        return true;
    }

    //
    // methods for LibraryManager
    //
    LogisimFile loadLogisimFile(File requestedFile) throws LoadFailedException {
        File actualFile = getSubstitution(requestedFile);
        for (File fileOpening : filesOpening) {
            if (fileOpening.equals(actualFile)) {
                throw new LoadFailedException(StringUtil.format(Strings.get("logisimCircularError"),
                        toProjectName(actualFile)));
            }
        }

        LogisimFile logisimFile;
        filesOpening.push(actualFile);
        try {
            logisimFile = LogisimFile.load(actualFile, this);
        } catch (IOException e) {
            throw new LoadFailedException(StringUtil.format(Strings.get("logisimLoadError"),
                    toProjectName(actualFile), e.toString()));
        } finally {
            filesOpening.pop();
        }
        logisimFile.setName(toProjectName(actualFile));
        return logisimFile;
    }

    Library loadJarFile(File requestedFile, String className) throws LoadFailedException {
        File actual = getSubstitution(requestedFile);
        // Up until 2.1.8, this was written to use a URLClassLoader, which
        // worked pretty well, except that the class never releases its file
        // handles. For this reason, with 2.2.0, it's been switched to use
        // a custom-written class ZipClassLoader instead. The ZipClassLoader
        // is based on something downloaded off a forum, and I'm not as sure
        // that it works as well. It certainly does more file accesses.

        // Anyway, here's the line for this new version:
        ZipClassLoader loader = new ZipClassLoader(actual);

        // And here's the code that was present up until 2.1.8, and which I
        // know to work well except for the closing-files bit. If necessary, we
        // can revert by deleting the above declaration and reinstating the below.
		/*
		URL url;
		try {
			url = new URL("file", "localhost", file.getCanonicalPath());
		} catch (MalformedURLException e1) {
			throw new LoadFailedException("Internal error: Malformed URL");
		} catch (IOException e1) {
			throw new LoadFailedException(Strings.get("jarNotOpenedError"));
		}
		URLClassLoader loader = new URLClassLoader(new URL[] { url });
		*/

        // load library class from loader
        Class<?> returnClass;
        try {
            returnClass = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new LoadFailedException(StringUtil.format(Strings.get("jarClassNotFoundError"), className));
        }
        if (!(Library.class.isAssignableFrom(returnClass))) {
            throw new LoadFailedException(StringUtil.format(Strings.get("jarClassNotLibraryError"), className));
        }

        // instantiate library
        Library returnLibrary;
        try {
            returnLibrary = (Library) returnClass.newInstance();
        } catch (Exception e) {
            throw new LoadFailedException(StringUtil.format(Strings.get("jarLibraryNotCreatedError"), className));
        }
        return returnLibrary;
    }

    //
    // Library methods
    //
    public Library loadLibrary(String desc) {
        return LibraryManager.instance.loadLibrary(this, desc);
    }

    public String getDescriptor(Library library) {
        return LibraryManager.instance.getDescriptor(this, library);
    }

    public void showError(String description) {
        if (!filesOpening.empty()) {
            File topFile = filesOpening.peek();
            String init = toProjectName(topFile) + ":";
            if (description.contains("\n")) {
                description = init + "\n" + description;
            } else {
                description = init + " " + description;
            }
        }

        if (description.contains("\n") || description.length() > 60) {
            int lines = 1;
            for (int position = description.indexOf('\n'); position >= 0;
                    position = description.indexOf('\n', position + 1)) {
                lines++;
            }
            lines = Math.max(4, Math.min(lines, 7));

            JTextArea textArea = new JTextArea(lines, 60);
            textArea.setEditable(false);
            textArea.setText(description);
            textArea.setCaretPosition(0);

            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(350, 150));
            JOptionPane.showMessageDialog(parent, scrollPane,
                    Strings.get("fileErrorTitle"), JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, description,
                    Strings.get("fileErrorTitle"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessages(LogisimFile sourceLogisimFile) {
        if (sourceLogisimFile == null) {
            return;
        }
        String message = sourceLogisimFile.getMessage();
        while (message != null) {
            JOptionPane.showMessageDialog(
                    parent,
                    message,
                    Strings.get("fileMessageTitle"),
                    JOptionPane.INFORMATION_MESSAGE);
            message = sourceLogisimFile.getMessage();
        }
    }

    //
    // helper methods
    //
    File getFileFor(String name, FileFilter filter) {
        // Determine the actual file name.
        File file = new File(name);
        if (!file.isAbsolute()) {
            File currentDirectory = getCurrentDirectory();
            if (currentDirectory != null) {
                file = new File(currentDirectory, name);
            }
        }
        while (!file.canRead()) {
            // It doesn't exist. Figure it out from the user.
            JOptionPane.showMessageDialog(parent, StringUtil.format(Strings.get("fileLibraryMissingError"), file.getName()));
            JFileChooser fileChooser = createChooser();
            fileChooser.setFileFilter(filter);
            fileChooser.setDialogTitle(StringUtil.format(Strings.get("fileLibraryMissingTitle"), file.getName()));
            int action = fileChooser.showDialog(parent, Strings.get("fileLibraryMissingButton"));
            if (action != JFileChooser.APPROVE_OPTION) {
                throw new LoaderException(Strings.get("fileLoadCanceledError"));
            }
            file = fileChooser.getSelectedFile();
        }
        return file;
    }

    private String toProjectName(File file) {
        String returnFile = file.getName();
        if (returnFile.endsWith(LOGISIM_EXTENSION)) {
            return returnFile.substring(0, returnFile.length() - LOGISIM_EXTENSION.length());
        } else {
            return returnFile;
        }
    }

    private static class LogisimFileFilter extends FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || file.getName().endsWith(LOGISIM_EXTENSION);
        }

        @Override
        public String getDescription() {
            return Strings.get("logisimFileFilter");
        }
    }

    private static class JarFileFilter extends FileFilter {

        @Override
        public boolean accept(File file) {
            return file.isDirectory() || file.getName().endsWith(".jar");
        }

        @Override
        public String getDescription() {
            return Strings.get("jarFileFilter");
        }
    }
}
/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.logisim.tools.Library;
import com.cburch.logisim.util.StringUtil;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.WeakHashMap;

class LibraryManager {

    public static final LibraryManager instance = new LibraryManager();

    private static final char DESCRIPTOR_SEPERATOR = '#';
    private HashMap<LibraryDescriptor, WeakReference<LoadedLibrary>> fileMap;
    private WeakHashMap<LoadedLibrary, LibraryDescriptor> invMap;

    private LibraryManager() {
        fileMap = new HashMap<>();
        invMap = new WeakHashMap<>();
        ProjectsDirty.initialize();
    }

    private static String toRelative(Loader loader, File file) {
        File currentDirectory = loader.getCurrentDirectory();
        if (currentDirectory == null) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                return file.toString();
            }
        }

        File fileDirectory = file.getParentFile();
        if (fileDirectory != null) {
            if (currentDirectory.equals(fileDirectory)) {
                return file.getName();
            } else if (currentDirectory.equals(fileDirectory.getParentFile())) {
                return fileDirectory.getName() + "/" + file.getName();
            } else if (fileDirectory.equals(currentDirectory.getParentFile())) {
                return "../" + file.getName();
            }
        }
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return file.toString();
        }
    }

    void setDirty(File file, boolean isDirty) {
        LoadedLibrary library = findKnown(file);
        if (library != null) {
            library.setDirty(isDirty);
        }
    }

    Collection<LogisimFile> getLogisimLibraries() {
        ArrayList<LogisimFile> logisimFiles = new ArrayList<>();
        for (LoadedLibrary library : invMap.keySet()) {
            if (library.getLibrary() instanceof LogisimFile) {
                logisimFiles.add((LogisimFile) library.getLibrary());
            }
        }
        return logisimFiles;
    }

    public Library loadLibrary(Loader loader, String descriptor) {
        // It may already be loaded.
        // Otherwise we'll have to decode it.
        int separator = descriptor.indexOf(DESCRIPTOR_SEPERATOR);
        if (separator < 0) {
            loader.showError(StringUtil.format(Strings.get("fileDescriptorError"), descriptor));
            return null;
        }
        String type = descriptor.substring(0, separator);
        String name = descriptor.substring(separator + 1);

        switch (type) {
            case "":
                Library library = loader.getBuiltin().getLibrary(name);
                if (library == null) {
                    loader.showError(StringUtil.format(Strings.get("fileBuiltinMissingError"), name));
                    return null;
                }
                return library;
            case "file": {
                File fileToRead = loader.getFileFor(name, Loader.LOGISIM_FILTER);
                return loadLogisimLibrary(loader, fileToRead);
            }
            case "jar": {
                int separatorLocation = name.lastIndexOf(DESCRIPTOR_SEPERATOR);
                String fileName = name.substring(0, separatorLocation);
                String className = name.substring(separatorLocation + 1);
                File fileToRead = loader.getFileFor(fileName, Loader.JAR_FILTER);
                return loadJarLibrary(loader, fileToRead, className);
            }
            default:
                loader.showError(StringUtil.format(Strings.get("fileTypeError"), type, descriptor));
                return null;
        }
    }

    public LoadedLibrary loadLogisimLibrary(Loader loader, File fileToRead) {
        LoadedLibrary loadedLibrary = findKnown(fileToRead);
        if (loadedLibrary != null) {
            return loadedLibrary;
        }

        try {
            loadedLibrary = new LoadedLibrary(loader.loadLogisimFile(fileToRead));
        } catch (LoadFailedException e) {
            loader.showError(e.getMessage());
            return null;
        }

        LogisimProjectDescriptor descriptor = new LogisimProjectDescriptor(fileToRead);
        fileMap.put(descriptor, new WeakReference<>(loadedLibrary));
        invMap.put(loadedLibrary, descriptor);
        return loadedLibrary;
    }

    public LoadedLibrary loadJarLibrary(Loader loader, File fileToRead, String className) {
        JarDescriptor jarDescriptor = new JarDescriptor(fileToRead, className);
        LoadedLibrary loadedLibrary = findKnown(jarDescriptor);
        if (loadedLibrary != null) {
            return loadedLibrary;
        }

        try {
            loadedLibrary = new LoadedLibrary(loader.loadJarFile(fileToRead, className));
        } catch (LoadFailedException e) {
            loader.showError(e.getMessage());
            return null;
        }

        fileMap.put(jarDescriptor, new WeakReference<>(loadedLibrary));
        invMap.put(loadedLibrary, jarDescriptor);
        return loadedLibrary;
    }

    public void reload(Loader loader, LoadedLibrary loadedLibrary) {
        LibraryDescriptor descriptor = invMap.get(loadedLibrary);
        if (descriptor == null) {
            loader.showError(StringUtil.format(Strings.get("unknownLibraryFileError"),
                    loadedLibrary.getDisplayName()));
        } else {
            try {
                descriptor.setBase(loader, loadedLibrary);
            } catch (LoadFailedException e) {
                loader.showError(e.getMessage());
            }
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public Library findReference(LogisimFile file, File queryFile) {
        for (Library library : file.getLibraries()) {
            LibraryDescriptor descriptor = invMap.get(library);
            if (descriptor != null && descriptor.concernsFile(queryFile)) {
                return library;
            }
            if (library instanceof LoadedLibrary) {
                LoadedLibrary loadedLibrary = (LoadedLibrary) library;
                if (loadedLibrary.getLibrary() instanceof LogisimFile) {
                    LogisimFile loadedProject = (LogisimFile) loadedLibrary.getLibrary();
                    Library referenceLibrary = findReference(loadedProject, queryFile);
                    if (referenceLibrary != null) {
                        return library;
                    }
                }
            }
        }
        return null;
    }

    public void fileSaved(Loader loader, File destination, File oldFile, LogisimFile file) {
        LoadedLibrary oldLibrary = findKnown(oldFile);
        if (oldLibrary != null) {
            oldLibrary.setDirty(false);
        }

        LoadedLibrary loadedLibrary = findKnown(destination);
        if (loadedLibrary != null) {
            LogisimFile clone = file.cloneLogisimFile(loader);
            clone.setName(file.getName());
            clone.setDirty(false);
            loadedLibrary.setLibrary(clone);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    public String getDescriptor(Loader loader, Library library) {
        if (loader.getBuiltin().getLibraries().contains(library)) {
            return DESCRIPTOR_SEPERATOR + library.getName();
        } else {
            LibraryDescriptor descriptor = invMap.get(library);
            if (descriptor != null) {
                return descriptor.toDescriptor(loader);
            } else {
                throw new LoaderException(StringUtil.format(Strings.get("fileDescriptorUnknownError"),
                        library.getDisplayName()));
            }
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    private LoadedLibrary findKnown(Object key) {
        WeakReference<LoadedLibrary> returnLibraryReference = fileMap.get(key);
        if (returnLibraryReference == null) {
            return null;
        } else {
            LoadedLibrary loadedLibrary = returnLibraryReference.get();
            if (loadedLibrary == null) {
                fileMap.remove(key);
                return null;
            } else {
                return loadedLibrary;
            }
        }
    }

    private static abstract class LibraryDescriptor {

        abstract boolean concernsFile(File query);

        abstract String toDescriptor(Loader loader);

        abstract void setBase(Loader loader, LoadedLibrary loadedLibrary)
                throws LoadFailedException;
    }

    private static class LogisimProjectDescriptor extends LibraryDescriptor {

        private File file;

        private LogisimProjectDescriptor(File file) {
            this.file = file;
        }

        @Override
        boolean concernsFile(File query) {
            return file.equals(query);
        }

        @Override
        String toDescriptor(Loader loader) {
            return "file#" + toRelative(loader, file);
        }

        @Override
        void setBase(Loader loader, LoadedLibrary loadedLibrary) throws LoadFailedException {
            loadedLibrary.setLibrary(loader.loadLogisimFile(file));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof LogisimProjectDescriptor)) {
                return false;
            }
            LogisimProjectDescriptor descriptor = (LogisimProjectDescriptor) other;
            return this.file.equals(descriptor.file);
        }

        @Override
        public int hashCode() {
            return file.hashCode();
        }
    }

    private static class JarDescriptor extends LibraryDescriptor {

        private File file;
        private String className;

        private JarDescriptor(File file, String className) {
            this.file = file;
            this.className = className;
        }

        @Override
        boolean concernsFile(File query) {
            return file.equals(query);
        }

        @Override
        String toDescriptor(Loader loader) {
            return "jar#" + toRelative(loader, file) + DESCRIPTOR_SEPERATOR + className;
        }

        @Override
        void setBase(Loader loader, LoadedLibrary loadedLibrary) throws LoadFailedException {
            loadedLibrary.setLibrary(loader.loadJarFile(file, className));
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof JarDescriptor)) {
                return false;
            }
            JarDescriptor jarDescriptor = (JarDescriptor) other;
            return this.file.equals(jarDescriptor.file) && this.className.equals(jarDescriptor.className);
        }

        @Override
        public int hashCode() {
            return file.hashCode() * 31 + className.hashCode();
        }
    }
}
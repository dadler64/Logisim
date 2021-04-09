/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.opts.OptionsFrame;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

class MenuFile extends Menu implements ActionListener {

    private final LogisimMenuBar menubar;
    private final JMenuItem newi = new JMenuItem();
    private final JMenuItem open = new JMenuItem();
    private final OpenRecent openRecent;
    private final JMenuItem close = new JMenuItem();
    private final JMenuItem save = new JMenuItem();
    private final JMenuItem saveAs = new JMenuItem();
    private final MenuItemImpl print = new MenuItemImpl(this, LogisimMenuBar.PRINT);
    private final MenuItemImpl exportImage = new MenuItemImpl(this, LogisimMenuBar.EXPORT_IMAGE);
    private final JMenuItem prefs = new JMenuItem();
    private final JMenuItem quit = new JMenuItem();

    public MenuFile(LogisimMenuBar menubar) {
        this.menubar = menubar;
        openRecent = new OpenRecent(menubar);

        int menuMask = getToolkit().getMenuShortcutKeyMaskEx();

        newi.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_N, menuMask));
        open.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_O, menuMask));
        close.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_W, menuMask | InputEvent.SHIFT_DOWN_MASK));
        save.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, menuMask));
        saveAs.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, menuMask | InputEvent.SHIFT_DOWN_MASK));
        print.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_P, menuMask));
        quit.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_Q, menuMask));

        add(newi);
        add(open);
        add(openRecent);
        addSeparator();
        add(close);
        add(save);
        add(saveAs);
        addSeparator();
        add(exportImage);
        add(print);
        if (!MacCompatibility.isPreferencesAutomaticallyPresent()) {
            addSeparator();
            add(prefs);
        }
        if (!MacCompatibility.isQuitAutomaticallyPresent()) {
            addSeparator();
            add(quit);
        }

        Project proj = menubar.getMenuProject();
        newi.addActionListener(this);
        open.addActionListener(this);
        if (proj == null) {
            close.setEnabled(false);
            save.setEnabled(false);
            saveAs.setEnabled(false);
        } else {
            close.addActionListener(this);
            save.addActionListener(this);
            saveAs.addActionListener(this);
        }
        menubar.registerItem(LogisimMenuBar.EXPORT_IMAGE, exportImage);
        menubar.registerItem(LogisimMenuBar.PRINT, print);
        prefs.addActionListener(this);
        quit.addActionListener(this);
    }

    public void localeChanged() {
        this.setText(Strings.get("fileMenu"));
        newi.setText(Strings.get("fileNewItem"));
        open.setText(Strings.get("fileOpenItem"));
        openRecent.localeChanged();
        close.setText(Strings.get("fileCloseItem"));
        save.setText(Strings.get("fileSaveItem"));
        saveAs.setText(Strings.get("fileSaveAsItem"));
        exportImage.setText(Strings.get("fileExportImageItem"));
        print.setText(Strings.get("filePrintItem"));
        prefs.setText(Strings.get("filePreferencesItem"));
        quit.setText(Strings.get("fileQuitItem"));
    }

    @Override
    void computeEnabled() {
        setEnabled(true);
        menubar.fireEnableChanged();
    }

    public void actionPerformed(ActionEvent event) {
        Object eventSource = event.getSource();
        Project project = menubar.getMenuProject();
        if (eventSource == newi) {
            ProjectActions.doNew(project);
        } else if (eventSource == open) {
            ProjectActions.doOpen(project == null ? null : project.getFrame().getCanvas(), project);
        } else if (eventSource == close) {
            Frame frame = project.getFrame();
            if (frame.confirmClose()) {
                frame.dispose();
                OptionsFrame optionsFrame = project.getOptionsFrame(false);
                if (optionsFrame != null) {
                    optionsFrame.dispose();
                }
            }
        } else if (eventSource == save) {
            ProjectActions.doSave(project);
        } else if (eventSource == saveAs) {
            ProjectActions.doSaveAs(project);
        } else if (eventSource == prefs) {
            PreferencesFrame.showPreferences();
        } else if (eventSource == quit) {
            ProjectActions.doQuit();
        }
    }
}

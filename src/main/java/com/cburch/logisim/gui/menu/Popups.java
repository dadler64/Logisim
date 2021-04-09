/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.file.LoadedLibrary;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.StatisticsDialog;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

public class Popups {

    public static JPopupMenu forCircuit(Project project, AddTool tool, Circuit circuit) {
        return new CircuitPopup(project, tool, circuit);
    }

    public static JPopupMenu forTool(Project project, Tool tool) {
        return null;
    }

    public static JPopupMenu forProject(Project project) {
        return new ProjectPopup(project);
    }

    public static JPopupMenu forLibrary(Project project, Library library, boolean isTop) {
        return new LibraryPopup(project, library, isTop);
    }

    private static class ProjectPopup extends JPopupMenu implements ActionListener {

        private final Project project;
        private final JMenuItem addItem = new JMenuItem(Strings.get("projectAddCircuitItem"));
        private final JMenu loadItem = new JMenu(Strings.get("projectLoadLibraryItem"));
        private final JMenuItem loadBuiltinItem = new JMenuItem(Strings.get("projectLoadBuiltinItem"));
        private final JMenuItem loadLogisimItem = new JMenuItem(Strings.get("projectLoadLogisimItem"));
        private final JMenuItem loadJarItem = new JMenuItem(Strings.get("projectLoadJarItem"));

        private ProjectPopup(Project project) {
            super(Strings.get("projMenu"));
            this.project = project;

            loadItem.add(loadBuiltinItem);
            loadBuiltinItem.addActionListener(this);
            loadItem.add(loadLogisimItem);
            loadLogisimItem.addActionListener(this);
            loadItem.add(loadJarItem);
            loadJarItem.addActionListener(this);

            add(addItem);
            addItem.addActionListener(this);
            add(loadItem);
        }

        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source == addItem) {
                ProjectCircuitActions.doAddCircuit(project);
            } else if (source == loadBuiltinItem) {
                ProjectLibraryActions.doLoadBuiltinLibrary(project);
            } else if (source == loadLogisimItem) {
                ProjectLibraryActions.doLoadLogisimLibrary(project);
            } else if (source == loadJarItem) {
                ProjectLibraryActions.doLoadJarLibrary(project);
            }
        }
    }

    private static class LibraryPopup extends JPopupMenu implements ActionListener {

        private final Project project;
        private final Library library;
        private final JMenuItem unloadItem = new JMenuItem(Strings.get("projectUnloadLibraryItem"));
        private final JMenuItem reloadItem = new JMenuItem(Strings.get("projectReloadLibraryItem"));

        private LibraryPopup(Project project, Library library, boolean isTop) {
            super(Strings.get("libMenu"));
            this.project = project;
            this.library = library;

            add(unloadItem);
            unloadItem.addActionListener(this);
            add(reloadItem);
            reloadItem.addActionListener(this);
            unloadItem.setEnabled(isTop);
            reloadItem.setEnabled(isTop && library instanceof LoadedLibrary);
        }

        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source == unloadItem) {
                ProjectLibraryActions.doUnloadLibrary(project, library);
            } else if (source == reloadItem) {
                Loader loader = project.getLogisimFile().getLoader();
                loader.reload((LoadedLibrary) library);
            }
        }
    }

    private static class CircuitPopup extends JPopupMenu implements ActionListener {

        private final Project project;
        private final Tool tool;
        private final Circuit circuit;
        private final JMenuItem analyzeItem = new JMenuItem(Strings.get("projectAnalyzeCircuitItem"));
        private final JMenuItem statsItem = new JMenuItem(Strings.get("projectGetCircuitStatisticsItem"));
        private final JMenuItem mainItem = new JMenuItem(Strings.get("projectSetAsMainItem"));
        private final JMenuItem removeItem = new JMenuItem(Strings.get("projectRemoveCircuitItem"));
        private final JMenuItem editLayoutItem = new JMenuItem(Strings.get("projectEditCircuitLayoutItem"));
        private final JMenuItem editAppearanceItem = new JMenuItem(Strings.get("projectEditCircuitAppearanceItem"));

        private CircuitPopup(Project project, Tool tool, Circuit circuit) {
            super(Strings.get("circuitMenu"));
            this.project = project;
            this.tool = tool;
            this.circuit = circuit;

            add(editLayoutItem);
            editLayoutItem.addActionListener(this);
            add(editAppearanceItem);
            editAppearanceItem.addActionListener(this);
            add(analyzeItem);
            analyzeItem.addActionListener(this);
            add(statsItem);
            statsItem.addActionListener(this);
            addSeparator();
            add(mainItem);
            mainItem.addActionListener(this);
            add(removeItem);
            removeItem.addActionListener(this);

            boolean canChange = project.getLogisimFile().contains(circuit);
            LogisimFile file = project.getLogisimFile();
            if (circuit == project.getCurrentCircuit()) {
                if (project.getFrame().getEditorView().equals(Frame.EDIT_APPEARANCE)) {
                    editAppearanceItem.setEnabled(false);
                } else {
                    editLayoutItem.setEnabled(false);
                }
            }
            mainItem.setEnabled(canChange && file.getMainCircuit() != circuit);
            removeItem.setEnabled(canChange && file.getCircuitCount() > 1
                && project.getDependencies().canRemove(circuit));
        }

        public void actionPerformed(ActionEvent event) {
            Object source = event.getSource();
            if (source.equals(editLayoutItem)) {
                project.setCurrentCircuit(circuit);
                project.getFrame().setEditorView(Frame.EDIT_LAYOUT);
            } else if (source.equals(editAppearanceItem)) {
                project.setCurrentCircuit(circuit);
                project.getFrame().setEditorView(Frame.EDIT_APPEARANCE);
            } else if (source.equals(analyzeItem)) {
                ProjectCircuitActions.doAnalyze(project, circuit);
            } else if (source.equals(statsItem)) {
                JFrame frame = (JFrame) SwingUtilities.getRoot(this);
                StatisticsDialog.show(frame, project.getLogisimFile(), circuit);
            } else if (source.equals(mainItem)) {
                ProjectCircuitActions.doSetAsMainCircuit(project, circuit);
            } else if (source.equals(removeItem)) {
                ProjectCircuitActions.doRemoveCircuit(project, circuit);
            }
        }
    }

}

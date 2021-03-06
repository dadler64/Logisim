/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

class SimulationExplorer extends JPanel implements ProjectListener, MouseListener {

    private final Project project;
    private final JTree tree;
    private SimulationTreeModel model;

    SimulationExplorer(Project project, MenuListener menu) {
        super(new BorderLayout());
        this.project = project;

        SimulationToolbarModel toolbarModel = new SimulationToolbarModel(project, menu);
        Toolbar toolbar = new Toolbar(toolbarModel);
        add(toolbar, BorderLayout.NORTH);

        model = new SimulationTreeModel(project.getSimulator().getCircuitState());
        model.setCurrentView(this.project.getCircuitState());
        tree = new JTree(model);
        tree.setCellRenderer(new SimulationTreeRenderer());
        tree.addMouseListener(this);
        tree.setToggleClickCount(3);
        add(new JScrollPane(tree), BorderLayout.CENTER);
        project.addProjectListener(this);
    }

    //
    // ProjectListener methods
    //
    public void projectChanged(ProjectEvent event) {
        int action = event.getAction();
        if (action == ProjectEvent.ACTION_SET_STATE) {
            Simulator simulator = project.getSimulator();
            CircuitState root = simulator.getCircuitState();
            if (model.getRootState() != root) {
                model = new SimulationTreeModel(root);
                tree.setModel(model);
            }
            model.setCurrentView(project.getCircuitState());
            TreePath path = model.mapToPath(project.getCircuitState());
            if (path != null) {
                tree.scrollPathToVisible(path);
            }
        }
    }

    //
    // MouseListener methods
    //
    //
    // MouseListener methods
    //
    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        requestFocus();
        checkForPopup(e);
    }

    public void mouseReleased(MouseEvent e) {
        checkForPopup(e);
    }

    private void checkForPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            // do nothing
        }
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            TreePath path = tree.getPathForLocation(e.getX(), e.getY());
            if (path != null) {
                Object last = path.getLastPathComponent();
                if (last instanceof SimulationTreeCircuitNode) {
                    SimulationTreeCircuitNode node;
                    node = (SimulationTreeCircuitNode) last;
                    project.setCircuitState(node.getCircuitState());
                }
            }
        }
    }
}

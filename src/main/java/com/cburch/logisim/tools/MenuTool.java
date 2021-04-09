/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;


import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.Project;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class MenuTool extends Tool {

    public MenuTool() {
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MenuTool;
    }

    @Override
    public int hashCode() {
        return MenuTool.class.hashCode();
    }

    @Override
    public String getName() {
        return "Menu Tool";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("menuTool");
    }

    @Override
    public String getDescription() {
        return Strings.get("menuToolDesc");
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Location point = Location.create(x, y);

        JPopupMenu menu;
        Project project = canvas.getProject();
        Selection selection = project.getSelection();
        Collection<Component> components = selection.getComponentsContaining(point, g);
        if (!components.isEmpty()) {
            Component component = components.iterator().next();
            if (selection.getComponents().size() > 1) {
                menu = new MenuSelection(project);
            } else {
                menu = new MenuComponent(project, canvas.getCircuit(), component);
                MenuExtender extender = (MenuExtender) component.getFeature(MenuExtender.class);
                if (extender != null) {
                    extender.configureMenu(menu, project);
                }
            }
        } else {
            Collection<Component> allContaining = canvas.getCircuit().getAllContaining(point, g);
            if (!allContaining.isEmpty()) {
                Component component = allContaining.iterator().next();
                menu = new MenuComponent(project, canvas.getCircuit(), component);
                MenuExtender extender = (MenuExtender) component.getFeature(MenuExtender.class);
                if (extender != null) {
                    extender.configureMenu(menu, project);
                }
            } else {
                menu = null;
            }
        }

        if (menu != null) {
            canvas.showPopupMenu(menu, x, y);
        }
    }

    @Override
    public void paintIcon(ComponentDrawContext context, int x, int y) {
        Graphics g = context.getGraphics();
        g.fillRect(x + 2, y + 1, 9, 2);
        g.drawRect(x + 2, y + 3, 15, 12);
        g.setColor(Color.lightGray);
        g.drawLine(x + 4, y + 2, x + 8, y + 2);
        for (int yOffset = y + 6; yOffset < y + 15; yOffset += 3) {
            g.drawLine(x + 4, yOffset, x + 14, yOffset);
        }
    }

    private static class MenuComponent extends JPopupMenu implements ActionListener {

        Project project;
        Circuit circuit;
        Component component;
        JMenuItem deleteItem = new JMenuItem(Strings.get("compDeleteItem"));
        JMenuItem showAttrItem = new JMenuItem(Strings.get("compShowAttrItem"));

        MenuComponent(Project project, Circuit circuit, Component component) {
            this.project = project;
            this.circuit = circuit;
            this.component = component;
            boolean canChange = project.getLogisimFile().contains(circuit);

            add(deleteItem);
            deleteItem.addActionListener(this);
            deleteItem.setEnabled(canChange);
            add(showAttrItem);
            showAttrItem.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == deleteItem) {
                Circuit circuit = project.getCurrentCircuit();
                CircuitMutation mutation = new CircuitMutation(circuit);
                mutation.remove(component);
                project.doAction(
                    mutation.toAction(Strings.getter("removeComponentAction", component.getFactory().getDisplayGetter())));
            } else if (source == showAttrItem) {
                project.getFrame().viewComponentAttributes(circuit, component);
            }
        }
    }

    private static class MenuSelection extends JPopupMenu implements ActionListener {

        Project project;
        JMenuItem deleteItem = new JMenuItem(Strings.get("selDeleteItem"));
        JMenuItem cutItem = new JMenuItem(Strings.get("selCutItem"));
        JMenuItem copyItem = new JMenuItem(Strings.get("selCopyItem"));

        MenuSelection(Project project) {
            this.project = project;
            boolean canChange = project.getLogisimFile().contains(project.getCurrentCircuit());
            add(deleteItem);
            deleteItem.addActionListener(this);
            deleteItem.setEnabled(canChange);
            add(cutItem);
            cutItem.addActionListener(this);
            cutItem.setEnabled(canChange);
            add(copyItem);
            copyItem.addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            Selection sel = project.getSelection();
            if (source == deleteItem) {
                project.doAction(SelectionActions.clear(sel));
            } else if (source == cutItem) {
                project.doAction(SelectionActions.cut(sel));
            } else if (source == copyItem) {
                project.doAction(SelectionActions.copy(sel));
            }
        }

        public void show(JComponent parent, int x, int y) {
            super.show(this, x, y);
        }
    }
}

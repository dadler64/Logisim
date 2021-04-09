/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

public class Toolbar extends JPanel {

    public static final Object VERTICAL = new Object();
    public static final Object HORIZONTAL = new Object();
    private final JPanel subPanel;
    private final MyListener myListener;
    private ToolbarModel model;
    private Object orientation;
    private ToolbarButton currentlyPressed;

    public Toolbar(ToolbarModel model) {
        super(new BorderLayout());
        this.subPanel = new JPanel();
        this.model = model;
        this.orientation = HORIZONTAL;
        this.myListener = new MyListener();
        this.currentlyPressed = null;

        this.add(new JPanel(), BorderLayout.CENTER);
        setOrientation(HORIZONTAL);

        computeContents();
        if (model != null) {
            model.addToolbarModelListener(myListener);
        }
    }

    public ToolbarModel getToolbarModel() {
        return model;
    }

    public void setToolbarModel(ToolbarModel value) {
        ToolbarModel oldValue = model;
        if (value != oldValue) {
            if (oldValue != null) {
                oldValue.removeToolbarModelListener(myListener);
            }
            if (value != null) {
                value.addToolbarModelListener(myListener);
            }
            model = value;
            computeContents();
        }
    }

    private void computeContents() {
        subPanel.removeAll();
        ToolbarModel model = this.model;
        if (model != null) {
            for (ToolbarItem item : model.getItems()) {
                subPanel.add(new ToolbarButton(this, item));
            }
            subPanel.add(Box.createGlue());
        }
        revalidate();
    }

    ToolbarButton getPressed() {
        return currentlyPressed;
    }

    void setPressed(ToolbarButton value) {
        ToolbarButton oldValue = currentlyPressed;
        if (oldValue != value) {
            currentlyPressed = value;
            if (oldValue != null) {
                oldValue.repaint();
            }
            if (value != null) {
                value.repaint();
            }
        }
    }

    Object getOrientation() {
        return orientation;
    }

    public void setOrientation(Object value) {
        int axis;
        String position;
        if (value == HORIZONTAL) {
            axis = BoxLayout.X_AXIS;
            position = BorderLayout.LINE_START;
        } else if (value == VERTICAL) {
            axis = BoxLayout.Y_AXIS;
            position = BorderLayout.NORTH;
        } else {
            throw new IllegalArgumentException();
        }
        this.remove(subPanel);
        subPanel.setLayout(new BoxLayout(subPanel, axis));
        this.add(subPanel, position);
        this.orientation = value;
    }

    private class MyListener implements ToolbarModelListener {

        public void toolbarAppearanceChanged(ToolbarModelEvent event) {
            repaint();
        }

        public void toolbarContentsChanged(ToolbarModelEvent event) {
            computeContents();
        }
    }
}

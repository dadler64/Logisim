/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.gui;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.SelectTool;
import com.cburch.logisim.gui.generic.AttrTable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class AttrTableDrawManager implements PropertyChangeListener {

    private final Canvas canvas;
    private final AttrTable table;
    private final AttrTableSelectionModel selectionModel;
    private final AttrTableToolModel toolModel;

    public AttrTableDrawManager(Canvas canvas, AttrTable table, DrawingAttributeSet attributes) {
        this.canvas = canvas;
        this.table = table;
        this.selectionModel = new AttrTableSelectionModel(canvas);
        this.toolModel = new AttrTableToolModel(attributes, null);

        canvas.addPropertyChangeListener(Canvas.TOOL_PROPERTY, this);
        updateToolAttributes();
    }

    public void attributesSelected() {
        updateToolAttributes();
    }

    //
    // PropertyChangeListener method
    //
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (propertyName.equals(Canvas.TOOL_PROPERTY)) {
            updateToolAttributes();
        }
    }

    private void updateToolAttributes() {
        CanvasTool tool = canvas.getTool();
        if (tool instanceof SelectTool) {
            table.setAttrTableModel(selectionModel);
        } else if (tool instanceof AbstractTool) {
            toolModel.setTool((AbstractTool) tool);
            table.setAttrTableModel(toolModel);
        } else {
            table.setAttrTableModel(null);
        }
    }
}

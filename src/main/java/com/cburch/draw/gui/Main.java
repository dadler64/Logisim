/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.gui;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Drawing;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.undo.UndoLog;
import com.cburch.draw.undo.UndoLogDispatcher;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.util.HorizontalSplitPane;
import com.cburch.logisim.util.VerticalSplitPane;
import java.awt.BorderLayout;
import java.util.Collections;
import javax.swing.JFrame;

public class Main {

    public static void main(String[] args) {
        DrawingAttributeSet attributes = new DrawingAttributeSet();
        Drawing drawing = new Drawing();
        CanvasObject rectangle = attributes.applyTo(new Rectangle(25, 25, 50, 50));
        drawing.addObjects(0, Collections.singleton(rectangle));

        showFrame(drawing, "Drawing 1");
        showFrame(drawing, "Drawing 2");
    }

    private static void showFrame(Drawing drawing, String title) {
        JFrame frame = new JFrame(title);
        DrawingAttributeSet attributes = new DrawingAttributeSet();

        Canvas canvas = new Canvas();
        Toolbar toolbar = new Toolbar(canvas, attributes);
        canvas.setModel(drawing, new UndoLogDispatcher(new UndoLog()));
        canvas.setTool(toolbar.getDefaultTool());

        AttrTable table = new AttrTable(frame);
        AttrTableDrawManager manager = new AttrTableDrawManager(canvas, table, attributes);
        manager.attributesSelected();
        HorizontalSplitPane westPane = new HorizontalSplitPane(toolbar, table, 0.5);
        VerticalSplitPane splitPane = new VerticalSplitPane(westPane, canvas, 0.3);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(splitPane, BorderLayout.CENTER);
        frame.pack();
        frame.setVisible(true);
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Oval;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.util.Icons;
import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;

public class OvalTool extends RectangularTool {

    private DrawingAttributeSet attributeSet;

    public OvalTool(DrawingAttributeSet attributeSet) {
        this.attributeSet = attributeSet;
    }

    @Override
    public Icon getIcon() {
        return Icons.getIcon("drawoval.gif");
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(attributeSet.getValue(DrawAttr.PAINT_TYPE));
    }

    @Override
    public CanvasObject createShape(int x, int y, int width, int height) {
        return attributeSet.applyTo(new Oval(x, y, width, height));
    }

    @Override
    public void drawShape(Graphics graphics, int x, int y, int width, int height) {
        graphics.drawOval(x, y, width, height);
    }

    @Override
    public void fillShape(Graphics graphics, int x, int y, int width, int height) {
        graphics.fillOval(x, y, width, height);
    }
}

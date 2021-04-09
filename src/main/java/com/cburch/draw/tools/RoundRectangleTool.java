/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.RoundRectangle;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.util.Icons;
import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;

public class RoundRectangleTool extends RectangularTool {

    private final DrawingAttributeSet attributeSet;

    public RoundRectangleTool(DrawingAttributeSet attributeSet) {
        this.attributeSet = attributeSet;
    }

    @Override
    public Icon getIcon() {
        return Icons.getIcon("drawrrct.gif");
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getRoundRectAttributes(attributeSet.getValue(DrawAttr.PAINT_TYPE));
    }

    @Override
    public CanvasObject createShape(int x, int y, int width, int height) {
        return attributeSet.applyTo(new RoundRectangle(x, y, width, height));
    }

    @Override
    public void drawShape(Graphics graphics, int x, int y, int width, int height) {
        int radius = 2 * attributeSet.getValue(DrawAttr.CORNER_RADIUS);
        graphics.drawRoundRect(x, y, width, height, radius, radius);
    }

    @Override
    public void fillShape(Graphics graphics, int x, int y, int width, int height) {
        int radius = 2 * attributeSet.getValue(DrawAttr.CORNER_RADIUS);
        graphics.fillRoundRect(x, y, width, height, radius, radius);
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.List;
import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Rectangle extends Rectangular {

    public Rectangle(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Rectangle) {
            return super.matches(object);
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        return super.matchesHashCode();
    }

    @Override
    public String toString() {
        return "Rectangle:" + getBounds();
    }

    @Override
    public String getDisplayName() {
        return Strings.get("shapeRect");
    }

    @Override
    public Element toSvgElement(Document document) {
        return SvgCreator.createRectangle(document, this);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(getPaintType());
    }

    @Override
    protected boolean contains(int x, int y, int width, int height, Location q) {
        return isInRectangle(q.getX(), q.getY(), x, y, width, height);
    }

    @Override
    protected Location getRandomPoint(Bounds bounds, Random random) {
        if (getPaintType() == DrawAttr.PAINT_STROKE) {
            int width = getWidth();
            int height = getHeight();
            int u = random.nextInt(2 * width + 2 * height);
            int x = getX();
            int y = getY();
            if (u < width) {
                x += u;
            } else if (u < 2 * width) {
                x += (u - width);
                y += height;
            } else if (u < 2 * width + height) {
                y += (u - 2 * width);
            } else {
                x += width;
                y += (u - 2 * width - height);
            }
            int strokeWidth = getStrokeWidth();
            if (strokeWidth > 1) {
                x += random.nextInt(strokeWidth) - strokeWidth / 2;
                y += random.nextInt(strokeWidth) - strokeWidth / 2;
            }
            return Location.create(x, y);
        } else {
            return super.getRandomPoint(bounds, random);
        }
    }

    @Override
    public void draw(Graphics graphics, int x, int y, int width, int height) {
        if (setForFill(graphics)) {
            graphics.fillRect(x, y, width, height);
        }
        if (setForStroke(graphics)) {
            graphics.drawRect(x, y, width, height);
        }
    }
}

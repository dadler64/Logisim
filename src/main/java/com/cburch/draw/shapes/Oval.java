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

public class Oval extends Rectangular {

    public Oval(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Oval) {
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
    public Element toSvgElement(Document document) {
        return SvgCreator.createOval(document, this);
    }

    @Override
    public String getDisplayName() {
        return Strings.get("shapeOval");
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getFillAttributes(getPaintType());
    }

    @Override
    protected boolean contains(int x, int y, int width, int height, Location q) {
        int qx = q.getX();
        int qy = q.getY();
        double dx = qx - (x + 0.5 * width);
        double dy = qy - (y + 0.5 * height);
        double sum = (dx * dx) / (width * width) + (dy * dy) / (height * height);
        return sum <= 0.25;
    }

    @Override
    protected Location getRandomPoint(Bounds bounds, Random random) {
        if (getPaintType() == DrawAttr.PAINT_STROKE) {
            double rx = getWidth() / 2.0;
            double ry = getHeight() / 2.0;
            double u = 2 * Math.PI * random.nextDouble();
            int x = (int) Math.round(getX() + rx + rx * Math.cos(u));
            int y = (int) Math.round(getY() + ry + ry * Math.sin(u));
            int d = getStrokeWidth();
            if (d > 1) {
                x += random.nextInt(d) - d / 2;
                y += random.nextInt(d) - d / 2;
            }
            return Location.create(x, y);
        } else {
            return super.getRandomPoint(bounds, random);
        }
    }

    @Override
    public void draw(Graphics graphics, int x, int y, int width, int height) {
        if (setForFill(graphics)) {
            graphics.fillOval(x, y, width, height);
        }
        if (setForStroke(graphics)) {
            graphics.drawOval(x, y, width, height);
        }
    }
}

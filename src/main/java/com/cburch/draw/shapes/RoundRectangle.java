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

public class RoundRectangle extends Rectangular {

    private int radius;

    public RoundRectangle(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.radius = 10;
    }

    private static boolean inCircle(int qx, int qy, int cx, int cy, int rx, int ry) {
        double dx = qx - cx;
        double dy = qy - cy;
        double sum = (dx * dx) / (4 * rx * rx) + (dy * dy) / (4 * ry * ry);
        return sum <= 0.25;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof RoundRectangle) {
            RoundRectangle that = (RoundRectangle) object;
            return super.matches(object) && this.radius == that.radius;
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        return super.matchesHashCode() * 31 + radius;
    }

    @Override
    public String getDisplayName() {
        return Strings.get("shapeRoundRect");
    }

    @Override
    public Element toSvgElement(Document document) {
        return SvgCreator.createRoundRectangle(document, this);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.getRoundRectAttributes(getPaintType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attribute) {
        if (attribute == DrawAttr.CORNER_RADIUS) {
            return (V) Integer.valueOf(radius);
        } else {
            return super.getValue(attribute);
        }
    }

    @Override
    public void updateValue(Attribute<?> attribute, Object object) {
        if (attribute == DrawAttr.CORNER_RADIUS) {
            radius = (Integer) object;
        } else {
            super.updateValue(attribute, object);
        }
    }

    @Override
    protected boolean contains(int x, int y, int width, int height, Location q) {
        int qx = q.getX();
        int qy = q.getY();
        int rx = radius;
        int ry = radius;
        if (2 * rx > width) {
            rx = width / 2;
        }
        if (2 * ry > height) {
            ry = height / 2;
        }
        if (!isInRectangle(qx, qy, x, y, width, height)) {
            return false;
        } else if (qx < x + rx) {
            if (qy < y + ry) {
                return inCircle(qx, qy, x + rx, y + ry, rx, ry);
            } else if (qy < y + height - ry) {
                return true;
            } else {
                return inCircle(qx, qy, x + rx, y + height - ry, rx, ry);
            }
        } else if (qx < x + width - rx) {
            return true;
        } else {
            if (qy < y + ry) {
                return inCircle(qx, qy, x + width - rx, y + ry, rx, ry);
            } else if (qy < y + height - ry) {
                return true;
            } else {
                return inCircle(qx, qy, x + width - rx, y + height - ry, rx, ry);
            }
        }
    }

    @Override
    protected Location getRandomPoint(Bounds bounds, Random random) {
        if (getPaintType() == DrawAttr.PAINT_STROKE) {
            int width = getWidth();
            int height = getHeight();
            int radius = this.radius;
            int horizontalLength = Math.max(0, width - 2 * radius); // length of horizontal segment
            int verticalLength = Math.max(0, height - 2 * radius);
            double length = 2 * horizontalLength + 2 * verticalLength + 2 * Math.PI * radius;
            double u = length * random.nextDouble();
            int x = getX();
            int y = getY();
            if (u < horizontalLength) {
                x += radius + (int) u;
            } else if (u < 2 * horizontalLength) {
                x += radius + (int) (u - horizontalLength);
                y += height;
            } else if (u < 2 * horizontalLength + verticalLength) {
                y += radius + (int) (u - 2 * horizontalLength);
            } else if (u < 2 * horizontalLength + 2 * verticalLength) {
                x += width;
                y += (u - 2 * width - height);
            } else {
                int radiusX = this.radius;
                int radiusY = this.radius;
                if (2 * radiusX > width) {
                    radiusX = width / 2;
                }
                if (2 * radiusY > height) {
                    radiusY = height / 2;
                }
                u = 2 * Math.PI * random.nextDouble();
                int deltaX = (int) Math.round(radiusX * Math.cos(u));
                int deltaY = (int) Math.round(radiusY * Math.sin(u));
                if (deltaX < 0) {
                    x += radius + deltaX;
                } else {
                    x += radius + horizontalLength + deltaX;
                }
                if (deltaY < 0) {
                    y += radius + deltaY;
                } else {
                    y += radius + verticalLength + deltaY;
                }
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
        int diameter = 2 * radius;
        if (setForFill(graphics)) {
            graphics.fillRoundRect(x, y, width, height, diameter, diameter);
        }
        if (setForStroke(graphics)) {
            graphics.drawRoundRect(x, y, width, height, diameter, diameter);
        }
    }
}

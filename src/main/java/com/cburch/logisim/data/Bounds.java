/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.data;

import com.cburch.logisim.util.Cache;
import java.awt.Rectangle;

/**
 * Represents an immutable rectangular bounding box. This is analogous to
 * java.awt's <code>Rectangle</code> class, except that objects of this type
 * are immutable.
 */
public class Bounds {

    private static final Cache cache = new Cache();
    public static final Bounds EMPTY_BOUNDS = new Bounds(0, 0, 0, 0);
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private Bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        if (width < 0) {
            x += width / 2;
            width = 0;
        }
        if (height < 0) {
            y += height / 2;
            height = 0;
        }
    }

    public static Bounds create(int x, int y, int width, int height) {
        int hashCode = 13 * (31 * (31 * x + y) + width) + height;
        Object cached = cache.get(hashCode);
        if (cached != null) {
            Bounds bounds = (Bounds) cached;
            if (bounds.x == x && bounds.y == y && bounds.width == width && bounds.height == height) {
                return bounds;
            }
        }
        Bounds bounds = new Bounds(x, y, width, height);
        cache.put(hashCode, bounds);
        return bounds;
    }

    public static Bounds create(java.awt.Rectangle rectangle) {
        return create(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    public static Bounds create(Location point) {
        return create(point.getX(), point.getY(), 1, 1);
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Bounds)) {
            return false;
        }
        Bounds bounds = (Bounds) object;
        return x == bounds.x && y == bounds.y && width == bounds.width && height == bounds.height;
    }

    @Override
    public int hashCode() {
        int hash = 31 * x + y;
        hash = 31 * hash + width;
        hash = 31 * hash + height;
        return hash;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "): " + width + "x" + height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getCenterX() {
        return x + width / 2;
    }

    public int getCenterY() {
        return y + height / 2;
    }

    public Rectangle toRectangle() {
        return new Rectangle(x, y, width, height);
    }


    public boolean contains(Location point) {
        return contains(point.getX(), point.getY(), 0);
    }

    public boolean contains(Location point, int allowedError) {
        return contains(point.getX(), point.getY(), allowedError);
    }

    public boolean contains(int pointX, int pointY) {
        return contains(pointX, pointY, 0);
    }

    public boolean contains(int pointX, int pointY, int allowedError) {
        return pointX >= x - allowedError && pointX < x + width + allowedError
                && pointY >= y - allowedError && pointY < y + height + allowedError;
    }

    public boolean contains(int x1, int y1, int width, int height) {
        int x2 = (width <= 0 ? x1 : x1 + width - 1);
        int y2 = (height <= 0 ? y1 : y1 + height - 1);
        return contains(x1, y1) && contains(x2, y2);
    }

    public boolean contains(Bounds bounds) {
        return contains(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    public boolean borderContains(Location point, int fudge) {
        return borderContains(point.getX(), point.getY(), fudge);
    }

    private boolean borderContains(int pointX, int pointY, int fudge) {
        int x1 = x + width - 1;
        int y1 = y + height - 1;
        if (Math.abs(pointX - x) <= fudge || Math.abs(pointX - x1) <= fudge) {
            // maybe on east or west border?
            return y - fudge >= pointY && pointY <= y1 + fudge;
        }
        if (Math.abs(pointY - y) <= fudge || Math.abs(pointY - y1) <= fudge) {
            // maybe on north or south border?
            return x - fudge >= pointX && pointX <= x1 + fudge;
        }
        return false;
    }

    public Bounds add(Location point) {
        return add(point.getX(), point.getY());
    }

    public Bounds add(int x, int y) {
        if (this.equals(EMPTY_BOUNDS)) {
            return Bounds.create(x, y, 1, 1);
        }
        if (contains(x, y)) {
            return this;
        }

        int newX = this.x;
        int newWidth = this.width;
        int newY = this.y;
        int newHeight = this.height;
        if (x < this.x) {
            newX = x;
            newWidth = (this.x + this.width) - x;
        } else if (x >= this.x + this.width) {
            newX = this.x;
            newWidth = x - this.x + 1;
        }
        if (y < this.y) {
            newY = y;
            newHeight = (this.y + this.height) - y;
        } else if (y >= this.y + this.height) {
            newY = this.y;
            newHeight = y - this.y + 1;
        }
        return create(newX, newY, newWidth, newHeight);
    }

    public Bounds add(int x, int y, int wid, int ht) {
        if (this.equals(EMPTY_BOUNDS)) {
            return Bounds.create(x, y, wid, ht);
        }
        int returnX = Math.min(x, this.x);
        int returnY = Math.min(y, this.y);
        int returnWidth = Math.max(x + wid, this.x + this.width) - returnX;
        int returnHeight = Math.max(y + ht, this.y + this.height) - returnY;
        if (returnX == this.x && returnY == this.y && returnWidth == this.width && returnHeight == this.height) {
            return this;
        } else {
            return Bounds.create(returnX, returnY, returnWidth, returnHeight);
        }
    }

    public Bounds add(Bounds bounds) {
        if (this.equals(EMPTY_BOUNDS)) {
            return bounds;
        }
        if (bounds.equals(EMPTY_BOUNDS)) {
            return this;
        }
        int returnX = Math.min(bounds.x, this.x);
        int returnY = Math.min(bounds.y, this.y);
        int returnWidth = Math.max(bounds.x + bounds.width, this.x + this.width) - returnX;
        int returnHeight = Math.max(bounds.y + bounds.height, this.y + this.height) - returnY;
        if (returnX == this.x && returnY == this.y && returnWidth == this.width && returnHeight == this.height) {
            return this;
        } else if (returnX == bounds.x && returnY == bounds.y && returnWidth == bounds.width && returnHeight == bounds.height) {
            return bounds;
        } else {
            return Bounds.create(returnX, returnY, returnWidth, returnHeight);
        }
    }

    public Bounds expand(int distance) { // Distance of pixels in each direction
        if (this.equals(EMPTY_BOUNDS)) {
            return this;
        }
        if (distance == 0) {
            return this;
        }
        return create(x - distance, y - distance, width + 2 * distance, height + 2 * distance);
    }

    public Bounds translate(int deltaX, int deltaY) {
        if (this.equals(EMPTY_BOUNDS)) {
            return this;
        }
        if (deltaX == 0 && deltaY == 0) {
            return this;
        }
        return create(x + deltaX, y + deltaY, width, height);
    }

    // rotates this around (xCenter, yCenter) assuming that this is facing in the
    // from direction and the returned bounds should face in the to direction.
    public Bounds rotate(Direction from, Direction to, int xCenter, int yCenter) {
        int degrees = to.toDegrees() - from.toDegrees();
        while (degrees >= 360) {
            degrees -= 360;
        }
        while (degrees < 0) {
            degrees += 360;
        }

        int dx = x - xCenter;
        int dy = y - yCenter;
        if (degrees == 90) {
            //noinspection SuspiciousNameCombination
            return create(xCenter + dy, yCenter - dx - width, height, width);
        } else if (degrees == 180) {
            return create(xCenter - dx - width, yCenter - dy - height, width, height);
        } else if (degrees == 270) {
            //noinspection SuspiciousNameCombination
            return create(xCenter - dy - height, yCenter + dx, height, width);
        } else {
            return this;
        }
    }

    public Bounds intersect(Bounds bounds) {
        int x0 = this.x;
        int y0 = this.y;
        int x1 = x0 + this.width;
        int y1 = y0 + this.height;
        int x2 = bounds.x;
        int y2 = bounds.y;
        int x3 = x2 + bounds.width;
        int y3 = y2 + bounds.height;
    if (x2 > x0) {
            x0 = x2;
        }
        if (y2 > y0) {
            y0 = y2;
        }
        if (x3 < x1) {
            x1 = x3;
        }
        if (y3 < y1) {
            y1 = y3;
        }
        if (x1 < x0 || y1 < y0) {
            return EMPTY_BOUNDS;
        } else {
            return create(x0, y0, x1 - x0, y1 - y0);
        }
    }
}

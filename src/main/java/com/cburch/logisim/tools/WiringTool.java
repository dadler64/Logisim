/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import javax.swing.Icon;

public class WiringTool extends Tool {

    private static final Icon toolIcon = Icons.getIcon("wiring.gif");
    private static final int HORIZONTAL = 1;
    private static final int VERTICAL = 2;
    private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private boolean exists = false;
    private boolean inCanvas = false;
    private Location start = Location.create(0, 0);
    private Location current = Location.create(0, 0);
    private boolean hasDragged = false;
    private boolean startShortening = false;
    private Wire shortening = null;
    private Action lastAction = null;
    private int direction = 0;

    public WiringTool() {
        super.select(null);
    }

    @Override
    public void select(Canvas canvas) {
        super.select(canvas);
        lastAction = null;
        reset();
    }

    private void reset() {
        exists = false;
        inCanvas = false;
        start = Location.create(0, 0);
        current = Location.create(0, 0);
        startShortening = false;
        shortening = null;
        direction = 0;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof WiringTool;
    }

    @Override
    public int hashCode() {
        return WiringTool.class.hashCode();
    }

    @Override
    public String getName() {
        return "Wiring Tool";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("wiringTool");
    }

    @Override
    public String getDescription() {
        return Strings.get("wiringToolDesc");
    }

    private boolean computeMove(int newX, int newY) {
        if (current.getX() == newX && current.getY() == newY) {
            return false;
        }
        Location start = this.start;
        if (direction == 0) {
            if (newX != start.getX()) {
                direction = HORIZONTAL;
            } else if (newY != start.getY()) {
                direction = VERTICAL;
            }
        } else if (direction == HORIZONTAL && newX == start.getX()) {
            if (newY == start.getY()) {
                direction = 0;
            } else {
                direction = VERTICAL;
            }
        } else if (direction == VERTICAL && newY == start.getY()) {
            if (newX == start.getX()) {
                direction = 0;
            } else {
                direction = HORIZONTAL;
            }
        }
        return true;
    }

    @Override
    public Set<Component> getHiddenComponents(Canvas canvas) {
        Component shorten = willShorten(start, current);
        if (shorten != null) {
            return Collections.singleton(shorten);
        } else {
            return null;
        }
    }

    @Override
    public void draw(Canvas canvas, ComponentDrawContext context) {
        Graphics g = context.getGraphics();
        if (exists) {
            Location end0 = start;
            Location end1 = current;
            Wire shortenBefore = willShorten(start, current);
            if (shortenBefore != null) {
                Wire shorten = getShortenResult(shortenBefore, start, current);
                if (shorten == null) {
                    return;
                } else {
                    end0 = shorten.getEnd0();
                    end1 = shorten.getEnd1();
                }
            }
            int x0 = end0.getX();
            int y0 = end0.getY();
            int x1 = end1.getX();
            int y1 = end1.getY();

            g.setColor(Color.BLACK);
            GraphicsUtil.switchToWidth(g, 3);
            if (direction == HORIZONTAL) {
                if (x0 != x1) {
                    g.drawLine(x0, y0, x1, y0);
                }
                if (y0 != y1) {
                    g.drawLine(x1, y0, x1, y1);
                }
            } else if (direction == VERTICAL) {
                if (y0 != y1) {
                    g.drawLine(x0, y0, x0, y1);
                }
                if (x0 != x1) {
                    g.drawLine(x0, y1, x1, y1);
                }
            }
        } else if (AppPreferences.ADD_SHOW_GHOSTS.getBoolean() && inCanvas) {
            g.setColor(Color.GRAY);
            g.fillOval(current.getX() - 2, current.getY() - 2, 5, 5);
        }
    }

    @Override
    public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
        inCanvas = true;
        canvas.getProject().repaintCanvas();
    }

    @Override
    public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
        inCanvas = false;
        canvas.getProject().repaintCanvas();
    }

    @Override
    public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
        if (exists) {
            mouseDragged(canvas, g, e);
        } else {
            Canvas.snapToGrid(e);
            inCanvas = true;
            int curX = e.getX();
            int curY = e.getY();
            if (current.getX() != curX || current.getY() != curY) {
                current = Location.create(curX, curY);
            }
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        if (!canvas.getProject().getLogisimFile().contains(canvas.getCircuit())) {
            exists = false;
            canvas.setErrorMessage(Strings.getter("cannotModifyError"));
            return;
        }

        if (exists) {
            mouseDragged(canvas, g, e);
        } else {
            Canvas.snapToGrid(e);
            start = Location.create(e.getX(), e.getY());
            current = start;
            exists = true;
            hasDragged = false;

            startShortening = !canvas.getCircuit().getWires(start).isEmpty();
            shortening = null;

            super.mousePressed(canvas, g, e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        if (exists) {
            Canvas.snapToGrid(e);
            int currentX = e.getX();
            int currentY = e.getY();
            if (!computeMove(currentX, currentY)) {
                return;
            }
            hasDragged = true;

            Rectangle rectangle = new Rectangle();
            rectangle.add(start.getX(), start.getY());
            rectangle.add(current.getX(), current.getY());
            rectangle.add(currentX, currentY);
            rectangle.grow(3, 3);

            current = Location.create(currentX, currentY);
            super.mouseDragged(canvas, g, e);

            Wire shorten = null;
            if (startShortening) {
                for (Wire wire : canvas.getCircuit().getWires(start)) {
                    if (wire.contains(current)) {
                        shorten = wire;
                        break;
                    }
                }
            }
            if (shorten == null) {
                for (Wire wire : canvas.getCircuit().getWires(current)) {
                    if (wire.contains(start)) {
                        shorten = wire;
                        break;
                    }
                }
            }
            shortening = shorten;

            canvas.repaint(rectangle);
        }
    }

    void resetClick() {
        exists = false;
    }

    @Override
    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        if (!exists) {
            return;
        }

        Canvas.snapToGrid(e);
        int currentX = e.getX();
        int currentY = e.getY();
        if (computeMove(currentX, currentY)) {
            current = Location.create(currentX, currentY);
        }
        if (hasDragged) {
            exists = false;
            super.mouseReleased(canvas, g, e);

            ArrayList<Wire> wires = new ArrayList<>(2);
            if (current.getY() == start.getY() || current.getX() == start.getX()) {
                Wire wire = Wire.create(current, start);
                wire = checkForRepairs(canvas, wire, wire.getEnd0());
                wire = checkForRepairs(canvas, wire, wire.getEnd1());
                if (performShortening(canvas, start, current)) {
                    return;
                }
                if (wire.getLength() > 0) {
                    wires.add(wire);
                }
            } else {
                Location location;
                if (direction == HORIZONTAL) {
                    location = Location.create(current.getX(), start.getY());
                } else {
                    location = Location.create(start.getX(), current.getY());
                }
                Wire wire0 = Wire.create(start, location);
                Wire wire1 = Wire.create(location, current);
                wire0 = checkForRepairs(canvas, wire0, start);
                wire1 = checkForRepairs(canvas, wire1, current);
                if (wire0.getLength() > 0) {
                    wires.add(wire0);
                }
                if (wire1.getLength() > 0) {
                    wires.add(wire1);
                }
            }
            if (wires.size() > 0) {
                CircuitMutation mutation = new CircuitMutation(canvas.getCircuit());
                mutation.addAll(wires);
                StringGetter description;
                if (wires.size() == 1) {
                    description = Strings.getter("addWireAction");
                } else {
                    description = Strings.getter("addWiresAction");
                }
                Action action = mutation.toAction(description);
                canvas.getProject().doAction(action);
                lastAction = action;
            }
        }
    }

    private Wire checkForRepairs(Canvas canvas, Wire wire, Location end) {
        if (wire.getLength() <= 10) {
            return wire; // don't repair a short wire to nothing
        }
        if (!canvas.getCircuit().getNonWires(end).isEmpty()) {
            return wire;
        }

        int delta = (end.equals(wire.getEnd0()) ? 10 : -10);
        Location location;
        if (wire.isVertical()) {
            location = Location.create(end.getX(), end.getY() + delta);
        } else {
            location = Location.create(end.getX() + delta, end.getY());
        }

        for (Component component : canvas.getCircuit().getNonWires(location)) {
            if (component.getBounds().contains(end, 2)) {
                WireRepair repair = (WireRepair) component.getFeature(WireRepair.class);
                if (repair != null && repair.shouldRepairWire(new WireRepairData(wire, location))) {
                    wire = Wire.create(wire.getOtherEnd(end), location);
                    canvas.repaint(end.getX() - 13, end.getY() - 13, 26, 26);
                    return wire;
                }
            }
        }
        return wire;
    }

    private Wire willShorten(Location drag0, Location drag1) {
        Wire shorten = shortening;
        if (shorten == null) {
            return null;
        } else if (shorten.endsAt(drag0) || shorten.endsAt(drag1)) {
            return shorten;
        } else {
            return null;
        }
    }

    private Wire getShortenResult(Wire shorten, Location drag0, Location drag1) {
        if (shorten == null) {
            return null;
        } else {
            Location end0;
            Location end1;
            if (shorten.endsAt(drag0)) {
                end0 = drag1;
                end1 = shorten.getOtherEnd(drag0);
            } else if (shorten.endsAt(drag1)) {
                end0 = drag0;
                end1 = shorten.getOtherEnd(drag1);
            } else {
                return null;
            }
            return end0.equals(end1) ? null : Wire.create(end0, end1);
        }
    }

    private boolean performShortening(Canvas canvas, Location drag0, Location drag1) {
        Wire shorten = willShorten(drag0, drag1);
        if (shorten == null) {
            return false;
        } else {
            CircuitMutation xn = new CircuitMutation(canvas.getCircuit());
            StringGetter actionName;
            Wire result = getShortenResult(shorten, drag0, drag1);
            if (result == null) {
                xn.remove(shorten);
                actionName = Strings.getter("removeComponentAction", shorten.getFactory().getDisplayGetter());
            } else {
                xn.replace(shorten, result);
                actionName = Strings.getter("shortenWireAction");
            }
            canvas.getProject().doAction(xn.toAction(actionName));
            return true;
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if (lastAction != null && canvas.getProject().getLastAction() == lastAction) {
                canvas.getProject().undoAction();
                lastAction = null;
            }
        }
    }

    @Override
    public void paintIcon(ComponentDrawContext context, int x, int y) {
        Graphics g = context.getGraphics();
        if (toolIcon != null) {
            toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
        } else {
            g.setColor(java.awt.Color.black);
            g.drawLine(x + 3, y + 13, x + 17, y + 7);
            g.fillOval(x + 1, y + 11, 5, 5);
            g.fillOval(x + 15, y + 5, 5, 5);
        }
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }
}

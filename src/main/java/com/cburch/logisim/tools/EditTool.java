/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

public class EditTool extends Tool {

    private static final int CACHE_MAX_SIZE = 32;
    private static final Location NULL_LOCATION = Location.create(Integer.MIN_VALUE, Integer.MIN_VALUE);
    private final Listener listener;
    private final SelectTool select;
    private final WiringTool wiring;
    private Tool current;
    private final LinkedHashMap<Location, Boolean> cache;
    private Canvas lastCanvas;
    private int lastRawX;
    private int lastRawY;
    private int lastX; // last coordinates where wiring was computed
    private int lastY;
    private int lastModifiers; // last modifiers for mouse event
    private Location wireLocation; // coordinates where to draw wiring indicator, if
    private int pressX; // last coordinate where mouse was pressed
    private int pressY; // (used to determine when a short wire has been clicked)

    public EditTool(SelectTool select, WiringTool wiring) {
        this.listener = new Listener();
        this.select = select;
        this.wiring = wiring;
        this.current = select;
        this.cache = new LinkedHashMap<>();
        this.lastX = -1;
        this.wireLocation = NULL_LOCATION;
        this.pressX = -1;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof EditTool;
    }

    @Override
    public int hashCode() {
        return EditTool.class.hashCode();
    }

    @Override
    public String getName() {
        return "Edit Tool";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("editTool");
    }

    @Override
    public String getDescription() {
        return Strings.get("editToolDesc");
    }

    @Override
    public AttributeSet getAttributeSet() {
        return select.getAttributeSet();
    }

    @Override
    public void setAttributeSet(AttributeSet attrs) {
        select.setAttributeSet(attrs);
    }

    @Override
    public AttributeSet getAttributeSet(Canvas canvas) {
        return canvas.getSelection().getAttributeSet();
    }

    @Override
    public boolean isAllDefaultValues(AttributeSet attributeSet, LogisimVersion version) {
        return true;
    }

    @Override
    public void paintIcon(ComponentDrawContext c, int x, int y) {
        select.paintIcon(c, x, y);
    }

    @Override
    public Set<Component> getHiddenComponents(Canvas canvas) {
        return current.getHiddenComponents(canvas);
    }

    @Override
    public void draw(Canvas canvas, ComponentDrawContext context) {
        Location wireLocation = this.wireLocation;
        if (wireLocation != NULL_LOCATION && current != wiring) {
            int x = wireLocation.getX();
            int y = wireLocation.getY();
            Graphics g = context.getGraphics();
            g.setColor(Value.TRUE_COLOR);
            GraphicsUtil.switchToWidth(g, 2);
            g.drawOval(x - 5, y - 5, 10, 10);
            g.setColor(Color.BLACK);
            GraphicsUtil.switchToWidth(g, 1);
        }
        current.draw(canvas, context);
    }

    @Override
    public void select(Canvas canvas) {
        current = select;
        lastCanvas = canvas;
        cache.clear();
        canvas.getCircuit().addCircuitListener(listener);
        canvas.getSelection().addListener(listener);
        select.select(canvas);
    }

    @Override
    public void deselect(Canvas canvas) {
        current = select;
        canvas.getSelection().setSuppressHandles(null);
        cache.clear();
        canvas.getCircuit().removeCircuitListener(listener);
        canvas.getSelection().removeListener(listener);
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        boolean isWire = updateLocation(canvas, e);
        Location wireLocation = this.wireLocation;
        this.wireLocation = NULL_LOCATION;
        lastX = Integer.MIN_VALUE;
        if (isWire) {
            current = wiring;
            Selection selection = canvas.getSelection();
            Circuit circuit = canvas.getCircuit();
            Collection<Component> selected = selection.getAnchoredComponents();
            ArrayList<Component> suppress = null;
            for (Wire wire : circuit.getWires()) {
                if (selected.contains(wire)) {
                    if (wire.contains(wireLocation)) {
                        if (suppress == null) {
                            suppress = new ArrayList<>();
                        }
                        suppress.add(wire);
                    }
                }
            }
            selection.setSuppressHandles(suppress);
        } else {
            current = select;
        }
        pressX = e.getX();
        pressY = e.getY();
        current.mousePressed(canvas, g, e);
    }

    @Override
    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        isClick(e);
        current.mouseDragged(canvas, g, e);
    }

    @Override
    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        boolean isClick = isClick(e) && current == wiring;
        canvas.getSelection().setSuppressHandles(null);
        current.mouseReleased(canvas, g, e);
        if (isClick) {
            wiring.resetClick();
            select.mousePressed(canvas, g, e);
            select.mouseReleased(canvas, g, e);
        }
        current = select;
        cache.clear();
        updateLocation(canvas, e);
    }

    @Override
    public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
        pressX = -1;
        current.mouseEntered(canvas, g, e);
        canvas.requestFocusInWindow();
    }

    @Override
    public void mouseExited(Canvas canvas, Graphics g, MouseEvent e) {
        pressX = -1;
        current.mouseExited(canvas, g, e);
    }

    @Override
    public void mouseMoved(Canvas canvas, Graphics g, MouseEvent e) {
        updateLocation(canvas, e);
        select.mouseMoved(canvas, g, e);
    }

    private boolean isClick(MouseEvent e) {
        int px = pressX;
        if (px < 0) {
            return false;
        } else {
            int dx = e.getX() - px;
            int dy = e.getY() - pressY;
            if (dx * dx + dy * dy <= 4) {
                return true;
            } else {
                pressX = -1;
                return false;
            }
        }
    }

    private boolean updateLocation(Canvas canvas, MouseEvent e) {
        return updateLocation(canvas, e.getX(), e.getY(), e.getModifiersEx());
    }

    private boolean updateLocation(Canvas canvas, KeyEvent e) {
        int x = lastRawX;
        if (x >= 0) {
            return updateLocation(canvas, x, lastRawY, e.getModifiersEx());
        } else {
            return false;
        }
    }

    private boolean updateLocation(Canvas canvas, int mx, int my, int modifiers) {
        int snapX = Canvas.snapXToGrid(mx);
        int snapY = Canvas.snapYToGrid(my);
        int dx = mx - snapX;
        int dy = my - snapY;
        boolean isEligible = dx * dx + dy * dy < 36;
        if ((modifiers & MouseEvent.ALT_DOWN_MASK) != 0) {
            isEligible = true;
        }
        if (!isEligible) {
            snapX = -1;
            snapY = -1;
        }
        boolean isModifierEqual = lastModifiers == modifiers;
        lastCanvas = canvas;
        lastRawX = mx;
        lastRawY = my;
        lastModifiers = modifiers;
        if (lastX == snapX && lastY == snapY && isModifierEqual) { // already computed
            return wireLocation != NULL_LOCATION;
        } else {
            Location snap = Location.create(snapX, snapY);
            if (isModifierEqual) {
                Boolean isSnap = cache.get(snap);
                if (isSnap != null) {
                    lastX = snapX;
                    lastY = snapY;
                    canvas.repaint();
                    boolean ret = isSnap;
                    wireLocation = ret ? snap : NULL_LOCATION;
                    return ret;
                }
            } else {
                cache.clear();
            }

            boolean isValid = isEligible && isWiringPoint(canvas, snap, modifiers);
            wireLocation = isValid ? snap : NULL_LOCATION;
            cache.put(snap, isValid);
            int toRemove = cache.size() - CACHE_MAX_SIZE;
            Iterator<Location> iterator = cache.keySet().iterator();
            while (iterator.hasNext() && toRemove > 0) {
                iterator.next();
                iterator.remove();
                toRemove--;
            }

            lastX = snapX;
            lastY = snapY;
            canvas.repaint();
            return isValid;
        }
    }

    private boolean isWiringPoint(Canvas canvas, Location location, int modsEx) {
        boolean wiring = (modsEx & MouseEvent.ALT_DOWN_MASK) == 0;
        boolean select = !wiring;

        if (canvas != null && canvas.getSelection() != null) {
            Collection<Component> components = canvas.getSelection().getComponents();
            if (components != null) {
                for (Component component : components) {
                    if (component instanceof Wire) {
                        Wire wire = (Wire) component;
                        if (wire.contains(location) && !wire.endsAt(location)) {
                            return select;
                        }
                    }
                }
            }
        }

        Circuit circuit = canvas.getCircuit();
        Collection<? extends Component> components = circuit.getComponents(location);
        if (components != null && components.size() > 0) {
            return wiring;
        }

        for (Wire wire : circuit.getWires()) {
            if (wire.contains(location)) {
                return wiring;
            }
        }
        return select;
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent e) {
        select.keyTyped(canvas, e);
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_BACK_SPACE:
            case KeyEvent.VK_DELETE:
                if (!canvas.getSelection().isEmpty()) {
                    Action action = SelectionActions.clear(canvas.getSelection());
                    canvas.getProject().doAction(action);
                    e.consume();
                } else {
                    wiring.keyPressed(canvas, e);
                }
                break;
            case KeyEvent.VK_INSERT:
                Action action = SelectionActions.duplicate(canvas.getSelection());
                canvas.getProject().doAction(action);
                e.consume();
                break;
            case KeyEvent.VK_UP:
                if (e.getModifiersEx() == 0) {
                    attemptReface(canvas, Direction.NORTH, e);
                } else {
                    select.keyPressed(canvas, e);
                }
                break;
            case KeyEvent.VK_DOWN:
                if (e.getModifiersEx() == 0) {
                    attemptReface(canvas, Direction.SOUTH, e);
                } else {
                    select.keyPressed(canvas, e);
                }
                break;
            case KeyEvent.VK_LEFT:
                if (e.getModifiersEx() == 0) {
                    attemptReface(canvas, Direction.WEST, e);
                } else {
                    select.keyPressed(canvas, e);
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (e.getModifiersEx() == 0) {
                    attemptReface(canvas, Direction.EAST, e);
                } else {
                    select.keyPressed(canvas, e);
                }
                break;
            case KeyEvent.VK_ALT:
                updateLocation(canvas, e);
                e.consume();
                break;
            default:
                select.keyPressed(canvas, e);
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.VK_ALT) {
            updateLocation(canvas, event);
            event.consume();
        } else {
            select.keyReleased(canvas, event);
        }
    }

    private void attemptReface(Canvas canvas, final Direction facing, KeyEvent e) {
        if (e.getModifiersEx() == 0) {
            final Circuit circuit = canvas.getCircuit();
            final Selection selection = canvas.getSelection();
            SetAttributeAction action = new SetAttributeAction(circuit,
                Strings.getter("selectionRefaceAction"));
            for (Component component : selection.getComponents()) {
                if (!(component instanceof Wire)) {
                    Attribute<Direction> attr = getFacingAttribute(component);
                    if (attr != null) {
                        action.set(component, attr, facing);
                    }
                }
            }
            if (!action.isEmpty()) {
                canvas.getProject().doAction(action);
                e.consume();
            }
        }
    }

    private Attribute<Direction> getFacingAttribute(Component comp) {
        AttributeSet attrs = comp.getAttributeSet();
        Object key = ComponentFactory.FACING_ATTRIBUTE_KEY;
        Attribute<?> attr = (Attribute<?>) comp.getFactory().getFeature(key, attrs);
        @SuppressWarnings("unchecked")
        Attribute<Direction> ret = (Attribute<Direction>) attr;
        return ret;
    }

    @Override
    public Cursor getCursor() {
        return select.getCursor();
    }

    private class Listener implements CircuitListener, Selection.Listener {

        public void circuitChanged(CircuitEvent event) {
            if (event.getAction() != CircuitEvent.ACTION_INVALIDATE) {
                lastX = -1;
                cache.clear();
                updateLocation(lastCanvas, lastRawX, lastRawY, lastModifiers);
            }
        }

        public void selectionChanged(Event event) {
            lastX = -1;
            cache.clear();
            updateLocation(lastCanvas, lastRawX, lastRawY, lastModifiers);
        }
    }
}

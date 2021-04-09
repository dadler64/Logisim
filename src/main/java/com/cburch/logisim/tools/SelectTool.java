/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurationResult;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.tools.move.MoveGesture;
import com.cburch.logisim.tools.move.MoveRequestListener;
import com.cburch.logisim.tools.move.MoveResult;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.Icon;

public class SelectTool extends Tool {

    private static final Cursor selectCursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
    private static final Cursor rectSelectCursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private static final Cursor moveCursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);

    private static final int IDLE = 0;
    private static final int MOVING = 1;
    private static final int RECT_SELECT = 2;
    private static final Icon toolIcon = Icons.getIcon("select.gif");

    private static final Color COLOR_UNMATCHED = new Color(192, 0, 0);
    private static final Color COLOR_COMPUTING = new Color(96, 192, 96);
    private static final Color COLOR_RECT_SELECT = new Color(0, 64, 128, 255);
    private static final Color BACKGROUND_RECT_SELECT = new Color(192, 192, 255, 192);
    private final HashSet<Selection> selectionsAdded;
    private final Listener selectionListener;
    private Location start;
    private int state;
    private int currentDx;
    private int currentDy;
    private boolean drawConnections;
    private MoveGesture moveGesture;
    private HashMap<Component, KeyConfigurator> keyHandlers;

    public SelectTool() {
        start = null;
        state = IDLE;
        selectionsAdded = new HashSet<>();
        selectionListener = new Listener();
        keyHandlers = null;
    }

    private static void clearCanvasMessage(Canvas canvas, int dx, int dy) {
        Object getter = canvas.getErrorMessage();
        if (getter instanceof ComputingMessage) {
            ComputingMessage message = (ComputingMessage) getter;
            if (message.dx == dx && message.dy == dy) {
                canvas.setErrorMessage(null);
                canvas.repaint();
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SelectTool;
    }

    @Override
    public int hashCode() {
        return SelectTool.class.hashCode();
    }

    @Override
    public String getName() {
        return "Select Tool";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("selectTool");
    }

    @Override
    public String getDescription() {
        return Strings.get("selectToolDesc");
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
    public void draw(Canvas canvas, ComponentDrawContext context) {
        Project project = canvas.getProject();
        int dx = currentDx;
        int dy = currentDy;
        if (state == MOVING) {
            project.getSelection().drawGhostsShifted(context, dx, dy);

            MoveGesture gesture = moveGesture;
            if (gesture != null && drawConnections && (dx != 0 || dy != 0)) {
                MoveResult result = gesture.findResult(dx, dy);
                if (result != null) {
                    Collection<Wire> wiresToAdd = result.getWiresToAdd();
                    Graphics g = context.getGraphics();
                    GraphicsUtil.switchToWidth(g, 3);
                    g.setColor(Color.GRAY);
                    for (Wire wire : wiresToAdd) {
                        Location location0 = wire.getEnd0();
                        Location location1 = wire.getEnd1();
                        g.drawLine(location0.getX(), location0.getY(), location1.getX(), location1.getY());
                    }
                    GraphicsUtil.switchToWidth(g, 1);
                    g.setColor(COLOR_UNMATCHED);
                    for (Location connection : result.getUnconnectedLocations()) {
                        int connectionX = connection.getX();
                        int connectionY = connection.getY();
                        g.fillOval(connectionX - 3, connectionY - 3, 6, 6);
                        g.fillOval(connectionX + dx - 3, connectionY + dy - 3, 6, 6);
                    }
                }
            }
        } else if (state == RECT_SELECT) {
            int left = start.getX();
            int right = left + dx;
            if (left > right) {
                int i = left;
                left = right;
                right = i;
            }
            int top = start.getY();
            int bot = top + dy;
            if (top > bot) {
                int i = top;
                top = bot;
                bot = i;
            }

            Graphics gBase = context.getGraphics();
            int width = right - left - 1;
            int height = bot - top - 1;
            if (width > 2 && height > 2) {
                gBase.setColor(BACKGROUND_RECT_SELECT);
                gBase.fillRect(left + 1, top + 1, width - 1, height - 1);
            }

            Circuit circuit = canvas.getCircuit();
            Bounds bounds = Bounds.create(left, top, right - left, bot - top);
            for (Component component : circuit.getAllWithin(bounds)) {
                Location componentLocation = component.getLocation();
                Graphics gDup = gBase.create();
                context.setGraphics(gDup);
                component.getFactory().drawGhost(context, COLOR_RECT_SELECT, componentLocation.getX(),
                    componentLocation.getY(), component.getAttributeSet());
                gDup.dispose();
            }

            gBase.setColor(COLOR_RECT_SELECT);
            GraphicsUtil.switchToWidth(gBase, 2);
            if (width < 0) {
                width = 0;
            }
            if (height < 0) {
                height = 0;
            }
            gBase.drawRect(left, top, width, height);
        }
    }

    @Override
    public void select(Canvas canvas) {
        Selection sel = canvas.getSelection();
        if (!selectionsAdded.contains(sel)) {
            sel.addListener(selectionListener);
        }
    }

    @Override
    public void deselect(Canvas canvas) {
        moveGesture = null;
    }

    @Override
    public void mouseEntered(Canvas canvas, Graphics g, MouseEvent e) {
        canvas.requestFocusInWindow();
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        Project project = canvas.getProject();
        Selection selection = project.getSelection();
        Circuit circuit = canvas.getCircuit();
        start = Location.create(e.getX(), e.getY());
        currentDx = 0;
        currentDy = 0;
        moveGesture = null;

        // if the user clicks into the selection,
        // selection is being modified
        Collection<Component> selected = selection.getComponentsContaining(start, g);
        if (!selected.isEmpty()) {
            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
                setState(project, MOVING);
                project.repaintCanvas();
                return;
            } else {
                Action act = SelectionActions.drop(selection, selected);
                if (act != null) {
                    project.doAction(act);
                }
            }
        }

        // if the user clicks into a component outside selection, user
        // wants to add/reset selection
        Collection<Component> clicked = circuit.getAllContaining(start, g);
        if (!clicked.isEmpty()) {
            if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
                if (selection.getComponentsContaining(start).isEmpty()) {
                    Action action = SelectionActions.dropAll(selection);
                    if (action != null) {
                        project.doAction(action);
                    }
                }
            }
            for (Component component : clicked) {
                if (!selected.contains(component)) {
                    selection.add(component);
                }
            }
            setState(project, MOVING);
            project.repaintCanvas();
            return;
        }

        // The user clicked on the background. This is a rectangular selection (maybe with the shift key down).
        if ((e.getModifiersEx() & InputEvent.SHIFT_DOWN_MASK) == 0) {
            Action action = SelectionActions.dropAll(selection);
            if (action != null) {
                project.doAction(action);
            }
        }
        setState(project, RECT_SELECT);
        project.repaintCanvas();
    }

    @Override
    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        if (state == MOVING) {
            Project project = canvas.getProject();
            computeDxDy(project, e, g);
            handleMoveDrag(canvas, currentDx, currentDy, e.getModifiersEx());
        } else if (state == RECT_SELECT) {
            Project project = canvas.getProject();
            currentDx = e.getX() - start.getX();
            currentDy = e.getY() - start.getY();
            project.repaintCanvas();
        }
    }

    private void handleMoveDrag(Canvas canvas, int dx, int dy, int modsEx) {
        boolean shouldConnect = shouldConnect(canvas, modsEx);
        drawConnections = shouldConnect;
        if (shouldConnect) {
            MoveGesture gesture = moveGesture;
            if (gesture == null) {
                gesture = new MoveGesture(new MoveRequestHandler(canvas), canvas.getCircuit(),
                    canvas.getSelection().getAnchoredComponents());
                moveGesture = gesture;
            }
            if (dx != 0 || dy != 0) {
                boolean queued = gesture.enqueueRequest(dx, dy);
                if (queued) {
                    canvas.setErrorMessage(new ComputingMessage(dx, dy), COLOR_COMPUTING);
                    // maybe CPU scheduled led the request to be satisfied just before the "if(queued)" statement. In any
                    // case, it doesn't hurt to check to ensure the message belongs.
                    if (gesture.findResult(dx, dy) != null) {
                        clearCanvasMessage(canvas, dx, dy);
                    }
                }
            }
        }
        canvas.repaint();
    }

    private boolean shouldConnect(Canvas canvas, int modsEx) {
        boolean isShiftReleased = (modsEx & MouseEvent.SHIFT_DOWN_MASK) == 0;
        boolean isDefault = AppPreferences.MOVE_KEEP_CONNECT.getBoolean();
        if (isShiftReleased) {
            return isDefault;
        } else {
            return !isDefault;
        }
    }

    @Override
    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        Project project = canvas.getProject();
        if (state == MOVING) {
            setState(project, IDLE);
            computeDxDy(project, e, g);
            int dx = currentDx;
            int dy = currentDy;
            if (dx != 0 || dy != 0) {
                if (!project.getLogisimFile().contains(canvas.getCircuit())) {
                    canvas.setErrorMessage(Strings.getter("cannotModifyError"));
                } else if (project.getSelection().hasConflictWhenMoved(dx, dy)) {
                    canvas.setErrorMessage(Strings.getter("exclusiveError"));
                } else {
                    boolean shouldConnect = shouldConnect(canvas, e.getModifiersEx());
                    drawConnections = false;
                    ReplacementMap replacementMap;
                    if (shouldConnect) {
                        MoveGesture gesture = moveGesture;
                        if (gesture == null) {
                            gesture = new MoveGesture(new MoveRequestHandler(canvas), canvas.getCircuit(),
                                canvas.getSelection().getAnchoredComponents());
                        }
                        canvas.setErrorMessage(new ComputingMessage(dx, dy), COLOR_COMPUTING);
                        MoveResult result = gesture.forceRequest(dx, dy);
                        clearCanvasMessage(canvas, dx, dy);
                        replacementMap = result.getReplacementMap();
                    } else {
                        replacementMap = null;
                    }
                    Selection selection = project.getSelection();
                    project.doAction(SelectionActions.translate(selection, dx, dy, replacementMap));
                }
            }
            moveGesture = null;
            project.repaintCanvas();
        } else if (state == RECT_SELECT) {
            Bounds bounds = Bounds.create(start).add(start.getX() + currentDx, start.getY() + currentDy);
            Circuit circuit = canvas.getCircuit();
            Selection selection = project.getSelection();
            Collection<Component> componentsWithin = selection.getComponentsWithin(bounds, g);
            for (Component component : circuit.getAllWithin(bounds, g)) {
                if (!componentsWithin.contains(component)) {
                    selection.add(component);
                }
            }
            Action action = SelectionActions.drop(selection, componentsWithin);
            if (action != null) {
                project.doAction(action);
            }
            setState(project, IDLE);
            project.repaintCanvas();
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent e) {
        if (state == MOVING && e.getKeyCode() == KeyEvent.VK_SHIFT) {
            handleMoveDrag(canvas, currentDx, currentDy, e.getModifiersEx());
        } else {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_BACK_SPACE:
                case KeyEvent.VK_DELETE:
                    if (!canvas.getSelection().isEmpty()) {
                        Action action = SelectionActions.clear(canvas.getSelection());
                        canvas.getProject().doAction(action);
                        e.consume();
                    }
                    break;
                default:
                    processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_PRESSED);
            }
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent e) {
        if (state == MOVING && e.getKeyCode() == KeyEvent.VK_SHIFT) {
            handleMoveDrag(canvas, currentDx, currentDy, e.getModifiersEx());
        } else {
            processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_RELEASED);
        }
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent e) {
        processKeyEvent(canvas, e, KeyConfigurationEvent.KEY_TYPED);
    }

    private void processKeyEvent(Canvas canvas, KeyEvent e, int type) {
        HashMap<Component, KeyConfigurator> handlers = keyHandlers;
        if (handlers == null) {
            handlers = new HashMap<>();
            Selection selection = canvas.getSelection();
            for (Component component : selection.getComponents()) {
                ComponentFactory factory = component.getFactory();
                AttributeSet attrs = component.getAttributeSet();
                Object handler = factory.getFeature(KeyConfigurator.class, attrs);
                if (handler != null) {
                    KeyConfigurator base = (KeyConfigurator) handler;
                    handlers.put(component, base.clone());
                }
            }
            keyHandlers = handlers;
        }

        if (!handlers.isEmpty()) {
            boolean shouldConsume = false;
            ArrayList<KeyConfigurationResult> results = new ArrayList<>();
            for (Map.Entry<Component, KeyConfigurator> entry : handlers.entrySet()) {
                Component component = entry.getKey();
                KeyConfigurator handler = entry.getValue();
                KeyConfigurationEvent event = new KeyConfigurationEvent(type, component.getAttributeSet(), e, component);
                KeyConfigurationResult result = handler.keyEventReceived(event);
                shouldConsume |= event.isConsumed();
                if (result != null) {
                    results.add(result);
                }
            }
            if (shouldConsume) {
                e.consume();
            }
            if (!results.isEmpty()) {
                SetAttributeAction action = new SetAttributeAction(canvas.getCircuit(),
                    Strings.getter("changeComponentAttributesAction"));
                for (KeyConfigurationResult result : results) {
                    Component component = (Component) result.getEvent().getData();
                    Map<Attribute<?>, Object> newValues = result.getAttributeValues();
                    for (Map.Entry<Attribute<?>, Object> entry : newValues.entrySet()) {
                        action.set(component, entry.getKey(), entry.getValue());
                    }
                }
                if (!action.isEmpty()) {
                    canvas.getProject().doAction(action);
                }
            }
        }
    }

    private void computeDxDy(Project project, MouseEvent e, Graphics g) {
        Bounds bounds = project.getSelection().getBounds(g);
        int dx;
        int dy;
        if (bounds == Bounds.EMPTY_BOUNDS) {
            dx = e.getX() - start.getX();
            dy = e.getY() - start.getY();
        } else {
            dx = Math.max(e.getX() - start.getX(), -bounds.getX());
            dy = Math.max(e.getY() - start.getY(), -bounds.getY());
        }

        Selection selection = project.getSelection();
        if (selection.shouldSnap()) {
            dx = Canvas.snapXToGrid(dx);
            dy = Canvas.snapYToGrid(dy);
        }
        currentDx = dx;
        currentDy = dy;
    }

    @Override
    public void paintIcon(ComponentDrawContext context, int x, int y) {
        Graphics g = context.getGraphics();
        if (toolIcon != null) {
            toolIcon.paintIcon(context.getDestination(), g, x + 2, y + 2);
        } else {
            int[] xp = {x + 5, x + 5, x + 9, x + 12, x + 14, x + 11, x + 16};
            int[] yp = {y, y + 17, y + 12, y + 18, y + 18, y + 12, y + 12};
            g.setColor(java.awt.Color.black);
            g.fillPolygon(xp, yp, xp.length);
        }
    }

    @Override
    public Cursor getCursor() {
        return state == IDLE ? selectCursor :
            (state == RECT_SELECT ? rectSelectCursor : moveCursor);
    }

    @Override
    public Set<Component> getHiddenComponents(Canvas canvas) {
        if (state == MOVING) {
            int dx = currentDx;
            int dy = currentDy;
            if (dx == 0 && dy == 0) {
                return null;
            }

            Set<Component> components = canvas.getSelection().getComponents();
            MoveGesture gesture = moveGesture;
            if (gesture != null && drawConnections) {
                MoveResult result = gesture.findResult(dx, dy);
                if (result != null) {
                    HashSet<Component> ret = new HashSet<>(components);
                    ret.addAll(result.getReplacementMap().getRemovals());
                    return ret;
                }
            }
            return components;
        } else {
            return null;
        }
    }

    private void setState(Project project, int newState) {
        if (state == newState) {
            return; // do nothing if state not new
        }

        state = newState;
        project.getFrame().getCanvas().setCursor(getCursor());
    }

    private static class MoveRequestHandler implements MoveRequestListener {

        private final Canvas canvas;

        MoveRequestHandler(Canvas canvas) {
            this.canvas = canvas;
        }

        public void requestSatisfied(MoveGesture gesture, int dx, int dy) {
            clearCanvasMessage(canvas, dx, dy);
        }
    }

    private static class ComputingMessage implements StringGetter {

        private final int dx;
        private final int dy;

        public ComputingMessage(int dx, int dy) {
            this.dx = dx;
            this.dy = dy;
        }

        public String get() {
            return Strings.get("moveWorkingMsg");
        }
    }

    private class Listener implements Selection.Listener {

        public void selectionChanged(Event event) {
            keyHandlers = null;
        }
    }
}

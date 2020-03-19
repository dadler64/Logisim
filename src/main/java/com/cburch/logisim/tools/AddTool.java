/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.adlerd.logger.Logger;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.SelectionActions;
import com.cburch.logisim.gui.main.ToolAttributeAction;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Dependencies;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurationResult;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.JOptionPane;

public class AddTool extends Tool {

    private static int INVALID_COORDINATE = Integer.MIN_VALUE;

    private static int SHOW_NONE = 0;
    private static int SHOW_GHOST = 1;
    private static int SHOW_ADD = 2;
    private static int SHOW_ADD_NO = 3;

    private static Cursor cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    private Class<? extends Library> descriptionBase;
    private FactoryDescription description;
    private boolean sourceLoadAttempted;
    private ComponentFactory factory;
    private AttributeSet attributes;
    private Bounds bounds;
    private boolean shouldSnap;
    private int lastX = INVALID_COORDINATE;
    private int lastY = INVALID_COORDINATE;
    private int state = SHOW_GHOST;
    private Action lastAddition;
    private boolean keyHandlerTried;
    private KeyConfigurator keyHandler;

    public AddTool(Class<? extends Library> base, FactoryDescription description) {
        this.descriptionBase = base;
        this.description = description;
        this.sourceLoadAttempted = false;
        this.shouldSnap = true;
        this.attributes = new FactoryAttributes(base, description);
        attributes.addAttributeListener(new MyAttributeListener());
        this.keyHandlerTried = false;
    }

    public AddTool(ComponentFactory source) {
        this.description = null;
        this.sourceLoadAttempted = true;
        this.factory = source;
        this.bounds = null;
        this.attributes = new FactoryAttributes(source);
        attributes.addAttributeListener(new MyAttributeListener());
        Boolean value = (Boolean) source.getFeature(ComponentFactory.SHOULD_SNAP, attributes);
        this.shouldSnap = value == null || value;
    }

    private AddTool(AddTool base) {
        this.descriptionBase = base.descriptionBase;
        this.description = base.description;
        this.sourceLoadAttempted = base.sourceLoadAttempted;
        this.factory = base.factory;
        this.bounds = base.bounds;
        this.shouldSnap = base.shouldSnap;
        this.attributes = (AttributeSet) base.attributes.clone();
        attributes.addAttributeListener(new MyAttributeListener());
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof AddTool)) {
            return false;
        }
        AddTool addTool = (AddTool) object;
        if (this.description != null) {
            return this.descriptionBase == addTool.descriptionBase
                    && this.description.equals(addTool.description);
        } else {
            return this.factory.equals(addTool.factory);
        }
    }

    @Override
    public int hashCode() {
        FactoryDescription description = this.description;
        return description != null ? description.hashCode() : factory.hashCode();
    }

    @Override
    public boolean sharesSource(Tool tool) {
        if (!(tool instanceof AddTool)) {
            return false;
        }
        AddTool addTool = (AddTool) tool;
        try {
            if (this.sourceLoadAttempted && addTool.sourceLoadAttempted) {
                // TODO Figure out why this causes errors on startup
//                return this.factory.equals(addTool.factory);
                // NOTE: This is my weak fix for the problem that keeps showing up
                // when the NOT, AND, and OR Gates are still in default.templ
                if (this.factory != null) {
                    return this.factory.equals(addTool.factory);
                } else {
                    return false;
                }
            } else if (this.description == null) {
                return addTool.description == null;
            } else {
                return this.description.equals(addTool.description);
            }
        } catch (NullPointerException e) {
            Logger.errorln(e, true, -69);
            return false; // Should never reach this point
        }
    }

    public ComponentFactory getFactory(boolean forceLoad) {
        return forceLoad ? getFactory() : this.factory;
    }

    public ComponentFactory getFactory() {
        ComponentFactory factory = this.factory;
        if (factory != null || sourceLoadAttempted) {
            return factory;
        } else {
            factory = description.getFactory(descriptionBase);
            if (factory != null) {
                AttributeSet base = getBaseAttributes();
                Boolean value = (Boolean) factory.getFeature(ComponentFactory.SHOULD_SNAP, base);
                shouldSnap = value == null || value;
            }
            this.factory = factory;
            sourceLoadAttempted = true;
            return factory;
        }
    }

    @Override
    public String getName() {
        FactoryDescription desc = description;
        return desc == null ? factory.getName() : desc.getName();
    }

    @Override
    public String getDisplayName() {
        FactoryDescription desc = description;
        return desc == null ? factory.getDisplayName() : desc.getDisplayName();
    }

    @Override
    public String getDescription() {
        String result;
        FactoryDescription factoryDesc = this.description;
        if (factoryDesc != null) {
            result = factoryDesc.getToolTip();
        } else {
            ComponentFactory source = getFactory();
            if (source != null) {
                result = (String) source.getFeature(ComponentFactory.TOOL_TIP,
                        getAttributeSet());
            } else {
                result = null;
            }
        }
        if (result == null) {
            result = StringUtil.format(Strings.get("addToolText"), getDisplayName());
        }
        return result;
    }

    @Override
    public Tool cloneTool() {
        return new AddTool(this);
    }

    @Override
    public AttributeSet getAttributeSet() {
        return attributes;
    }

    @Override
    public boolean isAllDefaultValues(AttributeSet attributeSet, LogisimVersion version) {
        return this.attributes == attributeSet && attributeSet instanceof FactoryAttributes
                && !((FactoryAttributes) attributeSet).isFactoryInstantiated();
    }

    @Override
    public Object getDefaultAttributeValue(Attribute<?> attribute, LogisimVersion version) {
        return getFactory().getDefaultAttributeValue(attribute, version);
    }

    @Override
    public void draw(Canvas canvas, ComponentDrawContext context) {
        // next "if" suggested roughly by Kevin Walsh of Cornell to take care of
        // repaint problems on OpenJDK under Ubuntu
        int x = lastX;
        int y = lastY;
        if (x == INVALID_COORDINATE || y == INVALID_COORDINATE) {
            return;
        }
        ComponentFactory source = getFactory();
        if (source == null) {
            return;
        }
        if (state == SHOW_GHOST) {
            source.drawGhost(context, Color.GRAY, x, y, getBaseAttributes());
        } else if (state == SHOW_ADD) {
            source.drawGhost(context, Color.BLACK, x, y, getBaseAttributes());
        }
    }

    private AttributeSet getBaseAttributes() {
        AttributeSet attributes = this.attributes;
        if (attributes instanceof FactoryAttributes) {
            attributes = ((FactoryAttributes) attributes).getBase();
        }
        return attributes;
    }

    // TODO look into why this is here
//    public void cancelOp() { }

    @Override
    public void select(Canvas canvas) {
        setState(canvas, SHOW_GHOST);
        bounds = null;
    }

    @Override
    public void deselect(Canvas canvas) {
        setState(canvas, SHOW_GHOST);
        moveTo(canvas, canvas.getGraphics(), INVALID_COORDINATE, INVALID_COORDINATE);
        bounds = null;
        lastAddition = null;
    }

    private synchronized void moveTo(Canvas canvas, Graphics graphics, int x, int y) {
        if (state != SHOW_NONE) {
            expose(canvas, lastX, lastY);
        }
        lastX = x;
        lastY = y;
        if (state != SHOW_NONE) {
            expose(canvas, lastX, lastY);
        }
    }

    @Override
    public void mouseEntered(Canvas canvas, Graphics graphics, MouseEvent event) {
        if (state == SHOW_GHOST || state == SHOW_NONE) {
            setState(canvas, SHOW_GHOST);
            canvas.requestFocusInWindow();
        } else if (state == SHOW_ADD_NO) {
            setState(canvas, SHOW_ADD);
            canvas.requestFocusInWindow();
        }
    }

    @Override
    public void mouseExited(Canvas canvas, Graphics graphics, MouseEvent event) {
        if (state == SHOW_GHOST) {
            moveTo(canvas, canvas.getGraphics(), INVALID_COORDINATE, INVALID_COORDINATE);
            setState(canvas, SHOW_NONE);
        } else if (state == SHOW_ADD) {
            moveTo(canvas, canvas.getGraphics(), INVALID_COORDINATE, INVALID_COORDINATE);
            setState(canvas, SHOW_ADD_NO);
        }
    }

    @Override
    public void mouseMoved(Canvas canvas, Graphics graphics, MouseEvent event) {
        if (state != SHOW_NONE) {
            if (shouldSnap) {
                Canvas.snapToGrid(event);
            }
            moveTo(canvas, graphics, event.getX(), event.getY());
        }
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics graphics, MouseEvent event) {
        // verify the addition would be valid
        Circuit circuit = canvas.getCircuit();
        if (!canvas.getProject().getLogisimFile().contains(circuit)) {
            canvas.setErrorMessage(Strings.getter("cannotModifyError"));
            return;
        }
        if (factory instanceof SubcircuitFactory) {
            SubcircuitFactory circuitFactory = (SubcircuitFactory) factory;
            Dependencies dependencies = canvas.getProject().getDependencies();
            if (!dependencies.canAdd(circuit, circuitFactory.getSubcircuit())) {
                canvas.setErrorMessage(Strings.getter("circularError"));
                return;
            }
        }

        if (shouldSnap) {
            Canvas.snapToGrid(event);
        }
        moveTo(canvas, graphics, event.getX(), event.getY());
        setState(canvas, SHOW_ADD);
    }

    @Override
    public void mouseDragged(Canvas canvas, Graphics graphics, MouseEvent event) {
        if (state != SHOW_NONE) {
            if (shouldSnap) {
                Canvas.snapToGrid(event);
            }
            moveTo(canvas, graphics, event.getX(), event.getY());
        }
    }

    @Override
    public void mouseReleased(Canvas canvas, Graphics graphics, MouseEvent event) {
        Component added = null;
        if (state == SHOW_ADD) {
            Circuit circuit = canvas.getCircuit();
            if (!canvas.getProject().getLogisimFile().contains(circuit)) {
                return;
            }
            if (shouldSnap) {
                Canvas.snapToGrid(event);
            }
            moveTo(canvas, graphics, event.getX(), event.getY());

            Location location = Location.create(event.getX(), event.getY());
            AttributeSet attributeSetCopy = (AttributeSet) attributes.clone();
            ComponentFactory source = getFactory();
            if (source == null) {
                return;
            }
            Component component = source.createComponent(location, attributeSetCopy);

            if (circuit.hasConflict(component)) {
                canvas.setErrorMessage(Strings.getter("exclusiveError"));
                return;
            }

            Bounds bounds = component.getBounds(graphics);
            if (bounds.getX() < 0 || bounds.getY() < 0) {
                canvas.setErrorMessage(Strings.getter("negativeCoordError"));
                return;
            }

            try {
                CircuitMutation mutation = new CircuitMutation(circuit);
                mutation.add(component);
                Action action = mutation.toAction(Strings.getter("addComponentAction", factory.getDisplayGetter()));
                canvas.getProject().doAction(action);
                lastAddition = action;
                added = component;
            } catch (CircuitException ex) {
                JOptionPane.showMessageDialog(canvas.getProject().getFrame(), ex.getMessage());
            }
            setState(canvas, SHOW_GHOST);
        } else if (state == SHOW_ADD_NO) {
            setState(canvas, SHOW_NONE);
        }

        Project project = canvas.getProject();
        Tool nextTool = determineNext(project);
        if (nextTool != null) {
            project.setTool(nextTool);
            Action action = SelectionActions.dropAll(canvas.getSelection());
            if (action != null) {
                project.doAction(action);
            }
            if (added != null) {
                canvas.getSelection().add(added);
            }
        }
    }

    private Tool determineNext(Project project) {
        String afterAdd = AppPreferences.ADD_AFTER.get();
        if (afterAdd.equals(AppPreferences.ADD_AFTER_UNCHANGED)) {
            return null;
        } else { // switch to Edit Tool
            Library base = project.getLogisimFile().getLibrary("Base");
            if (base == null) {
                return null;
            } else {
                return base.getTool("Edit Tool");
            }
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent event) {
        processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_PRESSED);

        if (!event.isConsumed() && event.getModifiersEx() == 0) {
            switch (event.getKeyCode()) {
                case KeyEvent.VK_UP:
                    setFacing(canvas, Direction.NORTH);
                    break;
                case KeyEvent.VK_DOWN:
                    setFacing(canvas, Direction.SOUTH);
                    break;
                case KeyEvent.VK_LEFT:
                    setFacing(canvas, Direction.WEST);
                    break;
                case KeyEvent.VK_RIGHT:
                    setFacing(canvas, Direction.EAST);
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    if (lastAddition != null && canvas.getProject().getLastAction() == lastAddition) {
                        canvas.getProject().undoAction();
                        lastAddition = null;
                    }
            }
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent event) {
        processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_RELEASED);
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent event) {
        processKeyEvent(canvas, event, KeyConfigurationEvent.KEY_TYPED);
    }

    private void processKeyEvent(Canvas canvas, KeyEvent event, int type) {
        KeyConfigurator handler = keyHandler;
        if (!keyHandlerTried) {
            ComponentFactory source = getFactory();
            AttributeSet baseAttributeSet = getBaseAttributes();
            handler = (KeyConfigurator) source.getFeature(KeyConfigurator.class, baseAttributeSet);
            keyHandler = handler;
            keyHandlerTried = true;
        }

        if (handler != null) {
            AttributeSet baseAttributeSet = getBaseAttributes();
            KeyConfigurationEvent keyEvent = new KeyConfigurationEvent(type, baseAttributeSet, event, this);
            KeyConfigurationResult keyResult = handler.keyEventReceived(keyEvent);
            if (keyResult != null) {
                Action action = ToolAttributeAction.create(keyResult);
                canvas.getProject().doAction(action);
            }
        }
    }

    private void setFacing(Canvas canvas, Direction facing) {
        ComponentFactory source = getFactory();
        if (source == null) {
            return;
        }
        AttributeSet base = getBaseAttributes();
        Object feature = source.getFeature(ComponentFactory.FACING_ATTRIBUTE_KEY, base);

        Attribute<Direction> attribute = (Attribute<Direction>) feature;
        if (attribute != null) {
            Action action = ToolAttributeAction.create(this, attribute, facing);
            canvas.getProject().doAction(action);
        }
    }

    @Override
    public void paintIcon(ComponentDrawContext drawContext, int x, int y) {
        FactoryDescription description = this.description;
        if (description != null && !description.isFactoryLoaded()) {
            Icon icon = description.getIcon();
            if (icon != null) {
                icon.paintIcon(drawContext.getDestination(), drawContext.getGraphics(), x + 2, y + 2);
                return;
            }
        }

        ComponentFactory source = getFactory();
        if (source != null) {
            AttributeSet base = getBaseAttributes();
            source.paintIcon(drawContext, x, y, base);
        }
    }

    private void expose(java.awt.Component component, int x, int y) {
        Bounds bounds = getBounds();
        component.repaint(x + bounds.getX(), y + bounds.getY(),
                bounds.getWidth(), bounds.getHeight());
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    private void setState(Canvas canvas, int value) {
        if (value == SHOW_GHOST) {
            if (canvas.getProject().getLogisimFile().contains(canvas.getCircuit())
                    && AppPreferences.ADD_SHOW_GHOSTS.getBoolean()) {
                state = SHOW_GHOST;
            } else {
                state = SHOW_NONE;
            }
        } else {
            state = value;
        }
    }

    private Bounds getBounds() {
        Bounds bounds = this.bounds;
        if (bounds == null) {
            ComponentFactory source = getFactory();
            if (source == null) {
                bounds = Bounds.EMPTY_BOUNDS;
            } else {
                AttributeSet base = getBaseAttributes();
                bounds = source.getOffsetBounds(base).expand(5);
            }
            this.bounds = bounds;
        }
        return bounds;
    }

    private class MyAttributeListener implements AttributeListener {

        public void attributeListChanged(AttributeEvent event) {
            bounds = null;
        }

        public void attributeValueChanged(AttributeEvent event) {
            bounds = null;
        }
    }
}

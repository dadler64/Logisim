/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import javax.swing.Icon;

public class Clock extends InstanceFactory {

    public static final Attribute<Integer> ATTR_HIGH
        = new DurationAttribute("highDuration", Strings.getter("clockHighAttr"), 1, Integer.MAX_VALUE);
    public static final Attribute<Integer> ATTR_LOW
        = new DurationAttribute("lowDuration", Strings.getter("clockLowAttr"), 1, Integer.MAX_VALUE);
    public static final Clock FACTORY = new Clock();
    private static final Icon toolIcon = Icons.getIcon("clock.gif");

    public Clock() {
        super("Clock", Strings.getter("clockComponent"));
        setAttributes(
            new Attribute[]{
                StdAttr.FACING,
                ATTR_HIGH,
                ATTR_LOW,
                StdAttr.LABEL,
                Pin.ATTR_LABEL_LOC,
                StdAttr.LABEL_FONT
            }, new Object[]{
                Direction.EAST,
                1,
                1,
                "",
                Direction.WEST,
                StdAttr.DEFAULT_LABEL_FONT
            }
        );
        setFacingAttribute(StdAttr.FACING);
        setInstanceLogger(ClockLogger.class);
        setInstancePoker(ClockPoker.class);
    }

    //
    // package methods
    //
    public static boolean tick(CircuitState circuitState, int ticks, Component component) {
        AttributeSet attrs = component.getAttributeSet();
        int durationHigh = attrs.getValue(ATTR_HIGH);
        int durationLow = attrs.getValue(ATTR_LOW);
        ClockState state = (ClockState) circuitState.getData(component);
        if (state == null) {
            state = new ClockState();
            circuitState.setData(component, state);
        }
        boolean currentValue = ticks % (durationHigh + durationLow) < durationLow;
        if (state.clicks % 2 == 1) {
            currentValue = !currentValue;
        }
        Value desired = (currentValue ? Value.FALSE : Value.TRUE);
        if (!state.sending.equals(desired)) {
            state.sending = desired;
            Instance.getInstanceFor(component).fireInvalidated();
            return true;
        } else {
            return false;
        }
    }

    private static ClockState getState(InstanceState state) {
        ClockState clockState = (ClockState) state.getData();
        if (clockState == null) {
            clockState = new ClockState();
            state.setData(clockState);
        }
        return clockState;
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        return Probe.getOffsetBounds(
            attributes.getValue(StdAttr.FACING),
            BitWidth.ONE, RadixOption.RADIX_2);
    }

    //
    // graphics methods
    //
    @Override
    public void paintIcon(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        if (toolIcon != null) {
            toolIcon.paintIcon(painter.getDestination(), g, 2, 2);
        } else {
            g.drawRect(4, 4, 13, 13);
            g.setColor(Value.FALSE.getColor());
            g.drawPolyline(new int[]{6, 6, 10, 10, 14, 14}, new int[]{10, 6, 6, 14, 14, 10}, 6);
        }

        Direction direction = painter.getAttributeValue(StdAttr.FACING);
        int pinX = 15;
        int pinY = 8;
        if (direction == Direction.EAST) {
            // keep defaults
        } else if (direction == Direction.WEST) {
            pinX = 3;
        } else if (direction == Direction.NORTH) {
            pinX = 8;
            pinY = 3;
        } else if (direction == Direction.SOUTH) {
            pinX = 8;
            pinY = 15;
        }
        g.setColor(Value.TRUE.getColor());
        g.fillOval(pinX, pinY, 3, 3);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        java.awt.Graphics g = painter.getGraphics();
        Bounds bounds = painter.getInstance()
            .getBounds(); // intentionally with no graphics object - we don't want label included
        int x = bounds.getX();
        int y = bounds.getY();
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawRect(x, y, bounds.getWidth(), bounds.getHeight());

        painter.drawLabel();

        boolean drawUp;
        if (painter.getShowState()) {
            ClockState state = getState(painter);
            g.setColor(state.sending.getColor());
            drawUp = state.sending == Value.TRUE;
        } else {
            g.setColor(Color.BLACK);
            drawUp = true;
        }
        x += 10;
        y += 10;
        int[] xs = {x - 6, x - 6, x, x, x + 6, x + 6};
        int[] ys;
        if (drawUp) {
            ys = new int[]{y, y - 4, y - 4, y + 4, y + 4, y};
        } else {
            ys = new int[]{y, y + 4, y + 4, y - 4, y - 4, y};
        }
        g.drawPolyline(xs, ys, xs.length);

        painter.drawPorts();
    }

    //
    // methods for instances
    //
    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        instance.setPorts(new Port[]{new Port(0, 0, Port.OUTPUT, BitWidth.ONE)});
        configureLabel(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == Pin.ATTR_LABEL_LOC) {
            configureLabel(instance);
        } else if (attribute == StdAttr.FACING) {
            instance.recomputeBounds();
            configureLabel(instance);
        }
    }

    @Override
    public void propagate(InstanceState state) {
        Value val = state.getPort(0);
        ClockState q = getState(state);
        if (!val.equals(q.sending)) { // ignore if no change
            state.setPort(0, q.sending, 1);
        }
    }

    //
    // private methods
    //
    private void configureLabel(Instance instance) {
        Direction facing = instance.getAttributeValue(StdAttr.FACING);
        Direction labelLocation = instance.getAttributeValue(Pin.ATTR_LABEL_LOC);
        Probe.configureLabel(instance, labelLocation, facing);
    }

    private static class ClockState implements InstanceData, Cloneable {

        Value sending = Value.FALSE;
        int clicks = 0;

        @Override
        public ClockState clone() {
            try {
                return (ClockState) super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    public static class ClockLogger extends InstanceLogger {

        @Override
        public String getLogName(InstanceState state, Object option) {
            return state.getAttributeValue(StdAttr.LABEL);
        }

        @Override
        public Value getLogValue(InstanceState state, Object option) {
            ClockState s = getState(state);
            return s.sending;
        }
    }

    public static class ClockPoker extends InstancePoker {

        boolean isPressed = true;

        @Override
        public void mousePressed(InstanceState state, MouseEvent e) {
            isPressed = isInside(state, e);
        }

        @Override
        public void mouseReleased(InstanceState state, MouseEvent e) {
            if (isPressed && isInside(state, e)) {
                ClockState myState = (ClockState) state.getData();
                myState.sending = myState.sending.not();
                myState.clicks++;
                state.fireInvalidated();
            }
            isPressed = false;
        }

        private boolean isInside(InstanceState state, MouseEvent e) {
            Bounds bounds = state.getInstance().getBounds();
            return bounds.contains(e.getX(), e.getY());
        }
    }
}

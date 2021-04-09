/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.comp.TextField;
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
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Objects;

public class Probe extends InstanceFactory {

    public static final Probe FACTORY = new Probe();

    public Probe() {
        super("Probe", Strings.getter("probeComponent"));
        setIconName("probe.gif");
        setFacingAttribute(StdAttr.FACING);
        setInstanceLogger(ProbeLogger.class);
    }

    static void paintValue(InstancePainter painter, Value value) {
        Graphics g = painter.getGraphics();
        Bounds bounds = painter.getBounds(); // intentionally with no graphics object - we don't want label included

        RadixOption radix = painter.getAttributeValue(RadixOption.ATTRIBUTE);
        if (radix == null || radix == RadixOption.RADIX_2) {
            int x = bounds.getX();
            int y = bounds.getY();
            int wid = value.getWidth();
            if (wid == 0) {
                x += bounds.getWidth() / 2;
                y += bounds.getHeight() / 2;
                GraphicsUtil.switchToWidth(g, 2);
                g.drawLine(x - 4, y, x + 4, y);
                return;
            }
            int x0 = bounds.getX() + bounds.getWidth() - 5;
            int compWidth = wid * 10;
            if (compWidth < bounds.getWidth() - 3) {
                x0 = bounds.getX() + (bounds.getWidth() + compWidth) / 2 - 5;
            }
            int cx = x0;
            int cy = bounds.getY() + bounds.getHeight() - 12;
            int current = 0;
            for (int k = 0; k < wid; k++) {
                GraphicsUtil.drawCenteredText(g, value.get(k).toDisplayString(), cx, cy);
                ++current;
                if (current == 8) {
                    current = 0;
                    cx = x0;
                    cy -= 20;
                } else {
                    cx -= 10;
                }
            }
        } else {
            String text = radix.toString(value);
            GraphicsUtil.drawCenteredText(g, text,
                bounds.getX() + bounds.getWidth() / 2,
                bounds.getY() + bounds.getHeight() / 2);
        }
    }

    private static Value getValue(InstanceState state) {
        StateData data = (StateData) state.getData();
        return data == null ? Value.NIL : data.curValue;
    }

    //
    // static methods
    //
    static Bounds getOffsetBounds(Direction dir, BitWidth width,
        RadixOption radix) {
        Bounds bounds = null;
        int len = radix == null || radix == RadixOption.RADIX_2 ? width.getWidth() : radix.getMaxLength(width);
        if (dir == Direction.EAST) {
            switch (len) {
                case 0:
                case 1:
                    bounds = Bounds.create(-20, -10, 20, 20);
                    break;
                case 2:
                    bounds = Bounds.create(-20, -10, 20, 20);
                    break;
                case 3:
                    bounds = Bounds.create(-30, -10, 30, 20);
                    break;
                case 4:
                    bounds = Bounds.create(-40, -10, 40, 20);
                    break;
                case 5:
                    bounds = Bounds.create(-50, -10, 50, 20);
                    break;
                case 6:
                    bounds = Bounds.create(-60, -10, 60, 20);
                    break;
                case 7:
                    bounds = Bounds.create(-70, -10, 70, 20);
                    break;
                case 8:
                    bounds = Bounds.create(-80, -10, 80, 20);
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    bounds = Bounds.create(-80, -20, 80, 40);
                    break;
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                    bounds = Bounds.create(-80, -30, 80, 60);
                    break;
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                    bounds = Bounds.create(-80, -40, 80, 80);
                    break;
            }
        } else if (dir == Direction.WEST) {
            switch (len) {
                case 0:
                case 1:
                    bounds = Bounds.create(0, -10, 20, 20);
                    break;
                case 2:
                    bounds = Bounds.create(0, -10, 20, 20);
                    break;
                case 3:
                    bounds = Bounds.create(0, -10, 30, 20);
                    break;
                case 4:
                    bounds = Bounds.create(0, -10, 40, 20);
                    break;
                case 5:
                    bounds = Bounds.create(0, -10, 50, 20);
                    break;
                case 6:
                    bounds = Bounds.create(0, -10, 60, 20);
                    break;
                case 7:
                    bounds = Bounds.create(0, -10, 70, 20);
                    break;
                case 8:
                    bounds = Bounds.create(0, -10, 80, 20);
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    bounds = Bounds.create(0, -20, 80, 40);
                    break;
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                    bounds = Bounds.create(0, -30, 80, 60);
                    break;
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                    bounds = Bounds.create(0, -40, 80, 80);
                    break;
            }
        } else if (dir == Direction.SOUTH) {
            switch (len) {
                case 0:
                case 1:
                    bounds = Bounds.create(-10, -20, 20, 20);
                    break;
                case 2:
                    bounds = Bounds.create(-10, -20, 20, 20);
                    break;
                case 3:
                    bounds = Bounds.create(-15, -20, 30, 20);
                    break;
                case 4:
                    bounds = Bounds.create(-20, -20, 40, 20);
                    break;
                case 5:
                    bounds = Bounds.create(-25, -20, 50, 20);
                    break;
                case 6:
                    bounds = Bounds.create(-30, -20, 60, 20);
                    break;
                case 7:
                    bounds = Bounds.create(-35, -20, 70, 20);
                    break;
                case 8:
                    bounds = Bounds.create(-40, -20, 80, 20);
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    bounds = Bounds.create(-40, -40, 80, 40);
                    break;
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                    bounds = Bounds.create(-40, -60, 80, 60);
                    break;
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                    bounds = Bounds.create(-40, -80, 80, 80);
                    break;
            }
        } else if (dir == Direction.NORTH) {
            switch (len) {
                case 0:
                case 1:
                    bounds = Bounds.create(-10, 0, 20, 20);
                    break;
                case 2:
                    bounds = Bounds.create(-10, 0, 20, 20);
                    break;
                case 3:
                    bounds = Bounds.create(-15, 0, 30, 20);
                    break;
                case 4:
                    bounds = Bounds.create(-20, 0, 40, 20);
                    break;
                case 5:
                    bounds = Bounds.create(-25, 0, 50, 20);
                    break;
                case 6:
                    bounds = Bounds.create(-30, 0, 60, 20);
                    break;
                case 7:
                    bounds = Bounds.create(-35, 0, 70, 20);
                    break;
                case 8:
                    bounds = Bounds.create(-40, 0, 80, 20);
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 13:
                case 14:
                case 15:
                case 16:
                    bounds = Bounds.create(-40, 0, 80, 40);
                    break;
                case 17:
                case 18:
                case 19:
                case 20:
                case 21:
                case 22:
                case 23:
                case 24:
                    bounds = Bounds.create(-40, 0, 80, 60);
                    break;
                case 25:
                case 26:
                case 27:
                case 28:
                case 29:
                case 30:
                case 31:
                case 32:
                    bounds = Bounds.create(-40, 0, 80, 80);
                    break;
            }
        }
        if (bounds == null) {
            bounds = Bounds.create(0, -10, 20, 20); // should never happen
        }
        return bounds;
    }

    static void configureLabel(Instance instance, Direction labelLocation, Direction facing) {
        Bounds bounds = instance.getBounds();
        int x;
        int y;
        int hAlign;
        int vAlign;
        if (labelLocation == Direction.NORTH) {
            hAlign = TextField.H_CENTER;
            vAlign = TextField.V_BOTTOM;
            x = bounds.getX() + bounds.getWidth() / 2;
            y = bounds.getY() - 2;
            if (facing == labelLocation) {
                hAlign = TextField.H_LEFT;
                x += 2;
            }
        } else if (labelLocation == Direction.SOUTH) {
            hAlign = TextField.H_CENTER;
            vAlign = TextField.V_TOP;
            x = bounds.getX() + bounds.getWidth() / 2;
            y = bounds.getY() + bounds.getHeight() + 2;
            if (facing == labelLocation) {
                hAlign = TextField.H_LEFT;
                x += 2;
            }
        } else if (labelLocation == Direction.EAST) {
            hAlign = TextField.H_LEFT;
            vAlign = TextField.V_CENTER;
            x = bounds.getX() + bounds.getWidth() + 2;
            y = bounds.getY() + bounds.getHeight() / 2;
            if (facing == labelLocation) {
                vAlign = TextField.V_BOTTOM;
                y -= 2;
            }
        } else { // WEST
            hAlign = TextField.H_RIGHT;
            vAlign = TextField.V_CENTER;
            x = bounds.getX() - 2;
            y = bounds.getY() + bounds.getHeight() / 2;
            if (facing == labelLocation) {
                vAlign = TextField.V_BOTTOM;
                y -= 2;
            }
        }

        instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, hAlign, vAlign);
    }

    @Override
    public AttributeSet createAttributeSet() {
        return new ProbeAttributes();
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        ProbeAttributes attrs = (ProbeAttributes) attributes;
        return getOffsetBounds(attrs.facing, attrs.width, attrs.radix);
    }

    //
    // graphics methods
    //
    @Override
    public void paintGhost(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Bounds bounds = painter.getOffsetBounds();
        g.drawOval(bounds.getX() + 1, bounds.getY() + 1, bounds.getWidth() - 1, bounds.getHeight() - 1);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Value value = getValue(painter);

        Graphics g = painter.getGraphics();
        Bounds bounds = painter.getBounds(); // intentionally with no graphics object - we don't want label included
        int x = bounds.getX();
        int y = bounds.getY();
        g.setColor(Color.WHITE);
        g.fillRect(x + 5, y + 5, bounds.getWidth() - 10, bounds.getHeight() - 10);
        g.setColor(Color.GRAY);
        if (value.getWidth() <= 1) {
            g.drawOval(x + 1, y + 1, bounds.getWidth() - 2, bounds.getHeight() - 2);
        } else {
            g.drawRoundRect(x + 1, y + 1, bounds.getWidth() - 2, bounds.getHeight() - 2, 6, 6);
        }

        g.setColor(Color.BLACK);
        painter.drawLabel();

        if (!painter.getShowState()) {
            if (value.getWidth() > 0) {
                GraphicsUtil.drawCenteredText(g, "x" + value.getWidth(), bounds.getX() + bounds.getWidth() / 2,
                    bounds.getY() + bounds.getHeight() / 2);
            }
        } else {
            paintValue(painter, value);
        }

        painter.drawPorts();
    }

    //
    // methods for instances
    //
    @Override
    protected void configureNewInstance(Instance instance) {
        instance.setPorts(new Port[]{new Port(0, 0, Port.INPUT, BitWidth.UNKNOWN)});
        instance.addAttributeListener();
        configureLabel(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == Pin.ATTR_LABEL_LOC) {
            configureLabel(instance);
        } else if (attribute == StdAttr.FACING || attribute == RadixOption.ATTRIBUTE) {
            instance.recomputeBounds();
            configureLabel(instance);
        }
    }

    @Override
    public void propagate(InstanceState state) {
        StateData oldData = (StateData) state.getData();
        Value oldValue = oldData == null ? Value.NIL : oldData.curValue;
        Value newValue = state.getPort(0);
        boolean same = Objects.equals(oldValue, newValue);
        if (!same) {
            if (oldData == null) {
                oldData = new StateData();
                oldData.curValue = newValue;
                state.setData(oldData);
            } else {
                oldData.curValue = newValue;
            }
            int oldWidth = oldValue == null ? 1 : oldValue.getBitWidth().getWidth();
            int newWidth = newValue.getBitWidth().getWidth();
            if (oldWidth != newWidth) {
                ProbeAttributes attrs = (ProbeAttributes) state.getAttributeSet();
                attrs.width = newValue.getBitWidth();
                state.getInstance().recomputeBounds();
                configureLabel(state.getInstance());
            }
            state.fireInvalidated();
        }
    }

    void configureLabel(Instance instance) {
        ProbeAttributes attrs = (ProbeAttributes) instance.getAttributeSet();
        Probe.configureLabel(instance, attrs.labelLocation, attrs.facing);
    }

    private static class StateData implements InstanceData, Cloneable {

        Value curValue = Value.NIL;

        @Override
        public Object clone() {
            try {
                return super.clone();
            } catch (CloneNotSupportedException e) {
                return null;
            }
        }
    }

    public static class ProbeLogger extends InstanceLogger {

        public ProbeLogger() {
        }

        @Override
        public String getLogName(InstanceState state, Object option) {
            String name = state.getAttributeValue(StdAttr.LABEL);
            return name != null && !name.equals("") ? name : null;
        }

        @Override
        public Value getLogValue(InstanceState state, Object option) {
            return getValue(state);
        }
    }
}

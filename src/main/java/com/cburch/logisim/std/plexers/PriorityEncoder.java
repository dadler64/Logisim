/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.plexers;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class PriorityEncoder extends InstanceFactory {

    private static final int OUT = 0;
    private static final int EN_IN = 1;
    private static final int EN_OUT = 2;
    private static final int GS = 3;

    public PriorityEncoder() {
        super("Priority Encoder", Strings.getter("priorityEncoderComponent"));
        setAttributes(
            new Attribute[]{
                StdAttr.FACING,
                Plexers.ATTR_SELECT,
                Plexers.ATTR_DISABLED
            }, new Object[]{
                Direction.EAST,
                BitWidth.create(3),
                Plexers.DISABLED_FLOATING
            }
        );
        setKeyConfigurator(new BitWidthConfigurator(Plexers.ATTR_SELECT, 1, 5, 0));
        setIconName("priencod.gif");
        setFacingAttribute(StdAttr.FACING);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        Direction direction = attributes.getValue(StdAttr.FACING);
        BitWidth select = attributes.getValue(Plexers.ATTR_SELECT);
        int inputs = 1 << select.getWidth();
        int offs = -5 * inputs;
        int len = 10 * inputs + 10;
        if (direction == Direction.NORTH) {
            return Bounds.create(offs, 0, len, 40);
        } else if (direction == Direction.SOUTH) {
            return Bounds.create(offs, -40, len, 40);
        } else if (direction == Direction.WEST) {
            return Bounds.create(0, offs, 40, len);
        } else { // dir == Direction.EAST
            return Bounds.create(-40, offs, 40, len);
        }
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == StdAttr.FACING || attribute == Plexers.ATTR_SELECT) {
            instance.recomputeBounds();
            updatePorts(instance);
        } else if (attribute == Plexers.ATTR_SELECT) {
            updatePorts(instance);
        } else if (attribute == Plexers.ATTR_DISABLED) {
            instance.fireInvalidated();
        }
    }

    private void updatePorts(Instance instance) {
        Object direction = instance.getAttributeValue(StdAttr.FACING);
        BitWidth select = instance.getAttributeValue(Plexers.ATTR_SELECT);
        int n = 1 << select.getWidth();
        Port[] ports = new Port[n + 4];
        if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            int x = -5 * n + 10;
            int y = direction == Direction.NORTH ? 40 : -40;
            for (int i = 0; i < n; i++) {
                ports[i] = new Port(x + 10 * i, y, Port.INPUT, 1);
            }
            ports[n + OUT] = new Port(0, 0, Port.OUTPUT, select.getWidth());
            ports[n + EN_IN] = new Port(x + 10 * n, y / 2, Port.INPUT, 1);
            ports[n + EN_OUT] = new Port(x - 10, y / 2, Port.OUTPUT, 1);
            ports[n + GS] = new Port(10, 0, Port.OUTPUT, 1);
        } else {
            int x = direction == Direction.EAST ? -40 : 40;
            int y = -5 * n + 10;
            for (int i = 0; i < n; i++) {
                ports[i] = new Port(x, y + 10 * i, Port.INPUT, 1);
            }
            ports[n + OUT] = new Port(0, 0, Port.OUTPUT, select.getWidth());
            ports[n + EN_IN] = new Port(x / 2, y + 10 * n, Port.INPUT, 1);
            ports[n + EN_OUT] = new Port(x / 2, y - 10, Port.OUTPUT, 1);
            ports[n + GS] = new Port(0, 10, Port.OUTPUT, 1);
        }

        for (int i = 0; i < n; i++) {
            ports[i].setToolTip(Strings.getter("priorityEncoderInTip", "" + i));
        }
        ports[n + OUT].setToolTip(Strings.getter("priorityEncoderOutTip"));
        ports[n + EN_IN].setToolTip(Strings.getter("priorityEncoderEnableInTip"));
        ports[n + EN_OUT].setToolTip(Strings.getter("priorityEncoderEnableOutTip"));
        ports[n + GS].setToolTip(Strings.getter("priorityEncoderGroupSignalTip"));

        instance.setPorts(ports);
    }

    @Override
    public void propagate(InstanceState state) {
        BitWidth select = state.getAttributeValue(Plexers.ATTR_SELECT);
        int index = 1 << select.getWidth();
        boolean enabled = state.getPort(index + EN_IN) != Value.FALSE;

        int out = -1;
        Value outDefault;
        if (enabled) {
            outDefault = Value.createUnknown(select);
            for (int i = index - 1; i >= 0; i--) {
                if (state.getPort(i) == Value.TRUE) {
                    out = i;
                    break;
                }
            }
        } else {
            Object opt = state.getAttributeValue(Plexers.ATTR_DISABLED);
            Value base = opt == Plexers.DISABLED_ZERO ? Value.FALSE : Value.UNKNOWN;
            outDefault = Value.repeat(base, select.getWidth());
        }
        if (out < 0) {
            state.setPort(index + OUT, outDefault, Plexers.DELAY);
            state.setPort(index + EN_OUT, enabled ? Value.TRUE : Value.FALSE, Plexers.DELAY);
            state.setPort(index + GS, Value.FALSE, Plexers.DELAY);
        } else {
            state.setPort(index + OUT, Value.createKnown(select, out), Plexers.DELAY);
            state.setPort(index + EN_OUT, Value.FALSE, Plexers.DELAY);
            state.setPort(index + GS, Value.TRUE, Plexers.DELAY);
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        Direction facing = painter.getAttributeValue(StdAttr.FACING);

        painter.drawBounds();
        Bounds bds = painter.getBounds();
        g.setColor(Color.GRAY);
        int x0;
        int y0;
        int hAlign;
        if (facing == Direction.WEST) {
            x0 = bds.getX() + bds.getWidth() - 3;
            y0 = bds.getY() + 15;
            hAlign = GraphicsUtil.H_RIGHT;
        } else if (facing == Direction.NORTH) {
            x0 = bds.getX() + 10;
            y0 = bds.getY() + bds.getHeight() - 2;
            hAlign = GraphicsUtil.H_CENTER;
        } else if (facing == Direction.SOUTH) {
            x0 = bds.getX() + 10;
            y0 = bds.getY() + 12;
            hAlign = GraphicsUtil.H_CENTER;
        } else {
            x0 = bds.getX() + 3;
            y0 = bds.getY() + 15;
            hAlign = GraphicsUtil.H_LEFT;
        }
        GraphicsUtil.drawText(g, "0", x0, y0, hAlign, GraphicsUtil.V_BASELINE);
        g.setColor(Color.BLACK);
        GraphicsUtil.drawCenteredText(g, "Pri",
            bds.getX() + bds.getWidth() / 2,
            bds.getY() + bds.getHeight() / 2);
        painter.drawPorts();
    }
}


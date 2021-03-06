/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

/**
 * Based on PUCTools (v0.9 beta) by CRC - PUC - Minas (pucmg.crc at gmail.com)
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics2D;

public class Power extends InstanceFactory {

    public Power() {
        super("Power", Strings.getter("powerComponent"));
        setIconName("power.gif");
        setAttributes(
            new Attribute[]{
                StdAttr.FACING,
                StdAttr.WIDTH
            }, new Object[]{
                Direction.NORTH,
                BitWidth.ONE
            }
        );
        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setPorts(new Port[]{new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH)});
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == StdAttr.FACING) {
            instance.recomputeBounds();
        }
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        return Bounds.create(0, -8, 15, 16)
            .rotate(Direction.EAST, attributes.getValue(StdAttr.FACING), 0, 0);
    }

    @Override
    public void propagate(InstanceState state) {
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        state.setPort(0, Value.repeat(Value.TRUE, width.getWidth()), 1);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        drawInstance(painter, false);
        painter.drawPorts();
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        drawInstance(painter, true);
    }

    private void drawInstance(InstancePainter painter, boolean isGhost) {
        Graphics2D g = (Graphics2D) painter.getGraphics().create();
        Location location = painter.getLocation();
        g.translate(location.getX(), location.getY());

        Direction from = painter.getAttributeValue(StdAttr.FACING);
        int degrees = Direction.EAST.toDegrees() - from.toDegrees();
        double radians = Math.toRadians((degrees + 360) % 360);
        g.rotate(radians);

        GraphicsUtil.switchToWidth(g, Wire.WIDTH);
        if (!isGhost && painter.getShowState()) {
            g.setColor(painter.getPort(0).getColor());
        }
        g.drawLine(0, 0, 5, 0);

        GraphicsUtil.switchToWidth(g, 1);
        if (!isGhost && painter.shouldDrawColor()) {
            BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
            g.setColor(Value.repeat(Value.TRUE, width.getWidth()).getColor());
        }
        g.drawPolygon(new int[]{6, 14, 6}, new int[]{-8, 0, 8}, 3);

        g.dispose();
    }
}

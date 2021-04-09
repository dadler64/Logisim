/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class Led extends InstanceFactory {

    public Led() {
        super("LED", Strings.getter("ledComponent"));
        setAttributes(
            new Attribute[]{
                StdAttr.FACING,
                Io.ATTR_ON_COLOR,
                Io.ATTR_OFF_COLOR,
                Io.ATTR_ACTIVE,
                StdAttr.LABEL,
                Io.ATTR_LABEL_LOCATION,
                StdAttr.LABEL_FONT,
                Io.ATTR_LABEL_COLOR
            }, new Object[]{
                Direction.WEST,
                new Color(240, 0, 0),
                Color.DARK_GRAY,
                Boolean.TRUE,
                "",
                Io.LABEL_CENTER,
                StdAttr.DEFAULT_LABEL_FONT,
                Color.BLACK
            }
        );
        setFacingAttribute(StdAttr.FACING);
        setIconName("led.gif");
        setPorts(new Port[]{new Port(0, 0, Port.INPUT, 1)});
        setInstanceLogger(Logger.class);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        Direction facing = attributes.getValue(StdAttr.FACING);
        return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST, facing, 0, 0);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        computeTextField(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == StdAttr.FACING) {
            instance.recomputeBounds();
            computeTextField(instance);
        } else if (attribute == Io.ATTR_LABEL_LOCATION) {
            computeTextField(instance);
        }
    }

    private void computeTextField(Instance instance) {
        Direction facing = instance.getAttributeValue(StdAttr.FACING);
        Object labelLocation = instance.getAttributeValue(Io.ATTR_LABEL_LOCATION);

        Bounds bounds = instance.getBounds();
        int x = bounds.getX() + bounds.getWidth() / 2;
        int y = bounds.getY() + bounds.getHeight() / 2;
        int hAlign = GraphicsUtil.H_CENTER;
        int vAlign = GraphicsUtil.V_CENTER;
        if (labelLocation == Direction.NORTH) {
            y = bounds.getY() - 2;
            vAlign = GraphicsUtil.V_BOTTOM;
        } else if (labelLocation == Direction.SOUTH) {
            y = bounds.getY() + bounds.getHeight() + 2;
            vAlign = GraphicsUtil.V_TOP;
        } else if (labelLocation == Direction.EAST) {
            x = bounds.getX() + bounds.getWidth() + 2;
            hAlign = GraphicsUtil.H_LEFT;
        } else if (labelLocation == Direction.WEST) {
            x = bounds.getX() - 2;
            hAlign = GraphicsUtil.H_RIGHT;
        }
        if (labelLocation == facing) {
            if (labelLocation == Direction.NORTH || labelLocation == Direction.SOUTH) {
                x += 2;
                hAlign = GraphicsUtil.H_LEFT;
            } else {
                y -= 2;
                vAlign = GraphicsUtil.V_BOTTOM;
            }
        }

        instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, hAlign, vAlign);
    }

    @Override
    public void propagate(InstanceState state) {
        Value value = state.getPort(0);
        InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
        if (data == null) {
            state.setData(new InstanceDataSingleton(value));
        } else {
            data.setValue(value);
        }
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        Graphics graphics = painter.getGraphics();
        Bounds bounds = painter.getBounds();
        GraphicsUtil.switchToWidth(graphics, 2);
        graphics.drawOval(bounds.getX() + 1, bounds.getY() + 1, bounds.getWidth() - 2, bounds.getHeight() - 2);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
        Value value = data == null ? Value.FALSE : (Value) data.getValue();
        Bounds bounds = painter.getBounds().expand(-1);

        Graphics graphics = painter.getGraphics();
        if (painter.getShowState()) {
            Color onColor = painter.getAttributeValue(Io.ATTR_ON_COLOR);
            Color offColor = painter.getAttributeValue(Io.ATTR_OFF_COLOR);
            Boolean active = painter.getAttributeValue(Io.ATTR_ACTIVE);
            Object desired = active ? Value.TRUE : Value.FALSE;
            graphics.setColor(value.equals(desired) ? onColor : offColor);
            graphics.fillOval(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }
        graphics.setColor(Color.BLACK);
        GraphicsUtil.switchToWidth(graphics, 2);
        graphics.drawOval(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        GraphicsUtil.switchToWidth(graphics, 1);
        graphics.setColor(painter.getAttributeValue(Io.ATTR_LABEL_COLOR));
        painter.drawLabel();
        painter.drawPorts();
    }

    public static class Logger extends InstanceLogger {

        @Override
        public String getLogName(InstanceState state, Object option) {
            return state.getAttributeValue(StdAttr.LABEL);
        }

        @Override
        public Value getLogValue(InstanceState state, Object option) {
            InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
            if (data == null) {
                return Value.FALSE;
            }
            return data.getValue() == Value.TRUE ? Value.TRUE : Value.FALSE;
        }
    }
}

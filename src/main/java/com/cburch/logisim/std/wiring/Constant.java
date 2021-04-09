/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Expressions;
import com.cburch.logisim.circuit.ExpressionComputer;
import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
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
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Constant extends InstanceFactory {

    public static final Attribute<Integer> ATTRIBUTE_VALUE
        = Attributes.forHexInteger("value", Strings.getter("constantValueAttr"));
    public static final InstanceFactory FACTORY = new Constant();
    private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);
    private static final List<Attribute<?>> ATTRIBUTES = Arrays.asList(StdAttr.FACING, StdAttr.WIDTH, ATTRIBUTE_VALUE);

    public Constant() {
        super("Constant", Strings.getter("constantComponent"));
        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(JoinedConfigurator.create(new ConstantConfigurator(), new BitWidthConfigurator(StdAttr.WIDTH)));
    }

    @Override
    public AttributeSet createAttributeSet() {
        return new ConstantAttributes();
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        updatePorts(instance);
    }

    private void updatePorts(Instance instance) {
        Port[] ports = {new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH)};
        instance.setPorts(ports);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == StdAttr.WIDTH) {
            instance.recomputeBounds();
            updatePorts(instance);
        } else if (attribute == StdAttr.FACING) {
            instance.recomputeBounds();
        } else if (attribute == ATTRIBUTE_VALUE) {
            instance.fireInvalidated();
        }
    }

    @Override
    protected Object getInstanceFeature(Instance instance, Object key) {
        if (key == ExpressionComputer.class) {
            return new ConstantExpression(instance);
        }
        return super.getInstanceFeature(instance, key);
    }

    @Override
    public void propagate(InstanceState state) {
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        int value = state.getAttributeValue(ATTRIBUTE_VALUE);
        state.setPort(0, Value.createKnown(width, value), 1);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        Direction facing = attributes.getValue(StdAttr.FACING);
        BitWidth width = attributes.getValue(StdAttr.WIDTH);
        int chars = (width.getWidth() + 3) / 4;

        Bounds bounds = null;
        if (facing == Direction.EAST) {
            switch (chars) {
                case 1:
                    bounds = Bounds.create(-16, -8, 16, 16);
                    break;
                case 2:
                    bounds = Bounds.create(-16, -8, 16, 16);
                    break;
                case 3:
                    bounds = Bounds.create(-26, -8, 26, 16);
                    break;
                case 4:
                    bounds = Bounds.create(-36, -8, 36, 16);
                    break;
                case 5:
                    bounds = Bounds.create(-46, -8, 46, 16);
                    break;
                case 6:
                    bounds = Bounds.create(-56, -8, 56, 16);
                    break;
                case 7:
                    bounds = Bounds.create(-66, -8, 66, 16);
                    break;
                case 8:
                    bounds = Bounds.create(-76, -8, 76, 16);
                    break;
            }
        } else if (facing == Direction.WEST) {
            switch (chars) {
                case 1:
                    bounds = Bounds.create(0, -8, 16, 16);
                    break;
                case 2:
                    bounds = Bounds.create(0, -8, 16, 16);
                    break;
                case 3:
                    bounds = Bounds.create(0, -8, 26, 16);
                    break;
                case 4:
                    bounds = Bounds.create(0, -8, 36, 16);
                    break;
                case 5:
                    bounds = Bounds.create(0, -8, 46, 16);
                    break;
                case 6:
                    bounds = Bounds.create(0, -8, 56, 16);
                    break;
                case 7:
                    bounds = Bounds.create(0, -8, 66, 16);
                    break;
                case 8:
                    bounds = Bounds.create(0, -8, 76, 16);
                    break;
            }
        } else if (facing == Direction.SOUTH) {
            switch (chars) {
                case 1:
                    bounds = Bounds.create(-8, -16, 16, 16);
                    break;
                case 2:
                    bounds = Bounds.create(-8, -16, 16, 16);
                    break;
                case 3:
                    bounds = Bounds.create(-13, -16, 26, 16);
                    break;
                case 4:
                    bounds = Bounds.create(-18, -16, 36, 16);
                    break;
                case 5:
                    bounds = Bounds.create(-23, -16, 46, 16);
                    break;
                case 6:
                    bounds = Bounds.create(-28, -16, 56, 16);
                    break;
                case 7:
                    bounds = Bounds.create(-33, -16, 66, 16);
                    break;
                case 8:
                    bounds = Bounds.create(-38, -16, 76, 16);
                    break;
            }
        } else if (facing == Direction.NORTH) {
            switch (chars) {
                case 1:
                    bounds = Bounds.create(-8, 0, 16, 16);
                    break;
                case 2:
                    bounds = Bounds.create(-8, 0, 16, 16);
                    break;
                case 3:
                    bounds = Bounds.create(-13, 0, 26, 16);
                    break;
                case 4:
                    bounds = Bounds.create(-18, 0, 36, 16);
                    break;
                case 5:
                    bounds = Bounds.create(-23, 0, 46, 16);
                    break;
                case 6:
                    bounds = Bounds.create(-28, 0, 56, 16);
                    break;
                case 7:
                    bounds = Bounds.create(-33, 0, 66, 16);
                    break;
                case 8:
                    bounds = Bounds.create(-38, 0, 76, 16);
                    break;
            }
        }
        if (bounds == null) {
            throw new IllegalArgumentException("unrecognized arguments " + facing + " " + width);
        }
        return bounds;
    }

    //
    // painting methods
    //
    @Override
    public void paintIcon(InstancePainter painter) {
        int width = painter.getAttributeValue(StdAttr.WIDTH).getWidth();
        int pinX = 16;
        int pinY = 9;
        Direction direction = painter.getAttributeValue(StdAttr.FACING);

        //if (direction.equals(Direction.EAST)) { }  // keep defaults
        if (direction == Direction.WEST) {
            pinX = 4;
        } else if (direction == Direction.NORTH) {
            pinX = 9;
            pinY = 4;
        } else if (direction == Direction.SOUTH) {
            pinX = 9;
            pinY = 16;
        }

        Graphics graphics = painter.getGraphics();
        if (width == 1) {
            int attributeValue = painter.getAttributeValue(ATTRIBUTE_VALUE);
            Value value = attributeValue == 1 ? Value.TRUE : Value.FALSE;
            graphics.setColor(value.getColor());
            GraphicsUtil.drawCenteredText(graphics, "" + attributeValue, 10, 9);
        } else {
            graphics.setFont(graphics.getFont().deriveFont(9.0f));
            GraphicsUtil.drawCenteredText(graphics, "x" + width, 10, 9);
        }
        graphics.fillOval(pinX, pinY, 3, 3);
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        int attributeValue = painter.getAttributeValue(ATTRIBUTE_VALUE);
        String valueStr = Integer.toHexString(attributeValue);
        Bounds bounds = getOffsetBounds(painter.getAttributeSet());

        Graphics graphics = painter.getGraphics();
        GraphicsUtil.switchToWidth(graphics, 2);
        graphics.fillOval(-2, -2, 5, 5);
        GraphicsUtil.drawCenteredText(graphics, valueStr, bounds.getX() + bounds.getWidth() / 2,
            bounds.getY() + bounds.getHeight() / 2);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Bounds bounds = painter.getOffsetBounds();
        BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
        int intValue = painter.getAttributeValue(ATTRIBUTE_VALUE);
        Value value = Value.createKnown(width, intValue);
        Location location = painter.getLocation();
        int x = location.getX();
        int y = location.getY();

        Graphics graphics = painter.getGraphics();
        if (painter.shouldDrawColor()) {
            graphics.setColor(BACKGROUND_COLOR);
            graphics.fillRect(x + bounds.getX(), y + bounds.getY(), bounds.getWidth(), bounds.getHeight());
        }
        if (value.getWidth() == 1) {
            if (painter.shouldDrawColor()) {
                graphics.setColor(value.getColor());
            }
            GraphicsUtil.drawCenteredText(graphics, value.toString(),
                x + bounds.getX() + bounds.getWidth() / 2,
                y + bounds.getY() + bounds.getHeight() / 2 - 2);
        } else {
            graphics.setColor(Color.BLACK);
            GraphicsUtil.drawCenteredText(graphics, value.toHexString(),
                x + bounds.getX() + bounds.getWidth() / 2,
                y + bounds.getY() + bounds.getHeight() / 2 - 2);
        }
        painter.drawPorts();
    }

    private static class ConstantAttributes extends AbstractAttributeSet {

        private Direction facing = Direction.EAST;
        private BitWidth width = BitWidth.ONE;
        private Value value = Value.TRUE;

        @Override
        protected void copyInto(AbstractAttributeSet destObj) {
            ConstantAttributes dest = (ConstantAttributes) destObj;
            dest.facing = this.facing;
            dest.width = this.width;
            dest.value = this.value;
        }

        @Override
        public List<Attribute<?>> getAttributes() {
            return ATTRIBUTES;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <V> V getValue(Attribute<V> attr) {
            if (attr == StdAttr.FACING) {
                return (V) facing;
            }
            if (attr == StdAttr.WIDTH) {
                return (V) width;
            }
            if (attr == ATTRIBUTE_VALUE) {
                return (V) Integer.valueOf(value.toIntValue());
            }
            return null;
        }

        @Override
        public <V> void setValue(Attribute<V> attribute, V value) {
            if (attribute == StdAttr.FACING) {
                facing = (Direction) value;
            } else if (attribute == StdAttr.WIDTH) {
                width = (BitWidth) value;
                this.value = this.value.extendWidth(width.getWidth(),
                    this.value.get(this.value.getWidth() - 1));
            } else if (attribute == ATTRIBUTE_VALUE) {
                int val = (Integer) value;
                this.value = Value.createKnown(width, val);
            } else {
                throw new IllegalArgumentException("unknown attribute " + attribute);
            }
            fireAttributeValueChanged(attribute, value);
        }
    }

    private static class ConstantExpression implements ExpressionComputer {

        private final Instance instance;

        private ConstantExpression(Instance instance) {
            this.instance = instance;
        }

        public void computeExpression(Map<Location, Expression> expressionMap) {
            AttributeSet attrs = instance.getAttributeSet();
            int intValue = attrs.getValue(ATTRIBUTE_VALUE);

            expressionMap.put(instance.getLocation(),
                Expressions.constant(intValue));
        }
    }

    //TODO: Allow editing of value via text tool/attribute table
}

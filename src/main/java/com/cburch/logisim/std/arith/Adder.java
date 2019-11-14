/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class Adder extends InstanceFactory {

    static final int PER_DELAY = 1;

    private static final int IN0 = 0;
    private static final int IN1 = 1;
    private static final int OUT = 2;
    private static final int C_IN = 3;
    private static final int C_OUT = 4;

    public Adder() {
        super("Adder", Strings.getter("adderComponent"));
        setAttributes(new Attribute[]{
                StdAttr.WIDTH
        }, new Object[]{
                BitWidth.create(8)
        });
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
        setIconName("adder.gif");

        Port[] ports = new Port[5];
        ports[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
        ports[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
        ports[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
        ports[C_IN] = new Port(-20, -20, Port.INPUT, 1);
        ports[C_OUT] = new Port(-20, 20, Port.INPUT, 1);
        ports[IN0].setToolTip(Strings.getter("adderInputTip"));
        ports[IN1].setToolTip(Strings.getter("adderInputTip"));
        ports[OUT].setToolTip(Strings.getter("adderOutputTip"));
        ports[C_IN].setToolTip(Strings.getter("adderCarryInTip"));
        ports[C_OUT].setToolTip(Strings.getter("adderCarryOutTip"));
        setPorts(ports);
    }

    static Value[] computeSum(BitWidth bitWidth, Value a, Value b, Value cIn) {
        int width = bitWidth.getWidth();
        if (cIn == Value.UNKNOWN || cIn == Value.NIL) {
            cIn = Value.FALSE;
        }
        if (a.isFullyDefined() && b.isFullyDefined() && cIn.isFullyDefined()) {
            if (width >= 32) {
                long mask = (1L << width) - 1;
                long ax = (long) a.toIntValue() & mask;
                long bx = (long) b.toIntValue() & mask;
                long cx = (long) cIn.toIntValue() & mask;
                long sum = ax + bx + cx;
                return new Value[]{Value.createKnown(bitWidth, (int) sum),
                        ((sum >> width) & 1) == 0 ? Value.FALSE : Value.TRUE};
            } else {
                int sum = a.toIntValue() + b.toIntValue() + cIn.toIntValue();
                return new Value[]{
                        Value.createKnown(bitWidth, sum),
                        ((sum >> width) & 1) == 0 ? Value.FALSE : Value.TRUE
                };
            }
        } else {
            Value[] bits = new Value[width];
            Value carry = cIn;
            for (int i = 0; i < width; i++) {
                if (carry == Value.ERROR) {
                    bits[i] = Value.ERROR;
                } else if (carry == Value.UNKNOWN) {
                    bits[i] = Value.UNKNOWN;
                } else {
                    Value ab = a.get(i);
                    Value bb = b.get(i);
                    if (ab == Value.ERROR || bb == Value.ERROR) {
                        bits[i] = Value.ERROR;
                        carry = Value.ERROR;
                    } else if (ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
                        bits[i] = Value.UNKNOWN;
                        carry = Value.UNKNOWN;
                    } else {
                        int sum = (ab == Value.TRUE ? 1 : 0)
                                + (bb == Value.TRUE ? 1 : 0)
                                + (carry == Value.TRUE ? 1 : 0);
                        bits[i] = (sum & 1) == 1 ? Value.TRUE : Value.FALSE;
                        carry = (sum >= 2) ? Value.TRUE : Value.FALSE;
                    }
                }
            }
            return new Value[]{Value.create(bits), carry};
        }
    }

    @Override
    public void propagate(InstanceState state) {
        // get attributes
        BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

        // compute outputs
        Value a = state.getPort(IN0);
        Value b = state.getPort(IN1);
        Value c_in = state.getPort(C_IN);
        Value[] outs = Adder.computeSum(dataWidth, a, b, c_in);

        // propagate them
        int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
        state.setPort(OUT, outs[0], delay);
        state.setPort(C_OUT, outs[1], delay);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics graphics = painter.getGraphics();
        painter.drawBounds();

        graphics.setColor(Color.GRAY);
        painter.drawPort(IN0);
        painter.drawPort(IN1);
        painter.drawPort(OUT);
        painter.drawPort(C_IN, "c in", Direction.NORTH);
        painter.drawPort(C_OUT, "c out", Direction.SOUTH);

        Location location = painter.getLocation();
        int x = location.getX();
        int y = location.getY();
        GraphicsUtil.switchToWidth(graphics, 2);
        graphics.setColor(Color.BLACK);
        graphics.drawLine(x - 15, y, x - 5, y);
        graphics.drawLine(x - 10, y - 5, x - 10, y + 5);
        GraphicsUtil.switchToWidth(graphics, 1);
    }
}

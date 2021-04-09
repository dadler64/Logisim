/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
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
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class BitExtender extends InstanceFactory {

    public static final BitExtender FACTORY = new BitExtender();
    private static final Attribute<BitWidth> ATTR_IN_WIDTH = Attributes
        .forBitWidth("in_width", Strings.getter("extenderInAttr"));
    private static final Attribute<BitWidth> ATTR_OUT_WIDTH = Attributes
        .forBitWidth("out_width", Strings.getter("extenderOutAttr"));
    private static final Attribute<AttributeOption> ATTR_TYPE = Attributes.forOption("type", Strings.getter("extenderTypeAttr"),
        new AttributeOption[]{
            new AttributeOption("zero", "zero", Strings.getter("extenderZeroType")),
            new AttributeOption("one", "one", Strings.getter("extenderOneType")),
            new AttributeOption("sign", "sign", Strings.getter("extenderSignType")),
            new AttributeOption("input", "input", Strings.getter("extenderInputType")),
        });

    public BitExtender() {
        super("Bit Extender", Strings.getter("extenderComponent"));
        setIconName("extender.gif");
        setAttributes(
            new Attribute[]{
                ATTR_IN_WIDTH,
                ATTR_OUT_WIDTH,
                ATTR_TYPE
            }, new Object[]{
                BitWidth.create(8),
                BitWidth.create(16),
                ATTR_TYPE.parse("zero")
            }
        );
        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(JoinedConfigurator.create(new BitWidthConfigurator(ATTR_OUT_WIDTH),
            new BitWidthConfigurator(ATTR_IN_WIDTH, 1, Value.MAX_WIDTH, 0)));
        setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    }

    //
    // graphics methods
    //
    @Override
    public void paintInstance(InstancePainter painter) {
        Graphics graphics = painter.getGraphics();
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int ascent = fontMetrics.getAscent();

        painter.drawBounds();

        String label;
        String type = getType(painter.getAttributeSet());
        switch (type) {
            case "zero":
                label = Strings.get("extenderZeroLabel");
                break;
            case "one":
                label = Strings.get("extenderOneLabel");
                break;
            case "sign":
                label = Strings.get("extenderSignLabel");
                break;
            case "input":
                label = Strings.get("extenderInputLabel");
                break;
            default:
                label = "???"; // should never happen

                break;
        }
        String mainLabel = Strings.get("extenderMainLabel");
        Bounds bounds = painter.getBounds();
        int x = bounds.getX() + bounds.getWidth() / 2;
        int y0 = bounds.getY() + (bounds.getHeight() / 2 + ascent) / 2;
        int y1 = bounds.getY() + (3 * bounds.getHeight() / 2 + ascent) / 2;
        GraphicsUtil.drawText(graphics, label, x, y0, GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
        GraphicsUtil.drawText(graphics, mainLabel, x, y1, GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);

        BitWidth w0 = painter.getAttributeValue(ATTR_OUT_WIDTH);
        BitWidth w1 = painter.getAttributeValue(ATTR_IN_WIDTH);
        painter.drawPort(0, "" + w0.getWidth(), Direction.WEST);
        painter.drawPort(1, "" + w1.getWidth(), Direction.EAST);
        if (type.equals("input")) {
            painter.drawPort(2);
        }
    }

    //
    // methods for instances
    //
    @Override
    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);
        instance.addAttributeListener();
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == ATTR_TYPE) {
            configurePorts(instance);
        }
        instance.fireInvalidated();
    }

    private void configurePorts(Instance instance) {
        Port port0 = new Port(0, 0, Port.OUTPUT, ATTR_OUT_WIDTH);
        Port port1 = new Port(-40, 0, Port.INPUT, ATTR_IN_WIDTH);
        String type = getType(instance.getAttributeSet());
        if (type.equals("input")) {
            instance.setPorts(new Port[]{port0, port1, new Port(-20, -20, Port.INPUT, 1)});
        } else {
            instance.setPorts(new Port[]{port0, port1});
        }
    }

    @Override
    public void propagate(InstanceState state) {
        Value in = state.getPort(1);
        BitWidth wout = state.getAttributeValue(ATTR_OUT_WIDTH);
        String type = getType(state.getAttributeSet());
        Value extend;
        switch (type) {
            case "one": {
                extend = Value.TRUE;
                break;
            }
            case "sign": {
                int win = in.getWidth();
                extend = win > 0 ? in.get(win - 1) : Value.ERROR;
                break;
            }
            case "input": {
                extend = state.getPort(2);
                if (extend.getWidth() != 1) {
                    extend = Value.ERROR;
                }
                break;
            }
            default: {
                extend = Value.FALSE;
                break;
            }
        }

        Value out = in.extendWidth(wout.getWidth(), extend);
        state.setPort(0, out, 1);
    }


    private String getType(AttributeSet attrs) {
        AttributeOption topt = attrs.getValue(ATTR_TYPE);
        return (String) topt.getValue();
    }
}

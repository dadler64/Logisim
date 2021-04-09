/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Attribute;
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
import com.cburch.logisim.tools.key.IntegerConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics;

public class ShiftRegister extends InstanceFactory {

    static final Attribute<Integer> ATTR_LENGTH = Attributes.forIntegerRange("length",
        Strings.getter("shiftRegLengthAttr"), 1, 32);
    static final Attribute<Boolean> ATTR_LOAD = Attributes.forBoolean("parallel",
        Strings.getter("shiftRegParallelAttr"));

    private static final int IN = 0;
    private static final int SH = 1;
    private static final int CK = 2;
    private static final int CLR = 3;
    private static final int OUT = 4;
    private static final int LD = 5;

    public ShiftRegister() {
        super("Shift Register", Strings.getter("shiftRegisterComponent"));
        setAttributes(
            new Attribute[]{
                StdAttr.WIDTH,
                ATTR_LENGTH,
                ATTR_LOAD,
                StdAttr.EDGE_TRIGGER,
                StdAttr.LABEL,
                StdAttr.LABEL_FONT
            }, new Object[]{
                BitWidth.ONE,
                8,
                Boolean.TRUE,
                StdAttr.TRIG_RISING,
                "",
                StdAttr.DEFAULT_LABEL_FONT
            }
        );
        setKeyConfigurator(JoinedConfigurator.create(
            new IntegerConfigurator(ATTR_LENGTH, 1, 32, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)
        ));

        setIconName("shiftreg.gif");
        setInstanceLogger(ShiftRegisterLogger.class);
        setInstancePoker(ShiftRegisterPoker.class);
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        Boolean parallel = attributes.getValue(ATTR_LOAD);
        if (parallel == null || parallel) {
            int len = attributes.getValue(ATTR_LENGTH);
            return Bounds.create(0, -20, 20 + 10 * len, 40);
        } else {
            return Bounds.create(0, -20, 30, 40);
        }
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        configurePorts(instance);
        instance.addAttributeListener();
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == ATTR_LOAD || attribute == ATTR_LENGTH || attribute == StdAttr.WIDTH) {
            instance.recomputeBounds();
            configurePorts(instance);
        }
    }

    private void configurePorts(Instance instance) {
        BitWidth widthObj = instance.getAttributeValue(StdAttr.WIDTH);
        int width = widthObj.getWidth();
        Boolean parallelObj = instance.getAttributeValue(ATTR_LOAD);
        Bounds bounds = instance.getBounds();
        Port[] ports;
        if (parallelObj == null || parallelObj) {
            Integer lengthObject = instance.getAttributeValue(ATTR_LENGTH);
            int length = lengthObject == null ? 8 : lengthObject;
            ports = new Port[6 + 2 * length];
            ports[LD] = new Port(10, -20, Port.INPUT, 1);
            ports[LD].setToolTip(Strings.getter("shiftRegLoadTip"));
            for (int i = 0; i < length; i++) {
                ports[6 + 2 * i] = new Port(20 + 10 * i, -20, Port.INPUT, width);
                ports[6 + 2 * i + 1] = new Port(20 + 10 * i, 20, Port.OUTPUT, width);
            }
        } else {
            ports = new Port[5];
        }
        ports[OUT] = new Port(bounds.getWidth(), 0, Port.OUTPUT, width);
        ports[SH] = new Port(0, -10, Port.INPUT, 1);
        ports[IN] = new Port(0, 0, Port.INPUT, width);
        ports[CK] = new Port(0, 10, Port.INPUT, 1);
        ports[CLR] = new Port(10, 20, Port.INPUT, 1);
        ports[OUT].setToolTip(Strings.getter("shiftRegOutTip"));
        ports[SH].setToolTip(Strings.getter("shiftRegShiftTip"));
        ports[IN].setToolTip(Strings.getter("shiftRegInTip"));
        ports[CK].setToolTip(Strings.getter("shiftRegClockTip"));
        ports[CLR].setToolTip(Strings.getter("shiftRegClearTip"));
        instance.setPorts(ports);

        instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, bounds.getX() + bounds.getWidth() / 2,
            bounds.getY() + bounds.getHeight() / 4, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    }

    private ShiftRegisterData getData(InstanceState state) {
        BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
        Integer lengthObject = state.getAttributeValue(ATTR_LENGTH);
        int length = lengthObject == null ? 8 : lengthObject;
        ShiftRegisterData data = (ShiftRegisterData) state.getData();
        if (data == null) {
            data = new ShiftRegisterData(width, length);
            state.setData(data);
        } else {
            data.setDimensions(width, length);
        }
        return data;
    }

    @Override
    public void propagate(InstanceState state) {
        Object triggerType = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
        boolean parallel = state.getAttributeValue(ATTR_LOAD);
        ShiftRegisterData data = getData(state);
        int length = data.getLength();

        boolean triggered = data.updateClock(state.getPort(CK), triggerType);
        if (state.getPort(CLR) == Value.TRUE) {
            data.clear();
        } else if (triggered) {
            if (parallel && state.getPort(LD) == Value.TRUE) {
                data.clear();
                for (int i = length - 1; i >= 0; i--) {
                    data.push(state.getPort(6 + 2 * i));
                }
            } else if (state.getPort(SH) != Value.FALSE) {
                data.push(state.getPort(IN));
            }
        }

        state.setPort(OUT, data.get(0), 4);
        if (parallel) {
            for (int i = 0; i < length; i++) {
                state.setPort(6 + 2 * i + 1, data.get(length - 1 - i), 4);
            }
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        // draw boundary, label
        painter.drawBounds();
        painter.drawLabel();

        // draw state
        boolean parallel = painter.getAttributeValue(ATTR_LOAD);
        if (parallel) {
            BitWidth widthObject = painter.getAttributeValue(StdAttr.WIDTH);
            int width = widthObject.getWidth();
            Integer lengthObject = painter.getAttributeValue(ATTR_LENGTH);
            int length = lengthObject == null ? 8 : lengthObject;
            if (painter.getShowState()) {
                if (width <= 4) {
                    ShiftRegisterData data = getData(painter);
                    Bounds bounds = painter.getBounds();
                    int x = bounds.getX() + 20;
                    int y = bounds.getY();
                    Object label = painter.getAttributeValue(StdAttr.LABEL);
                    if (label == null || label.equals("")) {
                        y += bounds.getHeight() / 2;
                    } else {
                        y += 3 * bounds.getHeight() / 4;
                    }
                    Graphics g = painter.getGraphics();
                    for (int i = 0; i < length; i++) {
                        String s = data.get(length - 1 - i).toHexString();
                        GraphicsUtil.drawCenteredText(g, s, x, y);
                        x += 10;
                    }
                }
            } else {
                Bounds bounds = painter.getBounds();
                int x = bounds.getX() + bounds.getWidth() / 2;
                int y = bounds.getY();
                int height = bounds.getHeight();
                Graphics g = painter.getGraphics();
                Object label = painter.getAttributeValue(StdAttr.LABEL);
                if (label == null || label.equals("")) {
                    String a = Strings.get("shiftRegisterLabel1");
                    GraphicsUtil.drawCenteredText(g, a, x, y + height / 4);
                }
                String b = Strings.get("shiftRegisterLabel2", "" + length, "" + width);
                GraphicsUtil.drawCenteredText(g, b, x, y + 3 * height / 4);
            }
        }

        // draw input and output ports
        int ports = painter.getInstance().getPorts().size();
        for (int i = 0; i < ports; i++) {
            if (i != CK) {
                painter.drawPort(i);
            }
        }
        painter.drawClock(CK, Direction.EAST);
    }
}

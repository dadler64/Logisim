/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Tty extends InstanceFactory {

    private static final int CLR = 0;
    private static final int CK = 1;
    private static final int WE = 2;
    private static final int IN = 3;

    private static final int BORDER = 5;
    private static final int ROW_HEIGHT = 15;
    private static final int COL_WIDTH = 7;
    private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 64);

    private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

    private static final Attribute<Integer> ATTR_COLUMNS
        = Attributes.forIntegerRange("cols", Strings.getter("ttyColsAttr"), 1, 120);
    private static final Attribute<Integer> ATTR_ROWS
        = Attributes.forIntegerRange("rows", Strings.getter("ttyRowsAttr"), 1, 48);

    public Tty() {
        super("TTY", Strings.getter("ttyComponent"));
        setAttributes(
            new Attribute[]{
                ATTR_ROWS,
                ATTR_COLUMNS,
                StdAttr.EDGE_TRIGGER,
                Io.ATTR_COLOR,
                Io.ATTR_BACKGROUND
            }, new Object[]{
                8,
                32,
                StdAttr.TRIG_RISING,
                Color.BLACK,
                DEFAULT_BACKGROUND
            }
        );
        setIconName("tty.gif");

        Port[] ports = new Port[4];
        ports[CLR] = new Port(20, 10, Port.INPUT, 1);
        ports[CK] = new Port(0, 0, Port.INPUT, 1);
        ports[WE] = new Port(10, 10, Port.INPUT, 1);
        ports[IN] = new Port(0, -10, Port.INPUT, 7);
        ports[CLR].setToolTip(Strings.getter("ttyClearTip"));
        ports[CK].setToolTip(Strings.getter("ttyClockTip"));
        ports[WE].setToolTip(Strings.getter("ttyEnableTip"));
        ports[IN].setToolTip(Strings.getter("ttyInputTip"));
        setPorts(ports);
    }

    private static int getRowCount(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return 4;
        }
    }

    private static int getColumnCount(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        } else {
            return 16;
        }
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        int rows = getRowCount(attributes.getValue(ATTR_ROWS));
        int cols = getColumnCount(attributes.getValue(ATTR_COLUMNS));
        int width = 2 * BORDER + cols * COL_WIDTH;
        int height = 2 * BORDER + rows * ROW_HEIGHT;
        if (width < 30) {
            width = 30;
        }
        if (height < 30) {
            height = 30;
        }
        return Bounds.create(0, 10 - height, width, height);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == ATTR_ROWS || attribute == ATTR_COLUMNS) {
            instance.recomputeBounds();
        }
    }

    @Override
    public void propagate(InstanceState circState) {
        Object trigger = circState.getAttributeValue(StdAttr.EDGE_TRIGGER);
        TtyState state = getTtyState(circState);
        Value clear = circState.getPort(CLR);
        Value clock = circState.getPort(CK);
        Value enable = circState.getPort(WE);
        Value in = circState.getPort(IN);

        synchronized (state) {
            Value lastClock = state.setLastClock(clock);
            if (clear == Value.TRUE) {
                state.clear();
            } else if (enable != Value.FALSE) {
                boolean go;
                if (trigger == StdAttr.TRIG_FALLING) {
                    go = lastClock == Value.TRUE && clock == Value.FALSE;
                } else {
                    go = lastClock == Value.FALSE && clock == Value.TRUE;
                }
                if (go) {
                    state.add(in.isFullyDefined() ? (char) in.toIntValue() : '?');
                }
            }
        }
    }

    @Override
    public void paintGhost(InstancePainter painter) {
        Graphics g = painter.getGraphics();
        GraphicsUtil.switchToWidth(g, 2);
        Bounds bounds = painter.getBounds();
        g.drawRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 10, 10);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        boolean showState = painter.getShowState();
        Graphics g = painter.getGraphics();
        Bounds bounds = painter.getBounds();
        painter.drawClock(CK, Direction.EAST);
        if (painter.shouldDrawColor()) {
            g.setColor(painter.getAttributeValue(Io.ATTR_BACKGROUND));
            g.fillRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 10, 10);
        }
        GraphicsUtil.switchToWidth(g, 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 2 * BORDER, 2 * BORDER);
        GraphicsUtil.switchToWidth(g, 1);
        painter.drawPort(CLR);
        painter.drawPort(WE);
        painter.drawPort(IN);

        int rows = getRowCount(painter.getAttributeValue(ATTR_ROWS));
        int cols = getColumnCount(painter.getAttributeValue(ATTR_COLUMNS));

        if (showState) {
            String[] rowData = new String[rows];
            int curRow;
            int curCol;
            TtyState state = getTtyState(painter);
            synchronized (state) {
                for (int i = 0; i < rows; i++) {
                    rowData[i] = state.getRowString(i);
                }
                curRow = state.getCursorRow();
                curCol = state.getCursorColumn();
            }

            g.setFont(DEFAULT_FONT);
            g.setColor(painter.getAttributeValue(Io.ATTR_COLOR));
            FontMetrics fm = g.getFontMetrics();
            int x = bounds.getX() + BORDER;
            int y = bounds.getY() + BORDER + (ROW_HEIGHT + fm.getAscent()) / 2;
            for (int i = 0; i < rows; i++) {
                g.drawString(rowData[i], x, y);
                if (i == curRow) {
                    int x0 = x + fm.stringWidth(rowData[i].substring(0, curCol));
                    g.drawLine(x0, y - fm.getAscent(), x0, y);
                }
                y += ROW_HEIGHT;
            }
        } else {
            String str = Strings.get("ttyDesc", "" + rows, "" + cols);
            FontMetrics fm = g.getFontMetrics();
            int strWidth = fm.stringWidth(str);
            if (strWidth + BORDER > bounds.getWidth()) {
                str = Strings.get("ttyDescShort");
                strWidth = fm.stringWidth(str);
            }
            int x = bounds.getX() + (bounds.getWidth() - strWidth) / 2;
            int y = bounds.getY() + (bounds.getHeight() + fm.getAscent()) / 2;
            g.drawString(str, x, y);
        }
    }

    private TtyState getTtyState(InstanceState state) {
        int rows = getRowCount(state.getAttributeValue(ATTR_ROWS));
        int cols = getColumnCount(state.getAttributeValue(ATTR_COLUMNS));
        TtyState ret = (TtyState) state.getData();
        if (ret == null) {
            ret = new TtyState(rows, cols);
            state.setData(ret);
        } else {
            ret.updateSize(rows, cols);
        }
        return ret;
    }

    public void sendToStdout(InstanceState state) {
        TtyState tty = getTtyState(state);
        tty.setSendStdout(true);
    }
}

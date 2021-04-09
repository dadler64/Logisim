/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class Tunnel extends InstanceFactory {

    public static final Tunnel FACTORY = new Tunnel();

    static final int MARGIN = 3;
    static final int ARROW_MARGIN = 5;
    static final int ARROW_DEPTH = 4;
    static final int ARROW_MIN_WIDTH = 16;
    static final int ARROW_MAX_WIDTH = 20;

    public Tunnel() {
        super("Tunnel", Strings.getter("tunnelComponent"));
        setIconName("tunnel.gif");
        setFacingAttribute(StdAttr.FACING);
        setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    }

    @Override
    public AttributeSet createAttributeSet() {
        return new TunnelAttributes();
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        TunnelAttributes attrs = (TunnelAttributes) attributes;
        Bounds bounds = attrs.getOffsetBounds();
        if (bounds == null) {
            int height = attrs.getFont().getSize();
            int width = height * attrs.getLabel().length() / 2;
            bounds = computeBounds(attrs, width, height, null, "");
            attrs.setOffsetBounds(bounds);
        }
        return bounds;
    }

    //
    // graphics methods
    //
    @Override
    public void paintGhost(InstancePainter painter) {
        TunnelAttributes attrs = (TunnelAttributes) painter.getAttributeSet();
        Direction facing = attrs.getFacing();
        String label = attrs.getLabel();

        Graphics g = painter.getGraphics();
        g.setFont(attrs.getFont());
        FontMetrics fm = g.getFontMetrics();
        Bounds bounds = computeBounds(attrs, fm.stringWidth(label), fm.getAscent() + fm.getDescent(), g, label);
        if (attrs.setOffsetBounds(bounds)) {
            Instance instance = painter.getInstance();
            if (instance != null) {
                instance.recomputeBounds();
            }
        }

        int x0 = bounds.getX();
        int y0 = bounds.getY();
        int x1 = x0 + bounds.getWidth();
        int y1 = y0 + bounds.getHeight();
        int maxWidth = ARROW_MAX_WIDTH / 2;
        int[] xp;
        int[] yp;
        if (facing == Direction.NORTH) {
            int yb = y0 + ARROW_DEPTH;
            if (x1 - x0 <= ARROW_MAX_WIDTH) {
                xp = new int[]{x0, 0, x1, x1, x0};
                yp = new int[]{yb, y0, yb, y1, y1};
            } else {
                xp = new int[]{x0, -maxWidth, 0, maxWidth, x1, x1, x0};
                yp = new int[]{yb, yb, y0, yb, yb, y1, y1};
            }
        } else if (facing == Direction.SOUTH) {
            int yb = y1 - ARROW_DEPTH;
            if (x1 - x0 <= ARROW_MAX_WIDTH) {
                xp = new int[]{x0, x1, x1, 0, x0};
                yp = new int[]{y0, y0, yb, y1, yb};
            } else {
                xp = new int[]{x0, x1, x1, maxWidth, 0, -maxWidth, x0};
                yp = new int[]{y0, y0, yb, yb, y1, yb, yb};
            }
        } else if (facing == Direction.EAST) {
            int xb = x1 - ARROW_DEPTH;
            if (y1 - y0 <= ARROW_MAX_WIDTH) {
                xp = new int[]{x0, xb, x1, xb, x0};
                yp = new int[]{y0, y0, 0, y1, y1};
            } else {
                xp = new int[]{x0, xb, xb, x1, xb, xb, x0};
                yp = new int[]{y0, y0, -maxWidth, 0, maxWidth, y1, y1};
            }
        } else {
            int xb = x0 + ARROW_DEPTH;
            if (y1 - y0 <= ARROW_MAX_WIDTH) {
                xp = new int[]{xb, x1, x1, xb, x0};
                yp = new int[]{y0, y0, y1, y1, 0};
            } else {
                xp = new int[]{xb, x1, x1, xb, xb, x0, xb};
                yp = new int[]{y0, y0, y1, y1, maxWidth, 0, -maxWidth};
            }
        }
        GraphicsUtil.switchToWidth(g, 2);
        g.drawPolygon(xp, yp, xp.length);
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        Location location = painter.getLocation();
        int x = location.getX();
        int y = location.getY();
        Graphics g = painter.getGraphics();
        g.translate(x, y);
        g.setColor(Color.BLACK);
        paintGhost(painter);
        g.translate(-x, -y);
        painter.drawPorts();
    }

    //
    // methods for instances
    //
    @Override
    protected void configureNewInstance(Instance instance) {
        instance.addAttributeListener();
        instance.setPorts(new Port[]{
            new Port(0, 0, Port.INOUT, StdAttr.WIDTH)
        });
        configureLabel(instance);
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        if (attribute == StdAttr.FACING) {
            configureLabel(instance);
            instance.recomputeBounds();
        } else if (attribute == StdAttr.LABEL || attribute == StdAttr.LABEL_FONT) {
            instance.recomputeBounds();
        }
    }

    @Override
    public void propagate(InstanceState state) {
        // nothing to do - handled by circuit
    }

    //
    // private methods
    //
    private void configureLabel(Instance instance) {
        TunnelAttributes attrs = (TunnelAttributes) instance.getAttributeSet();
        Location location = instance.getLocation();
        instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, location.getX() + attrs.getLabelX(),
            location.getY() + attrs.getLabelY(), attrs.getLabelHAlign(), attrs.getLabelVAlign());
    }

    private Bounds computeBounds(TunnelAttributes attrs, int textWidth, int textHeight, Graphics g, String label) {
        int x = attrs.getLabelX();
        int y = attrs.getLabelY();
        int hAlign = attrs.getLabelHAlign();
        int vAlign = attrs.getLabelVAlign();

        int minDimension = ARROW_MIN_WIDTH - 2 * MARGIN;
        int bw = Math.max(minDimension, textWidth);
        int bh = Math.max(minDimension, textHeight);
        int bx;
        int by;
        switch (hAlign) {
            case TextField.H_LEFT:
                bx = x;
                break;
            case TextField.H_RIGHT:
                bx = x - bw;
                break;
            default:
                bx = x - (bw / 2);
        }
        switch (vAlign) {
            case TextField.V_TOP:
                by = y;
                break;
            case TextField.V_BOTTOM:
                by = y - bh;
                break;
            default:
                by = y - (bh / 2);
        }

        if (g != null) {
            GraphicsUtil.drawText(g, label, bx + bw / 2, by + bh / 2, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
        }

        return Bounds.create(bx, by, bw, bh).expand(MARGIN).add(0, 0);
    }
}

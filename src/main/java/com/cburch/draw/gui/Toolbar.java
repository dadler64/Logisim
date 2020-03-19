/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.gui;

import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.draw.tools.AbstractTool;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.Icon;
import javax.swing.JComponent;

class Toolbar extends JComponent {

    private static int ICON_WIDTH = 16;
    private static int ICON_HEIGHT = 16;
    private static int ICON_SEPARATION = 4;
    private Canvas canvas;
    private AbstractTool[][] tools;
    private Listener listener;

    public Toolbar(Canvas canvas, DrawingAttributeSet attributes) {
        this.canvas = canvas;
        this.tools = new AbstractTool[][]{AbstractTool.getTools(attributes)};
        this.listener = new Listener();

        AbstractTool[] toolBase = AbstractTool.getTools(attributes);
        this.tools = new AbstractTool[2][];
        this.tools[0] = new AbstractTool[(toolBase.length + 1) / 2];
        this.tools[1] = new AbstractTool[toolBase.length / 2];
        for (int i = 0; i < toolBase.length; i++) {
            this.tools[i % 2][i / 2] = toolBase[i];
        }

        setPreferredSize(new Dimension(3 * ICON_SEPARATION + 2 * ICON_WIDTH,
                ICON_SEPARATION + tools[0].length * (ICON_HEIGHT + ICON_SEPARATION)));
        addMouseListener(listener);
        addMouseMotionListener(listener);
    }

    public AbstractTool getDefaultTool() {
        return tools[0][0];
    }

    @Override
    public void paintComponent(Graphics graphics) {
        graphics.clearRect(0, 0, getWidth(), getHeight());
        CanvasTool current = canvas.getTool();
        for (int i = 0; i < tools.length; i++) {
            AbstractTool[] tools = this.tools[i];
            int x = ICON_SEPARATION + i * (ICON_SEPARATION + ICON_WIDTH);
            int y = ICON_SEPARATION;
            for (AbstractTool tool : tools) {
                if (tool == listener.toolPressed && listener.inTool) {
                    graphics.setColor(Color.darkGray);
                    graphics.fillRect(x, y, ICON_WIDTH, ICON_HEIGHT);
                }
                Icon icon = tool.getIcon();
                if (icon != null) {
                    icon.paintIcon(this, graphics, x, y);
                }
                if (tool == current) {
                    GraphicsUtil.switchToWidth(graphics, 2);
                    graphics.setColor(Color.black);
                    graphics.drawRect(x - 1, y - 1, ICON_WIDTH + 2, ICON_HEIGHT + 2);
                }
                y += ICON_HEIGHT + ICON_SEPARATION;
            }
        }
        graphics.setColor(Color.black);
        GraphicsUtil.switchToWidth(graphics, 1);
    }

    private class Listener implements MouseListener, MouseMotionListener {

        private AbstractTool toolPressed;
        private boolean inTool;
        private int toolX;
        private int toolY;

        public void mouseClicked(MouseEvent event) {
        }

        public void mouseEntered(MouseEvent event) {
        }

        public void mouseExited(MouseEvent event) {
        }

        public void mousePressed(MouseEvent event) {
            int mx = event.getX();
            int my = event.getY();
            int column = (event.getX() - ICON_SEPARATION) / (ICON_WIDTH + ICON_SEPARATION);
            int row = (event.getY() - ICON_SEPARATION) / (ICON_HEIGHT + ICON_SEPARATION);
            int x0 = ICON_SEPARATION + column * (ICON_SEPARATION + ICON_WIDTH);
            int y0 = ICON_SEPARATION + row * (ICON_SEPARATION + ICON_HEIGHT);

            if (mx >= x0 && mx < x0 + ICON_WIDTH
                    && my >= y0 && my < y0 + ICON_HEIGHT
                    && column >= 0 && column < tools.length
                    && row >= 0 && row < tools[column].length) {
                toolPressed = tools[column][row];
                inTool = true;
                toolX = x0;
                toolY = y0;
                repaint();
            } else {
                toolPressed = null;
                inTool = false;
            }
        }

        public void mouseReleased(MouseEvent event) {
            mouseDragged(event);
            if (inTool) {
                canvas.setTool(toolPressed);
                repaint();
            }
            toolPressed = null;
            inTool = false;
        }

        public void mouseDragged(MouseEvent event) {
            int mx = event.getX();
            int my = event.getY();
            int x0 = toolX;
            int y0 = toolY;

            boolean wasInTool = inTool;
            boolean isInTool = toolPressed != null
                    && mx >= x0 && mx < x0 + ICON_WIDTH
                    && my >= y0 && my < y0 + ICON_HEIGHT;
            if (wasInTool != isInTool) {
                inTool = isInTool;
                repaint();
            }
        }

        public void mouseMoved(MouseEvent event) {
        }

    }
}

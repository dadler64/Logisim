/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class HorizontalSplitPane extends JPanel {

    static final int DRAG_TOLERANCE = 3;
    private static final Color DRAG_COLOR = new Color(0, 0, 0, 128);
    private final JComponent component0;
    private final JComponent component1;
    private final MyDragbar myDragbar;
    private double fraction;

    public HorizontalSplitPane(JComponent component0, JComponent component1) {
        this(component0, component1, 0.5);
    }

    public HorizontalSplitPane(JComponent component0, JComponent component1, double fraction) {
        this.component0 = component0;
        this.component1 = component1;
        this.myDragbar = new MyDragbar(); // above the other components
        this.fraction = fraction;

        setLayout(new MyLayout());
        add(myDragbar); // above the other components
        add(component0);
        add(component1);
    }

    public double getFraction() {
        return fraction;
    }

    public void setFraction(double value) {
        if (value < 0.0) {
            value = 0.0;
        }
        if (value > 1.0) {
            value = 1.0;
        }
        if (fraction != value) {
            fraction = value;
            revalidate();
        }
    }

    abstract static class Dragbar extends JComponent implements MouseListener, MouseMotionListener {

        private boolean dragging = false;
        private int curValue;

        Dragbar() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        abstract int getDragValue(MouseEvent e);

        abstract void setDragValue(int value);

        @Override
        public void paintComponent(Graphics g) {
            if (dragging) {
                g.setColor(DRAG_COLOR);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (!dragging) {
                curValue = getDragValue(e);
                dragging = true;
                repaint();
            }
        }

        public void mouseReleased(MouseEvent e) {
            if (dragging) {
                dragging = false;
                int newValue = getDragValue(e);
                if (newValue != curValue) {
                    setDragValue(newValue);
                }
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent e) {
            if (dragging) {
                int newValue = getDragValue(e);
                if (newValue != curValue) {
                    setDragValue(newValue);
                }
            }
        }

        public void mouseMoved(MouseEvent e) {
        }
    }

    private class MyLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component component) {
        }

        public void removeLayoutComponent(Component component) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            if (fraction <= 0.0) {
                return component1.getPreferredSize();
            }
            if (fraction >= 1.0) {
                return component0.getPreferredSize();
            }
            Insets insets = parent.getInsets();
            Dimension d0 = component0.getPreferredSize();
            Dimension d1 = component1.getPreferredSize();
            return new Dimension(insets.left + Math.max(d0.width, d1.width) + insets.right,
                insets.top + d0.height + d1.height + insets.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            if (fraction <= 0.0) {
                return component1.getMinimumSize();
            }
            if (fraction >= 1.0) {
                return component0.getMinimumSize();
            }
            Insets insets = parent.getInsets();
            Dimension d0 = component0.getMinimumSize();
            Dimension d1 = component1.getMinimumSize();
            return new Dimension(insets.left + Math.max(d0.width, d1.width) + insets.right,
                insets.top + d0.height + d1.height + insets.bottom);
        }

        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int maxWidth = parent.getWidth() - (insets.left + insets.right);
            int maxHeight = parent.getHeight() - (insets.top + insets.bottom);
            int split;
            if (fraction <= 0.0) {
                split = 0;
            } else if (fraction >= 1.0) {
                split = maxWidth;
            } else {
                split = (int) Math.round(maxHeight * fraction);
                split = Math.min(split, maxHeight - component1.getMinimumSize().height);
                split = Math.max(split, component0.getMinimumSize().height);
            }

            component0.setBounds(insets.left, insets.top, maxWidth, split);
            component1.setBounds(insets.left, insets.top + split, maxWidth, maxHeight - split);
            myDragbar.setBounds(insets.left, insets.top + split - DRAG_TOLERANCE, maxWidth, 2 * DRAG_TOLERANCE);
        }
    }

    private class MyDragbar extends Dragbar {

        MyDragbar() {
            setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
        }

        @Override
        int getDragValue(MouseEvent e) {
            return getY() + e.getY() - HorizontalSplitPane.this.getInsets().top;
        }

        @Override
        void setDragValue(int value) {
            Insets insets = HorizontalSplitPane.this.getInsets();
            setFraction((double) value / (HorizontalSplitPane.this.getHeight() - insets.bottom - insets.top));
            revalidate();
        }
    }
}

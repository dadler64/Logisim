/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPanel;

public class VerticalSplitPane extends JPanel {

    private final JComponent comp0;
    private final JComponent comp1;
    private final MyDragbar myDragbar;
    private double fraction;

    public VerticalSplitPane(JComponent comp0, JComponent comp1) {
        this(comp0, comp1, 0.5);
    }

    public VerticalSplitPane(JComponent comp0, JComponent comp1,
        double fraction) {
        this.comp0 = comp0;
        this.comp1 = comp1;
        this.myDragbar = new MyDragbar(); // above the other components
        this.fraction = fraction;

        setLayout(new MyLayout());
        add(myDragbar); // above the other components
        add(comp0);
        add(comp1);
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

    private class MyLayout implements LayoutManager {

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            if (fraction <= 0.0) {
                return comp1.getPreferredSize();
            }
            if (fraction >= 1.0) {
                return comp0.getPreferredSize();
            }
            Insets insets = parent.getInsets();
            Dimension d0 = comp0.getPreferredSize();
            Dimension d1 = comp1.getPreferredSize();
            return new Dimension(insets.left + d0.width + d1.width + insets.right, insets.top + Math.max(d0.height,
                d1.height) + insets.bottom);
        }

        public Dimension minimumLayoutSize(Container parent) {
            if (fraction <= 0.0) {
                return comp1.getMinimumSize();
            }
            if (fraction >= 1.0) {
                return comp0.getMinimumSize();
            }
            Insets insets = parent.getInsets();
            Dimension d0 = comp0.getMinimumSize();
            Dimension d1 = comp1.getMinimumSize();
            return new Dimension(insets.left + d0.width + d1.width + insets.right, insets.top + Math.max(d0.height,
                d1.height) + insets.bottom);
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
                split = (int) Math.round(maxWidth * fraction);
                split = Math.min(split, maxWidth - comp1.getMinimumSize().width);
                split = Math.max(split, comp0.getMinimumSize().width);
            }

            comp0.setBounds(insets.left, insets.top, split, maxHeight);
            comp1.setBounds(insets.left + split, insets.top, maxWidth - split, maxHeight);
            myDragbar.setBounds(insets.left + split - HorizontalSplitPane.DRAG_TOLERANCE, insets.top,
                2 * HorizontalSplitPane.DRAG_TOLERANCE, maxHeight);
        }
    }

    private class MyDragbar extends HorizontalSplitPane.Dragbar {

        MyDragbar() {
            setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
        }

        @Override
        int getDragValue(MouseEvent e) {
            return getX() + e.getX() - VerticalSplitPane.this.getInsets().left;
        }

        @Override
        void setDragValue(int value) {
            Insets insets = VerticalSplitPane.this.getInsets();
            setFraction((double) value / (VerticalSplitPane.this.getWidth() - insets.left - insets.right));
            revalidate();
        }
    }
}

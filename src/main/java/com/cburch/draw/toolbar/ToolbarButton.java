/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.toolbar;

import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JComponent;

class ToolbarButton extends JComponent implements MouseListener {

    private static final int BORDER = 2;

    private final Toolbar toolbar;
    private final ToolbarItem item;

    ToolbarButton(Toolbar toolbar, ToolbarItem item) {
        this.toolbar = toolbar;
        this.item = item;
        addMouseListener(this);
        setFocusable(true);
        setToolTipText("");
    }

    public ToolbarItem getItem() {
        return item;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dimension = item.getDimension(toolbar.getOrientation());
        dimension.width += 2 * BORDER;
        dimension.height += 2 * BORDER;
        return dimension;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        if (toolbar.getPressed() == this) {
            Dimension dimension = item.getDimension(toolbar.getOrientation());
            Color defaultColor = graphics.getColor();
            GraphicsUtil.switchToWidth(graphics, 2);
            graphics.setColor(Color.GRAY);
            graphics.fillRect(BORDER, BORDER, dimension.width, dimension.height);
            GraphicsUtil.switchToWidth(graphics, 1);
            graphics.setColor(defaultColor);
        }

        Graphics graphicsClone = graphics.create();
        graphicsClone.translate(BORDER, BORDER);
        item.paintIcon(ToolbarButton.this, graphicsClone);
        graphicsClone.dispose();

        // draw selection indicator
        if (toolbar.getToolbarModel().isSelected(item)) {
            Dimension dimension = item.getDimension(toolbar.getOrientation());
            GraphicsUtil.switchToWidth(graphics, 2);
            graphics.setColor(Color.BLACK);
            graphics.drawRect(BORDER, BORDER, dimension.width, dimension.height);
            GraphicsUtil.switchToWidth(graphics, 1);
        }
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        return item.getToolTip();
    }

    public void mousePressed(MouseEvent event) {
        if (item != null && item.isSelectable()) {
            toolbar.setPressed(this);
        }
    }

    public void mouseReleased(MouseEvent event) {
        if (toolbar.getPressed() == this) {
            toolbar.getToolbarModel().itemSelected(item);
            toolbar.setPressed(null);
        }
    }

    public void mouseClicked(MouseEvent event) {
    }

    public void mouseEntered(MouseEvent event) {
    }

    public void mouseExited(MouseEvent event) {
        toolbar.setPressed(null);
    }
}

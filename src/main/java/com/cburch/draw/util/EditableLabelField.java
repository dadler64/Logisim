/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.util;

import javax.swing.*;
import java.awt.*;

public class EditableLabelField extends JTextField {

    static final int FIELD_BORDER = 2;

    public EditableLabelField() {
        super(10);
        setBackground(new Color(255, 255, 255, 128));
        setOpaque(false);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.BLACK),
                BorderFactory.createEmptyBorder(1, 1, 1, 1)));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        graphics.setColor(getBackground());
        graphics.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(graphics);
    }

    public int viewToModel2D(Point fieldLocation) {
        return fieldLocation.x;
    }
}

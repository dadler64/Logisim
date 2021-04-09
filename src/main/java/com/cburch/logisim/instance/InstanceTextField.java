/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.TextField;
import com.cburch.logisim.comp.TextFieldEvent;
import com.cburch.logisim.comp.TextFieldListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.tools.TextEditable;
import java.awt.Font;
import java.awt.Graphics;

public class InstanceTextField implements AttributeListener, TextFieldListener,
    TextEditable {

    private final InstanceComponent component;
    private TextField field;
    private Attribute<String> labelAttribute;
    private Attribute<Font> fontAttribute;
    private int fieldX;
    private int fieldY;
    private int hAlign;
    private int vAlign;

    InstanceTextField(InstanceComponent component) {
        this.component = component;
        this.field = null;
        this.labelAttribute = null;
        this.fontAttribute = null;
    }

    void update(Attribute<String> labelAttribute, Attribute<Font> fontAttribute,
        int x, int y, int hAlign, int vAlign) {
        boolean wasReg = shouldRegister();
        this.labelAttribute = labelAttribute;
        this.fontAttribute = fontAttribute;
        this.fieldX = x;
        this.fieldY = y;
        this.hAlign = hAlign;
        this.vAlign = vAlign;
        boolean shouldReg = shouldRegister();
        AttributeSet attributeSet = component.getAttributeSet();
        if (!wasReg && shouldReg) {
            attributeSet.addAttributeListener(this);
        }
        if (wasReg && !shouldReg) {
            attributeSet.removeAttributeListener(this);
        }

        updateField(attributeSet);
    }

    private void updateField(AttributeSet attributeSet) {
        String text = attributeSet.getValue(labelAttribute);
        if (text == null || text.equals("")) {
            if (field != null) {
                field.removeTextFieldListener(this);
                field = null;
            }
        } else {
            if (field == null) {
                createField(attributeSet, text);
            } else {
                Font font = attributeSet.getValue(fontAttribute);
                if (font != null) {
                    field.setFont(font);
                }
                field.setLocation(fieldX, fieldY, hAlign, vAlign);
                field.setText(text);
            }
        }
    }

    private void createField(AttributeSet attributeSet, String text) {
        Font font = attributeSet.getValue(fontAttribute);
        field = new TextField(fieldX, fieldY, hAlign, vAlign, font);
        field.setText(text);
        field.addTextFieldListener(this);
    }

    private boolean shouldRegister() {
        return labelAttribute != null || fontAttribute != null;
    }

    Bounds getBounds(Graphics graphics) {
        return field == null ? Bounds.EMPTY_BOUNDS : field.getBounds(graphics);
    }

    void draw(ComponentDrawContext context) {
        if (field != null) {
            Graphics graphics = context.getGraphics().create();
            field.draw(graphics);
            graphics.dispose();
        }
    }

    public void attributeListChanged(AttributeEvent event) {
    }

    public void attributeValueChanged(AttributeEvent event) {
        Attribute<?> attribute = event.getAttribute();
        if (attribute == labelAttribute) {
            updateField(component.getAttributeSet());
        } else if (attribute == fontAttribute) {
            if (field != null) {
                field.setFont((Font) event.getValue());
            }
        }
    }

    public void textChanged(TextFieldEvent event) {
        String previous = event.getOldText();
        String next = event.getText();
        if (!next.equals(previous)) {
            component.getAttributeSet().setValue(labelAttribute, next);
        }
    }

    public Action getCommitAction(Circuit circuit, String oldText, String newText) {
        SetAttributeAction action = new SetAttributeAction(circuit, Strings.getter("changeLabelAction"));
        action.set(component, labelAttribute, newText);
        return action;
    }

    public Caret getTextCaret(ComponentUserEvent event) {
        Canvas canvas = event.getCanvas();
        Graphics graphics = canvas.getGraphics();

        // if field is absent, create it empty and if it is empty, just return a caret at its beginning
        if (field == null) {
            createField(component.getAttributeSet(), "");
        }
        String text = field.getText();
        if (text == null || text.equals("")) {
            return field.getCaret(graphics, 0);
        }

        Bounds bounds = field.getBounds(graphics);
        if (bounds.getWidth() < 4 || bounds.getHeight() < 4) {
            Location loc = component.getLocation();
            bounds = bounds.add(Bounds.create(loc).expand(2));
        }

        int x = event.getX();
        int y = event.getY();
        if (bounds.contains(x, y)) {
            return field.getCaret(graphics, x, y);
        } else {
            return null;
        }
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.draw.util.EditableLabel;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Text extends AbstractCanvasObject {

    private EditableLabel label;

    public Text(int x, int y, String text) {
        this(x, y, EditableLabel.LEFT, EditableLabel.BASELINE, text, DrawAttr.DEFAULT_FONT, Color.BLACK);
    }

    private Text(int x, int y, int horizontalAlignment, int verticalAlign, String text, Font font, Color color) {
        label = new EditableLabel(x, y, text, font);
        label.color = color;
        label.setHorizontalAlignment(horizontalAlignment);
        label.setVerticalAlignment(verticalAlign);
    }

    @Override
    public Text clone() {
        Text text = (Text) super.clone();
        text.label = this.label.clone();
        return text;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof Text) {
            Text text = (Text) object;
            return this.label.equals(text.label);
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        return label.hashCode();
    }

    @Override
    public Element toSvgElement(Document document) {
        return SvgCreator.createText(document, this);
    }

    public Location getLocation() {
        return Location.create(label.getX(), label.getY());
    }

    public String getText() {
        return label.getText();
    }

    public void setText(String value) {
        label.setText(value);
    }

    public EditableLabel getLabel() {
        return label;
    }

    @Override
    public String getDisplayName() {
        return Strings.get("shapeText");
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.ATTRS_TEXT;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attribute) {
        if (attribute == DrawAttr.FONT) {
            return (V) label.getFont();
        } else if (attribute == DrawAttr.FILL_COLOR) {
            return (V) label.color;
        } else if (attribute == DrawAttr.ALIGNMENT) {
            int horizontalAlignment = label.getHorizontalAlignment();
            AttributeOption option;
            if (horizontalAlignment == EditableLabel.LEFT) {
                option = DrawAttr.ALIGN_LEFT;
            } else if (horizontalAlignment == EditableLabel.RIGHT) {
                option = DrawAttr.ALIGN_RIGHT;
            } else {
                option = DrawAttr.ALIGN_CENTER;
            }
            return (V) option;
        } else {
            return null;
        }
    }

    @Override
    public void updateValue(Attribute<?> attribute, Object value) {
        if (attribute == DrawAttr.FONT) {
            label.setFont((Font) value);
        } else if (attribute == DrawAttr.FILL_COLOR) {
            label.color = (Color) value;
        } else if (attribute == DrawAttr.ALIGNMENT) {
            Integer intVal = (Integer) ((AttributeOption) value).getValue();
            label.setHorizontalAlignment(intVal);
        }
    }

    @Override
    public Bounds getBounds() {
        return label.getBounds();
    }

    @Override
    public boolean contains(Location location, boolean assumeFilled) {
        return label.contains(location.getX(), location.getY());
    }

    @Override
    public void translate(int deltaX, int deltaY) {
        label.setLocation(label.getX() + deltaX, label.getY() + deltaY);
    }

    public List<Handle> getHandles() {
        Bounds bounds = label.getBounds();
        int x = bounds.getX();
        int y = bounds.getY();
        int width = bounds.getWidth();
        int height = bounds.getHeight();
        return UnmodifiableList.create(new Handle[]{
                new Handle(this, x, y), new Handle(this, x + width, y),
                new Handle(this, x + width, y + height), new Handle(this, x, y + height)});
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        return getHandles();
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        label.paint(graphics);
    }
}

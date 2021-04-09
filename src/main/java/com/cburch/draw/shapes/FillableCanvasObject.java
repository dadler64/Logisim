/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import java.awt.Color;

abstract class FillableCanvasObject extends AbstractCanvasObject {

    private AttributeOption paintType;
    private int strokeWidth;
    private Color strokeColor;
    private Color fillColor;

    public FillableCanvasObject() {
        paintType = DrawAttr.PAINT_STROKE;
        strokeWidth = 1;
        strokeColor = Color.BLACK;
        fillColor = Color.WHITE;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof FillableCanvasObject) {
            FillableCanvasObject that = (FillableCanvasObject) object;
            boolean isSame = this.paintType == that.paintType;
            if (isSame && this.paintType != DrawAttr.PAINT_FILL) {
                isSame = isSame && this.strokeWidth == that.strokeWidth && this.strokeColor.equals(that.strokeColor);
            }
            if (isSame && this.paintType != DrawAttr.PAINT_STROKE) {
                isSame = isSame && this.fillColor.equals(that.fillColor);
            }
            return isSame;
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        int hashCode = paintType.hashCode();
        if (paintType != DrawAttr.PAINT_FILL) {
            hashCode = hashCode * 31 + strokeWidth;
            hashCode = hashCode * 31 + strokeColor.hashCode();
        } else {
            hashCode = hashCode * 31 * 31;
        }
        if (paintType != DrawAttr.PAINT_STROKE) {
            hashCode = hashCode * 31 + fillColor.hashCode();
        } else {
            hashCode = hashCode * 31;
        }
        return hashCode;
    }

    public AttributeOption getPaintType() {
        return paintType;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attribute) {
        if (attribute == DrawAttr.PAINT_TYPE) {
            return (V) paintType;
        } else if (attribute == DrawAttr.STROKE_COLOR) {
            return (V) strokeColor;
        } else if (attribute == DrawAttr.FILL_COLOR) {
            return (V) fillColor;
        } else if (attribute == DrawAttr.STROKE_WIDTH) {
            return (V) Integer.valueOf(strokeWidth);
        } else {
            return null;
        }
    }

    @Override
    public void updateValue(Attribute<?> attribute, Object object) {
        if (attribute == DrawAttr.PAINT_TYPE) {
            paintType = (AttributeOption) object;
            fireAttributeListChanged();
        } else if (attribute == DrawAttr.STROKE_COLOR) {
            strokeColor = (Color) object;
        } else if (attribute == DrawAttr.FILL_COLOR) {
            fillColor = (Color) object;
        } else if (attribute == DrawAttr.STROKE_WIDTH) {
            strokeWidth = (Integer) object;
        }
    }
}

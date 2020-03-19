/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.logisim.data.Location;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class SvgCreator {

    private SvgCreator() {
    }

    public static Element createRectangle(Document document, Rectangle rectangle) {
        return createRectangular(document, rectangle);
    }

    public static Element createRoundRectangle(Document document, RoundRectangle roundRectangle) {
        Element element = createRectangular(document, roundRectangle);
        int cornerRadiusValue = roundRectangle.getValue(DrawAttr.CORNER_RADIUS);
        element.setAttribute("rx", "" + cornerRadiusValue);
        element.setAttribute("ry", "" + cornerRadiusValue);
        return element;
    }

    private static Element createRectangular(Document document, Rectangular rectangular) {
        Element element = document.createElement("rect");
        element.setAttribute("x", "" + rectangular.getX());
        element.setAttribute("y", "" + rectangular.getY());
        element.setAttribute("width", "" + rectangular.getWidth());
        element.setAttribute("height", "" + rectangular.getHeight());
        populateFill(element, rectangular);
        return element;
    }

    public static Element createOval(Document document, Oval oval) {
        double x = oval.getX();
        double y = oval.getY();
        double width = oval.getWidth();
        double height = oval.getHeight();
        Element element = document.createElement("ellipse");
        element.setAttribute("cx", "" + (x + width / 2));
        element.setAttribute("cy", "" + (y + height / 2));
        element.setAttribute("rx", "" + (width / 2));
        element.setAttribute("ry", "" + (height / 2));
        populateFill(element, oval);
        return element;
    }

    public static Element createLine(Document document, Line line) {
        Element element = document.createElement("line");
        Location v1 = line.getEnd0();
        Location v2 = line.getEnd1();
        element.setAttribute("x1", "" + v1.getX());
        element.setAttribute("y1", "" + v1.getY());
        element.setAttribute("x2", "" + v2.getX());
        element.setAttribute("y2", "" + v2.getY());
        populateStroke(element, line);
        return element;
    }

    public static Element createCurve(Document document, Curve curve) {
        Element element = document.createElement("path");
        Location end0 = curve.getEnd0();
        Location end1 = curve.getEnd1();
        Location control = curve.getControl();
        element.setAttribute("d", "M" + end0.getX() + "," + end0.getY()
                + " Q" + control.getX() + "," + control.getY()
                + " " + end1.getX() + "," + end1.getY());
        populateFill(element, curve);
        return element;
    }

    public static Element createPoly(Document document, Poly poly) {
        Element element;
        if (poly.isClosed()) {
            element = document.createElement("polygon");
        } else {
            element = document.createElement("polyline");
        }

        StringBuilder points = new StringBuilder();
        boolean first = true;
        for (Handle handle : poly.getHandles(null)) {
            if (!first) {
                points.append(" ");
            }
            points.append(handle.getX());
            points.append(",");
            points.append(handle.getY());
            first = false;
        }
        element.setAttribute("points", points.toString());

        populateFill(element, poly);
        return element;
    }

    public static Element createText(Document document, Text text) {
        Element element = document.createElement("text");
        Location location = text.getLocation();
        Font font = text.getValue(DrawAttr.FONT);
        Color fill = text.getValue(DrawAttr.FILL_COLOR);
        Object horizontalAlignment = text.getValue(DrawAttr.ALIGNMENT);
        element.setAttribute("x", "" + location.getX());
        element.setAttribute("y", "" + location.getY());
        if (!colorMatches(fill, Color.BLACK)) {
            element.setAttribute("fill", getColorString(fill));
        }
        if (showOpacity(fill)) {
            element.setAttribute("fill-opacity", getOpacityString(fill));
        }
        element.setAttribute("font-family", font.getFamily());
        element.setAttribute("font-size", "" + font.getSize());
        int style = font.getStyle();
        if ((style & Font.ITALIC) != 0) {
            element.setAttribute("font-style", "italic");
        }
        if ((style & Font.BOLD) != 0) {
            element.setAttribute("font-weight", "bold");
        }
        if (horizontalAlignment == DrawAttr.ALIGN_LEFT) {
            element.setAttribute("text-anchor", "start");
        } else if (horizontalAlignment == DrawAttr.ALIGN_RIGHT) {
            element.setAttribute("text-anchor", "end");
        } else {
            element.setAttribute("text-anchor", "middle");
        }
        element.appendChild(document.createTextNode(text.getText()));
        return element;
    }

    private static void populateFill(Element element, AbstractCanvasObject shape) {
        Object type = shape.getValue(DrawAttr.PAINT_TYPE);
        if (type == DrawAttr.PAINT_FILL) {
            element.setAttribute("stroke", "none");
        } else {
            populateStroke(element, shape);
        }
        if (type == DrawAttr.PAINT_STROKE) {
            element.setAttribute("fill", "none");
        } else {
            Color fill = shape.getValue(DrawAttr.FILL_COLOR);
            if (colorMatches(fill, Color.BLACK)) {
                element.removeAttribute("fill");
            } else {
                element.setAttribute("fill", getColorString(fill));
            }
            if (showOpacity(fill)) {
                element.setAttribute("fill-opacity", getOpacityString(fill));
            }
        }
    }

    private static void populateStroke(Element element, AbstractCanvasObject shape) {
        Integer width = shape.getValue(DrawAttr.STROKE_WIDTH);
        if (width != null && width != 1) {
            element.setAttribute("stroke-width", width.toString());
        }
        Color stroke = shape.getValue(DrawAttr.STROKE_COLOR);
        element.setAttribute("stroke", getColorString(stroke));
        if (showOpacity(stroke)) {
            element.setAttribute("stroke-opacity", getOpacityString(stroke));
        }
        element.setAttribute("fill", "none");
    }

    private static boolean colorMatches(Color a, Color b) {
        return a.getRed() == b.getRed() && a.getGreen() == b.getGreen() && a.getBlue() == b.getBlue();
    }

    private static String getColorString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static boolean showOpacity(Color color) {
        return color.getAlpha() != 255;
    }

    private static String getOpacityString(Color color) {
        double alpha = color.getAlpha() / 255.0;
        return String.format(Locale.US, "%5.3f", alpha);
    }
}

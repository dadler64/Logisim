/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.w3c.dom.Element;

public class SvgReader {

    private static final Pattern PATH_REGEX = Pattern.compile("[a-zA-Z]|[-0-9.]+");

    private SvgReader() {
    }

    public static AbstractCanvasObject createShape(Element element) {
        String tagName = element.getTagName();
        AbstractCanvasObject canvasObject;
        switch (tagName) {
            case "ellipse":
                canvasObject = createOval(element);
                break;
            case "line":
                canvasObject = createLine(element);
                break;
            case "path":
                canvasObject = createPath(element);
                break;
            case "polyline":
                canvasObject = createPolyline(element);
                break;
            case "polygon":
                canvasObject = createPolygon(element);
                break;
            case "rect":
                canvasObject = createRectangle(element);
                break;
            case "text":
                canvasObject = createText(element);
                break;
            default:
                return null;
        }
        List<Attribute<?>> attributes = canvasObject.getAttributes();
        if (attributes.contains(DrawAttr.PAINT_TYPE)) {
            String stroke = element.getAttribute("stroke");
            String fill = element.getAttribute("fill");
            if (stroke.equals("") || stroke.equals("none")) {
                canvasObject.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_FILL);
            } else if (fill.equals("none")) {
                canvasObject.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE);
            } else {
                canvasObject.setValue(DrawAttr.PAINT_TYPE, DrawAttr.PAINT_STROKE_FILL);
            }
        }
        attributes = canvasObject.getAttributes(); // since changing paintType could change it
        if (attributes.contains(DrawAttr.STROKE_WIDTH) && element.hasAttribute("stroke-width")) {
            Integer width = Integer.valueOf(element.getAttribute("stroke-width"));
            canvasObject.setValue(DrawAttr.STROKE_WIDTH, width);
        }
        if (attributes.contains(DrawAttr.STROKE_COLOR)) {
            String color = element.getAttribute("stroke");
            String opacity = element.getAttribute("stroke-opacity");
            if (!color.equals("none")) {
                canvasObject.setValue(DrawAttr.STROKE_COLOR, getColor(color, opacity));
            }
        }
        if (attributes.contains(DrawAttr.FILL_COLOR)) {
            String color = element.getAttribute("fill");
            if (color.equals("")) {
                color = "#000000";
            }
            String opacity = element.getAttribute("fill-opacity");
            if (!color.equals("none")) {
                canvasObject.setValue(DrawAttr.FILL_COLOR, getColor(color, opacity));
            }
        }
        return canvasObject;
    }

    private static AbstractCanvasObject createRectangle(Element element) {
        int x = Integer.parseInt(element.getAttribute("x"));
        int y = Integer.parseInt(element.getAttribute("y"));
        int width = Integer.parseInt(element.getAttribute("width"));
        int height = Integer.parseInt(element.getAttribute("height"));
        if (element.hasAttribute("rx")) {
            AbstractCanvasObject canvasObject = new RoundRectangle(x, y, width, height);
            int radiusX = Integer.parseInt(element.getAttribute("rx"));
            canvasObject.setValue(DrawAttr.CORNER_RADIUS, radiusX);
            return canvasObject;
        } else {
            return new Rectangle(x, y, width, height);
        }
    }

    private static AbstractCanvasObject createOval(Element element) {
        double cx = Double.parseDouble(element.getAttribute("cx"));
        double cy = Double.parseDouble(element.getAttribute("cy"));
        double radiusX = Double.parseDouble(element.getAttribute("rx"));
        double radiusY = Double.parseDouble(element.getAttribute("ry"));
        int x = (int) Math.round(cx - radiusX);
        int y = (int) Math.round(cy - radiusY);
        int width = (int) Math.round(radiusX * 2);
        int height = (int) Math.round(radiusY * 2);
        return new Oval(x, y, width, height);
    }

    private static AbstractCanvasObject createLine(Element element) {
        int x0 = Integer.parseInt(element.getAttribute("x1"));
        int y0 = Integer.parseInt(element.getAttribute("y1"));
        int x1 = Integer.parseInt(element.getAttribute("x2"));
        int y1 = Integer.parseInt(element.getAttribute("y2"));
        return new Line(x0, y0, x1, y1);
    }

    private static AbstractCanvasObject createPolygon(Element element) {
        return new Poly(true, parsePoints(element.getAttribute("points")));
    }

    private static AbstractCanvasObject createPolyline(Element element) {
        return new Poly(false, parsePoints(element.getAttribute("points")));
    }

    private static AbstractCanvasObject createText(Element element) {
        int x = Integer.parseInt(element.getAttribute("x"));
        int y = Integer.parseInt(element.getAttribute("y"));
        String textContent = element.getTextContent();
        Text text = new Text(x, y, textContent);

        String fontFamily = element.getAttribute("font-family");
        String fontStyle = element.getAttribute("font-style");
        String fontWeight = element.getAttribute("font-weight");
        String fontSize = element.getAttribute("font-size");
        int styleFlags = Font.PLAIN;
        if (fontStyle.equals("italic")) {
            styleFlags |= Font.ITALIC;
        }
        if (fontWeight.equals("bold")) {
            styleFlags |= Font.BOLD;
        }
        int size = Integer.parseInt(fontSize);
        text.setValue(DrawAttr.FONT, new Font(fontFamily, styleFlags, size));

        String alignString = element.getAttribute("text-anchor");
        AttributeOption horizontalAlignment;
        if (alignString.equals("start")) {
            horizontalAlignment = DrawAttr.ALIGN_LEFT;
        } else if (alignString.equals("end")) {
            horizontalAlignment = DrawAttr.ALIGN_RIGHT;
        } else {
            horizontalAlignment = DrawAttr.ALIGN_CENTER;
        }
        text.setValue(DrawAttr.ALIGNMENT, horizontalAlignment);

        // fill color is handled after we return
        return text;
    }

    private static List<Location> parsePoints(String points) {
        Pattern pattern = Pattern.compile("[ ,\n\r\t]+");
        String[] tokens = pattern.split(points);
        Location[] locations = new Location[tokens.length / 2];
        for (int i = 0; i < locations.length; i++) {
            int x = Integer.parseInt(tokens[2 * i]);
            int y = Integer.parseInt(tokens[2 * i + 1]);
            locations[i] = Location.create(x, y);
        }
        return UnmodifiableList.create(locations);
    }

    private static AbstractCanvasObject createPath(Element element) {
        Matcher matcher = PATH_REGEX.matcher(element.getAttribute("d"));
        List<String> tokens = new ArrayList<>();
        int type = -1; // -1 error, 0 start, 1 curve, 2 polyline
        while (matcher.find()) {
            String token = matcher.group();
            tokens.add(token);
            if (Character.isLetter(token.charAt(0))) {
                switch (Character.toLowerCase(token.charAt(0))) {
                    case 'm':
                        if (type == -1) {
                            type = 0;
                        } else {
                            type = -1;
                        }
                        break;
                    case 'q':
                        if (type == 0) {
                            type = 1;
                        } else {
                            type = -1;
                        }
                        break;
                        /* not supported
				        case 'l':
                        case 'h':
				        case 'v':
					if (type == 0 || type == 2) type = 2;
					else type = -1;
					break;
				*/
                    default:
                        type = -1;
                }
                if (type == -1) {
                    throw new NumberFormatException("Unrecognized path command '" + token.charAt(0) + "'");
                }
            }
        }

        if (type == 1) {
            if (tokens.size() == 8 && tokens.get(0).equals("M")
                && tokens.get(3).equalsIgnoreCase("Q")) {
                int x0 = Integer.parseInt(tokens.get(1));
                int y0 = Integer.parseInt(tokens.get(2));
                int x1 = Integer.parseInt(tokens.get(4));
                int y1 = Integer.parseInt(tokens.get(5));
                int x2 = Integer.parseInt(tokens.get(6));
                int y2 = Integer.parseInt(tokens.get(7));
                if (tokens.get(3).equals("q")) {
                    x1 += x0;
                    y1 += y0;
                    x2 += x0;
                    y2 += y0;
                }
                Location end0 = Location.create(x0, y0);
                Location end1 = Location.create(x2, y2);
                Location count = Location.create(x1, y1);
                return new Curve(end0, end1, count);
            } else {
                throw new NumberFormatException("Unexpected format for curve");
            }
        } else {
            throw new NumberFormatException("Unrecognized path");
        }
    }

    private static Color getColor(String hue, String opacity) {
        int red;
        int green;
        int blue;
        if (hue == null || hue.equals("")) {
            red = 0;
            green = 0;
            blue = 0;
        } else {
            red = Integer.parseInt(hue.substring(1, 3), 16);
            green = Integer.parseInt(hue.substring(3, 5), 16);
            blue = Integer.parseInt(hue.substring(5, 7), 16);
        }
        int alpha;
        if (opacity == null || opacity.equals("")) {
            alpha = 255;
        } else {
            double x;
            try {
                x = Double.parseDouble(opacity);
            } catch (NumberFormatException e) {
                // some localizations use commas for decimal points
                int comma = opacity.lastIndexOf(',');
                if (comma >= 0) {
                    try {
                        String repl = opacity.substring(0, comma) + "." + opacity.substring(comma + 1);
                        x = Double.parseDouble(repl);
                    } catch (Throwable t) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
            alpha = (int) Math.round(x * 255);
        }
        return new Color(red, green, blue, alpha);
    }
}

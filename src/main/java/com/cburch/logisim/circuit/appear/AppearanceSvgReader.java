/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.shapes.SvgReader;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import java.util.Map;
import org.w3c.dom.Element;

public class AppearanceSvgReader {

    public static AbstractCanvasObject createShape(Element element, Map<Location, Instance> pins) {
        String name = element.getTagName();
        if (name.equals("circ-anchor") || name.equals("circ-origin")) {
            Location location = getLocation(element);
            AbstractCanvasObject canvasObject = new AppearanceAnchor(location);
            if (element.hasAttribute("facing")) {
                Direction facing = Direction.parse(element.getAttribute("facing"));
                canvasObject.setValue(AppearanceAnchor.FACING, facing);
            }
            return canvasObject;
        } else if (name.equals("circ-port")) {
            Location location = getLocation(element);
            String[] pinString = element.getAttribute("pin").split(",");
            Location pinLocation = Location
                .create(Integer.parseInt(pinString[0].trim()), Integer.parseInt(pinString[1].trim()));
            Instance pin = pins.get(pinLocation);
            if (pin == null) {
                return null;
            } else {
                return new AppearancePort(location, pin);
            }
        } else {
            return SvgReader.createShape(element);
        }
    }

    private static Location getLocation(Element elt) {
        double x = Double.parseDouble(elt.getAttribute("x"));
        double y = Double.parseDouble(elt.getAttribute("y"));
        double w = Double.parseDouble(elt.getAttribute("width"));
        double h = Double.parseDouble(elt.getAttribute("height"));
        int px = (int) Math.round(x + w / 2);
        int py = (int) Math.round(y + h / 2);
        return Location.create(px, py);
    }
}

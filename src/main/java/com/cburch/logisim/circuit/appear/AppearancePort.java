/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class AppearancePort extends AppearanceElement {

    public static final Color COLOR = Color.BLUE;
    private static final int INPUT_RADIUS = 4;
    private static final int OUTPUT_RADIUS = 5;
    private static final int MINOR_RADIUS = 2;
    private Instance pin;

    public AppearancePort(Location location, Instance pin) {
        super(location);
        this.pin = pin;
    }

    @Override
    public boolean matches(CanvasObject object) {
        if (object instanceof AppearancePort) {
            AppearancePort that = (AppearancePort) object;
            return this.matches(that) && this.pin == that.pin;
        } else {
            return false;
        }
    }

    @Override
    public int matchesHashCode() {
        return super.matchesHashCode() + pin.hashCode();
    }

    @Override
    public String getDisplayName() {
        return Strings.get("circuitPort");
    }

    @Override
    public Element toSvgElement(Document document) {
        Location loc = getLocation();
        Location pinLoc = pin.getLocation();
        Element ret = document.createElement("circ-port");
        int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
        ret.setAttribute("x", "" + (loc.getX() - r));
        ret.setAttribute("y", "" + (loc.getY() - r));
        ret.setAttribute("width", "" + 2 * r);
        ret.setAttribute("height", "" + 2 * r);
        ret.setAttribute("pin", "" + pinLoc.getX() + "," + pinLoc.getY());
        return ret;
    }

    public Instance getPin() {
        return pin;
    }

    void setPin(Instance value) {
        pin = value;
    }

    private boolean isInput() {
        Instance p = pin;
        return p == null || Pin.FACTORY.isInputPin(p);
    }

    @Override
    public Bounds getBounds() {
        int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
        return super.getBounds(r);
    }

    @Override
    public boolean contains(Location location, boolean assumeFilled) {
        if (isInput()) {
            return getBounds().contains(location);
        } else {
            return super.isInCircle(location, OUTPUT_RADIUS);
        }
    }

    @Override
    public List<Handle> getHandles(HandleGesture gesture) {
        Location loc = getLocation();

        int r = isInput() ? INPUT_RADIUS : OUTPUT_RADIUS;
        return UnmodifiableList.create(new Handle[]{
            new Handle(this, loc.translate(-r, -r)),
            new Handle(this, loc.translate(r, -r)),
            new Handle(this, loc.translate(r, r)),
            new Handle(this, loc.translate(-r, r))});
    }

    @Override
    public void paint(Graphics graphics, HandleGesture gesture) {
        Location location = getLocation();
        int x = location.getX();
        int y = location.getY();
        graphics.setColor(COLOR);
        if (isInput()) {
            int r = INPUT_RADIUS;
            graphics.drawRect(x - r, y - r, 2 * r, 2 * r);
        } else {
            int r = OUTPUT_RADIUS;
            graphics.drawOval(x - r, y - r, 2 * r, 2 * r);
        }
        graphics.fillOval(x - MINOR_RADIUS, y - MINOR_RADIUS, 2 * MINOR_RADIUS, 2 * MINOR_RADIUS);
    }
}

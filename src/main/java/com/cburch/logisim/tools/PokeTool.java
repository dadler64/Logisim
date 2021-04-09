/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.Icons;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.Icon;

public class PokeTool extends Tool {

    private static final Icon toolIcon = Icons.getIcon("poke.gif");
    private static final Color caretColor = new Color(255, 255, 150);
    private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
    private final Listener listener;
    private Circuit pokedCircuit;
    private Component pokedComponent;
    private Caret pokeCaret;

    public PokeTool() {
        this.listener = new Listener();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PokeTool;
    }

    @Override
    public int hashCode() {
        return PokeTool.class.hashCode();
    }

    @Override
    public String getName() {
        return "Poke Tool";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("pokeTool");
    }

    private void removeCaret(boolean normal) {
        Circuit circuit = pokedCircuit;
        Caret caret = pokeCaret;
        if (caret != null) {
            if (normal) {
                caret.stopEditing();
            } else {
                caret.cancelEditing();
            }
            circuit.removeCircuitListener(listener);
            pokedCircuit = null;
            pokedComponent = null;
            pokeCaret = null;
        }
    }

    private void setPokedComponent(Circuit circuit, Component component, Caret caret) {
        removeCaret(true);
        pokedCircuit = circuit;
        pokedComponent = component;
        pokeCaret = caret;
        if (caret != null) {
            circuit.addCircuitListener(listener);
        }
    }

    @Override
    public String getDescription() {
        return Strings.get("pokeToolDesc");
    }

    @Override
    public void draw(Canvas canvas, ComponentDrawContext context) {
        if (pokeCaret != null) {
            pokeCaret.draw(context.getGraphics());
        }
    }

    @Override
    public void deselect(Canvas canvas) {
        removeCaret(true);
        canvas.setHighlightedWires(WireSet.EMPTY);
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        Location location = Location.create(x, y);
        boolean isDirty = false;
        canvas.setHighlightedWires(WireSet.EMPTY);
        if (pokeCaret != null && !pokeCaret.getBounds(g).contains(location)) {
            isDirty = true;
            removeCaret(true);
        }
        if (pokeCaret == null) {
            ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);
            Circuit circuit = canvas.getCircuit();
            for (Component component : circuit.getAllContaining(location, g)) {
                if (pokeCaret != null) {
                    break;
                }

                if (component instanceof Wire) {
                    Caret caret = new WireCaret(canvas, (Wire) component, x, y,
                        canvas.getProject().getOptions().getAttributeSet());
                    setPokedComponent(circuit, component, caret);
                    canvas.setHighlightedWires(circuit.getWireSet((Wire) component));
                } else {
                    Pokable pokable = (Pokable) component.getFeature(Pokable.class);
                    if (pokable != null) {
                        Caret caret = pokable.getPokeCaret(event);
                        setPokedComponent(circuit, component, caret);
                        AttributeSet attrs = component.getAttributeSet();
                        if (attrs != null && attrs.getAttributes().size() > 0) {
                            Project project = canvas.getProject();
                            project.getFrame().viewComponentAttributes(circuit, component);
                        }
                    }
                }
            }
        }
        if (pokeCaret != null) {
            isDirty = true;
            pokeCaret.mousePressed(e);
        }
        if (isDirty) {
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        if (pokeCaret != null) {
            pokeCaret.mouseDragged(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        if (pokeCaret != null) {
            pokeCaret.mouseReleased(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent e) {
        if (pokeCaret != null) {
            pokeCaret.keyTyped(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent e) {
        if (pokeCaret != null) {
            pokeCaret.keyPressed(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent e) {
        if (pokeCaret != null) {
            pokeCaret.keyReleased(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Graphics g = c.getGraphics();
        if (toolIcon != null) {
            toolIcon.paintIcon(c.getDestination(), g, x + 2, y + 2);
        } else {
            g.setColor(java.awt.Color.black);
            g.drawLine(x + 4, y + 2, x + 4, y + 17);
            g.drawLine(x + 4, y + 17, x + 1, y + 11);
            g.drawLine(x + 4, y + 17, x + 7, y + 11);

            g.drawLine(x + 15, y + 2, x + 15, y + 17);
            g.drawLine(x + 15, y + 2, x + 12, y + 8);
            g.drawLine(x + 15, y + 2, x + 18, y + 8);
        }
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    private static class WireCaret extends AbstractCaret {

        AttributeSet options;
        Canvas canvas;
        Wire wire;
        int x;
        int y;

        WireCaret(Canvas canvas, Wire wire, int x, int y, AttributeSet options) {
            this.canvas = canvas;
            this.wire = wire;
            this.x = x;
            this.y = y;
            this.options = options;
        }

        @Override
        public void draw(Graphics g) {
            Value value = canvas.getCircuitState().getValue(wire.getEnd0());
            RadixOption radix1 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX1.get());
            RadixOption radix2 = RadixOption.decode(AppPreferences.POKE_WIRE_RADIX2.get());
            if (radix1 == null) {
                radix1 = RadixOption.RADIX_2;
            }
            String valueString = radix1.toString(value);
            if (radix2 != null && value.getWidth() > 1) {
                valueString += " / " + radix2.toString(value);
            }

            FontMetrics fm = g.getFontMetrics();
            g.setColor(caretColor);
            g.fillRect(x + 2, y + 2, fm.stringWidth(valueString) + 4, fm.getAscent() + fm.getDescent() + 4);
            g.setColor(Color.BLACK);
            g.drawRect(x + 2, y + 2, fm.stringWidth(valueString) + 4, fm.getAscent() + fm.getDescent() + 4);
            g.fillOval(x - 2, y - 2, 5, 5);
            g.drawString(valueString, x + 4, y + 4 + fm.getAscent());
        }
    }

    private class Listener implements CircuitListener {

        public void circuitChanged(CircuitEvent event) {
            Circuit circuit = pokedCircuit;
            if (event.getCircuit() == circuit && circuit != null && (event.getAction() == CircuitEvent.ACTION_REMOVE
                || event.getAction() == CircuitEvent.ACTION_CLEAR) && !circuit.contains(pokedComponent)) {
                removeCaret(false);
            }
        }
    }
}


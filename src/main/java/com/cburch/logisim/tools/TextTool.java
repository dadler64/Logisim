/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.base.Text;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class TextTool extends Tool {

    private static final Cursor cursor = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private final MyListener listener = new MyListener();
    private final AttributeSet attrs;
    private Caret caret = null;
    private boolean caretCreatingText = false;
    private Canvas caretCanvas = null;
    private Circuit caretCircuit = null;
    private Component caretComponent = null;

    public TextTool() {
        attrs = Text.FACTORY.createAttributeSet();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TextTool;
    }

    @Override
    public int hashCode() {
        return TextTool.class.hashCode();
    }

    @Override
    public String getName() {
        return "Text Tool";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("textTool");
    }

    @Override
    public String getDescription() {
        return Strings.get("textToolDesc");
    }

    @Override
    public AttributeSet getAttributeSet() {
        return attrs;
    }

    @Override
    public void paintIcon(ComponentDrawContext c, int x, int y) {
        Text.FACTORY.paintIcon(c, x, y, null);
    }

    @Override
    public void draw(Canvas canvas, ComponentDrawContext context) {
        if (caret != null) {
            caret.draw(context.getGraphics());
        }
    }

    @Override
    public void deselect(Canvas canvas) {
        if (caret != null) {
            caret.stopEditing();
            caret = null;
        }
    }

    @Override
    public void mousePressed(Canvas canvas, Graphics g, MouseEvent e) {
        Project project = canvas.getProject();
        Circuit circuit = canvas.getCircuit();

        if (!project.getLogisimFile().contains(circuit)) {
            if (caret != null) {
                caret.cancelEditing();
            }
            canvas.setErrorMessage(Strings.getter("cannotModifyError"));
            return;
        }

        // Maybe user is clicking within the current caret.
        if (caret != null) {
            if (caret.getBounds(g).contains(e.getX(), e.getY())) { // Yes
                caret.mousePressed(e);
                project.repaintCanvas();
                return;
            } else { // No. End the current caret.
                caret.stopEditing();
            }
        }
        // caret will be null at this point

        // Otherwise search for a new caret.
        int x = e.getX();
        int y = e.getY();
        Location location = Location.create(x, y);
        ComponentUserEvent event = new ComponentUserEvent(canvas, x, y);

        // First search in selection.
        for (Component component : project.getSelection().getComponentsContaining(location, g)) {
            TextEditable editable = (TextEditable) component.getFeature(TextEditable.class);
            if (editable != null) {
                caret = editable.getTextCaret(event);
                if (caret != null) {
                    project.getFrame().viewComponentAttributes(circuit, component);
                    caretComponent = component;
                    caretCreatingText = false;
                    break;
                }
            }
        }

        // Then search in circuit
        if (caret == null) {
            for (Component component : circuit.getAllContaining(location, g)) {
                TextEditable editable = (TextEditable) component.getFeature(TextEditable.class);
                if (editable != null) {
                    caret = editable.getTextCaret(event);
                    if (caret != null) {
                        project.getFrame().viewComponentAttributes(circuit, component);
                        caretComponent = component;
                        caretCreatingText = false;
                        break;
                    }
                }
            }
        }

        // if nothing found, create a new label
        if (caret == null) {
            if (location.getX() < 0 || location.getY() < 0) {
                return;
            }
            AttributeSet copy = (AttributeSet) attrs.clone();
            caretComponent = Text.FACTORY.createComponent(location, copy);
            caretCreatingText = true;
            TextEditable editable = (TextEditable) caretComponent.getFeature(TextEditable.class);
            if (editable != null) {
                caret = editable.getTextCaret(event);
                project.getFrame().viewComponentAttributes(circuit, caretComponent);
            }
        }

        if (caret != null) {
            caretCanvas = canvas;
            caretCircuit = canvas.getCircuit();
            caret.addCaretListener(listener);
            caretCircuit.addCircuitListener(listener);
        }
        project.repaintCanvas();
    }

    @Override
    public void mouseDragged(Canvas canvas, Graphics g, MouseEvent e) {
        //TODO: enhance label editing
    }

    @Override
    public void mouseReleased(Canvas canvas, Graphics g, MouseEvent e) {
        //TODO: enhance label editing
    }

    @Override
    public void keyPressed(Canvas canvas, KeyEvent e) {
        if (caret != null) {
            caret.keyPressed(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void keyReleased(Canvas canvas, KeyEvent e) {
        if (caret != null) {
            caret.keyReleased(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public void keyTyped(Canvas canvas, KeyEvent e) {
        if (caret != null) {
            caret.keyTyped(e);
            canvas.getProject().repaintCanvas();
        }
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    private class MyListener
        implements CaretListener, CircuitListener {

        public void editingCanceled(CaretEvent e) {
            if (e.getCaret() != caret) {
                e.getCaret().removeCaretListener(this);
                return;
            }
            caret.removeCaretListener(this);
            caretCircuit.removeCircuitListener(this);

            caretCircuit = null;
            caretComponent = null;
            caretCreatingText = false;
            caret = null;
        }

        public void editingStopped(CaretEvent e) {
            if (e.getCaret() != caret) {
                e.getCaret().removeCaretListener(this);
                return;
            }
            caret.removeCaretListener(this);
            caretCircuit.removeCircuitListener(this);

            String text = caret.getText();
            boolean isEmpty = (text == null || text.equals(""));
            Action action;
            Project project = caretCanvas.getProject();
            if (caretCreatingText) {
                if (!isEmpty) {
                    CircuitMutation mutation = new CircuitMutation(caretCircuit);
                    mutation.add(caretComponent);
                    action = mutation.toAction(Strings.getter("addComponentAction", Text.FACTORY.getDisplayGetter()));
                } else {
                    action = null; // don't add the blank text field
                }
            } else {
                if (isEmpty && caretComponent.getFactory() instanceof Text) {
                    CircuitMutation mutation = new CircuitMutation(caretCircuit);
                    mutation.add(caretComponent);
                    action = mutation.toAction(Strings.getter("removeComponentAction", Text.FACTORY.getDisplayGetter()));
                } else {
                    Object object = caretComponent.getFeature(TextEditable.class);
                    if (object == null) { // should never happen
                        action = null;
                    } else {
                        TextEditable editable = (TextEditable) object;
                        action = editable.getCommitAction(caretCircuit, e.getOldText(), e.getText());
                    }
                }
            }

            caretCircuit = null;
            caretComponent = null;
            caretCreatingText = false;
            caret = null;

            if (action != null) {
                project.doAction(action);
            }
        }

        public void circuitChanged(CircuitEvent event) {
            if (event.getCircuit() != caretCircuit) {
                event.getCircuit().removeCircuitListener(this);
                return;
            }
            int action = event.getAction();
            if (action == CircuitEvent.ACTION_REMOVE) {
                if (event.getData() == caretComponent) {
                    caret.cancelEditing();
                }
            } else if (action == CircuitEvent.ACTION_CLEAR) {
                if (caretComponent != null) {
                    caret.cancelEditing();
                }
            }
        }
    }
}


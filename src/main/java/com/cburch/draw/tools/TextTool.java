/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.tools;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.actions.ModelEditTextAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Text;
import com.cburch.draw.util.EditableLabelField;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.Icons;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

public class TextTool extends AbstractTool {

    private DrawingAttributeSet attributeSet;
    private EditableLabelField field;
    private FieldListener fieldListener;
    private Text currentText;
    private Canvas currentCanvas;
    private boolean isTextNew;

    public TextTool(DrawingAttributeSet attributeSet) {
        this.attributeSet = attributeSet;
        currentText = null;
        isTextNew = false;
        field = new EditableLabelField();

        fieldListener = new FieldListener();
        InputMap fieldInput = field.getInputMap();
        fieldInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "commit");
        fieldInput.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "cancel");
        ActionMap fieldAction = field.getActionMap();
        fieldAction.put("commit", fieldListener);
        fieldAction.put("cancel", new CancelListener());
    }

    @Override
    public Icon getIcon() {
        return Icons.getIcon("text.gif");
    }

    @Override
    public Cursor getCursor(Canvas canvas) {
        return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return DrawAttr.ATTRS_TEXT_TOOL;
    }

    @Override
    public void toolSelected(Canvas canvas) {
        cancelText(canvas);
    }

    @Override
    public void toolDeselected(Canvas canvas) {
        commitText(canvas);
    }

    @Override
    public void mousePressed(Canvas canvas, MouseEvent event) {
        if (currentText != null) {
            commitText(canvas);
        }

        Text clickedText = null;
        boolean isFound = false;
        int mouseX = event.getX();
        int mouseY = event.getY();
        Location mouseLocation = Location.create(mouseX, mouseY);
        for (CanvasObject object : canvas.getModel().getObjectsFromTop()) {
            if (object instanceof Text && object.contains(mouseLocation, true)) {
                clickedText = (Text) object;
                isFound = true;
                break;
            }
        }
        if (!isFound) {
            clickedText = attributeSet.applyTo(new Text(mouseX, mouseY, ""));
        }

        currentText = clickedText;
        currentCanvas = canvas;
        isTextNew = !isFound;
        clickedText.getLabel().configureTextField(field, canvas.getZoomFactor());
        field.setText(clickedText.getText());
        canvas.add(field);

        Point fieldLocation = field.getLocation();
        double zoom = canvas.getZoomFactor();
        fieldLocation.x = (int) Math.round(mouseX * zoom - fieldLocation.x);
        fieldLocation.y = (int) Math.round(mouseY * zoom - fieldLocation.y);
        int caret = field.viewToModel2D(fieldLocation);
        if (caret >= 0) {
            field.setCaretPosition(caret);
        }
        field.requestFocus();

        canvas.getSelection().setSelected(clickedText, true);
        canvas.getSelection().setHidden(Collections.singleton(clickedText), true);
        clickedText.addAttributeListener(fieldListener);
        canvas.repaint();
    }

    @Override
    public void zoomFactorChanged(Canvas canvas) {
        Text currentText = this.currentText;
        if (currentText != null) {
            currentText.getLabel().configureTextField(field, canvas.getZoomFactor());
        }
    }

    @Override
    public void draw(Canvas canvas, Graphics graphics) {
        // actually, there's nothing to do here - it's handled by the field
    }

    private void cancelText(Canvas canvas) {
        Text currentText = this.currentText;
        if (currentText != null) {
            this.currentText = null;
            currentText.removeAttributeListener(fieldListener);
            canvas.remove(field);
            canvas.getSelection().clearSelected();
            canvas.repaint();
        }
    }

    private void commitText(Canvas canvas) {
        Text currentText = this.currentText;
        boolean isNew = isTextNew;
        String newText = field.getText();
        if (currentText == null) {
            return;
        }
        cancelText(canvas);

        if (isNew) {
            if (!newText.equals("")) {
                currentText.setText(newText);
                canvas.doAction(new ModelAddAction(canvas.getModel(), currentText));
            }
        } else {
            String oldText = currentText.getText();
            if (newText.equals("")) {
                canvas.doAction(new ModelRemoveAction(canvas.getModel(), currentText));
            } else if (!oldText.equals(newText)) {
                canvas.doAction(new ModelEditTextAction(canvas.getModel(), currentText, newText));
            }
        }
    }

    private class FieldListener extends AbstractAction implements AttributeListener {

        public void actionPerformed(ActionEvent event) {
            commitText(currentCanvas);
        }

        public void attributeListChanged(AttributeEvent event) {
            Text currentText = TextTool.this.currentText;
            if (currentText != null) {
                double zoom = currentCanvas.getZoomFactor();
                currentText.getLabel().configureTextField(field, zoom);
                currentCanvas.repaint();
            }
        }

        public void attributeValueChanged(AttributeEvent event) {
            attributeListChanged(event);
        }
    }

    private class CancelListener extends AbstractAction {

        public void actionPerformed(ActionEvent event) {
            cancelText(currentCanvas);
        }
    }
}

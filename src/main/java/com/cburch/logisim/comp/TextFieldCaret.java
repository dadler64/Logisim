/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.CaretEvent;
import com.cburch.logisim.tools.CaretListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;

class TextFieldCaret implements Caret, TextFieldListener {

    private final LinkedList<CaretListener> listeners = new LinkedList<>();
    private final TextField field;
    private final Graphics g;
    private String oldText;
    private String curText;
    private int position;

    public TextFieldCaret(TextField field, Graphics g, int position) {
        this.field = field;
        this.g = g;
        this.oldText = field.getText();
        this.curText = field.getText();
        this.position = position;

        field.addTextFieldListener(this);
    }

    public TextFieldCaret(TextField field, Graphics g, int x, int y) {
        this(field, g, 0);
        moveCaret(x, y);
    }

    public void addCaretListener(CaretListener l) {
        listeners.add(l);
    }

    public void removeCaretListener(CaretListener l) {
        listeners.remove(l);
    }

    public String getText() {
        return curText;
    }

    public void commitText(String text) {
        curText = text;
        position = curText.length();
        field.setText(text);
    }

    public void draw(Graphics g) {
        if (field.getFont() != null) {
            g.setFont(field.getFont());
        }

        // draw boundary
        Bounds bounds = getBounds(g);
        g.setColor(Color.white);
        g.fillRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
        g.setColor(Color.black);
        g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

        // draw text
        int x = field.getX();
        int y = field.getY();
        FontMetrics fm = g.getFontMetrics();
        int width = fm.stringWidth(curText);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        switch (field.getHAlign()) {
            case TextField.H_CENTER:
                x -= width / 2;
                break;
            case TextField.H_RIGHT:
                x -= width;
                break;
            default:
                break;
        }
        switch (field.getVAlign()) {
            case TextField.V_TOP:
                y += ascent;
                break;
            case TextField.V_CENTER:
                y += (ascent - descent) / 2;
                break;
            case TextField.V_BOTTOM:
                y -= descent;
                break;
            default:
                break;
        }
        g.drawString(curText, x, y);

        // draw cursor
        if (position > 0) {
            x += fm.stringWidth(curText.substring(0, position));
        }
        g.drawLine(x, y, x, y - ascent);
    }

    public Bounds getBounds(Graphics g) {
        int x = field.getX();
        int y = field.getY();
        Font font = field.getFont();
        FontMetrics fm;
        if (font == null) {
            fm = g.getFontMetrics();
        } else {
            fm = g.getFontMetrics(font);
        }
        int width = fm.stringWidth(curText);
        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int height = ascent + descent;
        switch (field.getHAlign()) {
            case TextField.H_CENTER:
                x -= width / 2;
                break;
            case TextField.H_RIGHT:
                x -= width;
                break;
            default:
                break;
        }
        switch (field.getVAlign()) {
            case TextField.V_TOP:
                y += ascent;
                break;
            case TextField.V_CENTER:
                y += (ascent - descent) / 2;
                break;
            case TextField.V_BOTTOM:
                y -= descent;
                break;
            default:
                break;
        }
        return Bounds.create(x, y - ascent, width, height).add(field.getBounds(g)).expand(3);
    }

    public void cancelEditing() {
        CaretEvent event = new CaretEvent(this, oldText, oldText);
        curText = oldText;
        position = curText.length();
        for (CaretListener listener : new ArrayList<>(listeners)) {
            listener.editingCanceled(event);
        }
        field.removeTextFieldListener(this);
    }

    public void stopEditing() {
        CaretEvent event = new CaretEvent(this, oldText, curText);
        field.setText(curText);
        for (CaretListener listener : new ArrayList<>(listeners)) {
            listener.editingStopped(event);
        }
        field.removeTextFieldListener(this);
    }

    public void mousePressed(MouseEvent e) {
        //TODO: enhance label editing
        moveCaret(e.getX(), e.getY());
    }

    public void mouseDragged(MouseEvent e) {
        //TODO: enhance label editing
    }

    public void mouseReleased(MouseEvent e) {
        //TODO: enhance label editing
        moveCaret(e.getX(), e.getY());
    }

    public void keyPressed(KeyEvent e) {
        int ign = InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK;
        if ((e.getModifiersEx() & ign) != 0) {
            return;
        }
        switch (e.getKeyCode()) {
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_KP_LEFT:
                if (position > 0) {
                    --position;
                }
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_KP_RIGHT:
                if (position < curText.length()) {
                    ++position;
                }
                break;
            case KeyEvent.VK_HOME:
                position = 0;
                break;
            case KeyEvent.VK_END:
                position = curText.length();
                break;
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_CANCEL:
                cancelEditing();
                break;
            case KeyEvent.VK_CLEAR:
                curText = "";
                position = 0;
                break;
            case KeyEvent.VK_ENTER:
                stopEditing();
                break;
            case KeyEvent.VK_BACK_SPACE:
                if (position > 0) {
                    curText = curText.substring(0, position - 1)
                        + curText.substring(position);
                    --position;
                }
                break;
            case KeyEvent.VK_DELETE:
                if (position < curText.length()) {
                    curText = curText.substring(0, position)
                        + curText.substring(position + 1);
                }
                break;
            case KeyEvent.VK_INSERT:
            case KeyEvent.VK_COPY:
            case KeyEvent.VK_CUT:
            case KeyEvent.VK_PASTE:
                //TODO: enhance label editing
                break;
            default:
                // ignore
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
        int ign = InputEvent.ALT_DOWN_MASK | InputEvent.CTRL_DOWN_MASK | InputEvent.META_DOWN_MASK;
        if ((e.getModifiersEx() & ign) != 0) {
            return;
        }

        char c = e.getKeyChar();
        if (c == '\n') {
            stopEditing();
        } else if (c != KeyEvent.CHAR_UNDEFINED
            && !Character.isISOControl(c)) {
            if (position < curText.length()) {
                curText = curText.substring(0, position) + c
                    + curText.substring(position);
            } else {
                curText += c;
            }
            ++position;
        }
    }

    private void moveCaret(int x, int y) {
        Bounds bds = getBounds(g);
        FontMetrics fm = g.getFontMetrics();
        x -= bds.getX();
        int last = 0;
        for (int i = 0; i < curText.length(); i++) {
            int cur = fm.stringWidth(curText.substring(0, i + 1));
            if (x <= (last + cur) / 2) {
                position = i;
                return;
            }
            last = cur;
        }
        position = curText.length();
    }

    public void textChanged(TextFieldEvent e) {
        curText = field.getText();
        oldText = curText;
        position = curText.length();
    }
}

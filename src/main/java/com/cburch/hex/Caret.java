/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.hex;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Caret {

    private static final Stroke CURSOR_STROKE = new BasicStroke(2.0f);
    private static final Color SELECT_COLOR = new Color(192, 192, 255);
    private final HexEditor hex;
    private final ArrayList<ChangeListener> listeners;
    private long mark;
    private long cursor;
    private Object highlight;

    Caret(HexEditor hex) {
        this.hex = hex;
        this.listeners = new ArrayList<>();
        this.cursor = -1;

        Listener listener = new Listener();
        hex.addMouseListener(listener);
        hex.addMouseMotionListener(listener);
        hex.addKeyListener(listener);
        hex.addFocusListener(listener);

        InputMap inputMap = hex.getInputMap();
        ActionMap actionMap = hex.getActionMap();
        AbstractAction nullAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
            }
        };
        String nullKey = "null";
        actionMap.put(nullKey, nullAction);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_HOME, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_END, 0), nullKey);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), nullKey);
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listeners.remove(listener);
    }

    public long getMark() {
        return mark;
    }

    public long getDot() {
        return cursor;
    }

    public void setDot(long value, boolean keepMark) {
        HexModel model = hex.getModel();
        if (model == null || value < model.getFirstOffset() || value > model.getLastOffset()) {
            value = -1;
        }
        if (cursor != value) {
            long oldValue = cursor;
            if (highlight != null) {
                hex.getHighlighter().remove(highlight);
                highlight = null;
            }
            if (!keepMark) {
                mark = value;
            } else if (mark != value) {
                highlight = hex.getHighlighter().add(mark, value, SELECT_COLOR);
            }
            cursor = value;
            expose(oldValue, false);
            expose(value, true);
            if (!listeners.isEmpty()) {
                ChangeEvent event = new ChangeEvent(this);
                for (ChangeListener listener : listeners) {
                    listener.stateChanged(event);
                }
            }
        }
    }

    private void expose(long location, boolean scrollTo) {
        if (location >= 0) {
            Measures measures = hex.getMeasures();
            int x = measures.toX(location);
            int y = measures.toY(location);
            int w = measures.getCellWidth();
            int h = measures.getCellHeight();
            hex.repaint(x - 1, y - 1, w + 2, h + 2);
            if (scrollTo) {
                hex.scrollRectToVisible(new Rectangle(x, y, w, h));
            }
        }
    }

    void paintForeground(Graphics graphics, long start, long end) {
        if (cursor >= start && cursor < end && hex.isFocusOwner()) {
            Measures measures = hex.getMeasures();
            int x = measures.toX(cursor);
            int y = measures.toY(cursor);
            Graphics2D graphics2D = (Graphics2D) graphics;
            Stroke oldStroke = graphics2D.getStroke();
            graphics2D.setColor(hex.getForeground());
            graphics2D.setStroke(CURSOR_STROKE);
            graphics2D.drawRect(x, y, measures.getCellWidth() - 1, measures.getCellHeight() - 1);
            graphics2D.setStroke(oldStroke);
        }
    }

    private class Listener implements MouseListener, MouseMotionListener,
        KeyListener, FocusListener {

        public void mouseClicked(MouseEvent event) {
        }

        public void mousePressed(MouseEvent event) {
            Measures measures = hex.getMeasures();
            long location = measures.toAddress(event.getX(), event.getY());
            setDot(location, (event.getModifiersEx() & ActionEvent.SHIFT_MASK) != 0);
            if (!hex.isFocusOwner()) {
                hex.requestFocus();
            }
        }

        public void mouseReleased(MouseEvent event) {
            mouseDragged(event);
        }

        public void mouseEntered(MouseEvent event) {
        }

        public void mouseExited(MouseEvent event) {
        }

        public void mouseDragged(MouseEvent event) {
            Measures measures = hex.getMeasures();
            long loc = measures.toAddress(event.getX(), event.getY());
            setDot(loc, true);

            // TODO should repeat dragged events when mouse leaves the component
        }

        public void mouseMoved(MouseEvent event) {
        }

        public void keyTyped(KeyEvent event) {
            int mask = event.getModifiersEx();
            if ((mask & ~ActionEvent.SHIFT_MASK) != 0) {
                return;
            }

            char c = event.getKeyChar();
            int columnCount = hex.getMeasures().getColumnCount();
            switch (c) {
                case ' ':
                    if (cursor >= 0) {
                        setDot(cursor + 1, (mask & ActionEvent.SHIFT_MASK) != 0);
                    }
                    break;
                case '\n':
                    if (cursor >= 0) {
                        setDot(cursor + columnCount, (mask & ActionEvent.SHIFT_MASK) != 0);
                    }
                    break;
                case '\u0008':
                case '\u007f':
                    hex.delete();
                    // setDot(cursor - 1, (mask & InputEvent.SHIFT_MASK) != 0);
                    break;
                default:
                    int digit = Character.digit(event.getKeyChar(), 16);
                    if (digit >= 0) {
                        HexModel model = hex.getModel();
                        if (model != null && cursor >= model.getFirstOffset() && cursor <= model.getLastOffset()) {
                            int currentValue = model.get(cursor);
                            int newValue = 16 * currentValue + digit;
                            model.set(cursor, newValue);
                        }
                    }
            }
        }

        public void keyPressed(KeyEvent event) {
            int columnCount = hex.getMeasures().getColumnCount();
            int rows;
            boolean shift = (event.getModifiersEx() & ActionEvent.SHIFT_MASK) != 0;
            switch (event.getKeyCode()) {
                case KeyEvent.VK_UP:
                    if (cursor >= columnCount) {
                        setDot(cursor - columnCount, shift);
                    }
                    break;
                case KeyEvent.VK_LEFT:
                    if (cursor >= 1) {
                        setDot(cursor - 1, shift);
                    }
                    break;
                case KeyEvent.VK_DOWN:
                    if (cursor >= hex.getModel().getFirstOffset() && cursor <= hex.getModel().getLastOffset() - columnCount) {
                        setDot(cursor + columnCount, shift);
                    }
                    break;
                case KeyEvent.VK_RIGHT:
                    if (cursor >= hex.getModel().getFirstOffset() && cursor <= hex.getModel().getLastOffset() - 1) {
                        setDot(cursor + 1, shift);
                    }
                    break;
                case KeyEvent.VK_HOME:
                    if (cursor >= 0) {
                        int dist = (int) (cursor % columnCount);
                        if (dist == 0) {
                            setDot(0, shift);
                        } else {
                            setDot(cursor - dist, shift);
                        }
                        break;
                    }
                case KeyEvent.VK_END:
                    if (cursor >= 0) {
                        HexModel model = hex.getModel();
                        long destination = (cursor / columnCount * columnCount) + columnCount - 1;
                        if (model != null) {
                            long end = model.getLastOffset();
                            if (destination > end || destination == cursor) {
                                destination = end;
                            }
                            setDot(destination, shift);
                        } else {
                            setDot(destination, shift);
                        }
                    }
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    rows = hex.getVisibleRect().height / hex.getMeasures().getCellHeight();
                    if (rows > 2) {
                        rows--;
                    }
                    if (cursor >= 0) {
                        long max = hex.getModel().getLastOffset();
                        if (cursor + rows * columnCount <= max) {
                            setDot(cursor + rows * columnCount, shift);
                        } else {
                            long n = cursor;
                            while (n + columnCount < max) {
                                n += columnCount;
                            }
                            setDot(n, shift);
                        }
                    }
                    break;
                case KeyEvent.VK_PAGE_UP:
                    rows = hex.getVisibleRect().height / hex.getMeasures().getCellHeight();
                    if (rows > 2) {
                        rows--;
                    }
                    if (cursor >= rows * columnCount) {
                        setDot(cursor - rows * columnCount, shift);
                    } else if (cursor >= columnCount) {
                        setDot(cursor % columnCount, shift);
                    }
                    break;
            }
        }

        public void keyReleased(KeyEvent event) {
        }

        public void focusGained(FocusEvent event) {
            expose(cursor, false);
        }

        public void focusLost(FocusEvent event) {
            expose(cursor, false);
        }
    }
}

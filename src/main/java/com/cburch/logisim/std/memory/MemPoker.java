/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.proj.Project;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class MemPoker extends InstancePoker {

    private MemPoker sub;

    @Override
    public boolean init(InstanceState state, MouseEvent event) {
        Bounds bounds = state.getInstance().getBounds();
        MemState data = (MemState) state.getData();
        long addr = data.getAddressAt(event.getX() - bounds.getX(), event.getY() - bounds.getY());

        // See if outside box
        if (addr < 0) {
            sub = new AddrPoker();
        } else {
            sub = new DataPoker(state, data, addr);
        }
        return true;
    }

    @Override
    public Bounds getBounds(InstancePainter state) {
        return sub.getBounds(state);
    }

    @Override
    public void paint(InstancePainter painter) {
        sub.paint(painter);
    }

    @Override
    public void keyTyped(InstanceState state, KeyEvent e) {
        sub.keyTyped(state, e);
    }

    private static class DataPoker extends MemPoker {

        int initValue;
        int currentValue;

        private DataPoker(InstanceState state, MemState data, long address) {
            data.setCursor(address);
            initValue = data.getContents().get(data.getCursor());
            currentValue = initValue;

            Object attrs = state.getInstance().getAttributeSet();
            if (attrs instanceof RomAttributes) {
                Project project = state.getProject();
                if (project != null) {
                    ((RomAttributes) attrs).setProject(project);
                }
            }
        }

        @Override
        public Bounds getBounds(InstancePainter painter) {
            MemState data = (MemState) painter.getData();
            Bounds inBounds = painter.getInstance().getBounds();
            return data.getBounds(data.getCursor(), inBounds);
        }

        @Override
        public void paint(InstancePainter painter) {
            Bounds bounds = getBounds(painter);
            Graphics g = painter.getGraphics();
            g.setColor(Color.RED);
            g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
            g.setColor(Color.BLACK);
        }

        @Override
        public void stopEditing(InstanceState state) {
            MemState data = (MemState) state.getData();
            data.setCursor(-1);
        }

        @Override
        public void keyTyped(InstanceState state, KeyEvent e) {
            char c = e.getKeyChar();
            int value = Character.digit(e.getKeyChar(), 16);
            MemState data = (MemState) state.getData();
            if (value >= 0) {
                currentValue = currentValue * 16 + value;
                data.getContents().set(data.getCursor(), currentValue);
                state.fireInvalidated();
            } else if (c == ' ' || c == '\t') {
                moveTo(data, data.getCursor() + 1);
            } else if (c == '\r' || c == '\n') {
                moveTo(data, data.getCursor() + data.getColumns());
            } else if (c == '\u0008' || c == '\u007f') {
                moveTo(data, data.getCursor() - 1);
            }
        }

        private void moveTo(MemState data, long addr) {
            if (data.isValidAddress(addr)) {
                data.setCursor(addr);
                data.scrollToShow(addr);
                initValue = data.getContents().get(addr);
                currentValue = initValue;
            }
        }
    }

    private static class AddrPoker extends MemPoker {

        @Override
        public Bounds getBounds(InstancePainter painter) {
            MemState data = (MemState) painter.getData();
            return data.getBounds(-1, painter.getBounds());
        }

        @Override
        public void paint(InstancePainter painter) {
            Bounds bounds = getBounds(painter);
            Graphics g = painter.getGraphics();
            g.setColor(Color.RED);
            g.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
            g.setColor(Color.BLACK);
        }

        @Override
        public void keyTyped(InstanceState state, KeyEvent e) {
            char c = e.getKeyChar();
            int value = Character.digit(e.getKeyChar(), 16);
            MemState data = (MemState) state.getData();
            if (value >= 0) {
                long newScroll = (data.getScroll() * 16 + value) & (data.getLastAddress());
                data.setScroll(newScroll);
            } else if (c == ' ') {
                data.setScroll(data.getScroll() + (long) (data.getRows() - 1) * data.getColumns());
            } else if (c == '\r' || c == '\n') {
                data.setScroll(data.getScroll() + data.getColumns());
            } else if (c == '\u0008' || c == '\u007f') {
                data.setScroll(data.getScroll() - data.getColumns());
            }
        }
    }
}

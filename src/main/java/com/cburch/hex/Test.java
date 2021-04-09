/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.hex;

import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

public class Test {

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        HexModel model = new Model();
        HexEditor editor = new HexEditor(model);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new JScrollPane(editor));
        frame.pack();
        frame.setTitle("Hex Test");
        frame.setVisible(true);
    }

    private static class Model implements HexModel {

        private final ArrayList<HexModelListener> listeners = new ArrayList<>();
        private final int[] data = new int[924];

        public void addHexModelListener(HexModelListener listener) {
            listeners.add(listener);
        }

        public void removeHexModelListener(HexModelListener listener) {
            listeners.remove(listener);
        }

        public long getFirstOffset() {
            return 11111;
        }

        public long getLastOffset() {
            return data.length + 11110;
        }

        public int getValueWidth() {
            return 9;
        }

        public int get(long address) {
            return data[(int) (address - 11111)];
        }

        public void set(long address, int value) {
            int[] oldValues = new int[]{data[(int) (address - 11111)]};
            data[(int) (address - 11111)] = value & 0x1FF;
            for (HexModelListener l : listeners) {
                l.bytesChanged(this, address, 1, oldValues);
            }
        }

        public void set(long start, int[] values) {
            int[] oldValues = new int[values.length];
            System.arraycopy(data, (int) (start - 11111), oldValues, 0, values.length);
            System.arraycopy(values, 0, data, (int) (start - 11111), values.length);
            for (HexModelListener l : listeners) {
                l.bytesChanged(this, start, values.length, oldValues);
            }
        }

        public void fill(long start, long length, int value) {
            int[] oldValues = new int[(int) length];
            System.arraycopy(data, (int) (start - 11111), oldValues, 0, (int) length);
            Arrays.fill(data, (int) (start - 11111), (int) length, value);
            for (HexModelListener l : listeners) {
                l.bytesChanged(this, start, length, oldValues);
            }
        }
    }
}

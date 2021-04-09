/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import java.util.Arrays;

class ShiftRegisterData extends ClockState implements InstanceData {

    private BitWidth width;
    private Value[] values;
    private int valuesPosition;

    public ShiftRegisterData(BitWidth width, int length) {
        this.width = width;
        this.values = new Value[length];
        Arrays.fill(this.values, Value.createKnown(width, 0));
        this.valuesPosition = 0;
    }

    @Override
    public ShiftRegisterData clone() {
        ShiftRegisterData ret = (ShiftRegisterData) super.clone();
        ret.values = this.values.clone();
        return ret;
    }

    public int getLength() {
        return values.length;
    }

    public void setDimensions(BitWidth newBitWidth, int newLength) {
        Value[] values = this.values;
        BitWidth oldBitWidth = width;
        int oldWidth = oldBitWidth.getWidth();
        int newWidth = newBitWidth.getWidth();
        if (values.length != newLength) {
            Value[] newValues = new Value[newLength];
            int j = valuesPosition;
            int copy = Math.min(newLength, values.length);
            for (int i = 0; i < copy; i++) {
                newValues[i] = values[j];
                j++;
                if (j == values.length) {
                    j = 0;
                }
            }
            Arrays.fill(newValues, copy, newLength, Value.createKnown(newBitWidth, 0));
            values = newValues;
            valuesPosition = 0;
            this.values = newValues;
        }
        if (oldWidth != newWidth) {
            for (int i = 0; i < values.length; i++) {
                Value vi = values[i];
                if (vi.getWidth() != newWidth) {
                    values[i] = vi.extendWidth(newWidth, Value.FALSE);
                }
            }
            width = newBitWidth;
        }
    }

    public void clear() {
        Arrays.fill(values, Value.createKnown(width, 0));
        valuesPosition = 0;
    }

    public void push(Value value) {
        int position = valuesPosition;
        values[position] = value;
        valuesPosition = position >= values.length - 1 ? 0 : position + 1;
    }

    public Value get(int index) {
        int i = valuesPosition + index;
        Value[] v = values;
        if (i >= v.length) {
            i -= v.length;
        }
        return v[i];
    }

    public void set(int index, Value value) {
        int i = valuesPosition + index;
        Value[] values = this.values;
        if (i >= values.length) {
            i -= values.length;
        }
        values[i] = value;
    }
}

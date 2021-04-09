/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import java.awt.Color;

public class HexDigit extends InstanceFactory {

    public HexDigit() {
        super("Hex Digit Display", Strings.getter("hexDigitComponent"));
        setAttributes(
            new Attribute[]{
                Io.ATTR_ON_COLOR,
                Io.ATTR_OFF_COLOR,
                Io.ATTR_BACKGROUND
            }, new Object[]{
                new Color(240, 0, 0),
                SevenSegment.DEFAULT_OFF,
                Io.DEFAULT_BACKGROUND
            });
        setPorts(new Port[]{
            new Port(0, 0, Port.INPUT, 4),
            new Port(10, 0, Port.INPUT, 1)
        });
        setOffsetBounds(Bounds.create(-15, -60, 40, 60));
        setIconName("hexdig.gif");
    }

    @Override
    public void propagate(InstanceState state) {
        int summary = 0;
        Value baseValue = state.getPort(0);
        if (baseValue == null) {
            baseValue = Value.createUnknown(BitWidth.create(4));
        }
        int segment; // each nibble is one segment, in top-down, left-to-right
        // order: middle three nibbles are the three horizontal segments
        switch (baseValue.toIntValue()) {
            case 0:
                segment = 0x1110111;
                break;
            case 1:
                segment = 0x0000011;
                break;
            case 2:
                segment = 0x0111110;
                break;
            case 3:
                segment = 0x0011111;
                break;
            case 4:
                segment = 0x1001011;
                break;
            case 5:
                segment = 0x1011101;
                break;
            case 6:
                segment = 0x1111101;
                break;
            case 7:
                segment = 0x0010011;
                break;
            case 8:
                segment = 0x1111111;
                break;
            case 9:
                segment = 0x1011011;
                break;
            case 10:
                segment = 0x1111011;
                break;
            case 11:
                segment = 0x1101101;
                break;
            case 12:
                segment = 0x1110100;
                break;
            case 13:
                segment = 0x0101111;
                break;
            case 14:
                segment = 0x1111100;
                break;
            case 15:
                segment = 0x1111000;
                break;
            default:
                segment = 0x0001000;
                break; // a dash '-'
        }
        if ((segment & 0x1) != 0) {
            summary |= 4; // vertical seg in bottom right
        }
        if ((segment & 0x10) != 0) {
            summary |= 2; // vertical seg in top right
        }
        if ((segment & 0x100) != 0) {
            summary |= 8; // horizontal seg at bottom
        }
        if ((segment & 0x1000) != 0) {
            summary |= 64; // horizontal seg at middle
        }
        if ((segment & 0x10000) != 0) {
            summary |= 1; // horizontal seg at top
        }
        if ((segment & 0x100000) != 0) {
            summary |= 16; // vertical seg at bottom left
        }
        if ((segment & 0x1000000) != 0) {
            summary |= 32; // vertical seg at top left
        }
        if (state.getPort(1) == Value.TRUE) {
            summary |= 128;
        }

        Object value = summary;
        InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
        if (data == null) {
            state.setData(new InstanceDataSingleton(value));
        } else {
            data.setValue(value);
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        SevenSegment.drawBase(painter);
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class ShiftRegisterLogger extends InstanceLogger {

    @Override
    public Object[] getLogOptions(InstanceState state) {
        Integer stages = state.getAttributeValue(ShiftRegister.ATTR_LENGTH);
        Object[] ret = new Object[stages];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = i;
        }
        return ret;
    }

    @Override
    public String getLogName(InstanceState state, Object option) {
        String name = state.getAttributeValue(StdAttr.LABEL);
        if (name == null || name.equals("")) {
            name = Strings.get("shiftRegisterComponent") + state.getInstance().getLocation();
        }
        if (option instanceof Integer) {
            return name + "[" + option + "]";
        } else {
            return name;
        }
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
        BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
        if (dataWidth == null) {
            dataWidth = BitWidth.create(0);
        }
        ShiftRegisterData data = (ShiftRegisterData) state.getData();
        if (data == null) {
            return Value.createKnown(dataWidth, 0);
        } else {
            int index = option == null ? 0 : (Integer) option;
            return data.get(index);
        }
    }
}

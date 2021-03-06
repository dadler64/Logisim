/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.gray;

import com.adlerd.logger.Logger;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceState;

/**
 * Represents the state of a counter.
 */
class CounterData implements InstanceData, Cloneable {

    /**
     * The last clock input value observed.
     */
    private Value lastClock;
    /**
     * The current value emitted by the counter.
     */
    private Value value;

    /**
     * Constructs a state with the given values.
     */
    private CounterData(Value lastClock, Value value) {
        this.lastClock = lastClock;
        this.value = value;
    }

    /**
     * Retrieves the state associated with this counter in the circuit state,
     * generating the state if necessary.
     */
    public static CounterData get(InstanceState state, BitWidth width) {
        CounterData data = (CounterData) state.getData();
        if (data == null) {
            // If it doesn't yet exist, then we'll set it up with our default
            // values and put it into the circuit state so it can be retrieved
            // in future propagations.
            data = new CounterData(null, Value.createKnown(width, 0));
            state.setData(data);
        } else if (!data.value.getBitWidth().equals(width)) {
            data.value = data.value.extendWidth(width.getWidth(), Value.FALSE);
        }
        return data;
    }

    /**
     * Returns a copy of this object.
     */
    @Override
    public Object clone() {
        // We can just use what super.clone() returns: The only instance variables are
        // Value objects, which are immutable, so we don't care that both the copy
        // and the copied refer to the same Value objects. If we had mutable instance
        // variables, then of course we would need to clone them.
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            Logger.errorln(e, false);
            return null;
        }
    }

    /**
     * Updates the last clock observed, returning true if triggered.
     */
    public boolean updateClock(Value value) {
        Value oldValue = lastClock;
        lastClock = value;
        return oldValue == Value.FALSE && value == Value.TRUE;
    }

    /**
     * Returns the current value emitted by the counter.
     */
    public Value getValue() {
        return value;
    }

    /**
     * Updates the current value emitted by the counter.
     */
    public void setValue(Value value) {
        this.value = value;
    }
}


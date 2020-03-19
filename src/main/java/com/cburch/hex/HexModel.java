/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.hex;

public interface HexModel {

    /**
     * Registers a listener for changes to the values.
     */
    void addHexModelListener(HexModelListener listener);

    /**
     * Unregisters a listener for changes to the values.
     */
    void removeHexModelListener(HexModelListener listener);

    /**
     * Returns the offset of the initial value to be displayed.
     */
    long getFirstOffset();

    /**
     * Returns the number of values to be displayed.
     */
    long getLastOffset();

    /**
     * Returns number of bits in each value.
     */
    int getValueWidth();

    /**
     * Returns the value at the given address.
     */
    int get(long address);

    /**
     * Changes the value at the given address.
     */
    void set(long address, int value);

    /**
     * Changes a series of values at the given addresses.
     */
    void set(long start, int[] values);

    /**
     * Fills a series of values with the same value.
     */
    void fill(long start, long length, int value);
}

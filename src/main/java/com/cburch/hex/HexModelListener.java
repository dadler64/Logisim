/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.hex;

public interface HexModelListener {

    void metainfoChanged(HexModel source);

    void bytesChanged(HexModel source, long start, long numBytes,
            int[] oldValues);
}

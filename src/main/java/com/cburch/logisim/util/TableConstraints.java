/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

public class TableConstraints {

    private final int col;
    private final int row;

    private TableConstraints(int row, int col) {
        this.col = col;
        this.row = row;
    }

    public static TableConstraints at(int row, int col) {
        return new TableConstraints(row, col);
    }

    int getRow() {
        return row;
    }

    int getCol() {
        return col;
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.comp;

public class TextFieldEvent {

    private final TextField field;
    private final String oldValue;
    private final String newValue;

    public TextFieldEvent(TextField field, String old, String val) {
        this.field = field;
        this.oldValue = old;
        this.newValue = val;
    }

    public TextField getTextField() {
        return field;
    }

    public String getOldText() {
        return oldValue;
    }

    public String getText() {
        return newValue;
    }
}

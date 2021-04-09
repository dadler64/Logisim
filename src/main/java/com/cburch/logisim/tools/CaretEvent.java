/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

public class CaretEvent {

    private final Caret caret;
    private final String oldText;
    private final String newText;

    public CaretEvent(Caret caret, String oldText, String newText) {
        this.caret = caret;
        this.oldText = oldText;
        this.newText = newText;
    }

    public Caret getCaret() {
        return caret;
    }

    public String getOldText() {
        return oldText;
    }

    public String getText() {
        return newText;
    }
}

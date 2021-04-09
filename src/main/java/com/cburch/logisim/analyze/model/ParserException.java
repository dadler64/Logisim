/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.model;

import com.cburch.logisim.util.StringGetter;

public class ParserException extends Exception {

    private final StringGetter message;
    private final int start;
    private final int length;

    public ParserException(StringGetter message, int start, int length) {
        super(message.get());
        this.message = message;
        this.start = start;
        this.length = length;
    }

    @Override
    public String getMessage() {
        return message.get();
    }

    public StringGetter getMessageGetter() {
        return message;
    }

    public int getOffset() {
        return start;
    }

    public int getEndOffset() {
        return start + length;
    }
}

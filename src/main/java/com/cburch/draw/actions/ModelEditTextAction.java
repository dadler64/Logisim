/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.actions;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.Text;
import java.util.Collection;
import java.util.Collections;

public class ModelEditTextAction extends ModelAction {

    private final Text text;
    private final String oldValue;
    private final String newValue;

    public ModelEditTextAction(CanvasModel model, Text text, String newValue) {
        super(model);
        this.text = text;
        this.oldValue = text.getText();
        this.newValue = newValue;
    }

    @Override
    public Collection<CanvasObject> getObjects() {
        return Collections.singleton(text);
    }

    @Override
    public String getName() {
        return Strings.get("actionEditText");
    }

    @Override
    void doSub(CanvasModel model) {
        model.setText(text, newValue);
    }

    @Override
    void undoSub(CanvasModel model) {
        model.setText(text, oldValue);
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.actions;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import java.util.Collection;
import java.util.Collections;

public class ModelInsertHandleAction extends ModelAction {

    private Handle desired;

    public ModelInsertHandleAction(CanvasModel model, Handle desired) {
        super(model);
        this.desired = desired;
    }

    @Override
    public Collection<CanvasObject> getObjects() {
        return Collections.singleton(desired.getObject());
    }

    @Override
    public String getName() {
        return Strings.get("actionInsertHandle");
    }

    @Override
    void doSub(CanvasModel model) {
        model.insertHandle(desired, null);
    }

    @Override
    void undoSub(CanvasModel model) {
        model.deleteHandle(desired);
    }
}

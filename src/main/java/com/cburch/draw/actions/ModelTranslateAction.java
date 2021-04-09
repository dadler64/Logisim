/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.actions;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.undo.Action;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class ModelTranslateAction extends ModelAction {

    private final HashSet<CanvasObject> moved;
    private final int dx;
    private final int dy;

    public ModelTranslateAction(CanvasModel model,
        Collection<CanvasObject> moved, int dx, int dy) {
        super(model);
        this.moved = new HashSet<>(moved);
        this.dx = dx;
        this.dy = dy;
    }

    @Override
    public Collection<CanvasObject> getObjects() {
        return Collections.unmodifiableSet(moved);
    }

    @Override
    public String getName() {
        return Strings.get("actionTranslate", getShapesName(moved));
    }

    @Override
    void doSub(CanvasModel model) {
        model.translateObjects(moved, dx, dy);
    }

    @Override
    void undoSub(CanvasModel model) {
        model.translateObjects(moved, -dx, -dy);
    }

    @Override
    public boolean shouldAppendTo(Action action) {
        if (action instanceof ModelTranslateAction) {
            ModelTranslateAction translateAction = (ModelTranslateAction) action;
            return this.moved.equals(translateAction.moved);
        } else {
            return false;
        }
    }

    @Override
    public Action append(Action action) {
        if (action instanceof ModelTranslateAction) {
            ModelTranslateAction translateAction = (ModelTranslateAction) action;
            if (this.moved.equals(translateAction.moved)) {
                return new ModelTranslateAction(getModel(), moved, dx + translateAction.dx, dy + translateAction.dy);
            }
        }
        return super.append(action);
    }
}

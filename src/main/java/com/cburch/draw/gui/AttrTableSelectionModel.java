/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.gui;

import com.cburch.draw.actions.ModelChangeAttributeAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import java.util.HashMap;
import java.util.Map;

class AttrTableSelectionModel extends AttributeSetTableModel
        implements SelectionListener {

    private Canvas canvas;

    public AttrTableSelectionModel(Canvas canvas) {
        super(new SelectionAttributes(canvas.getSelection()));
        this.canvas = canvas;
        canvas.getSelection().addSelectionListener(this);
    }

    @Override
    public String getTitle() {
        Selection sel = canvas.getSelection();
        Class<? extends CanvasObject> commonClass = null;
        int commonCount = 0;
        CanvasObject firstObject = null;
        int totalCount = 0;
        for (CanvasObject obj : sel.getSelected()) {
            if (firstObject == null) {
                firstObject = obj;
                commonClass = obj.getClass();
                commonCount = 1;
            } else if (obj.getClass() == commonClass) {
                commonCount++;
            } else {
                commonClass = null;
            }
            totalCount++;
        }

        if (firstObject == null) {
            return null;
        } else if (commonClass == null) {
            return Strings.get("selectionVarious", "" + totalCount);
        } else if (commonCount == 1) {
            return Strings.get("selectionOne", firstObject.getDisplayName());
        } else {
            return Strings.get("selectionMultiple", firstObject.getDisplayName(),
                    "" + commonCount);
        }
    }

    @Override
    public void setValueRequested(Attribute<Object> attribute, Object value)
            throws AttrTableSetException {
        SelectionAttributes attrs = (SelectionAttributes) getAttributes();
        HashMap<AttributeMapKey, Object> oldVals;
        oldVals = new HashMap<>();
        HashMap<AttributeMapKey, Object> newVals;
        newVals = new HashMap<>();
        for (Map.Entry<AttributeSet, CanvasObject> ent : attrs.entries()) {
            AttributeMapKey key = new AttributeMapKey(attribute, ent.getValue());
            oldVals.put(key, ent.getKey().getValue(attribute));
            newVals.put(key, value);
        }
        CanvasModel model = canvas.getModel();
        canvas.doAction(new ModelChangeAttributeAction(model, oldVals, newVals));
    }

    //
    // SelectionListener method
    //
    public void selectionChanged(SelectionEvent e) {
        fireTitleChanged();
    }
}

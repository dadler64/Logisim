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
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import java.util.HashMap;
import java.util.Map;

class AttrTableSelectionModel extends AttributeSetTableModel implements SelectionListener {

    private final Canvas canvas;

    public AttrTableSelectionModel(Canvas canvas) {
        super(new SelectionAttributes(canvas.getSelection()));
        this.canvas = canvas;
        canvas.getSelection().addSelectionListener(this);
    }

    @Override
    public String getTitle() {
        Selection selection = canvas.getSelection();
        Class<? extends CanvasObject> commonClass = null;
        int commonCount = 0;
        CanvasObject firstObject = null;
        int totalCount = 0;
        for (CanvasObject object : selection.getSelected()) {
            if (firstObject == null) {
                firstObject = object;
                commonClass = object.getClass();
                commonCount = 1;
            } else if (object.getClass() == commonClass) {
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
            return Strings.get("selectionMultiple", firstObject.getDisplayName(), "" + commonCount);
        }
    }

    @Override
    public void setValueRequested(Attribute<Object> attribute, Object value) {
        SelectionAttributes attributes = (SelectionAttributes) getAttributes();
        HashMap<AttributeMapKey, Object> oldValues = new HashMap<>();
        HashMap<AttributeMapKey, Object> newValues = new HashMap<>();
        for (Map.Entry<AttributeSet, CanvasObject> entry : attributes.entries()) {
            AttributeMapKey key = new AttributeMapKey(attribute, entry.getValue());
            oldValues.put(key, entry.getKey().getValue(attribute));
            newValues.put(key, value);
        }
        CanvasModel model = canvas.getModel();
        canvas.doAction(new ModelChangeAttributeAction(model, oldValues, newValues));
    }

    //
    // SelectionListener method
    //
    public void selectionChanged(SelectionEvent event) {
        fireTitleChanged();
    }
}

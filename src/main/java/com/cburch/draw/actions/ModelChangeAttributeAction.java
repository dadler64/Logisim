/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.actions;

import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class ModelChangeAttributeAction extends ModelAction {

    private final Map<AttributeMapKey, Object> oldValues;
    private final Map<AttributeMapKey, Object> newValues;
    private Attribute<?> attribute;

    public ModelChangeAttributeAction(CanvasModel model,
        Map<AttributeMapKey, Object> oldValues,
        Map<AttributeMapKey, Object> newValues) {
        super(model);
        this.oldValues = oldValues;
        this.newValues = newValues;
    }

    @Override
    public Collection<CanvasObject> getObjects() {
        HashSet<CanvasObject> ret = new HashSet<>();
        for (AttributeMapKey key : newValues.keySet()) {
            ret.add(key.getObject());
        }
        return ret;
    }

    @Override
    public String getName() {
        Attribute<?> attribute = this.attribute;
        if (attribute == null) {
            boolean found = false;
            for (AttributeMapKey key : newValues.keySet()) {
                Attribute<?> keyAttribute = key.getAttribute();
                if (found) {
                    if (!Objects.equals(attribute, keyAttribute)) {
                        attribute = null;
                        break;
                    }
                } else {
                    found = true;
                    attribute = keyAttribute;
                }
            }
            this.attribute = attribute;
        }
        if (attribute == null) {
            return Strings.get("actionChangeAttributes");
        } else {
            return Strings.get("actionChangeAttribute", attribute.getDisplayName());
        }
    }

    @Override
    void doSub(CanvasModel model) {
        model.setAttributeValues(newValues);
    }

    @Override
    void undoSub(CanvasModel model) {
        model.setAttributeValues(oldValues);
    }
}

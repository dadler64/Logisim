/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.actions;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.ReorderRequest;
import com.cburch.draw.util.ZOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ModelReorderAction extends ModelAction {

    private final ArrayList<ReorderRequest> requests;
    private final ArrayList<CanvasObject> objects;
    private final int type;

    public ModelReorderAction(CanvasModel model, List<ReorderRequest> requests) {
        super(model);
        this.requests = new ArrayList<>(requests);
        this.objects = new ArrayList<>(requests.size());
        for (ReorderRequest r : requests) {
            objects.add(r.getObject());
        }
        int type = 0; // 0 = mixed/unknown, -1 = to greater index, 1 = to smaller index
        for (ReorderRequest r : requests) {
            int thisType;
            int from = r.getFromIndex();
            int to = r.getToIndex();
            if (to < from) {
                thisType = -1;
            } else if (to > from) {
                thisType = 1;
            } else {
                thisType = 0;
            }
            if (type == 2) {
                type = thisType;
            } else if (type != thisType) {
                type = 0;
                break;
            }
        }
        this.type = type;
    }

    public static ModelReorderAction createRaise(CanvasModel model, Collection<? extends CanvasObject> objects) {
        List<ReorderRequest> requests = new ArrayList<>();
        Map<CanvasObject, Integer> zMap = ZOrder.getZIndex(objects, model);
        for (Map.Entry<CanvasObject, Integer> entry : zMap.entrySet()) {
            CanvasObject object = entry.getKey();
            int fromIndex = entry.getValue();
            CanvasObject objectAbove = ZOrder.getObjectAbove(object, model, objects);
            if (objectAbove != null) {
                int toIndex = ZOrder.getZIndex(objectAbove, model);
                if (objects.contains(objectAbove)) {
                    toIndex--;
                }
                requests.add(new ReorderRequest(object, fromIndex, toIndex));
            }
        }
        if (requests.isEmpty()) {
            return null;
        } else {
            requests.sort(ReorderRequest.DESCENDING_FROM);
            repairRequests(requests);
            return new ModelReorderAction(model, requests);
        }
    }

    public static ModelReorderAction createLower(CanvasModel model, Collection<? extends CanvasObject> objects) {
        List<ReorderRequest> requests = new ArrayList<>();
        Map<CanvasObject, Integer> zMap = ZOrder.getZIndex(objects, model);
        for (Map.Entry<CanvasObject, Integer> entry : zMap.entrySet()) {
            CanvasObject object = entry.getKey();
            int fromIndex = entry.getValue();
            CanvasObject objectBelow = ZOrder.getObjectBelow(object, model, objects);
            if (objectBelow != null) {
                int toIndex = ZOrder.getZIndex(objectBelow, model);
                if (objects.contains(objectBelow)) {
                    toIndex++;
                }
                requests.add(new ReorderRequest(object, fromIndex, toIndex));
            }
        }
        if (requests.isEmpty()) {
            return null;
        } else {
            requests.sort(ReorderRequest.ASCENDING_FROM);
            repairRequests(requests);
            return new ModelReorderAction(model, requests);
        }
    }

    public static ModelReorderAction createRaiseTop(CanvasModel model, Collection<? extends CanvasObject> objects) {
        List<ReorderRequest> requests = new ArrayList<>();
        Map<CanvasObject, Integer> zMap = ZOrder.getZIndex(objects, model);
        int toIndex = model.getObjectsFromBottom().size() - 1;
        for (Map.Entry<CanvasObject, Integer> entry : zMap.entrySet()) {
            CanvasObject object = entry.getKey();
            int fromIndex = entry.getValue();
            requests.add(new ReorderRequest(object, fromIndex, toIndex));
        }
        if (requests.isEmpty()) {
            return null;
        } else {
            requests.sort(ReorderRequest.ASCENDING_FROM);
            repairRequests(requests);
            return new ModelReorderAction(model, requests);
        }
    }

    public static ModelReorderAction createLowerBottom(CanvasModel model, Collection<? extends CanvasObject> objects) {
        List<ReorderRequest> requests = new ArrayList<>();
        Map<CanvasObject, Integer> zMap = ZOrder.getZIndex(objects, model);
        int toIndex = 0;
        for (Map.Entry<CanvasObject, Integer> entry : zMap.entrySet()) {
            CanvasObject object = entry.getKey();
            int fromIndex = entry.getValue();
            requests.add(new ReorderRequest(object, fromIndex, toIndex));
        }
        if (requests.isEmpty()) {
            return null;
        } else {
            requests.sort(ReorderRequest.ASCENDING_FROM);
            repairRequests(requests);
            return new ModelReorderAction(model, requests);
        }
    }

    private static void repairRequests(List<ReorderRequest> requests) {
        for (int i = 0, n = requests.size(); i < n; i++) {
            ReorderRequest request = requests.get(i);
            int fromIndex = request.getFromIndex();
            int toIndex = request.getToIndex();
            for (int j = 0; j < i; j++) {
                ReorderRequest previousRequest = requests.get(j);
                int prevFromIndex = previousRequest.getFromIndex();
                int prevToIndex = previousRequest.getToIndex();
                if (prevFromIndex <= fromIndex && fromIndex < prevToIndex) {
                    fromIndex--;
                } else if (prevToIndex <= fromIndex && fromIndex < prevFromIndex) {
                    fromIndex++;
                }
                if (prevFromIndex <= toIndex && toIndex < prevToIndex) {
                    toIndex--;
                } else if (prevToIndex <= toIndex && toIndex < prevFromIndex) {
                    toIndex++;
                }
            }
            if (fromIndex != request.getFromIndex() || toIndex != request.getToIndex()) {
                requests.set(i, new ReorderRequest(request.getObject(), fromIndex, toIndex));
            }
        }
        for (int i = requests.size() - 1; i >= 0; i--) {
            ReorderRequest request = requests.get(i);
            if (request.getFromIndex() == request.getToIndex()) {
                requests.remove(i);
            }
        }
    }

    public List<ReorderRequest> getReorderRequests() {
        return Collections.unmodifiableList(requests);
    }

    @Override
    public Collection<CanvasObject> getObjects() {
        return objects;
    }

    @Override
    public String getName() {
        if (type < 0) {
            return Strings.get("actionRaise", getShapesName(objects));
        } else if (type > 0) {
            return Strings.get("actionLower", getShapesName(objects));
        } else {
            return Strings.get("actionReorder", getShapesName(objects));
        }
    }

    @Override
    void doSub(CanvasModel model) {
        model.reorderObjects(requests);
    }

    @Override
    void undoSub(CanvasModel model) {
        ArrayList<ReorderRequest> requests = new ArrayList<>(this.requests.size());
        for (int i = this.requests.size() - 1; i >= 0; i--) {
            ReorderRequest request = this.requests.get(i);
            requests.add(new ReorderRequest(request.getObject(), request.getToIndex(),
                    request.getFromIndex()));
        }
        model.reorderObjects(requests);
    }
}

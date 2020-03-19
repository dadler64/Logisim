/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.draw.shapes;

import com.cburch.draw.model.Handle;
import com.cburch.logisim.data.Location;

public class PolyUtil {

    private PolyUtil() {
    }

    public static ClosestResult getClosestPoint(Location location, boolean isClosed, Handle[] handles) {
        int xq = location.getX();
        int yq = location.getY();
        ClosestResult result = new ClosestResult();
        result.distance = Double.MAX_VALUE;
        if (handles.length > 0) {
            Handle h0 = handles[0];
            int x0 = h0.getX();
            int y0 = h0.getY();
            int stop = isClosed ? handles.length : (handles.length - 1);
            for (int i = 0; i < stop; i++) {
                Handle h1 = handles[(i + 1) % handles.length];
                int x1 = h1.getX();
                int y1 = h1.getY();
                double distanceSegment = LineUtil.ptDistSqSegment(x0, y0, x1, y1, xq, yq);
                if (distanceSegment < result.distance) {
                    result.distance = distanceSegment;
                    result.previousHandle = h0;
                    result.nextHandle = h1;
                }
                h0 = h1;
                x0 = x1;
                y0 = y1;
            }
        }
        if (result.distance == Double.MAX_VALUE) {
            return null;
        } else {
            Handle h0 = result.previousHandle;
            Handle h1 = result.nextHandle;
            double[] pointSegment = LineUtil.nearestPointSegment(xq, yq, h0.getX(), h0.getY(), h1.getX(), h1.getY());
            result.location = Location.create((int) Math.round(pointSegment[0]), (int) Math.round(pointSegment[1]));
            return result;
        }
    }

    public static class ClosestResult {

        private double distance;
        private Location location;
        private Handle previousHandle;
        private Handle nextHandle;

        public double getDistanceSq() {
            return distance;
        }

        public Location getLocation() {
            return location;
        }

        public Handle getPreviousHandle() {
            return previousHandle;
        }

        public Handle getNextHandle() {
            return nextHandle;
        }
    }
}

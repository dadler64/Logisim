/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools.move;

import com.adlerd.logger.Logger;
import com.cburch.logisim.circuit.ReplacementMap;

class ConnectorThread extends Thread {

    private static final ConnectorThread INSTANCE = new ConnectorThread();

    static {
        INSTANCE.start();
    }

    private final Object lock;
    private transient boolean overrideRequest;
    private MoveRequest nextRequest;
    private MoveRequest processingRequest;

    private ConnectorThread() {
        lock = new Object();
        overrideRequest = false;
        nextRequest = null;
    }

    public static void enqueueRequest(MoveRequest request, boolean priority) {
        synchronized (INSTANCE.lock) {
            if (!request.equals(INSTANCE.processingRequest)) {
                INSTANCE.nextRequest = request;
                INSTANCE.overrideRequest = priority;
                INSTANCE.lock.notifyAll();
            }
        }
    }

    public static boolean isOverrideRequested() {
        return INSTANCE.overrideRequest;
    }

    public boolean isAbortRequested() {
        return overrideRequest;
    }

    @Override
    public void run() {
        while (true) {
            MoveRequest request;
            boolean wasOverride;
            synchronized (lock) {
                processingRequest = null;
                while (nextRequest == null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        Logger.debugln(e.getMessage());
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                request = nextRequest;
                wasOverride = overrideRequest;
                nextRequest = null;
                overrideRequest = false;
                processingRequest = request;
            }

            try {
                MoveResult result = Connector.computeWires(request);
                if (result != null) {
                    MoveGesture gesture = request.getMoveGesture();
                    gesture.notifyResult(request, result);
                }
            } catch (Throwable t) {
                Logger.debugln(t.getMessage());
//                t.printStackTrace();
                if (wasOverride) {
                    MoveResult result = new MoveResult(request, new ReplacementMap(),
                        request.getMoveGesture().getConnections(), 0);
                    request.getMoveGesture().notifyResult(request, result);
                }
            }
        }
    }
}

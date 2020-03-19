/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.ArrayList;

public class Simulator {
    // begin DEBUGGING
//	private static PrintWriter debugLog;
//
//	static {
//		try {
//			debugLog = new PrintWriter(new BufferedWriter(new FileWriter("DEBUG")));
//		} catch (IOException e) {
//			System.err.println("Could not open debug log"); //OK
//		}
//	}
//
//	public static void log(String message) {
//		debugLog.println(message);
//	}
//
//	public static void flushLog() {
//		debugLog.flush();
//	}
    //end DEBUGGING*/

    private boolean isRunning = true;
    private boolean isTicking = false;
    private final PropagationManager manager;
    private boolean isExceptionEncountered = false;
    private double tickFrequency;
    private SimulatorTicker ticker;
    private ArrayList<SimulatorListener> listeners = new ArrayList<>();

    public Simulator() {
        manager = new PropagationManager();
        ticker = new SimulatorTicker(manager);
        try {
            manager.setPriority(manager.getPriority() - 1);
            ticker.setPriority(ticker.getPriority() - 1);
        } catch (SecurityException | IllegalArgumentException e) {
            e.printStackTrace();
        }
        manager.start();
        ticker.start();

        tickFrequency = 0.0;
        setTickFrequency(AppPreferences.TICK_FREQUENCY.get());
    }

    public void shutDown() {
        ticker.shutDown();
        manager.shutDown();
    }

    public CircuitState getCircuitState() {
        Propagator propagator = manager.getPropagator();
        return propagator == null ? null : propagator.getRootState();
    }

    public void setCircuitState(CircuitState state) {
        manager.setPropagator(state.getPropagator());
        renewTickerAwake();
    }

    public void requestReset() {
        manager.requestReset();
    }

    public void tick() {
        ticker.tickOnce();
    }

    public void step() {
        synchronized (manager) {
            manager.stepsRequested++;
            manager.notifyAll();
        }
    }

    public void drawStepPoints(ComponentDrawContext context) {
        manager.stepPoints.draw(context);
    }

    public boolean isExceptionEncountered() {
        return isExceptionEncountered;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        if (this.isRunning != isRunning) {
            this.isRunning = isRunning;
            renewTickerAwake();
            // DEBUGGING - comment out:
//			if (!isRunning) flushLog(); */
            fireSimulatorStateChanged();
        }
    }

    public boolean isTicking() {
        return isTicking;
    }

    public void setIsTicking(boolean isTicking) {
        if (this.isTicking != isTicking) {
            this.isTicking = isTicking;
            renewTickerAwake();
            fireSimulatorStateChanged();
        }
    }

    private void renewTickerAwake() {
        ticker.setAwake(isRunning && isTicking && tickFrequency > 0);
    }

    public double getTickFrequency() {
        return tickFrequency;
    }

    public void setTickFrequency(double tickFrequency) {
        if (this.tickFrequency != tickFrequency) {
            int milliseconds = (int) Math.round(1000 / tickFrequency);
            int ticks;
            if (milliseconds > 0) {
                ticks = 1;
            } else {
                milliseconds = 1;
                ticks = (int) Math.round(tickFrequency / 1000);
            }

            this.tickFrequency = tickFrequency;
            ticker.setTickFrequency(milliseconds, ticks);
            renewTickerAwake();
            fireSimulatorStateChanged();
        }
    }

    public void requestPropagate() {
        manager.requestPropagate();
    }

    public boolean isOscillating() {
        Propagator propagator = manager.getPropagator();
        return propagator != null && propagator.isOscillating();
    }

    public void addSimulatorListener(SimulatorListener listener) {
        listeners.add(listener);
    }

    public void removeSimulatorListener(SimulatorListener listener) {
        listeners.remove(listener);
    }

    private void firePropagationCompleted() {
        SimulatorEvent event = new SimulatorEvent(this);
        for (SimulatorListener listener : new ArrayList<>(listeners)) {
            listener.propagationCompleted(event);
        }
    }

    private void fireTickCompleted() {
        SimulatorEvent event = new SimulatorEvent(this);
        for (SimulatorListener listener : new ArrayList<>(listeners)) {
            listener.tickCompleted(event);
        }
    }

    private void fireSimulatorStateChanged() {
        SimulatorEvent event = new SimulatorEvent(this);
        for (SimulatorListener listener : new ArrayList<>(listeners)) {
            listener.simulatorStateChanged(event);
        }
    }

    class PropagationManager extends Thread {

        // These variables apply only if PRINT_TICK_RATE is set
        int tickRateTicks = 0;
        long tickRateStart = System.currentTimeMillis();
        private Propagator propagator = null;
        private PropagationPoints stepPoints = new PropagationPoints();
        private volatile int ticksRequested = 0;
        private volatile int stepsRequested = 0;
        private volatile boolean isResetRequested = false;
        private volatile boolean propagateRequested = false;
        private volatile boolean complete = false;

        private Propagator getPropagator() {
            return propagator;
        }

        private void setPropagator(Propagator value) {
            propagator = value;
        }

        private synchronized void requestPropagate() {
            if (!propagateRequested) {
                propagateRequested = true;
                notifyAll();
            }
        }

        private synchronized void requestReset() {
            if (!isResetRequested) {
                isResetRequested = true;
                notifyAll();
            }
        }

        public synchronized void requestTick() {
            if (ticksRequested < 16) {
                ticksRequested++;
            }
            notifyAll();
        }

        private synchronized void shutDown() {
            complete = true;
            notifyAll();
        }

        @Override
        public void run() {
            while (!complete) {
                synchronized (this) {
                    while (!complete && !propagateRequested
                            && !isResetRequested && ticksRequested == 0
                            && stepsRequested == 0) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

                if (isResetRequested) {
                    isResetRequested = false;
                    if (propagator != null) {
                        propagator.reset();
                    }
                    firePropagationCompleted();
                    //noinspection NonAtomicOperationOnVolatileField
                    propagateRequested = propagateRequested | isRunning;
                }

                if (propagateRequested || ticksRequested > 0 || stepsRequested > 0) {
                    boolean ticked = false;
                    propagateRequested = false;
                    if (isRunning) {
                        stepPoints.clear();
                        stepsRequested = 0;
                        if (propagator == null) {
                            ticksRequested = 0;
                        } else {
                            ticked = ticksRequested > 0;
                            if (ticked) {
                                doTick();
                            }
                            do {
                                propagateRequested = false;
                                try {
                                    isExceptionEncountered = false;
                                    propagator.propagate();
                                } catch (Throwable thr) {
                                    thr.printStackTrace();
                                    isExceptionEncountered = true;
                                    setIsRunning(false);
                                }
                            } while (propagateRequested);
                            if (isOscillating()) {
                                setIsRunning(false);
                                ticksRequested = 0;
                                propagateRequested = false;
                            }
                        }
                    } else {
                        if (stepsRequested > 0) {
                            if (ticksRequested > 0) {
                                ticksRequested = 1;
                                doTick();
                            }

                            synchronized (this) {
                                stepsRequested--;
                            }
                            isExceptionEncountered = false;
                            try {
                                stepPoints.clear();
                                propagator.step(stepPoints);
                            } catch (Throwable t) {
                                t.printStackTrace();
                                isExceptionEncountered = true;
                            }
                        }
                    }
                    if (ticked) {
                        fireTickCompleted();
                    }
                    firePropagationCompleted();
                }
            }
        }

        private void doTick() {
            synchronized (this) {
                ticksRequested--;
            }
            propagator.tick();
        }
    }
}

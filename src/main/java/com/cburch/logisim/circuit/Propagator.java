/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Options;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Random;

public class Propagator {

    private static int lastId = 0;
    private int id = lastId++;
    private CircuitState root; // root of state tree
    /**
     * The number of clock cycles to let pass before deciding that the
     * circuit is oscillating.
     */
    private static final int SIM_LIMIT = 1000;
    /**
     * On average, one out of every 2**simRandomShift propagations
     * through a component is delayed one step more than the component
     * requests. This noise is intended to address some circuits that would
     * otherwise oscillate within Logisim (though they wouldn't oscillate in
     * practice).
     */
    private volatile int simulateRandomShift;
    private PriorityQueue<SetData> toProcess = new PriorityQueue<>();
    private int clock = 0;
    private boolean isOscillating = false;
    private boolean oscillatorAdding = false;
    private PropagationPoints oscillatorPoints = new PropagationPoints();
    private int ticks = 0;
    private Random noiseSource = new Random();
    private int noiseCount = 0;
    private int setDataSerialNumber = 0;

    public Propagator(CircuitState root) {
        this.root = root;
        Listener l = new Listener(this);
        root.getProject().getOptions().getAttributeSet().addAttributeListener(l);
        updateRandomness();
    }

    //
    // static methods
    //
    static Value computeValue(SetData causes) {
        if (causes == null) {
            return Value.NIL;
        }
        Value value = causes.value;
        for (SetData data = causes.next; data != null; data = data.next) {
            value = value.combine(data.value);
        }
        return value;
    }

    private void updateRandomness() {
        Options options = root.getProject().getOptions();
        int value = options.getAttributeSet().getValue(Options.SIMULATOR_RANDOM_ATTRIBUTE);
        int logValue = 0;
        while ((1 << logValue) < value) {
            logValue++;
        }
        simulateRandomShift = logValue;
    }

    public boolean isOscillating() {
        return isOscillating;
    }

    @Override
    public String toString() {
        return "Prop" + id;
    }

    public void drawOscillatingPoints(ComponentDrawContext context) {
        if (isOscillating) {
            oscillatorPoints.draw(context);
        }
    }

    //
    // public methods
    //
    CircuitState getRootState() {
        return root;
    }

    void reset() {
        toProcess.clear();
        root.reset();
        isOscillating = false;
    }

    public void propagate() {
        oscillatorPoints.clear();
        clearDirtyPoints();
        clearDirtyComponents();

        int oscillatorThreshold = SIM_LIMIT;
        int logThreshold = 3 * oscillatorThreshold / 4;
        int count = 0;
        while (!toProcess.isEmpty()) {
            count++;

            if (count < logThreshold) {
                stepInternal(null);
            } else if (count < oscillatorThreshold) {
                oscillatorAdding = true;
                stepInternal(oscillatorPoints);
            } else {
                isOscillating = true;
                oscillatorAdding = false;
                return;
            }
        }
        isOscillating = false;
        oscillatorAdding = false;
        oscillatorPoints.clear();
    }

    void step(PropagationPoints changedPoints) {
        oscillatorPoints.clear();
        clearDirtyPoints();
        clearDirtyComponents();

        PropagationPoints oldOscillator = oscillatorPoints;
        oscillatorAdding = changedPoints != null;
        oscillatorPoints = changedPoints;
        stepInternal(changedPoints);
        oscillatorAdding = false;
        oscillatorPoints = oldOscillator;
    }

    private void stepInternal(PropagationPoints changedPoints) {
        if (toProcess.isEmpty()) {
            return;
        }

        // update clock
        clock = toProcess.peek().time;

        // propagate all values for this clock tick
        HashMap<CircuitState, HashSet<ComponentPoint>> visited = new HashMap<>();
        while (true) {
            SetData data = toProcess.peek();
            if (data == null || data.time != clock) {
                break;
            }
            toProcess.remove();
            CircuitState state = data.state;

            // if it's already handled for this clock tick, continue
            HashSet<ComponentPoint> handled = visited.get(state);
            if (handled != null) {
                if (!handled.add(new ComponentPoint(data.cause, data.location))) {
                    continue;
                }
            } else {
                handled = new HashSet<>();
                visited.put(state, handled);
                handled.add(new ComponentPoint(data.cause, data.location));
            }

			// TODO: DEBUGGING - comment out
			Simulator.log(data.time + ": proc " + data.location + " in "
					+ data.state + " to " + data.value
					+ " by " + data.cause); // */

            if (changedPoints != null) {
                changedPoints.add(state, data.location);
            }

            // change the information about value
            SetData oldHead = state.causes.get(data.location);
            Value oldValue = computeValue(oldHead);
            SetData newHead = addCause(state, oldHead, data);
            Value newValue = computeValue(newHead);

            // if the value at point has changed, propagate it
            if (!newValue.equals(oldValue)) {
                state.markPointAsDirty(data.location);
            }
        }

        clearDirtyPoints();
        clearDirtyComponents();
    }

    boolean isPending() {
        return !toProcess.isEmpty();
    }

    void locationTouched(CircuitState state, Location location) {
        if (oscillatorAdding) {
            oscillatorPoints.add(state, location);
        }
    }

    //
    // package-protected helper methods
    //
    void setValue(CircuitState state, Location point, Value value,
            Component cause, int delay) {
        if (cause instanceof Wire || cause instanceof Splitter) {
            return;
        }
        if (delay <= 0) {
            delay = 1;
        }
        int randomShift = simulateRandomShift;
        if (randomShift > 0) {
            // random noise is turned on
            // multiply the delay by 32 so that the random noise
            // only changes the delay by 3%.
            delay <<= randomShift;
            if (!(cause.getFactory() instanceof SubcircuitFactory)) {
                if (noiseCount > 0) {
                    noiseCount--;
                } else {
                    delay++;
                    noiseCount = noiseSource.nextInt(1 << randomShift);
                }
            }
        }
        toProcess.add(new SetData(clock + delay, setDataSerialNumber,
                state, point, cause, value));
		// TODO: DEBUGGING - comment out
		Simulator.log(String.format("%d: set %s in %s to %s by %s after %d",
                clock, point, state, value, cause, delay));
		         //*/

        setDataSerialNumber++;
    }

	// TODO: for the SimulatorPrototype class
	void step() {
		clock++;
		
		// propagate all values for this clock tick
		HashMap<CircuitState, HashSet<ComponentPoint>> visited = new HashMap<>(); // State -> set of ComponentPoints handled
		while (!toProcess.isEmpty()) {
			SetData data;
			data = toProcess.peek();
			if (data.time != clock) break;
			toProcess.remove();
			CircuitState state = data.state;

			// if it's already handled for this clock tick, continue
			HashSet<ComponentPoint> handled = visited.get(state);
			if (handled != null) {
				if (!handled.add(new ComponentPoint(data.cause, data.location))) {
                    continue;
                }
			} else {
				handled = new HashSet<>();
				visited.put(state, handled);
				handled.add(new ComponentPoint(data.cause, data.location));
			}
			
			if (oscillatorAdding) oscillatorPoints.add(state, data.location);

			// change the information about value
			SetData oldHead = state.causes.get(data.location);
			Value   oldVal  = computeValue(oldHead);
			SetData newHead = addCause(state, oldHead, data);
			Value   newVal  = computeValue(newHead);

			// if the value at point has changed, propagate it
			if (!newVal.equals(oldVal)) {
				state.markPointAsDirty(data.location);
			}
		}

		clearDirtyPoints();
		clearDirtyComponents();
	}
	// */

    public boolean tick() {
        ticks++;
        return root.tick(ticks);
    }

    public int getTickCount() {
        return ticks;
    }

    //
    // private methods
    //
    void checkComponentEnds(CircuitState state, Component comp) {
        for (EndData end : comp.getEnds()) {
            Location endLocation = end.getLocation();
            SetData oldHead = state.causes.get(endLocation);
            Value oldValue = computeValue(oldHead);
            SetData newHead = removeCause(state, oldHead, endLocation, comp);
            Value newValue = computeValue(newHead);
            Value wireValue = state.getValueByWire(endLocation);

            if (!newValue.equals(oldValue) || wireValue != null) {
                state.markPointAsDirty(endLocation);
            }
            if (wireValue != null) {
                state.setValueByWire(endLocation, Value.NIL);
            }
        }
    }

    private void clearDirtyPoints() {
        root.processDirtyPoints();
    }

    private void clearDirtyComponents() {
        root.processDirtyComponents();
    }

    private SetData addCause(CircuitState state, SetData head,
            SetData data) {
        if (data.value == null) { // actually, it should be removed
            return removeCause(state, head, data.location, data.cause);
        }

        HashMap<Location, SetData> causes = state.causes;

        // first check whether this is change of previous info.
        boolean replaced = false;
        for (SetData setData = head; setData != null; setData = setData.next) {
            if (setData.cause == data.cause) {
                setData.value = data.value;
                replaced = true;
                break;
            }
        }

        // otherwise, insert to list of causes
        if (!replaced) {
            if (head == null) {
                causes.put(data.location, data);
                head = data;
            } else {
                data.next = head.next;
                head.next = data;
            }
        }

        return head;
    }

    private SetData removeCause(CircuitState state, SetData head,
            Location location, Component cause) {
        HashMap<Location, SetData> causes = state.causes;
        if (head != null) {
            if (head.cause == cause) {
                head = head.next;
                if (head == null) {
                    causes.remove(location);
                } else {
                    causes.put(location, head);
                }
            } else {
                SetData previous = head;
                SetData current = head.next;
                while (current != null) {
                    if (current.cause == cause) {
                        previous.next = current.next;
                        break;
                    }
                    previous = current;
                    current = current.next;
                }
            }
        }
        return head;
    }

    static class SetData implements Comparable<SetData> {
        private int time;
        private int serialNumber;
        private CircuitState state; // state of circuit containing component
        private Component cause;    // component emitting the value
        private Location location;  // the location at which value is emitted
        private Value value;        // value being emitted
        private SetData next = null;

        private SetData(int time, int serialNumber, CircuitState state,
                Location location, Component cause, Value value) {
            this.time = time;
            this.serialNumber = serialNumber;
            this.state = state;
            this.cause = cause;
            this.location = location;
            this.value = value;
        }

        public int compareTo(SetData setData) {
            // Yes, these subtractions may overflow. This is intentional, as it
            // avoids potential wraparound problems as the counters increment.
            int returnTime = this.time - setData.time;
            if (returnTime != 0) {
                return returnTime;
            }
            return this.serialNumber - setData.serialNumber;
        }

        public SetData cloneFor(CircuitState newState) {
            Propagator newProp = newState.getPropagator();
            int clockTime = newProp.clock - state.getPropagator().clock;
            SetData setData = new SetData(this.time + clockTime,
                    newProp.setDataSerialNumber, newState, location, cause, value);
            newProp.setDataSerialNumber++;
            if (this.next != null) {
                setData.next = this.next.cloneFor(newState);
            }
            return setData;
        }

        @Override
        public String toString() {
            return location + ":" + value + "(" + cause + ")";
        }
    }

    private static class ComponentPoint {

        private Component cause;
        private Location location;

        private ComponentPoint(Component cause, Location location) {
            this.cause = cause;
            this.location = location;
        }

        @Override
        public int hashCode() {
            return 31 * cause.hashCode() + location.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof ComponentPoint)) {
                return false;
            }
            ComponentPoint componentPoint = (ComponentPoint) object;
            return this.cause.equals(componentPoint.cause) && this.location.equals(componentPoint.location);
        }
    }

    private static class Listener implements AttributeListener {

        private WeakReference<Propagator> propagators;

        private Listener(Propagator propagator) {
            propagators = new WeakReference<>(propagator);
        }

        public void attributeListChanged(AttributeEvent event) { }

        public void attributeValueChanged(AttributeEvent event) {
            Propagator propagator = propagators.get();
            if (propagator == null) {
                event.getSource().removeAttributeListener(this);
            } else if (event.getAttribute().equals(Options.SIMULATOR_RANDOM_ATTRIBUTE)) {
                propagator.updateRandomness();
            }
        }
    }

}

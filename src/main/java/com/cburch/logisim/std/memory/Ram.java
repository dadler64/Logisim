/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Ram extends Mem {

    static final AttributeOption BUS_COMBINED
        = new AttributeOption("combined", Strings.getter("ramBusSynchCombined"));
    static final AttributeOption BUS_ASYNCH
        = new AttributeOption("asynch", Strings.getter("ramBusAsynchCombined"));
    static final AttributeOption BUS_SEPARATE
        = new AttributeOption("separate", Strings.getter("ramBusSeparate"));

    static final Attribute<AttributeOption> ATTR_BUS
        = Attributes.forOption("bus", Strings.getter("ramBusAttr"), new AttributeOption[]{BUS_COMBINED, BUS_ASYNCH,
        BUS_SEPARATE});
    private static final int OE = MEM_INPUTS + 0;
    private static final int CLR = MEM_INPUTS + 1;
    private static final int CLK = MEM_INPUTS + 2;
    private static final int WE = MEM_INPUTS + 3;
    private static final int DIN = MEM_INPUTS + 4;
    private static final Attribute<?>[] ATTRIBUTES = {
        Mem.ADDR_ATTR, Mem.DATA_ATTR, ATTR_BUS
    };
    private static final Object[] DEFAULTS = {
        BitWidth.create(8), BitWidth.create(8), BUS_COMBINED
    };
    private static final Object[][] logOptions = new Object[9][];

    public Ram() {
        super("RAM", Strings.getter("ramComponent"), 3);
        setIconName("ram.gif");
        setInstanceLogger(Logger.class);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        super.configureNewInstance(instance);
        instance.addAttributeListener();
    }

    @Override
    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
        super.instanceAttributeChanged(instance, attribute);
        configurePorts(instance);
    }

    @Override
    void configurePorts(Instance instance) {
        Object bus = instance.getAttributeValue(ATTR_BUS);
        if (bus == null) {
            bus = BUS_COMBINED;
        }
        boolean asynch = bus != null && bus.equals(BUS_ASYNCH);
        boolean separate = bus != null && bus.equals(BUS_SEPARATE);

        int portCount = MEM_INPUTS;
        if (asynch) {
            portCount += 2;
        } else if (separate) {
            portCount += 5;
        } else {
            portCount += 3;
        }
        Port[] ps = new Port[portCount];

        configureStandardPorts(instance, ps);
        ps[OE] = new Port(-50, 40, Port.INPUT, 1);
        ps[OE].setToolTip(Strings.getter("ramOETip"));
        ps[CLR] = new Port(-30, 40, Port.INPUT, 1);
        ps[CLR].setToolTip(Strings.getter("ramClrTip"));
        if (!asynch) {
            ps[CLK] = new Port(-70, 40, Port.INPUT, 1);
            ps[CLK].setToolTip(Strings.getter("ramClkTip"));
        }
        if (separate) {
            ps[WE] = new Port(-110, 40, Port.INPUT, 1);
            ps[WE].setToolTip(Strings.getter("ramWETip"));
            ps[DIN] = new Port(-140, 20, Port.INPUT, DATA_ATTR);
            ps[DIN].setToolTip(Strings.getter("ramInTip"));
        } else {
            ps[DATA].setToolTip(Strings.getter("ramBusTip"));
        }
        instance.setPorts(ps);
    }

    @Override
    public AttributeSet createAttributeSet() {
        return AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
    }

    @Override
    MemState getState(InstanceState state) {
        BitWidth addressBits = state.getAttributeValue(ADDR_ATTR);
        BitWidth dataBits = state.getAttributeValue(DATA_ATTR);

        RamState myState = (RamState) state.getData();
        if (myState == null) {
            MemContents contents = MemContents.create(addressBits.getWidth(), dataBits.getWidth());
            Instance instance = state.getInstance();
            myState = new RamState(instance, contents, new MemListener(instance));
            state.setData(myState);
        } else {
            myState.setRam(state.getInstance());
        }
        return myState;
    }

    @Override
    MemState getState(Instance instance, CircuitState state) {
        BitWidth addressBits = instance.getAttributeValue(ADDR_ATTR);
        BitWidth dataBits = instance.getAttributeValue(DATA_ATTR);

        RamState myState = (RamState) instance.getData(state);
        if (myState == null) {
            MemContents contents = MemContents.create(addressBits.getWidth(), dataBits.getWidth());
            myState = new RamState(instance, contents, new MemListener(instance));
            instance.setData(state, myState);
        } else {
            myState.setRam(instance);
        }
        return myState;
    }

    @Override
    HexFrame getHexFrame(Project project, Instance instance, CircuitState circuitState) {
        RamState state = (RamState) getState(instance, circuitState);
        return state.getHexFrame(project);
    }

    @Override
    public void propagate(InstanceState state) {
        RamState myState = (RamState) getState(state);
        BitWidth dataBits = state.getAttributeValue(DATA_ATTR);
        Object busValue = state.getAttributeValue(ATTR_BUS);
        boolean isAsynch = busValue != null && busValue.equals(BUS_ASYNCH);
        boolean isSeparate = busValue != null && busValue.equals(BUS_SEPARATE);

        Value addressValue = state.getPort(ADDR);
        boolean chipSelect = state.getPort(CS) != Value.FALSE;
        boolean isTriggered = isAsynch || myState.setClock(state.getPort(CLK), StdAttr.TRIG_RISING);
        boolean outputEnabled = state.getPort(OE) != Value.FALSE;
        boolean shouldClear = state.getPort(CLR) == Value.TRUE;

        if (shouldClear) {
            myState.getContents().clear();
        }

        if (!chipSelect) {
            myState.setCurrent(-1);
            state.setPort(DATA, Value.createUnknown(dataBits), DELAY);
            return;
        }

        int address = addressValue.toIntValue();
        if (!addressValue.isFullyDefined() || address < 0) {
            return;
        }
        if (address != myState.getCurrent()) {
            myState.setCurrent(address);
            myState.scrollToShow(address);
        }

        if (!shouldClear && isTriggered) {
            boolean shouldStore;
            if (isSeparate) {
                shouldStore = state.getPort(WE) != Value.FALSE;
            } else {
                shouldStore = !outputEnabled;
            }
            if (shouldStore) {
                Value dataValue = state.getPort(isSeparate ? DIN : DATA);
                myState.getContents().set(address, dataValue.toIntValue());
            }
        }

        if (outputEnabled) {
            int value = myState.getContents().get(address);
            state.setPort(DATA, Value.createKnown(dataBits, value), DELAY);
        } else {
            state.setPort(DATA, Value.createUnknown(dataBits), DELAY);
        }
    }

    @Override
    public void paintInstance(InstancePainter painter) {
        super.paintInstance(painter);
        Object busValue = painter.getAttributeValue(ATTR_BUS);
        boolean isAsynch = busValue != null && busValue.equals(BUS_ASYNCH);
        boolean isSeparate = busValue != null && busValue.equals(BUS_SEPARATE);

        if (!isAsynch) {
            painter.drawClock(CLK, Direction.NORTH);
        }
        painter.drawPort(OE, Strings.get("ramOELabel"), Direction.SOUTH);
        painter.drawPort(CLR, Strings.get("ramClrLabel"), Direction.SOUTH);

        if (isSeparate) {
            painter.drawPort(WE, Strings.get("ramWELabel"), Direction.SOUTH);
            painter.getGraphics().setColor(Color.BLACK);
            painter.drawPort(DIN, Strings.get("ramDataLabel"), Direction.EAST);
        }
    }

    private static class RamState extends MemState implements InstanceData, AttributeListener {

        private final MemListener listener;
        private Instance parent;
        private HexFrame hexFrame = null;
        private ClockState clockState;

        RamState(Instance parent, MemContents contents, MemListener listener) {
            super(contents);
            this.parent = parent;
            this.listener = listener;
            this.clockState = new ClockState();
            if (parent != null) {
                parent.getAttributeSet().addAttributeListener(this);
            }
            contents.addHexModelListener(listener);
        }

        void setRam(Instance value) {
            if (parent == value) {
                return;
            }
            if (parent != null) {
                parent.getAttributeSet().removeAttributeListener(this);
            }
            parent = value;
            if (value != null) {
                value.getAttributeSet().addAttributeListener(this);
            }
        }

        @Override
        public RamState clone() {
            RamState state = (RamState) super.clone();
            state.parent = null;
            state.clockState = this.clockState.clone();
            state.getContents().addHexModelListener(listener);
            return state;
        }

        // Retrieves a HexFrame for editing within a separate window
        public HexFrame getHexFrame(Project project) {
            if (hexFrame == null) {
                hexFrame = new HexFrame(project, getContents());
                hexFrame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        hexFrame = null;
                    }
                });
            }
            return hexFrame;
        }

        //
        // methods for accessing the write-enable data
        //
        public boolean setClock(Value newClock, Object trigger) {
            return clockState.updateClock(newClock, trigger);
        }

        public void attributeListChanged(AttributeEvent e) {
        }

        public void attributeValueChanged(AttributeEvent e) {
            AttributeSet attrs = e.getSource();
            BitWidth addressBits = attrs.getValue(Mem.ADDR_ATTR);
            BitWidth dataBits = attrs.getValue(Mem.DATA_ATTR);
            getContents().setDimensions(addressBits.getWidth(), dataBits.getWidth());
        }
    }

    public static class Logger extends InstanceLogger {

        @Override
        public Object[] getLogOptions(InstanceState state) {
            int addressBits = state.getAttributeValue(ADDR_ATTR).getWidth();
            if (addressBits >= logOptions.length) {
                addressBits = logOptions.length - 1;
            }
            synchronized (logOptions) {
                Object[] options = logOptions[addressBits];
                if (options == null) {
                    options = new Object[1 << addressBits];
                    logOptions[addressBits] = options;
                    for (int i = 0; i < options.length; i++) {
                        options[i] = i;
                    }
                }
                return options;
            }
        }

        @Override
        public String getLogName(InstanceState state, Object option) {
            if (option instanceof Integer) {
                String display = Strings.get("ramComponent");
                Location loc = state.getInstance().getLocation();
                return display + loc + "[" + option + "]";
            } else {
                return null;
            }
        }

        @Override
        public Value getLogValue(InstanceState state, Object option) {
            if (option instanceof Integer) {
                MemState memState = (MemState) state.getData();
                int address = (Integer) option;
                return Value.createKnown(BitWidth.create(memState.getDataBits()), memState.getContents().get(address));
            } else {
                return Value.NIL;
            }
        }
    }
}

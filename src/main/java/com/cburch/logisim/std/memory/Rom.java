/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.adlerd.logger.Logger;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import javax.swing.JLabel;

public class Rom extends Mem {

    public static Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();

    // The following is so that instance's MemListeners aren't freed by the
    // garbage collector until the instance itself is ready to be freed.
    private final WeakHashMap<Instance, MemListener> memListeners;

    public Rom() {
        super("ROM", Strings.getter("romComponent"), 0);
        setIconName("rom.gif");
        memListeners = new WeakHashMap<>();
    }

    @Override
    void configurePorts(Instance instance) {
        Port[] ports = new Port[MEM_INPUTS];
        configureStandardPorts(instance, ports);
        instance.setPorts(ports);
    }

    @Override
    public AttributeSet createAttributeSet() {
        return new RomAttributes();
    }

    @Override
    MemState getState(Instance instance, CircuitState state) {
        MemState memState = (MemState) instance.getData(state);
        if (memState == null) {
            MemContents contents = getMemContents(instance);
            memState = new MemState(contents);
            instance.setData(state, memState);
        }
        return memState;
    }

    @Override
    MemState getState(InstanceState state) {
        MemState memState = (MemState) state.getData();
        if (memState == null) {
            MemContents contents = getMemContents(state.getInstance());
            memState = new MemState(contents);
            state.setData(memState);
        }
        return memState;
    }

    @Override
    HexFrame getHexFrame(Project project, Instance instance, CircuitState state) {
        return RomAttributes.getHexFrame(getMemContents(instance), project);
    }

    // TODO - maybe delete this method?
    MemContents getMemContents(Instance instance) {
        return instance.getAttributeValue(CONTENTS_ATTR);
    }

    @Override
    public void propagate(InstanceState state) {
        MemState myState = getState(state);
        BitWidth dataBits = state.getAttributeValue(DATA_ATTR);

        Value addressValue = state.getPort(ADDR);
        boolean chipSelect = state.getPort(CS) != Value.FALSE;

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

        int value = myState.getContents().get(address);
        state.setPort(DATA, Value.createKnown(dataBits, value), DELAY);
    }

    @Override
    protected void configureNewInstance(Instance instance) {
        super.configureNewInstance(instance);
        MemContents contents = getMemContents(instance);
        MemListener listener = new MemListener(instance);
        memListeners.put(instance, listener);
        contents.addHexModelListener(listener);
    }

    private static class ContentsAttribute extends Attribute<MemContents> {

        public ContentsAttribute() {
            super("contents", Strings.getter("romContentsAttr"));
        }

        @Override
        public java.awt.Component getCellEditor(Window source, MemContents value) {
            if (source instanceof Frame) {
                Project project = ((Frame) source).getProject();
                RomAttributes.register(value, project);
            }
            ContentsCell cell = new ContentsCell(source, value);
            cell.mouseClicked(null);
            return cell;
        }

        @Override
        public String toDisplayString(MemContents value) {
            return Strings.get("romContentsValue");
        }

        @Override
        public String toStandardString(MemContents state) {
            int address = state.getLogLength();
            int data = state.getWidth();
            StringWriter writer = new StringWriter();
            writer.write("address/data: " + address + " " + data + "\n");
            try {
                HexFile.save(writer, state);
            } catch (IOException e) {
                Logger.debugln(e.getMessage());
            }
            return writer.toString();
        }

        @Override
        public MemContents parse(String value) {
            int lineBreak = value.indexOf('\n');
            String first = lineBreak < 0 ? value : value.substring(0, lineBreak);
            String rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
            StringTokenizer tokens = new StringTokenizer(first);
            try {
                String header = tokens.nextToken();
                if (!header.equals("addr/data:")) {
                    return null;
                }
                int address = Integer.parseInt(tokens.nextToken());
                int data = Integer.parseInt(tokens.nextToken());
                MemContents ret = MemContents.create(address, data);
                HexFile.open(ret, new StringReader(rest));
                return ret;
            } catch (IOException | NumberFormatException | NoSuchElementException e) {
                return null;
            }
        }
    }

    private static class ContentsCell extends JLabel implements MouseListener {

        Window source;
        MemContents contents;

        ContentsCell(Window source, MemContents contents) {
            super(Strings.get("romContentsValue"));
            this.source = source;
            this.contents = contents;
            addMouseListener(this);
        }

        public void mouseClicked(MouseEvent e) {
            if (contents == null) {
                return;
            }
            Project proj = source instanceof Frame ? ((Frame) source).getProject() : null;
            HexFrame frame = RomAttributes.getHexFrame(contents, proj);
            frame.setVisible(true);
            frame.toFront();
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }
    }
}

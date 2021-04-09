/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.proj.Project;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;

class RomAttributes extends AbstractAttributeSet {

    private static final List<Attribute<?>> ATTRIBUTES = Arrays.asList(Mem.ADDR_ATTR, Mem.DATA_ATTR, Rom.CONTENTS_ATTR);
    private static final WeakHashMap<MemContents, RomContentsListener> listenerRegistry = new WeakHashMap<>();
    private static final WeakHashMap<MemContents, HexFrame> windowRegistry = new WeakHashMap<>();
    private BitWidth addressBits = BitWidth.create(8);
    private BitWidth dataBits = BitWidth.create(8);
    private MemContents contents;

    RomAttributes() {
        contents = MemContents.create(addressBits.getWidth(), dataBits.getWidth());
    }

    static void register(MemContents value, Project project) {
        if (project == null || listenerRegistry.containsKey(value)) {
            return;
        }
        RomContentsListener l = new RomContentsListener(project);
        value.addHexModelListener(l);
        listenerRegistry.put(value, l);
    }

    static HexFrame getHexFrame(MemContents value, Project project) {
        synchronized (windowRegistry) {
            HexFrame frame = windowRegistry.get(value);
            if (frame == null) {
                frame = new HexFrame(project, value);
                windowRegistry.put(value, frame);
            }
            return frame;
        }
    }

    void setProject(Project project) {
        register(contents, project);
    }

    @Override
    protected void copyInto(AbstractAttributeSet destination) {
        RomAttributes romAttributes = (RomAttributes) destination;
        romAttributes.addressBits = addressBits;
        romAttributes.dataBits = dataBits;
        romAttributes.contents = contents.clone();
    }

    @Override
    public List<Attribute<?>> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
        if (attr == Mem.ADDR_ATTR) {
            return (V) addressBits;
        }
        if (attr == Mem.DATA_ATTR) {
            return (V) dataBits;
        }
        if (attr == Rom.CONTENTS_ATTR) {
            return (V) contents;
        }
        return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
        if (attr == Mem.ADDR_ATTR) {
            addressBits = (BitWidth) value;
            contents.setDimensions(addressBits.getWidth(), dataBits.getWidth());
        } else if (attr == Mem.DATA_ATTR) {
            dataBits = (BitWidth) value;
            contents.setDimensions(addressBits.getWidth(), dataBits.getWidth());
        } else if (attr == Rom.CONTENTS_ATTR) {
            contents = (MemContents) value;
        }
        fireAttributeValueChanged(attr, value);
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Attributes;

public class Options {

    public static final AttributeOption GATE_UNDEFINED_IGNORE
        = new AttributeOption("ignore", Strings.getter("gateUndefinedIgnore"));
    public static final AttributeOption GATE_UNDEFINED_ERROR
        = new AttributeOption("error", Strings.getter("gateUndefinedError"));
    public static final Attribute<Integer> SIMULATOR_LIMIT_ATTRIBUTE
        = Attributes.forInteger("simlimit", Strings.getter("simLimitOption"));
    public static final Attribute<Integer> SIMULATOR_RANDOM_ATTRIBUTE
        = Attributes.forInteger("simrand", Strings.getter("simRandomOption"));
    public static final Attribute<AttributeOption> ATTR_GATE_UNDEFINED
        = Attributes.forOption("gateUndefined", Strings.getter("gateUndefinedOption"),
        new AttributeOption[]{GATE_UNDEFINED_IGNORE, GATE_UNDEFINED_ERROR});
    public static final Integer SIMULATOR_RANDOM_DEFAULT = 32;
    private static final Attribute<?>[] ATTRIBUTES = {
        ATTR_GATE_UNDEFINED, SIMULATOR_LIMIT_ATTRIBUTE, SIMULATOR_RANDOM_ATTRIBUTE
    };
    private static final Object[] DEFAULTS = {GATE_UNDEFINED_IGNORE, 1000, 0};

    private final AttributeSet attributeSet;
    private final MouseMappings mouseMappings;
    private final ToolbarData toolbarData;

    public Options() {
        attributeSet = AttributeSets.fixedSet(ATTRIBUTES, DEFAULTS);
        mouseMappings = new MouseMappings();
        toolbarData = new ToolbarData();
    }

    public AttributeSet getAttributeSet() {
        return attributeSet;
    }

    public MouseMappings getMouseMappings() {
        return mouseMappings;
    }

    public ToolbarData getToolbarData() {
        return toolbarData;
    }

    public void copyFrom(Options other, LogisimFile destination) {
        AttributeSets.copy(other.attributeSet, this.attributeSet);
        this.toolbarData.copyFrom(other.toolbarData, destination);
        this.mouseMappings.copyFrom(other.mouseMappings, destination);
    }
}

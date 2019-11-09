/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import java.awt.Font;

public interface StdAttr {

    Attribute<Direction> FACING
            = Attributes.forDirection("facing", Strings.getter("stdFacingAttr"));

    Attribute<BitWidth> WIDTH
            = Attributes.forBitWidth("width", Strings.getter("stdDataWidthAttr"));

    AttributeOption TRIG_RISING
            = new AttributeOption("rising", Strings.getter("stdTriggerRising"));
    AttributeOption TRIG_FALLING
            = new AttributeOption("falling", Strings.getter("stdTriggerFalling"));
    AttributeOption TRIG_HIGH
            = new AttributeOption("high", Strings.getter("stdTriggerHigh"));
    AttributeOption TRIG_LOW
            = new AttributeOption("low", Strings.getter("stdTriggerLow"));
    Attribute<AttributeOption> TRIGGER
            = Attributes.forOption("trigger", Strings.getter("stdTriggerAttr"),
            new AttributeOption[]{
                    TRIG_RISING, TRIG_FALLING, TRIG_HIGH, TRIG_LOW
            });
    Attribute<AttributeOption> EDGE_TRIGGER
            = Attributes.forOption("trigger", Strings.getter("stdTriggerAttr"),
            new AttributeOption[]{TRIG_RISING, TRIG_FALLING});

    Attribute<String> LABEL
            = Attributes.forString("label", Strings.getter("stdLabelAttr"));

    Attribute<Font> LABEL_FONT
            = Attributes.forFont("labelfont", Strings.getter("stdLabelFontAttr"));
    Font DEFAULT_LABEL_FONT
            = new Font("SansSerif", Font.PLAIN, 12);
}

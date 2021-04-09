/* Copyright (c) 2011, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;

public class AttrTableToolModel extends AttributeSetTableModel {

    Project project;
    Tool tool;

    public AttrTableToolModel(Project project, Tool tool) {
        super(tool.getAttributeSet());
        this.project = project;
        this.tool = tool;
    }

    @Override
    public String getTitle() {
        return Strings.get("toolAttrTitle", tool.getDisplayName());
    }

    public Tool getTool() {
        return tool;
    }

    @Override
    public void setValueRequested(Attribute<Object> attribute, Object value) {
        project.doAction(ToolAttributeAction.create(tool, attribute, value));
    }
}

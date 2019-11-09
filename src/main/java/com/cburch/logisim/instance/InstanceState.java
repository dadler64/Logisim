/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public interface InstanceState {

    Instance getInstance();

    InstanceFactory getFactory();

    Project getProject();

    AttributeSet getAttributeSet();

    <E> E getAttributeValue(Attribute<E> attr);

    Value getPort(int portIndex);

    boolean isPortConnected(int portIndex);

    void setPort(int portIndex, Value value, int delay);

    InstanceData getData();

    void setData(InstanceData value);

    void fireInvalidated();

    boolean isCircuitRoot();

    long getTickCount();
}

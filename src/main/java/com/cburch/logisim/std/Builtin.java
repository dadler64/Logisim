/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std;

import com.cburch.logisim.std.arith.Arithmetic;
import com.cburch.logisim.std.base.Base;
import com.cburch.logisim.std.gates.Gates;
import com.cburch.logisim.std.io.Io;
import com.cburch.logisim.std.memory.Memory;
import com.cburch.logisim.std.plexers.Plexers;
import com.cburch.logisim.std.wiring.Wiring;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Builtin extends Library {

    private final List<Library> libraries;

    public Builtin() {
        libraries = Arrays.asList(new Base(),
            new Gates(),
            new Wiring(),
            new Plexers(),
            new Arithmetic(),
            new Memory(),
            new Io());
    }

    @Override
    public String getName() {
        return "Builtin";
    }

    @Override
    public String getDisplayName() {
        return Strings.get("builtinLibrary");
    }

    @Override
    public List<Tool> getTools() {
        return Collections.emptyList();
    }

    @Override
    public List<Library> getLibraries() {
        return libraries;
    }
}

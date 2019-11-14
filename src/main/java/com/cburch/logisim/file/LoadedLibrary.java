/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class LoadedLibrary extends Library implements LibraryEventSource {

    private Library library;
    private boolean isDirty;
    private MyListener myListener;
    private EventSourceWeakSupport<LibraryListener> listeners;

    LoadedLibrary(Library library) {
        isDirty = false;
        myListener = new MyListener();
        listeners = new EventSourceWeakSupport<>();

        while (library instanceof LoadedLibrary) {
            library = ((LoadedLibrary) library).library;
        }
        this.library = library;
        if (library instanceof LibraryEventSource) {
            ((LibraryEventSource) library).addLibraryListener(myListener);
        }
    }

    private static void replaceAll(Map<ComponentFactory, ComponentFactory> componentMap,
            Map<Tool, Tool> toolMap) {
        for (Project project : Projects.getOpenProjects()) {
            Tool oldTool = project.getTool();
            Circuit oldCircuit = project.getCurrentCircuit();
            if (toolMap.containsKey(oldTool)) {
                project.setTool(toolMap.get(oldTool));
            }
            SubcircuitFactory oldFactory = oldCircuit.getSubcircuitFactory();
            if (componentMap.containsKey(oldFactory)) {
                SubcircuitFactory newFactory = (SubcircuitFactory) componentMap.get(oldFactory);
                project.setCurrentCircuit(newFactory.getSubcircuit());
            }
            replaceAll(project.getLogisimFile(), componentMap, toolMap);
        }
        for (LogisimFile file : LibraryManager.instance.getLogisimLibraries()) {
            replaceAll(file, componentMap, toolMap);
        }
    }

    private static void replaceAll(LogisimFile file,
            Map<ComponentFactory, ComponentFactory> compMap,
            Map<Tool, Tool> toolMap) {
        file.getOptions().getToolbarData().replaceAll(toolMap);
        file.getOptions().getMouseMappings().replaceAll(toolMap);
        for (Circuit circuit : file.getCircuits()) {
            replaceAll(circuit, compMap);
        }
    }

    private static void replaceAll(Circuit circuit,
            Map<ComponentFactory, ComponentFactory> compMap) {
        ArrayList<Component> toReplace = null;
        for (Component comp : circuit.getNonWires()) {
            if (compMap.containsKey(comp.getFactory())) {
                if (toReplace == null) {
                    toReplace = new ArrayList<>();
                }
                toReplace.add(comp);
            }
        }
        if (toReplace != null) {
            CircuitMutation xn = new CircuitMutation(circuit);
            for (Component comp : toReplace) {
                xn.remove(comp);
                ComponentFactory factory = compMap.get(comp.getFactory());
                if (factory != null) {
                    AttributeSet newAttrs = createAttributes(factory, comp.getAttributeSet());
                    xn.add(factory.createComponent(comp.getLocation(), newAttrs));
                }
            }
            xn.execute();
        }
    }

    private static AttributeSet createAttributes(ComponentFactory factory, AttributeSet src) {
        AttributeSet dest = factory.createAttributeSet();
        copyAttributes(dest, src);
        return dest;
    }

    static void copyAttributes(AttributeSet dest, AttributeSet src) {
        for (Attribute<?> destAttr : dest.getAttributes()) {
            Attribute<?> srcAttr = src.getAttribute(destAttr.getName());
            if (srcAttr != null) {
                @SuppressWarnings("unchecked")
                Attribute<Object> destAttr2 = (Attribute<Object>) destAttr;
                dest.setValue(destAttr2, src.getValue(srcAttr));
            }
        }
    }

    public void addLibraryListener(LibraryListener l) {
        listeners.add(l);
    }

    public void removeLibraryListener(LibraryListener l) {
        listeners.remove(l);
    }

    @Override
    public String getName() {
        return library.getName();
    }

    @Override
    public String getDisplayName() {
        return library.getDisplayName();
    }

    @Override
    public boolean isDirty() {
        return isDirty || library.isDirty();
    }

    void setDirty(boolean value) {
        if (isDirty != value) {
            isDirty = value;
            fireLibraryEvent(LibraryEvent.DIRTY_STATE, isDirty() ? Boolean.TRUE : Boolean.FALSE);
        }
    }

    @Override
    public List<? extends Tool> getTools() {
        return library.getTools();
    }

    @Override
    public List<Library> getLibraries() {
        return library.getLibraries();
    }

    Library getLibrary() {
        return library;
    }

    void setLibrary(Library value) {
        if (library instanceof LibraryEventSource) {
            ((LibraryEventSource) library).removeLibraryListener(myListener);
        }
        Library old = library;
        library = value;
        resolveChanges(old);
        if (library instanceof LibraryEventSource) {
            ((LibraryEventSource) library).addLibraryListener(myListener);
        }
    }

    private void fireLibraryEvent(int action, Object data) {
        fireLibraryEvent(new LibraryEvent(this, action, data));
    }

    private void fireLibraryEvent(LibraryEvent event) {
        if (event.getSource() != this) {
            event = new LibraryEvent(this, event.getAction(), event.getData());
        }
        for (LibraryListener l : listeners) {
            l.libraryChanged(event);
        }
    }

    private void resolveChanges(Library old) {
        if (listeners.isEmpty()) {
            return;
        }

        if (!library.getDisplayName().equals(old.getDisplayName())) {
            fireLibraryEvent(LibraryEvent.SET_NAME, library.getDisplayName());
        }

        HashSet<Library> changes = new HashSet<>(old.getLibraries());
        changes.removeAll(library.getLibraries());
        for (Library lib : changes) {
            fireLibraryEvent(LibraryEvent.REMOVE_LIBRARY, lib);
        }

        changes.clear();
        changes.addAll(library.getLibraries());
        changes.removeAll(old.getLibraries());
        for (Library lib : changes) {
            fireLibraryEvent(LibraryEvent.ADD_LIBRARY, lib);
        }

        HashMap<ComponentFactory, ComponentFactory> componentMap;
        HashMap<Tool, Tool> toolMap;
        componentMap = new HashMap<>();
        toolMap = new HashMap<>();
        for (Tool oldTool : old.getTools()) {
            Tool newTool = library.getTool(oldTool.getName());
            toolMap.put(oldTool, newTool);
            if (oldTool instanceof AddTool) {
                ComponentFactory oldFactory = ((AddTool) oldTool).getFactory();
                if (newTool != null && newTool instanceof AddTool) {
                    ComponentFactory newFactory = ((AddTool) newTool).getFactory();
                    componentMap.put(oldFactory, newFactory);
                } else {
                    componentMap.put(oldFactory, null);
                }
            }
        }
        replaceAll(componentMap, toolMap);

        HashSet<Tool> toolChanges = new HashSet<>(old.getTools());
        toolChanges.removeAll(toolMap.keySet());
        for (Tool tool : toolChanges) {
            fireLibraryEvent(LibraryEvent.REMOVE_TOOL, tool);
        }

        toolChanges = new HashSet<>(library.getTools());
        toolChanges.removeAll(toolMap.values());
        for (Tool tool : toolChanges) {
            fireLibraryEvent(LibraryEvent.ADD_TOOL, tool);
        }
    }

    private class MyListener implements LibraryListener {

        public void libraryChanged(LibraryEvent event) {
            fireLibraryEvent(event);
        }
    }
}

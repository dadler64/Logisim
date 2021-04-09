/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryEventSource;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.LogisimFileActions;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.ProjectExplorer;
import com.cburch.logisim.gui.generic.ProjectExplorerEvent;
import com.cburch.logisim.gui.generic.ProjectExplorerLibraryNode;
import com.cburch.logisim.gui.generic.ProjectExplorerListener;
import com.cburch.logisim.gui.generic.ProjectExplorerToolNode;
import com.cburch.logisim.gui.menu.Popups;
import com.cburch.logisim.gui.menu.ProjectCircuitActions;
import com.cburch.logisim.gui.menu.ProjectLibraryActions;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import javax.swing.JPopupMenu;

class ToolboxManip implements ProjectExplorerListener {

    private final Project project;
    private final ProjectExplorer explorer;
    private final MyListener myListener = new MyListener();
    private Tool lastSelected = null;

    ToolboxManip(Project project, ProjectExplorer explorer) {
        this.project = project;
        this.explorer = explorer;
        project.addProjectListener(myListener);
        myListener.setFile(null, project.getLogisimFile());
    }

    public void selectionChanged(ProjectExplorerEvent event) {
        Object selected = event.getTarget();
        if (selected instanceof ProjectExplorerToolNode) {
            Tool tool = ((ProjectExplorerToolNode) selected).getValue();
            if (tool instanceof AddTool) {
                AddTool addTool = (AddTool) tool;
                ComponentFactory source = addTool.getFactory();
                if (source instanceof SubcircuitFactory) {
                    SubcircuitFactory circFact = (SubcircuitFactory) source;
                    Circuit circ = circFact.getSubcircuit();
                    if (project.getCurrentCircuit() == circ) {
                        AttrTableModel m = new AttrTableCircuitModel(project, circ);
                        project.getFrame().setAttrTableModel(m);
                        return;
                    }
                }
            }

            lastSelected = project.getTool();
            project.setTool(tool);
            project.getFrame().viewAttributes(tool);
        }
    }

    public void doubleClicked(ProjectExplorerEvent event) {
        Object clicked = event.getTarget();
        if (clicked instanceof ProjectExplorerToolNode) {
            Tool baseTool = ((ProjectExplorerToolNode) clicked).getValue();
            if (baseTool instanceof AddTool) {
                AddTool tool = (AddTool) baseTool;
                ComponentFactory source = tool.getFactory();
                if (source instanceof SubcircuitFactory) {
                    SubcircuitFactory circFact = (SubcircuitFactory) source;
                    project.setCurrentCircuit(circFact.getSubcircuit());
                    project.getFrame().setEditorView(Frame.EDIT_LAYOUT);
                    if (lastSelected != null) {
                        project.setTool(lastSelected);
                    }
                }
            }
        }
    }

    public void moveRequested(ProjectExplorerEvent event, AddTool dragged, AddTool target) {
        LogisimFile file = project.getLogisimFile();
        int draggedIndex = file.getTools().indexOf(dragged);
        int targetIndex = file.getTools().indexOf(target);
        if (targetIndex > draggedIndex) {
            targetIndex++;
        }
        project.doAction(LogisimFileActions.moveCircuit(dragged, targetIndex));
    }

    public void deleteRequested(ProjectExplorerEvent event) {
        Object request = event.getTarget();
        if (request instanceof ProjectExplorerLibraryNode) {
            Library lib = ((ProjectExplorerLibraryNode) request).getValue();
            ProjectLibraryActions.doUnloadLibrary(project, lib);
        } else if (request instanceof ProjectExplorerToolNode) {
            Tool tool = ((ProjectExplorerToolNode) request).getValue();
            if (tool instanceof AddTool) {
                ComponentFactory factory = ((AddTool) tool).getFactory();
                if (factory instanceof SubcircuitFactory) {
                    SubcircuitFactory circFact = (SubcircuitFactory) factory;
                    ProjectCircuitActions.doRemoveCircuit(project, circFact.getSubcircuit());
                }
            }
        }
    }

    public JPopupMenu menuRequested(ProjectExplorerEvent event) {
        Object clicked = event.getTarget();
        if (clicked instanceof ProjectExplorerToolNode) {
            Tool baseTool = ((ProjectExplorerToolNode) clicked).getValue();
            if (baseTool instanceof AddTool) {
                AddTool tool = (AddTool) baseTool;
                ComponentFactory source = tool.getFactory();
                if (source instanceof SubcircuitFactory) {
                    Circuit circ = ((SubcircuitFactory) source).getSubcircuit();
                    return Popups.forCircuit(project, tool, circ);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else if (clicked instanceof ProjectExplorerLibraryNode) {
            Library lib = ((ProjectExplorerLibraryNode) clicked).getValue();
            if (lib == project.getLogisimFile()) {
                return Popups.forProject(project);
            } else {
                boolean is_top = event.getTreePath().getPathCount() <= 2;
                return Popups.forLibrary(project, lib, is_top);
            }
        } else {
            return null;
        }
    }

    private class MyListener
        implements ProjectListener, LibraryListener, AttributeListener {

        private LogisimFile curFile = null;

        public void projectChanged(ProjectEvent event) {
            int action = event.getAction();
            if (action == ProjectEvent.ACTION_SET_FILE) {
                setFile((LogisimFile) event.getOldData(),
                    (LogisimFile) event.getData());
                explorer.repaint();
            }
        }

        private void setFile(LogisimFile oldFile, LogisimFile newFile) {
            if (oldFile != null) {
                removeLibrary(oldFile);
                for (Library lib : oldFile.getLibraries()) {
                    removeLibrary(lib);
                }
            }
            curFile = newFile;
            if (newFile != null) {
                addLibrary(newFile);
                for (Library lib : newFile.getLibraries()) {
                    addLibrary(lib);
                }
            }
        }

        public void libraryChanged(LibraryEvent event) {
            int action = event.getAction();
            if (action == LibraryEvent.ADD_LIBRARY) {
                if (event.getSource() == curFile) {
                    addLibrary((Library) event.getData());
                }
            } else if (action == LibraryEvent.REMOVE_LIBRARY) {
                if (event.getSource() == curFile) {
                    removeLibrary((Library) event.getData());
                }
            } else if (action == LibraryEvent.ADD_TOOL) {
                Tool tool = (Tool) event.getData();
                AttributeSet attrs = tool.getAttributeSet();
                if (attrs != null) {
                    attrs.addAttributeListener(this);
                }
            } else if (action == LibraryEvent.REMOVE_TOOL) {
                Tool tool = (Tool) event.getData();
                AttributeSet attrs = tool.getAttributeSet();
                if (attrs != null) {
                    attrs.removeAttributeListener(this);
                }
            }
            explorer.repaint();
        }

        private void addLibrary(Library lib) {
            if (lib instanceof LibraryEventSource) {
                ((LibraryEventSource) lib).addLibraryListener(this);
            }
            for (Tool tool : lib.getTools()) {
                AttributeSet attrs = tool.getAttributeSet();
                if (attrs != null) {
                    attrs.addAttributeListener(this);
                }
            }
        }

        private void removeLibrary(Library lib) {
            if (lib instanceof LibraryEventSource) {
                ((LibraryEventSource) lib).removeLibraryListener(this);
            }
            for (Tool tool : lib.getTools()) {
                AttributeSet attrs = tool.getAttributeSet();
                if (attrs != null) {
                    attrs.removeAttributeListener(this);
                }
            }
        }


        public void attributeListChanged(AttributeEvent e) {
        }

        public void attributeValueChanged(AttributeEvent e) {
            explorer.repaint();
        }

    }

}

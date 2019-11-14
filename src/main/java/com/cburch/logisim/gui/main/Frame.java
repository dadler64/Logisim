/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.main;

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.draw.toolbar.ToolbarModel;
import com.cburch.logisim.Main;
import com.cburch.logisim.Main.Release;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.gui.appear.AppearanceView;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.BasicZoomModel;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.ZoomControl;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.HorizontalSplitPane;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.VerticalSplitPane;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Frame extends LFrame implements LocaleListener {

    public static final String EDITOR_VIEW = "editorView";
    public static final String EXPLORER_VIEW = "explorerView";
    public static final String EDIT_LAYOUT = "layout";
    public static final String EDIT_APPEARANCE = "appearance";
    public static final String VIEW_TOOLBOX = "toolbox";
    public static final String VIEW_SIMULATION = "simulation";

    private static final double[] ZOOM_OPTIONS = {25, 50, 75, 100, 125, 150, 175, 200, 225, 250, 275, 300, 325, 350, 375, 400};
    private Project project;
    private MyProjectListener myProjectListener = new MyProjectListener();
    private MenuListener menuListener;
    private Toolbar toolbar;
    private HorizontalSplitPane leftRegion;
    private VerticalSplitPane mainRegion;
    private JPanel mainPanelSuper;
    private CardPanel mainPanel;
    private CardPanel explorerPane;
    private Toolbox toolbox;
    private AttrTable attributeTable;
    private ZoomControl zoom;
    // for the Layout view
    private LayoutToolbarModel layoutToolbarModel;
    private Canvas layoutCanvas;
    private ZoomModel layoutZoomModel;
    private LayoutEditHandler layoutEditHandler;
    private AttrTableSelectionModel attributeTableSelectionModel;
    // for the Appearance view
    private AppearanceView appearance;

    public Frame(Project project) {
        this.project = project;

        setBackground(Color.white);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new MyWindowListener());

        project.addProjectListener(myProjectListener);
        project.addLibraryListener(myProjectListener);
        project.addCircuitListener(myProjectListener);
        computeTitle();

        // set up elements for the Layout view
        layoutToolbarModel = new LayoutToolbarModel(this, project);
        layoutCanvas = new Canvas(project);
        layoutZoomModel = new BasicZoomModel(AppPreferences.LAYOUT_SHOW_GRID, AppPreferences.LAYOUT_ZOOM, ZOOM_OPTIONS);

        layoutCanvas.getGridPainter().setZoomModel(layoutZoomModel);
        layoutEditHandler = new LayoutEditHandler(this);
        attributeTableSelectionModel = new AttrTableSelectionModel(project, this);

        // set up menu bar and toolbar
        // GUI elements shared between views
        LogisimMenuBar menubar = new LogisimMenuBar(this, project);
        menuListener = new MenuListener(this, menubar);
        menuListener.setEditHandler(layoutEditHandler);
        setJMenuBar(menubar);
        toolbar = new Toolbar(layoutToolbarModel);

        // set up the left-side components
        ToolbarModel toolbarModel = new ExplorerToolbarModel(this, menuListener);
        // left-side elements
        Toolbar projectToolbar = new Toolbar(toolbarModel);
        toolbox = new Toolbox(project, menuListener);
        SimulationExplorer simExplorer = new SimulationExplorer(project, menuListener);
        explorerPane = new CardPanel();
        explorerPane.addView(VIEW_TOOLBOX, toolbox);
        explorerPane.addView(VIEW_SIMULATION, simExplorer);
        explorerPane.setView(VIEW_TOOLBOX);
        attributeTable = new AttrTable(this);
        zoom = new ZoomControl(layoutZoomModel);

        // set up the central area
        CanvasPane canvasPane = new CanvasPane(layoutCanvas);
        mainPanelSuper = new JPanel(new BorderLayout());
        canvasPane.setZoomModel(layoutZoomModel);
        mainPanel = new CardPanel();
        mainPanel.addView(EDIT_LAYOUT, canvasPane);
        mainPanel.setView(EDIT_LAYOUT);
        mainPanelSuper.add(mainPanel, BorderLayout.CENTER);

        // set up the contents, split down the middle, with the canvas
        // on the right and a split pane on the left containing the
        // explorer and attribute values.
        JPanel explPanel = new JPanel(new BorderLayout());
        explPanel.add(projectToolbar, BorderLayout.NORTH);
        explPanel.add(explorerPane, BorderLayout.CENTER);
        JPanel attrPanel = new JPanel(new BorderLayout());
        attrPanel.add(attributeTable, BorderLayout.CENTER);
        attrPanel.add(zoom, BorderLayout.SOUTH);

        leftRegion = new HorizontalSplitPane(explPanel, attrPanel, AppPreferences.WINDOW_LEFT_SPLIT.get());
        mainRegion = new VerticalSplitPane(leftRegion, mainPanelSuper, AppPreferences.WINDOW_MAIN_SPLIT.get());

        getContentPane().add(mainRegion, BorderLayout.CENTER);

        computeTitle();

        this.setSize(AppPreferences.WINDOW_WIDTH.get(), AppPreferences.WINDOW_HEIGHT.get());
        Point prefPoint = getInitialLocation();
        if (prefPoint != null) {
            this.setLocation(prefPoint);
        }
        this.setExtendedState(AppPreferences.WINDOW_STATE.get());

        menuListener.register(mainPanel);
        KeyboardToolSelection.register(toolbar);

        project.setFrame(this);
        if (project.getTool() == null) {
            project.setTool(project.getOptions().getToolbarData().getFirstTool());
        }
        mainPanel.addChangeListener(myProjectListener);
        explorerPane.addChangeListener(myProjectListener);
        AppPreferences.TOOLBAR_PLACEMENT.addPropertyChangeListener(myProjectListener);
        placeToolbar();
        ((MenuListener.EnabledListener) toolbarModel).menuEnableChanged(menuListener);

        LocaleManager.addLocaleListener(this);
    }

    private static Point getInitialLocation() {
        String location = AppPreferences.WINDOW_LOCATION.get();
        if (location == null) {
            return null;
        }
        int comma = location.indexOf(',');
        if (comma < 0) {
            return null;
        }
        try {
            int x = Integer.parseInt(location.substring(0, comma));
            int y = Integer.parseInt(location.substring(comma + 1));
            while (isProjectFrameAt(x, y)) {
                x += 20;
                y += 20;
            }
            Rectangle desired = new Rectangle(x, y, 50, 50);

            int gcBestSize = 0;
            Point gcBestPoint = null;
            GraphicsEnvironment environment;
            environment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            for (GraphicsDevice device : environment.getScreenDevices()) {
                for (GraphicsConfiguration configuration : device.getConfigurations()) {
                    Rectangle gcBounds = configuration.getBounds();
                    if (gcBounds.intersects(desired)) {
                        Rectangle inter = gcBounds.intersection(desired);
                        int size = inter.width * inter.height;
                        if (size > gcBestSize) {
                            gcBestSize = size;
                            int x2 = Math.max(gcBounds.x, Math.min(inter.x,
                                    inter.x + inter.width - 50));
                            int y2 = Math.max(gcBounds.y, Math.min(inter.y,
                                    inter.y + inter.height - 50));
                            gcBestPoint = new Point(x2, y2);
                        }
                    }
                }
            }
            if (gcBestPoint != null) {
                if (isProjectFrameAt(gcBestPoint.x, gcBestPoint.y)) {
                    gcBestPoint = null;
                }
            }
            return gcBestPoint;
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isProjectFrameAt(int x, int y) {
        for (Project current : Projects.getOpenProjects()) {
            Frame frame = current.getFrame();
            if (frame != null) {
                Point location = frame.getLocationOnScreen();
                int d = Math.abs(location.x - x) + Math.abs(location.y - y);
                if (d <= 3) {
                    return true;
                }
            }
        }
        return false;
    }

    private void placeToolbar() {
        String location = AppPreferences.TOOLBAR_PLACEMENT.get();
        Container contents = getContentPane();
        contents.remove(toolbar);
        mainPanelSuper.remove(toolbar);
        if (!AppPreferences.TOOLBAR_HIDDEN.equals(location)) {
            if (AppPreferences.TOOLBAR_DOWN_MIDDLE.equals(location)) {
                toolbar.setOrientation(Toolbar.VERTICAL);
                mainPanelSuper.add(toolbar, BorderLayout.WEST);
            } else { // it is a BorderLayout constant
                Object value = BorderLayout.NORTH;
                for (Direction direction : Direction.cardinals) {
                    if (direction.toString().equals(location)) {
                        if (direction == Direction.EAST) {
                            value = BorderLayout.EAST;
                        } else if (direction == Direction.SOUTH) {
                            value = BorderLayout.SOUTH;
                        } else if (direction == Direction.WEST) {
                            value = BorderLayout.WEST;
                        } else {
                            value = BorderLayout.NORTH;
                        }
                    }
                }

                contents.add(toolbar, value);
                boolean vertical = value == BorderLayout.WEST || value == BorderLayout.EAST;
                toolbar.setOrientation(vertical ? Toolbar.VERTICAL : Toolbar.HORIZONTAL);
            }
        }
        // Otherwise don't place value anywhere

        contents.validate();
    }

    public Project getProject() {
        return project;
    }

    public void viewComponentAttributes(Circuit circuit, Component component) {
        if (component == null) {
            setAttrTableModel(null);
        } else {
            setAttrTableModel(new AttrTableComponentModel(project, circuit, component));
        }
    }

    void setAttrTableModel(AttrTableModel value) {
        attributeTable.setAttrTableModel(value);
        if (value instanceof AttrTableToolModel) {
            Tool tool = ((AttrTableToolModel) value).getTool();
            toolbox.setHaloedTool(tool);
            layoutToolbarModel.setHaloedTool(tool);
        } else {
            toolbox.setHaloedTool(null);
            layoutToolbarModel.setHaloedTool(null);
        }
        if (value instanceof AttrTableComponentModel) {
            Circuit circuit = ((AttrTableComponentModel) value).getCircuit();
            Component component = ((AttrTableComponentModel) value).getComponent();
            layoutCanvas.setHaloedComponent(circuit, component);
        } else {
            layoutCanvas.setHaloedComponent(null, null);
        }
    }

    public String getExplorerView() {
        return explorerPane.getView();
    }

    public void setExplorerView(String view) {
        explorerPane.setView(view);
    }

    public String getEditorView() {
        return mainPanel.getView();
    }

    public void setEditorView(String view) {
        String currentView = mainPanel.getView();
        if (currentView.equals(view)) {
            return;
        }

        if (view.equals(EDIT_APPEARANCE)) { // appearance view
            AppearanceView appearance = this.appearance;
            if (appearance == null) {
                appearance = new AppearanceView();
                appearance.setCircuit(project, project.getCircuitState());
                mainPanel.addView(EDIT_APPEARANCE, appearance.getCanvasPane());
                this.appearance = appearance;
            }
            toolbar.setToolbarModel(appearance.getToolbarModel());
            appearance.getAttrTableDrawManager(attributeTable).attributesSelected();
            zoom.setZoomModel(appearance.getZoomModel());
            menuListener.setEditHandler(appearance.getEditHandler());
            mainPanel.setView(view);
            appearance.getCanvas().requestFocus();
        } else { // layout view
            toolbar.setToolbarModel(layoutToolbarModel);
            zoom.setZoomModel(layoutZoomModel);
            menuListener.setEditHandler(layoutEditHandler);
            viewAttributes(project.getTool(), true);
            mainPanel.setView(view);
            layoutCanvas.requestFocus();
        }
    }

    public Canvas getCanvas() {
        return layoutCanvas;
    }

    private void computeTitle() {
        String str;
        Circuit circuit = project.getCurrentCircuit();
        String name = project.getLogisimFile().getName();
        if (circuit != null) {
            str = StringUtil.format(Strings.get("titleCircFileKnown"),
                    circuit.getName(), name);
        } else {
            str = StringUtil.format(Strings.get("titleFileKnown"), name);
        }

        // Adjust the title according to the release type
        if (Main.RELEASE_TYPE == Release.ALPHA) {
            this.setTitle("ALPHA " + Main.VERSION_NAME + " -- " + str);
        } else if (Main.RELEASE_TYPE == Release.BETA) {
            this.setTitle("BETA " + Main.VERSION_NAME + " -- " + str);
        } else if (Main.RELEASE_TYPE == Release.RELEASE) {
            this.setTitle(Main.VERSION_NAME + str);
        } else {
            this.setTitle(Main.VERSION_NAME + str);
        }

        myProjectListener.enableSave();
    }

    void viewAttributes(Tool newTool) {
        viewAttributes(null, newTool, false);
    }

    @SuppressWarnings("SameParameterValue")
    private void viewAttributes(Tool newTool, boolean force) {
        viewAttributes(null, newTool, force);
    }

    private void viewAttributes(Tool oldTool, Tool newTool, boolean force) {
        AttributeSet newAttributes;
        if (newTool == null) {
            newAttributes = null;
            if (!force) {
                return;
            }
        } else {
            newAttributes = newTool.getAttributeSet(layoutCanvas);
        }
        if (newAttributes == null) {
            AttrTableModel oldModel = attributeTable.getAttrTableModel();
            boolean same = oldModel instanceof AttrTableToolModel
                    && ((AttrTableToolModel) oldModel).getTool() == oldTool;
            if (!force && !same && !(oldModel instanceof AttrTableCircuitModel)) {
                return;
            }
        }
        if (newAttributes == null) {
            Circuit circuit = project.getCurrentCircuit();
            if (circuit != null) {
                setAttrTableModel(new AttrTableCircuitModel(project, circuit));
            } else if (force) {
                setAttrTableModel(null);
            }
        } else if (newAttributes instanceof SelectionAttributes) {
            setAttrTableModel(attributeTableSelectionModel);
        } else {
            setAttrTableModel(new AttrTableToolModel(project, newTool));
        }
    }

    public void localeChanged() {
        computeTitle();
    }

    public void savePreferences() {
        AppPreferences.TICK_FREQUENCY.set(project.getSimulator().getTickFrequency());
        AppPreferences.LAYOUT_SHOW_GRID.setBoolean(layoutZoomModel.getShowGrid());
        AppPreferences.LAYOUT_ZOOM.set(layoutZoomModel.getZoomFactor());
        if (appearance != null) {
            ZoomModel aZoom = appearance.getZoomModel();
            AppPreferences.APPEARANCE_SHOW_GRID.setBoolean(aZoom.getShowGrid());
            AppPreferences.APPEARANCE_ZOOM.set(aZoom.getZoomFactor());
        }
        int state = getExtendedState() & ~JFrame.ICONIFIED;
        AppPreferences.WINDOW_STATE.set(state);
        Dimension dimension = getSize();
        AppPreferences.WINDOW_WIDTH.set(dimension.width);
        AppPreferences.WINDOW_HEIGHT.set(dimension.height);
        Point location;
        try {
            location = getLocationOnScreen();
        } catch (IllegalComponentStateException e) {
            location = Projects.getLocation(this);
        }
        if (location != null) {
            AppPreferences.WINDOW_LOCATION.set(location.x + "," + location.y);
        }
        AppPreferences.WINDOW_LEFT_SPLIT.set(leftRegion.getFraction());
        AppPreferences.WINDOW_MAIN_SPLIT.set(mainRegion.getFraction());
        AppPreferences.DIALOG_DIRECTORY.set(JFileChoosers.getCurrentDirectory());
    }

    public boolean confirmClose() {
        return confirmClose(Strings.get("confirmCloseTitle"));
    }

    // returns true if user is OK with proceeding
    public boolean confirmClose(String title) {
        String message = StringUtil.format(Strings.get("confirmDiscardMessage"),
                project.getLogisimFile().getName());

        if (!project.isFileDirty()) {
            return true;
        }
        toFront();
        String[] options = {Strings.get("saveOption"), Strings.get("discardOption"), Strings.get("cancelOption")};
        int result = JOptionPane.showOptionDialog(this,
                message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                options, options[0]);
        boolean ret;
        if (result == 0) {
            ret = ProjectActions.doSave(project);
        } else {
            ret = result == 1;
        }
        if (ret) {
            dispose();
        }
        return ret;
    }

    class MyProjectListener
            implements ProjectListener, LibraryListener, CircuitListener,
            PropertyChangeListener, ChangeListener {

        public void projectChanged(ProjectEvent event) {
            int action = event.getAction();

            if (action == ProjectEvent.ACTION_SET_FILE) {
                computeTitle();
                project.setTool(project.getOptions().getToolbarData().getFirstTool());
                placeToolbar();
            } else if (action == ProjectEvent.ACTION_SET_CURRENT) {
                setEditorView(EDIT_LAYOUT);
                if (appearance != null) {
                    appearance.setCircuit(project, project.getCircuitState());
                }
                viewAttributes(project.getTool());
                computeTitle();
            } else if (action == ProjectEvent.ACTION_SET_TOOL) {
                if (attributeTable == null) {
                    return; // for startup
                }
                Tool oldTool = (Tool) event.getOldData();
                Tool newTool = (Tool) event.getData();
                if (getEditorView().equals(EDIT_LAYOUT)) {
                    viewAttributes(oldTool, newTool, false);
                }
            }
        }

        public void libraryChanged(LibraryEvent e) {
            if (e.getAction() == LibraryEvent.SET_NAME) {
                computeTitle();
            } else if (e.getAction() == LibraryEvent.DIRTY_STATE) {
                enableSave();
            }
        }

        public void circuitChanged(CircuitEvent event) {
            if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
                computeTitle();
            }
        }

        private void enableSave() {
            Project project = getProject();
            boolean isDirty = project.isFileDirty();
            getRootPane().putClientProperty("windowModified", isDirty);
        }

        public void attributeListChanged() { }

        public void propertyChange(PropertyChangeEvent event) {
            if (AppPreferences.TOOLBAR_PLACEMENT.isSource(event)) {
                placeToolbar();
            }
        }

        public void stateChanged(ChangeEvent event) {
            Object source = event.getSource();
            if (source == explorerPane) {
                firePropertyChange(EXPLORER_VIEW, "???", getExplorerView());
            } else if (source == mainPanel) {
                firePropertyChange(EDITOR_VIEW, "???", getEditorView());
            }
        }
    }

    class MyWindowListener extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            if (confirmClose(Strings.get("confirmCloseTitle"))) {
                layoutCanvas.closeCanvas();
                Frame.this.dispose();
            }
        }

        @Override
        public void windowOpened(WindowEvent e) {
            layoutCanvas.computeSize(true);
        }
    }
}

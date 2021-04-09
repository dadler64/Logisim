package com.cburch.logisim.gui.menu;

import com.adlerd.logger.Logger;
import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MenuTheme extends Menu {

    private static final String HELP_URL = "https://github.com/dadler64/Logisim/wiki";
    private final LogisimMenuBar menubar;
    private final JMenuItem tutorial = new JMenuItem();
    private final JMenuItem guide = new JMenuItem();
    private final JMenuItem library = new JMenuItem();
    private final JMenuItem about = new JMenuItem();

    public MenuTheme(LogisimMenuBar menubar) {
        this.menubar = menubar;

        add(tutorial);
        add(guide);
        add(library);
        if (!MacCompatibility.isAboutAutomaticallyPresent()) {
            addSeparator();
            add(about);
        }
    }

    /**
     * Create menu items for the Look & Feel menu
     */
    private JMenu createLAFMenu() {
        ButtonGroup buttonGroup = new ButtonGroup();

        JMenu themeMenu = new JMenu("UI Theme");
        themeMenu.setMnemonic('L');

        String themeId = UIManager.getLookAndFeel().getID();
        UIManager.LookAndFeelInfo[] themes = UIManager.getInstalledLookAndFeels();

        for (UIManager.LookAndFeelInfo theme : themes) {
            String lookAndFeelClass = theme.getClassName();
            String name = theme.getName();

            Action action = new ChangeLookAndFeelAction(lookAndFeelClass, name);
            JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(action);
            themeMenu.add(menuItem);
            buttonGroup.add(menuItem);

            if (name.equals(themeId)) {
                menuItem.setSelected(true);
            }
        }

        return themeMenu;
    }

    public void localeChanged() {
        this.setText(Strings.get("helpMenu"));
        tutorial.setText(Strings.get("helpTutorialItem"));
        guide.setText(Strings.get("helpGuideItem"));
        library.setText(Strings.get("helpLibraryItem"));
        about.setText(Strings.get("helpAboutItem"));
    }

    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        String extension = "";
        if (source == tutorial) {
            // TODO: Add url extension to get to the Tutorial Page
            extension = "";
        } else if (source == guide) {
            // TODO: Add url extension to get to the User Guide page
            extension = "";
        } else if (source == library) {
            // TODO: Add url extension to get to the Library Reference Page
            extension = "";
        } else if (source == about) {
            About.showAboutDialog(menubar.getParentWindow());
        }

        try {
            Desktop.getDesktop().browse(new URL(HELP_URL + extension).toURI());
        } catch (IOException e) {
            Logger.errorln("Could not open link");
            Logger.errorln(Strings.get("linkOpenFail"));
            e.printStackTrace();
        } catch (URISyntaxException e) {
            Logger.errorln(Strings.get("badUri"));
            e.printStackTrace();
        }
    }

    @Override
    void computeEnabled() {

    }

    /*
     *  Change the LAF and recreate the UIManagerDefaults so that the properties
     *  of the new LAF are correctly displayed.
     */
    static class ChangeLookAndFeelAction extends AbstractAction {

        private final String laf;

        private ChangeLookAndFeelAction(String laf, String name) {
            this.laf = laf;
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, getValue(Action.NAME));
        }

        public void actionPerformed(ActionEvent e) {
            try {
                JMenuItem mi = (JMenuItem) e.getSource();
                JPopupMenu popup = (JPopupMenu) mi.getParent();
                JRootPane rootPane = SwingUtilities.getRootPane(popup.getInvoker());
                Component c = rootPane.getContentPane().getComponent(0);
                rootPane.getContentPane().remove(c);

                UIManager.setLookAndFeel(laf);
//                rootPane.getContentPane().add(bindings.getContentPane());
                SwingUtilities.updateComponentTreeUI(rootPane);
                rootPane.requestFocusInWindow();
            } catch (Exception ex) {
                System.out.println("Failed loading L&F: " + laf);
                System.out.println(ex);
            }
        }
    }

}

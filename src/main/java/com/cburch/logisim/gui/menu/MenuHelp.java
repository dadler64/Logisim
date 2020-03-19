/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.gui.menu;

import com.adlerd.logger.Logger;
import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.util.MacCompatibility;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

class MenuHelp extends JMenu implements ActionListener {

    private LogisimMenuBar menubar;
    private JMenuItem tutorial = new JMenuItem();
    private JMenuItem guide = new JMenuItem();
    private JMenuItem library = new JMenuItem();
    private JMenuItem about = new JMenuItem();
    private static final String HELP_URL = "https://github.com/dadler64/Logisim/wiki";

    public MenuHelp(LogisimMenuBar menubar) {
        this.menubar = menubar;

        tutorial.addActionListener(this);
        guide.addActionListener(this);
        library.addActionListener(this);
        about.addActionListener(this);

        add(tutorial);
        add(guide);
        add(library);
        if (!MacCompatibility.isAboutAutomaticallyPresent()) {
            addSeparator();
            add(about);
        }
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
}

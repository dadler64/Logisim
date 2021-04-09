/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.tools;

import com.adlerd.logger.Logger;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import java.util.Arrays;
import java.util.List;
import javax.swing.Icon;

/**
 * This class allows an object to be created holding all the information
 * essential to showing a ComponentFactory in the explorer window, but without
 * actually loading the ComponentFactory unless a program genuinely gets around
 * to needing to use it. Note that for this to work, the relevant
 * ComponentFactory class must be in the same package as its Library class,
 * the ComponentFactory class must be public, and it must include a public
 * no-arguments constructor.
 */
public class FactoryDescription {

    private final String name;
    private final StringGetter displayName;
    private final String factoryClassName;
    private String iconName;
    private boolean iconLoadAttempted;
    private Icon icon;
    private boolean factoryLoadAttempted;
    private ComponentFactory factory;
    private StringGetter toolTip;

    public FactoryDescription(String name, StringGetter displayName, String iconName, String factoryClassName) {
        this(name, displayName, factoryClassName);
        this.iconName = iconName;
        this.iconLoadAttempted = false;
        this.icon = null;
    }

    public FactoryDescription(String name, StringGetter displayName, Icon icon, String factoryClassName) {
        this(name, displayName, factoryClassName);
        this.iconName = "???";
        this.iconLoadAttempted = true;
        this.icon = icon;
    }

    private FactoryDescription(String name, StringGetter displayName, String factoryClassName) {
        this.name = name;
        this.displayName = displayName;
        this.iconName = "???";
        this.iconLoadAttempted = true;
        this.icon = null;
        this.factoryClassName = factoryClassName;
        this.factoryLoadAttempted = false;
        this.factory = null;
        this.toolTip = null;
    }

    public static List<Tool> getTools(Class<? extends Library> base, FactoryDescription[] descriptions) {
        Tool[] tools = new Tool[descriptions.length];
        for (int i = 0; i < tools.length; i++) {
            tools[i] = new AddTool(base, descriptions[i]);
        }
        return Arrays.asList(tools);
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return displayName.get();
    }

    public boolean isFactoryLoaded() {
        return factoryLoadAttempted;
    }

    public Icon getIcon() {
        Icon icon = this.icon;
        if (icon == null && !iconLoadAttempted) {
            icon = Icons.getIcon(iconName);
            this.icon = icon;
            iconLoadAttempted = true;
        }
        return icon;
    }

    public ComponentFactory getFactory(Class<? extends Library> libraryClass) {
        ComponentFactory factory = this.factory;
        if (this.factory != null || factoryLoadAttempted) {
            return factory;
        } else {
            String message = "";
            try {
                message = "getting class loader";
                ClassLoader loader = libraryClass.getClassLoader();
                message = "getting package name";
                String name;
                Package pack = libraryClass.getPackage();
                if (pack == null) {
                    name = factoryClassName;
                } else {
                    name = pack.getName() + "." + factoryClassName;
                }
                message = "loading class";
                Class<?> factoryClass = loader.loadClass(name);
                message = "creating instance";
                Object factoryValue = factoryClass.getConstructor().newInstance();
                message = "converting to factory";
                if (factoryValue instanceof ComponentFactory) {
                    factory = (ComponentFactory) factoryValue;
                    this.factory = factory;
                    factoryLoadAttempted = true;
                    return factory;
                }
            } catch (Throwable t) {
                String name = t.getClass().getName();
                String errorMessage = t.getMessage();
                if (errorMessage != null) {
                    message = message + ": " + name + ": " + errorMessage;
                } else {
                    message = message + ": " + name;
                }
            }
            Logger.errorln("error while " + message); //OK
            this.factory = null;
            factoryLoadAttempted = true;
            return null;
        }
    }

    public String getToolTip() {
        StringGetter getter = toolTip;
        return getter == null ? null : getter.get();
    }

    public FactoryDescription setToolTip(StringGetter getter) {
        toolTip = getter;
        return this;
    }
}

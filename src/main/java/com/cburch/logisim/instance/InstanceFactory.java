/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.adlerd.logger.Logger;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.log.Loggable;
import com.cburch.logisim.tools.Pokable;
import com.cburch.logisim.tools.key.KeyConfigurator;
import com.cburch.logisim.util.Icons;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Collections;
import java.util.List;
import javax.swing.Icon;

/**
 * Represents a category of components that appear in a circuit. This class
 * and <code>Component</code> share the same sort of relationship as the
 * relation between <em>classes</em> and <em>instances</em> in Java. Normally,
 * there is only one ComponentFactory created for any particular category.
 */
public abstract class InstanceFactory extends AbstractComponentFactory {

    private String name;
    private StringGetter displayName;
    private StringGetter defaultToolTip;
    private String iconName;
    private Icon icon;
    private Attribute<?>[] attributes;
    private Object[] defaults;
    private AttributeSet defaultSet;
    private Bounds bounds;
    private List<Port> portList;
    private Attribute<Direction> facingAttribute;
    private Boolean shouldSnap;
    private KeyConfigurator keyConfigurator;
    private Class<? extends InstancePoker> pokerClass;
    private Class<? extends InstanceLogger> loggerClass;

    public InstanceFactory(String name) {
        this(name, StringUtil.constantGetter(name));
    }

    public InstanceFactory(String name, StringGetter displayName) {
        this.name = name;
        this.displayName = displayName;
        this.iconName = null;
        this.icon = null;
        this.attributes = null;
        this.defaults = null;
        this.bounds = Bounds.EMPTY_BOUNDS;
        this.portList = Collections.emptyList();
        this.keyConfigurator = null;
        this.facingAttribute = null;
        this.shouldSnap = Boolean.TRUE;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return getDisplayGetter().get();
    }

    @Override
    public StringGetter getDisplayGetter() {
        return displayName;
    }

    public void setIconName(String value) {
        iconName = value;
        icon = null;
    }

    public void setIcon(Icon value) {
        iconName = "";
        icon = value;
    }

    @Override
    public final void paintIcon(ComponentDrawContext context, int x, int y, AttributeSet attributes) {
        InstancePainter painter = context.getInstancePainter();
        painter.setFactory(this, attributes);
        Graphics graphics = painter.getGraphics();
        graphics.translate(x, y);
        paintIcon(painter);
        graphics.translate(-x, -y);

        if (painter.getFactory() == null) {
            Icon icon = this.icon;
            if (icon == null) {
                String iconName = this.iconName;
                if (iconName != null) {
                    icon = Icons.getIcon(iconName);
                    if (icon == null) {
                        iconName = null;
                    }
                }
            }
            if (icon != null) {
                icon.paintIcon(context.getDestination(), graphics, x + 2, y + 2);
            } else {
                super.paintIcon(context, x, y, attributes);
            }
        }
    }

    @Override
    public final Component createComponent(Location location, AttributeSet attributes) {
        InstanceComponent component = new InstanceComponent(this, location, attributes);
        configureNewInstance(component.getInstance());
        return component;
    }

    public void setOffsetBounds(Bounds value) {
        bounds = value;
    }

    @Override
    public Bounds getOffsetBounds(AttributeSet attributes) {
        Bounds bounds = this.bounds;
        if (bounds == null) {
            throw new RuntimeException("offset bounds unknown: use setOffsetBounds or override getOffsetBounds");
        }
        return bounds;
    }

    public boolean contains(Location location, AttributeSet attributeSet) {
        Bounds bounds = getOffsetBounds(attributeSet);
        if (bounds == null) {
            return false;
        }
        return bounds.contains(location, 1);
    }


    public Attribute<Direction> getFacingAttribute() {
        return facingAttribute;
    }

    public void setFacingAttribute(Attribute<Direction> value) {
        facingAttribute = value;
    }

    public KeyConfigurator getKeyConfigurator() {
        return keyConfigurator;
    }

    public void setKeyConfigurator(KeyConfigurator value) {
        keyConfigurator = value;
    }

    public void setAttributes(Attribute<?>[] attributes, Object[] defaults) {
        this.attributes = attributes;
        this.defaults = defaults;
    }

    @Override
    public AttributeSet createAttributeSet() {
        Attribute<?>[] attributes = this.attributes;
        return attributes == null ? AttributeSets.EMPTY : AttributeSets.fixedSet(attributes, defaults);
    }

    @Override
    public Object getDefaultAttributeValue(Attribute<?> attribute, LogisimVersion version) {
        Attribute<?>[] attributes = this.attributes;
        if (attributes != null) {
            for (int i = 0; i < attributes.length; i++) {
                if (attributes[i] == attribute) {
                    return defaults[i];
                }
            }
            return null;
        } else {
            AttributeSet defaultSet = this.defaultSet;
            if (defaultSet == null) {
                defaultSet = createAttributeSet();
                this.defaultSet = defaultSet;
            }
            return defaultSet.getValue(attribute);
        }
    }

    public List<Port> getPorts() {
        return portList;
    }

    public void setPorts(Port[] ports) {
        portList = new UnmodifiableList<>(ports);
    }

    public void setPorts(List<Port> ports) {
        portList = Collections.unmodifiableList(ports);
    }

    public StringGetter getDefaultToolTip() {
        return defaultToolTip;
    }

    public void setDefaultToolTip(StringGetter value) {
        defaultToolTip = value;
    }

    public void setInstancePoker(Class<? extends InstancePoker> pokerClass) {
        if (isClassOk(pokerClass, InstancePoker.class)) {
            this.pokerClass = pokerClass;
        }
    }

    public void setInstanceLogger(Class<? extends InstanceLogger> loggerClass) {
        if (isClassOk(loggerClass, InstanceLogger.class)) {
            this.loggerClass = loggerClass;
        }
    }

    public void setShouldSnap(boolean shouldSnap) {
        this.shouldSnap = shouldSnap;
    }

    private boolean isClassOk(Class<?> subclass, Class<?> superclass) {
        boolean isSubclass = superclass.isAssignableFrom(subclass);
        if (!isSubclass) {
            Logger.warnln(subclass.getName() + " must be a subclass of " + superclass.getName()); //OK
            return false;
        }
        try {
            subclass.getConstructor();
            return true;
        } catch (SecurityException e) {
            Logger.warnln(subclass.getName() + " needs its no-args constructor to be public"); //OK
        } catch (NoSuchMethodException e) {
            Logger.warnln(subclass.getName() + " is missing a no-arguments constructor"); //OK
        }
        return true;
    }

    @Override
    public final Object getFeature(Object key, AttributeSet attributeSet) {
        if (key == FACING_ATTRIBUTE_KEY) {
            return facingAttribute;
        }
        if (key == KeyConfigurator.class) {
            return keyConfigurator;
        }
        if (key == SHOULD_SNAP) {
            return shouldSnap;
        }
        return super.getFeature(key, attributeSet);
    }

    @Override
    public final void drawGhost(ComponentDrawContext context, Color color, int x, int y, AttributeSet attributes) {
        InstancePainter painter = context.getInstancePainter();
        Graphics graphics = painter.getGraphics();
        graphics.setColor(color);
        graphics.translate(x, y);
        painter.setFactory(this, attributes);
        paintGhost(painter);
        graphics.translate(-x, -y);
        if (painter.getFactory() == null) {
            super.drawGhost(context, color, x, y, attributes);
        }
    }

    public void paintIcon(InstancePainter painter) {
        painter.setFactory(null, null);
    }

    public void paintGhost(InstancePainter painter) {
        painter.setFactory(null, null);
    }

    public abstract void paintInstance(InstancePainter painter);

    public abstract void propagate(InstanceState state);

    // event methods
    protected void configureNewInstance(Instance instance) {
    }

    protected void instanceAttributeChanged(Instance instance, Attribute<?> attribute) {
    }

    protected Object getInstanceFeature(Instance instance, Object key) {
        if (key == Pokable.class && pokerClass != null) {
            return new InstancePokerAdapter(instance.getComponent(), pokerClass);
        } else if (key == Loggable.class && loggerClass != null) {
            return new InstanceLoggerAdapter(instance.getComponent(), loggerClass);
        } else {
            return null;
        }
    }

    public final InstanceState createInstanceState(CircuitState state, Instance instance) {
        return new InstanceStateImpl(state, instance.getComponent());
    }

    public final InstanceState createInstanceState(CircuitState state, Component comp) {
        return createInstanceState(state, ((InstanceComponent) comp).getInstance());
    }
}

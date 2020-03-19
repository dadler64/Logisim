/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.appear.AppearanceSvgReader;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

class XmlReader {

    private LibraryLoader loader;

    XmlReader(Loader loader) {
        this.loader = loader;
    }

    private static void findLibraryUses(ArrayList<Element> destinations, String label,
            Iterable<Element> candidates) {
        for (Element elements : candidates) {
            String lib = elements.getAttribute("lib");
            if (lib.equals(label)) {
                destinations.add(elements);
            }
        }
    }

    LogisimFile readLibrary(InputStream stream) throws IOException, SAXException {
        Document document = loadXmlFrom(stream);
        Element element = document.getDocumentElement();
        considerRepairs(document, element);
        LogisimFile file = new LogisimFile((Loader) loader);
        ReadContext context = new ReadContext(file);
        context.toLogisimFile(element);
        if (file.getCircuitCount() == 0) {
            file.addCircuit(new Circuit("main"));
        }
        if (context.messages.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (String message : context.messages) {
                builder.append(message);
                builder.append("\n");
            }
            loader.showError(builder.substring(0, builder.length() - 1));
        }
        return file;
    }

    private Document loadXmlFrom(InputStream stream) throws SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        }
        assert builder != null;
        return builder.parse(stream);
    }

    private void considerRepairs(Document document, Element root) {
        LogisimVersion version = LogisimVersion.parse(root.getAttribute("source"));
        if (version.compareTo(LogisimVersion.get(2, 3, 0)) < 0) {
            // This file was saved before an Edit tool existed. Most likely
            // we should replace the Select and Wiring tools in the toolbar
            // with the Edit tool instead.
            for (Element toolbar : XmlIterator.forChildElements(root, "toolbar")) {
                Element wiring = null;
                Element select = null;
                Element edit = null;
                for (Element element : XmlIterator.forChildElements(toolbar, "tool")) {
                    String eltName = element.getAttribute("name");
                    if (eltName != null && !eltName.equals("")) {
                        if (eltName.equals("Select Tool")) {
                            select = element;
                        }
                        if (eltName.equals("Wiring Tool")) {
                            wiring = element;
                        }
                        if (eltName.equals("Edit Tool")) {
                            edit = element;
                        }
                    }
                }
                if (select != null && wiring != null && edit == null) {
                    select.setAttribute("name", "Edit Tool");
                    toolbar.removeChild(wiring);
                }
            }
        }
        if (version.compareTo(LogisimVersion.get(2, 6, 3)) < 0) {
            for (Element circleElement : XmlIterator.forChildElements(root, "circuit")) {
                for (Element attributeElement : XmlIterator.forChildElements(circleElement, "a")) {
                    String name = attributeElement.getAttribute("name");
                    if (name != null && name.startsWith("label")) {
                        attributeElement.setAttribute("name", "c" + name);
                    }
                }
            }

            repairForWiringLibrary(document, root);
            repairForLegacyLibrary(document, root);
        }
    }

    private void repairForWiringLibrary(Document document, Element root) {
        Element oldBaseElement = null;
        String oldBaseLabel = null;
        Element gatesElement = null;
        String gatesLabel = null;
        int maxLabel = -1;
        Element firstLibraryElement = null;
        Element lastLibraryElement = null;
        for (Element libraryElement : XmlIterator.forChildElements(root, "lib")) {
            String desc = libraryElement.getAttribute("desc");
            String label = libraryElement.getAttribute("name");
            if (desc != null) { // skip these tests
                switch (desc) {
                    case "#Base":
                        oldBaseElement = libraryElement;
                        oldBaseLabel = label;
                        break;
                    case "#Wiring":
                        // Wiring library already in file. This shouldn't happen, but if
                        // somehow it does, we don't want to add it again.
                        return;
                    case "#Gates":
                        gatesElement = libraryElement;
                        gatesLabel = label;
                        break;
                }
            }

            if (firstLibraryElement == null) {
                firstLibraryElement = libraryElement;
            }
            lastLibraryElement = libraryElement;
            try {
                if (label != null) {
                    int thisLabel = Integer.parseInt(label);
                    if (thisLabel > maxLabel) {
                        maxLabel = thisLabel;
                    }
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        Element wiringElement;
        String wiringLabel;
        Element newBaseElement;
        String newBaseLabel;
        if (oldBaseElement != null) {
            wiringLabel = oldBaseLabel;
            wiringElement = oldBaseElement;
            wiringElement.setAttribute("desc", "#Wiring");

            newBaseLabel = "" + (maxLabel + 1);
            newBaseElement = document.createElement("lib");
            newBaseElement.setAttribute("desc", "#Base");
            newBaseElement.setAttribute("name", newBaseLabel);
            root.insertBefore(newBaseElement, lastLibraryElement.getNextSibling());
        } else {
            wiringLabel = "" + (maxLabel + 1);
            wiringElement = document.createElement("lib");
            wiringElement.setAttribute("desc", "#Wiring");
            wiringElement.setAttribute("name", wiringLabel);
            assert lastLibraryElement != null;
            root.insertBefore(wiringElement, lastLibraryElement.getNextSibling());

            newBaseLabel = null;
            newBaseElement = null;
        }

        HashMap<String, String> labelMap = new HashMap<>();
        addToLabelMap(labelMap, oldBaseLabel, newBaseLabel, "Poke Tool;"
                + "Edit Tool;Select Tool;Wiring Tool;Text Tool;Menu Tool;Text");
        addToLabelMap(labelMap, oldBaseLabel, wiringLabel, "Splitter;Pin;"
                + "Probe;Tunnel;Clock;Pull Resistor;Bit Extender");
        addToLabelMap(labelMap, gatesLabel, wiringLabel, "Constant");
        relocateTools(oldBaseElement, newBaseElement, labelMap);
        relocateTools(oldBaseElement, wiringElement, labelMap);
        relocateTools(gatesElement, wiringElement, labelMap);
        updateFromLabelMap(XmlIterator.forDescendantElements(root, "comp"), labelMap);
        updateFromLabelMap(XmlIterator.forDescendantElements(root, "tool"), labelMap);
    }

    private void addToLabelMap(HashMap<String, String> labelMap, String sourceLabel,
            String destinationLabel, String toolNames) {
        if (sourceLabel != null && destinationLabel != null) {
            for (String tool : toolNames.split(";")) {
                labelMap.put(sourceLabel + ":" + tool, destinationLabel);
            }
        }
    }

    private void relocateTools(Element source, Element destination, HashMap<String, String> labelMap) {
        if (source == null || source == destination) {
            return;
        }
        String srcLabel = source.getAttribute("name");
        if (srcLabel == null) {
            return;
        }

        ArrayList<Element> elementsToRemove = new ArrayList<>();
        for (Element element : XmlIterator.forChildElements(source, "tool")) {
            String name = element.getAttribute("name");
            if (name != null && labelMap.containsKey(srcLabel + ":" + name)) {
                elementsToRemove.add(element);
            }
        }
        for (Element element : elementsToRemove) {
            source.removeChild(element);
            if (destination != null) {
                destination.appendChild(element);
            }
        }
    }

    private void updateFromLabelMap(Iterable<Element> elements, HashMap<String, String> labelMap) {
        for (Element element : elements) {
            String oldLib = element.getAttribute("lib");
            String name = element.getAttribute("name");
            if (oldLib != null && name != null) {
                String newLib = labelMap.get(oldLib + ":" + name);
                if (newLib != null) {
                    element.setAttribute("lib", newLib);
                }
            }
        }
    }

    private void repairForLegacyLibrary(Document document, Element root) {
        Element legacyElement = null;
        String legacyLabel = null;
        for (Element libElement : XmlIterator.forChildElements(root, "lib")) {
            String desc = libElement.getAttribute("desc");
            String label = libElement.getAttribute("name");
            if (desc != null && desc.equals("#Legacy")) {
                legacyElement = libElement;
                legacyLabel = label;
            }
        }

        if (legacyElement != null) {
            root.removeChild(legacyElement);

            ArrayList<Element> elementsToRemove = new ArrayList<>();
            findLibraryUses(elementsToRemove, legacyLabel,
                    XmlIterator.forDescendantElements(root, "comp"));
            boolean componentsRemoved = !elementsToRemove.isEmpty();
            findLibraryUses(elementsToRemove, legacyLabel,
                    XmlIterator.forDescendantElements(root, "tool"));
            for (Element elt : elementsToRemove) {
                elt.getParentNode().removeChild(elt);
            }
            if (componentsRemoved) {
                String error = "Some components have been deleted;"
                        + " the Legacy library is no longer supported.";
                Element element = document.createElement("message");
                element.setAttribute("value", error);
                root.appendChild(element);
            }
        }
    }

    static class CircuitData {

        Element circuitElement;
        Circuit circuit;
        Map<Element, Component> knownComponents;
        List<AbstractCanvasObject> appearance;

        public CircuitData(Element circuitElement, Circuit circuit) {
            this.circuitElement = circuitElement;
            this.circuit = circuit;
        }
    }

    class ReadContext {

        private LogisimFile file;
        private LogisimVersion sourceVersion;
        private HashMap<String, Library> libraries = new HashMap<>();
        private ArrayList<String> messages;

        private ReadContext(LogisimFile file) {
            this.file = file;
            this.messages = new ArrayList<>();
        }

        private void addError(String message, String context) {
            messages.add(message + " [" + context + "]");
        }

        void addErrors(XmlReaderException exception, String context) {
            for (String message : exception.getMessages()) {
                messages.add(message + " [" + context + "]");
            }
        }

        private void toLogisimFile(Element element) {
            // determine the version producing this file
            String versionString = element.getAttribute("source");
            if (versionString.equals("")) {
                sourceVersion = Main.VERSION;
            } else {
                sourceVersion = LogisimVersion.parse(versionString);
            }

            // first, load the sub-libraries
            for (Element childElement : XmlIterator.forChildElements(element, "lib")) {
                Library library = toLibrary(childElement);
                if (library != null) {
                    file.addLibrary(library);
                }
            }

            // second, create the circuits - empty for now
            List<CircuitData> circuitsData = new ArrayList<>();
            for (Element circuitElement : XmlIterator.forChildElements(element, "circuit")) {
                String name = circuitElement.getAttribute("name");
                if (name == null || name.equals("")) {
                    addError(Strings.get("circNameMissingError"), "C??");
                }
                CircuitData circuitData = new CircuitData(circuitElement, new Circuit(name));
                file.addCircuit(circuitData.circuit);
                circuitData.knownComponents = loadKnownComponents(circuitElement);
                for (Element appearElement : XmlIterator.forChildElements(circuitElement, "appear")) {
                    loadAppearance(appearElement, circuitData, name + ".appear");
                }
                circuitsData.add(circuitData);
            }

            // third, process the other child elements
            for (Element childElement : XmlIterator.forChildElements(element)) {
                String name = childElement.getTagName();
                switch (name) {
                    case "circuit":
                        break;
                    case "lib":
                        // Nothing to do: Done earlier.
                        break;
                    case "options":
                        try {
                            initAttributeSet(childElement, file.getOptions().getAttributeSet(), null);
                        } catch (XmlReaderException e) {
                            addErrors(e, "options");
                        }
                        break;
                    case "mappings":
                        initMouseMappings(childElement);
                        break;
                    case "toolbar":
                        initToolbarData(childElement);
                        break;
                    case "main":
                        String main = childElement.getAttribute("name");
                        Circuit circuit = file.getCircuit(main);
                        if (circuit != null) {
                            file.setMainCircuit(circuit);
                        }
                        break;
                    case "message":
                        file.addMessage(childElement.getAttribute("value"));
                        break;
                }
            }

            // fourth, execute a transaction that initializes all the circuits
            XmlCircuitReader builder;
            builder = new XmlCircuitReader(this, circuitsData);
            builder.execute();
        }

        private Library toLibrary(Element elt) {
            if (!elt.hasAttribute("name")) {
                loader.showError(Strings.get("libNameMissingError"));
                return null;
            }
            if (!elt.hasAttribute("desc")) {
                loader.showError(Strings.get("libDescMissingError"));
                return null;
            }
            String name = elt.getAttribute("name");
            String desc = elt.getAttribute("desc");
            Library ret = loader.loadLibrary(desc);
            if (ret == null) {
                return null;
            }
            libraries.put(name, ret);
            for (Element sub_elt : XmlIterator.forChildElements(elt, "tool")) {
                if (!sub_elt.hasAttribute("name")) {
                    loader.showError(Strings.get("toolNameMissingError"));
                } else {
                    String tool_str = sub_elt.getAttribute("name");
                    Tool tool = ret.getTool(tool_str);
                    if (tool != null) {
                        try {
                            initAttributeSet(sub_elt, tool.getAttributeSet(), tool);
                        } catch (XmlReaderException e) {
                            addErrors(e, "lib." + name + "." + tool_str);
                        }
                    }
                }
            }
            return ret;
        }

        private Map<Element, Component> loadKnownComponents(Element elt) {
            Map<Element, Component> known = new HashMap<>();
            for (Element sub : XmlIterator.forChildElements(elt, "comp")) {
                try {
                    Component comp = XmlCircuitReader.getComponent(sub, this);
                    known.put(sub, comp);
                } catch (XmlReaderException e) {
                }
            }
            return known;
        }

        private void loadAppearance(Element appearElt, CircuitData circData,
                String context) {
            Map<Location, Instance> pins = new HashMap<>();
            for (Component comp : circData.knownComponents.values()) {
                if (comp.getFactory() == Pin.FACTORY) {
                    Instance instance = Instance.getInstanceFor(comp);
                    pins.put(comp.getLocation(), instance);
                }
            }

            List<AbstractCanvasObject> shapes = new ArrayList<>();
            for (Element sub : XmlIterator.forChildElements(appearElt)) {
                try {
                    AbstractCanvasObject m = AppearanceSvgReader.createShape(sub, pins);
                    if (m == null) {
                        addError(Strings.get("fileAppearanceNotFound", sub.getTagName()),
                                context + "." + sub.getTagName());
                    } else {
                        shapes.add(m);
                    }
                } catch (RuntimeException e) {
                    addError(Strings.get("fileAppearanceError", sub.getTagName()),
                            context + "." + sub.getTagName());
                }
            }
            if (!shapes.isEmpty()) {
                if (circData.appearance == null) {
                    circData.appearance = shapes;
                } else {
                    circData.appearance.addAll(shapes);
                }
            }
        }

        private void initMouseMappings(Element elt) {
            MouseMappings map = file.getOptions().getMouseMappings();
            for (Element sub_elt : XmlIterator.forChildElements(elt, "tool")) {
                Tool tool;
                try {
                    tool = toTool(sub_elt);
                } catch (XmlReaderException e) {
                    addErrors(e, "mapping");
                    continue;
                }

                String mods_str = sub_elt.getAttribute("map");
                if (mods_str == null || mods_str.equals("")) {
                    loader.showError(Strings.get("mappingMissingError"));
                    continue;
                }
                int mods;
                try {
                    mods = InputEventUtil.fromString(mods_str);
                } catch (NumberFormatException e) {
                    loader.showError(StringUtil.format(
                            Strings.get("mappingBadError"), mods_str));
                    continue;
                }

                tool = tool.cloneTool();
                try {
                    initAttributeSet(sub_elt, tool.getAttributeSet(), tool);
                } catch (XmlReaderException e) {
                    addErrors(e, "mapping." + tool.getName());
                }

                map.setToolFor(mods, tool);
            }
        }

        private void initToolbarData(Element elt) {
            ToolbarData toolbar = file.getOptions().getToolbarData();
            for (Element sub_elt : XmlIterator.forChildElements(elt)) {
                if (sub_elt.getTagName().equals("sep")) {
                    toolbar.addSeparator();
                } else if (sub_elt.getTagName().equals("tool")) {
                    Tool tool;
                    try {
                        tool = toTool(sub_elt);
                    } catch (XmlReaderException e) {
                        addErrors(e, "toolbar");
                        continue;
                    }
                    if (tool != null) {
                        tool = tool.cloneTool();
                        try {
                            initAttributeSet(sub_elt, tool.getAttributeSet(), tool);
                        } catch (XmlReaderException e) {
                            addErrors(e, "toolbar." + tool.getName());
                        }
                        toolbar.addTool(tool);
                    }
                }
            }
        }

        private Tool toTool(Element elt) throws XmlReaderException {
            Library lib = findLibrary(elt.getAttribute("lib"));
            String name = elt.getAttribute("name");
            if (name == null || name.equals("")) {
                throw new XmlReaderException(Strings.get("toolNameMissing"));
            }
            Tool tool = lib.getTool(name);
            if (tool == null) {
                throw new XmlReaderException(Strings.get("toolNotFound"));
            }
            return tool;
        }

        void initAttributeSet(Element parentElt, AttributeSet attrs,
                AttributeDefaultProvider defaults) throws XmlReaderException {
            ArrayList<String> messages = null;

            HashMap<String, String> attrsDefined = new HashMap<>();
            for (Element attrElt : XmlIterator.forChildElements(parentElt, "a")) {
                if (!attrElt.hasAttribute("name")) {
                    if (messages == null) {
                        messages = new ArrayList<>();
                    }
                    messages.add(Strings.get("attrNameMissingError"));
                } else {
                    String attrName = attrElt.getAttribute("name");
                    String attrVal;
                    if (attrElt.hasAttribute("val")) {
                        attrVal = attrElt.getAttribute("val");
                    } else {
                        attrVal = attrElt.getTextContent();
                    }
                    attrsDefined.put(attrName, attrVal);
                }
            }

            if (attrs == null) {
                return;
            }

            LogisimVersion ver = sourceVersion;
            boolean setDefaults = defaults != null
                    && !defaults.isAllDefaultValues(attrs, ver);
            // We need to process this in order, and we have to refetch the
            // attribute list each time because it may change as we iterate
            // (as it will for a splitter).
            for (int i = 0; true; i++) {
                List<Attribute<?>> attrList = attrs.getAttributes();
                if (i >= attrList.size()) {
                    break;
                }
                @SuppressWarnings("unchecked")
                Attribute<Object> attr = (Attribute<Object>) attrList.get(i);
                String attrName = attr.getName();
                String attrVal = attrsDefined.get(attrName);
                if (attrVal == null) {
                    if (setDefaults) {
                        Object val = defaults.getDefaultAttributeValue(attr, ver);
                        if (val != null) {
                            attrs.setValue(attr, val);
                        }
                    }
                } else {
                    try {
                        Object val = attr.parse(attrVal);
                        attrs.setValue(attr, val);
                    } catch (NumberFormatException e) {
                        if (messages == null) {
                            messages = new ArrayList<>();
                        }
                        messages.add(StringUtil.format(
                                Strings.get("attrValueInvalidError"),
                                attrVal, attrName));
                    }
                }
            }
            if (messages != null) {
                throw new XmlReaderException(messages);
            }
        }

        Library findLibrary(String lib_name) throws XmlReaderException {
            if (lib_name == null || lib_name.equals("")) {
                return file;
            }

            Library ret = libraries.get(lib_name);
            if (ret == null) {
                throw new XmlReaderException(StringUtil.format(
                        Strings.get("libMissingError"), lib_name));
            } else {
                return ret;
            }
        }
    }
}

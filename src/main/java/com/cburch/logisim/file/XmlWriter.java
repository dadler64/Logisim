/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.file;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.Main;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeDefaultProvider;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.InputEventUtil;
import com.cburch.logisim.util.StringUtil;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class XmlWriter {

    private LogisimFile file;
    private Document document;
    private LibraryLoader libraryLoader;
    private HashMap<Library, String> libraries = new HashMap<>();

    private XmlWriter(LogisimFile file, Document document, LibraryLoader libraryLoader) {
        this.file = file;
        this.document = document;
        this.libraryLoader = libraryLoader;
    }

    static void write(LogisimFile file, OutputStream outputStream, LibraryLoader libraryLoader)
            throws ParserConfigurationException,
            TransformerException {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

        Document document = docBuilder.newDocument();
        XmlWriter context = new XmlWriter(file, document, libraryLoader);
        context.fromLogisimFile();

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            transformerFactory.setAttribute("indent-number", 2);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        try {
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        Source src = new DOMSource(document);
        Result dest = new StreamResult(outputStream);
        transformer.transform(src, dest);
    }

    private Element fromLogisimFile() {
        Element element = document.createElement("project");
        document.appendChild(element);
        element.appendChild(document.createTextNode("\nThis file is intended to be loaded by Logisim (http://www.cburch.com/logisim/).\n"));
        element.setAttribute("version", "1.0");
        element.setAttribute("source", Main.VERSION_NAME);

        for (Library library : file.getLibraries()) {
            Element libraryElement = fromLibrary(library);
            if (libraryElement != null) {
                element.appendChild(libraryElement);
            }
        }

        if (file.getMainCircuit() != null) {
            Element mainElement = document.createElement("main");
            mainElement.setAttribute("name", file.getMainCircuit().getName());
            element.appendChild(mainElement);
        }

        element.appendChild(fromOptions());
        element.appendChild(fromMouseMappings());
        element.appendChild(fromToolbarData());

        for (Circuit circuit : file.getCircuits()) {
            element.appendChild(fromCircuit(circuit));
        }
        return element;
    }

    Element fromLibrary(Library lib) {
        Element ret = document.createElement("lib");
        if (libraries.containsKey(lib)) {
            return null;
        }
        String name = "" + libraries.size();
        String desc = libraryLoader.getDescriptor(lib);
        if (desc == null) {
            libraryLoader.showError("library location unknown: "
                    + lib.getName());
            return null;
        }
        libraries.put(lib, name);
        ret.setAttribute("name", name);
        ret.setAttribute("desc", desc);
        for (Tool t : lib.getTools()) {
            AttributeSet attrs = t.getAttributeSet();
            if (attrs != null) {
                Element toAdd = document.createElement("tool");
                toAdd.setAttribute("name", t.getName());
                addAttributeSetContent(toAdd, attrs, t);
                if (toAdd.getChildNodes().getLength() > 0) {
                    ret.appendChild(toAdd);
                }
            }
        }
        return ret;
    }

    Element fromOptions() {
        Element elt = document.createElement("options");
        addAttributeSetContent(elt, file.getOptions().getAttributeSet(), null);
        return elt;
    }

    private Element fromMouseMappings() {
        Element element = document.createElement("mappings");
        MouseMappings map = file.getOptions().getMouseMappings();
        for (Map.Entry<Integer, Tool> entry : map.getMappings().entrySet()) {
            Integer mods = entry.getKey();
            Tool tool = entry.getValue();
            Element toolElement = fromTool(tool);
            String mapValue = InputEventUtil.toString(mods);
            assert toolElement != null;
            toolElement.setAttribute("map", mapValue);
            element.appendChild(toolElement);
        }
        return element;
    }

    private Element fromToolbarData() {
        Element element = document.createElement("toolbar");
        ToolbarData toolbar = file.getOptions().getToolbarData();
        for (Tool tool : toolbar.getContents()) {
            if (tool == null) {
                element.appendChild(document.createElement("sep"));
            } else {
                element.appendChild(fromTool(tool));
            }
        }
        return element;
    }

    private Element fromTool(Tool tool) {
        Library library = findLibrary(tool);
        String libraryName;
        if (library == null) {
            libraryLoader.showError(StringUtil.format("tool `%s' not found",
                    tool.getDisplayName()));
            return null;
        } else if (library == file) {
            libraryName = null;
        } else {
            libraryName = libraries.get(library);
            if (libraryName == null) {
                libraryLoader.showError("unknown library within file");
                return null;
            }
        }

        Element element = document.createElement("tool");
        if (libraryName != null) {
            element.setAttribute("lib", libraryName);
        }
        element.setAttribute("name", tool.getName());
        addAttributeSetContent(element, tool.getAttributeSet(), tool);
        return element;
    }

    private Element fromCircuit(Circuit circuit) {
        Element CircuitElement = document.createElement("circuit");
        CircuitElement.setAttribute("name", circuit.getName());
        addAttributeSetContent(CircuitElement, circuit.getStaticAttributes(), null);
        if (!circuit.getAppearance().isDefaultAppearance()) {
            Element appearElement = document.createElement("appear");
            for (Object object : circuit.getAppearance().getObjectsFromBottom()) {
                if (object instanceof AbstractCanvasObject) {
                    Element svgElement = ((AbstractCanvasObject) object).toSvgElement(document);
                    if (svgElement != null) {
                        appearElement.appendChild(svgElement);
                    }
                }
            }
            CircuitElement.appendChild(appearElement);
        }
        for (Wire w : circuit.getWires()) {
            CircuitElement.appendChild(fromWire(w));
        }
        for (Component comp : circuit.getNonWires()) {
            Element elt = fromComponent(comp);
            if (elt != null) {
                CircuitElement.appendChild(elt);
            }
        }
        return CircuitElement;
    }

    Element fromComponent(Component comp) {
        ComponentFactory source = comp.getFactory();
        Library lib = findLibrary(source);
        String lib_name;
        if (lib == null) {
            libraryLoader.showError(source.getName() + " component not found");
            return null;
        } else if (lib == file) {
            lib_name = null;
        } else {
            lib_name = libraries.get(lib);
            if (lib_name == null) {
                libraryLoader.showError("unknown library within file");
                return null;
            }
        }

        Element ret = document.createElement("comp");
        if (lib_name != null) {
            ret.setAttribute("lib", lib_name);
        }
        ret.setAttribute("name", source.getName());
        ret.setAttribute("loc", comp.getLocation().toString());
        addAttributeSetContent(ret, comp.getAttributeSet(), comp.getFactory());
        return ret;
    }

    private Element fromWire(Wire wire) {
        Element element = document.createElement("wire");
        element.setAttribute("from", wire.getEnd0().toString());
        element.setAttribute("to", wire.getEnd1().toString());
        return element;
    }

    private void addAttributeSetContent(Element element, AttributeSet attributes,
            AttributeDefaultProvider source) {
        if (attributes == null) {
            return;
        }
        LogisimVersion version = Main.VERSION;
        if (source != null && source.isAllDefaultValues(attributes, version)) {
            return;
        }
        for (Attribute<?> attributeBase : attributes.getAttributes()) {
            @SuppressWarnings("unchecked")
            Attribute<Object> attribute = (Attribute<Object>) attributeBase;
            Object object = attributes.getValue(attribute);
            if (attributes.isToSave(attribute) && object != null) {
                Object defaultValue = source == null ? null : source.getDefaultAttributeValue(attribute, version);
                if (defaultValue == null || !defaultValue.equals(object)) {
                    Element a = document.createElement("a");
                    a.setAttribute("name", attribute.getName());
                    String value = attribute.toStandardString(object);
                    if (value.indexOf('\n') >= 0) {
                        a.appendChild(document.createTextNode(value));
                    } else {
                        a.setAttribute("val", attribute.toStandardString(object));
                    }
                    element.appendChild(a);
                }
            }
        }
    }

    private Library findLibrary(Tool tool) {
        if (libraryContains(file, tool)) {
            return file;
        }
        for (Library library : file.getLibraries()) {
            if (libraryContains(library, tool)) {
                return library;
            }
        }
        return null;
    }

    private Library findLibrary(ComponentFactory source) {
        if (file.contains(source)) {
            return file;
        }
        for (Library library : file.getLibraries()) {
            if (library.contains(source)) {
                return library;
            }
        }
        return null;
    }

    private boolean libraryContains(Library library, Tool query) {
        for (Tool tool : library.getTools()) {
            if (tool.sharesSource(query)) {
                return true;
            }
        }
        return false;
    }
}

/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.prefs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Template {

    private String contents;

    private Template(String contents) {
        this.contents = contents;
    }

    public static Template createEmpty() {
        String circName = Strings.get("newCircuitName");
        StringBuilder buf = new StringBuilder();
        buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        buf.append("<project version=\"1.0\">");
        buf.append(" <circuit name=\"" + circName + "\" />");
        buf.append("</project>");
        return new Template(buf.toString());
    }

    public static Template create(InputStream in) {
        InputStreamReader reader = new InputStreamReader(in);
        char[] buf = new char[4096];
        StringBuilder dest = new StringBuilder();
        while (true) {
            try {
                int nbytes = reader.read(buf);
                if (nbytes < 0) {
                    break;
                }
                dest.append(buf, 0, nbytes);
            } catch (IOException e) {
                break;
            }
        }
        return new Template(dest.toString());
    }

    public InputStream createStream() {
        return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    }
}

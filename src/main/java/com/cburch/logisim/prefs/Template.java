/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.prefs;

import com.adlerd.logger.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class Template {

    private final String contents;

    private Template(String contents) {
        this.contents = contents;
    }

    public static Template createEmpty() {
        String circName = Strings.get("newCircuitName");
        String builder = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<project version=\"1.0\">"
            + " <circuit name=\""
            + circName
            + "\" />"
            + "</project>";
        return new Template(builder);
    }

    public static Template create(InputStream in) {
        InputStreamReader reader = new InputStreamReader(in);
        char[] buffer = new char[4096];
        StringBuilder builder = new StringBuilder();
        while (true) {
            try {
                int nBytes = reader.read(buffer);
                if (nBytes < 0) {
                    break;
                }
                builder.append(buffer, 0, nBytes);
            } catch (IOException e) {
                Logger.errorln(e);
                break;
            }
        }
        return new Template(builder.toString());
    }

    public InputStream createStream() {
        return new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
    }
}

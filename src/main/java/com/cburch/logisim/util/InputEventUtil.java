/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

public class InputEventUtil {

    private static final String CTRL = "Ctrl";
    private static final String SHIFT = "Shift";
    private static final String ALT = "Alt";
    private static final String BUTTON1 = "Button1";
    private static final String BUTTON2 = "Button2";
    private static final String BUTTON3 = "Button3";

    private InputEventUtil() {
    }

    public static int fromString(String input) {
        int ret = 0;
        StringTokenizer tokenizer = new StringTokenizer(input);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            switch (token) {
                case CTRL:
                    ret |= InputEvent.CTRL_DOWN_MASK;
                    break;
                case SHIFT:
                    ret |= InputEvent.SHIFT_DOWN_MASK;
                    break;
                case ALT:
                    ret |= InputEvent.ALT_DOWN_MASK;
                    break;
                case BUTTON1:
                    ret |= InputEvent.BUTTON1_DOWN_MASK;
                    break;
                case BUTTON2:
                    ret |= InputEvent.BUTTON2_DOWN_MASK;
                    break;
                case BUTTON3:
                    ret |= InputEvent.BUTTON3_DOWN_MASK;
                    break;
                default:
                    throw new NumberFormatException("InputEventUtil");
            }
        }
        return ret;
    }

    public static String toString(int mods) {
        ArrayList<String> list = new ArrayList<>();
        if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
            list.add(CTRL);
        }
        if ((mods & InputEvent.ALT_DOWN_MASK) != 0) {
            list.add(ALT);
        }
        if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) {
            list.add(SHIFT);
        }
        if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            list.add(BUTTON1);
        }
        if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0) {
            list.add(BUTTON2);
        }
        if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0) {
            list.add(BUTTON3);
        }

        Iterator<String> iterator = list.iterator();
        if (iterator.hasNext()) {
            StringBuilder ret = new StringBuilder();
            ret.append(iterator.next());
            while (iterator.hasNext()) {
                ret.append(" ");
                ret.append(iterator.next());
            }
            return ret.toString();
        } else {
            return "";
        }
    }

    public static int fromDisplayString(String input) {
        int ret = 0;
        StringTokenizer tokenizer = new StringTokenizer(input);
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            if (token.equals(Strings.get("ctrlMod"))) {
                ret |= InputEvent.CTRL_DOWN_MASK;
            } else if (token.equals(Strings.get("altMod"))) {
                ret |= InputEvent.ALT_DOWN_MASK;
            } else if (token.equals(Strings.get("shiftMod"))) {
                ret |= InputEvent.SHIFT_DOWN_MASK;
            } else if (token.equals(Strings.get("button1Mod"))) {
                ret |= InputEvent.BUTTON1_DOWN_MASK;
            } else if (token.equals(Strings.get("button2Mod"))) {
                ret |= InputEvent.BUTTON2_DOWN_MASK;
            } else if (token.equals(Strings.get("button3Mod"))) {
                ret |= InputEvent.BUTTON3_DOWN_MASK;
            } else {
                throw new NumberFormatException("InputEventUtil");
            }
        }
        return ret;
    }

    public static String toDisplayString(int mods) {
        ArrayList<String> list = new ArrayList<>();
        if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
            list.add(Strings.get("ctrlMod"));
        }
        if ((mods & InputEvent.ALT_DOWN_MASK) != 0) {
            list.add(Strings.get("altMod"));
        }
        if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) {
            list.add(Strings.get("shiftMod"));
        }
        if ((mods & InputEvent.BUTTON1_DOWN_MASK) != 0) {
            list.add(Strings.get("button1Mod"));
        }
        if ((mods & InputEvent.BUTTON2_DOWN_MASK) != 0) {
            list.add(Strings.get("button2Mod"));
        }
        if ((mods & InputEvent.BUTTON3_DOWN_MASK) != 0) {
            list.add(Strings.get("button3Mod"));
        }

        if (list.isEmpty()) {
            return "";
        }

        Iterator<String> iterator = list.iterator();
        if (iterator.hasNext()) {
            StringBuilder builder = new StringBuilder();
            builder.append(iterator.next());
            while (iterator.hasNext()) {
                builder.append(" ");
                builder.append(iterator.next());
            }
            return builder.toString();
        } else {
            return "";
        }
    }

    public static String toKeyDisplayString(int mods) {
        ArrayList<String> list = new ArrayList<>();
        if ((mods & ActionEvent.META_MASK) != 0) {
            list.add(Strings.get("metaMod"));
        }
        if ((mods & ActionEvent.CTRL_MASK) != 0) {
            list.add(Strings.get("ctrlMod"));
        }
        if ((mods & ActionEvent.ALT_MASK) != 0) {
            list.add(Strings.get("altMod"));
        }
        if ((mods & ActionEvent.SHIFT_MASK) != 0) {
            list.add(Strings.get("shiftMod"));
        }

        Iterator<String> iterator = list.iterator();
        if (iterator.hasNext()) {
            StringBuilder builder = new StringBuilder();
            builder.append(iterator.next());
            while (iterator.hasNext()) {
                builder.append(" ");
                builder.append(iterator.next());
            }
            return builder.toString();
        } else {
            return "";
        }
    }
}

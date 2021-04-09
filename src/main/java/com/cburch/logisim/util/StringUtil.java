/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

public class StringUtil {

    private StringUtil() {
    }

    public static String capitalize(String a) {
        return Character.toTitleCase(a.charAt(0)) + a.substring(1);
    }

    public static String format(String format, String a1) {
        return format(format, a1, null, null);
    }

    public static String format(String format, String a1, String a2) {
        return format(format, a1, a2, null);
    }

    public static String format(String format, String a1, String a2, String a3) {
        StringBuilder ret = new StringBuilder();
        if (a1 == null) {
            a1 = "(null)";
        }
        if (a2 == null) {
            a2 = "(null)";
        }
        if (a3 == null) {
            a3 = "(null)";
        }
        int arg = 0;
        int position = 0;
        int next = format.indexOf('%');
        while (next >= 0) {
            ret.append(format, position, next);
            char c = format.charAt(next + 1);
            if (c == 's') {
                position = next + 2;
                switch (arg) {
                    case 0:
                        ret.append(a1);
                        break;
                    case 1:
                        ret.append(a2);
                        break;
                    default:
                        ret.append(a3);
                }
                ++arg;
            } else if (c == '$') {
                switch (format.charAt(next + 2)) {
                    case '1':
                        ret.append(a1);
                        position = next + 3;
                        break;
                    case '2':
                        ret.append(a2);
                        position = next + 3;
                        break;
                    case '3':
                        ret.append(a3);
                        position = next + 3;
                        break;
                    default:
                        ret.append("%$");
                        position = next + 2;
                }
            } else if (c == '%') {
                ret.append('%');
                position = next + 2;
            } else {
                ret.append('%');
                position = next + 1;
            }
            next = format.indexOf('%', position);
        }
        ret.append(format.substring(position));
        return ret.toString();
    }

    public static StringGetter formatter(final StringGetter base, final String arg) {
        return () -> format(base.get(), arg);
    }

    public static StringGetter formatter(final StringGetter base, final StringGetter arg) {
        return () -> format(base.get(), arg.get());
    }

    public static StringGetter constantGetter(final String value) {
        return () -> value;
    }

    public static String toHexString(int bits, int value) {
        if (bits < 32) {
            value &= (1 << bits) - 1;
        }
        StringBuilder builder = new StringBuilder(Integer.toHexString(value));
        int length = (bits + 3) / 4;
        while (builder.length() < length) {
            builder.insert(0, "0");
        }
        if (builder.length() > length) {
            builder = new StringBuilder(builder.substring(builder.length() - length));
        }
        return builder.toString();
    }
}

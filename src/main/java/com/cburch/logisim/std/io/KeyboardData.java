/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import java.awt.FontMetrics;

class KeyboardData implements InstanceData, Cloneable {

    private Value lastClock;
    private char[] buffer;
    private String str;
    private int bufferLength;
    private int cursorPos;
    private boolean displayValid;
    private int displayStart;
    private int displayEnd;

    public KeyboardData(int capacity) {
        lastClock = Value.UNKNOWN;
        buffer = new char[capacity];
        clear();
    }

    @Override
    public Object clone() {
        try {
            KeyboardData ret = (KeyboardData) super.clone();
            ret.buffer = this.buffer.clone();
            return ret;
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }

    public Value setLastClock(Value newClock) {
        Value ret = lastClock;
        lastClock = newClock;
        return ret;
    }

    public boolean isDisplayValid() {
        return displayValid;
    }

    public int getDisplayStart() {
        return displayStart;
    }

    public int getDisplayEnd() {
        return displayEnd;
    }

    public int getCursorPosition() {
        return cursorPos;
    }

    public void updateBufferLength(int len) {
        synchronized (this) {
            char[] buf = buffer;
            int oldLen = buf.length;
            if (oldLen != len) {
                char[] newBuf = new char[len];
                System.arraycopy(buf, 0, newBuf, 0, Math.min(len, oldLen));
                if (len < oldLen) {
                    if (bufferLength > len) {
                        bufferLength = len;
                    }
                    if (cursorPos > len) {
                        cursorPos = len;
                    }
                }
                buffer = newBuf;
                str = null;
                displayValid = false;
            }
        }
    }

    @Override
    public String toString() {
        String s = str;
        if (s != null) {
            return s;
        }
        StringBuilder build = new StringBuilder();
        char[] buf = buffer;
        int len = bufferLength;
        for (int i = 0; i < len; i++) {
            char c = buf[i];
            build.append(Character.isISOControl(c) ? ' ' : c);
        }
        str = build.toString();
        return str;
    }

    public char getChar(int pos) {
        return pos >= 0 && pos < bufferLength ? buffer[pos] : '\0';
    }

    public int getNextSpecial(int pos) {
        char[] buf = buffer;
        int len = bufferLength;
        for (int i = pos; i < len; i++) {
            char c = buf[i];
            if (Character.isISOControl(c)) {
                return i;
            }
        }
        return -1;
    }

    public void clear() {
        bufferLength = 0;
        cursorPos = 0;
        str = "";
        displayValid = false;
        displayStart = 0;
        displayEnd = 0;
    }

    public char dequeue() {
        char[] buf = buffer;
        int len = bufferLength;
        if (len == 0) {
            return '\0';
        }
        char ret = buf[0];
        if (len - 1 >= 0) {
            System.arraycopy(buf, 1, buf, 0, len - 1);
        }
        bufferLength = len - 1;
        int pos = cursorPos;
        if (pos > 0) {
            cursorPos = pos - 1;
        }
        str = null;
        displayValid = false;
        return ret;
    }

    public boolean insert(char value) {
        char[] buf = buffer;
        int len = bufferLength;
        if (len >= buf.length) {
            return false;
        }
        int pos = cursorPos;
        if (len - pos >= 0) {
            System.arraycopy(buf, pos, buf, pos + 1, len - pos);
        }
        buf[pos] = value;
        bufferLength = len + 1;
        cursorPos = pos + 1;
        str = null;
        displayValid = false;
        return true;
    }

    public boolean delete() {
        char[] buf = buffer;
        int len = bufferLength;
        int pos = cursorPos;
        if (pos >= len) {
            return false;
        }
        if (len - (pos + 1) >= 0) {
            System.arraycopy(buf, pos + 1, buf, pos + 1 - 1, len - (pos + 1));
        }
        bufferLength = len - 1;
        str = null;
        displayValid = false;
        return true;
    }

    public boolean moveCursorBy(int delta) {
        int len = bufferLength;
        int pos = cursorPos;
        int newPos = pos + delta;
        if (newPos < 0 || newPos > len) {
            return false;
        }
        cursorPos = newPos;
        displayValid = false;
        return true;
    }

    public boolean setCursor(int value) {
        int len = bufferLength;
        if (value > len) {
            value = len;
        }
        int pos = cursorPos;
        if (pos == value) {
            return false;
        }
        cursorPos = value;
        displayValid = false;
        return true;
    }

    public void updateDisplay(FontMetrics fm) {
        if (displayValid) {
            return;
        }
        int cursorPos = this.cursorPos;
        int i0 = displayStart;
        int i1 = displayEnd;
        String str = toString();
        int len = str.length();
        int max = Keyboard.WIDTH - 8 - 4;
        if (str.equals("") || fm.stringWidth(str) <= max) {
            i0 = 0;
            i1 = len;
        } else {
            // grow to include end of string if possible
            int w0 = fm.stringWidth(str.charAt(0) + "m");
            int w1 = fm.stringWidth("m");
            int w = i0 == 0 ? fm.stringWidth(str) : w0 + fm.stringWidth(str.substring(i0));
            if (w <= max) {
                i1 = len;
            }

            // rearrange start/end so as to include cursor
            if (cursorPos <= i0) {
                if (cursorPos < i0) {
                    i1 += cursorPos - i0;
                    i0 = cursorPos;
                }
                if (cursorPos == i0 && i0 > 0) {
                    i0--;
                    i1--;
                }
            }
            if (cursorPos >= i1) {
                if (cursorPos > i1) {
                    i0 += cursorPos - i1;
                    i1 = cursorPos;
                }
                if (cursorPos == i1 && i1 < len) {
                    i0++;
                    i1++;
                }
            }
            if (i0 <= 2) {
                i0 = 0;
            }

            // resize segment to fit
            if (fits(fm, str, w0, w1, i0, i1, max)) { // maybe should grow
                while (fits(fm, str, w0, w1, i0, i1 + 1, max)) {
                    i1++;
                }
                while (fits(fm, str, w0, w1, i0 - 1, i1, max)) {
                    i0--;
                }
            } else { // should shrink
                if (cursorPos < (i0 + i1) / 2) {
                    i1--;
                    while (!fits(fm, str, w0, w1, i0, i1, max)) {
                        i1--;
                    }
                } else {
                    i0++;
                    while (!fits(fm, str, w0, w1, i0, i1, max)) {
                        i0++;
                    }
                }

            }
            if (i0 == 1) {
                i0 = 0;
            }
        }
        displayStart = i0;
        displayEnd = i1;
        displayValid = true;
    }

    private boolean fits(FontMetrics fm, String str, int w0, int w1, int i0, int i1, int max) {
        if (i0 >= i1) {
            return true;
        }
        int len = str.length();
        if (i0 < 0 || i1 > len) {
            return false;
        }
        int w = fm.stringWidth(str.substring(i0, i1));
        if (i0 > 0) {
            w += w0;
        }
        if (i1 < str.length()) {
            w += w1;
        }
        return w <= max;
    }
}

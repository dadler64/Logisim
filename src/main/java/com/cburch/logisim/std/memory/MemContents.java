/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.std.memory;

import com.adlerd.logger.Logger;
import com.cburch.hex.HexModel;
import com.cburch.hex.HexModelListener;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.Arrays;

class MemContents implements Cloneable, HexModel {

    private static final int PAGE_SIZE_BITS = 12;
    private static final int PAGE_SIZE = 1 << PAGE_SIZE_BITS;
    private static final int PAGE_MASK = PAGE_SIZE - 1;
    private EventSourceWeakSupport<HexModelListener> listeners = null;
    private int width;
    private int addressBits;
    private int mask;
    private MemContentsSub.ContentsInterface[] pages;

    private MemContents(int addressBits, int width) {
        listeners = null;
        setDimensions(addressBits, width);
    }

    static MemContents create(int addrBits, int width) {
        return new MemContents(addrBits, width);
    }

    //
    // HexModel methods
    //
    public void addHexModelListener(HexModelListener listener) {
        if (listeners == null) {
            listeners = new EventSourceWeakSupport<>();
        }
        listeners.add(listener);
    }

    public void removeHexModelListener(HexModelListener listener) {
        if (listeners == null) {
            return;
        }
        listeners.add(listener);
        if (listeners.isEmpty()) {
            listeners = null;
        }
    }

    private void fireMetaInfoChanged() {
        if (listeners == null) {
            return;
        }
        boolean found = false;
        for (HexModelListener listener : listeners) {
            found = true;
            listener.metaInfoChanged(this);
        }
        if (!found) {
            listeners = null;
        }
    }

    private void fireBytesChanged(long start, long numBytes, int[] oldValues) {
        if (listeners == null) {
            return;
        }
        boolean found = false;
        for (HexModelListener listener : listeners) {
            found = true;
            listener.bytesChanged(this, start, numBytes, oldValues);
        }
        if (!found) {
            listeners = null;
        }
    }

    //
    // other methods
    //
    @Override
    public MemContents clone() {
        try {
            MemContents contents = (MemContents) super.clone();
            contents.listeners = null;
            contents.pages = new MemContentsSub.ContentsInterface[this.pages.length];
            for (int i = 0; i < contents.pages.length; i++) {
                if (this.pages[i] != null) {
                    contents.pages[i] = this.pages[i].clone();
                }
            }
            return contents;
        } catch (CloneNotSupportedException ex) {
            Logger.debugln(ex.getMessage());
            return this;
        }
    }

    public int getLogLength() {
        return addressBits;
    }

    public int getWidth() {
        return width;
    }

    public int get(long addr) {
        int page = (int) (addr >>> PAGE_SIZE_BITS);
        int offset = (int) (addr & PAGE_MASK);
        if (page < 0 || page >= pages.length || pages[page] == null) {
            return 0;
        }
        return pages[page].get(offset) & mask;
    }

    public boolean isClear() {
        for (MemContentsSub.ContentsInterface page : pages) {
            if (page != null) {
                for (int j = page.getLength() - 1; j >= 0; j--) {
                    if (page.get(j) != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public void set(long address, int value) {
        int page = (int) (address >>> PAGE_SIZE_BITS);
        int offset = (int) (address & PAGE_MASK);
        int old = pages[page] == null ? 0 : pages[page].get(offset) & mask;
        int val = value & mask;
        if (old != val) {
            if (pages[page] == null) {
                pages[page] = MemContentsSub.createContents(PAGE_SIZE, width);
            }
            pages[page].set(offset, val);
            fireBytesChanged(address, 1, new int[]{old});
        }
    }

    public void set(long start, int[] values) {
        if (values.length == 0) {
            return;
        }

        int pageStart = (int) (start >>> PAGE_SIZE_BITS);
        int startOffset = (int) (start & PAGE_MASK);
        int pageEnd = (int) ((start + values.length - 1) >>> PAGE_SIZE_BITS);
        int endOffset = (int) ((start + values.length - 1) & PAGE_MASK);

        if (pageStart == pageEnd) {
            ensurePage(pageStart);
            MemContentsSub.ContentsInterface page = pages[pageStart];
            if (!page.matches(values, startOffset, mask)) {
                int[] oldValues = page.get(startOffset, values.length);
                page.load(startOffset, values, mask);
                if (page.isClear()) {
                    pages[pageStart] = null;
                }
                fireBytesChanged(start, values.length, oldValues);
            }
        } else {
            int nextOffset;
            if (startOffset == 0) {
                pageStart--;
                nextOffset = 0;
            } else {
                ensurePage(pageStart);
                int[] vals = new int[PAGE_SIZE - startOffset];
                System.arraycopy(values, 0, vals, 0, vals.length);
                MemContentsSub.ContentsInterface page = pages[pageStart];
                if (!page.matches(vals, startOffset, mask)) {
                    int[] oldValues = page.get(startOffset, vals.length);
                    page.load(startOffset, vals, mask);
                    if (page.isClear()) {
                        pages[pageStart] = null;
                    }
                    fireBytesChanged(start, PAGE_SIZE - pageStart, oldValues);
                }
                nextOffset = vals.length;
            }
            int[] vals = new int[PAGE_SIZE];
            int offset = nextOffset;
            for (int i = pageStart + 1; i < pageEnd; i++, offset += PAGE_SIZE) {
                MemContentsSub.ContentsInterface page = pages[i];
                if (page == null) {
                    boolean allZeroes = true;
                    for (int j = 0; j < PAGE_SIZE; j++) {
                        if ((values[offset + j] & mask) != 0) {
                            allZeroes = false;
                            break;
                        }
                    }
                    if (!allZeroes) {
                        page = MemContentsSub.createContents(PAGE_SIZE, width);
                        pages[i] = page;
                    }
                }
                if (page != null) {
                    System.arraycopy(values, offset, vals, 0, PAGE_SIZE);
                    if (!page.matches(vals, startOffset, mask)) {
                        int[] oldValues = page.get(0, PAGE_SIZE);
                        page.load(0, vals, mask);
                        if (page.isClear()) {
                            pages[i] = null;
                        }
                        fireBytesChanged((long) i << PAGE_SIZE_BITS, PAGE_SIZE, oldValues);
                    }
                }
            }
            if (endOffset >= 0) {
                ensurePage(pageEnd);
                vals = new int[endOffset + 1];
                System.arraycopy(values, offset, vals, 0, endOffset + 1);
                MemContentsSub.ContentsInterface page = pages[pageEnd];
                if (!page.matches(vals, startOffset, mask)) {
                    int[] oldValues = page.get(0, endOffset + 1);
                    page.load(0, vals, mask);
                    if (page.isClear()) {
                        pages[pageEnd] = null;
                    }
                    fireBytesChanged((long) pageEnd << PAGE_SIZE_BITS, endOffset + 1, oldValues);
                }
            }
        }
    }

    public void fill(long start, long length, int value) {
        if (length == 0) {
            return;
        }

        int pageStart = (int) (start >>> PAGE_SIZE_BITS);
        int startOffset = (int) (start & PAGE_MASK);
        int pageEnd = (int) ((start + length - 1) >>> PAGE_SIZE_BITS);
        int endOffset = (int) ((start + length - 1) & PAGE_MASK);
        value &= mask;

        if (pageStart == pageEnd) {
            ensurePage(pageStart);
            int[] vals = new int[(int) length];
            Arrays.fill(vals, value);
            MemContentsSub.ContentsInterface page = pages[pageStart];
            if (!page.matches(vals, startOffset, mask)) {
                int[] oldValues = page.get(startOffset, (int) length);
                page.load(startOffset, vals, mask);
                if (value == 0 && page.isClear()) {
                    pages[pageStart] = null;
                }
                fireBytesChanged(start, length, oldValues);
            }
        } else {
            if (startOffset == 0) {
                pageStart--;
            } else {
                if (value == 0 && pages[pageStart] == null) {
                    // nothing to do
                } else {
                    ensurePage(pageStart);
                    int[] vals = new int[PAGE_SIZE - startOffset];
                    Arrays.fill(vals, value);
                    MemContentsSub.ContentsInterface page = pages[pageStart];
                    if (!page.matches(vals, startOffset, mask)) {
                        int[] oldValues = page.get(startOffset, vals.length);
                        page.load(startOffset, vals, mask);
                        if (value == 0 && page.isClear()) {
                            pages[pageStart] = null;
                        }
                        fireBytesChanged(start, PAGE_SIZE - pageStart, oldValues);
                    }
                }
            }
            if (value == 0) {
                for (int i = pageStart + 1; i < pageEnd; i++) {
                    if (pages[i] != null) {
                        clearPage(i);
                    }
                }
            } else {
                int[] vals = new int[PAGE_SIZE];
                Arrays.fill(vals, value);
                for (int i = pageStart + 1; i < pageEnd; i++) {
                    ensurePage(i);
                    MemContentsSub.ContentsInterface page = pages[i];
                    if (!page.matches(vals, 0, mask)) {
                        int[] oldValues = page.get(0, PAGE_SIZE);
                        page.load(0, vals, mask);
                        fireBytesChanged((long) i << PAGE_SIZE_BITS, PAGE_SIZE, oldValues);
                    }
                }
            }
            if (endOffset >= 0) {
                MemContentsSub.ContentsInterface page = pages[pageEnd];
                if (value == 0 && page == null) {
                    // nothing to do
                } else {
                    ensurePage(pageEnd);
                    int[] vals = new int[endOffset + 1];
                    Arrays.fill(vals, value);
                    if (!page.matches(vals, 0, mask)) {
                        int[] oldValues = page.get(0, endOffset + 1);
                        page.load(0, vals, mask);
                        if (value == 0 && page.isClear()) {
                            pages[pageEnd] = null;
                        }
                        fireBytesChanged((long) pageEnd << PAGE_SIZE_BITS, endOffset + 1, oldValues);
                    }
                }
            }
        }
    }

    public void clear() {
        for (int i = 0; i < pages.length; i++) {
            if (pages[i] != null) {
                if (pages[i] != null) {
                    clearPage(i);
                }
            }
        }
    }

    private void clearPage(int index) {
        MemContentsSub.ContentsInterface page = pages[index];
        int[] oldValues = new int[page.getLength()];
        boolean changed = false;
        for (int j = 0; j < oldValues.length; j++) {
            int val = page.get(j) & mask;
            oldValues[j] = val;
            if (val != 0) {
                changed = true;
            }
        }
        if (changed) {
            pages[index] = null;
            fireBytesChanged((long) index << PAGE_SIZE_BITS, oldValues.length, oldValues);
        }
    }

    public void setDimensions(int addressBits, int width) {
        if (addressBits == this.addressBits && width == this.width) {
            return;
        }
        this.addressBits = addressBits;
        this.width = width;
        this.mask = width == 32 ? 0xffffffff : ((1 << width) - 1);

        MemContentsSub.ContentsInterface[] oldPages = pages;
        int pageCount;
        int pageLength;
        if (addressBits < PAGE_SIZE_BITS) {
            pageCount = 1;
            pageLength = 1 << addressBits;
        } else {
            pageCount = 1 << (addressBits - PAGE_SIZE_BITS);
            pageLength = PAGE_SIZE;
        }
        pages = new MemContentsSub.ContentsInterface[pageCount];
        if (oldPages != null) {
            int n = Math.min(oldPages.length, pages.length);
            for (int i = 0; i < n; i++) {
                if (oldPages[i] != null) {
                    pages[i] = MemContentsSub.createContents(pageLength, width);
                    int m = Math.max(oldPages[i].getLength(), pageLength);
                    for (int j = 0; j < m; j++) {
                        pages[i].set(j, oldPages[i].get(j));
                    }
                }
            }
        }
        if (pageCount == 0 && pages[0] == null) {
            pages[0] = MemContentsSub.createContents(pageLength, width);
        }
        fireMetaInfoChanged();
    }

    public long getFirstOffset() {
        return 0;
    }

    public long getLastOffset() {
        return (1L << addressBits) - 1;
    }

    public int getValueWidth() {
        return width;
    }

    private void ensurePage(int index) {
        if (pages[index] == null) {
            pages[index] = MemContentsSub.createContents(PAGE_SIZE, width);
        }
    }
}

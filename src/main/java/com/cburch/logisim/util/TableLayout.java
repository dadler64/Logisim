/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.ArrayList;

public class TableLayout implements LayoutManager2 {

    private final int columnCount;
    private final ArrayList<Component[]> contents;
    private int currentRow;
    private int currentCol;
    private Dimension preferences;
    private int[] preferenceRow;
    private int[] preferenceColumn;
    private double[] rowWeight;

    public TableLayout(int columnCount) {
        this.columnCount = columnCount;
        this.contents = new ArrayList<>();
        this.currentRow = 0;
        this.currentCol = 0;
    }

    public void setRowWeight(int rowIndex, double weight) {
        if (weight < 0) {
            throw new IllegalArgumentException("weight must be nonnegative");
        }
        if (rowIndex < 0) {
            throw new IllegalArgumentException("row index must be nonnegative");
        }
        if ((rowWeight == null || rowIndex >= rowWeight.length) && weight != 0.0) {
            double[] a = new double[rowIndex + 10];
            if (rowWeight != null) {
                System.arraycopy(rowWeight, 0, a, 0, rowWeight.length);
            }
            rowWeight = a;
        }
        rowWeight[rowIndex] = weight;
    }

    public void addLayoutComponent(String name, Component comp) {
        while (currentRow >= contents.size()) {
            contents.add(new Component[columnCount]);
        }
        Component[] rowContents = contents.get(currentRow);
        rowContents[currentCol] = comp;
        ++currentCol;
        if (currentCol == columnCount) {
            ++currentRow;
            currentCol = 0;
        }
        preferences = null;
    }

    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints instanceof TableConstraints) {
            TableConstraints con = (TableConstraints) constraints;
            if (con.getRow() >= 0) {
                currentRow = con.getRow();
            }
            if (con.getCol() >= 0) {
                currentCol = con.getCol();
            }
        }
        addLayoutComponent("", comp);
    }

    public void removeLayoutComponent(Component component) {
        for (Component[] comp : contents) {
            for (int j = 0; j < comp.length; j++) {
                if (comp[j] == component) {
                    comp[j] = null;
                    return;
                }
            }
        }
        preferences = null;
    }

    public Dimension preferredLayoutSize(Container parent) {
        if (preferences == null) {
            int[] preferenceColumn = new int[columnCount];
            int[] preferenceRow = new int[contents.size()];
            int height = 0;
            for (int i = 0; i < preferenceRow.length; i++) {
                Component[] row = contents.get(i);
                int rowHeight = 0;
                for (int j = 0; j < row.length; j++) {
                    if (row[j] != null) {
                        Dimension dimension = row[j].getPreferredSize();
                        if (dimension.height > rowHeight) {
                            rowHeight = dimension.height;
                        }
                        if (dimension.width > preferenceColumn[j]) {
                            preferenceColumn[j] = dimension.width;
                        }
                    }
                }
                preferenceRow[i] = rowHeight;
                height += rowHeight;
            }
            int width = 0;
            for (int i : preferenceColumn) {
                width += i;
            }
            this.preferences = new Dimension(width, height);
            this.preferenceRow = preferenceRow;
            this.preferenceColumn = preferenceColumn;
        }
        return new Dimension(preferences);
    }

    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    public Dimension maximumLayoutSize(Container parent) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    public void layoutContainer(Container parent) {
        Dimension pref = preferredLayoutSize(parent);
        int[] preferenceRow = this.preferenceRow;
        int[] preferenceColumn = this.preferenceColumn;
        Dimension size = parent.getSize();

        double y0;
        int yRemaining = size.height - pref.height;
        double rowWeightTotal = 0.0;
        if (yRemaining != 0 && rowWeight != null) {
            for (double weight : rowWeight) {
                rowWeightTotal += weight;
            }
        }
        if (rowWeightTotal == 0.0 && yRemaining > 0) {
            y0 = yRemaining / 2.0;
        } else {
            y0 = 0;
        }

        int x0 = (size.width - pref.width) / 2;
        if (x0 < 0) {
            x0 = 0;
        }
        double y = y0;
        int i = -1;
        for (Component[] row : contents) {
            i++;
            int yRound = (int) (y + 0.5);
            int x = x0;
            for (int j = 0; j < row.length; j++) {
                Component comp = row[j];
                if (comp != null) {
                    row[j].setBounds(x, yRound, preferenceColumn[j], preferenceRow[i]);
                }
                x += preferenceColumn[j];
            }
            y += preferenceRow[i];
            if (rowWeightTotal > 0 && i < rowWeight.length) {
                y += yRemaining * rowWeight[i] / rowWeightTotal;
            }
        }

        // TODO Auto-generated method stub

    }

    public void invalidateLayout(Container parent) {
        preferences = null;
    }

}

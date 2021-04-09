/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Entry;
import com.cburch.logisim.analyze.model.Implicant;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.TruthTable;
import com.cburch.logisim.analyze.model.TruthTableEvent;
import com.cburch.logisim.analyze.model.TruthTableListener;
import com.cburch.logisim.analyze.model.VariableList;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Objects;
import javax.swing.JPanel;

class KarnaughMapPanel extends JPanel implements TruthTablePanel {

    private static final Font HEAD_FONT = new Font("Serif", Font.BOLD, 14);
    private static final Font BODY_FONT = new Font("Serif", Font.PLAIN, 14);
    private static final Color[] IMP_COLORS = new Color[]{
        new Color(255, 0, 0, 128),
        new Color(0, 150, 0, 128),
        new Color(0, 0, 255, 128),
        new Color(255, 0, 255, 128),
    };

    private static final int MAX_VARS = 4;

    private static final int[] ROW_VARS = {0, 0, 1, 1, 2};
    private static final int[] COLUMN_VARS = {0, 1, 1, 2, 2};
    private static final int CELL_HORIZONTAL_SEPARATION = 10;
    private static final int CELL_VERTICAL_SEPARATION = 10;
    private static final int IMPLICANT_INSET = 4;
    private static final int IMP_RADIUS = 5;
    private final AnalyzerModel model;
    private Entry provisionalValue = null;
    private int cellHeight = 1;
    private int cellWidth = 1;
    private int headHeight;
    private int provisionalX;
    private int provisionalY;
    private int tableHeight;
    private int tableWidth;
    private String output;

    public KarnaughMapPanel(AnalyzerModel model) {
        this.model = model;
        MyListener myListener = new MyListener();
        model.getOutputExpressions().addOutputExpressionsListener(myListener);
        model.getTruthTable().addTruthTableListener(myListener);
        setToolTipText(" ");
    }

    public void setOutput(String value) {
        boolean recompute = (output == null || value == null) && !Objects.equals(output, value);
        output = value;
        if (recompute) {
            computePreferredSize();
        } else {
            repaint();
        }
    }

    public TruthTable getTruthTable() {
        return model.getTruthTable();
    }

    public int getRow(MouseEvent event) {
        TruthTable table = model.getTruthTable();
        int inputs = table.getInputColumnCount();
        if (inputs >= ROW_VARS.length) {
            return -1;
        }
        int left = computeMargin(getWidth(), tableWidth);
        int top = computeMargin(getHeight(), tableHeight);
        int x = event.getX() - left - headHeight - cellWidth;
        int y = event.getY() - top - headHeight - cellHeight;
        if (x < 0 || y < 0) {
            return -1;
        }
        int row = y / cellHeight;
        int column = x / cellWidth;
        int rows = 1 << ROW_VARS[inputs];
        int columns = 1 << COLUMN_VARS[inputs];
        if (row >= rows || column >= columns) {
            return -1;
        }
        return getTableRow(row, column, rows, columns);
    }

    public int getOutputColumn(MouseEvent event) {
        return model.getOutputs().indexOf(output);
    }

    public void setEntryProvisional(int y, int x, Entry value) {
        provisionalY = y;
        provisionalX = x;
        provisionalValue = value;
        repaint();
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        TruthTable table = model.getTruthTable();
        int row = getRow(event);
        int column = getOutputColumn(event);
        Entry entry = table.getOutputEntry(row, column);
        return entry.getErrorMessage();
    }

    void localeChanged() {
        computePreferredSize();
        repaint();
    }

    private void computePreferredSize() {
        Graphics graphics = getGraphics();
        TruthTable table = model.getTruthTable();

        String message = null;
        if (output == null) {
            message = Strings.get("karnaughNoOutputError");
        } else if (table.getInputColumnCount() > MAX_VARS) {
            message = Strings.get("karnaughTooManyInputsError");
        }
        if (message != null) {
            if (graphics == null) {
                tableHeight = 15;
                tableWidth = 100;
            } else {
                FontMetrics fontMetrics = graphics.getFontMetrics(BODY_FONT);
                tableHeight = fontMetrics.getHeight();
                tableWidth = fontMetrics.stringWidth(message);
            }
            setPreferredSize(new Dimension(tableWidth, tableHeight));
            repaint();
            return;
        }

        if (graphics == null) {
            headHeight = 16;
            cellHeight = 16;
            cellWidth = 24;
        } else {
            FontMetrics headFontMetrics = graphics.getFontMetrics(HEAD_FONT);
            headHeight = headFontMetrics.getHeight();

            FontMetrics fontMetrics = graphics.getFontMetrics(BODY_FONT);
            cellHeight = fontMetrics.getAscent() + CELL_VERTICAL_SEPARATION;
            cellWidth = fontMetrics.stringWidth("00") + CELL_HORIZONTAL_SEPARATION;
        }

        int rows = 1 << ROW_VARS[table.getInputColumnCount()];
        int columns = 1 << COLUMN_VARS[table.getInputColumnCount()];
        tableWidth = headHeight + cellWidth * (columns + 1);
        tableHeight = headHeight + cellHeight * (rows + 1);
        setPreferredSize(new Dimension(tableWidth, tableHeight));
        invalidate();
        repaint();
    }

    @Override
    public void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        TruthTable table = model.getTruthTable();
        int inputCount = table.getInputColumnCount();
        Dimension size = getSize();
        String message = null;
        if (output == null) {
            message = Strings.get("karnaughNoOutputError");
        } else if (inputCount > MAX_VARS) {
            message = Strings.get("karnaughTooManyInputsError");
        }
        if (message != null) {
            graphics.setFont(BODY_FONT);
            GraphicsUtil.drawCenteredText(graphics, message, size.width / 2, size.height / 2);
            return;
        }

        int left = computeMargin(size.width, tableWidth);
        int top = computeMargin(size.height, tableHeight);
        int x = left;
        int y = top;
        int rowVars = ROW_VARS[inputCount];
        int columnVars = COLUMN_VARS[inputCount];
        int rows = 1 << rowVars;
        int columns = 1 << columnVars;

        graphics.setFont(HEAD_FONT);
        FontMetrics headFontMetrics = graphics.getFontMetrics();
        String rowHeader = header(0, rowVars);
        String columnHeader = header(rowVars, rowVars + columnVars);
        int xOffset = (tableWidth + headHeight + cellWidth - headFontMetrics.stringWidth(columnHeader)) / 2;
        graphics.drawString(columnHeader, x + xOffset, y + headFontMetrics.getAscent());
        int headerWidth = headFontMetrics.stringWidth(rowHeader);
        if (headerWidth <= headHeight) {
            int headX = x + (headHeight - headerWidth) / 2;
            int headY = y + (tableHeight + headHeight + cellHeight + headFontMetrics.getAscent()) / 2;
            graphics.drawString(rowHeader, headX, headY);
        } else if (graphics instanceof Graphics2D) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            int yOffset = (tableHeight + headHeight + cellHeight + headerWidth) / 2;
            int headX = x + headFontMetrics.getAscent();
            int headY = y + yOffset;
            graphics2D.rotate(-Math.PI / 2.0);
            graphics2D.drawString(rowHeader, -headY, headX);
            graphics2D.dispose();
        }

        x += headHeight;
        y += headHeight;
        graphics.setFont(BODY_FONT);
        FontMetrics fontMetrics = graphics.getFontMetrics();
        int deltaY = (cellHeight + fontMetrics.getAscent()) / 2;
        for (int i = 0; i < columns; i++) {
            String label = label(i, columns);
            graphics.drawString(label, x + (i + 1) * cellWidth + (cellWidth - fontMetrics.stringWidth(label)) / 2, y + deltaY);
        }
        for (int i = 0; i < rows; i++) {
            String label = label(i, rows);
            graphics.drawString(label, x + (cellWidth - fontMetrics.stringWidth(label)) / 2, y + (i + 1) * cellHeight + deltaY);
        }

        int outputColumn = table.getOutputIndex(output);
        x += cellWidth;
        y += cellHeight;
        graphics.setColor(ERROR_COLOR);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                int row = getTableRow(i, j, rows, columns);
                Entry entry = table.getOutputEntry(row, outputColumn);
                if (provisionalValue != null && row == provisionalY && outputColumn == provisionalX) {
                    entry = provisionalValue;
                }
                if (entry.isError()) {
                    graphics.fillRect(x + j * cellWidth, y + i * cellHeight, cellWidth, cellHeight);
                }
            }
        }

        List<Implicant> implicants = model.getOutputExpressions().getMinimalImplicants(output);
        if (implicants != null) {
            int index = 0;
            for (Implicant implicant : implicants) {
                graphics.setColor(IMP_COLORS[index % IMP_COLORS.length]);
                paintImplicant(graphics, implicant, x, y, rows, columns);
                index++;
            }
        }

        graphics.setColor(Color.GRAY);
        if (columns > 1 || inputCount == 0) {
            graphics.drawLine(x, y, left + tableWidth, y);
        }
        if (rows > 1 || inputCount == 0) {
            graphics.drawLine(x, y, x, top + tableHeight);
        }
        if (outputColumn < 0) {
            return;
        }

        graphics.setColor(Color.BLACK);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                int row = getTableRow(i, j, rows, columns);
                if (provisionalValue != null && row == provisionalY
                    && outputColumn == provisionalX) {
                    String text = provisionalValue.getDescription();
                    graphics.setColor(Color.GREEN);
                    graphics.drawString(text, x + j * cellWidth + (cellWidth - fontMetrics.stringWidth(text)) / 2,
                        y + i * cellHeight + deltaY);
                    graphics.setColor(Color.BLACK);
                } else {
                    Entry entry = table.getOutputEntry(row, outputColumn);
                    String text = entry.getDescription();
                    graphics.drawString(text, x + j * cellWidth + (cellWidth - fontMetrics.stringWidth(text)) / 2,
                        y + i * cellHeight + deltaY);
                }
            }
        }
    }

    private void paintImplicant(Graphics graphics, Implicant implicant, int x, int y, int rows, int columns) {
        int rowMax = -1;
        int rowMin = rows;
        int columnMax = -1;
        int columnMin = columns;
        boolean oneRowFound = false;
        int count = 0;
        for (Implicant sq : implicant.getTerms()) {
            int tableRow = sq.getRow();
            int row = getRow(tableRow, rows, columns);
            int column = getColumn(tableRow, rows, columns);
            if (row == 1) {
                oneRowFound = true;
            }
            if (row > rowMax) {
                rowMax = row;
            }
            if (row < rowMin) {
                rowMin = row;
            }
            if (column > columnMax) {
                columnMax = column;
            }
            if (column < columnMin) {
                columnMin = column;
            }
            ++count;
        }

        int numColumns = columnMax - columnMin + 1;
        int numRows = rowMax - rowMin + 1;
        int covered = numColumns * numRows;
        int diameter = 2 * IMP_RADIUS;
        if (covered == count) {
            graphics.fillRoundRect(x + columnMin * cellWidth + IMPLICANT_INSET, y + rowMin * cellHeight + IMPLICANT_INSET,
                numColumns * cellWidth - 2 * IMPLICANT_INSET, numRows * cellHeight - 2 * IMPLICANT_INSET, diameter, diameter);
        } else if (covered == 16) {
            if (count == 4) {
                int width = cellWidth - IMPLICANT_INSET;
                int height = cellHeight - IMPLICANT_INSET;
                int x1 = x + 3 * cellWidth + IMPLICANT_INSET;
                int y1 = y + 3 * cellHeight + IMPLICANT_INSET;
                graphics.fillRoundRect(x, y, width, height, diameter, diameter);
                graphics.fillRoundRect(x1, y, width, height, diameter, diameter);
                graphics.fillRoundRect(x, y1, width, height, diameter, diameter);
                graphics.fillRoundRect(x1, y1, width, height, diameter, diameter);
            } else if (oneRowFound) { // first and last columns
                int width = cellWidth - IMPLICANT_INSET;
                int height = 4 * cellHeight - 2 * IMPLICANT_INSET;
                int x1 = x + 3 * cellWidth + IMPLICANT_INSET;
                graphics.fillRoundRect(x, y + IMPLICANT_INSET, width, height, diameter, diameter);
                graphics.fillRoundRect(x1, y + IMPLICANT_INSET, width, height, diameter, diameter);
            } else { // first and last rows
                int width = 4 * cellWidth - 2 * IMPLICANT_INSET;
                int height = cellHeight - IMPLICANT_INSET;
                int y1 = y + 3 * cellHeight + IMPLICANT_INSET;
                graphics.fillRoundRect(x + IMPLICANT_INSET, y, width, height, diameter, diameter);
                graphics.fillRoundRect(x + IMPLICANT_INSET, y1, width, height, diameter, diameter);
            }
        } else if (numColumns == 4) {
            int top = y + rowMin * cellHeight + IMPLICANT_INSET;
            int width = cellWidth - IMPLICANT_INSET;
            int height = numRows * cellHeight - 2 * IMPLICANT_INSET;
            // handle half going off left edge
            graphics.fillRoundRect(x, top, width, height, diameter, diameter);
            // handle half going off right edge
            graphics.fillRoundRect(x + 3 * cellWidth + IMPLICANT_INSET, top, width, height, diameter, diameter);
			/* This is the proper way, with no rounded rectangles along
			 * the table's edge; but I found that the different regions were
			 * liable to overlap, particularly the arcs with the rectangles.
			 * (Plus, I was too lazy to figure this out for the 16 case.)
			int y0 = y + rowMin * cellHeight + IMP_INSET;
			int y1 = y + rowMax * cellHeight + cellHeight - IMP_INSET;
			int dy = y1 - y0;
			int x0 = x + cellWidth - IMP_INSET;
			int x1 = x + 3 * cellWidth + IMP_INSET;

			// half going off left edge
			g.fillRect(x,               y0, cellWidth - IMP_INSET - IMP_RADIUS, dy);
			g.fillRect(x0 - IMP_RADIUS, y0 + IMP_RADIUS, IMP_RADIUS, dy - d);
			g.fillArc(x0 - d, y0, d, d, 0, 90);
			g.fillArc(x0 - d, y1 - d, d, d, 0, -90);

			// half going off right edge
			g.fillRect(x1 + IMP_RADIUS, y0, cellWidth - IMP_INSET - IMP_RADIUS, dy);
			g.fillRect(x1, y0 + IMP_RADIUS, IMP_RADIUS, dy - d);
			g.fillArc(x1, y0, d, d, 180, 90);
			g.fillArc(x1, y1 - d, d, d, 180, -90);
			*/
        } else { // numRows == 4
            int left = x + columnMin * cellWidth + IMPLICANT_INSET;
            int width = numColumns * cellWidth - 2 * IMPLICANT_INSET;
            int height = cellHeight - IMPLICANT_INSET;
            // handle half going off top edge
            graphics.fillRoundRect(left, y, width, height, diameter, diameter);
            // handle half going off right edge
            graphics.fillRoundRect(left, y + 3 * cellHeight + IMPLICANT_INSET, width, height, diameter, diameter);
        }
    }

    private String header(int start, int stop) {
        if (start >= stop) {
            return "";
        }
        VariableList inputs = model.getInputs();
        StringBuilder builder = new StringBuilder(inputs.get(start));
        for (int i = start + 1; i < stop; i++) {
            builder.append(", ");
            builder.append(inputs.get(i));
        }
        return builder.toString();
    }

    private String label(int row, int rows) {
        switch (rows) {
            case 2:
                return "" + row;
            case 4:
                switch (row) {
                    case 0:
                        return "00";
                    case 1:
                        return "01";
                    case 2:
                        return "11";
                    case 3:
                        return "10";
                }
            default:
                return "";
        }
    }

    private int getTableRow(int row, int column, int rows, int columns) {
        return toRow(row, rows) * columns + toRow(column, columns);
    }

    private int toRow(int row, int rows) {
        if (rows == 4) {
            switch (row) {
                case 2:
                    return 3;
                case 3:
                    return 2;
                default:
                    return row;
            }
        } else {
            return row;
        }
    }

    private int getRow(int tableRow, int rows, int columns) {
        int ret = tableRow / columns;
        switch (ret) {
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return ret;
        }
    }

    private int getColumn(int tableRow, int rows, int columns) {
        int ret = tableRow % columns;
        switch (ret) {
            case 2:
                return 3;
            case 3:
                return 2;
            default:
                return ret;
        }
    }

    private int computeMargin(int compDimension, int tableDimension) {
        int ret = (compDimension - tableDimension) / 2;
        return ret >= 0 ? ret : Math.max(-headHeight, compDimension - tableDimension);
    }

    private class MyListener implements OutputExpressionsListener, TruthTableListener {

        public void expressionChanged(OutputExpressionsEvent event) {
            if (event.getType() == OutputExpressionsEvent.OUTPUT_MINIMAL && event.getVariable().equals(output)) {
                repaint();
            }
        }

        public void cellsChanged(TruthTableEvent event) {
            repaint();
        }

        public void structureChanged(TruthTableEvent event) {
            computePreferredSize();
        }

    }

}

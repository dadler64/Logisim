/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.ExpressionVisitor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.ArrayList;
import javax.swing.JPanel;

class ExpressionView extends JPanel {

    private static final int BADNESS_IDENT_BREAK = 10000;
    private static final int BADNESS_BEFORE_SPACE = 500;
    private static final int BADNESS_BEFORE_AND = 50;
    private static final int BADNESS_BEFORE_XOR = 30;
    private static final int BADNESS_BEFORE_OR = 0;
    private static final int BADNESS_NOT_BREAK = 100;
    private static final int BADNESS_PER_NOT_BREAK = 30;
    private static final int BADNESS_PER_PIXEL = 1;

    private static final int NOT_SEP = 3;
    private static final int EXTRA_LEADING = 4;
    private static final int MINIMUM_HEIGHT = 25;
    private final MyListener myListener = new MyListener();
    private RenderData renderData;

    public ExpressionView() {
        addComponentListener(myListener);
        setExpression(null);
    }

    public void setExpression(Expression expression) {
        ExpressionData data = new ExpressionData(expression);
        Graphics g = getGraphics();
        FontMetrics fm = g == null ? null : g.getFontMetrics();
        renderData = new RenderData(data, getWidth(), fm);
        setPreferredSize(renderData.getPreferredSize());
        revalidate();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (renderData != null) {
            int x = Math.max(0, (getWidth() - renderData.preferredWidth) / 2);
            int y = Math.max(0, (getHeight() - renderData.height) / 2);
            renderData.paint(g, x, y);
        }
    }

    void localeChanged() {
        repaint();
    }

    private static class NotData {

        int startIndex;
        int stopIndex;
        int depth;
    }

    private static class ExpressionData {

        final ArrayList<NotData> nots = new ArrayList<>();
        String text;
        int[] badness;

        ExpressionData(Expression expr) {
            if (expr == null) {
                text = "";
                badness = new int[0];
            } else {
                computeText(expr);
                computeBadnesses();
            }
        }

        private void computeText(Expression expr) {
            final StringBuilder text = new StringBuilder();
            expr.visit(new ExpressionVisitor<Object>() {
                public Object visitAnd(Expression a, Expression b) {
                    return binary(a, b, Expression.AND_LEVEL, " ");
                }

                public Object visitOr(Expression a, Expression b) {
                    return binary(a, b, Expression.OR_LEVEL, " + ");
                }

                public Object visitXor(Expression a, Expression b) {
                    return binary(a, b, Expression.XOR_LEVEL, " ^ ");
                }

                private Object binary(Expression a, Expression b, int level, String op) {
                    if (a.getPrecedence() < level) {
                        text.append("(");
                        a.visit(this);
                        text.append(")");
                    } else {
                        a.visit(this);
                    }
                    text.append(op);
                    if (b.getPrecedence() < level) {
                        text.append("(");
                        b.visit(this);
                        text.append(")");
                    } else {
                        b.visit(this);
                    }
                    return null;
                }

                public Object visitNot(Expression expression) {
                    NotData notData = new NotData();
                    notData.startIndex = text.length();
                    nots.add(notData);
                    expression.visit(this);
                    notData.stopIndex = text.length();
                    return null;
                }

                public Object visitVariable(String name) {
                    text.append(name);
                    return null;
                }

                public Object visitConstant(int value) {
                    text.append(Integer.toString(value, 16));
                    return null;
                }
            });
            this.text = text.toString();
        }

        private void computeBadnesses() {
            badness = new int[text.length() + 1];
            badness[text.length()] = 0;
            if (text.length() == 0) {
                return;
            }

            badness[0] = Integer.MAX_VALUE;
            NotData currentNot = nots.isEmpty() ? null : nots.get(0);
            int currentNotIndex = 0;
            char previous = text.charAt(0);
            for (int i = 1; i < text.length(); i++) {
                // invariant: currentNot.stopIndex >= i (and is first such),
                //    or currentNot == null if none such exists
                char current = text.charAt(i);
                if (current == ' ') {
                    badness[i] = BADNESS_BEFORE_SPACE;
                } else if (Character.isJavaIdentifierPart(current)) {
                    if (Character.isJavaIdentifierPart(previous)) {
                        badness[i] = BADNESS_IDENT_BREAK;
                    } else {
                        badness[i] = BADNESS_BEFORE_AND;
                    }
                } else if (current == '+') {
                    badness[i] = BADNESS_BEFORE_OR;
                } else if (current == '^') {
                    badness[i] = BADNESS_BEFORE_XOR;
                } else if (current == ')') {
                    badness[i] = BADNESS_BEFORE_SPACE;
                } else { // current == '('
                    badness[i] = BADNESS_BEFORE_AND;
                }

                while (currentNot != null && currentNot.stopIndex <= i) {
                    ++currentNotIndex;
                    currentNot = (currentNotIndex >= nots.size() ? null : nots.get(currentNotIndex));
                }

                if (currentNot != null && badness[i] < BADNESS_IDENT_BREAK) {
                    int depth = 0;
                    NotData notData = currentNot;
                    int notDataIndex = currentNotIndex;
                    while (notData != null && notData.startIndex < i) {
                        if (notData.stopIndex > i) {
                            ++depth;
                        }
                        ++notDataIndex;
                        notData = notDataIndex < nots.size() ? nots.get(notDataIndex) : null;
                    }
                    if (depth > 0) {
                        badness[i] += BADNESS_NOT_BREAK + (depth - 1) * BADNESS_PER_NOT_BREAK;
                    }
                }

                previous = current;
            }
        }
    }

    private static class RenderData {

        ExpressionData expressionData;
        int preferredWidth;
        int width;
        int height;
        String[] lineText;
        ArrayList<ArrayList<NotData>> lineNots;
        int[] lineY;

        RenderData(ExpressionData expressionData, int width, FontMetrics fm) {
            this.expressionData = expressionData;
            this.width = width;
            height = MINIMUM_HEIGHT;

            if (fm == null) {
                lineText = new String[]{expressionData.text};
                lineNots = new ArrayList<>();
                lineNots.add(expressionData.nots);
                computeNotDepths();
                lineY = new int[]{MINIMUM_HEIGHT};
            } else {
                if (expressionData.text.length() == 0) {
                    lineText = new String[]{Strings.get("expressionEmpty")};
                    lineNots = new ArrayList<>();
                    lineNots.add(new ArrayList<>());
                } else {
                    computeLineText(fm);
                    computeLineNots();
                    computeNotDepths();
                }
                computeLineY(fm);
                preferredWidth = lineText.length > 1 ? width : fm.stringWidth(lineText[0]);
            }
        }

        private void computeLineText(FontMetrics fm) {
            String text = expressionData.text;
            int[] badness = expressionData.badness;

            if (fm.stringWidth(text) <= width) {
                lineText = new String[]{text};
                return;
            }

            int startPosition = 0;
            ArrayList<String> lines = new ArrayList<>();
            while (startPosition < text.length()) {
                int stopPosition = startPosition + 1;
                String bestLine = text.substring(startPosition, stopPosition);
                if (stopPosition >= text.length()) {
                    lines.add(bestLine);
                    break;
                }
                int bestStopPosition = stopPosition;
                int lineWidth = fm.stringWidth(bestLine);
                int bestBadness = badness[stopPosition] + (width - lineWidth) * BADNESS_PER_PIXEL;
                while (stopPosition < text.length()) {
                    ++stopPosition;
                    String line = text.substring(startPosition, stopPosition);
                    lineWidth = fm.stringWidth(line);
                    if (lineWidth > width) {
                        break;
                    }

                    int lineBadness = badness[stopPosition] + (width - lineWidth) * BADNESS_PER_PIXEL;
                    if (lineBadness < bestBadness) {
                        bestBadness = lineBadness;
                        bestStopPosition = stopPosition;
                        bestLine = line;
                    }
                }
                lines.add(bestLine);
                startPosition = bestStopPosition;
            }
            lineText = lines.toArray(new String[0]);
        }

        private void computeLineNots() {
            ArrayList<NotData> allNots = expressionData.nots;
            lineNots = new ArrayList<>();
            for (int i = 0; i < lineText.length; i++) {
                lineNots.add(new ArrayList<>());
            }
            for (NotData notData : allNots) {
                int position = 0;
                for (int j = 0; j < lineNots.size() && position < notData.stopIndex; j++) {
                    String line = lineText[j];
                    int nextPosition = position + line.length();
                    if (nextPosition > notData.startIndex) {
                        NotData toAdd = new NotData();
                        toAdd.startIndex = Math.max(position, notData.startIndex) - position;
                        toAdd.stopIndex = Math.min(nextPosition, notData.stopIndex) - position;
                        lineNots.get(j).add(toAdd);
                    }
                    position = nextPosition;
                }
            }
        }

        private void computeNotDepths() {
            for (ArrayList<NotData> nots : lineNots) {
                int n = nots.size();
                int[] stack = new int[n];
                for (int i = 0; i < nots.size(); i++) {
                    NotData notData = nots.get(i);
                    int depth = 0;
                    int top = 0;
                    stack[0] = notData.stopIndex;
                    for (int j = i + 1; j < nots.size(); j++) {
                        NotData notData2 = nots.get(j);
                        if (notData2.startIndex >= notData.stopIndex) {
                            break;
                        }
                        while (notData2.startIndex >= stack[top]) {
                            top--;
                        }
                        ++top;
                        stack[top] = notData2.stopIndex;
                        if (top > depth) {
                            depth = top;
                        }
                    }
                    notData.depth = depth;
                }
            }
        }

        private void computeLineY(FontMetrics fm) {
            lineY = new int[lineNots.size()];
            int currentY = 0;
            for (int i = 0; i < lineY.length; i++) {
                int maxDepth = -1;
                ArrayList<NotData> nots = lineNots.get(i);
                for (NotData nd : nots) {
                    if (nd.depth > maxDepth) {
                        maxDepth = nd.depth;
                    }
                }
                lineY[i] = currentY + maxDepth * NOT_SEP;
                currentY = lineY[i] + fm.getHeight() + EXTRA_LEADING;
            }
            height = Math.max(MINIMUM_HEIGHT,
                currentY - fm.getLeading() - EXTRA_LEADING);
        }

        public Dimension getPreferredSize() {
            return new Dimension(10, height);
        }

        public void paint(Graphics g, int x, int y) {
            FontMetrics fm = g.getFontMetrics();
            int i = -1;
            for (String line : lineText) {
                i++;
                g.drawString(line, x, y + lineY[i] + fm.getAscent());

                ArrayList<NotData> nots = lineNots.get(i);
                int j = -1;
                for (NotData notData : nots) {
                    j++;
                    int notY = y + lineY[i] - notData.depth * NOT_SEP;
                    int startX = x + fm.stringWidth(line.substring(0, notData.startIndex));
                    int stopX = x + fm.stringWidth(line.substring(0, notData.stopIndex));
                    g.drawLine(startX, notY, stopX, notY);
                }
            }
        }
    }

    private class MyListener implements ComponentListener {

        public void componentResized(ComponentEvent arg0) {
            int width = getWidth();
            if (renderData != null && Math.abs(renderData.width - width) > 2) {
                Graphics g = getGraphics();
                FontMetrics fm = g == null ? null : g.getFontMetrics();
                renderData = new RenderData(renderData.expressionData, width, fm);
                setPreferredSize(renderData.getPreferredSize());
                revalidate();
                repaint();
            }
        }

        public void componentMoved(ComponentEvent arg0) {
        }

        public void componentShown(ComponentEvent arg0) {
        }

        public void componentHidden(ComponentEvent arg0) {
        }
    }
}

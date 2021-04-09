/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class OutputExpressions {

    private final AnalyzerModel model;
    private final HashMap<String, OutputData> outputData = new HashMap<>();
    private final ArrayList<OutputExpressionsListener> listeners = new ArrayList<>();
    private boolean updatingTable = false;

    public OutputExpressions(AnalyzerModel model) {
        this.model = model;
        MyListener myListener = new MyListener();
        model.getInputs().addVariableListListener(myListener);
        model.getOutputs().addVariableListListener(myListener);
        model.getTruthTable().addTruthTableListener(myListener);
    }

    private static Entry[] computeColumn(TruthTable table, Expression expression) {
        int rows = table.getRowCount();
        int columns = table.getInputColumnCount();
        Entry[] entries = new Entry[rows];
        if (expression == null) {
            Arrays.fill(entries, Entry.DONT_CARE);
        } else {
            Assignments assignments = new Assignments();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < columns; j++) {
                    assignments.put(table.getInputHeader(j),
                        TruthTable.isInputSet(i, j, columns));
                }
                entries[i] = expression.evaluate(assignments) ? Entry.ONE : Entry.ZERO;
            }
        }
        return entries;
    }

    private static boolean columnsMatch(Entry[] a, Entry[] b) {
        if (a.length != b.length) {
            return false;
        }
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                boolean bothDefined = (a[i] == Entry.ZERO || a[i] == Entry.ONE)
                    && (b[i] == Entry.ZERO || b[i] == Entry.ONE);
                if (bothDefined) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isAllUndefined(Entry[] entries) {
        for (Entry entry : entries) {
            if (entry == Entry.ZERO || entry == Entry.ONE) {
                return false;
            }
        }
        return true;
    }

    private static boolean implicantsSame(List<Implicant> implicantAList, List<Implicant> implicantBList) {
        if (implicantAList == null) {
            return implicantBList == null || implicantBList.size() == 0;
        } else if (implicantBList == null) {
            return implicantAList == null || implicantAList.size() == 0;
        } else if (implicantAList.size() != implicantBList.size()) {
            return false;
        } else {
            Iterator<Implicant> iterator = implicantAList.iterator();
            for (Implicant implicantB : implicantBList) {
                if (!iterator.hasNext()) {
                    return false; // should never happen
                }
                Implicant implicantA = iterator.next();
                if (!implicantA.equals(implicantB)) {
                    return false;
                }
            }
            return true;
        }
    }

    //
    // listener methods
    //
    public void addOutputExpressionsListener(OutputExpressionsListener listener) {
        listeners.add(listener);
    }

    public void removeOutputExpressionsListener(OutputExpressionsListener listener) {
        listeners.remove(listener);
    }

    @SuppressWarnings("SameParameterValue")
    private void fireModelChanged(int type) {
        fireModelChanged(type, null, null);
    }

    private void fireModelChanged(int type, String variable) {
        fireModelChanged(type, variable, null);
    }

    private void fireModelChanged(int type, String variable, Object data) {
        OutputExpressionsEvent event = new OutputExpressionsEvent(model, type, variable, data);
        for (OutputExpressionsListener l : listeners) {
            l.expressionChanged(event);
        }
    }

    //
    // access methods
    //
    public Expression getExpression(String output) {
        if (output == null) {
            return null;
        }
        return getOutputData(output, true).getExpression();
    }

    public String getExpressionString(String output) {
        if (output == null) {
            return "";
        }
        return getOutputData(output, true).getExpressionString();
    }

    public boolean isExpressionMinimal(String output) {
        OutputData data = getOutputData(output, false);
        return data == null || data.isExpressionMinimal();
    }

    public Expression getMinimalExpression(String output) {
        if (output == null) {
            return Expressions.constant(0);
        }
        return getOutputData(output, true).getMinimalExpression();
    }

    public List<Implicant> getMinimalImplicants(String output) {
        if (output == null) {
            return Implicant.MINIMAL_LIST;
        }
        return getOutputData(output, true).getMinimalImplicants();
    }

    public int getMinimizedFormat(String output) {
        if (output == null) {
            return AnalyzerModel.FORMAT_SUM_OF_PRODUCTS;
        }
        return getOutputData(output, true).getMinimizedFormat();
    }

    //
    // modifier methods
    //
    public void setMinimizedFormat(String output, int format) {
        int oldFormat = getMinimizedFormat(output);
        if (format != oldFormat) {
            getOutputData(output, true).setMinimizedFormat(format);
            invalidate(output, true);
        }
    }

    public void setExpression(String output, Expression expression) {
        setExpression(output, expression, null);
    }

    public void setExpression(String output, Expression expression, String expressionString) {
        if (output == null) {
            return;
        }
        getOutputData(output, true).setExpression(expression, expressionString);
    }

    private void invalidate(String output, boolean formatChanged) {
        OutputData data = getOutputData(output, false);
        if (data != null) {
            data.invalidate(false, false);
        }
    }

    private OutputData getOutputData(String output, boolean create) {
        if (output == null) {
            throw new IllegalArgumentException("null output name");
        }
        OutputData outputData = this.outputData.get(output);
        if (outputData == null && create) {
            if (model.getOutputs().indexOf(output) < 0) {
                throw new IllegalArgumentException("unrecognized output " + output);
            }
            outputData = new OutputData(output);
            this.outputData.put(output, outputData);
        }
        return outputData;
    }

    private class OutputData {

        String output;
        private int format;
        private Expression expression = null;
        private String expressionString = null;
        private List<Implicant> minimalImplicants = null;
        private Expression minimalExpression = null;
        private boolean invalidating = false;

        private OutputData(String output) {
            this.output = output;
            invalidate(true, false);
        }

        private boolean isExpressionMinimal() {
            return expression == minimalExpression;
        }

        private Expression getExpression() {
            return expression;
        }

        private String getExpressionString() {
            if (expressionString == null) {
                if (expression == null) {
                    invalidate(false, false);
                }
                expressionString = expression == null ? "" : expression.toString();
            }
            return expressionString;
        }

        private Expression getMinimalExpression() {
            if (minimalExpression == null) {
                invalidate(false, false);
            }
            return minimalExpression;
        }

        private List<Implicant> getMinimalImplicants() {
            return minimalImplicants;
        }

        private int getMinimizedFormat() {
            return format;
        }

        private void setMinimizedFormat(int value) {
            if (format != value) {
                format = value;
                this.invalidate(false, true);
            }
        }

        private void setExpression(Expression newExpression, String newExpressionString) {
            expression = newExpression;
            expressionString = newExpressionString;

            if (expression != minimalExpression) { // for efficiency to avoid recomputation
                Entry[] values = computeColumn(model.getTruthTable(), expression);
                int outputColumn = model.getOutputs().indexOf(output);
                updatingTable = true;
                try {
                    model.getTruthTable().setOutputColumn(outputColumn, values);
                } finally {
                    updatingTable = false;
                }
            }

            fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output, getExpression());
        }

        private void removeInput(String input) {
            Expression oldMinimalExpression = minimalExpression;
            minimalImplicants = null;
            minimalExpression = null;

            if (expressionString != null) {
                expressionString = null; // invalidate it so it recomputes
            }
            if (expression != null) {
                Expression oldExpression = expression;
                Expression newExpression;
                if (oldExpression == oldMinimalExpression) {
                    newExpression = getMinimalExpression();
                    expression = newExpression;
                } else {
                    newExpression = expression.removeVariable(input);
                }
                if (newExpression == null || !newExpression.equals(oldExpression)) {
                    expression = newExpression;
                    fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output, expression);
                }
            }
            fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output, minimalExpression);
        }

        private void replaceInput(String input, String newName) {
            minimalExpression = null;

            if (expressionString != null) {
                expressionString = Parser.replaceVariable(expressionString, input, newName);
            }
            if (expression != null) {
                Expression newExpression = expression.replaceVariable(input, newName);
                if (!newExpression.equals(expression)) {
                    expression = newExpression;
                    fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output);
                }
            } else {
                fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output);
            }
            fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output);
        }

        private void invalidate(boolean initializing, boolean formatChanged) {
            if (invalidating) {
                return;
            }
            invalidating = true;
            try {
                List<Implicant> oldImplicants = minimalImplicants;
                Expression oldMinimalExpression = minimalExpression;
                minimalImplicants = Implicant.computeMinimal(format, model, output);
                minimalExpression = Implicant.toExpression(format, model, minimalImplicants);
                boolean minimalChanged = !implicantsSame(oldImplicants, minimalImplicants);

                if (!updatingTable) {
                    // see whether the expression is still consistent with the truth table
                    TruthTable table = model.getTruthTable();
                    Entry[] outputColumn = computeColumn(model.getTruthTable(), expression);
                    int outputIndex = model.getOutputs().indexOf(output);

                    Entry[] currentColumn = table.getOutputColumn(outputIndex);
                    if (!columnsMatch(currentColumn, outputColumn) || isAllUndefined(outputColumn) || formatChanged) {
                        // if not, then we need to change the expression to maintain consistency
                        boolean expressionChanged = expression != oldMinimalExpression || minimalChanged;
                        expression = minimalExpression;
                        if (expressionChanged) {
                            expressionString = null;
                            if (!initializing) {
                                fireModelChanged(OutputExpressionsEvent.OUTPUT_EXPRESSION, output);
                            }
                        }
                    }
                }

                if (!initializing && minimalChanged) {
                    fireModelChanged(OutputExpressionsEvent.OUTPUT_MINIMAL, output);
                }
            } finally {
                invalidating = false;
            }
        }
    }

    private class MyListener implements VariableListListener, TruthTableListener {

        public void listChanged(VariableListEvent event) {
            if (event.getSource() == model.getInputs()) {
                inputsChanged(event);
            } else {
                outputsChanged(event);
            }
        }

        private void inputsChanged(VariableListEvent event) {
            int type = event.getType();
            if (type == VariableListEvent.ALL_REPLACED && !outputData.isEmpty()) {
                outputData.clear();
                fireModelChanged(OutputExpressionsEvent.ALL_VARIABLES_REPLACED);
            } else if (type == VariableListEvent.REMOVE) {
                String input = event.getVariable();
                for (String output : outputData.keySet()) {
                    OutputData data = getOutputData(output, false);
                    if (data != null) {
                        data.removeInput(input);
                    }
                }
            } else if (type == VariableListEvent.REPLACE) {
                String input = event.getVariable();
                int inputIndex = (Integer) event.getData();
                String newName = event.getSource().get(inputIndex);
                for (String output : outputData.keySet()) {
                    OutputData data = getOutputData(output, false);
                    if (data != null) {
                        data.replaceInput(input, newName);
                    }
                }
            } else if (type == VariableListEvent.MOVE || type == VariableListEvent.ADD) {
                for (String output : outputData.keySet()) {
                    OutputData data = getOutputData(output, false);
                    if (data != null) {
                        data.invalidate(false, false);
                    }
                }
            }
        }

        private void outputsChanged(VariableListEvent event) {
            int type = event.getType();
            if (type == VariableListEvent.ALL_REPLACED && !outputData.isEmpty()) {
                outputData.clear();
                fireModelChanged(OutputExpressionsEvent.ALL_VARIABLES_REPLACED);
            } else if (type == VariableListEvent.REMOVE) {
                outputData.remove(event.getVariable());
            } else if (type == VariableListEvent.REPLACE) {
                String oldName = event.getVariable();
                if (outputData.containsKey(oldName)) {
                    OutputData toMove = outputData.remove(oldName);
                    int inputIndex = (Integer) event.getData();
                    String newName = event.getSource().get(inputIndex);
                    toMove.output = newName;
                    outputData.put(newName, toMove);
                }
            }
        }

        public void cellsChanged(TruthTableEvent event) {
            String output = model.getOutputs().get(event.getColumn());
            invalidate(output, false);
        }

        public void structureChanged(TruthTableEvent event) {
        }
    }
}

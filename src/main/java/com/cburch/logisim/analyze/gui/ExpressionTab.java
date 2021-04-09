/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.gui;

import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.OutputExpressionsEvent;
import com.cburch.logisim.analyze.model.OutputExpressionsListener;
import com.cburch.logisim.analyze.model.Parser;
import com.cburch.logisim.analyze.model.ParserException;
import com.cburch.logisim.util.StringGetter;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

class ExpressionTab extends AnalyzerTab implements TabInterface {

    private final OutputSelector selector;
    private final ExpressionView prettyView = new ExpressionView();
    private final JTextArea textArea = new JTextArea(4, 25);
    private final JButton clearButton = new JButton();
    private final JButton revertButton = new JButton();
    private final JButton enterButton = new JButton();
    private final JLabel errorLabel = new JLabel();
    private final MyListener myListener = new MyListener();
    private final AnalyzerModel model;
    private int currentExpressionStringLength = 0;
    private StringGetter errorMessage;

    public ExpressionTab(AnalyzerModel model) {
        this.model = model;
        selector = new OutputSelector(model);

        model.getOutputExpressions().addOutputExpressionsListener(myListener);
        selector.addItemListener(myListener);
        clearButton.addActionListener(myListener);
        revertButton.addActionListener(myListener);
        enterButton.addActionListener(myListener);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), myListener);
        textArea.getDocument().addDocumentListener(myListener);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JPanel buttons = new JPanel();
        buttons.add(clearButton);
        buttons.add(revertButton);
        buttons.add(enterButton);

        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gc = new GridBagConstraints();
        setLayout(gb);
        gc.weightx = 1.0;
        gc.gridx = 0;
        gc.gridy = GridBagConstraints.RELATIVE;
        gc.fill = GridBagConstraints.BOTH;

        JPanel selectorPanel = selector.createPanel();
        gb.setConstraints(selectorPanel, gc);
        add(selectorPanel);
        gb.setConstraints(prettyView, gc);
        add(prettyView);
        Insets oldInsets = gc.insets;
        gc.insets = new Insets(10, 10, 0, 10);
        JScrollPane fieldPane = new JScrollPane(textArea,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        );
        gb.setConstraints(fieldPane, gc);
        add(fieldPane);
        gc.insets = oldInsets;
        gc.fill = GridBagConstraints.NONE;
        gc.anchor = GridBagConstraints.LINE_END;
        gb.setConstraints(buttons, gc);
        add(buttons);
        gc.fill = GridBagConstraints.BOTH;
        gb.setConstraints(errorLabel, gc);
        add(errorLabel);

        myListener.insertUpdate(null);
        setErrorLabel(null);
    }

    @Override
    void localeChanged() {
        selector.localeChanged();
        prettyView.localeChanged();
        clearButton.setText(Strings.get("exprClearButton"));
        revertButton.setText(Strings.get("exprRevertButton"));
        enterButton.setText(Strings.get("exprEnterButton"));
        if (errorMessage != null) {
            errorLabel.setText(errorMessage.get());
        }
    }

    @Override
    void updateTab() {
        String output = getCurrentVariable();
        prettyView.setExpression(model.getOutputExpressions().getExpression(output));
        myListener.currentStringChanged();
    }

    void registerDefaultButtons(DefaultRegistry registry) {
        registry.registerDefaultButton(textArea, enterButton);
    }

    String getCurrentVariable() {
        return selector.getSelectedOutput();
    }

    private void setErrorLabel(StringGetter message) {
        if (message == null) {
            errorMessage = null;
            errorLabel.setText(" ");
        } else {
            errorMessage = message;
            errorLabel.setText(message.get());
        }
    }

    public void copy() {
        textArea.requestFocus();
        textArea.copy();
    }

    public void paste() {
        textArea.requestFocus();
        textArea.paste();
    }

    public void delete() {
        textArea.requestFocus();
        textArea.replaceSelection("");
    }

    public void selectAll() {
        textArea.requestFocus();
        textArea.selectAll();
    }

    private class MyListener extends AbstractAction implements DocumentListener, OutputExpressionsListener, ItemListener {

        boolean isEdited = false;

        public void actionPerformed(ActionEvent e) {
            Object source = e.getSource();
            if (source == clearButton) {
                setErrorLabel(null);
                textArea.setText("");
                textArea.grabFocus();
            } else if (source == revertButton) {
                setErrorLabel(null);
                textArea.setText(getCurrentString());
                textArea.grabFocus();
            } else if ((source == textArea || source == enterButton) && enterButton.isEnabled()) {
                try {
                    String expressionString = textArea.getText();
                    Expression expression = Parser.parse(textArea.getText(), model);
                    setErrorLabel(null);
                    model.getOutputExpressions().setExpression(getCurrentVariable(), expression, expressionString);
                    insertUpdate(null);
                } catch (ParserException ex) {
                    setErrorLabel(ex.getMessageGetter());
                    textArea.setCaretPosition(ex.getOffset());
                    textArea.moveCaretPosition(ex.getEndOffset());
                }
                textArea.grabFocus();
            }
        }

        public void insertUpdate(DocumentEvent e) {
            String curText = textArea.getText();
            isEdited = curText.length() != currentExpressionStringLength || !curText.equals(getCurrentString());

            boolean enable = (isEdited && getCurrentVariable() != null);
            clearButton.setEnabled(curText.length() > 0);
            revertButton.setEnabled(enable);
            enterButton.setEnabled(enable);
        }

        public void removeUpdate(DocumentEvent e) {
            insertUpdate(e);
        }

        public void changedUpdate(DocumentEvent e) {
            insertUpdate(e);
        }

        public void expressionChanged(OutputExpressionsEvent event) {
            if (event.getType() == OutputExpressionsEvent.OUTPUT_EXPRESSION) {
                String output = event.getVariable();
                if (output.equals(getCurrentVariable())) {
                    prettyView.setExpression(model.getOutputExpressions().getExpression(output));
                    currentStringChanged();
                }
            }
        }

        public void itemStateChanged(ItemEvent e) {
            updateTab();
        }

        private String getCurrentString() {
            String output = getCurrentVariable();
            return output == null ? "" : model.getOutputExpressions().getExpressionString(output);
        }

        private void currentStringChanged() {
            String output = getCurrentVariable();
            String expressionString = model.getOutputExpressions().getExpressionString(output);
            currentExpressionStringLength = expressionString.length();
            if (!isEdited) {
                setErrorLabel(null);
                textArea.setText(getCurrentString());
            } else {
                insertUpdate(null);
            }
        }
    }
}

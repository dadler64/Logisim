/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.instance;

import com.adlerd.logger.Logger;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.tools.AbstractCaret;
import com.cburch.logisim.tools.Caret;
import com.cburch.logisim.tools.Pokable;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

class InstancePokerAdapter extends AbstractCaret implements Pokable {

    private InstanceComponent comp;
    private Canvas canvas;
    private InstancePoker poker;
    private InstanceStateImpl state;
    private ComponentDrawContext context;

    public InstancePokerAdapter(InstanceComponent comp, Class<? extends InstancePoker> pokerClass) {
        try {
            this.comp = comp;
            poker = pokerClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            handleError(t, pokerClass);
            poker = null;
        }
    }

    private void handleError(Throwable t, Class<? extends InstancePoker> pokerClass) {
        String className = pokerClass.getName();
        Logger.errorln("error while instantiating poker " + className + ": " + t.getClass().getName()); // OK
        String msg = t.getMessage();
        if (msg != null) {
            Logger.errorln("  (" + msg + ")"); // OK
        }
    }

    public Caret getPokeCaret(ComponentUserEvent event) {
        if (poker == null) {
            return null;
        } else {
            canvas = event.getCanvas();
            CircuitState circuitState = event.getCircuitState();
            InstanceStateImpl state = new InstanceStateImpl(circuitState, comp);
            MouseEvent mouseEvent = new MouseEvent(event.getCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, event.getX(), event.getY(), 1, false);
            boolean isAccepted = poker.init(state, mouseEvent);
            if (isAccepted) {
                this.state = state;
                this.context = new ComponentDrawContext(event.getCanvas(), event.getCanvas().getCircuit(), circuitState,
                    null, null);
                mousePressed(mouseEvent);
                return this;
            } else {
                poker = null;
                return null;
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (poker != null) {
            poker.mousePressed(state, e);
            checkCurrent();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (poker != null) {
            poker.mouseDragged(state, e);
            checkCurrent();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (poker != null) {
            poker.mouseReleased(state, e);
            checkCurrent();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (poker != null) {
            poker.keyPressed(state, e);
            checkCurrent();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (poker != null) {
            poker.keyReleased(state, e);
            checkCurrent();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (poker != null) {
            poker.keyTyped(state, e);
            checkCurrent();
        }
    }

    @Override
    public void stopEditing() {
        if (poker != null) {
            poker.stopEditing(state);
            checkCurrent();
        }
    }

    @Override
    public Bounds getBounds(Graphics g) {
        if (poker != null) {
            context.setGraphics(g);
            InstancePainter painter = new InstancePainter(context, comp);
            return poker.getBounds(painter);
        } else {
            return Bounds.EMPTY_BOUNDS;
        }
    }

    @Override
    public void draw(Graphics g) {
        if (poker != null) {
            context.setGraphics(g);
            InstancePainter painter = new InstancePainter(context, comp);
            poker.paint(painter);
        }
    }

    private void checkCurrent() {
        if (state != null && canvas != null) {
            CircuitState state0 = state.getCircuitState();
            CircuitState state1 = canvas.getCircuitState();
            if (state0 != state1) {
                state = new InstanceStateImpl(state1, comp);
            }
        }
    }
}

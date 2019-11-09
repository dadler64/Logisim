/* Copyright (c) 2010, Carl Burch. License information is located in the
 * com.cburch.logisim.Main source code and at www.cburch.com/logisim/. */

package com.cburch.logisim.analyze.model;


public interface ExpressionVisitor<T> {

    T visitAnd(Expression a, Expression b);

    T visitOr(Expression a, Expression b);

    T visitXor(Expression a, Expression b);

    T visitNot(Expression a);

    T visitVariable(String name);

    T visitConstant(int value);
}

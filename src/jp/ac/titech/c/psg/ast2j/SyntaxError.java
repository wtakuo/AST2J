/*
 * AST2J : A simple visitor generator for Java
 * Copyright (c) 2000-2017 Takuo Watanabe <takuo@acm.org>
 */

package jp.ac.titech.c.psg.ast2j;

class SyntaxError extends Exception {
    public SyntaxError (String message) {
        super(message);
    }
}

/*
 * AST2J : A simple visitor generator for Java
 * Copyright (c) 2000-2017 Takuo Watanabe <takuo@acm.org>
 */

package jp.ac.titech.c.psg.ast2j;

interface ParserHandler {
    public void handleAST (AST ast);
    public void handleCommand (String cmd, String arg);
}

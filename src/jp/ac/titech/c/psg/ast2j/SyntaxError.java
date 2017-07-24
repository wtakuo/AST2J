/*
 * $Id: SyntaxError.java,v 1.2 2005/05/19 08:23:10 takuo Exp $
 * SyntaxError.java (AST2J)
 * Copyright (C) 2000,2001,2002 Takuo Watanabe (takuo@acm.org)
 * All Rights Reserved
 *
 * This program was developed by Takuo Watanabe as a part of AST2J, 
 * a program that generates Java classes from AST (Abstrace Syntax
 * Tree) node definitions. The classes generated conform to the GoF
 * Visitor pattern.
 * 
 * Permission to use, copy, modify and distribute this software and
 * its documentation is hereby granted, provided that both the
 * copyright notice and this permission notice appear in all copies of
 * the software, derivative works or modified versions, and any
 * portions thereof, and that both notices appear in supporting
 * documentation.
 *
 * FREE USE OF THIS SOFTWARE IS ALLOWED IN ITS "AS IS" CONDITION. BUT
 * WITHOUT WARRANTY. THE AUTHORS DISCLAIM ANY LIABILITY OF ANY KIND
 * FOR ANY DAMAGES WHATSOEVER RESULTING FROM THE USE OF THIS SOFTWARE.
 */ 

package jp.ac.titech.c.psg.ast2j;

class SyntaxError extends Exception {
    public SyntaxError (String message) {
        super(message);
    }
}

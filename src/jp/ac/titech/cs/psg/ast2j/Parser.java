/*
 * $Id: Parser.java,v 1.4 2005/05/19 08:23:10 takuo Exp $
 * Parser.java (AST2J)
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

package jp.ac.titech.cs.psg.ast2j;

import jp.ac.titech.cs.psg.util.Tokenizer;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

class Parser {

    // Constants
    private static final char[] OPCHARS = { ':', '=', '[', ']' };
    private static final String[] OPERATORS = { "::=", "[]", ":" };

    private Tokenizer tokenizer = null;

    public Parser (InputStream in) {
        tokenizer = new Tokenizer(in);
        tokenizer.resetSyntax();
        tokenizer.setCharType('\u0000', ' ', Tokenizer.CT_WHITESPACE);
        tokenizer.setCharType('a', 'z', Tokenizer.CT_IDLETTER);
        tokenizer.setCharType('A', 'Z', Tokenizer.CT_IDLETTER);
        tokenizer.setCharType("_", Tokenizer.CT_IDLETTER);
        tokenizer.setCharType(":=[]", Tokenizer.CT_OPLETTER);
        tokenizer.setCharType("'\"", Tokenizer.CT_QUOTE);
        tokenizer.setCharType("\\", Tokenizer.CT_ESCAPE);
        tokenizer.setCharType('0', '9', Tokenizer.CT_DIGIT);
        tokenizer.setCharType("/", Tokenizer.CT_COMMENT1);
        tokenizer.setCharType("*", Tokenizer.CT_COMMENT2);
        tokenizer.useCComment(true);
        tokenizer.useCppComment(true);
    }

    private final boolean lookingAtToken (char token) {
        return tokenizer.ttype == token;
    }

    private final boolean lookingAtWord (String word) {
        return tokenizer.ttype == Tokenizer.TT_WORD
            && tokenizer.sval.equals(word);
    }

    private final boolean lookingAtNumber () {
        return tokenizer.ttype == Tokenizer.TT_NUMBER;
    }

    private final boolean lookingAtEOF () {
        return tokenizer.ttype == Tokenizer.TT_EOF;
    }

    private boolean lookingAtName () {
        if (tokenizer.ttype == Tokenizer.TT_WORD) {
            for (int i=0; i < OPCHARS.length; i++) {
                if (tokenizer.sval.indexOf(OPCHARS[i]) >= 0) return false;
            }
            return true;
        } else {
            return false;
        }
    }

    private final void nextToken () throws IOException {
        tokenizer.nextToken();
    }

    private final int getToken () {
        return tokenizer.ttype;
    }

    private final String getSval () {
        return tokenizer.sval;
    }

    private final int getNval () {
        return tokenizer.nval;
    }

    private final int lineNo () {
        return tokenizer.lineno();
    }

    // Program ::= { Command | Definition }

    public void parse (ParserHandler handler) throws SyntaxError, IOException {
        nextToken();
        while (lookingAtToken('#') || lookingAtName()) {
            if (lookingAtToken('#')) {
                parseCommand(handler);
            } else {
                handler.handleAST(parseDefinition());
            }
        }
        if (!lookingAtEOF()) {
            throw new SyntaxError("Unknown syntax at " + lineNo());
        }
    }

    // Command ::= '#' Name CommandArg
    // CommandArg ::= Name | String | Number

    private void parseCommand (ParserHandler handler) 
        throws SyntaxError, IOException {
        if (DEBUG) traceMessage("parseCommand");

        nextToken();
        if (lookingAtName()) {
            String cmd = getSval();
            nextToken();
            if (lookingAtNumber())
                handler.handleCommand(cmd, Integer.toString(getNval()));
            else if (lookingAtToken('"') || lookingAtName())
                handler.handleCommand(cmd, getSval());
            else
                throw new SyntaxError ("Bad command arg at " + lineNo());
            nextToken();
        } else
            throw new SyntaxError ("Bad command name at " + lineNo());
    }

    // Definition ::= AliasDef | SyntaxDef
    // AliasDef ::= Name '=' Type
    // SyntaxDef ::= Name '::=' NodeDef { '|' NodeDef }

    private DefinitionAST parseDefinition () throws SyntaxError, IOException {
        if (DEBUG) traceMessage("parseDefinition");

        String name = getSval();
        nextToken();
        if (lookingAtWord("=")) {
            nextToken();
            if (lookingAtName()) {
                TypeAST type = parseType();
                return new AliasDefAST(name, type);
            } else
                throw new SyntaxError ("Bad alias definition for " + name + 
                                       " at " + lineNo());
        } else if (lookingAtWord("::=")) {
            nextToken();
            if (lookingAtName()) {
                ArrayList ns = new ArrayList();
                ns.add(parseNodeDef());
                while (lookingAtToken('|')) {
                    nextToken();
                    ns.add(parseNodeDef());
                }
                return new SyntaxDefAST
                    (name, 
                     (NodeDefAST[])ns.toArray(new NodeDefAST[ns.size()]));
            } else {
                throw new SyntaxError ("Bad syntax definition for " + name + 
                                       " at " + lineNo());
            }
        } else {
            throw new SyntaxError ("Bad definition for " + name + 
                                   " at " + lineNo());
        }
    }


    // NodeDef ::= Name [ '(' [ FieldDef { ',' FieldDef }] ')' ]

    private NodeDefAST parseNodeDef () throws SyntaxError, IOException {
        if (DEBUG) traceMessage("parseNodeDef");

        String name = getSval();
        nextToken();
        if (lookingAtToken('(')) {
            nextToken();
            ArrayList fs = new ArrayList();
            if (lookingAtName()) {
                fs.add(parseFieldDef());
                while (lookingAtToken(',')) {
                    nextToken();
                    if (lookingAtName())
                        fs.add(parseFieldDef());
                    else
                        throw new SyntaxError ("Bad field definition for " + 
                                               name + " at " + lineNo());
                }
            }
            if (lookingAtToken(')')) {
                nextToken();
                return new CNodeDefAST
                    (name,
                     (FieldDefAST[])fs.toArray(new FieldDefAST[fs.size()]));
            } else
                throw new SyntaxError("Incomplete definition for " + name + 
                                      " at " + lineNo());
        } else
            return new ANodeDefAST(name);
    }

    // FieldDef ::= Name ':' Type

    private FieldDefAST parseFieldDef () throws SyntaxError, IOException {
        if (DEBUG) traceMessage("parseFieldDef");

        String name = getSval();
        nextToken();
        if (lookingAtWord(":")) {
            nextToken();
            TypeAST type = parseType();
            return new FieldDefAST(name, type);
        } else
            throw new SyntaxError("Bad field definition for " + name + 
                                  " at " + lineNo());
    }

    // Type ::= Name | Type '[]'
    // Type ::= Name { '[]' }

    private TypeAST parseType () throws SyntaxError, IOException {
        if (DEBUG) traceMessage("parseType");

        TypeAST type = new PrimTypeAST(getSval());
        nextToken();
        while (lookingAtWord("[]")) {
            nextToken();
            type = new ArrayTypeAST(type);
        }
        return type;
    }

    // for Debugging
    private static final boolean DEBUG = false;

    private final void traceMessage (String function) {
        System.err.println(function + 
                           " (looking at " + tokenizer.toString() + ")");
    }
}

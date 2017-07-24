/*
 * $Id: Tokenizer.java,v 1.2 2005/05/19 08:23:11 takuo Exp $
 * Tokenizer.java
 * Copyright (C) 2000,2001,2002 Takuo Watanabe (takuo@acm.org)
 * All Rights Reserved
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

package jp.ac.titech.cs.psg.util;

import java.io.*;

public class Tokenizer {
    private static final boolean DEBUG = false;

    private PushbackReader in = null;
    private boolean pushedback = false;

    // Character Types
    public static final int CT_WHITESPACE =   1;
    public static final int CT_DIGIT      =   2;
    public static final int CT_IDLETTER   =   4;
    public static final int CT_OPLETTER   =   8;
    public static final int CT_QUOTE      =  16;
    public static final int CT_ESCAPE     =  32;
    public static final int CT_COMMENT1   =  64;
    public static final int CT_COMMENT2   = 128;

    private int[] ctype = new int[256];

    private boolean use_c_comment = true;
    private boolean use_cpp_comment = true;

    // Token Types
    public static final int TT_EOF        = -1;
    public static final int TT_NUMBER     = -2;
    public static final int TT_WORD       = -3;
    public static final int TT_NOTHING    = -4;

    public int ttype = TT_NOTHING;
    public int nval = 0;
    public String sval = null;
    public int lineno = 0;

    // Private constructor that initializes everything except the streams.
    private Tokenizer () {
        lineno = 1;
        resetSyntax();
        setCharType('\u0000', ' ', CT_WHITESPACE);
        setCharType('a', 'z', CT_IDLETTER);
        setCharType('A', 'Z', CT_IDLETTER);
        setCharType("_", CT_IDLETTER);
        setCharType("!%&*+-/<=>^~|", CT_OPLETTER);
        setCharType("'\"", CT_QUOTE);
        setCharType("\\", CT_ESCAPE);
        setCharType('0', '9', CT_DIGIT);
        setCharType("/", CT_COMMENT1);
        setCharType("*", CT_COMMENT2);
        useCComment(true);
        useCppComment(true);
    }

    public Tokenizer (Reader r) {
        this();
        in = new PushbackReader(r);
    }

    public Tokenizer (InputStream s) {
        this(new InputStreamReader(s));
    }

    public void resetSyntax () {
        for (int i = 0; i < ctype.length; i++) {
            ctype[i] = 0;
        }
    }

    public void setCharType (char low, char high, int type) {
        // assert 0 <= low
        // assert high <= ctype.length
        for (char c = low; c <= high; c++) {
            ctype[c] |= type;
        }
    }

    public void setCharType (String chars, int type) {
        for (int i=0; i < chars.length(); i++) {
            ctype[chars.charAt(i)] |= type;
        }
    }

    private final boolean charType (int c, int type) {
        if ((c >= 0) && (c < ctype.length)) {
            return (ctype[c] & type) != 0;
        } else {
            return type == CT_IDLETTER;
        }
    }

    public void useCComment (boolean b) {
        use_c_comment = b;
    }

    public void useCppComment (boolean b) {
        use_cpp_comment = b;
    }

    public int nextToken () throws IOException {
        // FSA state
        final int ST_START        =  0;
        final int ST_START1       =  1;
        final int ST_COMMENT      =  2;
        final int ST_C_COMMENT1   =  3;
        final int ST_C_COMMENT2   =  4;
        final int ST_CPP_COMMENT  =  5;
        final int ST_NUMBER       =  6;
        final int ST_IDENTIFIER   =  7;
        final int ST_OPERATOR     =  8;
        final int ST_QUOTE        =  9;
        final int ST_QUOTE_ESCAPE = 10;

        if (pushedback) {
            pushedback = false;
            return ttype;
        }

        int state = ST_START;
        int c = in.read();
        int comment_char = -1;
        int quote_char = -1;
        StringBuffer sbuf = null;

        while (true) {
            switch (state) {
            case ST_START:
                if (DEBUG)
                    System.err.println("START       : c = " + (char)c);
                if (c == -1) {
                    ttype = TT_EOF;
                    return ttype;
                }
                if (charType(c, CT_COMMENT1)) {
                    comment_char = c;
                    c = in.read();
                    state = ST_COMMENT;
                    break;
                }
                // state = ST_START1;
                // break;

            case ST_START1:
                if (DEBUG) 
                    System.err.println("START1      : c = " + (char)c);
                if (charType(c, CT_WHITESPACE)) {
                    if (c == '\n') lineno++;
                    c = in.read();
                    state = ST_START;
                    break;
                }
                if (charType(c, CT_DIGIT)) {
                    sbuf = new StringBuffer();
                    sbuf.append((char)c);
                    c = in.read();
                    state = ST_NUMBER;
                    break;
                }
                if (charType(c, CT_IDLETTER)) {
                    sbuf = new StringBuffer();
                    sbuf.append((char)c);
                    c = in.read();
                    state = ST_IDENTIFIER;
                    break;
                }
                if (charType(c, CT_OPLETTER)) {
                    sbuf = new StringBuffer();
                    sbuf.append((char)c);
                    c = in.read();
                    state = ST_OPERATOR;
                    break;
                }
                if (charType(c, CT_QUOTE)) {
                    sbuf = new StringBuffer();
                    quote_char = c;
                    c = in.read();
                    state = ST_QUOTE;
                    break;
                }
                ttype = c;
                return ttype;

            case ST_COMMENT:
                if (DEBUG) 
                    System.err.println("COMMENT     : c = " + (char)c);
                if (use_c_comment && charType(c, CT_COMMENT2)) {
                    c = in.read();
                    state = ST_C_COMMENT1;
                    break;
                }
                if (use_cpp_comment && charType(c, CT_COMMENT1)) {
                    c = in.read();
                    state = ST_CPP_COMMENT;
                    break;
                }
                in.unread(c);
                c = comment_char;
                state = ST_START1;
                break;

            case ST_C_COMMENT1: // C-style comment
                if (DEBUG) 
                    System.err.println("C_COMMENT1  : c = " + (char)c);
                if (c == -1) abort("Unexpected EOF");
                if (c == '\n') lineno++;
                if (charType(c, CT_COMMENT2))
                    state = ST_C_COMMENT2;
                c = in.read();
                break;

            case ST_C_COMMENT2: // C-style comment
                if (DEBUG)
                    System.err.println("C_COMMENT2  : c = " + (char)c);
                if (c == -1) abort("Unexpected EOF");
                if (charType(c, CT_COMMENT1))
                    state = ST_START;
                else
                    state = ST_C_COMMENT1;
                c = in.read();
                break;

            case ST_CPP_COMMENT:
                if (DEBUG) 
                    System.err.println("CPP_COMMENT : c = " + (char)c);
                if (c == -1) state = ST_START;
                if (c == '\n') {
                    lineno++;
                    state = ST_START;
                }
                c = in.read();
                break;

            case ST_NUMBER: // Numbers
                if (DEBUG) 
                    System.err.println("NUMBER      : c = " + (char)c);
                if (!charType(c, CT_DIGIT)) {
                    nval = Integer.parseInt(sbuf.toString());
                    in.unread(c);
                    ttype = TT_NUMBER;
                    return ttype;
                }
                sbuf.append((char)c);
                c = in.read();
                state = ST_NUMBER;
                break;

            case ST_IDENTIFIER:
                if (DEBUG) 
                    System.err.println("IDENTIFIER  : c = " + (char)c);
                if (!charType(c, CT_IDLETTER | CT_DIGIT)) {
                    sval = sbuf.toString();
                    in.unread(c);
                    ttype = TT_WORD;
                    return ttype;
                }
                sbuf.append((char)c);
                c = in.read();
                state = ST_IDENTIFIER;
                break;

            case ST_OPERATOR:
                if (DEBUG) 
                    System.err.println("OPERATOR    : c = " + (char)c);
                if (!charType(c, CT_OPLETTER)) {
                    sval = sbuf.toString();
                    in.unread(c);
                    ttype = TT_WORD;
                    return ttype;
                }
                sbuf.append((char)c);
                c = in.read();
                state = ST_OPERATOR;
                break;

            case ST_QUOTE: // Quoted characters
                if (DEBUG) 
                    System.err.println("QUOTE       : c = " + (char)c);
                if (c == -1) abort("Unexpected EOF");
                if (c == '\n') lineno++;
                if (c == quote_char) {
                    sval = sbuf.toString();
                    ttype = c;
                    return ttype;
                }
                if (c == '\\') {
                    c = in.read();
                    state = ST_QUOTE_ESCAPE;
                    break;
                }
                sbuf.append((char)c);
                c = in.read();
                state = ST_QUOTE;
                break;

            case ST_QUOTE_ESCAPE:
                if (DEBUG) 
                    System.err.println("QUOTE_ESCAPE: c = " + (char)c);
                if (c == -1) abort("Unexpected EOF");
                if (c == '\n') lineno++;
                if (c == 'n') {
                    sbuf.append('\n');
                }
                else if (c == 't') {
                    sbuf.append('\t');
                }
                else {
                    sbuf.append((char)c);
                }
                c = in.read();
                state = ST_QUOTE;
                break;

            default: // should not come here
                throw new IOException("Unknown state. (c = '" + 
                                      (char)c + "')");
            }
        }
    }

    public void pushBack () {
        if (ttype != TT_NOTHING) {
            pushedback = true;
        }
    }

    public int lineno () {
        return lineno;
    }

    public String toString () {
        switch (ttype) {
        case TT_EOF:
            return "EOF (" + lineno + ")";
        case TT_NUMBER:
            return "NUMBER [" + nval + "] (" + lineno + ")";
        case TT_WORD:
            return "WORD [" + sval + "] (" + lineno + ")";
        case TT_NOTHING:
            return "NOTHING (" + lineno + ")";
        default:
            if (charType(ttype, CT_QUOTE)) 
                return "TOKEN [" + (char)ttype + sval + (char)ttype + 
                    "] (" + lineno + ")";
            else 
                return "TOKEN [" + (char)ttype + "] (" + lineno + ")";
        }
    }

    private final void abort (String message) throws IOException {
        throw new IOException (message);
    }
}

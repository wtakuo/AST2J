/*
 * $Id: TransAST.java,v 1.3 2005/05/19 08:23:11 takuo Exp $
 * TransAST.java
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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;

class TransAST extends ASTVisitor implements ParserHandler {

    // Constants
    private static final String fext = ".java";

    // Parameters
    private String param_root = "Object";
    private boolean param_genrootp = false;
    private String param_visitor = null;
    private String param_modifier = null;
    private String param_package = null;
    private String param_exception = null;
    private String param_source = null;
    private String param_author = null;
    private String param_version = null;
    private String param_message = null;
    private boolean param_uselineno = false;

    // Accessors for Parameters
    public void setRoot (String root) {
        param_root = root;
    }

    public void setGenRoot (boolean flag) {
        param_genrootp = flag;
    }

    public void setVisitor (String visitor) {
        param_visitor = visitor;
    }

    public void setModifier (String modifier) {
        param_modifier = modifier;
    }

    public void setPackage (String pkg) {
        param_package = pkg;
    }

    public void setException (String exc) {
        param_exception = exc;
    }

    public void setSource (String source) {
        param_source = source;
    }

    public void setAuthor (String author) {
        param_author = author;
    }

    public void setVersion (String version) {
        param_version = version;
    }

    public void setMessage (String message) {
        param_message = message;
    }

    public void setUseLineNo (boolean uselineno) {
        param_uselineno = uselineno;
    }

    // Tables
    private HashMap aliases = new HashMap();
    private HashMap supertbl = new HashMap();
    private ArrayList vmethods = new ArrayList();

    // Temporary Variables
    private PrintStream out = null;
    private String supername = null;
    private boolean debug = false;
 
    public TransAST () {}

    ////////////////////////////////////////////////////////////////////////
    // ParserCommand I/F Methods

    public void handleAST (AST ast) {
        ast.accept(this);
    }

    public void handleCommand (String cmd, String arg) {
        if (cmd.equalsIgnoreCase("root")) {
            setRoot(arg);
        } else if (cmd.equalsIgnoreCase("generate_root")) {
            if (arg.equalsIgnoreCase("yes")) setGenRoot(true);
        } else if (cmd.equalsIgnoreCase("visitor")) {
            setVisitor(arg);
        } else if (cmd.equalsIgnoreCase("package")) {
            setPackage(arg);
        } else if (cmd.equalsIgnoreCase("exception")) {
            setException(arg);
        } else if (cmd.equalsIgnoreCase("modifier")) {
            setModifier(arg);
        } else if (cmd.equalsIgnoreCase("use_lineno")) {
            if (arg.equalsIgnoreCase("yes")) setUseLineNo(true);
        } else if (cmd.equalsIgnoreCase("author")) {
            setAuthor(arg);
        } else if (cmd.equalsIgnoreCase("version")) {
            setVersion(arg);
        } else {
            System.err.println("Warning: unknown command " + cmd);
        }
    }

    ////////////////////////////////////////////////////////////////////////
    // ASTVisitor Methods

    public void visitAliasDefAST (AliasDefAST n) {
        String name = n.getName();
        if (aliases.containsKey(name))
            System.err.println("Warning: redefinition of " + name);
        aliases.put(name, n.getType());
    }

    public void visitSyntaxDefAST (SyntaxDefAST n) {
        String name = n.getName();
        NodeDefAST[] nodes = n.getDefs();
        if (nodes.length == 1 && (nodes[0] instanceof CNodeDefAST) &&
            ((CNodeDefAST)nodes[0]).getName().equals(name)) {
            if (supertbl.containsKey(name))
                supername = (String)supertbl.get(name);
            else
                supername = param_root;
        } else {
            if (!name.equals(param_root)) {
                try {
                    String filename = name + fext;
                    if (!debug) {
                        out = new PrintStream
                            (new BufferedOutputStream
                             (new FileOutputStream(filename)));
                    }
                    writeHeader(out, filename);
                    if (param_package != null) 
                        out.println("package " + param_package + ";");
                    if (param_modifier != null)
                        out.print(param_modifier + " ");
                    out.print("abstract class " + name + " extends ");
                    if (supertbl.containsKey(name))
                        out.print((String)supertbl.get(name));
                    else
                        out.print(param_root);
                    out.println(" {}");
                    out.flush();
                } catch (IOException e) {}
                finally {
                    if (!debug && out != null) out.close();
                }
            }
            supername = name;
        }
        for (int i=0; i<nodes.length; i++) {
            nodes[i].accept(this);
        }
    }

    public void visitCNodeDefAST (CNodeDefAST n) {
        try {
            String name = n.getName();
            String filename = name + fext;
            if (!debug) {
                out = new PrintStream
                    (new BufferedOutputStream
                     (new FileOutputStream(filename)));
            }
            writeHeader(out, filename);
            FieldDefAST[] fields = n.getFields();
            if (param_package != null) 
                out.println("package " + param_package + ";");
            if (param_modifier != null) 
                out.print(param_modifier + " ");
            out.println("class " + name + " extends " + supername + " {");

            // private fields
            if (param_uselineno) {
                out.println("    private int __lineno;");
            }
            for (int i=0; i<fields.length; i++) {
                out.print("    private ");
                fields[i].getType().accept(this);
                out.println(" " + fields[i].getVar() + ";");
            }

            // constructor
            out.print("    public " + name + " (");
            if (param_uselineno) {
                out.print("int __lineno");
                if (fields.length>0) {
                    out.print(", ");
                }
            }
            if (fields.length>0) {
                fields[0].getType().accept(this);
                out.print(" " + fields[0].getVar());
                for (int i=1; i<fields.length; i++) {
                    out.print(", ");
                    fields[i].getType().accept(this);
                    out.print(" " + fields[i].getVar());
                }
            }
            out.println(") {");
            if (param_uselineno) {
                out.println("        this.__lineno = __lineno;");
            }
            for (int i=0; i<fields.length; i++) {
                String v = fields[i].getVar();
                out.println("        this." + v + " = " + v + ";");
            }
            out.println("    }");

            // accessor
            if (param_uselineno) {
                out.println("    public int getLineNo () { return __lineno; }");
            }
            for (int i=0; i<fields.length; i++) {
                String v = fields[i].getVar();
                out.print("    public ");
                fields[i].getType().accept(this);
                char[]vs = v.toCharArray();
                vs[0] = Character.toUpperCase(vs[0]);
                out.println(" get" + new String(vs) + 
                            " () { return " + v + "; }");
            }
            // visitor
            if (param_visitor != null) {
                out.print("    public void accept (" + param_visitor + " v)");
                String vm = "void visit" + name + " (" + name + " n)";
                if (param_exception != null) {
                    out.print(" throws " + param_exception);
                    vm = vm + " throws " + param_exception;
                }
                out.println(" {");
                out.println("        v.visit" + name + "(this);");
                out.println("    }");
                vmethods.add(vm);
            }
            out.println("}");
            out.flush();

        } catch (IOException e) {}
        finally {
            if (!debug && out != null) out.close();
        }
    }

    public void visitANodeDefAST (ANodeDefAST n) {
        String name = n.getName();
        if (supertbl.containsKey(name))
            System.err.println("Warning: redefinition of " + name);
        supertbl.put(name, supername);
    }

    public void visitFieldDefAST (FieldDefAST n) {}

    public void visitPrimTypeAST (PrimTypeAST n) {
        String tname = n.getName();
        if (aliases.containsKey(tname)) {
            ((TypeAST)aliases.get(tname)).accept(this);
        }
        else
            out.print(tname);
    }

    public void visitArrayTypeAST (ArrayTypeAST n) {
        n.getElem().accept(this);
        out.print("[]");
    }

    ////////////////////////////////////////////////////////////////////////
    // Other Methods

    public void finishUp () {
        if (param_genrootp) {
            generateRootClass();
        }
        if (param_visitor != null) {
            generateVisitor();
        }
    }

    private void generateRootClass () {
        try {
            String filename = param_root + fext;
            if (!debug) {
                out  = new PrintStream
                    (new BufferedOutputStream
                     (new FileOutputStream(filename)));
            }
            writeHeader(out, filename);
            if (param_package != null) 
                out.println("package " + param_package + ";");
            if (param_modifier != null) 
                out.print(param_modifier + " ");
            out.print("abstract class " + param_root + " {");
            if (param_visitor != null) {
                out.println("");
                out.print("    public abstract void accept (" + 
                          param_visitor + " v)");
                if (param_exception != null) {
                    out.print(" throws " + param_exception);
                }
                out.println(";");
            }
            out.println("}");
        } catch (IOException e) {}
        finally {
            if (!debug && out != null) out.close(); 
        }
    }

    private void generateVisitor () {
        try {
            String filename = param_visitor + fext;
            if (!debug) {
                out = new PrintStream
                    (new BufferedOutputStream(new FileOutputStream(filename)));
            }
            writeHeader(out, filename);
            if (param_package != null) 
                out.println("package " + param_package + ";");
            if (param_modifier != null) 
                out.print(param_modifier + " ");
            out.println("abstract class " + param_visitor + " {");
            for (int i=0; i < vmethods.size(); i++) {
                out.println("    public abstract " + vmethods.get(i) + ";");
            }
            out.println("}");
            out.flush();
        } catch (IOException e) {}
        finally {
            if (!debug && out != null)
                out.close();
        }
    }

    private void writeHeader (PrintStream out, String filename) {
        if (!debug) {
            out.println("// " + filename);
            if (param_message != null) out.println(param_message);
        }
        if (param_source != null || 
            param_version != null || 
            param_author != null) {
            out.print("//");
            if (param_source != null) 
                out.print(" Source: " + param_source);
            if (param_version != null) 
                out.print(" Version: " + param_version);
            if (param_author != null) 
                out.print(" Author: " + param_author);
            out.println("");
        }
    }

    public void setDebug () {
        setDebug(System.out);
    }

    public void setDebug (PrintStream out) {
        debug = true;
        this.out = out;
    }

    public void unsetDebug () {
        debug = false;
        this.out = null;
    }
}

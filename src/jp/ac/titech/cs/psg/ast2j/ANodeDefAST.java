// ANodeDefAST.java
// This file was generated by AST2J (0.5a) on Thu May 19 17:16:11 JST 2005
// Source: ast2j.ast Version: 1.0 Author: Takuo Watanabe (takuo@acm.org)
package jp.ac.titech.cs.psg.ast2j;
class ANodeDefAST extends NodeDefAST {
    private String name;
    public ANodeDefAST (String name) {
        this.name = name;
    }
    public String getName () { return name; }
    public void accept (ASTVisitor v) {
        v.visitANodeDefAST(this);
    }
}

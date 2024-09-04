package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.stmt.AstStmt;

public class ArrRef {
    public AstExpr subscriptExpr;
    public AstNameExpr arrExpr;
    public AstStmt refStmt;
    public AstDefn arrDefn;
    public LoopDescriptor loop;
    public int depth;
    public int stmtNum;
    public boolean assign;

    public ArrRef(AstExpr subscriptExpr, AstNameExpr arrExpr, AstStmt refStmt,
                  AstDefn arrDefn, LoopDescriptor loop, int stmtNum, int depth) {
        this.subscriptExpr = subscriptExpr;
        this.arrExpr = arrExpr;
        this.refStmt = refStmt;
        this.arrDefn = arrDefn;
        this.loop = loop;
        this.stmtNum = stmtNum;
        this.depth = depth;
        this.assign = false;
    }

    @Override
    public String toString() {
        String expr = this.arrExpr.toString() + "[" + this.subscriptExpr.toString() + "]";
        if(this.assign) {
            expr = expr + "=";
        }else{
            expr =  "=" + expr;
        }

        return String.format("%-20s |\tassign=%-5s | L%dS%d", expr, this.assign, this.depth, this.stmtNum);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrRef ref) {
            return this.arrDefn == ref.arrDefn;
        }
        return false;
    }
}
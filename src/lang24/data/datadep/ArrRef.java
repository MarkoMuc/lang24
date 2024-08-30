package lang24.data.datadep;

import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.stmt.AstStmt;

public class ArrRef {
    public AstExpr subscript_expr;
    public AstExpr arrExpr;
    public AstStmt refStmt;
    public int depth;
    public boolean assign;

    public ArrRef(AstExpr subscriptExpr, AstExpr arrExpr, AstStmt refStmt, int depth) {
        this.subscript_expr = subscriptExpr;
        this.arrExpr = arrExpr;
        this.refStmt = refStmt;
        this.depth = depth;
        this.assign = false;
    }

    @Override
    public String toString() {
        String expr = arrExpr.toString();
        if(this.assign) {
            expr = expr + "=";
        }else{
            expr =  "=" + expr;
        }

        return String.format("%-20s |\tassign=%s", expr, assign);
    }
}
package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.stmt.AstStmt;

import java.util.Vector;

public class ArrRef {
    public Vector<AstExpr> subscriptExprs;
    public AstNameExpr arrExpr;
    public AstStmt refStmt;
    public AstDefn arrDefn;
    public LoopDescriptor loop;
    public int depth;
    public int stmtNum;
    public boolean assign;


    public ArrRef(AstExpr subscriptExpr, AstNameExpr arrExpr, AstStmt refStmt,
                  AstDefn arrDefn, LoopDescriptor loop, int stmtNum, int depth) {
        this.subscriptExprs = new Vector<>();
        this.subscriptExprs.add(subscriptExpr);
        this.arrExpr = arrExpr;
        this.refStmt = refStmt;
        this.arrDefn = arrDefn;
        this.loop = loop;
        this.stmtNum = stmtNum;
        this.depth = depth;
        this.assign = false;
    }

    public ArrRef(Vector<AstExpr> subscriptExprs, AstNameExpr arrExpr, AstStmt refStmt,
                  AstDefn arrDefn, LoopDescriptor loop, int stmtNum, int depth) {
        this.subscriptExprs = subscriptExprs;
        this.arrExpr = arrExpr;
        this.refStmt = refStmt;
        this.arrDefn = arrDefn;
        this.loop = loop;
        this.stmtNum = stmtNum;
        this.depth = depth;
        this.assign = false;
    }

    public int getSize() {
        return subscriptExprs.size();
    }


    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(this.arrExpr.toString());

        for (var subscript : subscriptExprs) {
            sb.append("[").append(subscript.toString()).append("]");
        }

        if(this.assign) {
            sb.append("=");
        }else{
            sb.insert(0, "=");
        }

        return String.format("(L%dS%d|%s) %s", this.depth, this.stmtNum, this.assign, sb);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ArrRef ref) {
            return this.arrDefn == ref.arrDefn;
        }
        return false;
    }
}
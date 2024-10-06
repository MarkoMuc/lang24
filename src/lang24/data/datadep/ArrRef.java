package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.stmt.AstStmt;

import java.util.Vector;

/**
 * Class representing the array reference inside a loop that will be checked for dependence.
 *
 * @author marko.muc12@gmail.com
 */
public class ArrRef {
    /**
     * The subscript expressions indexing this array.
     **/
    public Vector<AstExpr> subscriptExprs;

    /** AstNameExpr of this array. **/
    public AstNameExpr arrExpr;

    /** AstStmt object of the statement that carries this expression. **/
    public AstStmt refStmt;

    /** AstDefn object of this array variable. **/
    public AstDefn arrDefn;

    /** LoopDescriptor carrying this reference. **/
    public LoopDescriptor loop;

    /** Loop depth. **/
    private int depth;

    /** Statement number inside this loop. **/
    public int stmtNum;

    /** Write or read reference. **/
    public boolean assign;

    /**
     * Constructor for ArrRef with one subscript.
     * @param subscriptExpr    The subscript of this reference.
     * @param arrExpr           The AST object of the array expression.
     * @param refStmt           The ASTS stmt carrying this expression.
     * @param arrDefn           The definition of this array variable.
     * @param loop              The loop this array reference resides in.
     * @param stmtNum           Statement number of the statement.
     * @param depth             The depth of this statement based on the nest.
     */
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

    /**
     * Constructor for ArrRef with more than one subscript.
     * @param subscriptExprs    All subscripts tied to this reference.
     * @param arrExpr           The AST object of the array expression.
     * @param refStmt           The ASTS stmt carrying this expression.
     * @param arrDefn           The definition of this array variable.
     * @param loop              The loop this array reference resides in.
     * @param stmtNum           Statement number of the statement.
     * @param depth             The depth of this statement based on the nest.
     */
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

    /**
     * @return Returns the number of subscripts.
     */
    public int getSubscriptCount() {
        return subscriptExprs.size();
    }

    /**
     * Returns depth as per theory.
     *
     * @return Depth.
     */
    public int getDepth() {
        return this.depth;
    }

    /**
     * Used in indexing arrays that depend on loop depth.
     *
     * @return Depth of this loop -1.
     */
    public int getDepthAsIdx() {
        return this.depth - 1;
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
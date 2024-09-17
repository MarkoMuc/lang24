package lang24.data.ast.tree.expr;

import lang24.common.report.Locatable;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.visitor.AstVisitor;

/**
 * An array access expression.
 *
 * @author marko.muc12@gmail.com
 */
public class AstMultiArrExpr extends AstExpr {

    /**
     * The array.
     */
    public final AstExpr arr;

    /**
     * The index.
     */
    public final AstNodes<AstExpr> idxs;

    /**
     * Constructs an array access expression.
     *
     * @param location The location.
     * @param arr      The array.
     * @param idxs     The indexes
     */
    public AstMultiArrExpr(Locatable location, AstExpr arr, AstNodes<AstExpr> idxs) {
        super(location);
        this.arr = arr;
        this.idxs = idxs;
    }

    @Override
    public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
        return visitor.visit(this, arg);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", arr, idxs);
    }
}
package lang24.data.ast.tree.stmt;


import lang24.common.report.Locatable;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.visitor.AstVisitor;

/**
 * A vectorization decorator statement.
 *
 * @author marko.muc12@gmail.com
 */
public class AstDecoratorStmt extends AstStmt {

    /** Expressions tight to this decorator. */
    public final AstNodes<AstExpr> deps;

    /** The inner statement. */
    public final AstStmt stmt;

    /**
     * Constructs a decorator statement.
     *
     * @param location The location.
     * @param deps      The dependencies for the vectorization
     * @param body     The inner statement.
     */

    public AstDecoratorStmt(Locatable location, AstNodes<AstExpr> deps, AstStmt body) {
        super(location);
        this.deps = deps;
        this.stmt = body;
    }

    @Override
    public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
        return visitor.visit(this, arg);
    }
}

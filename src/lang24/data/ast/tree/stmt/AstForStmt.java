package lang24.data.ast.tree.stmt;

import lang24.common.report.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.visitor.*;

/**
 * A for statement.
 *
 */
public class AstForStmt extends AstStmt {

    /** The initialization state for a variable. */
    public final AstAssignStmt init;

    /** The condition. */
    public final AstExpr cond;

    /** The step for a variable. */
    public final AstAssignStmt step;

    /** The inner statement. */
    public final AstStmt stmt;

    /**
     * Constructs a for statement.
     *
     * @param location The location.
     * @param init     The initialization statement.
     * @param cond     The condition.
     * @param step     The step statement.
     * @param body     The inner statement.
     */
    public AstForStmt(Locatable location, AstAssignStmt init, AstExpr cond, AstAssignStmt step, AstStmt body) {
        super(location);
        this.init = init;
        this.cond = cond;
        this.step = step;
        this.stmt = body;
    }

    @Override
    public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
        return visitor.visit(this, arg);
    }

}

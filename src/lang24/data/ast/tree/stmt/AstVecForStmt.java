package lang24.data.ast.tree.stmt;

import lang24.common.report.Locatable;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.visitor.AstVisitor;

/**
 * A for statement.
 *
 */
public class AstVecForStmt extends AstStmt {

    /** The variable name. */
    public final AstNameExpr name;

    /** The lower limit bound constant. */
    public final AstExpr lower;

    /** The upper bound constant. */
    public final AstExpr upper;

    /** The step constant. */
    public final AstExpr step;

    /** The inner statement. */
    public final AstStmt stmt;

    /**
     * Constructs a for statement.
     *
     * @param location The location.
     * @param name     The iteration variable name.
     * @param lower    The lower bound and value to initialize at.
     * @param upper    The upper bound.
     * @param step     The step.
     * @param body     The inner statement.
     */
    public AstVecForStmt(Locatable location, AstNameExpr name, AstExpr lower, AstExpr upper, AstExpr step, AstStmt body) {
        super(location);
        this.name = name;
        this.lower = lower;
        this.upper = upper;
        this.step = step;
        this.stmt = body;
    }

    @Override
    public <Result, Argument> Result accept(AstVisitor<Result, Argument> visitor, Argument arg) {
        return visitor.visit(this, arg);
    }

}

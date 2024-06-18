package lang24.data.imc.code.stmt;

import lang24.common.logger.Logger;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.imc.visitor.ImcVisitor;

import java.util.Vector;

public class ImcVectStmt extends ImcStmt{

    /** The sequence of parameters. */
    public final Vector<AstNameExpr> pars;

    /** The sequence of statements. */
    public final ImcSTMTS stmts;

    /**
     * Constructs vector statement.
     *
     * @param pars The sequence of parameters.
     * @param stmts The sequence of statements.
     */
    public ImcVectStmt(Vector<AstNameExpr> pars, ImcSTMTS stmts) {
        this.pars = new Vector<>(pars);
        this.stmts = stmts;
    }

    @Override
    public <Result, Arg> Result accept(ImcVisitor<Result, Arg> visitor, Arg accArg) {
        return visitor.visit(this, accArg);
    }

    @Override
    public void log(Logger logger) {
        logger.begElement("imc");

        StringBuffer buffer = new StringBuffer();
        buffer.append("VectorStmt[");
        for (int s = 0; s < pars.size(); s++) {
            if (s > 0)
                buffer.append(", ");
            buffer.append(pars.get(s).name);
        }
        buffer.append("]");

        logger.addAttribute("instruction", buffer.toString());

        stmts.log(logger);
        logger.endElement();
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("VectorStmt[");
        for (int s = 0; s < pars.size(); s++) {
            if (s > 0)
                buffer.append(",");
            buffer.append(pars.get(s).toString());
        }
        buffer.append("]");
        buffer.append(stmts.toString());

        return buffer.toString();
    }
}

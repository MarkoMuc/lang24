package lang24.data.imc.code.stmt;

import lang24.common.logger.Logger;
import lang24.data.imc.code.expr.ImcExpr;
import lang24.data.imc.visitor.ImcVisitor;

public class ImcVecMOVE extends ImcStmt {

    /**
     * The destination.
     */
    public final ImcExpr dst;

    /**
     * The source.
     */
    public final ImcExpr src;

    /**
     * Constructs a move operation.
     */
    public ImcVecMOVE(ImcExpr dst, ImcExpr src) {
        this.dst = dst;
        this.src = src;
    }

    @Override
    public <Result, Arg> Result accept(ImcVisitor<Result, Arg> visitor, Arg accArg) {
        return visitor.visit(this, accArg);
    }

    @Override
    public void log(Logger logger) {
        logger.begElement("imc");
        logger.addAttribute("instruction", "VECMOVE");
        dst.log(logger);
        src.log(logger);
        logger.endElement();
    }

    @Override
    public String toString() {
        return "VECMOVE(" + dst.toString() + "," + src.toString() + ")";
    }

}

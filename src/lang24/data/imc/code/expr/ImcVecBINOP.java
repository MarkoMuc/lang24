package lang24.data.imc.code.expr;

import lang24.common.logger.Logger;
import lang24.data.imc.visitor.ImcVisitor;

public class ImcVecBINOP extends ImcExpr {
    public enum Oper {
        ADD, SUB, MUL, DIV, MOD,
    }

    /**
     * The operator.
     */
    public final Oper oper;

    /**
     * The first operand.
     */
    public final ImcExpr fstExpr;

    /**
     * The second operand.
     */
    public final ImcExpr sndExpr;

    /**
     * Constructs a new binary operation.
     *
     * @param oper    The operator.
     * @param fstExpr The first operand.
     * @param sndExpr The second operand.
     */
    public ImcVecBINOP(Oper oper, ImcExpr fstExpr, ImcExpr sndExpr) {
        this.oper = oper;
        this.fstExpr = fstExpr;
        this.sndExpr = sndExpr;
    }

    @Override
    public <Result, Arg> Result accept(ImcVisitor<Result, Arg> visitor, Arg accArg) {
        return visitor.visit(this, accArg);
    }

    @Override
    public void log(Logger logger) {
        logger.begElement("imc");
        logger.addAttribute("instruction", "VECBINOP(" + oper + ")");
        fstExpr.log(logger);
        sndExpr.log(logger);
        logger.endElement();
    }

    @Override
    public String toString() {
        return "VECBINOP(" + oper + "," + fstExpr.toString() + "," + sndExpr.toString() + ")";
    }

}

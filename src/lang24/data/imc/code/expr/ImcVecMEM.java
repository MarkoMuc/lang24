package lang24.data.imc.code.expr;

import lang24.common.logger.Logger;
import lang24.data.imc.visitor.ImcVisitor;

public class ImcVecMEM extends ImcExpr {

    public final ImcExpr addr;
    // Start is added to the main
    public final ImcExpr start;
    public final ImcExpr end;

    public ImcVecMEM(ImcExpr addr, ImcExpr start, ImcExpr end) {
        this.addr = addr;
        this.start = start;
        this.end = end;
    }

    @Override
    public <Result, Arg> Result accept(ImcVisitor<Result, Arg> visitor, Arg accArg) {
        return visitor.visit(this, accArg);
    }

    @Override
    public void log(Logger logger) {
        logger.begElement("imc");
        logger.addAttribute("instruction", "MEM");
        addr.log(logger);
        logger.endElement();
    }

    @Override
    public String toString() {
        return "MEM(" + addr.toString() + ")";
    }

}

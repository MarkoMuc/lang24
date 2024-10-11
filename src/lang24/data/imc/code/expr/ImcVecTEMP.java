package lang24.data.imc.code.expr;

import lang24.common.logger.Logger;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemTemp;

public class ImcVecTEMP extends ImcTEMP {

    /**
     * Constructs a temporary variable.
     *
     * @param temp The temporary variable.
     */
    public ImcVecTEMP(MemTemp temp) {
        super(temp);
    }

    @Override
    public <Result, Arg> Result accept(ImcVisitor<Result, Arg> visitor, Arg accArg) {
        return visitor.visit(this, accArg);
    }

    @Override
    public void log(Logger logger) {
        logger.begElement("imc");
        logger.addAttribute("instruction", "VECTEMP(vT" + temp.temp + ")");
        logger.endElement();
    }

    @Override
    public String toString() {
        return "VECTEMP(vT" + temp.temp + ")";
    }


}

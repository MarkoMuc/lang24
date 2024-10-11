package lang24.phase.imclin;

import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.imc.code.stmt.ImcVecMOVE;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemTemp;

import java.util.Vector;

/**
 * @author marko.muc12@gmail.com
 */
public class ExprCanonizer implements ImcVisitor<ImcExpr, Vector<ImcStmt>> {
    @Override
    public ImcExpr visit(ImcBINOP binOp, Vector<ImcStmt> visArg) {
        ImcExpr fstImc = binOp.fstExpr.accept(this, visArg);
        ImcExpr sndImc = binOp.sndExpr.accept(this, visArg);

        ImcTEMP resTemp = new ImcTEMP(new MemTemp());
        visArg.add(new ImcMOVE(resTemp,
                new ImcBINOP(binOp.oper, fstImc, sndImc)));

        return resTemp;
    }

    @Override
    public ImcExpr visit(ImcCALL call, Vector<ImcStmt> visArg) {
        Vector<ImcExpr> args = new Vector<>();

        if (call.args.size() > 1) {
            boolean containsCall = call.args.stream().anyMatch(expr -> expr instanceof ImcCALL);
            for (ImcExpr arg : call.args) {
                if (arg instanceof ImcCONST imcCONST) {
                    args.add(imcCONST);
                } else {
                    if (containsCall) {
                        MemTemp argTemp = new MemTemp();
                        visArg.add(new ImcMOVE(
                                new ImcTEMP(argTemp), arg.accept(this, visArg)));
                        args.add(new ImcTEMP(argTemp));
                    } else {
                        args.add(arg);
                    }
                }
            }
        }

        return new ImcCALL(call.label, call.offs, args);
    }

    @Override
    public ImcExpr visit(ImcCONST constant, Vector<ImcStmt> visArg) {
        return new ImcCONST(constant.value);
    }

    @Override
    public ImcExpr visit(ImcMEM mem, Vector<ImcStmt> visArg) {
        return new ImcMEM(mem.addr.accept(this, visArg));
    }

    @Override
    public ImcExpr visit(ImcNAME name, Vector<ImcStmt> visArg) {
        return new ImcNAME(name.label);
    }

    @Override
    public ImcExpr visit(ImcSEXPR sExpr, Vector<ImcStmt> visArg) {
        visArg.addAll(sExpr.stmt.accept(new StmtCanonizer(), null));
        return sExpr.expr.accept(this, visArg);
    }

    @Override
    public ImcExpr visit(ImcTEMP temp, Vector<ImcStmt> visArg) {
        if (temp instanceof ImcVecTEMP) {
            return new ImcVecTEMP(temp.temp);
        }
        return new ImcTEMP(temp.temp);
    }

    @Override
    public ImcExpr visit(ImcUNOP unOp, Vector<ImcStmt> visArg) {
        return new ImcUNOP(unOp.oper, unOp.subExpr.accept(this, visArg));
    }

    @Override
    public ImcExpr visit(ImcVecBINOP binOp, Vector<ImcStmt> visArg) {
        ImcExpr fstImc = binOp.fstExpr.accept(this, visArg);
        ImcExpr sndImc = binOp.sndExpr.accept(this, visArg);
        ImcVecTEMP resTemp = new ImcVecTEMP(new MemTemp());
        visArg.add(new ImcVecMOVE(resTemp,
                new ImcVecBINOP(binOp.oper, fstImc, sndImc)));

        return resTemp;
    }

    @Override
    public ImcExpr visit(ImcVecMEM vecMem, Vector<ImcStmt> visArg) {
        return new ImcVecMEM(vecMem.addr.accept(this, visArg),
                vecMem.start.accept(this, visArg),
                vecMem.end.accept(this, visArg));
    }

}

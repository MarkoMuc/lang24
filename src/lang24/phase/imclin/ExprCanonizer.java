package lang24.phase.imclin;

import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemTemp;

import java.util.Vector;

public class ExprCanonizer implements ImcVisitor<ImcExpr, Vector<ImcStmt>> {
    private boolean estmt = false;
    public ExprCanonizer(){}
    public ExprCanonizer(boolean estmt) {
        this.estmt = estmt;
    }

    @Override
    public ImcExpr visit(ImcBINOP binOp, Vector<ImcStmt> visArg) {

        // Saves values of both expressions into temps
        ImcTEMP fstTemp = new ImcTEMP(new MemTemp());
        ImcExpr fstImc = binOp.fstExpr.accept(this, visArg);
        visArg.add(new ImcMOVE(fstTemp, fstImc));

        ImcTEMP sndTemp = new ImcTEMP(new MemTemp());
        ImcExpr sndImc = binOp.sndExpr.accept(this, visArg);
        visArg.add(new ImcMOVE(sndTemp, sndImc));

        // Evaluates the operation
        ImcTEMP resTemp = new ImcTEMP(new MemTemp());
        visArg.add(new ImcMOVE(resTemp,
                new ImcBINOP(binOp.oper,fstTemp,sndTemp)));

        return resTemp;
    }

    @Override
    public ImcExpr visit(ImcCALL call, Vector<ImcStmt> visArg) {
        boolean isEstmt = estmt;
        estmt = false;
        Vector<ImcExpr> args = new Vector<>();

        // Saves args into temps
        if( call.args.size() > 1) {
            for (ImcExpr arg : call.args) {
                MemTemp argTemp = new MemTemp();
                visArg.add(new ImcMOVE(
                        new ImcTEMP(argTemp), arg.accept(this, visArg)));
                args.add(new ImcTEMP(argTemp));
            }
        }

        ImcExpr out = call;
        if ( !isEstmt ) {
            // Save result into temp and return it
            ImcTEMP resTemp = new ImcTEMP(new MemTemp());
            visArg.add(new ImcMOVE(resTemp,
                    new ImcCALL(call.label, call.offs, args)));
            out = resTemp;
        }

        return out;
    }

    @Override
    public ImcExpr visit(ImcCONST constant, Vector<ImcStmt> visArg) {
        return new ImcCONST(constant.value);
    }

    @Override
    public ImcExpr visit(ImcMEM mem, Vector<ImcStmt> visArg) {
        // Canonize the address
        return new ImcMEM(mem.addr.accept(this, visArg));
    }

    @Override
    public ImcExpr visit(ImcNAME name, Vector<ImcStmt> visArg) {
        return new ImcNAME(name.label);
    }

    @Override
    public ImcExpr visit(ImcSEXPR sExpr, Vector<ImcStmt> visArg) {
        // Canonize the stmt
        visArg.addAll(sExpr.stmt.accept(new StmtCanonizer(), null));
        return sExpr.expr.accept(this, visArg);
    }

    @Override
    public ImcExpr visit(ImcTEMP temp, Vector<ImcStmt> visArg) {
        return new ImcTEMP(temp.temp);
    }

    @Override
    public ImcExpr visit(ImcUNOP unOp, Vector<ImcStmt> visArg) {
        return new ImcUNOP(unOp.oper, unOp.subExpr.accept(this, visArg));
    }
}

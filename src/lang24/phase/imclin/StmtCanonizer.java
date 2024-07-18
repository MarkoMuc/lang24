package lang24.phase.imclin;

import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemTemp;

import java.util.List;
import java.util.Vector;

public class StmtCanonizer implements ImcVisitor<Vector<ImcStmt>, Object> {

    @Override
    public Vector<ImcStmt> visit(ImcCJUMP cjump, Object visArg) {
        Vector<ImcStmt> stmts = new Vector<>();

        stmts.add(new ImcCJUMP(cjump.cond.accept(new ExprCanonizer(), stmts), cjump.posLabel, cjump.negLabel));

        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcESTMT eStmt, Object visArg) {
        Vector<ImcStmt> stmts = new Vector<>();
        stmts.add(new ImcESTMT(eStmt.expr.accept(new ExprCanonizer(), stmts)));

        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcJUMP jump, Object visArg) {
        return new Vector<>(List.of(new ImcJUMP(jump.label)));
    }

    @Override
    public Vector<ImcStmt> visit(ImcLABEL label, Object visArg) {
        return new Vector<>(List.of(new ImcLABEL(label.label)));
    }

    @Override
    public Vector<ImcStmt> visit(ImcMOVE move, Object visArg) {
        Vector<ImcStmt> stmts = new Vector<>();
        MemTemp dstTemp = new MemTemp();

        if (move.dst instanceof ImcMEM) {
            stmts.add(new ImcMOVE(
                    new ImcTEMP(dstTemp),
                    ((ImcMEM) move.dst.accept(new ExprCanonizer(), stmts)).addr
            ));

            stmts.add(new ImcMOVE(
                    new ImcMEM(new ImcTEMP(dstTemp)),
                    move.src.accept(new ExprCanonizer(), stmts)
            ));
        } else if (move.dst instanceof ImcTEMP) {
            stmts.add(new ImcMOVE(
                    move.dst.accept(new ExprCanonizer(), stmts),
                    move.src.accept(new ExprCanonizer(), stmts)
            ));
        }

        return stmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcSTMTS stmts, Object visArg) {
        Vector<ImcStmt> imcStmts = new Vector<>();
        for (ImcStmt stmt : stmts.stmts) {
            imcStmts.addAll(stmt.accept(this, null));
        }

        return imcStmts;
    }

    @Override
    public Vector<ImcStmt> visit(ImcVectStmt imcVectStmt, Object accArg) {
        // TODO: Vectorize code
        return imcVectStmt.stmts.accept(this, accArg);
    }
}

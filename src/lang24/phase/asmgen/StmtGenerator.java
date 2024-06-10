package lang24.phase.asmgen;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.AsmMOVE;
import lang24.data.asm.AsmOPER;
import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;

import java.util.Vector;

public class StmtGenerator implements ImcVisitor<Vector<AsmInstr>, Object> {

    private final ExprGenerator eg = new ExprGenerator();

    @Override
    public Vector<AsmInstr> visit(ImcCJUMP cjump, Object arg) {
        Vector<AsmInstr> v = new Vector<>();
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();

        uses.add(cjump.cond.accept(eg, v));
        jumps.add(cjump.posLabel);
        jumps.add(cjump.negLabel);

        String instr = "bnez `s0, " + cjump.posLabel.name;
        v.add(new AsmOPER(instr, uses, defs, jumps));

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcESTMT eStmt, Object arg) {
        Vector<AsmInstr> v = new Vector<>();
        eStmt.expr.accept(eg, v);

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcJUMP jump, Object arg) {
        Vector<AsmInstr> v = new Vector<>();
        Vector<MemTemp> uses = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemLabel> jumps = new Vector<>();

        jumps.add(jump.label);

        String instr = "j " + jump.label.name;
        v.add(new AsmOPER(instr, uses, defs, jumps));

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcLABEL label, Object arg) {
        Vector<AsmInstr> v = new Vector<>();
        v.add(new AsmLABEL(label.label));

        return v;
    }

    //TODO:here
    @Override
    public Vector<AsmInstr> visit(ImcMOVE move, Object arg) {
        Vector<AsmInstr> v = new Vector<>();
        Vector<MemTemp> defs = new Vector<>();
        Vector<MemTemp> uses = new Vector<>();

        MemTemp srcTemp = move.src.accept(eg, v);
        uses.add(srcTemp);

        if (move.dst instanceof ImcMEM mem) {
            Vector<MemLabel> jumps = new Vector<>();
            MemTemp dstTemp = mem.addr.accept(eg, v);

            defs.add(dstTemp);

            v.add(new AsmOPER("sd `s0, 0(`d0)", uses, defs, jumps));
        } else {
            defs.add(move.dst.accept(eg, v));
            v.add(new AsmMOVE("mv `d0, `s0", uses, defs));
        }

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcSTMTS stmts, Object arg) {
        throw new Report.InternalError();
    }

}
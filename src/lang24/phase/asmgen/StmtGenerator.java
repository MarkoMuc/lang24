package lang24.phase.asmgen;

import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.*;
import lang24.data.mem.*;
import lang24.data.asm.*;
import lang24.common.report.*;

import java.util.*;

public class StmtGenerator implements ImcVisitor<Vector<AsmInstr>, Object> {

    private ExprGenerator eg = new ExprGenerator();

    @Override
    public Vector<AsmInstr> visit(ImcCJUMP cjump, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        uses.add(cjump.cond.accept(eg, v));
        jumps.add(cjump.posLabel);
        jumps.add(cjump.negLabel);

        String instr = "bnez `s0, " + cjump.posLabel.name;
        v.add(new AsmOPER(instr, uses, defs, jumps));

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcESTMT eStmt, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();
        eStmt.expr.accept(eg, v);

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcJUMP jump, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        jumps.add(jump.label);

        String instr = "j " + jump.label.name;
        v.add(new AsmOPER(instr, uses, defs, jumps));

        return v;
    }

    @Override
    public Vector<AsmInstr> visit(ImcLABEL label, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();
        v.add(new AsmLABEL(label.label));

        return v;
    }

    //TODO:here
    @Override
    public Vector<AsmInstr> visit(ImcMOVE move, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemTemp> uses = new Vector<MemTemp>();

        MemTemp srcTemp = move.src.accept(eg, v);
        uses.add(srcTemp);

        if (move.dst instanceof ImcMEM mem) { // write
            //CHECKME: do you need to check if label or reg here?
            Vector<MemLabel> jumps = new Vector<MemLabel>();
            MemTemp dstTemp = mem.addr.accept(eg, v);

            defs.add(dstTemp);

            // instr = "la `d0, `s1";
            // defs.add(regTemp); // d0
            // //uses.clear();
            // uses.add(dstTemp); // s0
            // v.add(new AsmOPER(instr, uses, defs, jumps));

            //uses.clear();
            //uses.add(srcTemp);// s0

            v.add(new AsmOPER("sd `s0, 0(`d0)", uses, defs, jumps));
        } else { // read
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
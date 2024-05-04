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

    //TODO:here
    @Override
    public Vector<AsmInstr> visit(ImcCJUMP cjump, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();

        String instr = "	BNZ	`s0," + cjump.posLabel.name;
        Vector<MemTemp> uses = new Vector<MemTemp>();
        uses.add(cjump.cond.accept(eg, v));
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        jumps.add(cjump.posLabel);
        jumps.add(cjump.negLabel);

        v.add(new AsmOPER(instr, uses, defs, jumps));
        return v;
    }

    //TODO:here
    @Override
    public Vector<AsmInstr> visit(ImcESTMT eStmt, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();

        eStmt.expr.accept(eg, v);

        return v;
    }

    //TODO:here
    @Override
    public Vector<AsmInstr> visit(ImcJUMP jump, Object arg) {
        Vector<AsmInstr> v = new Vector<AsmInstr>();

        String instr = "	JMP	" + jump.label.name;
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        jumps.add(jump.label);

        v.add(new AsmOPER(instr, uses, defs, jumps));
        return v;
    }

    //TODO:here
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

        String instr = null;
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemTemp> uses = new Vector<MemTemp>();
        uses.add(move.src.accept(eg, v));
        if (move.dst instanceof ImcMEM) { // write
            instr = "	STO	`s0,`s1,0";
            uses.add(((ImcMEM) move.dst).addr.accept(eg, v));
            Vector<MemLabel> jumps = new Vector<MemLabel>();
            v.add(new AsmOPER(instr, uses, defs, jumps));
        } else { // read
            instr = "	SET	`d0,`s0";
            defs.add(move.dst.accept(eg, v));
            v.add(new AsmMOVE(instr, uses, defs));
        }

        return v;
    }

    //TODO:here
    @Override
    public Vector<AsmInstr> visit(ImcSTMTS stmts, Object arg) {
        // imclin removed this
        throw new Report.InternalError();
    }

}
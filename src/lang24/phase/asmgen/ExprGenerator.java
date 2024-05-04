package lang24.phase.asmgen;

import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.*;
import lang24.data.imc.code.expr.*;
import lang24.data.asm.*;
import lang24.common.report.*;
import java.util.*;

public class ExprGenerator implements ImcVisitor<MemTemp, Vector<AsmInstr>> {
    @Override
    public MemTemp visit(ImcBINOP binOp, Vector<AsmInstr> arg) {
        MemTemp reg = new MemTemp();
        MemTemp fst = binOp.fstExpr.accept(this, arg);
        MemTemp snd = binOp.sndExpr.accept(this, arg);

        String instr = null;
        Vector<MemTemp> uses = new Vector<MemTemp>();
        uses.add(fst);
        uses.add(snd);

        Vector<MemTemp> defs = new Vector<MemTemp>();
        defs.add(reg);
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        if (binOp.oper == ImcBINOP.Oper.OR) {
            instr = "	or	`d0,`s0,`s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.AND) {
            instr = "	and	`d0,`s0,`s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.ADD) {
            instr = "	add	`d0,`s0,`s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.SUB) {
            instr = "	sub	`d0,`s0,`s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.MUL) {
            instr = "	mul	`d0,`s0,`s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.DIV) {
            instr = "	div	`d0,`s0,`s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.MOD) {
            instr = "	remw `d0,`s0,`s1";
            arg.add(new AsmOPER(instr, null, defs, jumps));
        } else if (binOp.oper == ImcBINOP.Oper.EQU) {
            // FIXME: Create function for this etc.
            Vector<AsmInstr> instrs = new Vector<>();
            defs.add(new MemTemp());//d1
            uses.add(new MemTemp());//s0
            uses.add(new MemTemp());//s1

            instrs.add( new AsmOPER( "lw `d0,`s0", uses, defs, jumps));
            instrs.add( new AsmOPER( "mv `d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "lw `d0,`s1", uses, defs, jumps));

            instrs.add( new AsmOPER( "sext.w `d1,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d0,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "sub `d0,`d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "seqz `d0,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "andi `d0,`d0, 0xff", uses, defs, jumps));
            arg.addAll(instrs);
        } else if (binOp.oper == ImcBINOP.Oper.NEQ) {
            Vector<AsmInstr> instrs = new Vector<>();
            defs.add(new MemTemp());//d1
            uses.add(new MemTemp());//s0
            uses.add(new MemTemp());//s1

            instrs.add( new AsmOPER( "lw `d0,`s0", uses, defs, jumps));
            instrs.add( new AsmOPER( "mv `d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "lw `d0,`s1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d1,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "sub `d0,`d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "snez `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "andi `d0,`d0,`0xff", uses, defs, jumps));
            arg.addAll(instrs);
        } else if (binOp.oper == ImcBINOP.Oper.LTH) {
            Vector<AsmInstr> instrs = new Vector<>();
            defs.add(new MemTemp());//d1
            uses.add(new MemTemp());//s0
            uses.add(new MemTemp());//s1

            instrs.add( new AsmOPER( "lw `d0,`s0", uses, defs, jumps));
            instrs.add( new AsmOPER( "mv `d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "lw `d0,`s1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d1,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "slt `d0,`d1,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "andi `d0,`d0,`0xff", uses, defs, jumps));

            arg.addAll(instrs);
        } else if (binOp.oper == ImcBINOP.Oper.GTH) {
            // CHECKME: if the SLT turned the right way
            //  sext.w but do we need sext.d?
            Vector<AsmInstr> instrs = new Vector<>();
            defs.add(new MemTemp());//d1
            uses.add(new MemTemp());//s0
            uses.add(new MemTemp());//s1

            instrs.add( new AsmOPER( "lw `d0,`s0", uses, defs, jumps));
            instrs.add( new AsmOPER( "mv `d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "lw `d0,`s1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d1,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "slt `d0,`d0,`d1", uses, defs, jumps));

            instrs.add( new AsmOPER( "andi `d0,`d0,`0xff", uses, defs, jumps));

            arg.addAll(instrs);
        } else if (binOp.oper == ImcBINOP.Oper.LEQ) {
            Vector<AsmInstr> instrs = new Vector<>();
            defs.add(new MemTemp());//d1
            uses.add(new MemTemp());//s0
            uses.add(new MemTemp());//s1

            instrs.add( new AsmOPER( "lw `d0,`s0", uses, defs, jumps));
            instrs.add( new AsmOPER( "mv `d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "lw `d0,`s1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d1,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "slt `d0,`d0,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "seqz `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "andi `d0,`d0,`0xff", uses, defs, jumps));

            arg.addAll(instrs);
        } else if (binOp.oper == ImcBINOP.Oper.GEQ) {
            Vector<AsmInstr> instrs = new Vector<>();
            defs.add(new MemTemp());//d1
            uses.add(new MemTemp());//s0
            uses.add(new MemTemp());//s1

            instrs.add( new AsmOPER( "lw `d0,`s0", uses, defs, jumps));
            instrs.add( new AsmOPER( "mv `d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "lw `d0,`s1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d1,`d1", uses, defs, jumps));
            instrs.add( new AsmOPER( "sext.w `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "slt `d0,`d1,`d0", uses, defs, jumps));
            instrs.add( new AsmOPER( "seqz `d0,`d0", uses, defs, jumps));

            instrs.add( new AsmOPER( "andi `d0,`d0,`0xff", uses, defs, jumps));

            arg.addAll(instrs);
        } else {
            throw new Report.InternalError();
        }
        return reg;
    }

    private void storeArgument(MemTemp funArg, long offset, Vector<AsmInstr> arg) {
        // arg size is always 8 bytes (BOOL | CHAR | INT | PTR)
        // STO value, x2, offset
        String instr = null;
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        uses.add(funArg);

        if (offset <= 0xff) {
            instr = "sd	`s0,"+offset+"(sp)";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else {
            //CHECKME: find out when you should calculate it with extra commands
            instr = "mv `d0, sp";
            MemTemp dest = new MemTemp();
            uses.clear();
            defs.add(dest);
            arg.add(new AsmOPER(instr, uses, defs, jumps));

            instr = "add `d0, `s0";
            MemTemp regz = (new ImcCONST(offset)).accept(this, arg);
            uses.add(regz);
            arg.add(new AsmOPER(instr, uses, defs, jumps));

            instr = "sd `s0,0(`d0)";
            uses.add(funArg);
            defs.add(dest);
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        }
    }

    @Override
    public MemTemp visit(ImcCALL call, Vector<AsmInstr> arg) {
        // x2 - $254 - SP
        // x3 - $253 - FP
        MemTemp reg = new MemTemp();
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        // set args
        for (int i = 0; i < call.args.size(); i++) {
            MemTemp funArg = call.args.get(i).accept(this, arg);
            long offset = call.offs.get(i);
            storeArgument(funArg, offset, arg);
        }
        jumps.add(call.label);

        String instr = "call " + call.label.name;
        arg.add(new AsmOPER(instr, uses, defs, jumps));

        // retval
        instr = "lw	`d0,0(sp)";
        uses = new Vector<MemTemp>();
        defs = new Vector<MemTemp>();
        jumps = new Vector<MemLabel>();

        defs.add(reg);
        arg.add(new AsmOPER(instr, uses, defs, jumps));

        return reg;
    }

    @Override
    public MemTemp visit(ImcCONST constant, Vector<AsmInstr> arg) {
        MemTemp reg = new MemTemp();
        String instr = null;
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        defs.add(reg);
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        instr = "li `d0," + constant.value;
        uses.add(reg);
        arg.add(new AsmOPER(instr, uses, defs, jumps));

        return reg;
    }

    @Override
    public MemTemp visit(ImcMEM mem, Vector<AsmInstr> arg) {
        MemTemp reg = new MemTemp();
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        Vector<MemTemp> defs = new Vector<MemTemp>();

        uses.add(mem.addr.accept(this, arg));
        defs.add(reg);

        String instr = "lw  `d0,`s0";
        arg.add(new AsmOPER(instr, uses, defs, jumps));
        return reg;
    }

    @Override
    public MemTemp visit(ImcNAME name, Vector<AsmInstr> arg) {
        MemTemp reg = new MemTemp();
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        defs.add(reg);

        String instr = "la  `d0," + name.label.name;
        arg.add(new AsmOPER(instr, uses, defs, jumps));
        return reg;
    }

    //CHECKME: Checkme
    @Override
    public MemTemp visit(ImcSEXPR sExpr, Vector<AsmInstr> arg) {
        // imclin removed this
        throw new Report.InternalError();
    }

    @Override
    public MemTemp visit(ImcTEMP temp, Vector<AsmInstr> arg) {
        return temp.temp;
    }

    @Override
    public MemTemp visit(ImcUNOP unOp, Vector<AsmInstr> arg) {
        MemTemp reg = new MemTemp();
        MemTemp sub = unOp.subExpr.accept(this, arg);
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        String instr = null;
        uses.add(sub);
        defs.add(reg);

        if (unOp.oper == ImcUNOP.Oper.NOT) {
            instr = "not    `d0,`s0";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (unOp.oper == ImcUNOP.Oper.NEG) {
            instr = "negw   `d0,`s0";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        }
        return reg;
    }
}

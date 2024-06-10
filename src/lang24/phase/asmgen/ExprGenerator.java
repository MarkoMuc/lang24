package lang24.phase.asmgen;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmOPER;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;

import java.util.Vector;

public class ExprGenerator implements ImcVisitor<MemTemp, Vector<AsmInstr>> {
    @Override
    public MemTemp visit(ImcBINOP binOp, Vector<AsmInstr> arg) {
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();

        MemTemp reg = new MemTemp();
        MemTemp fst = binOp.fstExpr.accept(this, arg);
        MemTemp snd = binOp.sndExpr.accept(this, arg);

        uses.add(fst);
        uses.add(snd);
        defs.add(reg);

        switch (binOp.oper) {
            case OR -> arg.add(new AsmOPER("or `d0, `s0, `s1", uses, defs, jumps));
            case AND -> arg.add(new AsmOPER("and `d0, `s0, `s1", uses, defs, jumps));
            case ADD -> arg.add(new AsmOPER("add `d0, `s0, `s1", uses, defs, jumps));
            case SUB -> arg.add(new AsmOPER("sub `d0, `s0, `s1", uses, defs, jumps));
            case MUL -> arg.add(new AsmOPER("mul `d0, `s0, `s1", uses, defs, jumps));
            case DIV -> arg.add(new AsmOPER("div `d0, `s0, `s1", uses, defs, jumps));
            // CHECKME: Does this only mod a word size?
            //          Do you need to use remw? whats the difference?
            case MOD -> arg.add(new AsmOPER("rem `d0, `s0, `s1", uses, defs, jumps));
            case EQU -> {
                // FIXME: is this again both use and def?
                arg.add(new AsmOPER("sub `d0, `s0, `s1", uses, defs, jumps));

                uses = new Vector<>();
                uses.add(reg);

                arg.add(new AsmOPER("seqz `d0, `d0", uses, defs, jumps));
            }
            case NEQ -> {
                arg.add(new AsmOPER("sub `d0, `s0, `s1", uses, defs, jumps));

                uses = new Vector<>();
                uses.add(reg);

                arg.add(new AsmOPER("snez `d0, `d0", uses, defs, jumps));
            }
            case LTH -> {
                arg.add(new AsmOPER("slt `d0, `s0, `s1", uses, defs, jumps));
            }
            case GTH -> {
                // CHECKME: Is sext even needed since we use all 64 bits?
                arg.add(new AsmOPER("slt `d0, `s1, `s0", uses, defs, jumps));
            }
            case LEQ -> {
                arg.add(new AsmOPER("slt `d0, `s1, `s0", uses, defs, jumps));

                uses = new Vector<>();
                uses.add(reg);

                arg.add(new AsmOPER("seqz `d0, `d0", uses, defs, jumps));
            }
            case GEQ -> {
                arg.add(new AsmOPER("slt `d0, `s0, `s1", uses, defs, jumps));

                uses = new Vector<>();
                uses.add(reg);

                arg.add(new AsmOPER("seqz `d0, `d0", uses, defs, jumps));
            }
            case null, default -> throw new Report.InternalError();
        }
        return reg;
    }

    private void storeArgument(MemTemp funArg, long offset, Vector<AsmInstr> arg) {
        // FIXME: change char and bool to 1 byte
        // arg size is always 8 bytes (BOOL | CHAR | INT | PTR)
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        String instr = null;

        if (offset <= 0xFF) {
            uses.add(funArg);

            instr = String.format("sd `s0, %d(sp)", offset);
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else {
            //CHECKME: find out when you should calculate it with extra commands
            MemTemp dest = new MemTemp();

            defs.add(dest);

            instr = "mv `d0, sp";
            arg.add(new AsmOPER(instr, uses, defs, jumps));

            MemTemp offsetReg = (new ImcCONST(offset)).accept(this, arg);
            uses = new Vector<>();
            defs = new Vector<>();
            jumps = new Vector<>();

            uses.add(dest);
            uses.add(offsetReg);
            defs.add(dest);

            //Does this mean I dest temporary in both uses and defs?
            // Does the second one need to be d0?
            instr = "add `d0, `s0, `s1";
            arg.add(new AsmOPER(instr, uses, defs, jumps));

            uses = new Vector<>();
            defs = new Vector<>();
            jumps = new Vector<>();

            //Is this both uses only? is there any defs here?
            uses.add(funArg);
            defs.add(dest);

            instr = "sd `s0, 0(`d0)"; //d0 is hand calculated SP!
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        }
    }

    @Override
    public MemTemp visit(ImcCALL call, Vector<AsmInstr> arg) {
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        String instr = null;
        MemTemp reg = new MemTemp();

        int argsSize = call.args.size();
        for (int i = 0; i < argsSize; i++) {
            MemTemp funArg = call.args.get(i).accept(this, arg);
            long offset = call.offs.get(i);
            storeArgument(funArg, offset, arg);
        }

        jumps.add(call.label);

        instr = String.format("call %s", call.label.name);
        arg.add(new AsmOPER(instr, uses, defs, jumps));

        uses = new Vector<MemTemp>();
        defs = new Vector<MemTemp>();
        jumps = new Vector<MemLabel>();

        defs.add(reg);

        instr = "ld `d0, 0(sp)"; // Read return value
        arg.add(new AsmOPER(instr, uses, defs, jumps));

        return reg;
    }

    @Override
    public MemTemp visit(ImcCONST constant, Vector<AsmInstr> arg) {
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        String instr = null;
        MemTemp reg = new MemTemp();

        defs.add(reg);

        instr = String.format("li `d0, %s", constant.value);

        arg.add(new AsmOPER(instr, uses, defs, jumps));

        return reg;
    }

    @Override
    public MemTemp visit(ImcMEM mem, Vector<AsmInstr> arg) {
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        MemTemp reg = new MemTemp();

        defs.add(reg);
        uses.add(mem.addr.accept(this, arg));

        arg.add(new AsmOPER("ld `d0, 0(`s0)", uses, defs, jumps));

        return reg;
    }

    @Override
    public MemTemp visit(ImcNAME name, Vector<AsmInstr> arg) {
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        MemTemp reg = new MemTemp();

        defs.add(reg);
        //String instr = String.format("lui `d0, %hi(%s)", name.label.name);
        //arg.add(new AsmOper(instr, uses, defs, jumps))
        //instr = String.format("lw `d0, %lo(%s)(`d0)", name.label.name);
        //arg.add(new AsmOper(instr, uses, defs, jumps))
        String instr = String.format("la `d0, %s", name.label.name);

        arg.add(new AsmOPER(instr, uses, defs, jumps));

        return reg;
    }

    @Override
    public MemTemp visit(ImcTEMP temp, Vector<AsmInstr> arg) {
        return temp.temp;
    }

    @Override
    public MemTemp visit(ImcUNOP unOp, Vector<AsmInstr> arg) {
        Vector<MemTemp> uses = new Vector<MemTemp>();
        Vector<MemTemp> defs = new Vector<MemTemp>();
        Vector<MemLabel> jumps = new Vector<MemLabel>();
        String instr = null;

        MemTemp reg = new MemTemp();
        MemTemp sub = unOp.subExpr.accept(this, arg);

        uses.add(sub);
        defs.add(reg);

        if (unOp.oper == ImcUNOP.Oper.NOT) {
            instr = "not `d0, `s0";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        } else if (unOp.oper == ImcUNOP.Oper.NEG) {
            instr = "negw `d0, `s0";
            arg.add(new AsmOPER(instr, uses, defs, jumps));
        }

        return reg;
    }

    @Override
    public MemTemp visit(ImcSEXPR sExpr, Vector<AsmInstr> arg) {
        throw new Report.InternalError();
    }
}

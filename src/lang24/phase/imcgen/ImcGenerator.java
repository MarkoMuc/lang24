package lang24.phase.imcgen;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.mem.*;
import lang24.data.type.SemArrayType;
import lang24.data.type.SemCharType;
import lang24.data.type.SemRecordType;
import lang24.data.type.SemType;
import lang24.phase.memory.MemEvaluator;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;

import java.util.Stack;
import java.util.Vector;

/*
    TODO:
     -> Check depths
     -> Check offsets
     -> Check SEXPR and ESTMT
     -> Check Comp expr
     -> Check Cast
 */

public class ImcGenerator implements AstFullVisitor<Object, Stack<MemFrame>> {

    Stack<ImcGenerator.FuncContext> funcContexts = new Stack<>();

    @Override
    public Object visit(AstFunDefn funDefn, Stack<MemFrame> arg) {
        if(arg == null){
            arg = new Stack<>();
        }
        MemLabel entryL = new MemLabel();
        MemLabel exitL = new MemLabel();

        FuncContext funcContext = new FuncContext(entryL, exitL);
        funcContexts.push(funcContext);

        MemFrame frame = Memory.frames.get(funDefn);
        arg.push(frame);

        ImcGen.entryLabel.put(funDefn, entryL);
        ImcGen.exitLabel.put(funDefn, exitL);

        if(funDefn.defns != null){
            funDefn.defns.accept(this, arg);
        }

        if(funDefn.stmt != null){
            funDefn.stmt.accept(this, arg);
        }

        funcContexts.pop();
        arg.pop();

        return null;
    }

    // EX1, EX2, EX3
    @Override
    public Object visit(AstAtomExpr atomExpr, Stack<MemFrame> arg) {
        //TODO: bounds check for int -> throw error if too large
        ImcExpr imcConst = switch (atomExpr.type){
            case BOOL -> new ImcCONST(atomExpr.value.equals("true") ? 1 : 0);
            case VOID, PTR -> new ImcCONST(0);
            case INT -> new ImcCONST(Long.parseLong(atomExpr.value));
            case CHAR -> new ImcCONST(createChar(atomExpr.value));
            case STR -> new ImcNAME(Memory.strings.get(atomExpr).label);
        };
        ImcGen.exprImc.put(atomExpr, imcConst);
        return imcConst;
    }

    // EX4, EX6
    @Override
    public Object visit(AstPfxExpr pfxExpr, Stack<MemFrame> arg) {
        ImcExpr imcPrefix = switch (pfxExpr.oper){
            case NOT -> new ImcUNOP(ImcUNOP.Oper.NOT, (ImcExpr) pfxExpr.expr.accept(this, arg));
            case SUB -> new ImcUNOP(ImcUNOP.Oper.NEG, (ImcExpr) pfxExpr.expr.accept(this, arg));
            case ADD -> ((ImcExpr) pfxExpr.expr.accept(this, arg));
            case PTR -> ((ImcMEM) pfxExpr.expr.accept(this, arg)).addr;
        };

        ImcGen.exprImc.put(pfxExpr, imcPrefix);
        return imcPrefix;
    }

    // EX5
    @Override
    public Object visit(AstBinExpr binExpr, Stack<MemFrame> arg) {
        ImcExpr expr1 = (ImcExpr) binExpr.fstExpr.accept(this, arg);
        ImcExpr expr2 = (ImcExpr) binExpr.sndExpr.accept(this, arg);

        ImcExpr imcBin = switch (binExpr.oper){
            case ADD -> new ImcBINOP(ImcBINOP.Oper.ADD, expr1, expr2);
            case AND -> new ImcBINOP(ImcBINOP.Oper.AND, expr1, expr2);
            case DIV -> new ImcBINOP(ImcBINOP.Oper.DIV, expr1, expr2);
            case EQU -> new ImcBINOP(ImcBINOP.Oper.EQU, expr1, expr2);
            case GEQ -> new ImcBINOP(ImcBINOP.Oper.GEQ, expr1, expr2);
            case GTH -> new ImcBINOP(ImcBINOP.Oper.GTH, expr1, expr2);
            case LEQ -> new ImcBINOP(ImcBINOP.Oper.LEQ, expr1, expr2);
            case LTH -> new ImcBINOP(ImcBINOP.Oper.LTH, expr1, expr2);
            case MOD -> new ImcBINOP(ImcBINOP.Oper.MOD, expr1, expr2);
            case MUL -> new ImcBINOP(ImcBINOP.Oper.MUL, expr1, expr2);
            case NEQ -> new ImcBINOP(ImcBINOP.Oper.NEQ, expr1, expr2);
            case OR -> new ImcBINOP(ImcBINOP.Oper.OR, expr1, expr2);
            case SUB -> new ImcBINOP(ImcBINOP.Oper.SUB, expr1, expr2);
        };

        ImcGen.exprImc.put(binExpr, imcBin);

        return imcBin;
    }

    // EX6
    @Override
    public Object visit(AstSfxExpr sfxExpr, Stack<MemFrame> arg) {
        ImcExpr imcSuffix = new ImcMEM((ImcExpr) sfxExpr.expr.accept(this, arg));
        ImcGen.exprImc.put(sfxExpr, imcSuffix);

        return imcSuffix;
    }

    // EX7
    @Override
    public Object visit(AstNameExpr nameExpr, Stack<MemFrame> arg) {
        AstDefn astDefn = SemAn.definedAt.get(nameExpr);
        ImcExpr imcExpr = null;

        MemAccess memAccess;
        if(astDefn instanceof AstVarDefn astVarDefn){
            memAccess = Memory.varAccesses.get(astVarDefn);
        } else if(astDefn instanceof AstFunDefn.AstParDefn parDefn){
            memAccess = Memory.parAccesses.get(parDefn);
        }else{
            return null;
        }

        if(memAccess instanceof MemAbsAccess){
            imcExpr = new ImcMEM(new ImcNAME(((MemAbsAccess) memAccess).label));
        } else{
            MemRelAccess local = (MemRelAccess) memAccess;
            ImcExpr expr = new ImcTEMP(arg.peek().FP);

            long depth = arg.peek().depth - local.depth;
            ImcCONST imcCONST = new ImcCONST(local.offset);

            for(int i = 0; i < depth; i++){
                expr = new ImcMEM(expr);
            }

            ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, expr, imcCONST);
            imcExpr = new ImcMEM(imcBin);
            if(astDefn instanceof AstFunDefn.AstRefParDefn){
                imcExpr = new ImcMEM(imcExpr);
            }
        }

        ImcGen.exprImc.put(nameExpr, imcExpr);

        return imcExpr;
    }

    // EX8
    @Override
    public Object visit(AstArrExpr arrExpr, Stack<MemFrame> arg) {
        ImcExpr arr = (ImcExpr) arrExpr.arr.accept(this, arg);
        ImcExpr idx = (ImcExpr) arrExpr.idx.accept(this, arg);

        SemArrayType semType = (SemArrayType) SemAn.ofType.get(arrExpr.arr).actualType();

        ImcBINOP imcOffset = new ImcBINOP(ImcBINOP.Oper.MUL, idx, new ImcCONST(semType.size));
        ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, arr, imcOffset);

        ImcMEM imcMEM = new ImcMEM(imcBin);

        ImcGen.exprImc.put(arrExpr, imcMEM);
        return imcMEM;
    }

    // EX9
    @Override
    public Object visit(AstCmpExpr cmpExpr, Stack<MemFrame> arg) {
        // FIXME: Check union vs struct and AstCmpDefn
        ImcMEM imcMEM = (ImcMEM) cmpExpr.expr.accept(this, arg);

        AstRecType.AstCmpDefn cmpDefn = (AstRecType.AstCmpDefn) SemAn.definedAt.get(cmpExpr.expr);
        MemRelAccess memRelAccess = (MemRelAccess) Memory.cmpAccesses.get(cmpDefn);

        ImcBINOP imcBINOP = new ImcBINOP(ImcBINOP.Oper.ADD, imcMEM.addr, new ImcCONST(memRelAccess.offset));
        ImcMEM imc = new ImcMEM(imcBINOP);

        ImcGen.exprImc.put(cmpExpr, imc);

        return imcMEM;
    }

    // EX10
    @Override
    public Object visit(AstCallExpr callExpr, Stack<MemFrame> arg) {
        // FIXME: Are the offsets correct?
        AstFunDefn funDefn = (AstFunDefn) SemAn.definedAt.get(callExpr);
        MemFrame memFrame = Memory.frames.get(funDefn);

        ImcExpr imcExpr1 = new ImcTEMP(arg.peek().FP);
        for(int i = 0; i < arg.peek().depth - memFrame.depth; i++){
            imcExpr1 = new ImcMEM(imcExpr1);
        }

        if(memFrame.depth == 0){
            imcExpr1 = new ImcCONST(0);
        }

        Vector<Long> offsets = new Vector<>();
        Vector<ImcExpr> args = new Vector<>();

        offsets.add(0L);
        args.add(imcExpr1);

        if(callExpr.args != null) {
            for (int i = 0; i < callExpr.args.size(); i++) {
                AstFunDefn.AstParDefn parDefn = funDefn.pars.get(i);
                MemRelAccess memRelAccess = (MemRelAccess) Memory.parAccesses.get(parDefn);
                offsets.add(memRelAccess.offset);
                ImcExpr expr = (ImcExpr) callExpr.args.get(i).accept(this, arg);
                if (parDefn instanceof AstFunDefn.AstRefParDefn && expr instanceof ImcMEM refPar) {
                    expr = refPar.addr;
                }
                args.add(expr);
            }
        }

        ImcCALL imcCALL = new ImcCALL(memFrame.label, offsets, args);
        ImcGen.exprImc.put(callExpr, imcCALL);

        return imcCALL;
    }

    // EX12, EX13
    @Override
    public Object visit(AstCastExpr castExpr, Stack<MemFrame> arg) {
        SemType semType = SemAn.isType.get(castExpr.type).actualType();
        ImcExpr imcExpr = (ImcExpr) castExpr.expr.accept(this, arg);
        ImcExpr imcCast;

        if(semType instanceof SemCharType){
            imcCast = new ImcBINOP(ImcBINOP.Oper.MOD, imcExpr, new ImcCONST(256));
        }else {
            imcCast = imcExpr;
        }

        ImcGen.exprImc.put(castExpr,imcCast);

        return imcCast;
    }

    @Override
    public Object visit(AstSizeofExpr sizeofExpr, Stack<MemFrame> arg) {
        SemType semType = SemAn.isType.get(sizeofExpr.type);
        ImcCONST imcCONST = new ImcCONST(MemEvaluator.SizeOfType(semType));

        ImcGen.exprImc.put(sizeofExpr, imcCONST);

        return imcCONST;
    }

    // ST1
    @Override
    public Object visit(AstExprStmt exprStmt, Stack<MemFrame> arg) {
        boolean first = funcContexts.peek().first;
        funcContexts.peek().first = false;

        ImcExpr imcExpr = (ImcExpr) exprStmt.expr.accept(this, arg);
        ImcStmt imcESTMT = new ImcESTMT(imcExpr);

        if(first){
            imcESTMT = funcBody(imcESTMT);
        }

        ImcGen.stmtImc.put(exprStmt, imcESTMT);

        return imcESTMT;
    }

    // ST2
    @Override
    public Object visit(AstAssignStmt assignStmt, Stack<MemFrame> arg) {
        boolean first = funcContexts.peek().first;
        funcContexts.peek().first = false;

        ImcExpr expr1 = (ImcExpr) assignStmt.src.accept(this, arg);
        ImcExpr expr2 = (ImcExpr) assignStmt.dst.accept(this, arg);
        ImcStmt imcMOVE = new ImcMOVE(expr2, expr1);

        if(first){
            imcMOVE = funcBody(imcMOVE);
        }

        ImcGen.stmtImc.put(assignStmt, imcMOVE);

        return imcMOVE;
    }

    // ST3, ST4, ST5, ST6
    @Override
    public Object visit(AstIfStmt ifStmt, Stack<MemFrame> arg) {
        boolean first = funcContexts.peek().first;
        funcContexts.peek().first = false;

        ImcExpr imcExpr = (ImcExpr) ifStmt.cond.accept(this, arg);
        ImcStmt stmt = (ImcStmt) ifStmt.thenStmt.accept(this, arg);

        Vector<ImcStmt> imcStmts = new Vector<>();
        MemLabel thenL = new MemLabel();
        MemLabel elseL = new MemLabel();
        MemLabel endL = new MemLabel();

        imcStmts.add(new ImcCJUMP(imcExpr, thenL, elseL));
        imcStmts.add(new ImcLABEL(thenL));
        imcStmts.add(stmt);
        imcStmts.add(new ImcJUMP(endL));
        imcStmts.add(new ImcLABEL(elseL));

        if(ifStmt.elseStmt != null){
            imcStmts.add((ImcStmt) ifStmt.elseStmt.accept(this, arg));
        }
        imcStmts.add(new ImcLABEL(endL));

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);

        if(first){
            imcSTMTS = funcBody(imcSTMTS);
        }

        ImcGen.stmtImc.put(ifStmt, imcSTMTS);

        return imcSTMTS;
    }

    // ST7, ST8
    @Override
    public Object visit(AstWhileStmt whileStmt, Stack<MemFrame> arg) {
        boolean first = funcContexts.peek().first;
        funcContexts.peek().first = false;

        ImcExpr imcExpr = (ImcExpr) whileStmt.cond.accept(this, arg);
        ImcStmt imcStmt = (ImcStmt) whileStmt.stmt.accept(this, arg);

        MemLabel cond = new MemLabel();
        MemLabel body = new MemLabel();
        MemLabel end = new MemLabel();

        Vector<ImcStmt> imcStmts = new Vector<>();

        imcStmts.add(new ImcLABEL(cond));
        imcStmts.add(new ImcCJUMP(imcExpr,body,end));

        imcStmts.add(new ImcLABEL(body));
        imcStmts.add(imcStmt);

        imcStmts.add(new ImcJUMP(cond));
        imcStmts.add(new ImcLABEL(end));

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);

        if(first){
            imcSTMTS = funcBody(imcSTMTS);
        }
        ImcGen.stmtImc.put(whileStmt, imcSTMTS);

        return imcSTMTS;
    }

    // ST9
    @Override
    public Object visit(AstBlockStmt blockStmt, Stack<MemFrame> arg) {
        boolean first = funcContexts.peek().first;
        funcContexts.peek().first = false;

        Vector<ImcStmt> imcStmts = new Vector<>();

        for (int i = 0; i < blockStmt.stmts.size(); i++) {
            ImcStmt midStmt = (ImcStmt) blockStmt.stmts.get(i).accept(this, arg);
            imcStmts.add(midStmt);
            if(blockStmt.stmts.get(i) instanceof AstReturnStmt &&
                    !(i == blockStmt.stmts.size() - 1 && first)){
                // Adds early return
                imcStmts.add(new ImcJUMP(funcContexts.peek().exitL));
            }
        }

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);

        if(first){
            imcSTMTS = funcBody(imcSTMTS);
        }

        ImcGen.stmtImc.put(blockStmt, imcSTMTS);

        return imcSTMTS;
    }

    @Override
    public Object visit(AstReturnStmt retStmt, Stack<MemFrame> arg) {
        boolean first = funcContexts.peek().first;
        funcContexts.peek().first = false;

        ImcStmt imcMOVE = new ImcMOVE(new ImcTEMP(arg.peek().RV), (ImcExpr) retStmt.expr.accept(this, arg));

        if(first){
            imcMOVE = funcBody(imcMOVE);
        }

        ImcGen.stmtImc.put(retStmt, imcMOVE);

        return imcMOVE;
    }

    private int createChar(String value){
        int c = -1;

        //Remove ' '
        int fst = value.indexOf('\'');
        int lst = value.lastIndexOf('\'') == -1 ? value.length() : value.lastIndexOf('\'');
        value = value.substring(fst + 1, lst);

        if(value.length() == 1) { // Simple char
            c = value.charAt(0);
        }else if(value.length() == 2){
            value = value.substring(value.indexOf("\\") + 1);
            c = value.indexOf('n') == -1 ? value.charAt(0) : 0x0A;
        } else if(value.length() == 3){ // Escape sequence
            c = Integer.parseInt(value.substring(value.indexOf("\\") + 1), 16);
        } else {
            throw new Report.Error("Not a valid char" + value);
        }

        return c;
    }

    private ImcStmt funcBody(ImcStmt mainStmt){
        if(mainStmt instanceof ImcSTMTS mainSTMTS){
            mainSTMTS.stmts.addFirst(new ImcLABEL(funcContexts.peek().entryL));
            // CHECKME: Pretty sure this doesn't need to be added, we just need a label
            //mainSTMTS.stmts.addLast(new ImcJUMP(funcContexts.peek().exitL));
            mainSTMTS.stmts.addLast(new ImcLABEL(funcContexts.peek().exitL));
        } else {
            Vector<ImcStmt> stmtVector = new Vector<>();
            stmtVector.add(new ImcLABEL(funcContexts.peek().entryL));
            stmtVector.add(mainStmt);
            // CHECKME: Pretty sure this doesn't need to be added, we just need a label
            //stmtVector.add(new ImcJUMP(funcContexts.peek().exitL));

            stmtVector.add(new ImcLABEL(funcContexts.peek().exitL));

            mainStmt = new ImcSTMTS(stmtVector);
        }

        funcContexts.peek().first = false;

        return mainStmt;
    }

    private class FuncContext{
        boolean first;
        MemLabel entryL;
        MemLabel exitL;

        FuncContext(MemLabel entryL, MemLabel exitL){
            this.first = true;
            this.entryL = entryL;
            this.exitL = exitL;
        }
    }

}
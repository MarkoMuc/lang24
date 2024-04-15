package lang24.phase.imcgen;

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
import lang24.data.type.SemType;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;

import java.util.Stack;
import java.util.Vector;

/*
    TODO:
     -> Read how instructions are written down
     -> Check each rule
     -> Check sizeof
     -> Check depths
     -> Check IFs
 */

public class ImcGenerator implements AstFullVisitor<Object, Stack<MemFrame>> {

    @Override
    public Object visit(AstFunDefn funDefn, Stack<MemFrame> arg) {
        if(arg == null){
            arg = new Stack<>();
        }

        MemFrame frame = Memory.frames.get(funDefn);
        arg.push(frame);

        if(funDefn.stmt != null){
            funDefn.stmt.accept(this, arg);
        }
        if(funDefn.defns != null){
            funDefn.defns.accept(this, arg);
        }
        arg.pop();

        return null;
    }

    // EX1, EX2, EX3
    @Override
    public Object visit(AstAtomExpr atomExpr, Stack<MemFrame> arg) {
        //TODO: bounds check for int?
        //FIXME:
        //  - Char
        //      -> remove the ' '  and then turn to char!
        //      -> char can be hex, normal char, or special char
        ImcExpr imcConst = switch (atomExpr.type){
            case BOOL -> new ImcCONST(atomExpr.value.equals("true") ? 1 : 0);
            case VOID, PTR -> new ImcCONST(0);
            case INT -> new ImcCONST(Long.parseLong(atomExpr.value));
            case CHAR -> new ImcCONST(atomExpr.value.charAt(atomExpr.value.length() - 2));
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
        if(astDefn instanceof AstFunDefn funDefn){
            MemFrame memFrame = Memory.frames.get(funDefn);
            ImcExpr imcExpr1 = new ImcTEMP(arg.peek().FP);

            for (int i = 0; i < arg.peek().depth - memFrame.depth + 1; i++) {
                imcExpr1 = new ImcMEM(imcExpr1);
            }
            if(memFrame.depth == 0){
                imcExpr1 = new ImcCONST(0);
            }

            Vector<Long> offsets = new Vector<>();
            Vector<ImcExpr> args = new Vector<>();
            offsets.add(0L);
            args.add(imcExpr1);

            ImcCALL imc = new ImcCALL(memFrame.label, offsets, args);
            ImcGen.exprImc.put(nameExpr, imc);

            return imc;
        }
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

            long depth = arg.peek().depth - local.depth + 1;
            ImcCONST imcCONST = new ImcCONST(local.offset);

            for(int i = 0; i < depth; i++){
                expr = new ImcMEM(expr);
            }

            ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, expr, imcCONST);
            imcExpr = new ImcMEM(imcBin);
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

        AstRecType.AstCmpDefn cmpDefn = (AstRecType.AstCmpDefn) SemAn.definedAt.get(cmpExpr);
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
        for(int i = 0; i < arg.peek().depth - memFrame.depth + 1; i++){
            imcExpr1 = new ImcMEM(imcExpr1);
        }

        if(memFrame.depth == 0){
            imcExpr1 = new ImcCONST(0);
        }

        Vector<Long> offsets = new Vector<>();
        Vector<ImcExpr> args = new Vector<>();

        offsets.add(0L);
        args.add(imcExpr1);

        for(int i = 0; i < callExpr.args.size(); i++){
            AstFunDefn.AstParDefn parDefn = funDefn.pars.get(i);
            MemRelAccess memRelAccess = (MemRelAccess) Memory.parAccesses.get(parDefn);
            offsets.add(memRelAccess.offset);
            args.add((ImcExpr) callExpr.args.get(i).accept(this, arg));
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

    // ST1
    @Override
    public Object visit(AstExprStmt exprStmt, Stack<MemFrame> arg) {
        ImcExpr imcExpr = (ImcExpr) exprStmt.expr.accept(this, arg);
        ImcESTMT imcESTMT = new ImcESTMT(imcExpr);
        ImcGen.stmtImc.put(exprStmt, imcESTMT);

        return imcESTMT;
    }

    // ST2
    @Override
    public Object visit(AstAssignStmt assignStmt, Stack<MemFrame> arg) {
        ImcExpr expr1 = (ImcExpr) assignStmt.src.accept(this, arg);
        ImcExpr expr2 = (ImcExpr) assignStmt.dst.accept(this, arg);
        ImcMOVE imcMOVE = new ImcMOVE(expr2, expr1);

        ImcGen.stmtImc.put(assignStmt, imcMOVE);

        return imcMOVE;
    }

    // ST3, ST4, ST5, ST6
    @Override
    public Object visit(AstIfStmt ifStmt, Stack<MemFrame> arg) {
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

        imcStmts.add(new ImcLABEL(endL));

        ImcSTMTS imcSTMTS = new ImcSTMTS(imcStmts);
        ImcGen.stmtImc.put(ifStmt, imcSTMTS);

        return imcSTMTS;
    }

    // ST7, ST8
    @Override
    public Object visit(AstWhileStmt whileStmt, Stack<MemFrame> arg) {
        ImcExpr imcExpr = (ImcExpr) whileStmt.cond.accept(this, arg);
        ImcStmt imcStmt = (ImcStmt) whileStmt.stmt.accept(this, arg);

        MemLabel cond = new MemLabel();
        MemLabel body = new MemLabel();
        MemLabel end = new MemLabel();

        Vector<ImcStmt> imcStmts = new Vector<>();
        //Add condition label and expr
        imcStmts.add(new ImcLABEL(cond));
        imcStmts.add(new ImcCJUMP(imcExpr,body,end));
        //Add body label and stmt
        imcStmts.add(new ImcLABEL(body));
        imcStmts.add(imcStmt);
        //Check condition
        imcStmts.add(new ImcJUMP(cond));
        imcStmts.add(new ImcLABEL(end));

        ImcSTMTS imcSTMTS = new ImcSTMTS(imcStmts);
        ImcGen.stmtImc.put(whileStmt, imcSTMTS);

        return imcSTMTS;
    }

    // ST9
    @Override
    public Object visit(AstBlockStmt blockStmt, Stack<MemFrame> arg) {
        Vector<ImcStmt> imcStmts = new Vector<>();

        for (int i = 0; i < blockStmt.stmts.size() - 1; i++) {
            imcStmts.add((ImcStmt) blockStmt.stmts.get(i).accept(this, arg));
        }

        ImcStmt imcStmt = (ImcStmt) blockStmt.stmts.get(blockStmt.stmts.size() - 1).accept(this, arg);

        ImcExpr imcExpr;

        if(imcStmt instanceof ImcESTMT){
            imcExpr = new ImcSEXPR(new ImcSTMTS(imcStmts), ((ImcESTMT) imcStmt).expr);
        } else{
            imcStmts.add(imcStmt);
            imcExpr = new ImcSEXPR(new ImcSTMTS(imcStmts), new ImcCONST(0));
        }

        ImcMOVE imcMOVE = new ImcMOVE(new ImcTEMP(arg.peek().RV), imcExpr);

        ImcGen.stmtImc.put(blockStmt, imcMOVE);

        return imcMOVE;
    }

    @Override
    public Object visit(AstReturnStmt retStmt, Stack<MemFrame> arg) {
        // FIXME: I think the last part of block stmt, is what should be here
        retStmt.expr.accept(this, arg);

        return null;
    }
}

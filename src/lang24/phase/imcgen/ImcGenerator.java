package lang24.phase.imcgen;

import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
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
import lang24.data.type.*;
import lang24.phase.memory.MemEvaluator;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;

import java.util.Stack;
import java.util.Vector;

public class ImcGenerator implements AstFullVisitor<Object, Stack<MemFrame>> {

    Stack<ImcGenerator.FuncContext> funcContexts = new Stack<>();
    boolean conditionalStatement = false;
    int ifStatementDepth = -1;
    boolean conditionalContainsReturn = false;

    @Override
    public Object visit(AstFunDefn funDefn, Stack<MemFrame> arg) {
        if (arg == null) {
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

        if (funDefn.defns != null) {
            funDefn.defns.accept(this, arg);
        }

        if (funDefn.stmt != null) {
            ImcStmt functionBodyStmt = (ImcStmt) funDefn.stmt.accept(this, arg);
            functionBodyStmt = functionBody(functionBodyStmt);
            ImcGen.stmtImc.put(funDefn.stmt, functionBodyStmt);

            if (!funcContext.hasReturnOrExit &&
                    !(SemAn.ofType.get(funDefn).actualType() instanceof SemVoidType)) {
                String errorString = String.format("Missing return statement in non void function: %s", funDefn.name);
                throw new Report.Error(funDefn, errorString);
            }
        }

        funcContexts.pop();
        arg.pop();

        return null;
    }

    // EX1, EX2, EX3
    @Override
    public Object visit(AstAtomExpr atomExpr, Stack<MemFrame> arg) {
        ImcExpr imcConst = switch (atomExpr.type) {
            case BOOL -> new ImcCONST(atomExpr.value.equals("true") ? 1 : 0);
            case VOID -> new ImcCONST(-1);
            case PTR -> new ImcCONST(0);
            case INT -> new ImcCONST(checkAndParse(atomExpr.value));
            case CHAR -> new ImcCONST(createChar(atomExpr.value));
            case STR -> new ImcNAME(Memory.strings.get(atomExpr).label);
        };
        ImcGen.exprImc.put(atomExpr, imcConst);
        return imcConst;
    }

    // EX4, EX6
    @Override
    public Object visit(AstPfxExpr pfxExpr, Stack<MemFrame> arg) {
        ImcExpr imcPrefix = switch (pfxExpr.oper) {
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

        ImcExpr imcBin = switch (binExpr.oper) {
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
        if (astDefn instanceof AstVarDefn astVarDefn) {
            memAccess = Memory.varAccesses.get(astVarDefn);
        } else if (astDefn instanceof AstFunDefn.AstParDefn parDefn) {
            memAccess = Memory.parAccesses.get(parDefn);
        } else {
            return null;
        }

        if (memAccess instanceof MemAbsAccess) {
            imcExpr = new ImcMEM(new ImcNAME(((MemAbsAccess) memAccess).label));
        } else {
            MemRelAccess local = (MemRelAccess) memAccess;
            ImcExpr expr = new ImcTEMP(arg.peek().FP);

            long depth = arg.peek().depth - local.depth;
            ImcCONST imcCONST = new ImcCONST(local.offset);

            for (int i = 0; i < depth; i++) {
                expr = new ImcMEM(expr);
            }

            ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, expr, imcCONST);
            imcExpr = new ImcMEM(imcBin);
            if (astDefn instanceof AstFunDefn.AstRefParDefn) {
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

        SemType semType = SemAn.ofType.get(arrExpr.arr).actualType();

        ImcCONST typeSize = null;
        if (semType instanceof SemArrayType arrType) {
            typeSize = new ImcCONST(MemEvaluator.SizeOfType(arrType.elemType));
            if (arr instanceof ImcMEM mem) {
                arr = mem.addr;
            }
        } else if (semType instanceof SemPointerType ptrType) {
            typeSize = new ImcCONST(MemEvaluator.SizeOfType(ptrType.baseType));
        } else {
            throw new Report.Error(arrExpr, "This shouldn't happen.");
        }

        ImcBINOP imcOffset = new ImcBINOP(ImcBINOP.Oper.MUL, idx, typeSize);
        ImcBINOP imcBin = new ImcBINOP(ImcBINOP.Oper.ADD, arr, imcOffset);

        ImcMEM imcMEM = new ImcMEM(imcBin);

        ImcGen.exprImc.put(arrExpr, imcMEM);

        return imcMEM;
    }

    @Override
    public Object visit(AstMultiArrExpr multiArrExpr, Stack<MemFrame> arg) {
        ImcExpr arr = (ImcExpr) multiArrExpr.arr.accept(this, arg);

        SemType semType = SemAn.ofType.get(multiArrExpr.arr).actualType();

        ImcCONST typeSize = null;
        ImcExpr imcMEM = arr;

        if (semType instanceof SemPointerType ptrType) {
            SemType type = ptrType;
            for (var idx : multiArrExpr.idxs) {
                ImcExpr idxImcExpr = (ImcExpr) idx.accept(this, arg);
                typeSize = new ImcCONST(MemEvaluator.SizeOfType(ptrType.baseType));
                ImcBINOP imcOffset = new ImcBINOP(ImcBINOP.Oper.MUL, idxImcExpr, typeSize);

                imcMEM = new ImcMEM(new ImcBINOP(ImcBINOP.Oper.ADD, imcMEM, imcOffset));

                if (type instanceof SemPointerType tmpType) {
                    type = tmpType.baseType;
                }
            }
        } else {
            throw new Report.Error(multiArrExpr, "This shouldn't happen.");
        }

        ImcGen.exprImc.put(multiArrExpr, imcMEM);

        return imcMEM;
    }


    // EX9
    @Override
    public Object visit(AstCmpExpr cmpExpr, Stack<MemFrame> arg) {
        ImcMEM imcMEM = (ImcMEM) cmpExpr.expr.accept(this, arg);

        AstRecType.AstCmpDefn cmpDefn = (AstRecType.AstCmpDefn) SemAn.definedAt.get(cmpExpr);
        MemRelAccess memRelAccess = (MemRelAccess) Memory.cmpAccesses.get(cmpDefn);

        ImcBINOP imcBINOP = new ImcBINOP(ImcBINOP.Oper.ADD, imcMEM.addr, new ImcCONST(memRelAccess.offset));
        ImcMEM imc = new ImcMEM(imcBINOP);

        ImcGen.exprImc.put(cmpExpr, imc);

        return imc;
    }

    // EX10
    @Override
    public Object visit(AstCallExpr callExpr, Stack<MemFrame> arg) {
        AstFunDefn funDefn = (AstFunDefn) SemAn.definedAt.get(callExpr);
        MemFrame memFrame = Memory.frames.get(funDefn);

        if (callExpr.name.equals("exit")) {
            if (!conditionalStatement) {
                funcContexts.peek().hasReturnOrExit = true;
            } else if (ifStatementDepth == 0) {
                conditionalContainsReturn = true;
            }
        }

        ImcExpr imcExpr1 = new ImcTEMP(arg.peek().FP);
        for (int i = 0; i < arg.peek().depth - memFrame.depth + 1; i++) {
            imcExpr1 = new ImcMEM(imcExpr1);
        }

        if (memFrame.depth == 0) {
            imcExpr1 = new ImcCONST(0);
        }

        Vector<Long> offsets = new Vector<>();
        Vector<ImcExpr> args = new Vector<>();

        offsets.add(0L);
        args.add(imcExpr1);

        if (callExpr.args != null) {
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

        if (semType instanceof SemCharType) {
            imcCast = new ImcBINOP(ImcBINOP.Oper.MOD, imcExpr, new ImcCONST(256));
        } else {
            imcCast = imcExpr;
        }

        ImcGen.exprImc.put(castExpr, imcCast);

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
        ImcExpr imcExpr = (ImcExpr) exprStmt.expr.accept(this, arg);
        ImcStmt imcESTMT = new ImcESTMT(imcExpr);

        ImcGen.stmtImc.put(exprStmt, imcESTMT);

        return imcESTMT;
    }

    // ST2
    @Override
    public Object visit(AstAssignStmt assignStmt, Stack<MemFrame> arg) {
        ImcExpr expr1 = (ImcExpr) assignStmt.src.accept(this, arg);
        ImcExpr expr2 = (ImcExpr) assignStmt.dst.accept(this, arg);
        ImcStmt imcMOVE = new ImcMOVE(expr2, expr1);

        ImcGen.stmtImc.put(assignStmt, imcMOVE);

        return imcMOVE;
    }

    // ST3, ST4, ST5, ST6
    @Override
    public Object visit(AstIfStmt ifStmt, Stack<MemFrame> arg) {
        boolean previousConditionalValue = conditionalStatement;
        conditionalStatement = true;
        ifStatementDepth++;

        ImcExpr imcExpr = (ImcExpr) ifStmt.cond.accept(this, arg);
        ImcStmt stmt = (ImcStmt) ifStmt.thenStmt.accept(this, arg);

        Vector<ImcStmt> imcStmts = new Vector<>();
        MemLabel thenL = new MemLabel();
        MemLabel elseL = new MemLabel();
        MemLabel endL = new MemLabel();

        imcStmts.add(new ImcCJUMP(imcExpr, thenL, elseL));
        imcStmts.add(new ImcLABEL(thenL));
        imcStmts.add(stmt);

        if (ifStmt.elseStmt != null) {
            imcStmts.add(new ImcJUMP(endL));
        }

        imcStmts.add(new ImcLABEL(elseL));

        if (ifStmt.elseStmt != null) {
            boolean ifContainsReturn = conditionalContainsReturn;
            conditionalStatement = false;

            imcStmts.add((ImcStmt) ifStmt.elseStmt.accept(this, arg));

            if (ifStatementDepth == 0 && conditionalContainsReturn && ifContainsReturn) {
                funcContexts.peek().hasReturnOrExit = true;
            }

            imcStmts.add(new ImcLABEL(endL));
        }

        if (ifStatementDepth == 0) {
            conditionalContainsReturn = false;
        }

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);
        ImcGen.stmtImc.put(ifStmt, imcSTMTS);

        conditionalStatement = previousConditionalValue;
        ifStatementDepth--;


        return imcSTMTS;
    }

    // ST7, ST8
    @Override
    public Object visit(AstWhileStmt whileStmt, Stack<MemFrame> arg) {
        boolean previousConditionalValue = conditionalStatement;
        conditionalStatement = true;

        ImcExpr imcExpr = (ImcExpr) whileStmt.cond.accept(this, arg);
        ImcStmt imcStmt = (ImcStmt) whileStmt.stmt.accept(this, arg);

        MemLabel cond = new MemLabel();
        MemLabel body = new MemLabel();
        MemLabel end = new MemLabel();

        Vector<ImcStmt> imcStmts = new Vector<>();

        imcStmts.add(new ImcLABEL(cond));
        imcStmts.add(new ImcCJUMP(imcExpr, body, end));

        imcStmts.add(new ImcLABEL(body));
        imcStmts.add(imcStmt);

        imcStmts.add(new ImcJUMP(cond));
        imcStmts.add(new ImcLABEL(end));

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);

        conditionalStatement = previousConditionalValue;
        ImcGen.stmtImc.put(whileStmt, imcSTMTS);

        return imcSTMTS;
    }

    @Override
    public Object visit(AstForStmt forStmt, Stack<MemFrame> arg) {
        boolean previousConditionalValue = conditionalStatement;
        conditionalStatement = true;

        ImcStmt imcInitStmt = (ImcStmt) forStmt.init.accept(this, arg);
        ImcExpr imcExpr = (ImcExpr) forStmt.cond.accept(this, arg);
        ImcStmt imcStepStmt = (ImcStmt) forStmt.step.accept(this, arg);
        ImcStmt imcBodyStmt = (ImcStmt) forStmt.stmt.accept(this, arg);

        MemLabel cond = new MemLabel();
        MemLabel body = new MemLabel();
        MemLabel end = new MemLabel();

        Vector<ImcStmt> imcStmts = new Vector<>();

        imcStmts.add(imcInitStmt);
        imcStmts.add(new ImcLABEL(cond));
        imcStmts.add(new ImcCJUMP(imcExpr, body, end));

        imcStmts.add(new ImcLABEL(body));
        imcStmts.add(imcBodyStmt);

        imcStmts.add(imcStepStmt);
        imcStmts.add(new ImcJUMP(cond));
        imcStmts.add(new ImcLABEL(end));

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);

        conditionalStatement = previousConditionalValue;
        ImcGen.stmtImc.put(forStmt, imcSTMTS);

        return imcSTMTS;
    }

    @Override
    public Object visit(AstVecForStmt vecForStmt, Stack<MemFrame> arg) {
        boolean previousConditionalValue = conditionalStatement;
        conditionalStatement = true;

        ImcExpr name = (ImcExpr) vecForStmt.name.accept(this, arg);
        ImcExpr lower = (ImcExpr) vecForStmt.lower.accept(this, arg);
        ImcExpr upper = (ImcExpr) vecForStmt.upper.accept(this, arg);
        ImcExpr step = (ImcExpr) vecForStmt.step.accept(this, arg);
        ImcStmt imcBodyStmt = (ImcStmt) vecForStmt.stmt.accept(this, arg);

        MemLabel cond = new MemLabel();
        MemLabel body = new MemLabel();
        MemLabel end = new MemLabel();

        Vector<ImcStmt> imcStmts = new Vector<>();

        ImcStmt imcInitStmt = new ImcMOVE(name, lower);
        ImcStmt imcStepStmt = new ImcMOVE(name, new ImcBINOP(ImcBINOP.Oper.ADD, name, step));
        ImcExpr condExpr = new ImcBINOP(ImcBINOP.Oper.LTH, name, upper);


        imcStmts.add(imcInitStmt);
        imcStmts.add(new ImcLABEL(cond));
        imcStmts.add(new ImcCJUMP(condExpr, body, end));

        imcStmts.add(new ImcLABEL(body));
        imcStmts.add(imcBodyStmt);

        imcStmts.add(imcStepStmt);
        imcStmts.add(new ImcJUMP(cond));
        imcStmts.add(new ImcLABEL(end));

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);

        conditionalStatement = previousConditionalValue;
        ImcGen.stmtImc.put(vecForStmt, imcSTMTS);

        return imcSTMTS;
    }

    // ST9
    @Override
    public Object visit(AstBlockStmt blockStmt, Stack<MemFrame> arg) {
        Vector<ImcStmt> imcStmts = new Vector<>();

        for (int i = 0; i < blockStmt.stmts.size(); i++) {
            ImcStmt midStmt = (ImcStmt) blockStmt.stmts.get(i).accept(this, arg);
            imcStmts.add(midStmt);
        }

        ImcStmt imcSTMTS = new ImcSTMTS(imcStmts);
        ImcGen.stmtImc.put(blockStmt, imcSTMTS);

        return imcSTMTS;
    }

    @Override
    public Object visit(AstReturnStmt retStmt, Stack<MemFrame> arg) {
        Vector<ImcStmt> returnStms = new Vector<>();
        ImcMEM saveTo = new ImcMEM(new ImcTEMP(arg.peek().FP));
        ImcStmt imcStmt;

        if (!conditionalStatement) {
            funcContexts.peek().hasReturnOrExit = true;
        } else if (ifStatementDepth == 0) {
            conditionalContainsReturn = true;
        }

        returnStms.add(new ImcMOVE(saveTo, (ImcExpr) retStmt.expr.accept(this, arg)));

        if (conditionalStatement) {
            returnStms.add(new ImcJUMP(funcContexts.peek().exitL));
            imcStmt = new ImcSTMTS(returnStms);
        } else {
            imcStmt = returnStms.getFirst();
        }

        ImcGen.stmtImc.put(retStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public Object visit(AstDecoratorStmt decStmt, Stack<MemFrame> arg) {
        ImcSTMTS stmts = (ImcSTMTS) decStmt.stmt.accept(this, arg);
        Vector<AstNameExpr> exprs = new Vector<>();
        for (AstExpr expr : decStmt.deps) {
            exprs.add((AstNameExpr) expr);
        }

        ImcVectStmt vectStmt = new ImcVectStmt(exprs, stmts);
        ImcGen.stmtImc.put(decStmt, vectStmt);

        return vectStmt;
    }

    @Override
    public Object visit(AstNodes<? extends AstNode> nodes, Stack<MemFrame> arg) {
        for (AstNode node : nodes) {
            if (node instanceof AstDefn def) {
                if (def instanceof AstFunDefn fun) {
                    fun.accept(this, arg);
                }
            } else {
                node.accept(this, arg);
            }
        }

        return null;
    }

    private int createChar(String value) {
        int c;

        int fst = value.indexOf('\'');
        int lst = value.lastIndexOf('\'') == -1 ? value.length() : value.lastIndexOf('\'');
        value = value.substring(fst + 1, lst);

        if (value.length() == 1) {
            c = value.charAt(0);
        } else if (value.length() == 2) {
            value = value.substring(value.indexOf("\\") + 1);
            c = value.indexOf('n') == -1 ? value.charAt(0) : 0x0A;
        } else if (value.length() == 3) {
            c = Integer.parseInt(value.substring(value.indexOf("\\") + 1), 16);
        } else {
            throw new Report.Error("Not a valid char" + value);
        }

        return c;
    }

    private long checkAndParse(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new Report.Error("Not a valid number" + value);
        }
    }

    private ImcStmt functionBody(ImcStmt mainStmt) {
        if (mainStmt instanceof ImcSTMTS mainSTMTS) {
            mainSTMTS.stmts.addFirst(new ImcLABEL(funcContexts.peek().entryL));
            mainSTMTS.stmts.addLast(new ImcLABEL(funcContexts.peek().exitL));
        } else {
            Vector<ImcStmt> stmtVector = new Vector<>();
            stmtVector.add(new ImcLABEL(funcContexts.peek().entryL));
            stmtVector.add(mainStmt);

            stmtVector.add(new ImcLABEL(funcContexts.peek().exitL));

            mainStmt = new ImcSTMTS(stmtVector);
        }

        return mainStmt;
    }

    private class FuncContext {
        MemLabel entryL;
        MemLabel exitL;
        boolean first;
        boolean hasReturnOrExit;

        FuncContext(MemLabel entryL, MemLabel exitL) {
            this.first = true;
            this.hasReturnOrExit = false;
            this.entryL = entryL;
            this.exitL = exitL;
        }
    }

}
package lang24.phase.memory;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.tree.type.AstStrType;
import lang24.data.ast.tree.type.AstUniType;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemRelAccess;
import lang24.data.type.*;
import lang24.phase.seman.SemAn;

import java.util.Arrays;

/**
 * Computing memory layout: stack frames and variable accesses.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 * @author marko.muc12@gmail.com
 */
public class MemEvaluator implements AstFullVisitor<Object, MemEvaluator.Carry> {

    private static final String[] STD_FUNCTIONS = {
            "start",
            "exit",
            "getchar",
            "getint",
            "mmap",
            "putcarr",
            "putchar",
            "putcstr",
            "putint",
            "read",
            "write"
    };
    private static int strCount = 0;

    public static Long SizeOfType(SemType type) {
        long size = 0L;

        if (type.actualType() instanceof SemCharType ||
                type.actualType() instanceof SemBoolType) {
            size = 8L;
            return size;
        }

        if (type.actualType() instanceof SemArrayType arr) {
            long typeSize = SizeOfType(arr.elemType);
            if (arr.elemType.actualType() instanceof SemCharType ||
                    arr.elemType.actualType() instanceof SemBoolType) {
                typeSize = typeSize + (8 - (typeSize % 8)) % 8;
            }
            size = arr.size * typeSize;
            return size;
        }

        if (type.actualType() instanceof SemIntType ||
                type.actualType() instanceof SemPointerType) {
            size = 8L;
            return size;
        }

        if (type.actualType() instanceof SemStructType structType) {
            for (SemType cmpType : structType.cmpTypes) {
                long typeSize = SizeOfType(cmpType);
                typeSize = typeSize + (8 - (typeSize % 8)) % 8;

                size += typeSize;
            }
            return size;
        }

        if (type.actualType() instanceof SemUnionType unionType) {
            for (SemType cmpType : unionType.cmpTypes) {
                long typeSize = SizeOfType(cmpType);
                typeSize = typeSize + (8 - (typeSize % 8)) % 8;
                size = Math.max(size, typeSize);
            }
            return size;
        }

        return size;
    }

    @Override
    public Object visit(AstVarDefn varDefn, Carry arg) {
        varDefn.type.accept(this, arg);
        SemType type = SemAn.isType.get(varDefn.type);

        long size = SizeOfType(type);
        if (arg == null) {
            Memory.varAccesses.put(varDefn, new MemAbsAccess(size, new MemLabel(varDefn.name)));
        } else {
            FuncCarry funcCarry = (FuncCarry) arg;
            funcCarry.LocalsSize += size;
            MemRelAccess memRelAccess = new MemRelAccess(size, -funcCarry.LocalsSize, funcCarry.depth);
            Memory.varAccesses.put(varDefn, memRelAccess);
        }
        return null;
    }

    @Override
    public Object visit(AstFunDefn funDefn, Carry arg) {
        FuncCarry funcCarry = new FuncCarry();
        MemLabel label = null;

        if (arg == null) {
            funcCarry.depth = 0;

            if (funDefn.name.equals("main") &&
                    !(SemAn.ofType.get(funDefn) instanceof SemIntType)) {
                throw new Report.Error(funDefn, "Global function " + funDefn.name
                        + " must be of type int.");
            }

            if (funDefn.name.equalsIgnoreCase("start")) {
                throw new Report.Error(funDefn, "Global function start is a reserved and can't be used.");
            }

            if (Arrays.asList(STD_FUNCTIONS).contains(funDefn.name.toLowerCase())
                    && funDefn.stmt != null) {
                throw new Report.Error(funDefn, "Global function " + funDefn.name
                        + " is a reserved standard function so it can only be a function prototype.");
            }

            label = new MemLabel(funDefn.name);
        } else {
            funcCarry.depth = arg.depth + 1;
            label = new MemLabel();
        }

        if (funDefn.pars != null) {
            funDefn.pars.accept(this, funcCarry);
        }

        funDefn.type.accept(this, funcCarry);

        if (funDefn.defns != null) {
            funDefn.defns.accept(this, funcCarry);
        }

        if (funDefn.stmt != null) {
            funDefn.stmt.accept(this, funcCarry);
        }

        // One 8L for SL, one 8L for FP
        long size = funcCarry.LocalsSize + funcCarry.ArgsSize + 8L + 8L;
        MemFrame memFrame = new MemFrame(label, funcCarry.depth,
                funcCarry.LocalsSize, funcCarry.ArgsSize, size);

        Memory.frames.put(funDefn, memFrame);

        return null;
    }

    @Override
    public Object visit(AstFunDefn.AstRefParDefn refParDefn, Carry arg) {
        refParDefn.type.accept(this, arg);
        SemType type = SemAn.isType.get(refParDefn.type);
        long size = SizeOfType(type);

        FuncCarry funcCarry = (FuncCarry) arg;
        funcCarry.ParSize += size;

        MemRelAccess memRelAccess = new MemRelAccess(size,
                funcCarry.ParSize, funcCarry.depth);
        Memory.parAccesses.put(refParDefn, memRelAccess);

        return null;
    }

    @Override
    public Object visit(AstFunDefn.AstValParDefn valParDefn, Carry arg) {
        SemType type = SemAn.isType.get(valParDefn.type);
        long size = SizeOfType(type);

        FuncCarry funcCarry = (FuncCarry) arg;
        funcCarry.ParSize += size;

        MemRelAccess memRelAccess = new MemRelAccess(size,
                funcCarry.ParSize, funcCarry.depth);
        Memory.parAccesses.put(valParDefn, memRelAccess);

        return null;
    }

    @Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, Carry arg) {
        cmpDefn.type.accept(this, arg);
        SemType type = SemAn.isType.get(cmpDefn.type);
        RecCarry recCarry = (RecCarry) arg;

        long offset = 0;
        if (recCarry.type == RecCarry.RecType.STR) {
            offset = recCarry.CompSize;
        }

        long size = SizeOfType(type);
        MemRelAccess memRelAccess = new MemRelAccess(size, offset, arg.depth);

        if (recCarry.type == RecCarry.RecType.STR) {
            size = size + (8 - (size % 8)) % 8;
            recCarry.CompSize += size;
        } else {
            recCarry.CompSize = Math.max(size, recCarry.CompSize);
        }

        Memory.cmpAccesses.put(cmpDefn, memRelAccess);

        return null;
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, Carry arg) {
        if (atomExpr.type == AstAtomExpr.Type.STR) {
            String StrConst = atomExpr.value.substring(1, atomExpr.value.length() - 1);
            long size = (StrConst.length() + 1) * 8L;
            MemAbsAccess memAbsAccess = new MemAbsAccess(size, new MemLabel("__str" + strCount), StrConst);
            Memory.strings.put(atomExpr, memAbsAccess);
            strCount++;
        }
        return null;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Carry arg) {
        long ArgsSize = 8L;

        if (callExpr.args != null) {
            callExpr.args.accept(this, arg);

            for (AstExpr astExpr : callExpr.args) {
                astExpr.accept(this, arg);
                ArgsSize += SizeOfType(SemAn.ofType.get(astExpr));
            }
        }

        FuncCarry funcCarry = (FuncCarry) arg;
        funcCarry.ArgsSize = Math.max(ArgsSize, funcCarry.ArgsSize);

        return null;
    }

    @Override
    public Object visit(AstUniType uniType, Carry arg) {
        uniType.cmps.accept(this, new RecCarry(RecCarry.RecType.UNI));
        return null;
    }

    @Override
    public Object visit(AstStrType strType, Carry arg) {
        strType.cmps.accept(this, new RecCarry(RecCarry.RecType.STR));
        return null;
    }

    public static abstract class Carry {
        long depth;
    }

    public class FuncCarry extends Carry {
        long LocalsSize;
        long ArgsSize;
        long ParSize;
    }

    public class RecCarry extends Carry {
        long CompSize;
        RecType type = null;

        RecCarry(RecType type) {
            this.type = type;
            this.depth = -1;
            this.CompSize = 0;
        }

        enum RecType {STR, UNI}
    }
}

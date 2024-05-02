package lang24.phase.memory;

import java.security.SignatureException;
import java.util.*;

import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.tree.type.AstStrType;
import lang24.data.ast.tree.type.AstUniType;
import lang24.data.ast.visitor.*;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.data.type.visitor.*;
import lang24.phase.seman.SemAn;

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */

/*
    FIXME:
     1. No function frame for function prototypes
    TODO:
     1. Check global name usage
 */

public class MemEvaluator implements AstFullVisitor<Object, MemEvaluator.Carry> {

    HashSet<String> GlobalNames = new HashSet<>();

    @Override
    public Object visit(AstVarDefn varDefn, Carry arg) {
        varDefn.type.accept(this, arg);
        SemType type = SemAn.isType.get(varDefn.type);
        long size = SizeOfType(type);
        if(arg == null){
            if(GlobalNames.contains(varDefn.name)){
                // Is this even needed? -> Shadowing in same scope, is this even allowed in our language?
                Memory.varAccesses.put(varDefn, new MemAbsAccess(size, new MemLabel()));
            }else{
                GlobalNames.add(varDefn.name);
                Memory.varAccesses.put(varDefn, new MemAbsAccess(size, new MemLabel(varDefn.name)));
            }

        }else {
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

        if(arg == null){
            funcCarry.depth = 0;
            if(GlobalNames.contains(funDefn.name)) {
                label = new MemLabel();
            }else{
                GlobalNames.add(funDefn.name);
                label = new MemLabel(funDefn.name);
            }
        }else {
            funcCarry.depth = arg.depth + 1;
            label = new MemLabel();
        }

        if(funDefn.pars != null){
            funDefn.pars.accept(this, funcCarry);
        }

        funDefn.type.accept(this, funcCarry);

        if(funDefn.defns != null){
            funDefn.defns.accept(this, funcCarry);
        }

        if(funDefn.stmt != null){
            funDefn.stmt.accept(this, funcCarry);
        }

        // One 8L for SL, one 8L for FP
        long size = funcCarry.LocalsSize + funcCarry.ArgsSize + 8L + 8L;
        MemFrame memFrame = new MemFrame(label, funcCarry.depth,
                funcCarry.LocalsSize, funcCarry.ArgsSize, size);

        Memory.frames.put(funDefn, memFrame);

        return  null;
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
        valParDefn.type.accept(this, arg);
        SemType type = SemAn.isType.get(valParDefn.type);
        long size = SizeOfType(type);

        FuncCarry funcCarry = (FuncCarry) arg;
        funcCarry.ParSize += size;

        MemRelAccess memRelAccess = new MemRelAccess(size, funcCarry.ParSize,
                funcCarry.depth);
        Memory.parAccesses.put(valParDefn, memRelAccess);

        return null;
    }

    @Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, Carry arg) {
        cmpDefn.type.accept(this, arg);
        SemType type = SemAn.isType.get(cmpDefn.type);
        RecCarry recCarry = (RecCarry) arg;
        long offset = 0;
        if(recCarry.type == RecCarry.RecType.STR){
            offset = recCarry.CompSize;
        }

        long size = SizeOfType(type);
        MemRelAccess memRelAccess = new MemRelAccess(size, offset, arg.depth);

        if (recCarry.type == RecCarry.RecType.STR) {
                size = size + (8 - (size % 8) % 8);
            recCarry.CompSize += size;
        }else {
            recCarry.CompSize = Math.max(size, recCarry.CompSize);
        }

        Memory.cmpAccesses.put(cmpDefn, memRelAccess);

        return null;
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, Carry arg) {
        if(atomExpr.type == AstAtomExpr.Type.STR){
            String StrConst = atomExpr.value.substring(1, atomExpr.value.length()-1);
            long size = (StrConst.length() + 1);
            MemAbsAccess memAbsAccess = new MemAbsAccess(size, new MemLabel(StrConst), StrConst);
            Memory.strings.put(atomExpr, memAbsAccess);
        }
        return null;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Carry arg) {
        long ArgsSize = 8L;

        if(callExpr.args != null) {
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
    public class FuncCarry extends Carry{
        long LocalsSize;
        long ArgsSize;
        long ParSize;
    }
    public class RecCarry extends Carry{
        long CompSize;
        enum RecType {STR, UNI};
        RecType type = null;
        RecCarry(RecType type){
            this.type = type;
            this.depth = -1;
        }
    }

    public static Long SizeOfType(SemType type){
        long size = 0L;

        if(type.actualType() instanceof SemCharType ||
                type.actualType() instanceof SemBoolType){
            size = 8L;
            return size;
        }

        if(type.actualType() instanceof SemArrayType arr){
            long typeSize = SizeOfType(arr.elemType);
            if(arr.elemType.actualType() instanceof SemCharType ||
                arr.elemType.actualType() instanceof SemBoolType){
                typeSize = typeSize + (8 - (typeSize % 8)) % 8;
            }
            size = arr.size * typeSize;
            return size;
        }

        if(type.actualType() instanceof SemIntType ||
            type.actualType() instanceof SemPointerType){
            size = 8L;
            return size;
        }

        if(type.actualType() instanceof SemStructType structType){
            for (SemType cmpType : structType.cmpTypes) {
                long typeSize = SizeOfType(cmpType);
                typeSize = typeSize + (8 - (typeSize % 8)) % 8;

                size += typeSize;
            }
            return size;
        }

        if(type.actualType() instanceof SemUnionType unionType){
            for (SemType cmpType : unionType.cmpTypes) {
                long typeSize = SizeOfType(cmpType);
                typeSize = typeSize + (8 - (typeSize % 8)) % 8;
                size = Math.max(size, typeSize);
            }
            return size;
        }

        return size;
    }
}

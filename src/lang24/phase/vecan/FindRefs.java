package lang24.phase.vecan;

import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.datadep.*;
import lang24.phase.seman.SemAn;

import java.util.Stack;
import java.util.Vector;

public class FindRefs implements AstFullVisitor<ArrRef, LoopDescriptor> {

    Stack<AstStmt> currStmt = new Stack<>();
    Stack<AstDefn> loopVars = new Stack<>();
    boolean inSubscript = false;

    @Override
    public ArrRef visit(AstVecForStmt vecForStmt, LoopDescriptor arg) {
        Vector<LoopDescriptor> nested = null;
        int depth = 0;
        if (arg != null) {
            depth = arg.depth + 1;
            nested = arg.nest;
        }

        LoopDescriptor loopDescriptor = new LoopDescriptor(
                depth,
                vecForStmt.name,
                vecForStmt.lower,
                vecForStmt.upper,
                vecForStmt.step,
                nested
        );
        loopDescriptor.addLoop(arg);
        loopVars.push(SemAn.definedAt.get(vecForStmt.name));

        vecForStmt.stmt.accept(this, loopDescriptor);

        if (loopDescriptor.vectorizable) {
            System.out.println(loopDescriptor);
            VecAn.loopDescriptors.put(vecForStmt, loopDescriptor);
        }

        loopVars.pop();

        return null;
    }

    @Override
    public ArrRef visit(AstArrExpr arrExpr, LoopDescriptor arg) {
        ArrRef ref = null;
        if (arg == null) {
            return null;
        }
        if (inSubscript || !arg.vectorizable) {
            arg.vectorizable = false;
            return null;
        }

        inSubscript = true;
        arrExpr.idx.accept(this, arg);
        inSubscript = false;

        if (arrExpr.arr instanceof AstNameExpr) {
            ref = new ArrRef(
                    arrExpr.idx,
                    arrExpr,
                    currStmt.peek(),
                    arg.depth
            );
            arg.addArrayRef(ref);
        }else{
            arg.vectorizable = false;
        }

        return ref;
    }

    @Override
    public ArrRef visit(AstNameExpr nameExpr, LoopDescriptor arg) {
        //CHECKME: What should happen if outer loop isn't vectorizable but inner could be
        if(arg != null){
            if(!arg.vectorizable){
                return null;
            } else if (inSubscript &&
                    !loopVars.contains(SemAn.definedAt.get(nameExpr))) {
                arg.vectorizable = false;
            }
        }

        return null;
    }

    @Override
    public ArrRef visit(AstSfxExpr sfxExpr, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }

        return null;
    }

    @Override
    public ArrRef visit(AstCmpExpr cmpExpr, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }

        return null;
    }

    @Override
    public ArrRef visit(AstSizeofExpr sizeofExpr, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }

        return null;
    }

    @Override
    public ArrRef visit(AstCallExpr callExpr, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }

        return null;
    }

    @Override
    public ArrRef visit(AstExprStmt exprStmt, LoopDescriptor arg) {
        currStmt.push(exprStmt);
        exprStmt.expr.accept(this, arg);
        currStmt.pop();

        return null;
    }

    @Override
    public ArrRef visit(AstAssignStmt assignStmt, LoopDescriptor arg) {
        if (arg == null || !arg.vectorizable) {
            return null;
        }

        currStmt.push(assignStmt);

        assignStmt.src.accept(this, arg);
        ArrRef dstSrc = assignStmt.dst.accept(this, arg);

        currStmt.pop();

        if (dstSrc != null) {
            dstSrc.assign = true;
        }

        return null;
    }

    @Override
    public ArrRef visit(AstIfStmt ifStmt, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }
        return null;
    }

    @Override
    public ArrRef visit(AstReturnStmt retStmt, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }
        return null;
    }

    @Override
    public ArrRef visit(AstWhileStmt whileStmt, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }
        return null;
    }

    @Override
    public ArrRef visit(AstForStmt forStmt, LoopDescriptor arg) {
        if (arg != null) {
            arg.vectorizable = false;
        }
        return null;
    }

    @Override
    public ArrRef visit(AstBlockStmt blockStmt, LoopDescriptor arg) {
        for (AstStmt stmt : blockStmt.stmts) {
            stmt.accept(this, arg);
        }
        return null;
    }
}

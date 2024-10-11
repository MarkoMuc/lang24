package lang24.phase.vecan;

import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
import lang24.phase.seman.SemAn;

import java.util.Stack;
import java.util.Vector;

/*
    Currently only works with normalized linear loops meaning:
        - no symbols in loop boundaries
        - step = 1
        - lower bound = 0
        - upper bound is non-negative
        - Only constants and loop increment variables
    @author marko.muc12@gmail.com
 */
public class FindRefs implements AstFullVisitor<ArrRef, LoopDescriptor> {
    Stack<AstStmt> currStmt = new Stack<>();
    Vector<LoopDescriptor> loopVars = new Vector<>();
    Stack<AstVecForStmt> descStack;

    boolean inSubscript = false;
    int stmtNum = 0;

    @Override
    public ArrRef visit(AstVecForStmt vecForStmt, LoopDescriptor arg) {
        int oldStmtNum = stmtNum;
        stmtNum = 0;
        if (vecForStmt.lower instanceof AstAtomExpr lower &&
                vecForStmt.upper instanceof AstAtomExpr upper &&
                vecForStmt.step instanceof AstAtomExpr step) {
            // Forces linear and normalized loops only
            if (Integer.parseInt(step.value) != 1 || Integer.parseInt(lower.value) > 0 ||
                    Integer.parseInt(upper.value) < 0) {
                if (arg != null) {
                    arg.vectorizable = false;
                }
                return null;
            }
        } else {
            // If an inner loop is not vectorizable, propagate it upwards
            if (arg != null) {
                arg.vectorizable = false;
            }
            return null;
        }

        LoopDescriptor loopDescriptor = new LoopDescriptor(
                vecForStmt,
                vecForStmt.name,
                Integer.parseInt(lower.value),
                Integer.parseInt(upper.value),
                Integer.parseInt(step.value)
        );
        if (descStack == null) {
            descStack = new Stack<>();
        }
        descStack.push(vecForStmt);

        for (var loop : loopVars) {
            if (SemAn.definedAt.get(loop.loopIndex) == SemAn.definedAt.get(loopDescriptor.loopIndex)) {
                if (arg != null) {
                    arg.vectorizable = false;
                }
                return null;
            }
        }

        // Creates a loop nest by adding outer loops
        if(arg !=null && arg.vectorizable) {
            loopDescriptor.addOuterLoops(arg);
        }

        // Add itself to the nest
        loopVars.addLast(loopDescriptor);

        // Check loop body
        vecForStmt.stmt.accept(this, loopDescriptor);

        if (loopDescriptor.vectorizable) {
            // The outermost loop is added
            if (arg == null) {
                VecAn.loops.add(loopDescriptor);
                VecAn.outerToNest.put(vecForStmt, new Vector<>(descStack));
            }
        } else if (arg != null) {
            // Propagates inner loop not being vectorizable
            arg.vectorizable = false;
            return null;
        } else {
            return null;
        }

        // Adds array references of the inner loops
        if (arg != null) {
            arg.addInnerRefs(loopDescriptor);
        }

        // Removes its own loop variable which should be the last one in the row
        loopVars.removeLast();

        // Resets stmt number
        stmtNum = oldStmtNum;

        return null;
    }

    @Override
    public ArrRef visit(AstMultiArrExpr multiArrExpr, LoopDescriptor arg) {
        ArrRef ref = null;
        if (arg == null) {
            return null;
        }
        if (inSubscript || !arg.vectorizable) {
            arg.vectorizable = false;
            return null;
        }

        inSubscript = true;
        var idxs = new Vector<AstExpr>();
        for (var idx : multiArrExpr.idxs) {
            idx.accept(this, arg);
            idxs.add(idx);
        }
        inSubscript = false;

        if (multiArrExpr.arr instanceof AstNameExpr nameExpr) {
            ref = new ArrRef(
                    idxs,
                    nameExpr,
                    currStmt.peek(),
                    SemAn.definedAt.get(nameExpr),
                    arg,
                    stmtNum,
                    arg.getDepth()
            );
            arg.addArrayRef(ref);
        } else {
            arg.vectorizable = false;
        }

        return ref;
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

        if (arrExpr.arr instanceof AstNameExpr nameExpr) {
            ref = new ArrRef(
                    arrExpr.idx,
                    nameExpr,
                    currStmt.peek(),
                    SemAn.definedAt.get(nameExpr),
                    arg,
                    stmtNum,
                    arg.getDepth()
            );
            arg.addArrayRef(ref);
        }else{
            arg.vectorizable = false;
        }

        return ref;
    }

    @Override
    public ArrRef visit(AstNameExpr nameExpr, LoopDescriptor arg) {
        if(arg != null){
            if(!arg.vectorizable){
                return null;
            } else if (inSubscript) {
                for (int i = loopVars.size() - 1; i >= 0; i--) {
                    LoopDescriptor loop = loopVars.get(i);
                    if (SemAn.definedAt.get(loop.loopIndex) ==
                            SemAn.definedAt.get(nameExpr)) {
                        VecAn.loopDescriptors.put(nameExpr, loop);
                        return null;
                    }
                }
                arg.vectorizable = false;
            }
        }

        return null;
    }

    @Override
    public ArrRef visit(AstPfxExpr pfxExpr, LoopDescriptor arg) {
        if (arg != null &&
                !(pfxExpr.oper == AstPfxExpr.Oper.ADD ||
                        pfxExpr.oper == AstPfxExpr.Oper.SUB ||
                        pfxExpr.expr instanceof AstAtomExpr ||
                        pfxExpr.expr instanceof AstNameExpr)) {
            arg.vectorizable = false;
        }
        pfxExpr.expr.accept(this, arg);

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
        stmtNum++;

        exprStmt.expr.accept(this, arg);

        currStmt.pop();

        return null;
    }

    @Override
    public ArrRef visit(AstAssignStmt assignStmt, LoopDescriptor arg) {
        if (arg == null || !arg.vectorizable) {
            return null;
        }

        stmtNum++;
        currStmt.push(assignStmt);

        assignStmt.src.accept(this, arg);
        ArrRef dstSrc = assignStmt.dst.accept(this, arg);

        currStmt.pop();

        if (dstSrc != null) {
            dstSrc.assign = true;
        } else {
            // Assign statement can only be used on array expressions
            arg.vectorizable = false;
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

    @Override
    public ArrRef visit(AstAtomExpr atomExpr, LoopDescriptor arg) {
        if (arg != null && atomExpr.type != AstAtomExpr.Type.INT) {
            arg.vectorizable = false;
        }
        return null;
    }

    @Override
    public ArrRef visit(AstBinExpr binExpr, LoopDescriptor arg) {
        // If contains illegal operand
        return AstFullVisitor.super.visit(binExpr, arg);
    }
}

package lang24.phase.imclin;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.lin.BasicBlock;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemAccess;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemLabel;
import lang24.phase.imcgen.ImcGen;
import lang24.phase.memory.Memory;

import java.util.Vector;

/**
 * @author marko.muc12@gmail.com
 */
public class ChunkGenerator implements AstFullVisitor<Object, Object> {

    @Override
    public Object visit(AstVarDefn varDefn, Object arg) {
        MemAccess access = Memory.varAccesses.get(varDefn);
        if (access instanceof MemAbsAccess abs) {
            ImcLin.addDataChunk(new LinDataChunk(abs));
        }
        return null;
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, Object arg) {
        if (atomExpr.type == AstAtomExpr.Type.STR) {
            MemAbsAccess memAbsAccess = Memory.strings.get(atomExpr);
            ImcLin.addDataChunk(new LinDataChunk(memAbsAccess));
        }
        return null;
    }

    @Override
    public Object visit(AstFunDefn funDefn, Object arg) {
        MemFrame memFrame = Memory.frames.get(funDefn);

        MemLabel entryLabel = ImcGen.entryLabel.get(funDefn);
        MemLabel exitLabel = ImcGen.exitLabel.get(funDefn);

        if (funDefn.defns != null) {
            funDefn.defns.accept(this, null);
        }

        if (funDefn.stmt != null) {
            ImcStmt imcStmt = ImcGen.stmtImc.get(funDefn.stmt);
            Vector<ImcStmt> canonized = imcStmt.accept(new StmtCanonizer(), null);
            Vector<ImcStmt> linStmts = linearization(canonized);

            ImcLin.addCodeChunk(new LinCodeChunk(memFrame, linStmts, entryLabel, exitLabel));

            funDefn.stmt.accept(this, null);
        }

        return null;
    }

    private Vector<BasicBlock> genBasicBlocks(Vector<ImcStmt> stmts) {
        Vector<BasicBlock> basicBlocks = new Vector<>();
        Vector<ImcStmt> temp = new Vector<>();
        ImcLABEL label = null;
        boolean startBlock = false;

        for (ImcStmt stmt : stmts) {
            if (stmt instanceof ImcLABEL labelStmt) {
                if (startBlock) {
                    basicBlocks.add(new BasicBlock(label, null, temp));
                    label = labelStmt;
                    temp = new Vector<>();
                    startBlock = false;
                } else {
                    label = labelStmt;
                    startBlock = true;
                }
            } else if (stmt instanceof ImcJUMP || stmt instanceof ImcCJUMP) {
                if (startBlock) {
                    basicBlocks.add(new BasicBlock(label, stmt, temp));
                    label = null;
                    temp = new Vector<>();
                    startBlock = false;
                }
            } else {
                temp.add(stmt);
            }
        }

        if (label != null) {
            // TODO: any smarter way
            //  -> if its jump to exit, how to differentiate it from all the other exists?
            //  -> i dont think it actually matters?
            basicBlocks.add(new BasicBlock(label, new ImcJUMP(new MemLabel("DONE")), temp));
        }


        for (BasicBlock basicBlock : basicBlocks) {
            if (basicBlock.getEntry() == null) {
                basicBlock.setEntry(new ImcLABEL(new MemLabel()));
            }
        }

        for (int i = 0; i < basicBlocks.size(); i++) {
            BasicBlock block = basicBlocks.get(i);
            if (block.getExit() == null) {
                if (i + 1 >= basicBlocks.size()) {
                    throw new Report.Error("Basic blocks creation error, no next block for jump");
                }

                block.setExit(new ImcJUMP(
                        basicBlocks.get(i + 1).getEntry().label));
            }
        }

        return basicBlocks;
    }

    private void debug(Vector<BasicBlock> basicBlocks) {
        for (BasicBlock basicBlock : basicBlocks) {
            System.out.println(basicBlock);
            System.out.println(basicBlock.getEntry());
            for (ImcStmt stmt : basicBlock.getStmts()) {
                System.out.println(stmt);
            }
            System.out.println(basicBlock.getExit());
        }
    }

    private Vector<ImcStmt> linearization(Vector<ImcStmt> stmts) {
        Vector<ImcStmt> linearStmts = new Vector<>();

        for (int i = 0; i < stmts.size(); i++) {
            ImcStmt stmt = stmts.get(i);
            if (stmt instanceof ImcCJUMP imcCJump) {
                MemLabel negLabel = new MemLabel();
                linearStmts.add(new ImcCJUMP(imcCJump.cond, imcCJump.posLabel, negLabel));
                linearStmts.add(new ImcLABEL(negLabel));
                linearStmts.add(new ImcJUMP(imcCJump.negLabel));
            } else {
                linearStmts.add(stmt);
            }
        }

        return linearStmts;
    }

}

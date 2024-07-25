package lang24.phase.imclin;

import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemAccess;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemLabel;
import lang24.phase.imcgen.ImcGen;
import lang24.phase.memory.Memory;

import java.util.Vector;

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

            Vector<ImcStmt> linStmts = LinImcCALL(canonized);

            ImcLin.addCodeChunk(new LinCodeChunk(memFrame, linStmts, entryLabel, exitLabel));

            // For declaration stmts
            funDefn.stmt.accept(this, null);
        }

        return null;
    }

    private Vector<ImcStmt> LinImcCALL(Vector<ImcStmt> stmts) {
        Vector<ImcStmt> linearStmts = new Vector<ImcStmt>();
        for (int s = 0; s < stmts.size(); s++) {
            ImcStmt stmt = stmts.get(s);
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

    //* Permute

}

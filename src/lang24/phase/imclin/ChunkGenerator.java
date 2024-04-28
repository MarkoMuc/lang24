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
        //Data chunk for the variable
        MemAccess access = Memory.varAccesses.get(varDefn);
        if (access instanceof MemAbsAccess) {
            ImcLin.addDataChunk(
                    new LinDataChunk((MemAbsAccess) access));
        }
        return null;
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, Object arg) {
        //Data chunk for the string
        if (atomExpr.type == AstAtomExpr.Type.STR) {
            MemAbsAccess memAbsAccess = Memory.strings.get(atomExpr);
            ImcLin.addDataChunk(
                    new LinDataChunk(memAbsAccess));
            return null;
        }
        return null;
    }

    //Code Chunk
    @Override
    public Object visit(AstFunDefn funDefn, Object arg) {
        //Get function frame
        MemFrame memFrame = Memory.frames.get(funDefn);

        //Vector for the statements
        Vector<ImcStmt> imcStmtVector = new Vector<>();

        //Prepares the Entry and Exit label
        MemLabel entryLabel = new MemLabel();
        MemLabel exitLabel = new MemLabel();

        //Adds entry label for the function body
        imcStmtVector.add(new ImcLABEL(entryLabel));

        if(funDefn.defns != null){
            funDefn.defns.accept(this, null);
        }

        if(funDefn.stmt != null) {
            ImcStmt imcStmt = ImcGen.stmtImc.get(funDefn.stmt);
            //Canonizes the Stmts
            Vector<ImcStmt> canonized = imcStmt.accept(new StmtCanonizer(), null);
            imcStmtVector.addAll(canonized);

            //Jump to epilogue
            imcStmtVector.add(new ImcJUMP(exitLabel));

            //Linearizes the stmts
            Vector<ImcStmt> linStmts = linearize(imcStmtVector);

            ImcLin.addCodeChunk(new LinCodeChunk(memFrame, linStmts, entryLabel, exitLabel));

            // For declaration stmts
            funDefn.stmt.accept(this,null);
        } else {
            //TODO: is this needed?
            // ImcLin.addCodeChunk(new LinCodeChunk(memFrame,imcStmtVector,entryLabel,exitLabel));
        }

        return null;
    }

    private Vector<ImcStmt> linearize(Vector<ImcStmt> stmts) {
        Vector<ImcStmt> linearStmts = new Vector<ImcStmt>();
        for (int s = 0; s < stmts.size(); s++) {
            ImcStmt stmt = stmts.get(s);
            if (stmt instanceof ImcCJUMP) {
                ImcCJUMP imcCJump = (ImcCJUMP)stmt;
                MemLabel negLabel = new MemLabel();
                linearStmts.add(new ImcCJUMP(imcCJump.cond, imcCJump.posLabel, negLabel));
                linearStmts.add(new ImcLABEL(negLabel));
                linearStmts.add(new ImcJUMP(imcCJump.negLabel));
            }
            else
                linearStmts.add(stmt);
        }
        return linearStmts;
    }
    //* Permute
}

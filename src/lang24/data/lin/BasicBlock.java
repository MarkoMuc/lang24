package lang24.data.lin;

import lang24.common.report.Report;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcStmt;

import java.util.HashSet;
import java.util.Vector;

public class BasicBlock {
    private ImcLABEL fEntry;
    private ImcLABEL fExit;

    private ImcLABEL entry;
    private ImcStmt exit;
    private Vector<ImcStmt> stmts;

    private HashSet<BasicBlock> entryBlockSet;
    private HashSet<BasicBlock> exitBlockSet;

    public BasicBlock(ImcLABEL entry, ImcStmt exit, Vector<ImcStmt> stmts) {
        if(exit != null && !(exit instanceof ImcJUMP || exit instanceof ImcCJUMP)){
            throw new Report.Error("Basic blocks can only end with a CJUMP or JUMP statement.");
        }

        this.entry = entry;
        this.exit = exit;
        this.stmts = stmts;
        this.entryBlockSet = new HashSet<>();
        this.exitBlockSet = new HashSet<>();
    }

    public void addEntryBlock(BasicBlock block) {
        this.entryBlockSet.add(block);
    }

    public void addExitBlock(BasicBlock block) {
        this.exitBlockSet.add(block);
    }

    public HashSet<BasicBlock> getEntryBlockSet() {
        return entryBlockSet;
    }

    public HashSet<BasicBlock> getExitBlockSet() {
        return exitBlockSet;
    }

    public ImcLABEL getEntry() {
        return entry;
    }

    public ImcStmt getExit() {
        return exit;
    }

    public Vector<ImcStmt> getStmts() {
        return stmts;
    }

    public void setEntry(ImcLABEL entry) {
        this.entry = entry;
    }

    public void setExit(ImcStmt exit) {
        this.exit = exit;
    }

    @Override
    public String toString() {
        return String.format("BasicBlock{entry=%s, exit=%s}", entry, exit);
    }
}

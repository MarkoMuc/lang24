package lang24.data.datadep.depgraph;

import lang24.data.ast.tree.stmt.AstStmt;

import java.util.Vector;

public class DDGNode {
    public AstStmt stmt;
    public Vector<DDGConnection> connections;
    public int depth;
    public int stmtNum;

    public DDGNode(AstStmt stmt, int depth, int stmtNum) {
        this.stmt = stmt;
        this.depth = depth;
        this.stmtNum = stmtNum;
        this.connections = new Vector<>();
    }

    public void addConnection(DDGConnection connection) {
        this.connections.add(connection);
    }

    public int getRealStmtNum() {
        return stmtNum + 1;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        sb.append(String.format("NODE(L%dS%d) {\n", this.depth, this.getRealStmtNum()));
        for (var conn : this.connections) {
            sb.append("\t").append(conn).append("\n");
        }
        sb.append("}");

        return sb.toString();
    }
}

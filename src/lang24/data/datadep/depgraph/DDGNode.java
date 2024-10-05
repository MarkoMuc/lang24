package lang24.data.datadep.depgraph;

import lang24.data.ast.tree.stmt.AstStmt;

import java.util.Vector;

/**
 * Class representing the node of a data dependence graph.
 * Each node represents a statement in a loop.
 *
 * @author marko.muc12@gmail.com
 */
public class DDGNode {
    /**
     * The statement this nodes represents.
     **/
    public AstStmt stmt;

    /** All connections of this node equivalent to all dependences where this statement is a source. **/
    public Vector<DDGConnection> connections;

    /** Depth of this statement in a loop nest. **/
    public int depth;

    /** The position of the statement in the loop. **/
    public int stmtNum;

    /**
     * Constructor of a DDGNode.
     *
     * @param stmt      The AST of the statement.
     * @param depth     The depth of the loop containing this node.
     * @param stmtNum   Position of the statement.
     */
    public DDGNode(AstStmt stmt, int depth, int stmtNum) {
        this.stmt = stmt;
        this.depth = depth;
        this.stmtNum = stmtNum;
        this.connections = new Vector<>();
    }

    /**
     * Adds a connection to this node. Where this node is the source.
     *
     * @param connection   Connection to add.
     */
    public void addConnection(DDGConnection connection) {
        this.connections.add(connection);
    }

    /**
     * Returns the real statement number. Statement numbers start with 0 to index the array better.
     *
     * @return Statement number raised by 1.
     */
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

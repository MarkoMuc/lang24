package lang24.data.datadep;

import lang24.data.ast.tree.stmt.AstStmt;

public class SubscriptPair {
    AstStmt sourceStmt;
    AstStmt sinkStmt;
    Subscript sourceSubscript;
    Subscript sinkSubscript;
    public int numberOfIndexes;

    public SubscriptPair(AstStmt sourceStmt, Subscript sourceSubscript, AstStmt sinkStmt, Subscript sinkSubscript) {
        this.sourceStmt = sourceStmt;
        this.sinkStmt = sinkStmt;
        this.sourceSubscript = sourceSubscript;
        this.sinkSubscript = sinkSubscript;
        this.numberOfIndexes = Math.max(sourceSubscript.getVariableCount(), sinkSubscript.getVariableCount());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceSubscript);
        sb.append("->");
        sb.append(sinkSubscript);
        sb.append('{').append(numberOfIndexes).append('}');

        return sb.toString();
    }
}

package lang24.data.datadep.depgraph;

import lang24.data.datadep.DirectionVector;

public class DDGConnection {
    public DDGNode source;
    public DDGNode sink;
    public DirectionVector directionVector;

    public DDGConnection(DDGNode source, DDGNode sink, DirectionVector directionVector) {
        this.source = source;
        this.sink = sink;
        this.directionVector = directionVector;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("->").append(String.format("L%dS%d: ", sink.depth, sink.stmtNum));
        sb.append(directionVector);

        return sb.toString();
    }
}

package lang24.data.datadep.depgraph;

import lang24.data.datadep.ArrRef;
import lang24.data.datadep.deptest.DirectionVector;

//CHECKME: Is it worth it to save both whole ArrRefs?
public class DDGConnection {
    public ArrRef sourceRef;
    public ArrRef sinkRef;
    public DDGNode source;
    public DDGNode sink;
    public DirectionVector directionVector;

    public DDGConnection(ArrRef sinkRef, ArrRef sourceRef, DDGNode source,
                         DDGNode sink, DirectionVector directionVector) {
        this.sourceRef = sourceRef;
        this.sinkRef = sinkRef;
        this.source = source;
        this.sink = sink;
        this.directionVector = directionVector;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("->").append(String.format("L%dS%d |%s|: ", sink.depth, sink.getRealStmtNum(), sourceRef.arrExpr));
        sb.append(directionVector);

        return sb.toString();
    }
}

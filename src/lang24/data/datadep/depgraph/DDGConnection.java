package lang24.data.datadep.depgraph;

import lang24.data.datadep.ArrRef;
import lang24.data.datadep.deptest.DirectionVector;

//CHECKME: Is it worth it to save both whole ArrRefs?

/**
 * Class represents a directed connection or edge of a Data Dependence Graph.
 * A connection connects two statements by its dependence cause by the array referenced.
 *
 * @author marko.muc12@gmail.com
 */
public class DDGConnection {
    /**
     * Array Reference of the source node of the direction.
     **/
    public ArrRef sourceRef;

    /** Array Reference of the sink node of the direction. **/
    public ArrRef sinkRef;

    /** Source statement. **/
    public DDGNode source;

    /** Sink statement. **/
    public DDGNode sink;

    /** Direction vector of this dependence. **/
    public DirectionVector directionVector;

    /**
     * Constructor of a data dependence graph connection.
     *
     * @param sinkRef           Sink array reference of the dependence.
     * @param sourceRef         Source array reference of the dependence.
     * @param source            DDGNode of the source statement.
     * @param sink              DDGNode of the sink statement.
     * @param directionVector   Direction vector of the dependence.
     */
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

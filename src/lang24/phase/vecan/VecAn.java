package lang24.phase.vecan;

import lang24.common.report.Report;
import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.stmt.AstVecForStmt;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.depgraph.DDGNode;
import lang24.data.datadep.depgraph.DataDependenceGraph;
import lang24.data.datadep.deptest.DirectionVectorSet;
import lang24.data.datadep.subscript.Partition;
import lang24.data.datadep.subscript.Subscript;
import lang24.data.datadep.subscript.SubscriptPair;
import lang24.phase.Phase;

import java.util.HashMap;
import java.util.Vector;

import static lang24.data.datadep.deptest.DependenceTests.*;
import static lang24.data.datadep.subscript.Partition.partition;

/**
 * @author marko.muc12@gmail.com
 */

public class VecAn extends Phase {

    // There can be multiple ones bdw
    public final static Attribute<AstNameExpr, LoopDescriptor> loopDescriptors = new Attribute<>();
    public final static Vector<LoopDescriptor> loops = new Vector<>();
    public final static HashMap<AstVecForStmt, DataDependenceGraph> graphs = new HashMap<>();
    public final static HashMap<AstVecForStmt, Vector<AstVecForStmt>> outerToNest = new HashMap<>();
    public final static HashMap<AstExpr, Subscript> exprToSubscript = new HashMap<>();

    public VecAn() {
        super("vecan");
    }

    public void loopAnalysis() {
        for (LoopDescriptor loopDescriptor : loops) {
            int len = loopDescriptor.arrayRefs.size();
            var DDG = new DataDependenceGraph();
            loopRefs:
            for (int i = 0; i < len; i++) {
                ArrRef source = loopDescriptor.arrayRefs.get(i);
                DDG.addDDGNode(new DDGNode(source.refStmt, source.getDepthAsIdx(), source.stmtNum - 1));

                for (int j = i; j < len; j++) {
                    ArrRef sink = loopDescriptor.arrayRefs.get(j);
                    if (i == j && source.assign) {
                        // We are testing self output dependence.
                        if (source.getDepth() <= source.getSubscriptCount()) {
                            // If loops surrounding the ref. is LEQ to num. of subscript self output. cannot exist.
                            continue;
                        }
                    }
                    if (!source.assign && !sink.assign) {
                        // Both source and sink reference are reads
                        continue;
                    }
                    DDG.addDDGNode(new DDGNode(sink.refStmt, sink.getDepthAsIdx(), sink.stmtNum - 1));
                    if (source.equals(sink) && source.getSubscriptCount() == sink.getSubscriptCount()) {
                        var DVSet = new DirectionVectorSet(Math.min(source.getDepth(), sink.getDepth()));
                        var depExists = testDependence(source, sink, DVSet);
                        if (depExists == null) {
                            loopDescriptor.vectorizable = false;
                            break loopRefs;
                        } else if (!depExists) {
                            //FIXME:Why not just continue?
                            DVSet.getDirectionVectors().clear();
                        }
                        var fixedVectors = DVSet.purgeIllegal();

                        if (!fixedVectors.isEmpty()) {
                            DDG.addDVSet(sink, source, new DirectionVectorSet(fixedVectors));
                        }
                        DDG.addDVSet(source, sink, DVSet);
                    }
                }
            }

            if (loopDescriptor.vectorizable) {
                VecAn.graphs.put(loopDescriptor.loop, DDG);
            }
        }

        loops.removeAll(loops.stream().filter(f -> !f.vectorizable).toList());
    }

    private Boolean testDependence(ArrRef source, ArrRef sink, DirectionVectorSet DVSet) {
        // Create and analyze partition pairs
        Vector<SubscriptPair> subscriptPairs = createAndAnalyzeSubscriptPairs(source, sink);
        if (subscriptPairs == null) {
            // No linear pairs found, assume complete dependence
            // TODO: Implement complete dependence in this cases
            return null;
        }

        // Create Partitions
        var partitions = partition(subscriptPairs,
                source.getDepth() > sink.getDepth() ? sink.loop : source.loop);

        //Test separable
        for (var partition : partitions) {
            if (partition.getSize() == 1) {
                if (!testSeparable(partition, DVSet)) {
                    return false;
                }
            } else {
                // TODO: Implement complete dependence in this cases
                throw new Report.Error("Coupled subscript groups are not implemented");
            }
        }

        return true;
    }

    private boolean testSeparable(Partition partition, DirectionVectorSet DVSet) {
        boolean depExists;
        if (partition.getSize() > 1) {
            throw new Report.InternalError();
        }

        var subscriptPair = partition.getPairs().getFirst();
        var DV = new DirectionVectorSet();
        if (subscriptPair.numberOfIndexes == 0) {
            return ZIVTest(subscriptPair);
        } else if (subscriptPair.numberOfIndexes == 1) {
            depExists = SIVTest(subscriptPair, DV);
        } else {
            depExists = MIVTest(subscriptPair, DV);
        }

        if (depExists) {
            mergeVectorsSets(subscriptPair.loopIndexLevels, DVSet, DV);
        } else {
            return false;
        }

        return true;
    }

    // Checks both subscripts and creates a subscript pair
    private Vector<SubscriptPair> createAndAnalyzeSubscriptPairs(ArrRef source, ArrRef sink) {
        var pairs = new Vector<SubscriptPair>();
        var deepestLoop = source.getDepth() > sink.getDepth() ? sink.loop : source.loop;
        var commonLoops = findCommonLoops(source, sink);

        for (int i = 0; i < source.getSubscriptCount(); i++) {
            Subscript sourceSubscript = new Subscript(source);
            AstExpr sourceExpr = source.subscriptExprs.get(i);
            sourceExpr.accept(new SubscriptAnalyzer(), sourceSubscript);

            if (!sourceSubscript.isLinear()) {
                return null;
            }

            Subscript sinkSubscript = new Subscript(sink);
            AstExpr sinkExpr = sink.subscriptExprs.get(i);
            sinkExpr.accept(new SubscriptAnalyzer(), sinkSubscript);

            if (!sourceSubscript.isLinear()) {
                return null;
            }
            VecAn.exprToSubscript.put(sourceExpr, sourceSubscript);
            VecAn.exprToSubscript.put(sinkExpr, sinkSubscript);

            pairs.add(new SubscriptPair(sourceSubscript, sinkSubscript,
                    Math.max(source.getDepth(), sink.getDepth()),
                    Math.min(source.getDepth(), sink.getDepth()),
                    deepestLoop, commonLoops));
        }

        if (pairs.isEmpty()) {
            throw new Report.Error("0 pairs in a subscript pair should not happen.");
        }

        return pairs;
    }

    // Common loops run to the deepest loop that carries the deepest reference of the two.
    private Vector<LoopDescriptor> findCommonLoops(ArrRef source, ArrRef sink) {
        var loops = new Vector<LoopDescriptor>();
        LoopDescriptor stmtLoop;

        if (source.getDepth() > sink.getDepth()) {
            stmtLoop = sink.loop;
        } else {
            stmtLoop = source.loop;
        }

        if (stmtLoop.nest.isEmpty()) {
            // We are at the outermost loop
            loops.add(stmtLoop);
            return loops;
        } else {
            // Add all outer loops
            loops.addAll(stmtLoop.nest);
            // Add innermost common loop
            loops.add(stmtLoop);
        }

        return loops;
    }
}
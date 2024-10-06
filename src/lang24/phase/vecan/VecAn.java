package lang24.phase.vecan;

import lang24.common.report.Report;
import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.codegen.SCCDependenceGraph;
import lang24.data.datadep.depgraph.DDGNode;
import lang24.data.datadep.depgraph.DataDependenceGraph;
import lang24.data.datadep.deptest.DirectionVectorSet;
import lang24.data.datadep.subscript.Partition;
import lang24.data.datadep.subscript.Subscript;
import lang24.data.datadep.subscript.SubscriptPair;
import lang24.phase.Phase;
import lang24.phase.seman.SemAn;

import java.util.Vector;

import static lang24.data.datadep.deptest.DependenceTests.*;
import static lang24.data.datadep.subscript.Partition.partition;


/*
 *  TODO: Subscript pairs
 *   -> Since our language does not directly support multi dimensional arrays, there is always only one subscript pair!
 *  FIXME:
 *   F1) This should also check if they both have the same number of subscripts
 *   F2) Implement MIV
 */

public class VecAn extends Phase {

    public final static Attribute<AstNameExpr, LoopDescriptor> loopDescriptors = new Attribute<>();
    public final static Vector<LoopDescriptor> loops = new Vector<>();

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
                DDG.addDGNode(new DDGNode(source.refStmt, source.depth, source.stmtNum - 1));

                for (int j = i; j < len; j++) {
                    ArrRef sink = loopDescriptor.arrayRefs.get(j);
                    if (!source.assign && !sink.assign) {
                        // Both source and sink reference are reads
                        continue;
                    }
                    DDG.addDGNode(new DDGNode(sink.refStmt, sink.depth, sink.stmtNum - 1));
                    if (source.equals(sink) && source.getSize() == sink.getSize()) {
                        //#F1
                        var DVSet = new DirectionVectorSet(Math.max(source.depth, sink.depth));
                        var depExists = testDependence(source, sink, loopDescriptor, DVSet);
                        if (depExists == null) {
                            loopDescriptor.vectorizable = false;
                            break loopRefs;
                        } else if (!depExists) {
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
                System.out.println(DDG);
                var TSCC = DDG.TarjansSCC();
                System.out.println("TSCC[" + TSCC.size() + "]");
                int i = 1;
                for (var region : TSCC) {
                    System.out.println("REGION[" + i + "," + region.getSize() + "]:");
                    for (var node : region.getNodes()) {
                        System.out.println(node);
                    }
                    i++;
                }
                codegen(0, DDG);
            }
        }

        loops.removeAll(loops.stream().filter(f -> !f.vectorizable).toList());
    }

    private Boolean testDependence(ArrRef source, ArrRef sink,
                                   LoopDescriptor loopDescriptor, DirectionVectorSet DVSet) {
        //TODO: In future this has to go through all idxExpressions

        //Create and analyze partition pairs
        Vector<SubscriptPair> subscriptPairs = createAndAnalyzeSubscriptPair(source, sink);
        if (subscriptPairs == null) {
            return null;
        }

        // Create Partitions
        var partitions = partition(subscriptPairs,
                source.depth > sink.depth ? source.loop : sink.loop);

        //Test separable
        for (var partition : partitions) {
            if (partition.getSize() == 1) {
                if (!testSeparable(partition, DVSet)) {
                    return false;
                }
            } else {
                throw new Report.Error("Coupled subscript groups are not yet implemented");
            }
        }

        return true;
    }

    //TODO: add direction vector set
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
            //#F2
            depExists = MIVTest(subscriptPair, DV);
        }

        if (depExists) {
            mergeVectorsSets(subscriptPair.loopIndexLevels, DVSet, DV);
        } else {
            return false;
        }

        //CHECKME:This returns true in case the linear element does not exist?????
        return true;
    }

    private Vector<SubscriptPair> createAndAnalyzeSubscriptPair(ArrRef source, ArrRef sink) {
        //TODO: If a ref is nonlinear once, it is always non linear
        //      -> Early break/continue whenever this same ArrRef is to be checked

        var pairs = new Vector<SubscriptPair>();

        for (int i = 0; i < source.getSize(); i++) {
            Subscript sourceSubscript = new Subscript(source);
            source.subscriptExprs.get(i).accept(new SubscriptAnalyzer(), sourceSubscript);

            if (!sourceSubscript.isLinear()) {
                return null;
            }

            Subscript sinkSubscript = new Subscript(sink);
            sink.subscriptExprs.get(i).accept(new SubscriptAnalyzer(), sinkSubscript);

            if (!sourceSubscript.isLinear()) {
                return null;
            }
            pairs.add(new SubscriptPair(source.refStmt, sourceSubscript,
                    sink.refStmt, sinkSubscript, Math.max(source.depth, sink.depth)));
        }

        if (pairs.isEmpty()) {
            throw new Report.Error("0 pairs in a subscript pair should not happen.");
        }

        return pairs;
    }

    public void codegen(int k, DataDependenceGraph D) {
        var SCCset = D.TarjansSCC();
        var sccDependenceGraph = new SCCDependenceGraph();
        sccDependenceGraph.addSCCs(SCCset);
        sccDependenceGraph.buildGraph();

        System.out.println(sccDependenceGraph);

        var piblocks = sccDependenceGraph.topologicalSort();
        System.out.println(sccDependenceGraph.toStringSorted());
    }

    // Checks both subscripts and creates a subscript pair
    private Vector<SubscriptPair> createAndAnalyzeSubscriptPair(ArrRef source, ArrRef sink) {
        var pairs = new Vector<SubscriptPair>();
        var deepestLoop = source.depth > sink.depth ? source.loop : sink.loop;
        var commonLoops = findCommonLoops(source, sink);

        for (int i = 0; i < source.getSubscriptCount(); i++) {
            Subscript sourceSubscript = new Subscript(source);
            source.subscriptExprs.get(i).accept(new SubscriptAnalyzer(), sourceSubscript);

            if (!sourceSubscript.isLinear()) {
                return null;
            }

            Subscript sinkSubscript = new Subscript(sink);
            sink.subscriptExprs.get(i).accept(new SubscriptAnalyzer(), sinkSubscript);

            if (!sourceSubscript.isLinear()) {
                return null;
            }

            pairs.add(new SubscriptPair(sourceSubscript, sinkSubscript,
                    Math.max(source.depth, sink.depth), deepestLoop, commonLoops));
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

        if (source.depth > sink.depth) {
            stmtLoop = source.loop;
        } else {
            stmtLoop = sink.loop;
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
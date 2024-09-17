package lang24.phase.vecan;

import lang24.common.report.Report;
import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
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

                for (int j = i + 1; j < len; j++) {
                    ArrRef sink = loopDescriptor.arrayRefs.get(j);
                    if (source.equals(sink)) {
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
            }
        }
        loops.removeAll(loops.stream().filter(f -> !f.vectorizable).toList());
    }

    private Boolean testDependence(ArrRef source, ArrRef sink,
                                   LoopDescriptor loopDescriptor, DirectionVectorSet DVSet) {
        //TODO: In future this has to go through all idxExpressions

        //Create and analyze partition pairs
        SubscriptPair subscriptPair = createAndAnalyzeSubscriptPair(source, sink);
        if (subscriptPair == null) {
            return null;
        }

        // Create Partitions
        Vector<AstDefn> loopIndexes = new Vector<>();
        Vector<LoopDescriptor> nest;

        if (source.depth > sink.depth) {
            nest = source.loop.nest;
            subscriptPair.setLoop(source.loop);
            loopIndexes.addLast(SemAn.definedAt.get(source.loop.loopIndex));
        } else {
            nest = sink.loop.nest;
            subscriptPair.setLoop(sink.loop);
            loopIndexes.addLast(SemAn.definedAt.get(sink.loop.loopIndex));
        }

        for (var loop : nest.reversed()) {
            loopIndexes.addFirst(SemAn.definedAt.get(loop.loopIndex));
        }

        var partitions = partition(new Vector<>() {{
            add(subscriptPair);
        }}, loopIndexes);

        //Test separable
        for (var partition : partitions) {
            if (partition.getSize() == 1) {
                if (!testSeparable(partition, DVSet)) {
                    return false;
                }
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

    private SubscriptPair createAndAnalyzeSubscriptPair(ArrRef source, ArrRef sink) {
        //TODO: If a ref is nonlinear once, it is always non linear
        //      -> Early break/continue whenever this same ArrRef is to be checked
        //TODO: This should create a Vector of SubscriptPairs, one for each position
        Subscript sourceSubscript = new Subscript(source);
        source.subscriptExpr.accept(new SubscriptAnalyzer(), sourceSubscript);
        if (!sourceSubscript.isLinear()) {
            return null;
        }

        Subscript sinkSubscript = new Subscript(sink);
        sink.subscriptExpr.accept(new SubscriptAnalyzer(), sinkSubscript);
        if (!sourceSubscript.isLinear()) {
            return null;
        }

        return new SubscriptPair(source.refStmt, sourceSubscript,
                sink.refStmt, sinkSubscript, Math.max(source.depth, sink.depth));
    }

}
package lang24.phase.vecan;

import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.datadep.*;
import lang24.phase.Phase;
import lang24.phase.seman.SemAn;

import java.util.Vector;


/*
 *   TODO: Subscript pairs
 *       -> Since our language does not directly support multi dimensional arrays, there is always only one subscript pair!
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

            loopRefs:
            for (int i = 0; i < len; i++) {
                ArrRef source = loopDescriptor.arrayRefs.get(i);
                for (int j = i + 1; j < len; j++) {
                    ArrRef sink = loopDescriptor.arrayRefs.get(j);
                    if (source.equals(sink)) {
                        var depExists = testDependence(source, sink, loopDescriptor);
                        if (depExists == null) {
                            loopDescriptor.vectorizable = false;
                            break loopRefs;
                        }
                    }
                }
            }
        }
        loops.removeAll(loops.stream().filter(f -> !f.vectorizable).toList());
    }

    //FIXME: This should return DVset or null if dependence cannot be tested
    private Boolean testDependence(ArrRef source, ArrRef sink, LoopDescriptor loopDescriptor) {
        //TODO: In future this has to go through all idxExpressions
        SubscriptPair subscriptPair = createAndAnalyzeSubscriptPair(source, sink);
        if (subscriptPair == null) {
            return null;
        }
        Vector<AstDefn> loopIndexes = new Vector<>();
        Vector<LoopDescriptor> nest;
        int depth;
        if (source.depth > sink.depth) {
            depth = source.depth;
            nest = source.loop.nest;
            loopIndexes.addLast(SemAn.definedAt.get(source.loop.loopIndex));
        } else {
            depth = sink.depth;
            nest = sink.loop.nest;
            loopIndexes.addLast(SemAn.definedAt.get(sink.loop.loopIndex));

        }

        // Here i should take the most deep one, since it carries its own loop descriptor
        // This loop descriptor holds the outermost ones in order

        loopIndexes.add(SemAn.definedAt.get(loopDescriptor.loopIndex));

        for (var loop : nest.reversed()) {
            loopIndexes.addFirst(SemAn.definedAt.get(loop.loopIndex));
        }

        var partitions = partition(new Vector<>() {{
            add(subscriptPair);
        }}, loopIndexes);

        //System.out.println(subscriptPair);
        for (var part : partitions) {
            System.out.println(part);
        }

        return true;
    }

    private SubscriptPair createAndAnalyzeSubscriptPair(ArrRef source, ArrRef sink) {
        //TODO: If a ref is nonlinear once, it is always non linear
        //      -> Early break/continue whenever this same ArrRef is to be checked

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

        return new SubscriptPair(source.refStmt, sourceSubscript, sink.refStmt, sinkSubscript);
    }

    private Vector<Partition> partition(Vector<SubscriptPair> pairs, Vector<AstDefn> loopIndexes) {
        Vector<Partition> partitions = new Vector<>();
        int numOfPartitions = pairs.size();
        int n = loopIndexes.size();

        for (var pair : pairs) {
            partitions.add(new Partition(pair));
        }

        for (int i = 0; i < n; i++) {
            Integer k = null;
            for (int j = 0; j < numOfPartitions; j++) {
                if (i == j) {
                    continue;
                }
                Partition partition = partitions.get(j);
                if (partition.pairContainsIndex(loopIndexes.get(i))) {
                    if (k == null) {
                        k = j;
                    } else {
                        partitions.get(k).mergePartitions(partition);
                        partitions.remove(partition);
                        numOfPartitions--;
                    }
                }
            }
        }

        return partitions;
    }


}
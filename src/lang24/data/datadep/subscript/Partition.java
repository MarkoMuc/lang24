package lang24.data.datadep.subscript;

import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.datadep.LoopDescriptor;
import lang24.phase.seman.SemAn;

import java.util.Vector;

public class Partition {
    private final Vector<SubscriptPair> pairs;

    public Partition(SubscriptPair pair) {
        this.pairs = new Vector<>();
        this.pairs.add(pair);
    }

    public int getSize() {
        return pairs.size();
    }

    public void mergePartitions(Partition other) {
        this.pairs.addAll(other.pairs);
    }

    public Vector<SubscriptPair> getPairs() {
        return this.pairs;
    }

    public boolean pairContainsIndex(int idxDepth) {
        for (var pair : pairs) {
            if (pair.containsIndex(idxDepth)) {
                return true;
            }
        }

        return false;
    }

    public boolean pairContainsIndex(AstDefn idx, int loopLevel) {
        for (var pair : pairs) {
            if (pair.sourceSubscript.containsIndex(idx, loopLevel)) {
                return true;
            } else if (pair.sinkSubscript.containsIndex(idx, loopLevel)) {
                return true;
            }
        }

        return false;
    }

    public static Vector<Partition> partition(Vector<SubscriptPair> pairs, LoopDescriptor ld) {
        Vector<Partition> partitions = new Vector<>();
        Vector<AstDefn> loopIndexes = new Vector<>();
        int numOfPartitions = pairs.size();

        loopIndexes.addLast(SemAn.definedAt.get(ld.loopIndex));

        for (var loop : ld.nest.reversed()) {
            loopIndexes.addFirst(SemAn.definedAt.get(loop.loopIndex));
        }

        for (var pair : pairs) {
            partitions.add(new Partition(pair));
        }

        int n = loopIndexes.size();
        for (int i = 0; i < n; i++) {
            Integer k = null;
            for (int j = 0; j < numOfPartitions; j++) {
                Partition partition = partitions.get(j);
                if (partition.pairContainsIndex(loopIndexes.get(i), i)) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Partition {\n");
        for (var pair : pairs) {
            sb.append('\t').append(pair).append('\n');
        }
        sb.append("}");

        return sb.toString();
    }
}

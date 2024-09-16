package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;

import java.util.HashSet;
import java.util.Vector;

public class Partition {
    private HashSet<SubscriptPair> pairs;

    public Partition() {
        this.pairs = new HashSet<>();
    }

    public Partition(SubscriptPair pair) {
        this.pairs = new HashSet<>();
        this.pairs.add(pair);
    }

    public void mergePartitions(Partition other) {
        this.pairs.addAll(other.pairs);
    }

    public boolean pairContainsIndex(AstDefn idx) {
        for (var pair : pairs) {
            if (pair.sourceSubscript.containsIndex(idx)) {
                return true;
            } else if (pair.sinkSubscript.containsIndex(idx)) {
                return true;
            }
        }
        return false;
    }


    public static Vector<Partition> partition(Vector<SubscriptPair> pairs, Vector<AstDefn> loopIndexes) {
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

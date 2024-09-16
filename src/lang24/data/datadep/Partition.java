package lang24.data.datadep;

import lang24.data.ast.tree.defn.AstDefn;

import java.util.HashSet;

public class Partition {
    HashSet<SubscriptPair> pairs;

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

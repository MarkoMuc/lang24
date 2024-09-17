package lang24.data.datadep;

import lang24.data.ast.tree.stmt.AstStmt;

import java.util.HashSet;
import java.util.Vector;

public class SubscriptPair {
    public AstStmt sourceStmt;
    public AstStmt sinkStmt;
    public Subscript sourceSubscript;
    public Subscript sinkSubscript;
    private LoopDescriptor loop;
    public int numberOfIndexes;
    public int innermostLevel;
    public Vector<Integer> loopIndexLevels;

    public SubscriptPair(AstStmt sourceStmt, Subscript sourceSubscript,
                         AstStmt sinkStmt, Subscript sinkSubscript, int innermostLevel) {
        this.sourceStmt = sourceStmt;
        this.sinkStmt = sinkStmt;
        this.sourceSubscript = sourceSubscript;
        this.sinkSubscript = sinkSubscript;
        this.numberOfIndexes = getNumberOfIndexes();
        this.loopIndexLevels = createLoopIndexLevels();
        this.innermostLevel = innermostLevel;
    }

    public LoopDescriptor getLoop() {
        return loop;
    }

    public void setLoop(LoopDescriptor loop) {
        this.loop = loop;
    }

    private Vector<Integer> createLoopIndexLevels() {
        HashSet<Integer> levels = new HashSet<>();
        for (var term : this.sourceSubscript.getTerms()) {
            levels.add(term.depth);
        }

        for (var term : this.sinkSubscript.getTerms()) {
            levels.add(term.depth);
        }

        return new Vector<>(levels);
    }

    private int getNumberOfIndexes() {
        int count = this.sourceSubscript.getVariableCount() + this.sinkSubscript.getVariableCount();

        //We assume now all overlapping are already added
        for (var sourceIndex : this.sourceSubscript.getTerms()) {
            for (var sinkIndex : this.sinkSubscript.getTerms()) {
                if (sourceIndex.depth.equals(sinkIndex.depth)
                        && sourceIndex.variable.equals(sinkIndex.variable)) {
                    count--;
                    break;
                }
            }
        }

        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sourceSubscript);
        sb.append("->");
        sb.append(sinkSubscript);
        sb.append('{').append(numberOfIndexes).append('}');

        return sb.toString();
    }
}

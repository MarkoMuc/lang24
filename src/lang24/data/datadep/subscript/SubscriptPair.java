package lang24.data.datadep.subscript;

import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;

import java.util.HashSet;
import java.util.Vector;

/**
 * @author marko.muc12@gmail.com
 */
public class SubscriptPair {
    public Subscript sourceSubscript;
    public Subscript sinkSubscript;
    private LoopDescriptor loop;
    public int numberOfIndexes;
    public int maxNestLevel;
    public int minNestLevel;
    public Vector<Integer> loopIndexLevels;
    public Vector<LoopDescriptor> commonLoops;

    public SubscriptPair(Subscript sourceSubscript, Subscript sinkSubscript,
                         int maxNestLevel, int minNestLevel, LoopDescriptor loop, Vector<LoopDescriptor> commonLoops) {
        this.sourceSubscript = sourceSubscript;
        this.sinkSubscript = sinkSubscript;
        this.loop = loop;
        this.numberOfIndexes = getNumberOfIndexes();
        this.loopIndexLevels = createLoopIndexLevels();
        this.commonLoops = commonLoops;
        this.maxNestLevel = maxNestLevel;
        this.minNestLevel = minNestLevel;
    }

    public LoopDescriptor getLoop() {
        return this.loop;
    }

    public void setLoop(LoopDescriptor loop) {
        this.loop = loop;
    }

    public ArrRef getSourceArrRef() {
        return this.sourceSubscript.getArrRef();
    }

    public ArrRef getSinkArrRef() {
        return this.sinkSubscript.getArrRef();
    }

    public boolean containsIndex(int idxDepth) {
        return this.loopIndexLevels.contains(idxDepth);
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

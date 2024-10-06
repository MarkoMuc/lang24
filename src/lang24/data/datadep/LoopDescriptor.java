package lang24.data.datadep;

import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.stmt.AstVecForStmt;

import java.util.Vector;

/**
 * Class represents the loop header and array references inside the body.
 * Note that the upper bound is lowered by 1 since loop is upper bound exclusive.
 *
 * @author marko.muc12@gmail.com
 */
public class LoopDescriptor {
    /**
     * AstSTMT object of this loop.
     **/
    public AstVecForStmt loop;

    /** AstExpr object of the loop index variable. **/
    public AstExpr loopIndex;

    /**
     * Lower bound of the loop.
     **/
    public int lowerBound;

    /**
     * Upper bound of the loop corrected by 1.
     **/
    public int upperBound;

    /**
     * Step size of the loop.
     **/
    public int step;

    /**
     * Depth of this loop in the loop nest.
     **/
    public int depth;

    /** Array refs of this and all loop nested in it. **/
    public Vector<ArrRef> arrayRefs = new Vector<>();

    /** Loop descriptors of the shallower loops in the nest.**/
    public Vector<LoopDescriptor> nest = new Vector<>();

    /** Flag to indicate the loop is not vectorizable. **/
    public boolean vectorizable;

    /**
     * Constructor for a loop descriptor. A loop descriptor carries the depth, limits and array refs.
     * Each nested loop carries all of its "parent" nests.
     * Note that the upper bound is lowered by 1, since the for loop is upper bound exclusive.
     *
     * @param loop       ASTStmt object of this vfor loop.
     * @param loopIndex  The index variable of this loop. Represented by a AstExpr object.
     * @param lowerBound Lower bound of the loop.
     * @param upperBound Upper bound of the loop.
     * @param step       Step size of the loop.
     */
    public LoopDescriptor(AstVecForStmt loop, AstExpr loopIndex, int lowerBound, int upperBound, int step) {
        this.depth = 1;
        this.loop = loop;
        this.loopIndex = loopIndex;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound - 1;
        this.step = step;
        this.vectorizable = true;
    }

    /**
     * Add this array reference.
     *
     * @param arrayRef An array reference.
     */
    public void addArrayRef(ArrRef arrayRef) {
        this.arrayRefs.add(arrayRef);
    }

    /**
     * Adds all array refs of the inner loop of this loop.
     *
     * @param inner The loop descriptor of the inner loop.
     */
    public void addInnerRefs(LoopDescriptor inner) {
        this.arrayRefs.addAll(inner.arrayRefs);
    }

    /**
     * Adds outer loops of this loop in the nest.
     *
     * @param outer Outer loop.
     */
    public void addOuterLoops(LoopDescriptor outer) {
        this.depth = outer.depth + 1;
        addLoops(outer.nest);
        addLoop(outer);
    }

    /**
     * Adds a loop to the loop nest.
     *
     * @param loop Loop descriptor to add.
     */
    public void addLoop(LoopDescriptor loop) {
        if(loop != null){
            this.nest.add(loop);
        }
    }

    /**
     * Adds multiple loops to the loop nest.
     * @param loops Loop descriptors to add.
     */
    public void addLoops(Vector<LoopDescriptor> loops) {
        if (loops != null) {
            this.nest.addAll(loops);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LoopDescriptor {");
        sb.append("\n\t vectorizable= ").append(vectorizable);
        sb.append("\n\t depth= ").append(depth);
        sb.append("\n\t loopIndex= ").append(loopIndex);
        sb.append("\n\t lowerBound= ").append(lowerBound);
        sb.append("\n\t upperBound= ").append(upperBound);
        sb.append("\n\t step= ").append(step);
        sb.append("\n\t arrayRefs= {\n");
        for(ArrRef arrayRef : arrayRefs){
            sb.append("\t\t").append(arrayRef).append("\n");
        }
        sb.append("\t}");
        sb.append("\n}\n");
        return sb.toString();
    }
}

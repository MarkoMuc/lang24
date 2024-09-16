package lang24.data.datadep;

import lang24.data.ast.tree.expr.AstExpr;
import lang24.data.ast.tree.stmt.AstVecForStmt;

import java.util.Vector;

public class LoopDescriptor {
    public int depth;
    public AstVecForStmt loop;
    public AstExpr loopIndex;
    public AstExpr lowerBound;
    public AstExpr upperBound;
    public AstExpr step;
    public Vector<ArrRef> arrayRefs = new Vector<>();
    //Nest contains all loops BEFORE this one
    public Vector<LoopDescriptor> nest = new Vector<>();
    public boolean vectorizable;

    public LoopDescriptor(AstVecForStmt loop, AstExpr loopIndex, AstExpr lowerBound, AstExpr upperBound, AstExpr step) {
        this.depth = 0;
        this.loop = loop;
        this.loopIndex = loopIndex;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.step = step;
        this.vectorizable = true;
    }

    public void addArrayRef(ArrRef arrayRef) {
        this.arrayRefs.add(arrayRef);
    }

    public void addInner(LoopDescriptor inner) {
        this.arrayRefs.addAll(inner.arrayRefs);
    }

    public void addOuter(LoopDescriptor outer) {
        this.depth = outer.depth + 1;
        addLoops(outer.nest);
        addLoop(outer);
    }

    public void addLoop(LoopDescriptor loop) {
        if(loop != null){
            this.nest.add(loop);
        }
    }

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
        //FIXME: remove nested level should be same as depth
        sb.append("\n\t nestedLevel= ").append(nest.size());
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

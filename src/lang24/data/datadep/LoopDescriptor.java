package lang24.data.datadep;

import lang24.data.ast.tree.expr.AstExpr;

import java.util.Vector;

public class LoopDescriptor {
    public int depth;
    public AstExpr loopIndex;
    public AstExpr lowerBound;
    public AstExpr upperBound;
    public AstExpr step;
    public Vector<ArrRef> arrayRefs = new Vector<>();
    public Vector<LoopDescriptor> nest = new Vector<>();
    public boolean vectorizable;

    public LoopDescriptor(int depth, AstExpr loopIndex,
                          AstExpr lowerBound, AstExpr upperBound, AstExpr step,
                            Vector<LoopDescriptor> nested) {
        this.depth = depth;
        this.loopIndex = loopIndex;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.step = step;
        this.vectorizable = true;
        if(nested != null){
            addLoops(nest);
        }
    }

    public void addArrayRef(ArrRef arrayRef) {
        this.arrayRefs.add(arrayRef);
    }

    public void addLoop(LoopDescriptor loop) {
        if(loop != null){
            this.nest.add(loop);
        }
    }

    public void addLoops(Vector<LoopDescriptor> loops) {
        this.nest.addAll(loops);
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

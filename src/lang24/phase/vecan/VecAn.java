package lang24.phase.vecan;

import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.stmt.AstStmt;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.Subscript;
import lang24.phase.Phase;

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

            for (int i = 0; i < len; i++) {
                ArrRef source = loopDescriptor.arrayRefs.get(i);
                for (int j = i + 1; j < len; j++) {
                    ArrRef sink = loopDescriptor.arrayRefs.get(j);
                    if (source.equals(sink)) {
                        testDependence(source, sink, loopDescriptor);

                    }
                }
            }
        }
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


    //FIXME: This should return DVset or null if dependence cannot be tested
    private void testDependence(ArrRef source, ArrRef sink, LoopDescriptor loopDescriptor) {
        SubscriptPair subscriptPair = createAndAnalyzeSubscriptPair(source, sink);
        if (subscriptPair == null) {
            return;
        }
        System.out.println(subscriptPair);

    }

    private class SubscriptPair {
        AstStmt sourceStmt;
        AstStmt sinkStmt;
        Subscript sourceSubscript;
        Subscript sinkSubscript;
        public int numberOfIndexes;

        SubscriptPair(AstStmt sourceStmt, Subscript sourceSubscript, AstStmt sinkStmt, Subscript sinkSubscript) {
            this.sourceStmt = sourceStmt;
            this.sinkStmt = sinkStmt;
            this.sourceSubscript = sourceSubscript;
            this.sinkSubscript = sinkSubscript;
            this.numberOfIndexes = Math.max(sourceSubscript.variableCount, sinkSubscript.variableCount);
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
}

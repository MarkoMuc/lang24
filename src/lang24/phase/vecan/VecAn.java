package lang24.phase.vecan;

import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.stmt.AstVecForStmt;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.Subscript;
import lang24.data.datadep.SubscriptAnalyzer;
import lang24.phase.Phase;

import java.util.Vector;

public class VecAn extends Phase {

    //CHECKME: Is this needed?
    public final static Attribute<AstVecForStmt, LoopDescriptor> loopDescriptors = new Attribute<>();
    public final static Vector<LoopDescriptor> loops = new Vector<>();


    public VecAn(){
        super("vecan");
    }

    public void createRefPairs() {
        Vector<Vector<RefPair>> loopPairs = new Vector<>();
        for (LoopDescriptor l : loops) {
            Vector<RefPair> pairs = new Vector<>();

            int len = l.arrayRefs.size();

            for (int i = 0; i < len; i++) {
                ArrRef source = l.arrayRefs.get(i);
                for (int j = i + 1; j < len; j++) {
                    ArrRef sink = l.arrayRefs.get(j);
                    if (source.equals(sink)) {
                        pairs.add(new RefPair(source, sink));
                    }
                }
            }
            loopPairs.add(pairs);
        }
        analyzeSubscript(loopPairs);
    }

    private void analyzeSubscript(Vector<Vector<RefPair>> loopPairs) {
        for (Vector<RefPair> pars : loopPairs) {
            for (RefPair pair : pars) {
                Subscript s1 = new Subscript();
                pair.source.subscriptExpr.accept(new SubscriptAnalyzer(), s1);
                Subscript s2 = new Subscript();
                pair.sink.subscriptExpr.accept(new SubscriptAnalyzer(), s2);
                s1.collect();
                s2.collect();
                System.out.println(s1);
                System.out.println(s2);
            }
        }
    }

    private class RefPair {
        ArrRef source;
        ArrRef sink;

        RefPair(ArrRef source, ArrRef sink) {
            this.source = source;
            this.sink = sink;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(source.arrExpr).append("[").append(source.subscriptExpr).append("]");
            sb.append("->");
            sb.append(sink.arrExpr).append("[").append(sink.subscriptExpr).append("]");

            return sb.toString();
        }
    }
}

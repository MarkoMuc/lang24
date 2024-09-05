package lang24.phase.vecan;

import lang24.data.ast.attribute.Attribute;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.Subscript;
import lang24.phase.Phase;

import java.util.Vector;

public class VecAn extends Phase {

    public final static Attribute<AstNameExpr, LoopDescriptor> loopDescriptors = new Attribute<>();
    public final static Vector<LoopDescriptor> loops = new Vector<>();


    public VecAn() {
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
            analyzeSubscript(pairs, l);
        }
    }

    private void analyzeSubscript(Vector<RefPair> loopPairs, LoopDescriptor loop) {
        for (RefPair pair : loopPairs) {
            Subscript s1 = new Subscript(pair.source);
            pair.source.subscriptExpr.accept(new SubscriptAnalyzer(), s1);
            Subscript s2 = new Subscript(pair.sink);
            pair.sink.subscriptExpr.accept(new SubscriptAnalyzer(), s2);
            s1.collect();
            s2.collect();
            System.out.print(s1);
            System.out.print("->");
            System.out.print(s2);
            System.out.print(" vs ");
            System.out.print(s1.toString2());
            System.out.print("->");
            System.out.println(s2.toString2());
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
            String sb = source.arrExpr + "[" + source.subscriptExpr + "]" +
                    "->" +
                    sink.arrExpr + "[" + sink.subscriptExpr + "]";

            return sb;
        }
    }
}

package lang24.data.datadep.deptest;

import lang24.common.report.Report;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.datadep.subscript.SubscriptPair;

import java.util.Objects;
import java.util.Vector;

public class DependenceTests {

    public static void mergeVectorsSets(Vector<Integer> levels, DirectionVectorSet DVSet, DirectionVectorSet DV) {
        var newDVSet = new DirectionVectorSet();
        int nI = levels.size();

        //CHECKME: Does simplify here work?
        for (int i = 0; i < DV.size(); i++) {
            for (int j = 0; j < DVSet.size(); j++) {
                //CHECKME: DO I really need to create a copy here???
                var thisDV = DVSet.getDirectionVector(j).copy();
                for (int k = 0; k < nI; k++) {
                    //CHECKME: Why does the proposed algorithm use .get(k)?
                    thisDV.direction.set(levels.get(k), DV.getDirectionVector(i).direction.get(levels.get(k)));
                }
                newDVSet.addDirectionVector(thisDV);
            }
        }
        DVSet.setDirectionVectors(newDVSet.getDirectionVectors());
    }

    public static boolean ZIVTest(SubscriptPair pair) {
        return pair.sourceSubscript.getConstant().coefficient.equals(pair.sinkSubscript.getConstant().coefficient);
    }

    //FIXME: fix SIV detection -> SIV MEANS ONLY ONE LOOP!
    public static Boolean SIVTest(SubscriptPair pair, DirectionVectorSet DV) {
        var sourceIndex = pair.sourceSubscript.getVariable(0);
        var sinkIndex = pair.sinkSubscript.getVariable(0);
        var sourceConstant = pair.sourceSubscript.getConstant();
        var sinkConstant = pair.sinkSubscript.getConstant();

        var upperBound = Integer.parseInt(((AstAtomExpr) pair.getLoop().upperBound).value) - 1;
        var lowerBound = Integer.parseInt(((AstAtomExpr) pair.getLoop().lowerBound).value);

        if (sinkIndex == null && sourceIndex == null) {
            throw new Report.Error("This should be a ZIV test");
        }

        DirectionVector vector;
        if (sourceIndex == null) {
            // Same as sourceIndex.coefficient == 0
            vector = weakZeroSIVTest(sourceConstant.coefficient - sinkConstant.coefficient,
                    sinkIndex.coefficient, upperBound, lowerBound);
        } else if (sinkIndex == null) {
            // Same as sinkIndex.coefficient == 0
            vector = weakZeroSIVTest(sinkConstant.coefficient - sourceConstant.coefficient,
                    sourceIndex.coefficient, upperBound, lowerBound);
        } else if (sinkIndex.coefficient.equals(sourceIndex.coefficient)) {
            vector = strongSIVTest(sourceConstant.coefficient - sinkConstant.coefficient,
                    sourceIndex.coefficient, upperBound, lowerBound);
        } else if (sinkIndex.coefficient.equals(-sourceIndex.coefficient)) {
            vector = weakCrossingSIVTest(sinkConstant.coefficient - sourceConstant.coefficient,
                    sourceIndex.coefficient, upperBound, lowerBound);
        } else {
            //TODO: Here should be an Exact SIV Test
            //FIXME: until done should return null to indicate that this loop will not be vectorized
            throw new Report.Error("Exact SIV Test not implemented");
            //return null;
        }

        int depth = Objects.requireNonNullElse(sinkIndex, sourceIndex).depth;

        if (vector != null) {
            vector.generateDirection(pair.innermostLevel, depth);
            DV.addDirectionVector(vector);
            return true;
        }

        return false;
    }


    private static DirectionVector weakCrossingSIVTest(int constantDifference, int coeffcient, int upperLimit, int lowerLimit) {
        coeffcient = 2 * coeffcient;
        float i = (float) constantDifference / (float) coeffcient;

        if (constantDifference % coeffcient == 0 || i % 1 == 0.5) {
            if (Math.abs(i) <= (upperLimit - lowerLimit)) {
                //CHECKME: Is this cool?
                return new DirectionVector(Math.round(i));
            }
        }

        return null;
    }

    private static DirectionVector weakZeroSIVTest(int constantDifference, int coeffcient, int upperLimit, int lowerLimit) {
        if (constantDifference % coeffcient != 0) {
            return null;
        }

        int i = constantDifference / coeffcient;

        if (Math.abs(i) <= (upperLimit - lowerLimit)) {
            return new DirectionVector(i);
        }

        return null;
    }

    //FIXME:Create direction vector
    private static DirectionVector strongSIVTest(int constantDiff, int coefficient, int upperLimit, int lowerLimit) {
        if (constantDiff % coefficient != 0) {
            return null;
        }

        int dependenceDistance = constantDiff / coefficient;
        if (Math.abs(dependenceDistance) <= (upperLimit - lowerLimit)) {
            return new DirectionVector(dependenceDistance);
        }

        return null;
    }

    public static boolean MIVTest(SubscriptPair pair, DirectionVectorSet DV) {
        //TODO:Implement delta test
        throw new Report.Error("MIV Test not implemented");
    }
}

package lang24.data.datadep.deptest;

import lang24.common.report.Report;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.subscript.Subscript;
import lang24.data.datadep.subscript.SubscriptPair;
import lang24.data.datadep.subscript.Term;

import java.util.Collection;
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
                    thisDV.changeDirection(levels.get(k), DV.getDirectionVector(i).getDirection(levels.get(k)));
                    //thisDV.directions.set(levels.get(k), DV.getDirectionVector(i).directions.get(levels.get(k)));
                }
                newDVSet.addDirectionVector(thisDV);
            }
        }
        DVSet.setDirectionVectors(newDVSet.getDirectionVectors());
    }

    public static boolean ZIVTest(SubscriptPair pair) {
        return pair.sourceSubscript.getConstant().coefficient.equals(pair.sinkSubscript.getConstant().coefficient);
    }

    //FIXME: fix SIV detection -> SIV MEANS ONLY ONE LOOP! Not really? Only means it uses just one variable
    public static Boolean SIVTest(SubscriptPair pair, DirectionVectorSet DV) {
        var sourceIndex = pair.sourceSubscript.getVariable(0);
        var sinkIndex = pair.sinkSubscript.getVariable(0);
        var sourceConstant = pair.sourceSubscript.getConstant();
        var sinkConstant = pair.sinkSubscript.getConstant();

        // Lower bound is raised by 1, otherwise it does not find correct dependence
        var upperBound = Integer.parseInt(((AstAtomExpr) pair.getLoop().upperBound).value);
        var lowerBound = Integer.parseInt(((AstAtomExpr) pair.getLoop().lowerBound).value) + 1;

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

    private static DirectionVector strongSIVTest(int constDiff, int coefficient, int upperLimit, int lowerLimit) {
        if (constDiff % coefficient != 0) {
            return null;
        }

        int dependenceDistance = constantDiff / coefficient;
        if (Math.abs(dependenceDistance) <= (upperLimit - lowerLimit)) {
            return new DirectionVector(dependenceDistance);
        }

        return null;
    }

    private static boolean BanerjeeSelfSIVTest(Subscript source, Subscript sink, int upperBound) {
        //FIXME: how do i set direction vector?
        if (!GCDTest(source, sink)) {
            return false;
        }

        var a1 = source.getVariable(0).coefficient;
        var a0 = source.getConstant().coefficient;
        var b1 = sink.getVariable(0).coefficient;
        var b0 = sink.getConstant().coefficient;

        var leftSide = -b1 - Zpos((Zneg(a1) + b1)) * (upperBound - 2);
        var rightSide = -b1 + Zpos((Zpos(a1) - b1)) * (upperBound - 2);
        var middle = a0 + a1 - b0 - b1;

        return leftSide <= middle && middle <= rightSide;
    }

    public static boolean MIVTest(SubscriptPair pair, DirectionVectorSet DVset) {
        if (!GCDTest(pair.sourceSubscript, pair.sinkSubscript)) {
            return false;
        } else {
            //FIXME:what gotta do here is this correct?
            var dvlist = new DirectionVectorSet();
            var startingDV = DirectionVector.generateStartingDV(pair.innermostLevel);
            dvlist = MIVDirectionVectorTest(pair, startingDV, 0, dvlist);
            DVset.addDirectionVectors(dvlist);
        }
        return true;
    }

    private static DirectionVectorSet MIVDirectionVectorTest(SubscriptPair pair, DirectionVector DV,
                                                             int depth, DirectionVectorSet DVlist) {
        if (!BanerjeeTest(pair, DV)) {
            return DVlist;
        }
        if (depth == pair.innermostLevel) {
            //CHECKME: Merge or unite???
            DVlist.addDirectionVector(DV);
            return DVlist;
        }
        var dirs = new Vector<DependenceDirection.Direction>() {{
            add(DependenceDirection.Direction.LESS);
            add(DependenceDirection.Direction.EQU);
            add(DependenceDirection.Direction.MORE);
        }};

        for (var dir : dirs) {
            //FIXME: what is i here? -> its depth right?
            DV.setDirection(dir, depth);
            DVlist = MIVDirectionVectorTest(pair, DV, depth + 1, DVlist);
        }
        return DVlist;
    }

    private static boolean GCDTest(Subscript source, Subscript sink) {
        var gcdValue = gcdMultiple(source.getTerms(), sink.getTerms());
        return (sink.getConstant().coefficient - source.getConstant().coefficient) / gcdValue == 0;
    }

    private static boolean BanerjeeTest(SubscriptPair pair, DirectionVector DV) {
        var source = pair.sourceSubscript;
        var sink = pair.sinkSubscript;
        var constantDiff = source.getConstant().coefficient - sink.getConstant().coefficient;

        var loopLimits = new Vector<>(pair.getLoop().nest);
        loopLimits.add(pair.getLoop());

        var bounds = calculateBounds(pair, DV, loopLimits);
        return bounds.leftSide <= constantDiff && constantDiff <= bounds.rightSide;
    }

    private static Bounds calculateBounds(SubscriptPair pair, DirectionVector DV, Vector<LoopDescriptor> loopLimits) {
        //DIR vector "contains" all possible variables, so you need to make sure
        // that the ones in inside are in the correct order!
        //FIXME: arrRef should be used to make sure that we are talking about the correct loop
        //          Im pretty sure that currently loop nest just carries all possible loops
        //          And if 2 loops are on the same level, then we run into an issue
        //          since the 2 refs are same depth, but carry different loop descriptors
        //          and the loop nest in them will be different(each will be missing the other ones loop)
        //          Or the nest might contain all of the loops aka too many of them? -> Check this!
        //          Also how do you handle the case, where one of them is a loop deeper?
        var directions = DV.getDirections();
        var bound = new Bounds(0, 0);

        var lowerLoops = loopLimits.stream()
                .map(t -> t.lowerBound)
                .map(t -> ((AstAtomExpr) t).value)
                .toList();
        var upperLoops = loopLimits.stream()
                .map(t -> t.upperBound)
                .map(t -> ((AstAtomExpr) t).value)
                .toList();

        var source = pair.sourceSubscript;
        var sink = pair.sinkSubscript;
        int sr_j = 0;
        int si_j = 0;

        for (int i = 0; i < directions.size(); i++) {
            var direction = directions.get(i);
            //FIXME: THIS

            var sourceTerm = source.getVariable(sr_j);
            var sinkTerm = sink.getVariable(si_j);
            var sourceCoef = 0;
            var sinkCoef = 0;

            if (sourceTerm != null && sourceTerm.depth == i) {
                sourceCoef = sourceTerm.coefficient;
                sr_j++;
            }

            if (sinkTerm != null && sinkTerm.depth == i) {
                sinkCoef = sinkTerm.coefficient;
                si_j++;
            }

            if (direction.direction == DependenceDirection.Direction.LESS) {
                var lowerBound = Integer.parseInt(lowerLoops.get(i));
                var upperBound = Integer.parseInt(upperLoops.get(i));
                var partialBound = calculateLessDirection(sourceCoef, sinkCoef,
                        lowerBound, upperBound);
                bound.leftSide += partialBound.leftSide;
                bound.rightSide += partialBound.rightSide;
            } else if (direction.direction == DependenceDirection.Direction.MORE) {
                var lowerBound = Integer.parseInt(lowerLoops.get(i));
                var upperBound = Integer.parseInt(upperLoops.get(i));
                var partialBound = calculateMoreDirection(sourceCoef, sinkCoef,
                        lowerBound, upperBound);
                bound.leftSide += partialBound.leftSide;
                bound.rightSide += partialBound.rightSide;
            } else if (direction.direction == DependenceDirection.Direction.EQU) {
                var lowerBound = Integer.parseInt(lowerLoops.get(i));
                var upperBound = Integer.parseInt(upperLoops.get(i));
                var partialBound = calculateEquDirection(sourceCoef, sinkCoef,
                        lowerBound, upperBound);
                bound.leftSide += partialBound.leftSide;
                bound.rightSide += partialBound.rightSide;
            } else {
                //FIXME: this loop limits
                var sourceLowerBound = Integer.parseInt(lowerLoops.get(i));
                var sourceUpperBound = Integer.parseInt(upperLoops.get(i));
                var sinkLowerBound = Integer.parseInt(lowerLoops.get(i));
                var sinkUpperBound = Integer.parseInt(upperLoops.get(i));

                var partialBound = calculateStarDirection(sourceCoef, sinkCoef,
                        sourceLowerBound, sourceUpperBound, sinkLowerBound, sinkUpperBound);
                bound.leftSide += partialBound.leftSide;
                bound.rightSide += partialBound.rightSide;
            }
        }

        return bound;
    }

    private static Bounds calculateLessDirection(Integer a, Integer b, Integer L, Integer U) {
        //FIXME: how do we take into account that it probably expect for Li to be >= 1?
        //          could do + 1 and do -1 afterwards?
        var leftSide = -Zpos((Zneg(a) + b)) * (U - 1) + (Zneg((Zneg(a) + b)) + Zpos(a)) * L - b;
        var rightSide = Zpos((Zpos(a) + b)) * (U - 1) - (Zneg((Zpos(a) + b)) + Zneg(a)) * L - b;

        return new Bounds(leftSide, rightSide);
    }

    private static Bounds calculateMoreDirection(Integer a, Integer b, Integer L, Integer U) {
        //FIXME: how do we take into account that it probably expect for Li to be >= 1?
        //          could do + 1 and do -1 afterwards?
        var leftSide = -Zneg(a - Zpos(b)) * (U - 1) + (Zpos((a - Zpos(b))) + Zneg(b)) * L + a;
        var rightSide = Zpos(a - Zneg(b)) * (U - 1) + (Zneg((a - Zneg(b))) + Zpos(b)) * L + a;

        return new Bounds(leftSide, rightSide);
    }

    private static Bounds calculateEquDirection(Integer a, Integer b, Integer L, Integer U) {
        //FIXME: how do we take into account that it probably expect for Li to be >= 1?
        //          could do + 1 and do -1 afterwards?
        var leftSide = -Zneg(a - b) * U + Zpos(a - b) * L;
        var rightSide = Zpos(a - b) * U - Zneg(a - b) * L;

        return new Bounds(leftSide, rightSide);
    }

    private static Bounds calculateStarDirection(Integer a, Integer b,
                                                 Integer Lx, Integer Ux, Integer Ly, Integer Uy) {
        //FIXME: how do we take into account that it probably expect for Li to be >= 1?
        //          could do + 1 and do -1 afterwards?
        var leftSide = -Zneg(a) * Ux + Zpos(a) * Lx - Zpos(b) * Uy + Zneg(b) * Ly;
        var rightSide = Zpos(a) * Ux - Zneg(a) * Lx - Zneg(b) * Uy - Zpos(b) * Ly;

        return new Bounds(leftSide, rightSide);
    }

    public static int gcd(int a, int b) {
        while (b != 0) {
            var temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    public static int gcdMultiple(Collection<Term> source, Collection<Term> sink) {
        var allValues = new java.util.ArrayList<>(source
                .stream()
                .map(t -> t.coefficient)
                .toList());
        allValues.addAll(sink
                .stream()
                .map(t -> t.coefficient)
                .toList());
        var result = allValues.getFirst();
        allValues.removeFirst();
        for (var b : allValues) {
            result = gcd(result, b);
        }

        return result;
    }

    private static int Zneg(int n) {
        //FIXME: Check if true!
        if (n > 0) {
            return 0;
        } else {
            return -n;
        }
    }

    private static int Zpos(int n) {
        if (n >= 0) {
            return 0;
        } else {
            return n;
        }
    }

    private static class Bounds {
        Integer leftSide;
        Integer rightSide;

        public Bounds(int leftSide, int rigthtSide) {
            this.leftSide = leftSide;
            this.rightSide = rigthtSide;
        }
    }

}

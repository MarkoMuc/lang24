package lang24.data.datadep.deptest;

import lang24.common.report.Report;
import lang24.data.datadep.LoopDescriptor;
import lang24.data.datadep.subscript.Subscript;
import lang24.data.datadep.subscript.SubscriptPair;
import lang24.data.datadep.subscript.Term;

import java.util.Collection;
import java.util.Objects;
import java.util.Vector;

/**
 * @author marko.muc12@gmail.com
 */

public class DependenceTests {

    public static void mergeVectorsSets(Vector<Integer> levels, DirectionVectorSet DVSet, DirectionVectorSet DV) {
        var newDVSet = new DirectionVectorSet();
        int nI = levels.size();

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

    public static Boolean SIVTest(SubscriptPair pair, DirectionVectorSet DV) {
        var sourceIndex = pair.sourceSubscript.getVariable(0);
        var sinkIndex = pair.sinkSubscript.getVariable(0);
        var sourceConstant = pair.sourceSubscript.getConstant();
        var sinkConstant = pair.sinkSubscript.getConstant();

        // Depth is the nesting level of the loop index variable we are testing
        final int depth = Objects.requireNonNullElse(sourceIndex, sinkIndex).depth;

        // Since SIV is tied to a single loop variable, get that loop variable
        var commonLoop = Objects.requireNonNullElse(sourceIndex, sinkIndex).loop;

        var upperBound = commonLoop.upperBound;
        var lowerBound = commonLoop.lowerBound;

        if (sinkIndex == null && sourceIndex == null) {
            throw new Report.Error("This should be a ZIV test");
        }

        Integer dependenceDistance;
        if (sourceIndex == null) {
            // Same as sourceIndex.coefficient == 0
            dependenceDistance = weakZeroSIVTest(sourceConstant.coefficient - sinkConstant.coefficient,
                    sinkIndex.coefficient, upperBound, lowerBound);
        } else if (sinkIndex == null) {
            // Same as sinkIndex.coefficient == 0
            dependenceDistance = weakZeroSIVTest(sinkConstant.coefficient - sourceConstant.coefficient,
                    sourceIndex.coefficient, upperBound, lowerBound);
        } else if (sinkIndex.coefficient.equals(sourceIndex.coefficient)) {
            dependenceDistance = strongSIVTest(sourceConstant.coefficient - sinkConstant.coefficient,
                    sourceIndex.coefficient, upperBound, lowerBound);
        } else if (sinkIndex.coefficient.equals(-sourceIndex.coefficient)) {
            dependenceDistance = weakCrossingSIVTest(sinkConstant.coefficient - sourceConstant.coefficient,
                    sourceIndex.coefficient, upperBound, lowerBound);
        } else {
            //FIXME: until done should return null to indicate that this loop will not be vectorized
            throw new Report.Error("Exact SIV Test not implemented");
        }

        if (dependenceDistance != null) {
            DV.addDirectionVector(new DirectionVector(dependenceDistance, pair.minNestLevel, depth));
            return true;
        }

        return false;
    }

    private static Integer weakCrossingSIVTest(int constantDifference, int coeffcient, int upperLimit, int lowerLimit) {
        coeffcient = 2 * coeffcient;
        float i = (float) constantDifference / (float) coeffcient;

        if (constantDifference % coeffcient == 0 || Math.round(i) == i) {
            if (Math.abs(i) <= (upperLimit - lowerLimit)) {
                // FIXME: the rounding can cause a wrong direction?
                return Math.round(i);
            }
        }

        return null;
    }

    private static Integer weakZeroSIVTest(int constDiff, int coefficient, int upperLimit, int lowerLimit) {
        if (constDiff % coefficient != 0) {
            return null;
        }

        int i = constDiff / coefficient;

        if (Math.abs(i) <= (upperLimit - lowerLimit)) {
            return i;
        }

        return null;
    }

    private static Integer strongSIVTest(int constDiff, int coefficient, int upperLimit, int lowerLimit) {
        if (constDiff % coefficient != 0) {
            return null;
        }

        int dependenceDistance = constDiff / coefficient;
        if (Math.abs(dependenceDistance) <= (upperLimit - lowerLimit)) {
            return dependenceDistance;
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
            var dvlist = new DirectionVectorSet();
            var startingDV = DirectionVector.generateStartingDV(pair.maxNestLevel);
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

        if (depth == pair.minNestLevel - 1) {
            //CHECKME: Merge or unite???
            //FIXME: This is unite!
            DVlist.addDirectionVector(DV);
            return DVlist;
        }
        var dirs = new Vector<DependenceDirection.Direction>() {{
            add(DependenceDirection.Direction.LESS);
            add(DependenceDirection.Direction.EQU);
            add(DependenceDirection.Direction.MORE);
        }};

        for (var dir : dirs) {
            DV.changeDirection(depth + 1, dir);
            DVlist = MIVDirectionVectorTest(pair, DV, depth + 1, DVlist);
        }
        return DVlist;
    }

    private static boolean GCDTest(Subscript source, Subscript sink) {
        var gcdValue = gcdMultiple(source.getTerms(), sink.getTerms());
        return (sink.getConstant().coefficient - source.getConstant().coefficient) % gcdValue == 0;
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
        //          Does this actually work correctly, we only need to calculate as many as the deepest variable
        var directions = DV.getDirections();
        var bound = new Bounds(0, 0);

        var lowerLoops = loopLimits.stream()
                .map(t -> t.lowerBound)
                .toList();
        var upperLoops = loopLimits.stream()
                .map(t -> t.upperBound)
                .toList();

        var source = pair.sourceSubscript;
        var sink = pair.sinkSubscript;
        int sr_j = 0;
        int si_j = 0;

        for (int i = 0; i < directions.size(); i++) {
            var direction = directions.get(i);

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

            var partialBound = new Bounds(0, 0);

            if (direction.direction != DependenceDirection.Direction.STAR) {
                var lowerBound = lowerLoops.get(i);
                var upperBound = upperLoops.get(i);
                if (direction.direction == DependenceDirection.Direction.LESS) {
                    partialBound = calculateLessDirection(sourceCoef, sinkCoef,
                            lowerBound, upperBound);
                } else if (direction.direction == DependenceDirection.Direction.MORE) {
                    partialBound = calculateMoreDirection(sourceCoef, sinkCoef,
                            lowerBound, upperBound);
                } else if (direction.direction == DependenceDirection.Direction.EQU) {
                    partialBound = calculateEquDirection(sourceCoef, sinkCoef,
                            lowerBound, upperBound);
                }
            } else {
                //FIXME: this loop limits
                var sourceLowerBound = lowerLoops.get(i);
                var sourceUpperBound = upperLoops.get(i);
                var sinkLowerBound = lowerLoops.get(i);
                var sinkUpperBound = upperLoops.get(i);

                partialBound = calculateStarDirection(sourceCoef, sinkCoef,
                        sourceLowerBound, sourceUpperBound, sinkLowerBound, sinkUpperBound);
            }

            bound.leftSide += partialBound.leftSide;
            bound.rightSide += partialBound.rightSide;
        }

        return bound;
    }

    private static Bounds calculateLessDirection(Integer a, Integer b, Integer L, Integer U) {
        var leftSide = -Zpos((Zneg(a) + b)) * (U - 1) + (Zneg((Zneg(a) + b)) + Zpos(a)) * L - b;
        var rightSide = Zpos((Zpos(a) + b)) * (U - 1) - (Zneg((Zpos(a) + b)) + Zneg(a)) * L - b;

        return new Bounds(leftSide, rightSide);
    }

    private static Bounds calculateMoreDirection(Integer a, Integer b, Integer L, Integer U) {
        var leftSide = -Zneg(a - Zpos(b)) * (U - 1) + (Zpos((a - Zpos(b))) + Zneg(b)) * L + a;
        var rightSide = Zpos(a - Zneg(b)) * (U - 1) + (Zneg((a - Zneg(b))) + Zpos(b)) * L + a;

        return new Bounds(leftSide, rightSide);
    }

    private static Bounds calculateEquDirection(Integer a, Integer b, Integer L, Integer U) {
        var leftSide = -Zneg(a - b) * U + Zpos(a - b) * L;
        var rightSide = Zpos(a - b) * U - Zneg(a - b) * L;

        return new Bounds(leftSide, rightSide);
    }

    private static Bounds calculateStarDirection(Integer a, Integer b,
                                                 Integer Lx, Integer Ux, Integer Ly, Integer Uy) {
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
        if (n > 0) {
            return 0;
        } else {
            return -n;
        }
    }

    private static int Zpos(int n) {
        if (n >= 0) {
            return n;
        } else {
            return 0;
        }
    }

    private static class Bounds {
        Integer leftSide;
        Integer rightSide;

        public Bounds(int leftSide, int rightSide) {
            this.leftSide = leftSide;
            this.rightSide = rightSide;
        }
    }

}

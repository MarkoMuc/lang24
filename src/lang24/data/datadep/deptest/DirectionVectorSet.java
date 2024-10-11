package lang24.data.datadep.deptest;

import java.util.Vector;

/**
 * @author marko.muc12@gmail.com
 */
public class DirectionVectorSet {
    private Vector<DirectionVector> directionVectors;

    public DirectionVectorSet() {
        this.directionVectors = new Vector<>();
    }

    public DirectionVectorSet(Vector<DirectionVector> directionVectors) {
        this.directionVectors = directionVectors;
    }

    public DirectionVectorSet(int size) {
        this.directionVectors = new Vector<>();
        this.directionVectors.add(DirectionVector.generateStartingDV(size));
    }

    public void addDirectionVector(int idx, DirectionVector directionVector) {
        if (idx > directionVectors.size()) {
            directionVectors.setSize(idx + 1);
        }
        this.directionVectors.add(idx, directionVector);
    }

    public void addDirectionVector(DirectionVector directionVector) {
        if (!this.containsDirectionVector(directionVector)) {
            this.directionVectors.add(directionVector);
        }
    }

    public Vector<DirectionVector> purgeIllegal() {
        Vector<DirectionVector> toFix = new Vector<>();
        for (var vec : this.directionVectors) {
            for (var direction : vec.getDirections()) {
                if (direction.direction != DependenceDirection.Direction.EQU) {
                    // Left most component is > -> Illegal
                    if (direction.direction == DependenceDirection.Direction.MORE) {
                        toFix.add(vec);
                        break;
                    }
                }
            }
        }
        this.directionVectors.removeAll(toFix);

        for (var vec : toFix) {
            //CHECKME: check if this works correctly
            var tmpVec = new Vector<>(vec.size);
            for (var direction : vec.getDirections()) {
                if (direction.direction == DependenceDirection.Direction.MORE) {
                    direction.direction = DependenceDirection.Direction.LESS;
                } else if (direction.direction == DependenceDirection.Direction.LESS) {
                    direction.direction = DependenceDirection.Direction.MORE;
                }
            }
        }

        return toFix;
    }

    public int size() {
        return this.directionVectors.size();
    }

    public DirectionVector getDirectionVector(int j) {
        return this.directionVectors.get(j);
    }

    public Vector<DirectionVector> getDirectionVectors() {
        return this.directionVectors;
    }

    public void setDirectionVectors(Vector<DirectionVector> directionVectors) {
        this.directionVectors = directionVectors;
    }

    public void addDirectionVectors(DirectionVectorSet dvlist) {
        for (var vects : dvlist.directionVectors) {
            this.addDirectionVector(vects);
        }
    }

    private boolean containsDirectionVector(DirectionVector directionVector) {
        for (DirectionVector dv : this.directionVectors) {
            if (dv.getSize() == directionVector.getSize()) {
                var same = true;

                for (int i = 0; i < dv.getSize(); i++) {
                    if (dv.getDirection(i).equals(directionVector.getDirection(i))) {
                        same = false;
                    }
                }

                if (same) {
                    return true;
                }
            }
        }

        return false;
    }
}

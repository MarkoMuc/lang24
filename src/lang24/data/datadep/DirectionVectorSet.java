package lang24.data.datadep;

import java.util.Vector;

public class DirectionVectorSet {
    private Vector<DirectionVector> directionVectors;

    public DirectionVectorSet() {
        this.directionVectors = new Vector<>();
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
        this.directionVectors.add(directionVector);
    }

    public void purgeIllegal() {

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
}

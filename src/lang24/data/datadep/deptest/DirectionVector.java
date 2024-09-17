package lang24.data.datadep.deptest;

import java.util.Collections;
import java.util.Vector;

public class DirectionVector {
    public Vector<DependenceDirection> directions;
    public int startDistance;
    public int size;

    public DirectionVector() {
    }

    public DirectionVector(int startDistance) {
        this.startDistance = startDistance;
    }

    private DirectionVector(Vector<DependenceDirection> starting) {
        this.directions = starting;
        this.size = starting.size();
        this.startDistance = 0;
    }

    public void setDirections(Vector<DependenceDirection> directions) {
        this.directions = directions;
    }

    public void generateDirection(int size, int level) {
        this.size = size + 1;
        this.directions = createDirection(this.startDistance, this.size, level);
    }

    public Vector<DependenceDirection> createDirection(int distance, int size, int level) {
        Vector<DependenceDirection> direction = new Vector<>(
                Collections.nCopies(size, new DependenceDirection(DependenceDirection.Direction.STAR)));
        direction.set(level, createDirection(distance));

        return direction;
    }

    public static DirectionVector generateStartingDV(int size) {
        return new DirectionVector(new Vector<>(
                Collections.nCopies(size + 1, new DependenceDirection(DependenceDirection.Direction.STAR))));
    }

    public DependenceDirection createDirection(int distance) {
        return new DependenceDirection(DependenceDirection.findDirection(distance), distance);
    }

    public DirectionVector copy() {
        var copy = new DirectionVector();
        copy.directions = new Vector<>();
        copy.directions.addAll(this.directions);
        copy.size = this.size;
        copy.startDistance = this.startDistance;

        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append('(');
        for (int i = 0; i < this.size; i++) {
            sb.append(this.directions.get(i));

            if (i != this.size - 1) {
                sb.append(',');
            }
        }
        sb.append(')');

        return sb.toString();
    }
}

package lang24.data.datadep.deptest;

import java.util.Collections;
import java.util.Vector;

public class DirectionVector {
    private Vector<DependenceDirection> directions;
    public int startDistance;
    public int size;
    public int loopLevel;

    public DirectionVector() {
    }

    public DirectionVector(int startDistance) {
        this.startDistance = startDistance;
    }

    private DirectionVector(Vector<DependenceDirection> starting) {
        this.directions = starting;
        this.size = starting.size();
        this.startDistance = 0;
        this.loopLevel = findLoopLevel();
    }

    private int findLoopLevel() {
        var first = this.directions
                .stream()
                .filter(d -> d.direction != DependenceDirection.Direction.EQU)
                .findFirst()
                .orElse(null);

        return this.directions.indexOf(first);
    }

    public void changeDirection(int idx, DependenceDirection newDirection) {
        this.directions.set(idx, newDirection);
        if (idx <= this.loopLevel || this.loopLevel == -1) {
            this.loopLevel = findLoopLevel();
        }
    }

    public DependenceDirection getDirection(int idx) {
        return this.directions.get(idx);
    }

    public void setDirections(Vector<DependenceDirection> directions) {
        this.directions = directions;
        this.loopLevel = findLoopLevel();
    }

    public void generateDirection(int size, int level) {
        this.size = size + 1;
        this.directions = createDirection(this.startDistance, this.size, level);
        this.loopLevel = findLoopLevel();
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
        copy.loopLevel = this.loopLevel;

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
        sb.append(';').append(loopLevel).append(')');

        return sb.toString();
    }

    public Vector<DependenceDirection> getDirections() {
        return this.directions;
    }
}

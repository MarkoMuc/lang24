package lang24.data.datadep.deptest;

import java.util.Collections;
import java.util.Vector;

public class DirectionVector {
    private Vector<DependenceDirection> directions;
    public int size;
    public int depLevel;

    public DirectionVector() {
    }

    public DirectionVector(int distance, int size, int level) {
        this.size = size;
        this.directions = createDirectionVector(distance, this.size, level);
        this.depLevel = findDependenceLevel();
    }

    private DirectionVector(Vector<DependenceDirection> starting) {
        this.directions = starting;
        this.size = starting.size();
        this.depLevel = findDependenceLevel();
    }

    public void setDirections(Vector<DependenceDirection> directions) {
        this.directions = directions;
        this.depLevel = findDependenceLevel();
    }

    public Vector<DependenceDirection> getDirections() {
        return this.directions;
    }

    public DependenceDirection getDirection(int idx) {
        idx = idx - 1;
        return this.directions.get(idx);
    }

    public int getSize() {
        return this.size;
    }

    /**
     * Changes a certain direction
     *
     * @param idx          direction to change
     * @param newDirection new direction as an direction enum
     */
    public void changeDirection(int idx, DependenceDirection.Direction newDirection) {
        idx = idx - 1;
        if (idx >= this.directions.size()) {
            return;
        }

        this.directions.set(idx, new DependenceDirection(newDirection));

        if (idx <= this.depLevel || this.depLevel == -1) {
            this.depLevel = findDependenceLevel();
        }
    }

    /**
     * Changes a certain direction
     *
     * @param idx          direction to change
     * @param newDirection new direction as an object
     */
    public void changeDirection(int idx, DependenceDirection newDirection) {
        idx = idx - 1;
        if (idx >= this.directions.size()) {
            return;
        }

        this.directions.set(idx, newDirection);

        if (idx <= this.depLevel || this.depLevel == -1) {
            this.depLevel = findDependenceLevel();
        }
    }

    /**
     * @param distance first distance
     * @param size     size of the vector
     * @param level    loop level for which the distance has been calculated
     * @return the vector of directions
     */
    public Vector<DependenceDirection> createDirectionVector(int distance, int size, int level) {
        level = level - 1;
        Vector<DependenceDirection> direction = new Vector<>(
                Collections.nCopies(size, new DependenceDirection(DependenceDirection.Direction.STAR)));
        direction.set(level, createDirection(distance));

        return direction;
    }

    /**
     * Creates a dependence direction from distance
     * @return dependence direction
     */
    public DependenceDirection createDirection(int distance) {
        return new DependenceDirection(DependenceDirection.findDirection(distance), distance);
    }

    /**
     * Creates a deep copy of the direction vector
     *
     * @return copy of the direction vector
     */
    public DirectionVector copy() {
        var copy = new DirectionVector();
        var direction = new Vector<DependenceDirection>(this.size);
        for (var dir : this.directions) {
            direction.add(new DependenceDirection(dir.direction, dir.distance));
        }

        copy.setDirections(direction);
        copy.size = this.size;
        copy.depLevel = this.depLevel;

        return copy;
    }

    /**
     * Finds first non "=" direction which represents the dependence level.
     * Note that the loop level is raised by 1, to reflect theory.
     *
     * @return dependence level
     */
    private int findDependenceLevel() {
        var first = this.directions
                .stream()
                .filter(d -> d.direction != DependenceDirection.Direction.EQU)
                .findFirst()
                .orElse(null);

        return this.directions.indexOf(first) + 1;
    }

    /**
     * Creates a starting direction vector of length size.
     * Starting vector contains only "*" [*, *, ..., *].
     *
     * @param size size of the vector to generate
     */
    public static DirectionVector generateStartingDV(int size) {
        return new DirectionVector(new Vector<>(
                Collections.nCopies(size, new DependenceDirection(DependenceDirection.Direction.STAR))));
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
        sb.append(';').append(depLevel).append(')');

        return sb.toString();
    }
}

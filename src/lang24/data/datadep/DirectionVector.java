package lang24.data.datadep;

import java.util.Collections;
import java.util.Vector;

public class DirectionVector {
    public enum DependenceDirection {
        EQU,
        LESS,
        MORE,
        STAR
    }

    public Vector<DependenceDirection> direction;
    public int startDistance;
    public int size;

    public DirectionVector() {
    }

    public DirectionVector(int startDistance) {
        this.startDistance = startDistance;
    }

    public void generateDirection(int size, int level) {
        this.size = size + 1;
        this.direction = createDirection(this.startDistance, this.size, level);
    }

    public Vector<DependenceDirection> createDirection(int distance, int size, int level) {
        Vector<DependenceDirection> direction = new Vector<>(Collections.nCopies(size, DependenceDirection.STAR));
        direction.set(level, findDirection(distance));

        return direction;
    }

    public DependenceDirection findDirection(int distance) {
        if (distance == 0) {
            return DependenceDirection.EQU;
        } else if (distance < 0) {
            return DependenceDirection.MORE;
        } else {
            return DependenceDirection.LESS;
        }
    }

    public DirectionVector copy() {
        var copy = new DirectionVector();
        copy.direction = new Vector<>();
        copy.direction.addAll(this.direction);
        copy.size = this.size;
        copy.startDistance = 0;

        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append('(');
        for (int i = 0; i < this.size; i++) {
            sb.append(switch (this.direction.get(i)) {
                case EQU -> "=" + "[" + startDistance + "]";
                case LESS -> "<" + "[" + startDistance + "]";
                case MORE -> ">" + "[" + startDistance + "]";
                case STAR -> "*";
            });

            if (i != this.size - 1) {
                sb.append(',');
            }
        }
        sb.append(')');

        return sb.toString();
    }
}

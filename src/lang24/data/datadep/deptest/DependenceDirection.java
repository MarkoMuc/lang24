package lang24.data.datadep.deptest;

public class DependenceDirection {

    public enum Direction {
        EQU,
        LESS,
        MORE,
        STAR
    }

    public Direction direction;
    public Integer distance;

    public DependenceDirection(Direction direction) {
        this.direction = direction;
        this.distance = null;
    }

    public DependenceDirection(Direction direction, Integer distance) {
        this.direction = direction;
        this.distance = distance;
    }

    public static DependenceDirection.Direction findDirection(int distance) {
        if (distance == 0) {
            return DependenceDirection.Direction.EQU;
        } else if (distance < 0) {
            return DependenceDirection.Direction.MORE;
        } else {
            return DependenceDirection.Direction.LESS;
        }
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public void setDistance(Integer distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        sb.append(switch (this.direction) {
            case EQU -> "=";
            case LESS -> "<";
            case MORE -> ">";
            default -> "*";
        });

        if (this.direction != Direction.STAR && distance != null) {
            sb.append("[").append(distance).append("]");
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DependenceDirection dir) {
            if (this.direction != dir.direction) {
                return false;
            }
            if (this.distance == null) {
                return dir.distance == null;
            } else if (dir.distance == null) {
                return false;
            } else {
                return this.distance.equals(dir.distance);
            }

        }
        return false;
    }
}

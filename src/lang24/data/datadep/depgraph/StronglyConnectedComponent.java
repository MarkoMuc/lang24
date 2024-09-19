package lang24.data.datadep.depgraph;

import java.util.Collection;
import java.util.Vector;

public class StronglyConnectedComponent {
    private Vector<DDGNode> nodes;

    public StronglyConnectedComponent(Collection<DDGNode> nodes) {
        this.nodes = new Vector<>(nodes);
    }

    public int getSize() {
        return nodes.size();
    }

    public Vector<DDGNode> getNodes() {
        return nodes;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        for (var node : nodes) {
            sb.append(node.toString()).append('\n');
        }

        return sb.toString();
    }

    public String toShortString() {
        var sb = new StringBuilder();

        for (var node : nodes) {
            sb.append("-> NODE(L").append(node.depth).append("S").append(node.stmtNum + 1).append(")\n");
        }

        return sb.toString();
    }

}

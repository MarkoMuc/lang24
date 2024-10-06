package lang24.data.datadep.depgraph;

import java.util.Collection;
import java.util.Vector;

/**
 * Class representing the strongly connected components of a data dependence graph.
 *
 * @author marko.muc12@gmail.com
 */
public class StronglyConnectedComponent {
    /**
     * Nodes that are part of this SCC.
     **/
    private Vector<DDGNode> nodes;

    /**
     * Constructor of a SCC.
     *
     * @param nodes A collection of the nodes that are part of this SCC.
     */
    public StronglyConnectedComponent(Collection<DDGNode> nodes) {
        this.nodes = new Vector<>(nodes);
    }

    /**
     * @return Number of nodes in this SCC.
     */
    public int getSize() {
        return nodes.size();
    }

    /**
     * @return Vector of nodes in this SCC.
     */
    public Vector<DDGNode> getNodes() {
        return nodes;
    }

    /**
     * @return A shortened string of this object.
     */
    public String toShortString() {
        var sb = new StringBuilder();

        for (var node : nodes) {
            sb.append("-> NODE(L").append(node.depth + 1).append("S").append(node.stmtNum + 1).append(")\n");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();

        for (var node : nodes) {
            sb.append(node.toString()).append('\n');
        }

        return sb.toString();
    }
}

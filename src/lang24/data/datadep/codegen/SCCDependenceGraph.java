package lang24.data.datadep.codegen;

import lang24.common.report.Report;
import lang24.data.datadep.depgraph.StronglyConnectedComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

/**
 * Class representing a dependence graph where:
 * - Vertices are Strongly Connected Components
 * - Edges are dependences spanning from one node inside an SCC
 * to another node inside another SCC
 * - Resulting graph is an acyclic dependence graph
 *
 * @author marko.muc12@gmail.com
 */
public class SCCDependenceGraph {
    /** Graph represented by a map that relates SCC with its edges. **/
    private HashMap<StronglyConnectedComponent, HashSet<StronglyConnectedComponent>> graph;

    /** Topological sort of the graph. **/
    private Vector<StronglyConnectedComponent> sorted;

    /** Keeps from adding additional nodes after it is built once. **/
    private boolean addedNodes;

    /**
     * Creates an empty dependence graph.
     **/
    public SCCDependenceGraph() {
        this.graph = new HashMap<>();
        this.sorted = new Vector<>();
        this.addedNodes = false;
    }

    /**
     * Adds all the SCCs to the graph.
     * Throws an error if nodes have been added already.
     *
     * @param TSCCset Set of SCCs.
     */
    public void addSCCs(Vector<StronglyConnectedComponent> TSCCset) {
        if (this.addedNodes) {
            throw new Report.Error("SCCDependenceGraph: SCCs already added.");
        }

        for (var component : TSCCset) {
            this.graph.put(component, new HashSet<>());
        }

        this.addedNodes = true;
    }

    /**
     * Helper method for building the graph one component at a time.
     *
     * @param component The SCC component to add.
     */
    private void connectSSCs(StronglyConnectedComponent component) {
        var connections = this.graph.get(component);
        for (var DGGNode : component.getNodes()) {
            var outsideConnections = DGGNode.connections
                    .stream()
                    .filter(conn -> !component.getNodes().contains(conn.sink))
                    .toList();
            for (var DDConnection : outsideConnections) {
                for (var scc : this.graph.keySet()) {
                    if (scc == component) {
                        continue;
                    }
                    if (scc.getNodes().contains(DDConnection.sink)) {
                        connections.add(scc);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Builds the graph out of the SCCs.
     * If SCCs have not been added it throws an Error.
     */
    public void buildGraph() {
        if (!this.addedNodes) {
            throw new Report.Error("SCCDependenceGraph: SCCs have not been added yet.");
        }

        for (var component : this.graph.keySet()) {
            connectSSCs(component);
        }
    }

    /**
     * Depth first search used in topological sort.
     *
     * @param node          Current node.
     * @param flatGraph    Vector of all SCCs.
     * @param mark          Array of marks.
     */
    private void DFS(StronglyConnectedComponent node, Vector<StronglyConnectedComponent> flatGraph, boolean[] mark) {
        int i = flatGraph.indexOf(node);
        mark[i] = true;

        for (var neighbor : this.graph.get(node)) {
            if (!mark[flatGraph.indexOf(neighbor)]) {
                DFS(neighbor, flatGraph, mark);
            }
        }

        sorted.addFirst(node);
    }

    /**
     * Topologically sorts the graph and stores it in the sorted vector.
     *
     * @return Vector of sorted nodes.
     */
    public Vector<StronglyConnectedComponent> topologicalSort() {
        if (!this.sorted.isEmpty()) {
            return this.sorted;
        }

        var flatGraph = new Vector<>(this.graph.keySet());
        var mark = new boolean[flatGraph.size()];

        this.sorted = new Vector<>(flatGraph.size());

        for (var SCComponent : flatGraph) {
            if (!mark[flatGraph.indexOf(SCComponent)]) {
                DFS(SCComponent, flatGraph, mark);
            }
        }

        return this.sorted;
    }

    /**
     * toString method for the sorted nodes.
     *
     * @return String of the sorted graph.
     */
    public String toStringSorted() {
        var sb = new StringBuilder();
        var SCCs = this.sorted;

        sb.append("Sorted D_SCC[").append(SCCs.size()).append("]").append('\n');

        for (var scc : SCCs) {
            int i = SCCs.indexOf(scc);
            sb.append("SCC").append(i).append("\n");
            for (var conn : this.graph.get(scc)) {
                sb.append("=>").append(SCCs.indexOf(conn)).append("\n");
            }
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        var SCCs = new Vector<>(this.graph.keySet());

        sb.append("D_SCC[").append(SCCs.size()).append("]").append('\n');

        for (var scc : SCCs) {
            int i = SCCs.indexOf(scc);
            sb.append("SCC").append(i).append("\n");
            sb.append(scc.toShortString());
            for (var conn : this.graph.get(scc)) {
                sb.append("=>").append(SCCs.indexOf(conn)).append("\n");
            }
        }

        return sb.toString();
    }
}

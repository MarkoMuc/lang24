package lang24.data.datadep.codegen;

import lang24.common.report.Report;
import lang24.data.datadep.depgraph.StronglyConnectedComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class SCCDependenceGraph {
    private HashMap<StronglyConnectedComponent, HashSet<StronglyConnectedComponent>> graph;
    private boolean addedNodes = false;
    private Vector<StronglyConnectedComponent> sorted;

    public SCCDependenceGraph() {
        this.graph = new HashMap<>();
        this.sorted = new Vector<>();
    }

    public void addSCCs(Vector<StronglyConnectedComponent> TSCCset) {
        for (var component : TSCCset) {
            this.graph.put(component, new HashSet<>());
        }
        this.addedNodes = true;
    }

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

    public void buildGraph() {
        if (!this.addedNodes) {
            throw new Report.InternalError();
        }

        for (var component : this.graph.keySet()) {
            connectSSCs(component);
        }
    }


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
}

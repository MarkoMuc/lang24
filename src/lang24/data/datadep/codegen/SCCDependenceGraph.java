package lang24.data.datadep.codegen;

import lang24.common.report.Report;
import lang24.data.datadep.depgraph.StronglyConnectedComponent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;

public class SCCDependenceGraph {
    private HashMap<StronglyConnectedComponent, HashSet<StronglyConnectedComponent>> graph;
    private boolean addedNodes = false;

    public SCCDependenceGraph() {
        this.graph = new HashMap<>();
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

package lang24.data.datadep.depgraph;

import lang24.data.ast.tree.stmt.AstStmt;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.deptest.DirectionVectorSet;

import java.util.HashMap;
import java.util.Vector;

public class DataDependenceGraph {
    private HashMap<Integer, Vector<DDGNode>> graph;

    public DataDependenceGraph() {
        this.graph = new HashMap<>();
    }

    public void addDGNode(DDGNode node) {
        var loop = graph.getOrDefault(node.depth, new Vector<>(10));
        if (node.stmtNum >= loop.size()) {
            loop.setSize(node.stmtNum + 1);
        }
        loop.set(node.stmtNum, node);
        graph.put(node.depth, loop);
    }

    public DDGNode getDGNode(int depth, AstStmt stmt) {
        var loop = graph.getOrDefault(depth, null);
        if (loop != null) {
            for (var node : loop) {
                if (node != null && node.stmt == stmt) {
                    return node;
                }
            }
        }

        return null;
    }

    public void addDVSet(ArrRef source, ArrRef sink, DirectionVectorSet DVSet) {
        var fstNode = getDGNode(source.depth, source.refStmt);
        var sndNode = getDGNode(sink.depth, sink.refStmt);

        if (fstNode == null) {
            fstNode = new DDGNode(source.refStmt, source.depth, source.stmtNum - 1);
        }

        if (sndNode == null) {
            sndNode = new DDGNode(sink.refStmt, sink.depth, sink.stmtNum - 1);
        }

        for (var dirVect : DVSet.getDirectionVectors()) {
            var conn = new DDGConnection(fstNode, sndNode, dirVect);
            fstNode.addConnection(conn);
        }
        addDGNode(fstNode);
        addDGNode(sndNode);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        for (var loops : graph.keySet()) {
            sb.append("L").append(loops).append("\n");
            for (var node : graph.get(loops)) {
                if (node != null) {
                    sb.append('\t').append(node).append("\n");
                }
            }
        }

        return sb.toString();
    }
}

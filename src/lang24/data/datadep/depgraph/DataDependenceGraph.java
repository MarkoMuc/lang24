package lang24.data.datadep.depgraph;

import lang24.data.ast.tree.stmt.AstStmt;
import lang24.data.datadep.ArrRef;
import lang24.data.datadep.deptest.DirectionVectorSet;

import java.util.*;
import java.util.stream.Collectors;

public class DataDependenceGraph {
    private HashMap<Integer, Vector<DDGNode>> graph;
    private Vector<Vector<DDGNode>> TSCC = new Vector<>();

    public DataDependenceGraph() {
        this.graph = new HashMap<>();
    }

    public void addDGNode(DDGNode node) {
        var loop = graph.getOrDefault(node.depth, new Vector<>(10));
        if (node.stmtNum >= loop.size()) {
            loop.setSize(node.stmtNum + 1);
        } else {
            if (loop.get(node.stmtNum) != null) {
                return;
            }
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
        // "stmtNum - 1" is done to index the DDG Vector better, otherwise position 0 for every loop would be never used
        if (fstNode == null) {
            fstNode = new DDGNode(source.refStmt, source.depth, source.stmtNum - 1);
            addDGNode(fstNode);
        }

        if (sndNode == null) {
            sndNode = new DDGNode(sink.refStmt, sink.depth, sink.stmtNum - 1);
            addDGNode(sndNode);
        }

        for (var dirVect : DVSet.getDirectionVectors()) {
            var conn = new DDGConnection(source, sink, fstNode, sndNode, dirVect);
            fstNode.addConnection(conn);
        }
    }

    public Collection<DDGNode> DFS(Vector<DDGNode> flatGraph, DDGNode node, Vector<Integer> num, Vector<Integer> lowest, Vector<DDGNode> visited,
                                   int counter, Vector<DDGNode> processed, Stack<DDGNode> stack) {
        int nodeNum = flatGraph.indexOf(node);
        /* 1. Mark as visited */
        visited.add(node);
        /* 2. Initialize num */
        num.set(nodeNum, counter);
        /* 3. Initialize lowest */
        lowest.set(nodeNum, counter);

        counter++;
        stack.push(node);

        /* 4. Visit neighbors */
        for (var conn : node.connections) {
            var neighbor = conn.sink;
            var neighborNum = flatGraph.indexOf(neighbor);
            if (!visited.contains(neighbor)) {
                /* 4. Not yet visited */
                //What if it meets itself?
                DFS(flatGraph, neighbor, num, lowest, visited, counter, processed, stack);
                lowest.set(nodeNum, Math.min(lowest.get(nodeNum), lowest.get(neighborNum)));
            } else if (!processed.contains(neighbor)) {
                /* 5. Visited but not processed neighbors -> back edge*/
                lowest.set(nodeNum, Math.min(lowest.get(nodeNum), num.get(neighborNum)));
            }
        }

        /* 6. Mark as processed */
        processed.add(node);

        /* 7. If true, v is the starting vertex of its SCC */
        if (num.get(nodeNum).equals(lowest.get(nodeNum))) {
            var SCC = new HashSet<DDGNode>();
            while (stack.peek() != node) {
                SCC.add(stack.pop());
            }
            SCC.add(stack.pop());
            this.TSCC.add(new Vector<>(SCC));
            return SCC;
        }

        return null;
    }

    public Vector<Vector<DDGNode>> TarjansSCC() {
        var vertexes = graph.values()
                .stream()
                .flatMap(Vector::stream)
                .collect(Collectors.toCollection(Vector::new));
        var num = new Vector<>(Collections.nCopies(vertexes.size(), -1));
        var lowest = new Vector<>(Collections.nCopies(vertexes.size(), -1));
        var visited = new Vector<DDGNode>();
        var processed = new Vector<DDGNode>();
        var counter = 0;
        var stack = new Stack<DDGNode>();

        for (var node : vertexes) {
            if (!visited.contains(node)) {
                DFS(vertexes, node, num, lowest, visited, counter, processed, stack);
            }
        }
        return this.TSCC;
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

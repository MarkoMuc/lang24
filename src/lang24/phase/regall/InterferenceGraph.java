package lang24.phase.regall;

import lang24.data.mem.MemTemp;

import java.util.HashSet;

public class InterferenceGraph {

    private final HashSet<IFGNode> nodes;

    InterferenceGraph() {
        nodes = new HashSet<>();
    }

    public int getSize() {
        return this.nodes.size();
    }

    public HashSet<IFGNode> getNodes() {
        return new HashSet<IFGNode>(nodes);
    }

    public IFGNode getLowDegreeNode(int numRegs) {
        for (IFGNode node : this.nodes) {
            if (node.getDegree() < numRegs) {
                return node;
            }
        }
        return null;
    }

    public IFGNode getHighDegreeNode(int numRegs) {
        for (IFGNode node : this.nodes) {
            if (node.getDegree() >= numRegs) {
                return node;
            }
        }
        return null;
    }

    public IFGNode findNode(MemTemp temp) {
        for (IFGNode node : this.nodes) {
            if (node.getTemp() == temp) {
                return node;
            }
        }

        return null;
    }

    public void addNode(IFGNode node) {
        for (IFGNode n2 : this.nodes) {
            if (n2.equals(node)) {
                return;
            }
        }
        this.nodes.add(node);
    }

    public void removeNode(IFGNode node) {
        if (node == null) {
            return;
        }
        this.nodes.remove(node);
        for (IFGNode i : node.getConnections()) {
            i.removeConnection(node);
        }
    }
}

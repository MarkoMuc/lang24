package lang24.phase.regall;

import lang24.data.mem.MemTemp;

import java.util.HashSet;
import java.util.Vector;

public class InterferenceGraph {
    private HashSet<IFGNode> nodes;

    InterferenceGraph(){
        this.nodes = new HashSet<>();
    }

    InterferenceGraph(HashSet<IFGNode> nodes){
        this.nodes = nodes;
    }

    public Integer getSize() { return this.nodes.size();}

    public HashSet<IFGNode> getNodes() {
        return nodes;
    }

    public void addNode(IFGNode node) {
        for(IFGNode n : nodes){
            if(n.equals(node)){
                return;
            }
        }
        this.nodes.add(node);
    }

    public void addNodes(Vector<IFGNode> nodes) {
        for(IFGNode n : nodes){
            this.addNode(n);
        }
    }

    public IFGNode findNode(MemTemp temp) {
        for(IFGNode n : this.nodes){
            if(n.getTemp().equals(temp)){
                return n;
            }
        }

        return null;
    }
    
    public void removeNode(IFGNode node) {
        if(node == null){
            return;
        }
        this.nodes.remove(node);
        for(IFGNode n : node.getConnectionsCopy()){
            n.removeConnection(node);
        }
    }

    public IFGNode getLowDegreeNode(int maxDegree) {
        for (IFGNode node : this.nodes) {
            if (node.degree() < maxDegree) {
                return node;
            }
        }
        return null;
    }

    public IFGNode getHighDegreeNode(int minDegree) {
        for (IFGNode node : this.nodes) {
            if (node.degree() >= minDegree) {
                return node;
            }
        }
        return null;
    }

    public HashSet<IFGNode> getNodesCopy() {
        return new HashSet<IFGNode>(nodes);
    }

}

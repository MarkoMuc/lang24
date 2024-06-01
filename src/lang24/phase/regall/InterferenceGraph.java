package lang24.phase.regall;

import lang24.data.mem.*;
import java.util.*;

public class InterferenceGraph {

	private HashSet<IFGNode> IFGNodes = new HashSet<IFGNode>();

	public void addNode(IFGNode n) {
		for (IFGNode n2 : this.IFGNodes)
			if (n2.equals(n)) return;
		this.IFGNodes.add(n);
	}

	public void addNodes(Set<IFGNode> _IFG_nodes) {
		for (IFGNode n : _IFG_nodes)
			this.addNode(n);
	}

	public IFGNode findNode(MemTemp temp) {
		for (IFGNode n : this.IFGNodes)
			if (n.id() == temp) return n;
		return null;
	}

	public void removeNode(IFGNode n) {
		if (n == null) return;
		this.IFGNodes.remove(n);
		for (IFGNode i : n.connections()) {
			i.delConnection(n);
		}
	}

	public IFGNode getLowDegreeNode(int maxDegree) {
		for (IFGNode n : this.IFGNodes)
			if (n.degree() < maxDegree)
				return n;
		return null;
	}

	public IFGNode getHighDegreeNode(int minDegree) {
		for (IFGNode n : this.IFGNodes)
			if (n.degree() >= minDegree)
				return n;
		return null;
	}

	public int size() {
		return this.IFGNodes.size();
	}

	public HashSet<IFGNode> nodes() {
		return new HashSet<IFGNode>(IFGNodes);
	}

	public void print() {
		System.out.println("Graph nodes: " + this.IFGNodes.size());
		for (IFGNode n : this.IFGNodes) {
			System.out.println("	" + n);
		}
	}

	public void printMore() {
		System.out.println("Graph nodes: " + this.IFGNodes.size());
		for (IFGNode n : this.IFGNodes) {
			System.out.print("    ");
			n.print();
		}
	}
}

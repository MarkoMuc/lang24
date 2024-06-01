package lang24.phase.regall;

import java.util.*;
import lang24.data.mem.*;
import lang24.common.report.*;

public class IFGNode {

	private final MemTemp temp;
	private final HashSet<IFGNode> connections;
	private boolean potentialSpill = false;
	private int color = -1;

	public IFGNode(MemTemp temp) {
		this.temp = temp;
		this.connections = new HashSet<>();
	}

	public void setColor(int color, int numRegs) {
		if (color <= 0)
			throw new Report.Error("color cannot be negative or zero");
		else if (numRegs <= color) {
			if (potentialSpill)
				this.color = numRegs;
			else
				throw new Report.Error(this + "(Tried to spill without potential)");
		} else {
			this.color = color;
		}
	}

	public int getColor() {
		return this.color;
	}

	public void addConnection(IFGNode n) {
		if (this == n) return;
		if (this.connections.add(n)) {
			n.addConnection(this);
		}
	}

	public void addConnections(Set<IFGNode> knownConnections) {
		for (IFGNode n : knownConnections) {
			this.addConnection(n);
		}
	}

	public void delConnection(IFGNode n) {
		if (this == n) return;
		if (this.connections.remove(n)) {
			n.delConnection(this);
		}
	}

	public void delConnections(Set<IFGNode> connectionsToRemove) {
		for (IFGNode n : connectionsToRemove) {
			this.delConnection(n);
		}
	}

	public HashSet<IFGNode> connections() {
		return new HashSet<IFGNode>(this.connections);
	}

	public void markPotentialSpill() {
		this.potentialSpill = true;
	}

	public MemTemp id() {
		return this.temp;
	}

	public int degree() {
		return this.connections.size();
	}

	public void print() {
		System.out.println(this);
		for (IFGNode n : this.connections) {
			System.out.println("	-> " + n);
		}
	}

	public void printSpill() {
		if (this.potentialSpill)
			System.out.println(this + " (potential spill)");
		else
			System.out.println(this);
	}

	@Override
	public String toString() {
		return "IFGNode: " + this.temp + "[" + this.color + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IFGNode n) {
            return n.temp == this.temp;
		}
		return false;
	}
}

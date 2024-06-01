package lang24.phase.regall;

import lang24.common.report.Report;
import lang24.data.mem.MemTemp;

import java.util.HashSet;

public class IFGNode {

    private final MemTemp temp;
    private final HashSet<IFGNode> connections;
    private boolean potentialSpill = false;
    private int color;

    public IFGNode(MemTemp temp) {
        this.temp = temp;
        this.color = -1;
        this.connections = new HashSet<>();
    }

    public int getColor() {
        return this.color;
    }

    public MemTemp getTemp() {
        return this.temp;
    }

    public int getDegree() {
        return this.connections.size();
    }

    public HashSet<IFGNode> getConnections() {
        return new HashSet<IFGNode>(this.connections);
    }

    public void setColor(int color, int numRegs) {
        if (color == 0) {
            throw new Report.Error("Register 0 is reserved in RISC-V architecture.");
        } else if (color < 0) {
            throw new Report.Error("Register number needs to be a positive number.");
        } else if (numRegs <= color) {
            if (potentialSpill) {
                this.color = numRegs;
            } else {
                throw new Report.Error(this + "Spilled but not marked as a potential spill.");
            }
        } else {
            this.color = color;
        }
    }

    public void addConnection(IFGNode node) {
        if (this == node) {
            return;
        }
        if (this.connections.add(node)) {
            node.addConnection(this);
        }
    }

    public void removeConnection(IFGNode node) {
        if (this == node) {
            return;
        }
        if (this.connections.remove(node)) {
            node.removeConnection(this);
        }
    }

    public void markPotentialSpill() {
        this.potentialSpill = true;
    }

    @Override
    public String toString() {
        return "IFGNode: " + this.temp + "[" + this.color + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof IFGNode node) {
            return node.temp == this.temp;
        }
        return false;
    }
}

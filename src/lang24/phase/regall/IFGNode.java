package lang24.phase.regall;

import lang24.common.report.Report;
import lang24.data.mem.MemTemp;

import java.util.HashSet;

public class IFGNode {
    private MemTemp temp;
    private HashSet<IFGNode> connections;
    private int color;
    private boolean potentialSpill;

    IFGNode(MemTemp temp) {
        this.temp = temp;
        this.color = -1;
        this.potentialSpill = false;
        this.connections = new HashSet<>();
    }

    public MemTemp getTemp() {
        return this.temp;
    }

    public int getColor() {
        return this.color;
    }

    public HashSet<IFGNode> getConnections() {
        return this.connections;
    }

    public HashSet<IFGNode> getConnectionsCopy() {
        return new HashSet<>(this.connections);
    }

    public boolean getPotentialSpill() {
        return this.potentialSpill;
    }

    public void setPotentialSpill(boolean potentialSpill) {
        this.potentialSpill = potentialSpill;
    }

    public int degree(){
        return this.connections.size();
    }

    public boolean addConnection(IFGNode node) {
        if (this == node) {
            return false;
        }

        if (this.connections.add(node)) {
            node.addConnection(this);
        }

        return true;
    }

    public boolean removeConnection(IFGNode node) {
        if (this == node) {
            return false;
        }

        if (this.connections.remove(node)) {
            node.removeConnection(this);
        }

        return true;
    }

    public void setColor(int color, int K){
        if(color < 0){
            throw new Report.Error("Color is negative");
        } else if(K <= color){
            if(potentialSpill){
                this.color = K;
            }else{
                throw new Report.Error("Potential spill error on node: " + this);
            }
        }else{
            //TODO: 0 is also illegal for RISC-V
            this.color = color;
        }
    }

    @Override
    public String toString() {
        return "IFGNode: " + this.temp.toString() + "[" + this.color + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof IFGNode node) {
            if(node.temp == this.temp) {
                return true;
            }
        }
        return false;
    }
}

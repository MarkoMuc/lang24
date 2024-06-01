package lang24.phase.regall;

import java.util.*;

import lang24.Compiler;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.mem.*;
import lang24.data.asm.*;
import lang24.phase.*;
import lang24.phase.asmgen.*;
import lang24.phase.livean.LiveAn;
import lang24.phase.regall.RISCVRegisters;

/**
 * Register allocation.
 */
public class RegAll extends Phase {

	/** Mapping of temporary variables to registers. */
	public final HashMap<MemTemp, Integer> tempToReg = new HashMap<MemTemp, Integer>();
	public final HashMap<MemTemp, String> tempToStringReg = new HashMap<>();

	private Stack<IFGNode> nodeStack = new Stack<>();
	private int numRegs = Compiler.numRegs;
	private Code currentCode = null;
	private RISCVRegisters riscv = new RISCVRegisters();

	public RegAll() {
		super("regall");
	}

	public void simplify(InterferenceGraph graph, InterferenceGraph tmp){
		IFGNode node = tmp.getLowDegreeNode(numRegs);
		while(node != null){
			tmp.removeNode(node);
			nodeStack.push(graph.findNode(node.getTemp()));
			node = tmp.getLowDegreeNode(numRegs);
		}

		if(tmp.getSize() == 0){
			build(graph);
		}else{
			spill(graph, tmp);
		}

	}

	public void spill(InterferenceGraph graph, InterferenceGraph tmp){
		IFGNode node = tmp.getHighDegreeNode(numRegs);
		tmp.removeNode(node);

		IFGNode reg = graph.findNode(node.getTemp());
		reg.setPotentialSpill(true);
		nodeStack.push(reg);

		simplify(graph, tmp);
	}

	//TODO: add more booleans to functions, it simplifies the control flow
	public void build(InterferenceGraph graph){
		boolean spill = false;
		IFGNode node = null;
		while(!nodeStack.isEmpty() && !spill){
			node = nodeStack.pop();
			spill = colorNode(node, graph);
		}

		if(spill){
			startOver(node);
		}else{
			for(IFGNode n : graph.getNodesCopy()){
				tempToReg.put(n.getTemp(), n.getColor());
				tempToStringReg.put(n.getTemp(), riscv.getABI(n.getColor()));
			}
		}
	}

	public boolean colorNode(IFGNode node, InterferenceGraph graph){
		if(node.getTemp() == currentCode.frame.FP){
			return false;
		}

		IFGNode curr = graph.findNode(node.getTemp());
		ArrayList<Integer> neighbors = new ArrayList<>(curr.degree());

		for(IFGNode n : curr.getConnectionsCopy()){
			int color = n.getColor();
			if( color != -1 ){
				if(!neighbors.contains(color)){
					neighbors.add(color);
				}
			}
		}
		Collections.sort(neighbors);

		//TODO: move form 0 to 1
		int chosen = chooseColor(neighbors, 1);
		node.setColor(chosen, numRegs);
		if(neighbors.size() >= numRegs){
			return true;
		}
		return false;
	}

	private int chooseColor(ArrayList<Integer> neighbors, int current){
		if(current >= numRegs){
			return numRegs;
		}
		for(int i = 0; i < neighbors.size(); i++){
			int neighbor = neighbors.get(i);
			if(neighbor == current){
				return chooseColor(neighbors, i + 1);
			}
		}
		return current;
	}

	public void startOver(IFGNode node){
		nodeStack.clear();

		// modify code
		currentCode.tempSize += 8;
		MemTemp FP = currentCode.frame.FP;
		long locsSize = currentCode.frame.locsSize;
		long offs = -locsSize - 16 - currentCode.tempSize;
		ImcCONST offset = new ImcCONST(offs);

		for (int i = 0; i < currentCode.instrs.size(); i++) {
			AsmInstr instr = currentCode.instrs.get(i);
			if (node.getTemp() == FP) continue;
			boolean used = instr.uses().contains(node.getTemp());
			boolean defined = instr.defs().contains(node.getTemp());

			// check used first:
			// imagine ADD $1,$1,10
			// first load $1
			// then add $1,$1,10
			// then store $1
			if (used) {
				Vector<AsmInstr> inst = new Vector<AsmInstr>();
				MemTemp offsetReg = offset.accept(
						new ExprGenerator(),
						inst
				);
				String instrString = "	lw `d0,`s0,`s1";
				Vector<MemTemp> uses = new Vector<MemTemp>();
				Vector<MemTemp> defs = new Vector<MemTemp>();
				Vector<MemLabel> jumps = new Vector<MemLabel>();
				uses.add(FP);
				uses.add(offsetReg);
				defs.add(node.getTemp());

				AsmOPER load = new AsmOPER(
						instrString, uses, defs, jumps
				);
				inst.add(load);
				currentCode.instrs.addAll(i, inst);
				i += inst.size();
			}
			if (defined) {
				Vector<AsmInstr> inst = new Vector<AsmInstr>();
				MemTemp reg = node.getTemp();
				MemTemp offsetReg = offset.accept(
						new ExprGenerator(),
						inst
				);
				String instrString = "	sw `s0,`s1,`s2";
				Vector<MemTemp> uses = new Vector<MemTemp>();
				Vector<MemTemp> defs = new Vector<MemTemp>();
				Vector<MemLabel> jumps = new Vector<MemLabel>();
				uses.add(reg);
				uses.add(FP);
				uses.add(offsetReg);
				AsmOPER store = new AsmOPER(
						instrString, uses, defs, jumps
				);
				inst.add(store);
				currentCode.instrs.addAll(i + 1, inst);
				i += inst.size();
			}
		}

		// redo from livean
		LiveAn livean = new LiveAn();
		livean.analysis();
		//Report.info("spilled: " + n.id());
		currentCode = null;
		this.allocate();
	}

	public InterferenceGraph init(Code code){
		InterferenceGraph graph = new InterferenceGraph();
		MemTemp FP = code.frame.FP;
		for(AsmInstr instr : code.instrs){
			for(MemTemp temp : instr.uses()){
				IFGNode node = new IFGNode(temp);
				graph.addNode(node);
			}

			for(MemTemp temp : instr.defs()){
				IFGNode node = new IFGNode(temp);
				graph.addNode(node);
			}
		}

		for (AsmInstr instr : code.instrs) {
			for (MemTemp t1 : instr.out()) {
				for (MemTemp t2 : instr.out()) {
					if (t1 != t2 && t1 != FP && t2 != FP) {
						IFGNode n = graph.findNode(t1);
						IFGNode m = graph.findNode(t2);
						n.addConnection(m);
					}
				}
			}
		}

		return graph;
	}

	public void allocateBody(Code code){
		currentCode = code;
		InterferenceGraph graph = init(code);
		InterferenceGraph tmp = new InterferenceGraph(graph.getNodesCopy());

		simplify(graph, tmp);
		tempToReg.put(code.frame.FP, 31);
		tempToStringReg.put(code.frame.FP, "FP");

	}

	public void allocate() {
		for(Code code : AsmGen.codes){
			allocateBody(code);
			nodeStack.clear();
		}
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("body", code.entryLabel.name);
			logger.addAttribute("epilogue", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toRegsString(tempToStringReg));
				logger.begElement("temps");
				logger.addAttribute("name", "use");
				for (MemTemp temp : instr.uses()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "def");
				for (MemTemp temp : instr.defs()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "in");
				for (MemTemp temp : instr.in()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.begElement("temps");
				logger.addAttribute("name", "out");
				for (MemTemp temp : instr.out()) {
					logger.begElement("temp");
					logger.addAttribute("name", temp.toString());
					logger.endElement();
				}
				logger.endElement();
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}

}

package lang24.phase.regall;

import java.util.*;

import lang24.Compiler;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.mem.*;
import lang24.data.asm.*;
import lang24.phase.*;
import lang24.phase.asmgen.*;
import lang24.phase.livean.LiveAn;

/**
 * Register allocation.
 */
public class RegAll extends Phase {

	/** Mapping of temporary variables to registers. */
	public final HashMap<MemTemp, Integer> tempToReg = new HashMap<MemTemp, Integer>();

	public final int numRegs = Compiler.numRegs;
	private final Stack<IFGNode> savedIFGNodes = new Stack<IFGNode>();
	public final HashMap<MemTemp, String> tempToSReg = new HashMap<MemTemp, String>();
	private final RISCVRegisters riscv = new RISCVRegisters();

	private Code currentCode = null;

	public RegAll() {
		super("regall");
	}

	private void simplify(InterferenceGraph graph, InterferenceGraph tmp) {
		IFGNode node = tmp.getLowDegreeNode(numRegs);
		while (node != null) {
			tmp.removeNode(node);
			savedIFGNodes.add(graph.findNode(node.getTemp()));
			node = tmp.getLowDegreeNode(numRegs);
		}

		if (tmp.getSize() == 0) {
			build(graph);
		} else{
			spill(graph, tmp);
		}
	}

	private void spill(InterferenceGraph graph, InterferenceGraph tmp) {
		IFGNode node = tmp.getHighDegreeNode(numRegs);
		tmp.removeNode(node);

		IFGNode reg = graph.findNode(node.getTemp());
		reg.markPotentialSpill();
		savedIFGNodes.push(reg);
		simplify(graph, tmp);
	}

	private int chooseColor(ArrayList<Integer> neighbors, int current) {
		if (current >= numRegs){
			return numRegs;
		}
		
		for (int i = 0; i < neighbors.size(); i++) {
			int neighbor = neighbors.get(i);
			if (neighbor == current) {
				return chooseColor(neighbors, current + 1);
			}
		}

		return current;
	}

	private boolean colorNode(IFGNode node, InterferenceGraph graph) {
		if (node.getTemp() == currentCode.frame.FP){
			return false;
		}

		IFGNode current = graph.findNode(node.getTemp());
		ArrayList<Integer> neighbors = new ArrayList<Integer>(current.getDegree());

		for (IFGNode i : current.getConnections()) {
			int color = i.getColor();
			if (color != -1) {
				if (!neighbors.contains(color)) {
					neighbors.add(color);
				}
			}
		}
		Collections.sort(neighbors);

		node.setColor(chooseColor(neighbors, 1), numRegs);

		if (neighbors.size() >= numRegs){
			return true;
		}

		return false;
	}

	private void build(InterferenceGraph graph) {
		boolean spillHappened = false;
		IFGNode node = null;

		while (!savedIFGNodes.empty() && !spillHappened) {
			node = savedIFGNodes.pop();
			spillHappened |= colorNode(node, graph);
		}

		if (spillHappened) {
			startOver(node);
		} else {
			for (IFGNode i : graph.getNodes()) {
				tempToReg.put(i.getTemp(), i.getColor());
				tempToSReg.put(i.getTemp(), riscv.getABI(i.getColor()));
			}
		}
	}

	private void startOver(IFGNode n) {
		// reinitialize
		while (!savedIFGNodes.empty()) savedIFGNodes.pop();

		// modify code
		currentCode.tempSize += 8;
		MemTemp FP = currentCode.frame.FP;
		long locsSize = currentCode.frame.locsSize;
		long offs = -locsSize - 16 - currentCode.tempSize;
		ImcCONST offset = new ImcCONST(offs);

		for (int i = 0; i < currentCode.instrs.size(); i++) {
			AsmInstr instr = currentCode.instrs.get(i);
			if (n.getTemp() == FP) continue;
			boolean used = instr.uses().contains(n.getTemp());
			boolean defined = instr.defs().contains(n.getTemp());

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
				defs.add(n.getTemp());
				AsmOPER load = new AsmOPER(
						instrString, uses, defs, jumps
				);
				inst.add(load);
				currentCode.instrs.addAll(i, inst);
				i += inst.size();
			}
			if (defined) {
				Vector<AsmInstr> inst = new Vector<AsmInstr>();
				MemTemp reg = n.getTemp();
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

	private InterferenceGraph buildIFGraph(Code code) {
		InterferenceGraph graph = new InterferenceGraph();
		MemTemp FP = code.frame.FP;

		for (AsmInstr instr : code.instrs) {
			for (MemTemp use : instr.uses()) {
				graph.addNode(new IFGNode(use));
			}
			for (MemTemp def : instr.defs()) {
				graph.addNode(new IFGNode(def));
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

	public void allocate() {
		for (Code code : AsmGen.codes) {
			currentCode = code;
			InterferenceGraph tmp = buildIFGraph(code);
			InterferenceGraph graph = buildIFGraph(code);
			simplify(graph, tmp);
			// TODO:fix this bruh it aint 253
			tempToReg.put(code.frame.FP, 253);
			tempToSReg.put(code.frame.FP, "FP");
		}
		currentCode = null;
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
				logger.addAttribute("code", instr.toRegsString(tempToSReg));
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

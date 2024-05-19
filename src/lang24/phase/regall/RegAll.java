package lang24.phase.regall;

import java.util.*;

import lang24.Compiler;
import lang24.common.report.Report;
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

	private final Stack<Node> savedNodes = new Stack<Node>();

	public RegAll() {
		super("regall");
	}

	private void simplify(Graph tmp, Graph graph) {
		// Take a temp with degree < numRegs
		// and push it onto the stack
		Node n = tmp.getLowDegreeNode(numRegs);
		while (n != null) {
			tmp.removeNode(n);
			savedNodes.add(graph.findNode(n.id()));
			n = tmp.getLowDegreeNode(numRegs);
		}

		if (tmp.size() == 0)
			select(graph);
		else
			spill(tmp, graph);
	}

	private void spill(Graph tmp, Graph graph) {
		// Take a temp with degree >= numRegs
		// and push it onto the stack
		// marking it a "potential spill"
		Node n = tmp.getHighDegreeNode(numRegs);
		// n cannot be null (either graph empty and spill() is not
		// called, or it has elements with high enough degree)
		Node gn = graph.findNode(n.id());
		gn.markPotentialSpill();
		tmp.removeNode(n);
		savedNodes.push(gn);
		simplify(tmp, graph);
	}

	private int chooseColor(ArrayList<Integer> unavailable, int current) {
		if (current >= numRegs) return numRegs;
		for (int i = 0; i < unavailable.size(); i++) {
			int uc = unavailable.get(i);
			if (uc == current) {
				return chooseColor(unavailable, current + 1);
			}
		}
		return current;
	}

	private boolean colorNode(Node n, Graph graph) {
		if (n.id() == currentCode.frame.FP) return false;
		Node current = graph.findNode(n.id());
		ArrayList<Integer> unavailableColors =
				new ArrayList<Integer>(current.degree());

		for (Node i : current.connections()) {
			int c = i.getColor();
			if (c != -1) {
				if (!unavailableColors.contains(c)) {
					unavailableColors.add(c);
				}
			}
		}
		Collections.sort(unavailableColors);

		int chosenColor = chooseColor(unavailableColors, 0);
		n.setColor(chosenColor, numRegs);
		// if next instruction has numRegs outs we have to spill NOW
		// otherwise we cannot load address into available reg
		//if (chosenColor == numRegs) return true;
		if (unavailableColors.size() >= numRegs) return true;
		int maxUsedRegs = numRegs - unavailableColors.size();
		return false;
	}

	private void select(Graph graph) {
		// Take temp from stack and color it
		// "potential spill" -> {"colored node", "spill"}
		boolean spillHappened = false;
		Node n = null;
		while (!savedNodes.empty() && !spillHappened) {
			n = savedNodes.pop();
			spillHappened |= colorNode(n, graph);
		}
		if (spillHappened) {
			startOver(n);
		} else {
			for (Node i : graph.nodes()) {
				tempToReg.put(i.id(), i.getColor());
			}
		}
	}

	private Code currentCode = null;

	private void startOver(Node n) {
		// reinitialize
		while (!savedNodes.empty()) savedNodes.pop();

		// modify code
		currentCode.tempSize += 8;
		MemTemp FP = currentCode.frame.FP;
		long locsSize = currentCode.frame.locsSize;
		long offs = -locsSize - 16 - currentCode.tempSize;
		ImcCONST offset = new ImcCONST(offs);

		for (int i = 0; i < currentCode.instrs.size(); i++) {
			AsmInstr instr = currentCode.instrs.get(i);
			if (n.id() == FP) continue;
			boolean used = instr.uses().contains(n.id());
			boolean defined = instr.defs().contains(n.id());

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
				defs.add(n.id());
				AsmOPER load = new AsmOPER(
						instrString, uses, defs, jumps
				);
				inst.add(load);
				currentCode.instrs.addAll(i, inst);
				i += inst.size();
			}
			if (defined) {
				Vector<AsmInstr> inst = new Vector<AsmInstr>();
				MemTemp reg = n.id();
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

	// every MemTemp in code is a Vortex
	// (except FP which already has assigned register 253)
	// (except SP which already has assigned register 254)
	// every out in code defines an Edge
	private Graph initGraph(Code code) {
		Graph g = new Graph();
		MemTemp FP = code.frame.FP;

		for (AsmInstr instr : code.instrs) {
			for (MemTemp use : instr.uses()) {
				g.addNode(new Node(use));
			}
			for (MemTemp def : instr.defs()) {
				g.addNode(new Node(def));
			}
		}
		for (AsmInstr instr : code.instrs) {
			for (MemTemp t1 : instr.out()) {
				for (MemTemp t2 : instr.out()) {
					if (t1 != t2 && t1 != FP && t2 != FP) {
						Node n = g.findNode(t1);
						Node m = g.findNode(t2);
						n.addConnection(m);
					}
				}
			}
		}
		return g;
	}

	public void allocate() {
		for (Code code : AsmGen.codes) {
			currentCode = code;
			Graph tmp = initGraph(code);
			Graph graph = initGraph(code);
			simplify(tmp, graph);
			// TODO:fix this bruh it aint 253
			tempToReg.put(code.frame.FP, 253);
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
				logger.addAttribute("code", instr.toString(tempToReg));
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

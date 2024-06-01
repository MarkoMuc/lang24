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

	public RegAll() {
		super("regall");
	}

	private void simplify(InterferenceGraph tmp, InterferenceGraph interferenceGraph) {
		// Take a temp with degree < numRegs
		// and push it onto the stack
		IFGNode n = tmp.getLowDegreeNode(numRegs);
		while (n != null) {
			tmp.removeNode(n);
			savedIFGNodes.add(interferenceGraph.findNode(n.getTemp()));
			n = tmp.getLowDegreeNode(numRegs);
		}

		if (tmp.getSize() == 0)
			select(interferenceGraph);
		else
			spill(tmp, interferenceGraph);
	}

	private void spill(InterferenceGraph tmp, InterferenceGraph interferenceGraph) {
		// Take a temp with degree >= numRegs
		// and push it onto the stack
		// marking it a "potential spill"
		IFGNode n = tmp.getHighDegreeNode(numRegs);
		// n cannot be null (either graph empty and spill() is not
		// called, or it has elements with high enough degree)
		IFGNode gn = interferenceGraph.findNode(n.getTemp());
		gn.markPotentialSpill();
		tmp.removeNode(n);
		savedIFGNodes.push(gn);
		simplify(tmp, interferenceGraph);
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

	private boolean colorNode(IFGNode n, InterferenceGraph interferenceGraph) {
		if (n.getTemp() == currentCode.frame.FP) return false;
		IFGNode current = interferenceGraph.findNode(n.getTemp());
		ArrayList<Integer> unavailableColors =
				new ArrayList<Integer>(current.getDegree());

		for (IFGNode i : current.getConnections()) {
			int c = i.getColor();
			if (c != -1) {
				if (!unavailableColors.contains(c)) {
					unavailableColors.add(c);
				}
			}
		}
		Collections.sort(unavailableColors);

		int chosenColor = chooseColor(unavailableColors, 1);
		n.setColor(chosenColor, numRegs);
		// if next instruction has numRegs outs we have to spill NOW
		// otherwise we cannot load address into available reg
		//if (chosenColor == numRegs) return true;
		if (unavailableColors.size() >= numRegs) return true;
		int maxUsedRegs = numRegs - unavailableColors.size();
		return false;
	}

	private void select(InterferenceGraph interferenceGraph) {
		// Take temp from stack and color it
		// "potential spill" -> {"colored node", "spill"}
		boolean spillHappened = false;
		IFGNode n = null;
		while (!savedIFGNodes.empty() && !spillHappened) {
			n = savedIFGNodes.pop();
			spillHappened |= colorNode(n, interferenceGraph);
		}
		if (spillHappened) {
			startOver(n);
		} else {
			for (IFGNode i : interferenceGraph.getNodes()) {
				tempToReg.put(i.getTemp(), i.getColor());
				tempToSReg.put(i.getTemp(), riscv.getABI(i.getColor()));
			}
		}
	}

	private Code currentCode = null;

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

	// every MemTemp in code is a Vortex
	// (except FP which already has assigned register 253)
	// (except SP which already has assigned register 254)
	// every out in code defines an Edge
	private InterferenceGraph initGraph(Code code) {
		InterferenceGraph g = new InterferenceGraph();
		MemTemp FP = code.frame.FP;

		for (AsmInstr instr : code.instrs) {
			for (MemTemp use : instr.uses()) {
				g.addNode(new IFGNode(use));
			}
			for (MemTemp def : instr.defs()) {
				g.addNode(new IFGNode(def));
			}
		}
		for (AsmInstr instr : code.instrs) {
			for (MemTemp t1 : instr.out()) {
				for (MemTemp t2 : instr.out()) {
					if (t1 != t2 && t1 != FP && t2 != FP) {
						IFGNode n = g.findNode(t1);
						IFGNode m = g.findNode(t2);
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
			InterferenceGraph tmp = initGraph(code);
			InterferenceGraph interferenceGraph = initGraph(code);
			simplify(tmp, interferenceGraph);
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

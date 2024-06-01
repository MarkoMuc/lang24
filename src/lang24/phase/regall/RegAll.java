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
	//CHECKME: is -2 correct?
	private int numRegs = Compiler.numRegs - 2;
	private Code currentCode = null;
	private RISCVRegisters riscv = new RISCVRegisters();

	public RegAll() {
		super("regall");
	}

	public void simplify(InterferenceGraph graph, InterferenceGraph tmp){
		IFGNode node = tmp.getLowDegreeNode(numRegs);
		while(node != null){
			tmp.removeNode(node);
			nodeStack.add(graph.findNode(node.getTemp()));
			node = tmp.getLowDegreeNode(numRegs);
		}

		if(tmp.getSize() == 0){
			build(graph);
		}else{
			spill(graph, tmp);
		}

	}

	public void spill(InterferenceGraph graph, InterferenceGraph tmp){
		IFGNode n = tmp.getHighDegreeNode(numRegs);
		// n cannot be null (either graph empty and spill() is not
		// called, or it has elements with high enough degree)
		IFGNode gn = graph.findNode(n.getTemp());
		gn.setPotentialSpill(true);
		tmp.removeNode(n);
		nodeStack.push(gn);
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
		if (node.getTemp() == currentCode.frame.FP) return false;
		IFGNode current = graph.findNode(node.getTemp());
		ArrayList<Integer> unavailableColors =
				new ArrayList<Integer>(current.degree());

		for (IFGNode i : current.getConnectionsCopy()) {
			int c = i.getColor();
			if (c != -1) {
				if (!unavailableColors.contains(c)) {
					unavailableColors.add(c);
				}
			}
		}
		Collections.sort(unavailableColors);

		int chosenColor = chooseColor(unavailableColors, 1);
		node.setColor(chosenColor, numRegs);
		// if next instruction has numRegs outs we have to spill NOW
		// otherwise we cannot load address into available reg
		//if (chosenColor == numRegs) return true;
		if (unavailableColors.size() >= numRegs) return true;
		int maxUsedRegs = numRegs - unavailableColors.size();
		return false;
	}

	private int chooseColor(ArrayList<Integer> unavailable, int current){
		if (current >= numRegs){
			return numRegs;
		}

		if(current == 2){
			// This is for SP
			return chooseColor(unavailable, current + 1);
		}
		for (int i = 0; i < unavailable.size(); i++) {
			int uc = unavailable.get(i);
			if (uc == current) {
				return chooseColor(unavailable, current + 1);
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
		for(Code code : AsmGen.codes){
			currentCode = code;
			InterferenceGraph graph = init(code);
			InterferenceGraph tmp = init(code);
			simplify(graph, tmp);
			tempToReg.put(code.frame.FP, 31);
			tempToStringReg.put(code.frame.FP, "FP");
		}

		currentCode = null;

		for(Code code : AsmGen.codes){
			for(AsmInstr instr : code.instrs){
				System.out.println(instr.toRegsString(tempToStringReg));
			}
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

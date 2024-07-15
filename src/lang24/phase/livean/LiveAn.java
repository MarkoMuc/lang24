package lang24.phase.livean;

import lang24.common.report.Report;
import lang24.data.mem.*;
import lang24.data.asm.*;
import lang24.phase.*;
import lang24.phase.asmgen.*;

import java.util.HashMap;
import java.util.HashSet;

/**
 * Liveness analysis.
 */
public class LiveAn extends Phase {

	public LiveAn() {
		super("livean");
	}

	private AsmInstr getValidJump(Code code, int idx){
		AsmInstr instr = null;
		int size = code.instrs.size();

		while(idx < size){
			instr = code.instrs.get(idx);
			if(!(instr instanceof AsmLABEL)){
				return instr;
			}
			idx++;
		}

		return instr;
	}

	private void processOuts(Code code, AsmInstr instr, int idx, HashMap<String, Integer> labels){
		// Last instruction in the frame
		if(idx + 1 >= code.instrs.size()){
			return;
		}
		AsmInstr next = code.instrs.get(idx + 1);
		// Add ins from the successor
		instr.addOutTemp(next.in());

		//Check possible jumps, calls do not matter, since all regs are saved at entry of the frame
		// TODO:this should make sure that CALL is not a function/name
		//		.matches("call (_|\\w)*"
		if(instr.jumps() != null && !instr.toString().contains("call")){
			for(MemLabel label : instr.jumps()){
				Integer labelIdx = labels.get(label.name);
				if(labelIdx != null){
					// Get the next instruction, should this go + 1 until a non Label type?
					// I'm pretty sure we can get label, label situation, so it should work like that
					// CHECKME: This validation can be saved for future use
					AsmInstr temp = getValidJump(code, labelIdx);

					if (temp != null) {
						instr.addOutTemp(temp.in());
					}
				}else{
					throw new Report.Error("NO label " + label.name + " found, this shouldn't happen");
				}
			}
		}
	}

	private void processIns(Code code, AsmInstr instr, int idx){
		instr.addInTemps(new HashSet<>(instr.uses()));

        HashSet<MemTemp> temp = new HashSet<>(instr.out());

		instr.defs().forEach(temp::remove);

		instr.addInTemps(temp);
	}

	private void analyseCode(Code code){
		// CHECKME: Do we need the string or can we just use the MemLable object directly?
		int size = code.instrs.size();
		HashMap<String, Integer> labels = new HashMap<>();

		// Make sure ins and outs are clear
		// CHECKME: this loop could also calculate the validateJumps
		for(int i = 0; i < size; i++) {
			AsmInstr instr = code.instrs.get(i);

			if(instr instanceof AsmOPER oper){
				oper.removeAllFromIn();
				oper.removeAllFromOut();
			}

			if(instr instanceof AsmLABEL label){
				labels.put(label.toString(), i);
			}
		}

		long count = 0;
		boolean keepItUp = false;
		do{
			keepItUp = false;
			// Top Down
			// for(int i = 0; i < size; i++) {
			// Bottom up
			for(int i = size - 1; i >= 0; i--) {
				AsmInstr instr = code.instrs.get(i);
				if(instr instanceof AsmLABEL label) {
					labels.put(label.toString(), i);
				}else {
					count = instr.out().size();
					processOuts(code, instr, i, labels);

					keepItUp = count != instr.out().size() || keepItUp;

					count = instr.in().size();
					processIns(code, instr, i);

					keepItUp = count != instr.in().size() || keepItUp;
				}
			}
		}while(keepItUp);
	}

	public void analysis() {
		for(Code code : AsmGen.codes){
			analyseCode(code);
		}
	}

	public void log() {
		if (logger == null)
			return;
		for (Code code : AsmGen.codes) {
			logger.begElement("code");
			logger.addAttribute("prologue", code.entryLabel.name);
			logger.addAttribute("body", code.entryLabel.name);
			logger.addAttribute("epilogue", code.exitLabel.name);
			logger.addAttribute("tempsize", Long.toString(code.tempSize));
			code.frame.log(logger);
			logger.begElement("instructions");
			for (AsmInstr instr : code.instrs) {
				logger.begElement("instruction");
				logger.addAttribute("code", instr.toString());
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

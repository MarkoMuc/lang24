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
		int size = code.instrs.size();
		AsmInstr instr = null;

		while(idx < size){
			instr = code.instrs.get(idx);
			if(!(instr instanceof AsmLABEL)){
				return instr;
			}
			idx++;
		}

		return instr;
	}

	private void ProcessOuts(Code code, AsmInstr instr, int idx, HashMap<String, Integer> labels){
		// Last instruction in the frame
		if(idx + 1 >= code.instrs.size()){
			return;
		}
		AsmInstr next = code.instrs.get(idx + 1);
		// Add ins from the successor
		instr.addOutTemp(next.in());

		//Check possible jumps, calls do not matter, since all regs are saved at entry of the frame
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

	private void ProcessIns(Code code, AsmInstr instr, int idx){
		instr.addInTemps(new HashSet<>(instr.uses()));

        HashSet<MemTemp> temp = new HashSet<>(instr.out());
		temp.retainAll(instr.defs());
		instr.addInTemps(temp);
	}

	private void AnalyseCode(Code code){
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
		boolean KeepItUp = false;
		do{
			KeepItUp = false;
			// Top Down
			// for(int i = 0; i < size; i++) {
			// Bottom up
			for(int i = size - 1; i >= 0; i--) {
				AsmInstr instr = code.instrs.get(i);
				if(instr instanceof AsmLABEL label) {
					labels.put(label.toString(), i);
				}else {
					count = instr.out().size();
					ProcessOuts(code, instr, i, labels);
					// process outs
					KeepItUp = count != instr.out().size() || KeepItUp;

					count = instr.in().size();
					ProcessIns(code, instr, i);
					// process in
					KeepItUp = count != instr.in().size() || KeepItUp;
				}
			}
		}while(KeepItUp);
	}

	public void analysis() {
		for(Code code : AsmGen.codes){
			AnalyseCode(code);
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

package lang24.phase.asmgen;

import lang24.data.asm.AsmInstr;
import lang24.data.asm.Code;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.lin.LinCodeChunk;
import lang24.phase.Phase;
import lang24.phase.imclin.ImcLin;

import java.util.Vector;

/**
 * Machine code generator.
 *
 * @author marko.muc12@gmail.com
 */
public class AsmGen extends Phase {

	public static Vector<Code> codes = new Vector<Code>();

	public AsmGen() {
		super("asmgen");
	}

	public void genAsmCodes() {
		for (LinCodeChunk codeChunk : ImcLin.codeChunks()) {
			Code code = genAsmCode(codeChunk);
			codes.add(code);
		}
	}

	public Code genAsmCode(LinCodeChunk codeChunk) {
		Vector<AsmInstr> instrs = new Vector<AsmInstr>();
		for (ImcStmt stmt : codeChunk.stmts()) {
			instrs.addAll(stmt.accept(new StmtGenerator(), null));
		}
		return new Code(codeChunk.frame, codeChunk.entryLabel, codeChunk.exitLabel, instrs);
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
				logger.endElement();
			}
			logger.endElement();
			logger.endElement();
		}
	}

}

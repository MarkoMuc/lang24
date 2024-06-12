package lang24.phase.all;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.AsmOPER;
import lang24.data.asm.Code;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemTemp;
import lang24.phase.Phase;
import lang24.phase.asmgen.AsmGen;
import lang24.phase.imclin.ImcLin;
import lang24.phase.regall.RegAll;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;


public class All extends Phase {
    private PrintWriter writer;

    public All() {
        super("all");
    }

    private void initWriter(String path) {
        try {
            FileWriter file = new FileWriter(path);
            this.writer = new PrintWriter(new BufferedWriter(file));
        } catch (Exception e) {
            throw new Report.Error("File: " + path + "caused an IOException.");
        }
    }

    private void dataSegment() {
        //TODO: data can be closer to its first use
        Vector<LinDataChunk> chunks = ImcLin.dataChunks();

        if(chunks.isEmpty()) {
            return;
        }

        writer.println(".section .data");
        writer.println(".align 2");

        for (LinDataChunk chunk : chunks) {
            String data;
            String type = null;
            if (chunk.init == null) {
                data = String.join(",", Collections.nCopies((int) (chunk.size / 8), "0"));
                type = ".dword";
            } else {
                type = ".string";
                data = chunk.init;
            }
            writer.printf("%s:\t%s %s\n", chunk.label.name, type, data);
        }

        writer.println();
    }

    private void entry() {
        writer.println(".section .text");
        writer.println(".align 2");
        writer.println(".global _start");
        writer.println("_start:");

        /*Malloc and fill if needed, jump to main*/
        /*BODY*/
        // TODO: patch in exit(), malloc(), putc(), getc(), putint(), getint(), etc.
        // This is will err if there is no main function
        printInstr("j _main\n");
    }

    private void subroutines(Vector<Code> subroutines, RegAll regAll) {
        HashMap<MemTemp, String> tempToString = regAll.tempToSReg;
        HashMap<Code, HashSet<String>> codeToRegs = regAll.codeToRegs;

        for (Code subroutine : subroutines) {
            MemFrame frame = subroutine.frame;
            HashSet<String> usedRegs = codeToRegs.get(subroutine);
            long offset = 0;
            writer.println(frame.label.name + ":");

            //Prologue

            // Save FP and RA
            // FIXME: This is actually wrong? WHere is SL?
            // CHECKME: When is direct offset addressing wrong, as in the offset is too large?
            offset = frame.locsSize + 16;
            printInstr(String.format("sd fp, %d(sp)\n", offset)); // Stores old FP
            printInstr("mv fp, sp\n"); // Moves old SP to current FP

            offset = offset + 8;
            printInstr(String.format("sd ra, %d(fp)\n", offset)); // Saves return address

            // Saves regs
            for (String reg : usedRegs) {
                offset += 8;
                printInstr(String.format("sd %s, %d(fp)\n", reg, offset));
            }

            offset += subroutine.tempSize * 8;
            printInstr(String.format("add sp, fp, %d\n", offset)); // Sets SP

            printInstr(String.format("j %s\n", subroutine.entryLabel.name));

            for (AsmInstr instr : subroutine.instrs) {
                String instruction = instr.toRegsString(tempToString);
                if(instr instanceof AsmLABEL){
                    writer.printf("%s:\n", instruction);
                } else{
                    printInstr(String.format("%s\n", instruction));
                }
            }

            //Epilogue
            writer.println(subroutine.exitLabel.name + ":");
            offset = offset - subroutine.tempSize * 8;

            // Restores used regs
            for (String reg : usedRegs.stream().toList().reversed()) {
                //TODO: this offset-8 needs to be done by the machine, so better to use offset
                printInstr(String.format("ld %s, %d(fp)\n", reg, offset));
                offset = offset - 8;
            }

            // FIXME: Do it SP based
            printInstr(String.format("ld ra, %d(fp)\n", offset)); // Restores RA
            offset = offset - 8;

            printInstr("mv sp, fp\n"); // Restores previous SP

            printInstr(String.format("ld fp, %d(sp)\n", offset)); // Restores FP

            //TODO: RA only needs to be saved if changed
            printInstr("ret");


            writer.println();
        }
        //Add exit if there is no exit yet
        printInstr("li a0, 1\n");
        printInstr("sd a0, 0(sp)\n");
        printInstr("sd a0, 8(sp)\n");
        printInstr("call _exit\n");
    }

    private void printInstr(String instr){
        writer.print('\t');
        writer.printf(instr);
    }

    //TODO: name it something like temp or something idk
    public void allTogether(String path, RegAll regAll) {
        initWriter(path + ".s");

        dataSegment();
        entry();
        subroutines(AsmGen.codes, regAll);

        writer.flush();
        writer.close();
    }
}

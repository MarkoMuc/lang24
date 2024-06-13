package lang24.phase.all;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
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
    private final int DATA_ALIGN = 4;
    private final int INSTR_ALIGN = 6;
    private PrintWriter writer;
    private final HashSet<LinDataChunk> mmapGlobals = new HashSet<>();

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

        if (chunks.isEmpty()) {
            return;
        }

        writer.println(".section .data");
        writer.printf(".align %d\n", DATA_ALIGN);

        for (LinDataChunk chunk : chunks) {
            String data;
            long size;
            String type = null;
            if (chunk.init == null) {
                size = chunk.size / 8;
                if (size > 32) {
                    data = "0";
                    mmapGlobals.add(chunk);
                } else {
                    data = String.join(",", Collections.nCopies((int) (size), "0"));
                }

                type = ".dword";
            } else {
                type = ".string";
                data = chunk.init;
            }
            writer.printf("%s:\t%s %s\n", chunk.label.name, type, data);
        }

        writer.println();
    }

    private void mmapGenerate(LinDataChunk chunk) {
        printInstr(String.format("li a1, %d\n", chunk.size / 8));
        printInstr("sd a1, 8(sp)\n");
        printInstr("call _mmap\n");
        printInstr("ld a0, 0(sp)\n");
        printInstr(String.format("la a1, %s\n", chunk.label.name));
        printInstr("sd a0, 0(a1)\n");
        writer.println();
    }

    private void entry() {
        writer.println(".section .text");
        writer.printf(".align %d\n", INSTR_ALIGN);
        writer.println(".global _start");
        writer.println("_start:");

        //TODO: If err is not found, throw Report.Error
        for (LinDataChunk chunk : mmapGlobals) {
            mmapGenerate(chunk);
        }

        printInstr("j _main\n");
        writer.println();
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
            // CHECKME: When is direct offset addressing wrong, as in the offset is too large?

            // 0. + 1.
            offset = frame.locsSize + 8;

            printInstr(String.format("addi sp, sp, -%d\n", offset));
            printInstr("sd fp, 0(sp)\n"); // Stores old FP
            printInstr("mv fp, sp\n");
            printInstr(String.format("addi fp, fp, %d\n", offset)); // Align FP back to the old SP

            // 2.
            offset = 8;
            printInstr(String.format("addi sp, sp, -%d\n", offset));
            printInstr("sd ra, 0(sp)\n"); // Saves return address

            // 3.
            for (String reg : usedRegs) {
                printInstr(String.format("addi sp, sp, -%d\n", offset));
                printInstr(String.format("sd %s, 0(sp)\n", reg));
            }

            if (subroutine.tempSize != 0) {
                offset = subroutine.tempSize;
                printInstr(String.format("addi sp, sp, -%d\n", offset));
            }

            if (frame.argsSize != 0) {
                offset = subroutine.frame.argsSize;
                printInstr(String.format("addi sp, sp, -%d\n", offset)); // Sets SP
            }

            printInstr(String.format("j %s\n", subroutine.entryLabel.name));

            for (AsmInstr instr : subroutine.instrs) {
                String instruction = instr.toRegsString(tempToString);
                if (instr instanceof AsmLABEL) {
                    writer.printf("%s:\n", instruction);
                } else {
                    printInstr(String.format("%s\n", instruction));
                }
            }

            //Epilogue
            writer.println(subroutine.exitLabel.name + ":");

            // 0.
            if (frame.argsSize != 0) {
                offset = subroutine.frame.argsSize;
                printInstr(String.format("addi sp, sp, %d\n", offset)); // Sets SP
            }

            // 1.
            if (subroutine.tempSize != 0) {
                offset = subroutine.tempSize;
                printInstr(String.format("addi sp, sp, %d\n", offset));
            }

            // 2.
            offset = 8;
            for (String reg : usedRegs.stream().toList().reversed()) {
                printInstr(String.format("ld %s, 0(sp)\n", reg));
                printInstr(String.format("addi sp, sp, %d\n", offset));
            }

            // 3.
            printInstr("ld ra, 0(sp)\n"); // Restores RA
            printInstr(String.format("addi sp, sp, %d\n", offset));

            // 4. + 5.
            printInstr("ld fp, 0(sp)\n"); // Restores FP

            offset = frame.locsSize + 8;
            printInstr(String.format("addi sp, sp, %d\n", offset)); // Restores old SP

            //TODO: RA only needs to be saved if changed
            printInstr("ret\n");

            writer.println();
        }
        //Add exit if there is no exit yet
        printInstr("li a0, 1\n");
        printInstr("sd a0, 0(sp)\n");
        printInstr("sd a0, 8(sp)\n");
        printInstr("call _exit\n");
    }

    private void printInstr(String instr) {
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

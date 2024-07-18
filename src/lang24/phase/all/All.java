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
    private final int DATA_ALIGN = 6;
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
        Vector<LinDataChunk> chunks = ImcLin.dataChunks();
        Vector<LinDataChunk> ROChunks = new Vector<>();

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
                size = chunk.size / 8;

                if (size > 32) {
                    type = ".dword";
                    data = "0";
                    mmapGlobals.add(chunk);
                } else {
                    ROChunks.add(chunk);
                    continue;
                }
            }
            writer.printf("%s:\t%s %s\n", chunk.label.name, type, data);
        }

        writer.println();

        if(!ROChunks.isEmpty()) {
            writer.println(".section .rodata");
            writer.printf(".align %d\n", DATA_ALIGN);
            for(LinDataChunk chunk : ROChunks) {
                writer.printf("%s:\t.string \"%s\"\n", chunk.label.name, chunk.init);
            }

            writer.println();
        }
    }

    private void mmapGenerate(LinDataChunk chunk) {
        printInstr(String.format("li a1, %d\n", chunk.size));
        printInstr("sd a1, 8(sp)\n");
        printInstr("addi a1, a1, 8\n");
        printInstr("sd a1, 16(sp)\n");
        printInstr("call _mmap\n");
        printInstr("ld a0, 0(sp)\n");
        printInstr(String.format("la a1, %s\n", chunk.label.name));
        printInstr("sd a0, 0(a1)\n");
        if(chunk.label.name.contains("__str")) {
            Vector<Integer> data = createCharArray(chunk.init);
            for(Integer c : data) {
                printInstr(String.format("li a1, %d\n", c));
                printInstr("sb a1, 0(a0)\n");
                printInstr("addi a0, a0, 1\n");
            }
            printInstr("li a1, 0\n");
            printInstr("sb a1, 0(a0)\n");
        }

        writer.println();
    }

    private void entry(boolean compileAsLibrary) {
        writer.println(".section .text");
        writer.printf(".align %d\n", INSTR_ALIGN);
        if(!compileAsLibrary) {
            writer.println(".global _start");
            writer.println("_start:");
        }

        for (LinDataChunk chunk : mmapGlobals) {
            mmapGenerate(chunk);
        }

        if(!compileAsLibrary) {
            printInstr("call _main\n");

            printInstr("ld a0, 0(sp)\n");
            printInstr("li a1, 0\n");
            printInstr("sd a1, 0(sp)\n");
            printInstr("sd a0, 8(sp)\n");
            printInstr("call _exit\n");
        }

        writer.println();
    }

    private void subroutines(RegAll regAll, Vector<Code> subroutines, boolean compileAsLibrary) {
        HashMap<MemTemp, String> tempToString = regAll.tempToSReg;
        HashMap<Code, HashSet<String>> codeToRegs = regAll.codeToRegs;
        boolean mainSubroutine = false;

        for (Code subroutine : subroutines) {
            MemFrame frame = subroutine.frame;
            HashSet<String> usedRegs = codeToRegs.get(subroutine);
            long offset = 0;
            if (frame.label.name.equals("main") &&
                frame.depth == 0){
                mainSubroutine = true;
            }

            writer.println(frame.label.name + ":");
            //Prologue

            // Save FP and RA
            // CHECKME: When is direct offset addressing wrong, as in the offset is too large?

            offset = frame.locsSize + 8;

            printInstr(String.format("addi sp, sp, -%d\n", offset));
            printInstr("sd fp, 0(sp)\n"); // Stores old FP
            printInstr("mv fp, sp\n");
            printInstr(String.format("addi fp, fp, %d\n", offset)); // Align FP back to the old SP

            offset = 8;
            printInstr(String.format("addi sp, sp, -%d\n", offset));
            printInstr("sd ra, 0(sp)\n"); // Saves return address

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

            if (frame.argsSize != 0) {
                printInstr(String.format("addi sp, sp, %d\n", offset)); // Sets SP
            }

            if (subroutine.tempSize != 0) {
                offset = subroutine.tempSize;
                printInstr(String.format("addi sp, sp, %d\n", offset));
            }

            offset = 8;
            for (String reg : usedRegs.stream().toList().reversed()) {
                printInstr(String.format("ld %s, 0(sp)\n", reg));
                printInstr(String.format("addi sp, sp, %d\n", offset));
            }

            printInstr("ld ra, 0(sp)\n"); // Restores RA
            printInstr(String.format("addi sp, sp, %d\n", offset));

            printInstr("ld fp, 0(sp)\n"); // Restores FP

            offset = frame.locsSize + 8;
            printInstr(String.format("addi sp, sp, %d\n", offset)); // Restores old SP
            printInstr("ret\n");

            writer.println();
        }

        if(mainSubroutine && !compileAsLibrary) {
            throw new Report.Error("No main functions found at the global scope.");
        }
    }

    private void printInstr(String instr) {
        writer.print('\t');
        writer.printf(instr);
    }

    public void allTogether(String path, RegAll regAll, boolean compileAsLibrary) {
        initWriter(path + ".s");

        dataSegment();
        entry(compileAsLibrary);
        subroutines(regAll, AsmGen.codes, compileAsLibrary);

        writer.flush();
        writer.close();
    }

    private Vector<Integer> createCharArray(String value){
        Vector<Integer> chars = new Vector<>();
        boolean escape = false;
        StringBuilder v = new StringBuilder();

        for(char c : value.toCharArray()){
            if(!escape && c != '\\'){
                chars.add((int)c);
            }else{
                if(escape){
                    if(c == '\\'){
                        chars.add((int)'\\');
                        escape = false;
                    }else if(c == 'n'){
                        chars.add((int)'\n');
                        escape = false;
                    }else if(c >= 'A' && c <= 'F'){
                        v.append(c);
                        if(v.length() == 2){
                            chars.add(Integer.parseInt(v.toString(), 16));
                            v.setLength(0);
                            escape = false;
                        }
                    }
                }else{
                    escape = true;
                }
            }


        }

        return chars;
    }
}

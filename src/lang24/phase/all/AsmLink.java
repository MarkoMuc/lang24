package lang24.phase.all;

import lang24.common.report.Report;
import lang24.phase.Phase;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Vector;

/*
    Assembles and links the resulting assembly code.
 */
public class AsmLink extends Phase {
    private File errFile;
    public AsmLink(){
        super("AsmLink");
    }

    private String find_lib(){
        String lib_path = null;
        int depth = 0;
        try {
            String currWorkDir = new File(".").getCanonicalPath();
            while(depth < 4){
                Path[] dirs = java.nio.file.Files.walk(Paths.get(currWorkDir))
                        .filter(path -> path.toFile().isDirectory())
                        .filter(path -> path.getFileName().endsWith("lib"))
                        .toArray(Path[]::new);
                for (Path dir : dirs) {
                    if (Files.walk(dir)
                            .filter(path -> path.getFileName().endsWith("riscv64-unknown-elf-ld") ||
                                    path.getFileName().endsWith("riscv64-unknown-elf-as"))
                            .toArray(Path[]::new).length > 0) {
                        return dir.toFile().getAbsolutePath();
                    }
                }
                depth++;
                currWorkDir = new File(currWorkDir).getParent();
            }
        }catch (Exception e){
            throw new Report.Error("Error while looking for lib folder");
        }
        return null;
    }

    private void linker(String path, String lib_path){
        ArrayList<String> commands = new ArrayList<>();
        commands.add(String.format("%s/riscv64-unknown-elf-ld", lib_path));
        commands.add(String.format("-o %s", path));
        commands.add(String.format("%s", path));

        ProcessBuilder pb = new ProcessBuilder(commands);
        try{
            pb.redirectErrorStream(true);
            pb.redirectOutput(this.errFile);
            int exitCode = pb.start().waitFor();

            if(exitCode != 0){
                Scanner sc = new Scanner(this.errFile);
                while(sc.hasNextLine()){
                    System.out.println(sc.nextLine());
                }
                sc.close();
                if(!this.errFile.delete()){
                    System.err.println("Couldn't delete " + this.errFile.getAbsolutePath());
                }

                throw new Report.Error("Linker failed");
            }
            if(!this.errFile.delete()){
                System.err.println("Couldn't delete " + this.errFile.getAbsolutePath());
            }
        }catch (Exception e){
            throw new Report.Error("Linker failed");
        }
    }
    private String assembler(String path, String lib_path){
        ArrayList<String> commands = new ArrayList<>();
        commands.add(String.format("%s/riscv64-unknown-elf-as", lib_path));
        commands.add(String.format("-o %s.o", path));
        commands.add(String.format("%s", path));

        ProcessBuilder pb = new ProcessBuilder(commands);

        try{
            pb.redirectErrorStream(true);
            pb.redirectOutput(this.errFile);
            int exitCode = pb.start().waitFor();

            if(exitCode != 0){
                Scanner sc = new Scanner(this.errFile);
                while(sc.hasNextLine()){
                    System.out.println(sc.nextLine());
                }
                sc.close();
                if(!this.errFile.delete()){
                    System.err.println("Couldn't delete " + this.errFile.getAbsolutePath());
                }

                throw new Report.Error("Assembler failed");
            }
        }catch (Exception e){
            throw new Report.Error("Assembler failed");
        }

        return path + ".o";
    }

    public void assembleAndLink(String path){
        //TODO: delete asm file
        this.errFile = new File("err.temp");
        String lib_path = find_lib();
        String file = assembler(path, lib_path);
        linker(file, lib_path);
    }
}

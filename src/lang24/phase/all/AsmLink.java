package lang24.phase.all;

import lang24.common.report.Report;
import lang24.phase.Phase;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

/*
    Assembles and links the resulting assembly code.
 */
public class AsmLink extends Phase {
    private File errFile;
    private final HashSet<String> stdFiles = new HashSet<>();

    public AsmLink() {
        super("AsmLink");
    }

    private void getStd(String lib_path){
        Path obj_path = Paths.get(lib_path, "std", "obj");
        try {
            Path[] paths = java.nio.file.Files.walk(obj_path)
                    .filter(path -> path.getFileName().toString().endsWith(".o"))
                    .toArray(Path[]::new);
            for(Path path : paths){
                this.stdFiles.add(path.toFile().getAbsolutePath());
            }
        }catch (Exception e){
            throw new Report.Error("Error while looking for std object files which should be in lib/obj " + lib_path);
        }
    }

    private String find_lib() {
        int depth = 0;
        try {
            String currWorkDir = new File(".").getCanonicalPath();
            while (depth < 4) {
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
        } catch (Exception e) {
            throw new Report.Error("Error while looking for lib folder");
        }
        return null;
    }

    private void linker(String path, String lib_path) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add(String.format("%s/riscv64-unknown-elf-ld", lib_path));
        commands.add(String.format("-o%s", path));
        commands.add(String.format("%s.o", path));
        for(String std_path : stdFiles){
            commands.add(String.format("%s", std_path));
        }

        ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            pb.redirectErrorStream(true);
            pb.redirectOutput(this.errFile);
            int exitCode = pb.start().waitFor();

            if (exitCode != 0) {
                Scanner sc = new Scanner(this.errFile);
                while (sc.hasNextLine()) {
                    System.out.println(sc.nextLine());
                }
                sc.close();
                if (!this.errFile.delete()) {
                    System.err.println("Couldn't delete " + this.errFile.getAbsolutePath());
                }

                throw new Report.Error("Linker failed.");
            }
            if (!this.errFile.delete()) {
                System.err.println("Couldn't delete " + this.errFile.getAbsolutePath());
            }
        } catch (Exception e) {
            throw new Report.Error("Linker failed.");
        }
    }

    private void assembler(String path, String lib_path) {
        ArrayList<String> commands = new ArrayList<>();
        commands.add(String.format("%s/riscv64-unknown-elf-as", lib_path));
        commands.add("-g");
        commands.add(String.format("-o%s.o", path));
        commands.add(String.format("%s.s", path));

        ProcessBuilder pb = new ProcessBuilder(commands);

        try {
            pb.redirectErrorStream(true);
            pb.redirectOutput(this.errFile);
            int exitCode = pb.start().waitFor();

            if (exitCode != 0) {
                Scanner sc = new Scanner(this.errFile);
                while (sc.hasNextLine()) {
                    System.out.println(sc.nextLine());
                }
                sc.close();
                if (!this.errFile.delete()) {
                    System.err.println("Couldn't delete " + this.errFile.getAbsolutePath());
                }

                throw new Report.Error("Assembler failed");
            }
        } catch (Exception e) {
            throw new Report.Error("Assembler failed");
        }

    }

    public void assembleAndLink(String path, boolean objectFileOnly) {
        this.errFile = new File("err.temp");
        String lib_path = find_lib();
        assembler(path, lib_path);
        getStd(lib_path);
        if(!objectFileOnly){
            linker(path, lib_path);
        }
        if(!new File(path + ".s").delete()){
            throw new Report.Error("Couldn't delete the temporary asm file.");
        }
        if(objectFileOnly){
            return;
        }
        if(!new File(path + ".o").delete()){
            throw new Report.Error("Couldn't delete the temporary objective file.");
        }
    }
}

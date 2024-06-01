package lang24.phase.regall;

import java.util.HashMap;
import java.util.Vector;

public class RISCVRegisters {
    private static final HashMap<Integer, String> ABIRegister = new HashMap<>();
    private static final HashMap<Integer, String> NUMRegister = new HashMap<>();
    private static final String[] registerNUMNames = {
            "x0", // Hard wired zero
            "x1", // Return Address
            "x2", // Stack pointer
            "x3", // Global pointer
            "x4", // Thread pointer
            "x5", // Temporary/alternate link register
            "x6", "x7", // Temporaries
            "x8", // AKA s0, Saved register/Frame pointer
            "x9", // Saved register
            "x10", "x11", // Function arguments/return values
            "x12", "x13", "x14", "x15", "x16", "x17", // Function arguments
            "x18", "x19", "x20", "x21", "x22", "x23", "x24", "x25", "x26", "x27", // Saved registers
            "x28", "x29", "x30", "x31", // Temporaries
            //|-----------------------------------FLOATING POINT-------------------------------|
            "f0", "f1", "f2", "f3", "f4", "f5", "f6", "f7", // FP temporaries
            "f8", "f9", // FP saved registers
            "f10", "f11", // FP arguments/return values
            "f12", "f13", "f14", "f15", "f16", "f17", // FP arguments
            "f18", "f19", "f20", "f21", "f22", "f23", "f24", "f25", "f26", "f27", // FP saved registers
            "f28", "f29", "ft30", "ft31" // FP temporaries
    };
    private static final String[] registerABINames = {
            "zero", // Hard wired zero
            "ra", // Return Address
            "sp", // Stack pointer
            "gp", // Global pointer
            "tp", // Thread pointer
            "t0", // Temporary/alternate link register
            "t1", "t2", // Temporaries
            "fp", // AKA s0, Saved register/Frame pointer
            "s1", // Saved register
            "a0", "a1", // Function arguments/return values
            "a2", "a3", "a4", "a5", "a6", "a7", // Function arguments
            "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10", "s11", // Saved registers
            "t3", "t4", "t5", "t6", // Temporaries
            //|-----------------------------------FLOATING POINT-------------------------------|
            "ft0", "ft1", "ft2", "ft3", "ft4", "ft5", "ft6", "ft7", // FP temporaries
            "fs0", "fs1", // FP saved registers
            "fa0", "fa1", // FP arguments/return values
            "fa2", "fa3", "fa4", "fa5", "fa6", "fa7", // FP arguments
            "fs2", "fs3", "fs4", "fs5", "fs6", "fs7", "fs8", "fs9", "fs10", "fs11", // FP saved registers
            "ft8", "ft9", "ft10", "ft11" // FP temporaries
    };

    RISCVRegisters() {
        initABI();
        initREGS();
    }

    public String getABI(Integer idx) {
        return ABIRegister.getOrDefault(idx, "ERROR");
    }

    public Vector<String> getABIRegisters() {
        return new Vector<>(ABIRegister.values());
    }

    public String getNUM(Integer idx) {
        return ABIRegister.getOrDefault(idx, "ERROR");
    }

    public Vector<String> getNUMRegisters() {
        return new Vector<>(NUMRegister.values());
    }

    private void initABI() {
        Integer i = 0;
        for (String reg : registerABINames) {
            ABIRegister.put(i, reg);
            i++;
        }
    }

    private void initREGS() {
        Integer i = 0;
        for (String reg : registerNUMNames) {
            NUMRegister.put(i, reg);
            i++;
        }
    }
}

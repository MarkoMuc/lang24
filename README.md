# Compiler for Lang24

Lang24's language specification was designed and authored by Dr. Boštjan Slivnik (bostjan.slivnik@fri.uni-lj.si).

This compiler was developed as part of the Compiler course at the University of Ljubljana, Faculty of Computer and Information Science, under the instruction of Dr. Boštjan Slivnik. Some portions of the compiler code were also contributed by him.

## Building

1. Add the ANTLR4 jar, RISC-V GNU assembler, and RISC-V GNU linker to the lib/ folder.
2. Run make from the root directory's Makefile to build the compiler.

## Compiler

The compiler targets the 64-bit RISC-V architecture.

## Extension

An additional feature of this compiler is its support for the RISC-V Vector extension. Vectorization is achieved through data dependence analysis, with both the analysis and generation of vectorized intermediate code implemented.

This extension was developed as part of my undergraduate project under the mentorship of Dr. Boštjan Slivnik.

#!/bin/bash

mkdir -p ./lib/std/obj

for path in $(ls ./lib/std/asm/); do
    filename=$(basename -- "$path")
    filename="${filename%.*}"
    path="./lib/std/asm/$path"
    # Assemble the file and save it to ./obj
    ./lib/riscv64-unknown-elf-as "$path" -o "./lib/std/obj/$filename.o"
done

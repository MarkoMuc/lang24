#!/bin/bash

PRINT=${1:-false}

RESULT="tmp"

OUTFILE="./outs"

if [ -d $OUTFILE ]; then
    OUTFILE="./$RESULT"
fi;

mkdir -p $OUTFILE

if [ "$PRINT" == "print" ]; then
  RESULT="/dev/stdout"
elif [ "$PRINT" != "false"  ]; then
  echo -e "${CLR}Unknown argument, defaulting to saving results.${NC}"
  echo -e "${CLR}Use print to output to stdout.${NC}"
fi

echo -e "${CLR}Using $RESULT ${NC}"

GEN_TEST_DIR="./general_tests/"
MAIN_TEST_DIR="./main_tests/"

RED='\033[0;31m'
YELLOW='\033[1;33m'
GREEN='\033[0;32m'
CLR='\033[0;34m'
NC='\033[0m'


JAVA="java"
ANTLRDIR="../lib/antlr-4.13.1-complete.jar"
TARGETPHASE="all"
LOGGEDPHASE="none"

echo -e "${CLR}COMPILE AND RUN CHECK${NC}"

echo "$MAIN_TEST_DIR"
for file in "$GEN_TEST_DIR"/*.lang24; do
    cp "$file" test.lang24

    filename=$(basename "$file")
    if [ $filename == "io.lang24" ]; then continue; fi;
    filename_no_ext="${filename%.*}"


    SAVE_FILE="$RESULT/$filename_no_ext.log"
    if [ "$PRINT" == "print" ]; then SAVE_FILE="$RESULT"; fi

    echo -e "Testing ${YELLOW}$filename.${NC}"

    # Run make on the file
    "$JAVA" -cp ../bin:../src:"$ANTLRDIR" lang24.Compiler --xsl=../lib/xsl/ --logged-phase="$LOGGEDPHASE" --target-phase="$TARGETPHASE" --num-regs=32 test.lang24 && qemu-riscv64-static "test" 2>/dev/null > "$SAVE_FILE"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}QEMU failed for ${YELLOW}$filename${NC}"
        exit 1
    fi
    echo ""
done


echo "$MAIN_TEST_DIR"
for file in "$MAIN_TEST_DIR"/*.lang24; do
    cp "$file" test.lang24

    filename=$(basename "$file")
    filename_no_ext="${filename%.*}"

    SAVE_FILE="$RESULT/$filename_no_ext.log"
    if [ "$PRINT" == "print" ]; then SAVE_FILE="$RESULT"; fi

    echo -e "Testing ${YELLOW}$filename.${NC}"

    # Run make on the file
    "$JAVA" -cp ../bin:../src:"$ANTLRDIR" lang24.Compiler --xsl=../lib/xsl/ --logged-phase="$LOGGEDPHASE" --target-phase="$TARGETPHASE" --num-regs=32 &>/dev/null && qemu-riscv64-static "test" 2>/dev/null > "$SAVE_FILE"
    if [ $? -ne 0 ]; then
        echo -e "${RED}QEMU failed for ${YELLOW}$filename${NC}"
        exit 1
    fi
    echo ""
done

echo -e "${GREEN}All compile and run tests passed successfully.${NC}"

echo ""

echo -e "${CLR}REGRESSION TESTS${NC}"

if ! [ -d "$RESULT" ] || [ "$(ls $RESULT)" == "" ] || [ "$RESULT" == "/dev/stdout" ]; then
    echo "Skipping regression check, since outs are just getting generated or printing to stdout"
    exit 0
fi

for file in "./outs"/*.log; do
    filename=$(basename "$file")
    filename_no_ext="${filename%.*}"
    diff "./$RESULT/$filename" "./outs/$filename"
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}diff failed for ${YELLOW}$filename${NC}"
        exit 1
    fi

done

echo ""

echo -e "${GREEN}All regression tests passed successfully.${NC}"

exit 0

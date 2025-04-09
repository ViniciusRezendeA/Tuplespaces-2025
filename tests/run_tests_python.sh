#!/bin/bash

################################################
### PATHS (feel free to tweak paths accordingly)
CLI_PATH=${PWD}/../Client-Python  # Caminho para o cliente Python
TESTS_FOLDER=${PWD}
TESTS_OUT_EXPECTED=${TESTS_FOLDER}/expected
TESTS_OUTPUT=${TESTS_FOLDER}/test-outputs
PYTHON_EXEC=python 
################################################
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'
################################################

rm -rf $TESTS_OUTPUT
mkdir -p $TESTS_OUTPUT

cd $CLI_PATH

i=1
while :
do
    TEST=$(printf "%02d" $i)
    if [ -e ${TESTS_FOLDER}/input$TEST.txt ]
    then 
        # Executa o cliente Python e processa o output
        $PYTHON_EXEC client_main.py localhost:8002 1 < ${TESTS_FOLDER}/input$TEST.txt | \
            awk '/^>/ {found=1} found' | sed '1iClientMain' > ${TESTS_OUTPUT}/out$TEST.txt


        # Compara com a saída esperada
        DIFF=$(diff -b ${TESTS_OUTPUT}/out$TEST.txt ${TESTS_OUT_EXPECTED}/out$TEST.txt)
        if [ "$DIFF" != "" ] 
        then
            echo -e "${RED}[$TEST] TEST FAILED${NC}"
        else
            echo -e "${GREEN}[$TEST] TEST PASSED${NC}"
        fi
        i=$((i+1))
    else
        break
    fi
done

echo "Check the outputs of each test in ${TESTS_OUTPUT}."

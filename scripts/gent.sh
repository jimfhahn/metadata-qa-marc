#!/usr/bin/env bash

. ./setdir.sh
NAME=gent
# TYPE_PARAMS="--marcVersion GENT"
# MARC_DIR=${BASE_INPUT_DIR}/gent/marc/2019-06-05
# MASK=*.mrc
TYPE_PARAMS="--marcVersion GENT --alephseq"
MARC_DIR=${BASE_INPUT_DIR}/gent/marc/2020-09-19
MASK=*.export

. ./common-script

echo "DONE"
exit 0

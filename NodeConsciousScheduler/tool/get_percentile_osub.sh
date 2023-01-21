#!/bin/bash

# SETTING
NODE=150
ALGO=EasyBackfillingOC
NAME=UniLu-Gaia-2014-1
COUNT=count_job.sh
t=h
M=`seq 1 8`
RESULT_DIR=../

for i in ${M};
do
  fname=${RESULT_DIR}/*_${NAME}_${ALGO}_n${NODE}c*_M${i}/test.out
  cp -p $fname wk_M${i}.out

  bash ${COUNT} wk_M${i}.out ${t}

done

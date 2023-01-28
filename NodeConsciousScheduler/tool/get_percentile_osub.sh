#!/bin/bash

# SETTING
NODE=150
CORE=12
ALGO=EasyBackfillingOC
#NAME=UniLu-Gaia-2014-1_6512_alloced
#NAME=UniLu-Gaia-2014-1_15942_alloced
NAME=UniLu-Gaia-2014-1_25365_alloced
#COUNT=count_trsc.sh
#COUNT=count_job.sh
COUNT=count_rsc.sh
UTIL=get_utilization.sh
t=h
M=`seq 1 8`
#RESULT_DIR=/home/minami/work/20220721_accurate_int/NodeConsciousScheduler/NodeConsciousScheduler/result/6512_res
#RESULT_DIR=/home/minami/work/20220721_accurate_int/NodeConsciousScheduler/NodeConsciousScheduler/result/15942_res
RESULT_DIR=/home/minami/work/20220721_accurate_int/NodeConsciousScheduler/NodeConsciousScheduler/result/25365_res

for i in ${M};
do
  fname=${RESULT_DIR}/*_${NAME}_${ALGO}_n${NODE}c*_M${i}/test.out
  cp -p $fname wk_M${i}.out

  bash ${COUNT} wk_M${i}.out ${t}
  mv wk_count.out res_M${i}.out

  bash $UTIL wk_M${i}.out ${NODE} ${CORE}
  mv wk_makespan.out mkres_M${i}.out
done

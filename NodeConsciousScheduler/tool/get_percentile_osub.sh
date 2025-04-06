#!/bin/bash

# SETTING
#NODE=150
NODE=146 # R=3
CORE=12
ALGO=EasyBackfillingOC
#NAME=UniLu-Gaia-2014-1_alloced
NAME=UniLu-Gaia-2014-1_1762_only_batch
#NAME=UniLu-Gaia-2014-1_6500_alloced
#NAME=UniLu-Gaia-2014-1_15913_alloced
#NAME=UniLu-Gaia-2014-1_25365_alloced
COUNT1=count_job.sh
COUNT2=count_rsc.sh
COUNT3=count_trsc.sh
UTIL=get_utilization.sh
t=h
M=`seq 2 8`
#RESULT_DIR=/home/minami/work/20220721_accurate_int/NodeConsciousScheduler/NodeConsciousScheduler/result/6512_res
#RESULT_DIR=/home/minami/work/20220721_accurate_int/NodeConsciousScheduler/NodeConsciousScheduler/result/15942_res
RESULT_DIR=/home/minami/work/20220721_accurate_int/NodeConsciousScheduler/NodeConsciousScheduler/result/1762_res/osub_variable

for i in ${M};
do
  fname=${RESULT_DIR}/*_${NAME}_${ALGO}_n${NODE}c*_M${i}/test.out
  cp -p $fname wk_M${i}.out

  bash ${COUNT1} wk_M${i}.out ${t}
  mv wk_count.out res_job_M${i}.out
  bash ${COUNT2} wk_M${i}.out ${t}
  mv wk_count.out res_rsc_M${i}.out
  bash ${COUNT3} wk_M${i}.out ${t}
  mv wk_count.out res_trsc_M${i}.out

  bash $UTIL wk_M${i}.out ${NODE} ${CORE}
  mv wk_makespan.out mkres_M${i}.out
done

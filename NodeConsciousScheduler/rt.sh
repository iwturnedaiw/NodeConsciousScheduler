#! /bin/bash

LOG=./log/test_`date +%Y%m%d%H%M`.log
CASE="n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c32"
TP=gen01
DATADIR=./data-set
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
ALGORITHM=EasyBackfill
export CLASSPATH=./build/classes


test() {
  for c in ${CASE}
  do
    cp -p ./${DATADIR}/${TP}${c}.swf.machines ./${DATADIR}/${TP}.swf.machines
    RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
    java nodeconsciousscheduler.NodeConsciousScheduler > /dev/null 2>&1
    MASTER_FILE=./${MASTER}/${ALGORITHM}/${TP}/${c}/${FILENAME}
    diff --strip-trailing-cr ${MASTER_FILE} ${RESULT_FILE}
    RET=$?
    echo -n "${PATTER}\t${c}\t"
    if [ ${RET} -eq 0 ]; then
      echo "OK"
    else
      echo "NG"
    fi
  done
}



test | tee -a ${LOG}

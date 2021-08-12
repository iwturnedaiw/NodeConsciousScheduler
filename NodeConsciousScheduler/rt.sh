#! /bin/bash

LOG=./log/test_`date +%Y%m%d%H%M`.log

# configuration
#CASE="n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512" # for OC
#CASE="n8c32" # for OC
CASE="n8c32"
#CASE="n2c16"
#TP="gen01 gen02 short short1"
#TP="gen01 gen02"
#TP="short1"
#TP="gen02"
#TP="short short1"
TP="short1"
M=1
ALGORITHM=FCFS
#ALGORITHM=FCFSOC
#ALGORITHM=EasyBackfilling
#ALGORITHM=EasyBackfillingOC

# path
DATADIR=./data-set
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
TEMPLATE=template.machines
export CLASSPATH=./build/classes


test() {
  OC_FLAG=0
  CHECK=${ALGORITHM##*OC}
  LEN=${#CHECK}
  if [ ${LEN} -eq 0 ]; then
    OC_FLAG=1
  fi
  for tp in ${TP}
  do
    echo ${tp}
    for c in ${CASE}
    do
      core=${c#*c}
      node=${c%c*}
      node=${node#*n}
      echo "\tn${node} c${core}"
      sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
      sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
      RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
      java nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${ALGORITHM} ${M} > /dev/null 2>&1
      if [ ${OC_FLAG} -eq 0 ]; then
        MASTER_FILE=./${MASTER}/${ALGORITHM}/${tp}/${c}/${FILENAME}
        #diff --strip-trailing-cr ${MASTER_FILE} ${RESULT_FILE}
        python3 diff.py ${MASTER_FILE} ${RESULT_FILE} ${core}
      else
        echo "\tcheck by python"
        coreOC=$((core*M))
        algo=${ALGORITHM%OC}
        MASTER_FILE=./${MASTER}/${algo}/${tp}/n${node}c${coreOC}/${FILENAME}
        python3 diff.py ${MASTER_FILE} ${RESULT_FILE} ${core}
      fi
      RET=$?
      echo -n "\t\t${PATTERN}\t${c}\t"
      if [ ${RET} -eq 0 ]; then
        echo "OK"
      else
        echo "NG"
      fi
    done
  done
}



test | tee -a ${LOG}

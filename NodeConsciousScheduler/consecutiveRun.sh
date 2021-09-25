#! /bin/bash

LOG=./log/consecutiveRun_`date +%Y%m%d%H%M`.log

# configuration
CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
TP="gen03"
M="1"
ALGORITHM="FCFS EasyBackfilling"

# path
DATADIR=./data-set
RUN_SCRIPT=run.sh
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
TEMPLATE=template.machines
SH=bash
export CLASSPATH=./build/classes
CONCURRENT=4
SLEEP=2



test() {
  OC_FLAG=0
  CHECK=${ALGORITHM##*OC}
  LEN=${#CHECK}
  if [ ${LEN} -eq 0 ]; then
    OC_FLAG=1
  fi
  cnt=0
  for algorithm in ${ALGORITHM}
  do
    echo ${algorithm}

  for m in ${M}
  do
  echo "M=${m}"
  for tp in ${TP}
  do
    echo ${tp}
    for c in ${CASE}
    do
      core=${c#*c}
      node=${c%c*}
      node=${node#*n}
      #echo "\tn${node} c${core}"
      sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
      sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
      cnt=`jobs | grep Running | wc -l`
      while [ $cnt -ge $CONCURRENT ]
      do
        sleep $SLEEP
        cnt=`jobs | grep Running | wc -l`
      done
      echo -e "\t\t${algorithm}\tM=${m}\t${tp}\t${c}\tStarting...\t"
      ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1 &
    done
  done
  done
  done
  wait
}



test 2>&1 | tee -a ${LOG}

#!/bin/bash

LOG=./log/run_`date +%Y%m%d%H%M`.log


# path
DATADIR=./data-set
TEMPLATE=template.machines
export CLASSPATH=./build/classes


run() {
  if [ ${#} -ne 5 ]; then
    echo "Please specify the arguments"
    exit
  fi


  tp=${1}
  algorithm=${2}
  node=${3}
  core=${4}
  m=${5}

  case="n${node}c${core}"
 
  #echo -e "\t\t\tn${node} c${core}"
  sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
  sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
  java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 2>&1
  wait
  RET=$?
  echo -ne "${tp}\t${algorithm}\tM=${m}\t${case}\t"
  if [ ${RET} -eq 0 ]; then
    echo "OK"
    return 0
  else
    echo "NG"
    return 1
  fi
}





run ${1} ${2} ${3} ${4} ${5} 2>&1 | tee -a ${LOG}

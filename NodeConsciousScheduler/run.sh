#!/bin/bash

####################################################
# run.sh is the run driver.
# You set the folloing arguments:
#  TP
#  ALGORITHM
#  NODE
#  CORE
#  M
#
# usage:
#  $ pwd
#   ${INSTALLED_DIR}/NodeConsciousScheduler
#  $ bash run.sh ${TP} ${ALGORITHM} ${NODE} ${CORE} ${M}
#  For example:
#  $ bash run.sh gen01 FCFSOC 4 8 2
#
# return value:
#  0 (normal termination)
#  1 (otherwise)
####################################################

LOG=./log/run_`date +%Y%m%d%H%M`.log
CURRENT_DIR=`pwd`

# path
DATADIR=./data-set
TEMPLATE=template.machines
export CLASSPATH=./build/classes


run() {
  if [ ${#} -lt 5 ] || [ ${#} -gt 6 ]; then
    echo "Please specify the arguments"
    exit
  fi


  tp=${1}
  algorithm=${2}
  node=${3}
  core=${4}
  m=${5}
  memory=${6}
  if [ ${#} -eq 4 ]; then
    memory=104857600
  fi

  case="n${node}c${core}"
 
  #echo -e "\t\t\tn${node} c${core}"
  sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
  sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
  sed s/memory/${memory}/g -i ./${DATADIR}/${tp}.swf.machines
  java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 
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





run ${1} ${2} ${3} ${4} ${5} ${6} 2>&1 | tee -a ${LOG}

#!/bin/bash

LOG=./log/run_`date +%Y%m%d%H%M`.log
CURRENT_DIR=`pwd`

# path
DATADIR=./data-set
TEMPLATE=template.machines
export CLASSPATH=./build/classes

TAB=$'\t'

log_info() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') [INFO] $*"
}

log_warn() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') [WARN] $*"
}

log_error() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') [ERROR] $*"
}



run() {
  if [ ${#} -lt 5 ] || [ ${#} -gt 6 ]; then
    log_error "Please specify the arguments"
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
  ERROR_MSG=$(java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} 2>&1 1>/dev/null)
  RET=$?
  #echo -ne "${tp}\t${algorithm}\tM=${m}\t${case}\t"
  if [ ${RET} -eq 0 ]; then
    log_info "${tp}${TAB}${algorithm}${TAB}M=${m}${TAB}${case}${TAB}OK"
    return 0
  else
    log_info "${tp}${TAB}${algorithm}${TAB}M=${m}${TAB}${case}${TAB}NG"
    log_error "${ERROR_MSG}"
    return 1
  fi
}





run ${1} ${2} ${3} ${4} ${5} ${6} 2>&1 | tee -a ${LOG}
RET=${PIPESTATUS[0]}
exit $RET

#!/bin/bash

####################################################
# 
# run.sh is the run driver.
# You set the folloing arguments:
#  TP
#  ALGORITHM
#  NODE
#  CORE
#  M
#  [MEMORY]
#
# usage:
#  $ pwd
#   ${INSTALLED_DIR}/NodeConsciousScheduler
#  $ bash run.sh ${TP} ${ALGORITHM} ${NODE} ${CORE} ${M} [${MEMORY}]
#  For example:
#  $ bash run.sh gen01 FCFSOC 4 8 2
#
# return value:
#  0 (normal termination)
#  1 (otherwise)
# 
####################################################


# path
DATADIR=./data-set
TEMPLATE=template.machines
export CLASSPATH=./build/classes

TAB=$'\t'

FNAME=$(basename $0)

log_info() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $FNAME [INFO] $*"
}

log_warn() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $FNAME [WARN] $*"
}

log_error() {
  echo "$(date '+%Y-%m-%d %H:%M:%S') $FNAME [ERROR] $*"
}

tp=""
algorithm=""
node=""
core=""
m=""
memory=""

check_args() {
  if [ ${#} -lt 5 ] || [ ${#} -gt 6 ]; then
    return 1
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

  return 0
}

check_oc() {
  local var=${algorithm##*OC}
  local len=${#var}
  if [ ${len} -eq 0 ]; then
    return 1 
  fi
  return 0
}

run() {
  local tp=${1}
  local algorithm=${2}
  local node=${3}
  local core=${4}
  local m=${5}
  local memory=${6}

  case="n${node}c${core}"
 
  sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
  sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
  sed s/memory/${memory}/g -i ./${DATADIR}/${tp}.swf.machines
  ERROR_MSG=$(java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} 2>&1 1>/dev/null)
  RET=$?
  if [ ${RET} -eq 0 ]; then
    log_info "${tp}${TAB}${algorithm}${TAB}M=${m}${TAB}${case}${TAB}COMPLETED"
    return 0
  else
    log_info "${tp}${TAB}${algorithm}${TAB}M=${m}${TAB}${case}${TAB}NOT COMPLETED"
    log_error "${ERROR_MSG}"
    return 1
  fi
}


LOG=./log/run_`date +%Y%m%d%H%M%S`.log
check_args $@
RET=$?
if [ ${RET} -eq 1 ]; then
  log_error "Please specify the correct arguments, current $# arguments: $@" | tee -a ${LOG}
  exit ${RET}
fi

check_oc
RET=$?
if [ ${RET} -eq 0 -a ${m} -ne 1 ]; then
  prev_m=${m}
  m=1
  LOG=./log/run_${tp}_${algorithm}_N${node}_C${core}_M${m}_MEM${memory}_`date +%Y%m%d%H%M%S`.log
  log_warn "Current setting is OC, but m is specified ${prev_m}. m is changed m to 1" | tee -a ${LOG}
fi

LOG=./log/run_${tp}_${algorithm}_N${node}_C${core}_M${m}_MEM${memory}_`date +%Y%m%d%H%M%S`.log
run ${tp} ${algorithm} ${node} ${core} ${m} ${memory} 2>&1 | tee -a ${LOG}
RET=${PIPESTATUS[0]}
exit $RET

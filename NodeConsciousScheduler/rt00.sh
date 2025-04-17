#! /bin/bash

####################################################
# rt00.sh conducts testing if the simulator
# terminates normally 
# You set the folloing environment variables:
#  CASE
#  TP
#  M
#  ALGORITHM
#
# usage:
#  $ pwd
#   ${INSTALLED_DIR}/NodeConsciousScheduler
#  $ bash rt00.sh
#
####################################################

LOG=./log/rt00_`date +%Y%m%d%H%M`.log

# configuration
#CASE="n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512" # for OC
#CASE="n8c32" # for OC
#CASE="n2c16"
CASE="n1c16 n1c32"
#TP="gen01 gen02 gen03 short short1 hpc2n"
#TP="gen01 gen02 gen03"
#TP="short short1"
#TP="gen01 gen02"
#TP="short1"
TP="gen01 hpc2n"
#TP="short short1"
#TP="hpc2n"
#M=5
M="1 2 3 4 5 6 7 8"
#M=4
#ALGORITHMS=FCFS
#ALGORITHM=FCFSOC
#ALGORITHM=EasyBackfilling
#ALGORITHM=EasyBackfillingOC
ALGORITHMS="FCFS FCFSOC EasyBackfilling EasyBackfillingOC"

# path
DATADIR=./data-set
#RUN_SCRIPT=./tool/run_for_rt.sh
RUN_SCRIPT=./run.sh
RESULTDIR=./result
MASTER=./master
TEMPLATE=template.machines
SH=bash
export CLASSPATH=./build/classes

TAB=$'\t'
NL=$'\n'

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

test() {
  for ALGORITHM in ${ALGORITHMS}
  do
    OC_FLAG=0
    CHECK=${ALGORITHM##*OC}
    LEN=${#CHECK}
    if [ ${LEN} -eq 0 ]; then
      OC_FLAG=1
    fi
    log_info "Testing algorithm ${ALGORITHM}..."
    for m in ${M}
    do
      log_info "${TAB}Testing multiplicity ${m}..."
      if [ $OC_FLAG -eq 0 -a $m -gt 1 ]; then
        log_warn "${TAB}${TAB}Tests with multiplicity M=${m} are SKIP for ALGORITHM $ALGORITHM"
        log_info "${TAB}Done testing multiplicity ${m}"
	break
      fi
      for tp in ${TP}
      do
        log_info "${TAB}${TAB}Testing trace ${tp} ..."
        if [ ${tp} = "hpc2n" ]; then
          log_warn "${TAB}${TAB}${TAB}Configuration for trace ${tp} is changed to n120c2..."
          c="n120c2"
          core=${c#*c}
          node=${c%c*}
          node=${node#*n}
          sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
          sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
          ${SH} ./${RUN_SCRIPT} ${tp} ${ALGORITHM} ${node} ${core} ${m} > /dev/null 2>&1
          wait
          RET=$?
	  RET_STR=""
          if [ ${RET} -eq 0 ]; then
            log_info "${TAB}${TAB}${TAB}ALGORITHM=${ALGORITHM} M=${m} ${tp} ${c} OK"
          else
            log_error "${TAB}${TAB}${TAB}ALGORITHM=${ALGORITHM} M=${m} ${tp} ${c} NG"
          fi
          log_info "${TAB}${TAB}${TAB}ALGORITHM=${ALGORITHM} M=${m} ${tp} ${c} ${RET_STR}"
        else 
          for c in ${CASE}
          do
            core=${c#*c}
            node=${c%c*}
            node=${node#*n}
            sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
            sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
            ${SH} ./${RUN_SCRIPT} ${tp} ${ALGORITHM} ${node} ${core} ${m} > /dev/null 2>&1
            #wait
            RET=$?
            if [ ${RET} -eq 0 ]; then
              log_info "${TAB}${TAB}${TAB}ALGORITHM=${ALGORITHM} M=${m} ${tp} ${c} OK"
            else
              log_error "${TAB}${TAB}${TAB}ALGORITHM=${ALGORITHM} M=${m} ${tp} ${c} NG"
            fi
          done
        fi
        log_info "${TAB}${TAB}Done testing trace ${tp} ..."
      done
      log_info "${TAB}Done testing multiplicity ${m}"
    done
    log_info "Done testing algorithm ${ALGORITHM}"
  done
}



test 2>&1 | tee -a ${LOG}

#!/bin/bash

####################################################
# rt04.sh conducts tests by checking for inconsistencies.
# You specify the list to an argument.
#
# list format:
#  ${ALGORITHM}  M=${M} ${TP} ${CASE} [OK|NG]
#
# usage:
#  $ pwd
#   ${INSTALLED_DIR}/NodeConsciousScheduler
#  $ cat tp.list
#  FCFSOC  M=1     gen03   n8c32   NG
#  EasyBackfillingOC       M=1     gen03   n8c32   NG 
#  $ bash rt04.sh tp.list
#
####################################################


LOG=./log/rt04_`date +%Y%m%d%H%M`.log

# path
DATADIR=./data-set
RESULTDIR=./result
MASTER=./master
RUN_SCRIPT=./tool/run_for_rt.sh
PYTHON_SCRIPT=./tool/vis_oc_enabled.py
FILENAME=./for_visualization.out
TEMPLATE=template.machines
export CLASSPATH=./build/classes
SH=bash
PYTHON=python3
CURRENT_HOME=`pwd`
TMP_DIR=${CURRENT_HOME}/exec_dir


test() {
  LIST_FILE=${1}

  cat ${LIST_FILE} | while read l
  do
    IFS="$(echo -e '\t' )"
    record=($l)
    unset $IFS

    algorithm=${record[0]}
    m=${record[1]}
    m=${m#*=}
    tp=${record[2]}
    case=${record[3]}

    OC_FLAG=0
    CHECK=${algorithm##*OC}
    LEN=${#CHECK}
    if [ ${LEN} -eq 0 ]; then
      OC_FLAG=1
    fi
 
    core=${case#*c}
    node=${case%c*}
    node=${node#*n}
    echo -e "\t\t\tn${node} c${core}"
    sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
    sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
    ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
    RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${case}_*_M${m} | head -n 1`
    RESULT_FILE+=/${FILENAME}
    wait
    mkdir ${TMP_DIR}
    cd ${TMP_DIR}
    ${PYTHON} ${CURRENT_HOME}/${PYTHON_SCRIPT} ${CURRENT_HOME}/${RESULT_FILE} ${node} ${core} ${m} > /dev/null
    RET=$?
    echo -ne "\t\t\t\t${algorithm}\tM=${m}\t${tp}\t${case}\t"
    if [ ${RET} -eq 0 ]; then
      echo "OK"
    else
    echo "NG"
    fi
    cd ${CURRENT_HOME}
    rm -r ${TMP_DIR}
  done
}

test ${1} 2>&1 | tee -a ${LOG}

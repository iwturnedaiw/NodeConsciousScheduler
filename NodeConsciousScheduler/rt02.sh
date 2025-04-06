#!/bin/bash

####################################################
# rt02.sh conducts tests comparing master data
# which is located at ./master
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
#  $ bash rt02.sh tp.list
#
####################################################

LOG=./log/rt02_`date +%Y%m%d%H%M`.log


# path
DATADIR=./data-set
RUN_SCRIPT=./tool/run_for_rt.sh
PYTHON_SCRIPT=./tool/diff.py
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
TEMPLATE=template.machines
SH=bash
PYTHON=python3
export CLASSPATH=./build/classes


test() {
  if [ ${#} -ne 1 ]; then
    echo "Please specify the arguments"
    exit
  fi

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
    #RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
    ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
    wait
    RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${case}_*_M${m} | head -n 1`
    RESULT_FILE+=/${FILENAME}
    if [ ${OC_FLAG} -eq 1 ]; then
      MASTER_FILE=./${MASTER}/${algorithm}_M${m}/${tp}/${case}/${FILENAME}
    else
      MASTER_FILE=./${MASTER}/${algorithm}/${tp}/${case}/${FILENAME}
    fi
    ${PYTHON} ${PYTHON_SCRIPT} ${MASTER_FILE} ${RESULT_FILE} ${core}
    RET=$?
    echo -ne "\t\t\t\t${algorithm}\tM=${m}\t${tp}\t${case}\t"
    if [ ${RET} -eq 0 ]; then
      echo "OK"
    else
    echo "NG"
    fi
  done
}





test ${1} | tee -a ${LOG}

#!/bin/bash

LOG=./log/revistest_`date +%Y%m%d%H%M`.log

# path
DATADIR=./data-set
RESULTDIR=./result
MASTER=./master
#FILENAME=./test.out
FILENAME=./for_visualization.out
TEMPLATE=template.machines
export CLASSPATH=./build/classes
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
    RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
    java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 2>&1
    wait
    if [ ${OC_FLAG} -eq 1 ]; then
      MASTER_FILE=./${MASTER}/${algorithm}_M${m}/${tp}/${case}/${FILENAME}
    else
      MASTER_FILE=./${MASTER}/${algorithm}/${tp}/${case}/${FILENAME}
    fi
    mkdir ${TMP_DIR}
    cd ${TMP_DIR}
    python3 ${CURRENT_HOME}/vis_oc_enabled.py ${CURRENT_HOME}/${RESULT_FILE} ${node} ${core} ${m} > /dev/null
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

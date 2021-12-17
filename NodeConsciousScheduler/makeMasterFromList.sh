#!/bin/bash

LOG=./log/retest_`date +%Y%m%d%H%M`.log


# path
DATADIR=./data-set
RUN_SCRIPT=run.sh
PYTHON_SCRIPT=diff.py
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
TEMPLATE=template.machines
TMPBASE=./tmpdir
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
    echo -e "\t\t\t${algoritm}\tM=${m}\ttp=${tp}\t${c}\tGenerating..."
    sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
    sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
    #RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
    ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
    wait
    RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${case}_*_M${m} | head -n 1`
    RESULT_FILE+=/${FILENAME}
    TMPDIR=./${TMPBASE}/${algorithm}_M${m}/${tp}/${case}
		mkdir -p ${TMPDIR}
    cp -p ${RESULT_FILE} ${TMPDIR}
    echo -e "\t\t\t${algoritm}\tM=${m}\ttp=${tp}\t${c}\tDone"
  done
}





test ${1} | tee -a ${LOG}

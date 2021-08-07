#! /bin/bash

LOG=./log/makeMaster_`date +%Y%m%d%H%M`.log
CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c32"
TP=gen02
DATADIR=./data-set
RESULTDIR=./result
MASTER=./master
TMPBASE=./tmpdir
FILENAME=./test.out
ALGORITHM=EasyBackfilling
TEMPLATE=template.machines
export CLASSPATH=./build/classes


test() {
  for c in ${CASE}
  do
    core=${c#*c}
    node=${c%c*}
    node=${node#*n}
    echo n${node} c${core}
    #cat ./${DATADIR}/${TEMPLATE}
    sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${TP}.swf.machines
    sed s/core/${core}/g -i ./${DATADIR}/${TP}.swf.machines
    #cat ./${DATADIR}/${TP}.swf.machines

    RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
    java nodeconsciousscheduler.NodeConsciousScheduler ${TP}.swf ${ALGORITHM} > /dev/null 2>&1
    TMPDIR=./${TMPBASE}/${ALGORITHM}/${TP}/${c}
		mkdir -p ${TMPDIR}
    cp -p ${RESULT_FILE} ${TMPDIR}
  done
}



test | tee -a ${LOG}


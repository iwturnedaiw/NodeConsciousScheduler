#! /bin/bash

LOG=./log/makeMaster_`date +%Y%m%d%H%M`.log
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024"
#CASE="n1c192 n16c64"
#CASE="n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48"
#CASE="n2c16 n4c16 n8c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64" #for FCFSOC, M=2. gen01
#CASE="n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64" #for FCFSOC, M=2. gen02
#CASE="n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n2c64 n4c64 n8c64" #for FCFSOC, M=2. gen03
#CASE="n8c32 n4c64" #for FCFSOC, M=2. short
CASE="n8c64"
#TP="gen01 gen02 gen03 short short1 hpc2n"
TP="short"
#TP="gen01 hpc2n gen02"
#TP="gen01 gen02 gen03"
#TP="gen01"
#TP="short"
#M="2 3 4 5 6 7 8"
M="7"
#TP="short1"
#TP="short1"
DATADIR=./data-set
RESULTDIR=./result
MASTER=./master
TMPBASE=./tmpdir
FILENAME=./test.out
#ALGORITHM=EasyBackfilling
#ALGORITHM="FCFSOC EasyBackfillingOC"
ALGORITHM="EasyBackfillingOC"
#ALGORITHM="FCFSOC"
#ALGORITHM=EasyBackfillingOC
#ALGORITHM=FCFSOC
#ALGORITHM=FCFS
TEMPLATE=template.machines
export CLASSPATH=./build/classes


test() {
  for algorithm in ${ALGORITHM}
  do
    echo ${algorithm}
  for m in ${M}
  do
    echo "\tM=${m}"
  for tp in ${TP}
  do
    echo "\t\t${tp}"

    if [ ${tp} = "hpc2n" ]; then
      c=n120c2
      core=${c#*c}
      node=${c%c*}
      node=${node#*n}
      echo "\t\t\t${algoritm}\tM=${m}\ttp=${tp}\t${c}\tGenerating..."
      #cat ./${DATADIR}/${TEMPLATE}
      sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
      sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
      #cat ./${DATADIR}/${TP}.swf.machines

      RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
      java nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 2>&1
      TMPDIR=./${TMPBASE}/${algorithm}_M${m}/${tp}/${c}
		  mkdir -p ${TMPDIR}
      cp -p ${RESULT_FILE} ${TMPDIR}
      echo "\t\t\t${algoritm}\tM=${m}\ttp=${tp}\t${c}\tDone"
      continue
    fi

    for c in ${CASE}
    do
      core=${c#*c}
      node=${c%c*}
      node=${node#*n}
      echo "\t\t\t${algoritm}\tM=${m}\ttp=${tp}\t${c}\tGenerating..."
      #cat ./${DATADIR}/${TEMPLATE}
      sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
      sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
      #cat ./${DATADIR}/${TP}.swf.machines

      RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
      java nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 2>&1
      TMPDIR=./${TMPBASE}/${algorithm}_M${m}/${tp}/${c}
		  mkdir -p ${TMPDIR}
      cp -p ${RESULT_FILE} ${TMPDIR}
      echo "\t\t\t${algoritm}\tM=${m}\ttp=${tp}\t${c}\tDone"
    done
  done
  done
  done
}



test | tee -a ${LOG}


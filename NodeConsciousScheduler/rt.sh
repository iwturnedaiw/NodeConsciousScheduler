#!/bin/bash

LOG=./log/test_`date +%Y%m%d%H%M`.log

# configuration
#CASE="n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512" # for OC
#CASE="n8c32" # for OC
#CASE="n1c192 n16c64 n8c32"
#CASE="n1c16"
#CASE="n4c16"
TP="gen01 gen02 gen03 short short1 hpc2n"
#TP="gen03"
#TP="hpc2n"
#TP="gen01 gen03 short"
#TP="short"
#TP="gen01 gen02 gen03 hpc2n"
#TP="hpc2n"
#TP="short"
#TP="gen03"
M="2 3 4 5 6 7 8"
#M="1"
#ALGORITHM=FCFS
#ALGORITHM=FCFSOC
#ALGORITHM="EasyBackfilling"
#ALGORITHM="FCFS EasyBackfilling"
#ALGORITHM="FCFS EasyBackfilling FCFSOC EasyBackfillingOC"
#ALGORITHM="FCFSOC EasyBackfillingOC"
#ALGORITHM="EasyBackfillingOC"
#ALGORITHM="FCFS EasyBackfilling EasyBackfillingOC"
ALGORITHM="FCFSOC EasyBackfillingOC"
#ALGORITHM="FCFS FCFSOC"
#ALGORITHM=EasyBackfillingOC

# path
DATADIR=./data-set
RUN_SCRIPT=run.sh
PYTHON_SCRIPT=diff.py
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
TEMPLATE=template.machines
SH=bash
PYTHON=python3
export CLASSPATH=./build/classes


test() {
  OC_FLAG=0
  CHECK=${ALGORITHM##*OC}
  LEN=${#CHECK}
  if [ ${LEN} -eq 0 ]; then
    OC_FLAG=1
  fi
  for algorithm in ${ALGORITHM}
  do
    echo ${algorithm}
    for m in ${M}
    do
      echo -e "\tM=${m}"
      for tp in ${TP}
      do
        echo -e "\t\t${tp}"
        if [ ${tp} = "hpc2n" ]; then
          c="n120c2"
          core=${c#*c}
          node=${c%c*}
          node=${node#*n}
          echo -e "\t\t\tn${node} c${core}"
          sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
          sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
          #RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
          #java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 2>&1
          ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
          RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${c}_M${m} | head -n 1`
          RESULT_FILE+=/${FILENAME}
          if [ ${OC_FLAG} -eq 1 ]; then
            MASTER_FILE=./${MASTER}/${algorithm}_M${m}/${tp}/${c}/${FILENAME}
          else
            MASTER_FILE=./${MASTER}/${algorithm}/${tp}/${c}/${FILENAME}
          fi
          ${PYTHON} ${PYTHON_SCRIPT} ${MASTER_FILE} ${RESULT_FILE} ${core}
          RET=$?
          echo -ne "\t\t\t\t${algorithm}\tM=${m}\t${tp}\t${c}\t"
          if [ ${RET} -eq 0 ]; then
            echo "OK"
          else
            echo "NG"
          fi
        else 
          for c in ${CASE}
          do
            core=${c#*c}
            node=${c%c*}
            node=${node#*n}
            echo -e "\t\t\tn${node} c${core}"
            sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
            sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
            #RESULT_FILE=./${RESULTDIR}/`date +%Y%m%d%H%M`/${FILENAME}
            #java -ea nodeconsciousscheduler.NodeConsciousScheduler ${tp}.swf ${algorithm} ${m} > /dev/null 2>&1
            ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
            wait
    #      if [ ${OC_FLAG} -eq 0 ]; then
    #        MASTER_FILE=./${MASTER}/${algorithm}/${tp}/${c}/${FILENAME}
    #        #diff --strip-trailing-cr ${MASTER_FILE} ${RESULT_FILE}
    #        python3 diff.py ${MASTER_FILE} ${RESULT_FILE} ${core}
    #      else
    #        echo "\tcheck by python"
    #        coreOC=$((core*M))
    #        algo=${algorithm%OC}
    #        MASTER_FILE=./${MASTER}/${algo}/${tp}/n${node}c${coreOC}/${FILENAME}
    #        python3 diff.py ${MASTER_FILE} ${RESULT_FILE} ${core}
    #      fi
            #RESULT_FILE=`ls ./${RESULTDIR}/*_${tp}_${c}_M${m}/${FILENAME}`
            #RESULT_FILE=`ls ./${RESULTDIR}/*_${tp}_${algorithm}_${c}_M${m}/${FILE_NAME}`
            RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${c}_M${m} | head -n 1`
            RESULT_FILE+=/${FILENAME}
            if [ ${OC_FLAG} -eq 1 ]; then
              MASTER_FILE=./${MASTER}/${algorithm}_M${m}/${tp}/${c}/${FILENAME}
            else
              MASTER_FILE=./${MASTER}/${algorithm}/${tp}/${c}/${FILENAME}
            fi
            ${PYTHON} ${PYTHON_SCRIPT} ${MASTER_FILE} ${RESULT_FILE} ${core}
            RET=$?
            echo -ne "\t\t\t\t${algorithm}\tM=${m}\t${tp}\t${c}\t"
            if [ ${RET} -eq 0 ]; then
              echo "OK"
            else
              echo "NG"
            fi
          done
        fi
      done
    done
  done
}



test | tee -a ${LOG}

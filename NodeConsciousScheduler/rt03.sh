#!/bin/bash

####################################################
# rt03.sh conducts tests by checking for inconsistencies.
# You set the folloing environment variables:
#  CASE
#  TP
#  M
#  ALGORITHM
#
# usage:
#  $ pwd
#   ${INSTALLED_DIR}/NodeConsciousScheduler
#  $ bash rt03.sh
#
####################################################

LOG=./log/rt03_`date +%Y%m%d%H%M`.log

# configuration
#CASE="n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512"
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512" # for OC
#CASE="n8c32" # for OC
#CASE="n1c192 n16c64 n8c32"
CASE="n20c80"
#CASE="n1c32"
#TP="gen01 gen02 gen03 short short1 hpc2n"
#TP="wagap-2013"
TP="hpc2n"
#TP="gen01 gen03 short"
#TP="short"
#TP="gen01 gen02 gen03 hpc2n"
#TP="hpc2n"
#TP="short"
#TP="gen03"
#M="2 3 4 5 6 7 8"
M="1"
#ALGORITHM=FCFS
#ALGORITHM=FCFSOC
#ALGORITHM="EasyBackfilling"
ALGORITHM="FCFS EasyBackfilling"
#ALGORITHM="FCFS EasyBackfilling FCFSOC EasyBackfillingOC"
#ALGORITHM="FCFSOC EasyBackfillingOC"
#ALGORITHM="EasyBackfillingOC"
#ALGORITHM="FCFS EasyBackfilling EasyBackfillingOC"
#ALGORITHM="FCFSOC EasyBackfillingOC"
#ALGORITHM="FCFS FCFSOC"
#ALGORITHM=EasyBackfillingOC

# path
DATADIR=./data-set
RUN_SCRIPT=./tool/run_for_rt.sh
PYTHON_SCRIPT=./tool/vis_oc_enabled.py
RESULTDIR=./result
MASTER=./master
FILENAME=./for_visualization.out
TEMPLATE=template.machines
SH=bash
PYTHON=python3
export CLASSPATH=./build/classes
CURRENT_HOME=`pwd`
TMP_DIR=${CURRENT_HOME}/exec_dir


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
          ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
          wait
          RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${c}_*_M${m} | head -n 1`
          RESULT_FILE+=/${FILENAME}
          mkdir ${TMP_DIR}
          cd ${TMP_DIR}
          ${PYTHON} ${CURRENT_HOME}/${PYTHON_SCRIPT} ${CURRENT_HOME}/${RESULT_FILE} ${node} ${core} ${m} > /dev/null 2>&1
          RET=$?
          echo -ne "\t\t\t\t${algorithm}\tM=${m}\t${tp}\t${c}\t"
          if [ ${RET} -eq 0 ]; then
            echo "OK"
          else
            echo "NG"
          fi
          cd ${CURRENT_HOME}
          rm -r ${TMP_DIR}
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
            ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} > /dev/null 2>&1
            wait
            RESULT_FILE=`ls -td ./${RESULTDIR}/*_${tp}_${algorithm}_${c}_*_M${m} | head -n 1`
            RESULT_FILE+=/${FILENAME}
            mkdir ${TMP_DIR}
            cd ${TMP_DIR}
            ${PYTHON} ${CURRENT_HOME}/${PYTHON_SCRIPT} ${CURRENT_HOME}/${RESULT_FILE} ${node} ${core} ${m} > /dev/null 2>&1
            RET=$?
            echo -ne "\t\t\t\t${algorithm}\tM=${m}\t${tp}\t${c}\t"
            if [ ${RET} -eq 0 ]; then
              echo "OK"
            else
              echo "NG"
            fi
            cd ${CURRENT_HOME}
            rm -r ${TMP_DIR}
          done
        fi
      done
    done
  done
}



test | tee -a ${LOG}

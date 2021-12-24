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
CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512 n1c192 n2c64 n4c64 n8c64 n16c64 n1c1024" # for non-OC
#CASE="n1c16 n2c16 n4c16 n8c16 n16c16 n1c48 n1c96 n1c32 n2c32 n4c32 n8c32 n16c32 n1c64 n1c128 n1c256 n1c512" # for OC
#CASE="n8c32" # for OC
#CASE="n2c16"
#CASE="n1c512"
#TP="gen01 gen02 gen03 short short1 hpc2n"
#TP="gen01 gen02 gen03"
#TP="short short1"
#TP="gen01 gen02"
#TP="short1"
#TP="gen03"
#TP="short short1"
TP="hpc2n"
#M=5
M="2 3 4 5 6 7 8"
#M=4
#ALGORITHM=FCFS
#ALGORITHM=FCFSOC
#ALGORITHM=EasyBackfilling
ALGORITHM=EasyBackfillingOC

# path
DATADIR=./data-set
RUN_SCRIPT=./tool/run_for_rt.sh
RESULTDIR=./result
MASTER=./master
TEMPLATE=template.machines
SH=bash
export CLASSPATH=./build/classes


test() {
  OC_FLAG=0
  CHECK=${ALGORITHM##*OC}
  LEN=${#CHECK}
  if [ ${LEN} -eq 0 ]; then
    OC_FLAG=1
  fi
  echo ${ALGORITHM}
  for m in ${M}
  do
  echo "M=${m}"
  for tp in ${TP}
  do
    echo ${tp}
    if [ ${tp} = "hpc2n" ]; then
      c="n120c2"
      core=${c#*c}
      node=${c%c*}
      node=${node#*n}
      echo -e "\tn${node} c${core}"
      sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
      sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
      ${SH} ./${RUN_SCRIPT} ${tp} ${ALGORITHM} ${node} ${core} ${m} > /dev/null 2>&1
      wait
      RET=$?
      echo -en "\t\tM=${m}\t${tp}\t${c}\tRUNCHECK\t"
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
      #echo "\tn${node} c${core}"
      echo -e "\t${tp}\t${c}\tRUNCHECK\t"
      sed s/node/${node}/g ./${DATADIR}/${TEMPLATE} > ./${DATADIR}/${tp}.swf.machines
      sed s/core/${core}/g -i ./${DATADIR}/${tp}.swf.machines
      ${SH} ./${RUN_SCRIPT} ${tp} ${ALGORITHM} ${node} ${core} ${m} > /dev/null 2>&1
      wait
      RET=$?
      echo -en "\t\tM=${m}\t${tp}\t${c}\tRUNCHECK\t"
      if [ ${RET} -eq 0 ]; then
        echo "OK"
      else
        echo "NG"
      fi
    done
    fi
  done
  done
}



test 2>&1 | tee -a ${LOG}

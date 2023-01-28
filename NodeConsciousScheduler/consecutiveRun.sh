#! /bin/bash

LOG=./log/consecutiveRun_`date +%Y%m%d%H%M`.log

# configuration
CASE="n150c12" # for non-OC
#CASE="n120c2" # for non-OC
MEMORY="67108864"
#TP="UniLu-Gaia-2014-1_alloced"
#TP="UniLu-Gaia-2014-1_6512_alloced UniLu-Gaia-2014-1_15942_alloced UniLu-Gaia-2014-1_25365_alloced"
#TP="UniLu-Gaia-2014-1_6512m_alloced UniLu-Gaia-2014-1_15942m_alloced UniLu-Gaia-2014-1_25365m_alloced UniLu-Gaia-2014-1_34801m_alloced UniLu-Gaia-2014-1_39502m_alloced"
TP="UniLu-Gaia-2014-1_6500_alloced UniLu-Gaia-2014-1_15913_alloced UniLu-Gaia-2014-1_25336_alloced UniLu-Gaia-2014-1_34772_alloced UniLu-Gaia-2014-1_39473_alloced"
#TP="hpc2n_int"
M="1 2 3 4 5 6 7 8"
#M="1 2 3 6 7 8"
#M="4 5"
#M="4"
#ALGORITHM="FCFS EasyBackfilling"
ALGORITHM="FCFSOC EasyBackfillingOC"
#ALGORITHM="FCFSOC"
#ALGORITHM="EasyBackfillingOC"

# path
DATADIR=./data-set
RUN_SCRIPT=run.sh
RESULTDIR=./result
MASTER=./master
FILENAME=./test.out
TEMPLATE=template.machines.UniLu-Gaia-2014-1
SH=bash
export CLASSPATH=./build/classes
CONCURRENT=18
SLEEP=2
SSLEEP=1



test() {
  OC_FLAG=0
  CHECK=${ALGORITHM##*OC}
  LEN=${#CHECK}
  if [ ${LEN} -eq 0 ]; then
    OC_FLAG=1
  fi
  cnt=0
  for algorithm in ${ALGORITHM}
  do
    echo ${algorithm}

  for m in ${M}
  do
  echo "M=${m}"
  for tp in ${TP}
  do
    echo ${tp}
    for c in ${CASE}
    do
      core=${c#*c}
      node=${c%c*}
      node=${node#*n}
      #echo "\tn${node} c${core}"
      cnt=`jobs | grep Running | wc -l`
      while [ $cnt -ge $CONCURRENT ]
      do
        sleep $SLEEP
        cnt=`jobs | grep Running | wc -l`
      done
      echo -e "\t\t${algorithm}\tM=${m}\t${tp}\t${c}\tStarting...\t"
      ${SH} ./${RUN_SCRIPT} ${tp} ${algorithm} ${node} ${core} ${m} ${memory} > /dev/null 2>&1 &
      sleep $SSLEEP
    done
  done
  done
  done
  wait
}



test 2>&1 | tee -a ${LOG}

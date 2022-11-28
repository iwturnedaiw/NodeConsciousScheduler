#!/bin/bash

#CORE=20
CORE=12
ALGO="FCFS EasyBackfilling"

### slowdown
shukei () {
  ARG=$1
  ARG2=$2
  for algo in $ALGO
  do
    echo $algo
    for node in $NODE
    do
      for m in $M
      do
        if [ $ARG -eq 12 ] && [ $ARG2 -eq 0 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 != "0") print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 12 ] && [ $ARG2 -eq 1 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 != "0" && $8 > 600) print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 12 ] && [ $ARG2 -eq 2 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0") print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 12 ] && [ $ARG2 -eq 3 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0" && $8 > 600) print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 0 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 != "0") print $5}' | sort -n | tail -n 1
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 1 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 != "0") {a+=1; b+=$5}}; END{print a, b, b/a}' 
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 2 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0") print $5}' | sort -n | tail -n 1
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 3 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0") {a+=1; b+=$5}}; END{print a, b, b/a}' 
        elif [ $ARG -eq 10 ] && [ $ARG2 -eq 0 ]; then
          cat ./*_${TP}_${algo}*_n${node}c${CORE}_MEM_OFF_M${m}/test.out | awk '{a=$7; b+=$10*$15}; END{print b, a, b/a/N/C}' N=$node C=$CORE
        fi
      done
    done
  done
}

<< COMMENTOUT
NODE="1 4 7 10 15"
TP="UniLu-Gaia-2014-1_only_int"
M="1"
echo "### only_int"
echo "### maximumSlowdown"
shukei 12 0
echo "### maximumSlowdown(T>600)"
shukei 12 1
echo "### maximumWaitingTime"
shukei 5 0
echo "### averagedWaitingTime"
shukei 5 1
echo "### util"
shukei 10 0

NODE="150 149 148 145 142 139 135"
TP="UniLu-Gaia-2014-1_only_batch"
M="1"
echo "### only_batch"
echo "### maximumSlowdown"
shukei 12 0
echo "### maximumSlowdown(T>600)"
shukei 12 1
echo "### maximumWaitingTime"
shukei 5 0
echo "### averagedWaitingTime"
shukei 5 1
echo "### util"
shukei 10 0
COMMENTOUT

#NODE="1152"
NODE="150"
#TP="KIT-FH2-2016-1"
TP="UniLu-Gaia-2014-1"
M="1 2 3 4 5 6 7 8"
echo "### OC system"
echo "### maximumSlowdown"
shukei 12 0
echo "### maximumSlowdown(T>600)"
shukei 12 1
echo "### maximumSlowdown(int)"
shukei 12 2
echo "### maximumSlowdown(int, T>600)"
shukei 12 3
echo "### maximumWaitingTime"
shukei 5 0
echo "### averagedWaitingTime"
shukei 5 1
echo "### maximumWaitingTime(int)"
shukei 5 2
echo "### averagedWaitingTime(int)"
shukei 5 3
echo "### util"
shukei 10 0

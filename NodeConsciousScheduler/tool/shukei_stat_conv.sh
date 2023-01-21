#!/bin/bash

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
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 12 ] && [ $ARG2 -eq 1 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{if ($8 > 600) print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 12 ] && [ $ARG2 -eq 2 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0") print $12}' | sort -n | tail -n 1
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 0 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{print $5}' | sort -n | tail -n 1
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 1 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{a+=1; b+=$5}; END{print a, b, b/a}' 
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 2 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0") print $5}' | sort -n | tail -n 1
        elif [ $ARG -eq 5 ] && [ $ARG2 -eq 3 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{if ($14 == "0") {a+=1; b+=$5}}; END{print a, b, b/a}' 
        elif [ $ARG -eq 10 ] && [ $ARG2 -eq 0 ]; then
          cat ./*_${TP}_${algo}*_n${node}c12_MEM_OFF_M${m}/test.out | awk '{a=$7; b+=$10*$15}; END{print b, a, b/a/N/C}' N=$node C=$CORE
        fi
      done
    done
  done
}

NODE="1 4 7 10 15"
#NODE="10 15 18 22 30"
#NODE="30 37 45 52 60"
TP="UniLu-Gaia-2014-1_only_int"
#TP="UniLu-Gaia-2014-1_only_int_plus4756"
#TP="UniLu-Gaia-2014-1_only_int_plus15953"
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

NODE="150 149 146 143 140 135"
#NODE="150 139 135 132 127 120"
#NODE="150 120 112 105 97 90"
TP="UniLu-Gaia-2014-1_only_batch"
#TP="UniLu-Gaia-2014-1_only_batch_minus4756"
#TP="UniLu-Gaia-2014-1_only_batch_minus15953"
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


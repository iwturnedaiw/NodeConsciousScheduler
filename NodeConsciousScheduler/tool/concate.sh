#! /bin/bash

# SETTING
NODE=150
ALGO=EasyBackfilling
BNAME=UniLu-Gaia-2014-1_only_batch
INAME=UniLu-Gaia-2014-1_only_int
ISTART=431217 # ARRIVAL TIME OF FIRST INTERACTIVE JOB
RESULT_DIR=../

function concate() {
  r=$1
  inode=`bc <<< $NODE*$r/100`
  bnode=$((NODE - inode))
  bfname=${RESULT_DIR}/*_${BNAME}_${ALGO}_n${bnode}c*/test.out
  ifname=${RESULT_DIR}/*_${INAME}_${ALGO}_n${inode}c*/test.out
  cp -p ${bfname} b.out
  cp -p ${ifname} i.out
  wk=wkfile.out

  awk -v ista=${ISTART} \
   '{ft=$7; if (ft == "finishedTime") {next;}; ft = ft + ista; $7 = ft; print $0}' \
   i.out > im.out
  sed -i "s/ /	/g" im.out
  cat b.out im.out | sort -n -k 7 > ${wk}
 
  mv ${wk} wk_R${r}.out
  rm b.out i.out im.out 
}

concate $1

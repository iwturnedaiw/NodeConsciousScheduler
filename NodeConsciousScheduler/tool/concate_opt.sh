#! /bin/bash

# SETTING
NODE=150
ALGO=EasyBackfillingOC
#BNAME=UniLu-Gaia-2014-1_1762_only_batch
#BNAME=UniLu-Gaia-2014-1_6500_only_batch
#BNAME=UniLu-Gaia-2014-1_15913_only_batch
#BNAME=UniLu-Gaia-2014-1_25336_only_batch
#BNAME=UniLu-Gaia-2014-1_34772_only_batch
BNAME=UniLu-Gaia-2014-1_39473_only_batch
#INAME=UniLu-Gaia-2014-1_1762_int_alloced
#INAME=UniLu-Gaia-2014-1_6500_int_alloced
#INAME=UniLu-Gaia-2014-1_15913_int_alloced
#INAME=UniLu-Gaia-2014-1_25336_int_alloced
#INAME=UniLu-Gaia-2014-1_34772_int_alloced
INAME=UniLu-Gaia-2014-1_39473_int_alloced
ISTART=431217 # ARRIVAL TIME OF FIRST INTERACTIVE JOB
RESULT_DIR=/home/minami/work/20230128_ovh/NodeConsciousScheduler/NodeConsciousScheduler/result/39473/MQ

function concate() {
  m=$1
  mi=4
  r=18
  inode=`bc <<< $NODE*$r/100`
  bnode=$((NODE - inode))
  bfname=${RESULT_DIR}/*_${BNAME}_${ALGO}_n${bnode}c*_M${m}/test.out
  ifname=${RESULT_DIR}/*_${INAME}_${ALGO}_n${inode}c*_M${mi}/test.out
  cp -p ${bfname} b.out
  cp -p ${ifname} i.out
  wk=wkfile.out

  awk -v ista=${ISTART} \
   '{ft=$7; if (ft == "finishedTime") {next;}; ft = ft + ista; $7 = ft; print $0}' \
   i.out > im.out
  sed -i "s/ /	/g" im.out
  cat b.out im.out | sort -n -k 7 > ${wk}
 
  mv ${wk} wk_M${m}.out
  rm b.out i.out im.out 
}

concate $1

#!/bin/bash

# SETTING
#JOBNUM=51871
JOBNUM=50109 # only_batch
PAR90=`bc <<< $JOBNUM*0.90`
PAR95=`bc <<< $JOBNUM*0.95`
PAR90=`echo ${PAR90} | awk '{r=int($1); print (r==$1) ? r : r+1}'`
PAR95=`echo ${PAR95} | awk '{r=int($1); print (r==$1) ? r : r+1}'`

function shukei() {
  file=$1
  day=$((60*60*24))
  hour=$((60*60))
  minute=$((60))
  second=$((1))

  if [[ $2 = "d" ]]; then 
    t=$day
  elif [[ $2 = "h" ]]; then
    t=$hour
  elif [[ $2 = "m" ]]; then
    t=$minute
  elif [[ $2 = "s" ]]; then
    t=$second
  fi

  awk -v THRESHOLD=$t -v PAR90=$PAR90 -v PAR95=$PAR95 \
      'BEGIN{u=THRESHOLD; i=0; cnt=0; par90=0; par95=0};
       {ft=$7; if (ft == "finishedTime") {next;}; while (ft > i * u) {print i, cnt; i = i + 1; }; {cnt = cnt + 1}; if (cnt == PAR90) {par90 = i;} if (cnt == PAR95) {par95 = i;} } END{print i, cnt} END{print par90, par95}' \
      ${file} > wk_count.out
}


file=$1
t=$2
shukei ${file} ${t}

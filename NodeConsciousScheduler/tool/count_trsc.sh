#!/bin/bash

# SETTING
JOBNUM=51871
#SUM_RSC=512097
SUM_CPUTIME=6915622385 # 1762
SUM_CPUTIME=6905481383 # 1762, only_batch
#SUM_CPUTIME=6885289677 # 6500
#SUM_CPUTIME=6859583133 # 15942
#SUM_CPUTIME=6764598847 # 25365

PAR90=`bc <<< $SUM_CPUTIME*0.90`
PAR95=`bc <<< $SUM_CPUTIME*0.95`
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
      '{OFMT="%.f"} BEGIN{u=THRESHOLD; i=0; cnt=0; par90=0; par95=0};
       {ft=$7; if (ft == "finishedTime") {next;}; while (ft > i * u) {print i, cnt; i = i + 1; }; {cnt = cnt + $10 * $15}; if (par90 == 0 && cnt >= PAR90) {par90 = i;} if (par95 == 0 && cnt >= PAR95) {par95 = i;} } END{print i, cnt} END{print par90, par95}' \
      ${file} > wk_count.out
}


file=$1
t=$2
shukei ${file} ${t}

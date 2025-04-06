#!/bin/bash

# SETTING

function shukei() {
  file=$1
  node=$2
  core=$3

  awk -v node=${node} -v core=${core} \
      'BEGIN{i=0; cnt=0; endTime=0};
       { ft=$7; if (ft == "finishedTime") {next;};
         cnt += $10 * $15; endTime=$7;
       };
       END{ d=endTime * core * node; u = cnt/d};
       END{print u}' \
      ${file} > wk_makespan.out
}


file=$1
shukei ${file} $2 $3

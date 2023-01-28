CONC=concate.sh
COUNT=count_trsc.sh
UTIL=get_utilization.sh
t=h
R="1 3 5 7 10"
NODE=150
CORE=12
for i in $R;
do
  bash $CONC $i
  bash $COUNT wk_R${i}.out $t
  mv wk_count.out res_R${i}.out
  bash $UTIL wk_R${i}.out ${NODE} ${CORE}
  mv wk_makespan.out mkres_R${i}.out
done

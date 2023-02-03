CONC=concate.sh
COUNT1=count_job.sh
COUNT2=count_rsc.sh
COUNT3=count_trsc.sh
UTIL=get_utilization.sh
t=h
# 15913, 20 25 30 35 40
R="$@"
NODE=150
CORE=12
for i in $R;
do
  bash $CONC $i
  bash $COUNT1 wk_R${i}.out $t
  mv wk_count.out res_job_R${i}.out
  bash $COUNT2 wk_R${i}.out $t
  mv wk_count.out res_rsc_R${i}.out
  bash $COUNT3 wk_R${i}.out $t
  mv wk_count.out res_trsc_R${i}.out
  bash $UTIL wk_R${i}.out ${NODE} ${CORE}
  mv wk_makespan.out mkres_R${i}.out
done

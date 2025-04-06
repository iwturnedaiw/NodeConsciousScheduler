CONC=concate_opt.sh
COUNT1=count_job.sh
COUNT2=count_rsc.sh
COUNT3=count_trsc.sh
UTIL=get_utilization.sh
t=h
# 15913, 20 25 30 35 40
#R=2 # for 1762, 6500
#R=6 # for 15913
#R=12 # for 25336
R=18 # for 34772, 39473
NODE=150
CORE=12
M=`seq 8 8`
for m in $M;
do
  bash $CONC $m
  bash $COUNT1 wk_M${m}.out $t
  mv wk_count.out res_job_M${m}.out
  bash $COUNT2 wk_M${m}.out $t
  mv wk_count.out res_rsc_M${m}.out
  bash $COUNT3 wk_M${m}.out $t
  mv wk_count.out res_trsc_M${m}.out
  bash $UTIL wk_M${m}.out ${NODE} ${CORE}
  mv wk_makespan.out mkres_M${m}.out
done

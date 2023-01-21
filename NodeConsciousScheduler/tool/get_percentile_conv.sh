CONC=concate.sh
COUNT=count_job.sh
t=d
R="1 3 5 7 10"
for i in $R;
do
  bash $CONC $i
  bash $COUNT wk_R${i}.out $t
done

#awk 'BEGIN{cnt=0; icnt=0} {cnt += 1; if ($15 == 0) {icnt += 1}} END{print icnt/cnt, icnt, cnt }' $1
#awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $5; cnt += 1; if ($15 == 0) {isum += $5; icnt += 1}} END{print isum/sum, isum, sum}' $1
#awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $5*$4; cnt += 1; if ($15 == 0) {isum += $5*$4; icnt += 1}} END{print isum/sum, isum, sum}' $1

REDUC_RATIO=25
awk -v ratio="$REDUC_RATIO" 'BEGIN{cnt=0; icnt=0; res1=0; res2=0; ires1=0; ires2=0;} {bcore= ($5/ratio >= 1) ? int($5/ratio) : 1; icore=$5; core = bcore; if ($15 == 0) {core = icore;} ;res1+=core; res2+=$4*core; cnt += 1; if ($15 == 0) {ires1+=core; ires2+=$4*core; icnt += 1}} END{print icnt, cnt, icnt/cnt, ires1, res1, ires1/res1, ires2, res2, ires2/res2}' $1

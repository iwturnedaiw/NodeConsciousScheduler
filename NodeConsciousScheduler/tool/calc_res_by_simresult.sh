awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15; cnt += 1; if ($14 == 0) {isum += $15; icnt += 1}} END{print isum/sum, icnt/cnt}' $1
awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15; cnt += 1; if ($14 == 0) {isum += $15; icnt += 1}} END{print isum/sum, }' $1
awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15*$8; cnt += 1; if ($14 == 0) {isum += $15*$8; icnt += 1}} END{print isum/sum, }' $1
awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15*$8; cnt += 1; if ($14 == 0) {isum += $15*$8; icnt += 1}} END{print isum/sum, isum, sum }' $1
awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15*$10; cnt += 1; if ($14 == 0) {isum += $15*$10; icnt += 1}} END{print isum/sum, isum, sum }' $1
awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15*$10; cnt += 1; if ($14 == 0) {isum += $15*$10; icnt += 1}} END{print isum/sum, isum, sum }' $1
awk 'BEGIN{sum=0; isum=0; cnt=0; icnt=0} {sum += $15; cnt += 1; if ($14 == 0) {isum += $15; icnt += 1}} END{print isum/sum, isum, sum }' $1


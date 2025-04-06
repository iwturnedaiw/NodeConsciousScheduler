awk 'BEGIN{spos=-1; epos=0; cnt=0; peak_flag=0; util=0;} {
    if (spos != -1) {
        if ($2 != 0.0) {
            cnt += 1;
            util += $2;
            if ($2 == 1.0) {
                peak_flag=1;
            }
        } else {
            epos = $1;
            cnt += 1
            if (peak_flag == 1) {
                printf "[%s, %s] (%sh) %s\n", spos-1, epos, cnt, util/cnt;
                peak_flag=0;
            }
            spos = -1; # Reset spos after printing
        }
    } else {
        if ($2 != 0.0) {
            spos=$1; cnt = 1; util=$2
        }
        if ($2 == 1.0) {
            peak_flag=1;
        }
    }
} END {
    if (peak_flag == 1) {
        print spos, epos, cnt; # Handle the case when the peak extends till the end of the file
    }
}' executing_resources_hour.out

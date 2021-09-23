/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

/**
 *
 * @author sminami
 */
public class Constants {
    static int UNUSED = -1;
    static int UNUPDATED = -1;
    static int TS_ENDTIME = 1 << 30;
    static int CANNOT_START = -1;
    static int UNSTARTED = -1;
    static int BLANK_JOBID = -1;
    static int NOTSPECIFIED = -1;
    static String RESULT_DIRECTORY = "result";
    static String FINISH_ORDER_JOB_OUTPUT = "test.out";
    static String FOR_VISUALIZATION_OUTPUT = "for_visualization.out";
    static String UTILIZATION_RATIO_OUTPUT = "utilization.out";
    static String SLOWDOWN_OUTPUT = "slowdown.out";
    static String CUMULATIVE_JOB_PER_DAY_OUTPUT = "cumulative_job_day.out";
    static int DAY_IN_SECOND = 60 * 60 * 24;
    static String CUMULATIVE_JOB_PER_HOUR_OUTPUT = "cumulative_job_hour.out";
    static int HOUR_IN_SECOND = 60 * 60;
    static String CUMULATIVE_JOB_PER_MINUTE_OUTPUT = "cumulative_job_minute.out";
    static int MINUTE_IN_SECOND = 60;
}

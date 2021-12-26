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
    static int UNSPECIFIED = -1;
    static int TS_ENDTIME = 1 << 30;
    static int CANNOT_START = -1;
    static int UNSTARTED = -1;
    static int BLANK_JOBID = -1;
    static int NOTSPECIFIED = -1;
    static String CONFIGURATION_FILE = "configuration.properties";
    static String RESULT_DIRECTORY = "result";
    static String DATASET_DIRECTORY = "data-set";
    static String FINISH_ORDER_JOB_OUTPUT = "test.out";
    static String FOR_VISUALIZATION_OUTPUT = "for_visualization.out";
    static String UTILIZATION_RATIO_OUTPUT = "utilization.out";
    static String SLOWDOWN_OUTPUT = "slowdown.out";
    static String SLOWDOWN_OC_OUTPUT = "slowdown_oc.out";
    static String RESULT_EACH_USER = "each_user_result.out";
    static String RESULT_EACH_GROUP = "each_group_result.out";    
    
    static String INSTANT_UTILIZATION_RATIO_DAY_OUTPUT = "instant_utilization_ratio_day.out";
    static String INSTANT_UTILIZATION_RATIO_HOUR_OUTPUT = "instant_utilization_ratio_hour.out";
    static String INSTANT_UTILIZATION_RATIO_MINUTE_OUTPUT = "instant_utilization_ratio_minute.out";

    static String INSTANT_UTILIZATION_RATIO_OC_DAY_OUTPUT = "instant_utilization_ratio_OC_day.out";
    static String INSTANT_UTILIZATION_RATIO_OC_HOUR_OUTPUT = "instant_utilization_ratio_OC_hour.out";
    static String INSTANT_UTILIZATION_RATIO_OC_MINUTE_OUTPUT = "instant_utilization_ratio_OC_minute.out";

    static String CUMULATIVE_FINISHED_JOB_PER_DAY_OUTPUT = "cumulative_finished_job_day.out";
    static String CUMULATIVE_FINISHED_JOB_PER_HOUR_OUTPUT = "cumulative_finished_job_hour.out";
    static String CUMULATIVE_FINISHED_JOB_PER_MINUTE_OUTPUT = "cumulative_finished_job_minute.out";

    static String WAITING_JOB_PER_DAY_OUTPUT = "waiting_job_day.out";
    static String WAITING_JOB_PER_HOUR_OUTPUT = "waiting_job_hour.out";
    static String WAITING_JOB_PER_MINUTE_OUTPUT = "waiting_job_minute.out";    

    static String ARRIVAL_JOB_PER_DAY_OUTPUT = "arrival_job_day.out";
    static String ARRIVAL_JOB_PER_HOUR_OUTPUT = "arrival_job_hour.out";
    static String ARRIVAL_JOB_PER_MINUTE_OUTPUT = "arrival_job_minute.out";    

    static String START_JOB_PER_DAY_OUTPUT = "start_job_day.out";
    static String START_JOB_PER_HOUR_OUTPUT = "start_job_hour.out";
    static String START_JOB_PER_MINUTE_OUTPUT = "start_job_minute.out";    

    static String FINISHED_JOB_PER_DAY_OUTPUT = "finished_job_day.out";
    static String FINISHED_JOB_PER_HOUR_OUTPUT = "finished_job_hour.out";
    static String FINISHED_JOB_PER_MINUTE_OUTPUT = "finished_job_minute.out";    
    
    static String CUMULATIVE_STARTED_JOB_PER_DAY_OUTPUT = "cumulative_started_job_day.out";
    static String CUMULATIVE_STARTED_JOB_PER_HOUR_OUTPUT = "cumulative_started_job_hour.out";
    static String CUMULATIVE_STARTED_JOB_PER_MINUTE_OUTPUT = "cumulative_started_job_minute.out";    
    
    static String WAITING_RESOURCES_PER_DAY_OUTPUT = "waiting_resources_day.out";
    static String WAITING_RESOURCES_PER_HOUR_OUTPUT = "waiting_resources_hour.out";
    static String WAITING_RESOURCES_PER_MINUTE_OUTPUT = "waiting_resources_minute.out";

    static String EXECUTING_RESOURCES_PER_DAY_OUTPUT = "executing_resources_day.out";
    static String EXECUTING_RESOURCES_PER_HOUR_OUTPUT = "executing_resources_hour.out";
    static String EXECUTING_RESOURCES_PER_MINUTE_OUTPUT = "executing_resources_minute.out";
    
    static String WAITING_MEMORY_RESOURCES_PER_DAY_OUTPUT = "waiting_memory_resources_day.out";
    static String WAITING_MEMORY_RESOURCES_PER_HOUR_OUTPUT = "waiting_memory_resources_hour.out";
    static String WAITING_MEMORY_RESOURCES_PER_MINUTE_OUTPUT = "waiting_memory_resources_minute.out";

    static String EXECUTING_MEMORY_RESOURCES_PER_DAY_OUTPUT = "executing_memory_resources_day.out";
    static String EXECUTING_MEMORY_RESOURCES_PER_HOUR_OUTPUT = "executing_memory_resources_hour.out";
    static String EXECUTING_MEMORY_RESOURCES_PER_MINUTE_OUTPUT = "executing_memory_resources_minute.out";
    
    static int DAY_IN_SECOND = 60 * 60 * 24;
    static int HOUR_IN_SECOND = 60 * 60;
    static int MINUTE_IN_SECOND = 60;

    static int NOT_FINISHED = 2 << 30;
}

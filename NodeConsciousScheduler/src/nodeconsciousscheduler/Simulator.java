/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Math.min;
import static java.lang.StrictMath.max;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.Constants.ARRIVAL_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_MEMORY_RESOURCES_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_MEMORY_RESOURCES_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_MEMORY_RESOURCES_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_RESOURCES_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_RESOURCES_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_RESOURCES_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_FINISHED_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_FINISHED_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_FINISHED_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_STARTED_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_STARTED_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_STARTED_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.DAY_IN_SECOND;
import static nodeconsciousscheduler.Constants.EXECUTING_MEMORY_RESOURCES_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.EXECUTING_MEMORY_RESOURCES_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.EXECUTING_MEMORY_RESOURCES_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.EXECUTING_RESOURCES_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.EXECUTING_RESOURCES_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.EXECUTING_RESOURCES_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISHED_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISHED_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISHED_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISH_ORDER_JOB_OUTPUT;
import static nodeconsciousscheduler.Constants.FOR_VISUALIZATION_OUTPUT;
import static nodeconsciousscheduler.Constants.HOUR_IN_SECOND;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_MEMORY_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_MEMORY_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_MEMORY_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_OC_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_OC_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_OCCUPANCY_OC_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_WASTED_RESOURCE_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_WASTED_RESOURCE_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_WASTED_RESOURCE_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_WASTED_RESOURCE_SECOND_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_WASTED_RESOURCE_TIMESERIES_OUTPUT;
import static nodeconsciousscheduler.Constants.MINUTE_IN_SECOND;
import static nodeconsciousscheduler.Constants.OCCUPANCY_OUTPUT;
import static nodeconsciousscheduler.Constants.RESULT_DIRECTORY;
import static nodeconsciousscheduler.Constants.RESULT_EACH_GROUP;
import static nodeconsciousscheduler.Constants.RESULT_EACH_USER;
import static nodeconsciousscheduler.Constants.SECOND;
import static nodeconsciousscheduler.Constants.SLOWDOWN_OC_OUTPUT;
import static nodeconsciousscheduler.Constants.SLOWDOWN_OUTPUT;
import static nodeconsciousscheduler.Constants.START_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.START_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.START_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.START_TIME;
import nodeconsciousscheduler.Constants.ScheduleConsiderJobType;
import nodeconsciousscheduler.Constants.TimeDesc;
import nodeconsciousscheduler.Constants.OsubOverheadModelType;
import static nodeconsciousscheduler.Constants.UNUPDATED;
import static nodeconsciousscheduler.Constants.WAITING_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_MEMORY_RESOURCES_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_MEMORY_RESOURCES_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_MEMORY_RESOURCES_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_RESOURCES_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_RESOURCES_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_RESOURCES_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.WASTED_RESOURCE_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.WASTED_RESOURCE_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.WASTED_RESOURCE_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.WASTED_RESOURCE_OF_SYSTEM;


/**
 *
 * @author sminami
 */
public class Simulator {
    private ArrayList<Job> jobList;
    private Map<Integer, Job> jobMap = new HashMap<>();
    private ArrayList<NodeInfo> allNodesInfo;
    private ScheduleAlgorithm scheAlgo;
    private Scheduler sche;
    private EventQueue evq;
    private ArrayList<Job> executingJobList;
    private ArrayList<Job> completedJobList;
    private PrintWriter pw;
    private PrintWriter pwForVis;
    private PrintWriter pwForUtilMinute;
    private PrintWriter pwForUtilHour;
    private PrintWriter pwForUtilDay;
    private PrintWriter pwForWastedResourceMinute;
    private PrintWriter pwForWastedResourceHour;
    private PrintWriter pwForWastedResourceDay;
    private PrintWriter pwForWastedResourceSecond;
    private PrintWriter pwForWastedResourceTimeseries;

    private Path p;
    ArrayList<Double> thresholdForSlowdown;
    private boolean outputMinuteBoolean;
    private boolean scheduleUsingMemory;
    private boolean crammingMemoryScheduling;
    private boolean considerJobMatching;
    Map<JobMatching, Double> jobMatchingTable = new HashMap<>();
    private boolean usingAffinityForSchedule;
    private double thresholdForAffinitySchedule;
    private boolean outputUtilizationRatio;
    private boolean outputSecondWastedResources;
    private ScheduleConsiderJobType scheduleConsiderJobType;
    private OsubOverheadModelType osubOverheadModelType;
    private double osubOverheadConst;

    
    Simulator(ArrayList<Job> jobList, ArrayList<NodeInfo> allNodesInfo, ScheduleAlgorithm scheAlgo, SimulatorConfiguration simConf) {
        this.jobList = jobList;
        for (int i = 0; i < jobList.size(); ++i) {
            Job ljob = jobList.get(i);
            int jobId = ljob.getJobId();
            this.jobMap.put(jobId, ljob);
        }
        this.allNodesInfo = allNodesInfo;
        this.scheAlgo = scheAlgo;
        this.outputUtilizationRatio = simConf.isOutputUtilizationRatio();
        initScheduler(scheAlgo);
        this.outputMinuteBoolean = simConf.isOutputMinuteTimeseries();
        this.outputSecondWastedResources = simConf.isOutputSecondWastedResources();
        makeEventQueue();
        this.executingJobList = new ArrayList<Job>();
        this.completedJobList = new ArrayList<Job>();
        this.thresholdForSlowdown = simConf.getThresholdForSlowdown();
        this.scheduleUsingMemory = simConf.isScheduleUsingMemory();
        this.crammingMemoryScheduling = simConf.isCrammingMemoryScheduling();
        this.considerJobMatching = simConf.isConsiderJobMatching();
        this.jobMatchingTable = simConf.getJobMatchingTable();
        this.usingAffinityForSchedule = simConf.isUsingAffinityForSchedule();
        this.thresholdForAffinitySchedule = simConf.getThresholdForAffinitySchedule();
        this.scheduleConsiderJobType = simConf.getScheduleConsiderJobType();
        this.osubOverheadModelType = simConf.getOsubOverheadModelType();
        if (this.osubOverheadModelType == OsubOverheadModelType.CONST) {
            this.osubOverheadConst = simConf.getOsubOverheadConst();
        }
        this.p = obtainPath();
        try {
            initOutputResult();
        } catch (IOException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void run() {
        while (evq.size() > 0) {
//            Event ev = (Event) evq.poll();
//            System.out.println("Job Id: " + ev.getJob().getJobId() + ", Submission time " + ev.getJob().getSubmitTime());
            advance();
        }
        finalizeSimulation();        
     }
    
    private void makeEventQueue() {
        PriorityQueue<Event> pq = new EventQueue();
        evq = (EventQueue) pq;
        
        if (isOutputUtilizationRatio()) {
            evq.enqueueUtilizationMeasuringEvent(START_TIME, this.outputMinuteBoolean);
            evq.enqueueWastedResourceMeasuringEvent(START_TIME, this.outputMinuteBoolean, this.outputSecondWastedResources);
        }
        
        for (int i = 0; i < jobList.size(); ++i) {
            Job job = jobList.get(i);
            evq.enqueueJob(job);
        }
    }

    private void advance() {
        evq.handle();
    }


    private void initScheduler(ScheduleAlgorithm scheAlgo) {
        if (scheAlgo == ScheduleAlgorithm.FCFS) {
            this.sche = new FCFS();
            NodeConsciousScheduler.M = 1;
        } else if (scheAlgo == ScheduleAlgorithm.EasyBackfilling) {
            this.sche = new EasyBackfilling();
            NodeConsciousScheduler.M = 1;            
        } else if (scheAlgo == ScheduleAlgorithm.FCFSOC) {
            this.sche = new FCFSOC();
        } else if (scheAlgo == ScheduleAlgorithm.EasyBackfillingOC) {
            this.sche = new EasyBackfillingOC();
        }
        /*
        } else if (scheAlgo == ScheduleAlgorithm.ConservativeBackfiling) {
            this.sche = new ConservativeBackfilling);
        }        
        */
    }

    public ArrayList<Job> getJobList() {
        return jobList;
    }

    public ArrayList<NodeInfo> getAllNodesInfo() {
        return allNodesInfo;
    }

    public ScheduleAlgorithm getScheAlgo() {
        return scheAlgo;
    }

    public Scheduler getSche() {
        return sche;
    }

    public EventQueue getEvq() {
        return evq;
    }

    public ArrayList<Job> getExecutingJobList() {
        return executingJobList;
    }

    public void setJobList(ArrayList<Job> jobList) {
        this.jobList = jobList;
    }

    public void setAllNodesInfo(ArrayList<NodeInfo> allNodesInfo) {
        this.allNodesInfo = allNodesInfo;
    }

    public void setScheAlgo(ScheduleAlgorithm scheAlgo) {
        this.scheAlgo = scheAlgo;
    }

    public void setSche(Scheduler sche) {
        this.sche = sche;
    }

    public void setEvq(EventQueue evq) {
        this.evq = evq;
    }

    public void setExecutingJobList(ArrayList<Job> executingJobList) {
        this.executingJobList = executingJobList;
    }
 
    private Path obtainPath() {
        String dir = RESULT_DIRECTORY;

        Calendar cl = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        String date = sdf.format(cl.getTime());

        String fname[] = NodeConsciousScheduler.fname.split(".swf");
        
        String memoryInfo = "OFF";
        if (this.scheduleUsingMemory) {
            memoryInfo = "ON";
        }
        
        dir += "/" + date + "_" + fname[0] + "_" + this.scheAlgo + "_n" + NodeConsciousScheduler.numNodes + "c" + NodeConsciousScheduler.numCores + "_MEM_" + memoryInfo + "_M" + NodeConsciousScheduler.M;
        Path p = Paths.get(dir);
        return p;
    }

    void initOutputResult() throws IOException {
        Files.createDirectories(this.p);
        
        String fileName = FINISH_ORDER_JOB_OUTPUT;
        String fileNameForVis = FOR_VISUALIZATION_OUTPUT;
        String fileNameUtilHour = INSTANT_UTILIZATION_RATIO_HOUR_OUTPUT;
        String fileNameUtilDay = INSTANT_UTILIZATION_RATIO_DAY_OUTPUT;
        String fileNameUtilMinute = INSTANT_UTILIZATION_RATIO_MINUTE_OUTPUT;
        String fileNameWastedResourceHour = INSTANT_WASTED_RESOURCE_HOUR_OUTPUT;
        String fileNameWastedResourceDay = INSTANT_WASTED_RESOURCE_DAY_OUTPUT;
        String fileNameWastedResourceMinute = INSTANT_WASTED_RESOURCE_MINUTE_OUTPUT;        
        String fileNameWastedResourceSecond = INSTANT_WASTED_RESOURCE_SECOND_OUTPUT;        
        String fileNameWastedResourceTimeseries = INSTANT_WASTED_RESOURCE_TIMESERIES_OUTPUT;        

        try {
            this.pw = new PrintWriter(this.p + "/" + fileName);
            pw.println("JobID\tuserId\tgroupId\tarrivalTime\twaitTime\tstartTime\tfinishedTime\toriginalRunningTime\trunnningTime\tcpuTimePerCore\tslowdown\tslowdownOC\tspecifiedRequiredTime\tqueueNum\tnumCores\tnumNodes\tnodeNum(tcoreNum)");
            this.pwForVis = new PrintWriter(this.p + "/" + fileNameForVis);
            if (outputUtilizationRatio) {
                this.pwForUtilHour = new PrintWriter(this.p + "/" + fileNameUtilHour);
                this.pwForUtilDay = new PrintWriter(this.p + "/" + fileNameUtilDay);
                writeUtilFileHeader(pwForUtilHour);
                writeUtilFileHeader(pwForUtilDay);
                this.pwForWastedResourceHour = new PrintWriter(this.p + "/" + fileNameWastedResourceHour);
                this.pwForWastedResourceDay = new PrintWriter(this.p + "/" + fileNameWastedResourceDay);
                writeUtilFileHeader(pwForWastedResourceHour);
                writeUtilFileHeader(pwForWastedResourceDay);
                if (outputMinuteBoolean) {
                    this.pwForUtilMinute = new PrintWriter(this.p + "/" + fileNameUtilMinute);
                    writeUtilFileHeader(pwForUtilMinute);
                    this.pwForWastedResourceMinute = new PrintWriter(this.p + "/" + fileNameWastedResourceMinute);
                    writeUtilFileHeader(pwForWastedResourceMinute);
                }
                if (outputSecondWastedResources) {
                    this.pwForWastedResourceSecond = new PrintWriter(this.p + "/" + fileNameWastedResourceSecond);
                    writeUtilFileHeader(pwForWastedResourceSecond);
                }
                this.pwForWastedResourceTimeseries = new PrintWriter(this.p + "/" + fileNameWastedResourceTimeseries);
                writeUtilFileHeader(pwForWastedResourceTimeseries);
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void outputResult(Job job) {
        
        int jobId = job.getJobId();
        int userId = job.getUserId();
        int groupId = job.getGroupId();
        int arrivalTime = job.getSubmitTime();
        int waitTime = job.getWaitTime();
        int startTime = job.getStartTime();
        int finishedTime = job.getFinishedTime();
        int runningTime = job.getRunningTimeDed() + job.getRunningTimeOC();
        boolean interactiveJob = job.isInteracitveJob();
        if (interactiveJob) {
            runningTime += job.getPrologTime() + job.getSumIdleTime() + job.getEpilogTIme();            
        }
        double slowdown = max(1.0, (double) (waitTime + runningTime) /runningTime);
        job.setSlowdown(slowdown);
        int originalRunningTime = job.getActualExecuteTime();
        double slowdownOC = max(1.0, (double) (waitTime + runningTime) /originalRunningTime);
        job.setSlowdownByOriginalRunningTime(slowdownOC);
        int numCores = job.getRequiredCores();
        int numNodes = job.getRequiredNodes();
        int specifiedRequiredTime = job.getRequiredTime();
        double cpuTimePerCore = job.getAccumulatedCpuTime();
        int queueNum = job.getQueueNum();
        
        pw.print(jobId + "\t" + userId + "\t" + groupId + "\t" + arrivalTime + "\t" + waitTime + "\t" + startTime + "\t" + finishedTime + "\t" + originalRunningTime + "\t" + runningTime + "\t" + cpuTimePerCore + "\t"
                + slowdown + "\t" + slowdownOC + "\t" + specifiedRequiredTime + "\t" + queueNum + "\t" + numCores + "\t" + numNodes + "\t");
        
        ArrayList<UsingNode> usingNodesList = job.getUsingNodesList();
        Collections.sort(usingNodesList);
        for (int i = 0; i < numNodes; ++i) {
            UsingNode usingNode = usingNodesList.get(i);
            int nodeNo = usingNode.getNodeNum();
            int numUsingCores = usingNode.getNumUsingCores();
            ArrayList<Integer> usingCoreNum = usingNode.getUsingCoreNum();
            
            pw.print(nodeNo + "(");

            for (int j = 0; j < numUsingCores; ++j) {
                int coreNum = usingCoreNum.get(j);
                pw.print(coreNum);
                if (j != numUsingCores - 1) pw.print(",");
            }
            pw.print(")\t");
        }
        pw.println();

    }

    public ArrayList<Job> getCompletedJobList() {
        return completedJobList;
    }

    public void setCompletedJobList(ArrayList<Job> completedJobList) {
        this.completedJobList = completedJobList;
    }

    
    public void makeResults() {
        
        outputOccupancy();
        outputInstatntOccupancy(INSTANT_OCCUPANCY_DAY_OUTPUT, DAY_IN_SECOND, false);
        outputInstatntOccupancy(INSTANT_OCCUPANCY_HOUR_OUTPUT, HOUR_IN_SECOND, false);
        outputInstatntOccupancy(INSTANT_OCCUPANCY_OC_DAY_OUTPUT, DAY_IN_SECOND, true);
        outputInstatntOccupancy(INSTANT_OCCUPANCY_OC_HOUR_OUTPUT, HOUR_IN_SECOND, true);

        outputWastedResource(WASTED_RESOURCE_HOUR_OUTPUT, HOUR_IN_SECOND);
        outputWastedResource(WASTED_RESOURCE_DAY_OUTPUT, DAY_IN_SECOND);
        
        if (scheduleUsingMemory) {
            outputInstatntMemoryUtilizationRatio(INSTANT_OCCUPANCY_MEMORY_DAY_OUTPUT, DAY_IN_SECOND);
            outputInstatntMemoryUtilizationRatio(INSTANT_OCCUPANCY_MEMORY_HOUR_OUTPUT, HOUR_IN_SECOND);
        }
        
        outputSlowdown(false);
        outputSlowdown(true);
        outputResultEachUserAndGroup();

        outputWaitingAndNewArrivalJobAndStartJobAndFinishedJob(WAITING_JOB_PER_DAY_OUTPUT, ARRIVAL_JOB_PER_DAY_OUTPUT, START_JOB_PER_DAY_OUTPUT, CUMULATIVE_STARTED_JOB_PER_DAY_OUTPUT, FINISHED_JOB_PER_DAY_OUTPUT, CUMULATIVE_FINISHED_JOB_PER_DAY_OUTPUT, WAITING_RESOURCES_PER_DAY_OUTPUT, EXECUTING_RESOURCES_PER_DAY_OUTPUT, ARRIVAL_RESOURCES_PER_DAY_OUTPUT,WAITING_MEMORY_RESOURCES_PER_DAY_OUTPUT, EXECUTING_MEMORY_RESOURCES_PER_DAY_OUTPUT, ARRIVAL_MEMORY_RESOURCES_PER_DAY_OUTPUT, DAY_IN_SECOND);
        outputWaitingAndNewArrivalJobAndStartJobAndFinishedJob(WAITING_JOB_PER_HOUR_OUTPUT, ARRIVAL_JOB_PER_HOUR_OUTPUT, START_JOB_PER_HOUR_OUTPUT, CUMULATIVE_STARTED_JOB_PER_HOUR_OUTPUT, FINISHED_JOB_PER_HOUR_OUTPUT, CUMULATIVE_FINISHED_JOB_PER_HOUR_OUTPUT, WAITING_RESOURCES_PER_HOUR_OUTPUT, EXECUTING_RESOURCES_PER_HOUR_OUTPUT, ARRIVAL_RESOURCES_PER_HOUR_OUTPUT, WAITING_MEMORY_RESOURCES_PER_HOUR_OUTPUT, EXECUTING_MEMORY_RESOURCES_PER_HOUR_OUTPUT, ARRIVAL_MEMORY_RESOURCES_PER_HOUR_OUTPUT, HOUR_IN_SECOND);
        
        if (outputMinuteBoolean) {
            outputInstatntOccupancy(INSTANT_OCCUPANCY_MINUTE_OUTPUT, MINUTE_IN_SECOND, false);
            outputInstatntOccupancy(INSTANT_OCCUPANCY_OC_MINUTE_OUTPUT, MINUTE_IN_SECOND, true);
            outputInstatntMemoryUtilizationRatio(INSTANT_OCCUPANCY_MEMORY_MINUTE_OUTPUT, MINUTE_IN_SECOND);
            outputWaitingAndNewArrivalJobAndStartJobAndFinishedJob(WAITING_JOB_PER_MINUTE_OUTPUT, ARRIVAL_JOB_PER_MINUTE_OUTPUT, START_JOB_PER_MINUTE_OUTPUT, CUMULATIVE_STARTED_JOB_PER_MINUTE_OUTPUT, FINISHED_JOB_PER_MINUTE_OUTPUT, CUMULATIVE_FINISHED_JOB_PER_MINUTE_OUTPUT, WAITING_RESOURCES_PER_MINUTE_OUTPUT, EXECUTING_RESOURCES_PER_MINUTE_OUTPUT, ARRIVAL_RESOURCES_PER_MINUTE_OUTPUT, WAITING_MEMORY_RESOURCES_PER_MINUTE_OUTPUT, EXECUTING_MEMORY_RESOURCES_PER_MINUTE_OUTPUT, ARRIVAL_MEMORY_RESOURCES_PER_MINUTE_OUTPUT, MINUTE_IN_SECOND);
            outputWastedResource(WASTED_RESOURCE_MINUTE_OUTPUT, MINUTE_IN_SECOND);
        }
        
        return;
    }

    private Collection<? extends Integer> countSlowdown(ArrayList<Double> threshold, boolean OCFlag) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i <= threshold.size(); ++i) result.add((Integer) 0);
        
        for (int i = 0; i < completedJobList.size(); ++i) {
            double slowdown;
            if (!OCFlag) {
                slowdown = completedJobList.get(i).getSlowdown();                
            } else {
                slowdown = completedJobList.get(i).getSlowdownByOriginalRunningTime();
            }
            
            for (int j = 0; j < threshold.size(); ++j) {
                if (slowdown <= threshold.get(j)) {
                    result.set(j, result.get(j) + 1);
                    break;
                }
            }
            if (slowdown > threshold.get(threshold.size()-1)) {
                result.set(threshold.size(), result.get(threshold.size()) + 1);
            }
        }
        return result;
    }

    private void outputSlowdown(boolean OCFlag) {
        try {
            String fileName;
            if (!OCFlag) {
                fileName = SLOWDOWN_OUTPUT;
            } else {
                fileName = SLOWDOWN_OC_OUTPUT;
            }

            PrintWriter pwSlowdown;
            pwSlowdown = new PrintWriter(this.p + "/" + fileName);

            ArrayList<Integer> histgram = new ArrayList<Integer>();
            histgram.addAll(countSlowdown(thresholdForSlowdown, OCFlag));
            
            for (int i = 0; i < histgram.size() -1; ++i) {
                histgram.set(i+1, histgram.get(i) + histgram.get(i+1));
            }
            int jobnum = histgram.get(histgram.size()-1);

            for (int i = 0; i < histgram.size() - 1; ++i) {
                pwSlowdown.println("<=" + thresholdForSlowdown.get(i) + "\t" + (double)histgram.get(i)/jobnum);
            }
            pwSlowdown.println(">" + thresholdForSlowdown.get(thresholdForSlowdown.size() - 1) + "\t" + (double)histgram.get(histgram.size() - 1)/jobnum);
            pwSlowdown.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void outputFinishedAndCumulativeFinishedJob(String fileNameFinished, String fileNameCumulativeFinished, int MODE) {
        PairIntegers result = new PairIntegers();
        result = countCumlativeJob(MODE);
        ArrayList<Integer> resultFinished = result.getNumNewFinishedJobs();
        printTimeSeriesToFile(fileNameFinished, resultFinished);        
        ArrayList<Integer> resultCumulative = result.getNumCumulativeFinishedJobs();
        printTimeSeriesToFile(fileNameCumulativeFinished, resultCumulative);                
    }
    
    private PairIntegers countCumlativeJob(int THRESHOLD) {
        ArrayList<Integer> resultFinished = new ArrayList<Integer>();
        ArrayList<Integer> resultCumulative = new ArrayList<Integer>();
        PairIntegers result = new PairIntegers();
        result.setNumNewFinishedJobs(resultFinished);
        result.setNumCumulativeFinishedJobs(resultCumulative);
        int i = 0;
        int threshold = 0;
        int cnt = 0;
        for (;;) {
            int newFinished = 0;
            while (completedJobList.get(i).getFinishedTime() <= threshold) {
                ++cnt;
                ++newFinished;
                ++i;
                if (i == completedJobList.size()) break;
            }
            resultFinished.add(newFinished);
            resultCumulative.add(cnt);
            threshold +=  THRESHOLD;
            if (i == completedJobList.size()) break;
        }
        return result;
    }

    private void finalizeSimulation() {
        pw.close();
        pwForVis.close();
        if (outputUtilizationRatio) {
            pwForUtilHour.close();
            pwForUtilDay.close();
            pwForWastedResourceHour.close();
            pwForWastedResourceDay.close();
            if (outputMinuteBoolean) {
                pwForUtilMinute.close();
                pwForWastedResourceMinute.close();

            }
            if (outputSecondWastedResources) {
                pwForWastedResourceSecond.close();
            }
            try {
                flushWastedResourcesRatio();
            } catch (FileNotFoundException ex) {
                Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
            }
            pwForWastedResourceTimeseries.close();
        }
    }
    
    void flushWastedResourcesRatio() throws FileNotFoundException {               
        List<Double> wastedResources = sche.getWastedResources();
        PrintWriter pwWasted;
        String fileName = WASTED_RESOURCE_OF_SYSTEM;
        pwWasted = new PrintWriter(this.p + "/" + fileName);
        int numNode = NodeConsciousScheduler.numNodes;
        for (int i = 0; i < numNode; ++i) {
            pwWasted.print(i);
            if (i != numNode - 1) {
                pwWasted.print("\t");
            } else {
                pwWasted.print("\tSum\n");
            }
        }

        double totalWastedRatio = 0.0;
        int i = 0;
        for (Double wastedResource: wastedResources) {
            pwWasted.print(wastedResource);
            totalWastedRatio += wastedResource;
            if (i != numNode - 1) {
                pwWasted.print("\t");
            } else {
                pwWasted.print("\t");
                pwWasted.print(totalWastedRatio);
                pwWasted.print("\n");
            }
            ++i;
        }
        pwWasted.close();
    }

    void outputResultForVis(Job job, int currentTime) {
        int jobId = job.getJobId();
        int previousMigratingTime = job.getPreviousMigratingTime();
        int numCores = job.getRequiredCores();
        int numNodes = job.getRequiredNodes();
        int startFlag = previousMigratingTime == job.getStartTime() ? 1 : 0;
        int endFlag = currentTime == job.getFinishedTime() ? 1 : 0;

        pwForVis.print(jobId + "\t" + previousMigratingTime + "\t" + currentTime + "\t" + startFlag + "\t" + endFlag + "\t" + numCores + "\t" + numNodes + "\t");
        
        ArrayList<UsingNode> usingNodesList = job.getUsingNodesList();
        Collections.sort(usingNodesList);
        for (int i = 0; i < numNodes; ++i) {
            UsingNode usingNode = usingNodesList.get(i);
            int nodeNo = usingNode.getNodeNum();
            int numUsingCores = usingNode.getNumUsingCores();
            ArrayList<Integer> usingCoreNum = usingNode.getUsingCoreNum();
            
            pwForVis.print(nodeNo + "(");

            for (int j = 0; j < numUsingCores; ++j) {
                int coreNum = usingCoreNum.get(j);
                pwForVis.print(coreNum);
                if (j != numUsingCores - 1) pwForVis.print(",");
            }
            pwForVis.print(")\t");
        }
        pwForVis.println();
    }

    void outputResultForVis(Job job) {
        outputResultForVis(job, job.getFinishedTime());

    }

    private void outputOccupancy() {
        try {
            String fileName = OCCUPANCY_OUTPUT;        
            PrintWriter pwUtilizationRatio;
            pwUtilizationRatio = new PrintWriter(this.p + "/" + fileName);

            int numNodes = NodeConsciousScheduler.numNodes;
            int numCores = NodeConsciousScheduler.numCores;
            
            LinkedList<TimeSlice> tsList = this.sche.completedTimeSlices;

            double[] utilizationRatioOC = new double[numNodes];
            double[] utilizationRatio = new double[numNodes];
            for (int i = 0; i < numNodes; ++i) {
                utilizationRatioOC[i] = 0;
                utilizationRatio[i] = 0;
            }
            
            int startTime = UNUPDATED;
            int endTime = UNUPDATED;
            
            for (TimeSlice ts: tsList) {
                if (startTime == UNUPDATED) startTime = ts.getStartTime();
                endTime = max(endTime, ts.getEndTime());
                
                int duration = ts.getDuration();
                ArrayList<Integer> availableCores = ts.getAvailableCores();
                
                for (int i = 0; i < numNodes; ++i) {
                    int numRunningCores = numCores - availableCores.get(i);
                    utilizationRatioOC[i] += numRunningCores * duration;
                    utilizationRatio[i] += min(numRunningCores, numCores) * duration;                    
                }
            }
            
            int duration = endTime - startTime; 

            double totalUtilizationRatio = 0.0;
            double totalUtilizationRatioOC = 0.0;
            
            for (int i = 0; i < numNodes; ++i) {
                utilizationRatioOC[i] = utilizationRatioOC[i]/duration/numCores*100;
                utilizationRatio[i] = utilizationRatio[i]/duration/numCores*100;
                totalUtilizationRatio += utilizationRatio[i];
                totalUtilizationRatioOC += utilizationRatioOC[i];
            }
            totalUtilizationRatio /= numNodes;
            totalUtilizationRatioOC /= numNodes;
            
            
            pwUtilizationRatio.println("NodeId\tutilizationRatio\tutilizationRatioOC");
            
            for (int i = 0; i < numNodes; ++i) {
                pwUtilizationRatio.println(i + "\t" + utilizationRatio[i] + "\t" + utilizationRatioOC[i]);
            }
            
            pwUtilizationRatio.println("Total\t" + totalUtilizationRatio + "\t" + totalUtilizationRatioOC);
            pwUtilizationRatio.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    private void outputInstatntOccupancy(String fileName, int MODE, boolean OCFlag) {
        try {
            PrintWriter pwUtilizationRatio;
            pwUtilizationRatio = new PrintWriter(this.p + "/" + fileName);

            ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
            result.addAll(calcInstantUtilizationRatio(MODE, OCFlag));
            
            for (int j = 0; j < NodeConsciousScheduler.numNodes; ++j) {
                pwUtilizationRatio.print("\t" + j);
            }
            pwUtilizationRatio.println("\tAve.");
            
            
            if (result.size() != 0) {
                for (int i = 0; i < result.size() - 1; ++i) {
                    //pwUtilizationRatio.println(idxWaiting + "\t" + resultWaiting.get(idxWaiting));
                    pwUtilizationRatio.print(i);
                    ArrayList<Double> ret = result.get(i);
                    double totalUtilizationRatio = 0.0;
                    for (int j = 0; j < ret.size(); ++j) {
                        pwUtilizationRatio.print("\t" + ret.get(j));
                        totalUtilizationRatio += ret.get(j);
                    }
                    pwUtilizationRatio.println("\t" + totalUtilizationRatio/NodeConsciousScheduler.numNodes);
                }
                pwUtilizationRatio.print(result.size() - 1);

                double totalUtilizationRatio = 0.0;
                ArrayList<Double> ret = result.get(result.size() - 1);
                for (int j = 0; j < ret.size(); ++j) {
                    pwUtilizationRatio.print("\t" + ret.get(j));
                    totalUtilizationRatio += ret.get(j);
                }
                pwUtilizationRatio.println("\t" + totalUtilizationRatio / NodeConsciousScheduler.numNodes);                
            }
            pwUtilizationRatio.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
  
    private void outputWastedResource(String fileName, int MODE) {
        try {
            PrintWriter pwWastedResource;
            pwWastedResource = new PrintWriter(this.p + "/" + fileName);

            ArrayList<Double> result = new ArrayList<Double>();
            result.addAll(calcWastedResource(MODE));
            
            for (int j = 0; j < NodeConsciousScheduler.numNodes; ++j) {
                pwWastedResource.print("\t" + j);
            }
            pwWastedResource.println("\tAve.");
            
            
            double totalUtilizationRatio = 0.0;
            for (int i = 0; i < result.size() - 1; ++i) {
                Double ret = result.get(i);
                pwWastedResource.print("\t" + ret);
                totalUtilizationRatio += ret;

            }
            pwWastedResource.println("\t" + totalUtilizationRatio / NodeConsciousScheduler.numNodes);

            pwWastedResource.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }
    
    private ArrayList<ArrayList<Double>> calcInstantUtilizationRatio(int THRESHOLD, boolean OCFlag) {
        ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
        LinkedList<TimeSlice> completedTimeSlices = this.sche.completedTimeSlices;
        int i = 0;
        int threshold = THRESHOLD;
        result.add(calcUtilizationAtTs(completedTimeSlices.get(i), OCFlag));
        for (;;) {
            while (completedTimeSlices.get(i).getEndTime() <= threshold) {
                ++i;
                if (i == completedTimeSlices.size()) break;
            }
            int idx = i == completedTimeSlices.size() ? i-1:i;
            result.add(calcUtilizationAtTs(completedTimeSlices.get(idx), OCFlag));
            threshold +=  THRESHOLD;
            if (i == completedTimeSlices.size()) break;
        }
        return result;
    }
 
    private ArrayList<Double> calcWastedResource(int THRESHOLD) {
        ArrayList<Double> result = new ArrayList<Double>();
        result.addAll(calcWastedResourcePerNodeFromTs());
        return result;
    }
    
    private ArrayList<Double> calcUtilizationAtTs(TimeSlice ts, boolean OCFlag) {
        ArrayList<Double> result = new ArrayList<Double>(); 
        ArrayList<Integer> availableCores = ts.getAvailableCores();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            int numRuuningCore = OCFlag ? NodeConsciousScheduler.numCores - availableCores.get(i) : min(NodeConsciousScheduler.numCores - availableCores.get(i), NodeConsciousScheduler.numCores);
            result.add((double)numRuuningCore/NodeConsciousScheduler.numCores*100);
        }
        return result;
    }

    private ArrayList<Double> calcWastedResourcePerNodeFromTs() {
        LinkedList<TimeSlice> completedTimeSlices = this.sche.completedTimeSlices;
        int numNode = NodeConsciousScheduler.numNodes;
        int numCore = NodeConsciousScheduler.numCores;

        ArrayList<Double> result = new ArrayList<Double>();
        for (int i = 0; i < numNode; ++i) {
            double allocateTime = 0.0;
            result.add(allocateTime);
        }

        int timeSliceSize = completedTimeSlices.size();
        for (TimeSlice ts: completedTimeSlices) {
            ArrayList<Integer> availableCores = ts.getAvailableCores();
            int duration = ts.getDuration();
            for (int i = 0; i < numNode; ++i) {
                int numRuuningCore = min(numCore - availableCores.get(i), numCore);
                double allocateTime = result.get(i);
                allocateTime += numRuuningCore * duration;
                result.set(i, allocateTime);
            }
        }
        return result;
    }
    
    private void outputWaitingAndNewArrivalJobAndStartJobAndFinishedJob(String fileNameWaiting, String fileNameArrival, String fileNameStart, String fileNameCumulativeStart, String fileNameFinished, String fileNameCumulativeFinished, String fileNameWaitingRscs, String fileNameExecutingRscs, String fileNameArrivalRscs, String fileNameWaitingMemRscs, String fileNameExecutingMemRscs, String fileNameArrivalMemRscs, int MODE) {
            PairIntegers pairResult = new PairIntegers();
            pairResult = calcWaitingAndNewArrivalJobAndStartJob(MODE);

            ArrayList<Integer> resultWaiting = pairResult.getNumWaitingJobs();
            printTimeSeriesToFile(fileNameWaiting, resultWaiting);

            ArrayList<Integer> resultArrival = pairResult.getNumNewArrivalJobs();
            printTimeSeriesToFile(fileNameArrival, resultArrival);

            ArrayList<Integer> resultStart = pairResult.getNumNewStartJobs();
            printTimeSeriesToFile(fileNameStart, resultStart);

            ArrayList<Integer> resultCumulativeStart = pairResult.getNumCumulativeStartJobs();
            printTimeSeriesToFile(fileNameCumulativeStart, resultCumulativeStart);

            ArrayList<Integer> resultFinished = pairResult.getNumNewFinishedJobs();
            printTimeSeriesToFile(fileNameFinished, resultFinished);
            
            ArrayList<Integer> resultCumulativeFinished = pairResult.getNumCumulativeFinishedJobs();
            printTimeSeriesToFile(fileNameCumulativeFinished, resultCumulativeFinished);

            ArrayList<Long> resultCumulativeWaitingRscs = pairResult.getNumCumulativeWaitingJobResources();
            ArrayList<Long> resultCumulativeStartedRscs = pairResult.getNumCumulativeStartedJobResources();
            ArrayList<Long> resultCumulativeFinishedRscs = pairResult.getNumCumulativeFinishedJobResources();
            ArrayList<Long> resultCumulativeArrivalRscs = pairResult.getNumCumulativeArrivalJobResources();
            ArrayList<Long> resultCumulativeArrivalRscsShifted = (ArrayList<Long>) resultCumulativeArrivalRscs.clone();
            resultCumulativeArrivalRscsShifted.add(0, (long) 0);
            resultCumulativeArrivalRscsShifted.remove(resultCumulativeArrivalRscs.size()-1);
            assert resultCumulativeArrivalRscs.size() == resultCumulativeArrivalRscsShifted.size();
            printTimeSeriesToFile(fileNameWaitingRscs, resultCumulativeWaitingRscs, resultCumulativeStartedRscs, NodeConsciousScheduler.numNodes*NodeConsciousScheduler.numCores);
            printTimeSeriesToFile(fileNameExecutingRscs, resultCumulativeStartedRscs, resultCumulativeFinishedRscs, NodeConsciousScheduler.numNodes*NodeConsciousScheduler.numCores);
            printTimeSeriesToFile(fileNameArrivalRscs, resultCumulativeArrivalRscs, resultCumulativeArrivalRscsShifted, NodeConsciousScheduler.numNodes*NodeConsciousScheduler.numCores);
            

            if (scheduleUsingMemory) {
                ArrayList<Long> resultCumulativeWaitingMemoryRscs = pairResult.getNumCumulativeWaitingJobMemoryResources();
                ArrayList<Long> resultCumulativeStartedMemoryRscs = pairResult.getNumCumulativeStartedJobMemoryResources();
                ArrayList<Long> resultCumulativeFinishedMemoryRscs = pairResult.getNumCumulativeFinishedJobMemoryResources();
                ArrayList<Long> resultCumulativeArrivalMemoryRscs = pairResult.getNumCumulativeArrivalJobMemoryResources();
                ArrayList<Long> resultCumulativeArrivalMemoryRscsShifted = (ArrayList<Long>) resultCumulativeArrivalMemoryRscs.clone();
                resultCumulativeArrivalMemoryRscsShifted.add(0, (long) 0);
                resultCumulativeArrivalMemoryRscsShifted.remove(resultCumulativeArrivalMemoryRscs.size()-1);
                assert resultCumulativeArrivalMemoryRscs.size() == resultCumulativeArrivalMemoryRscsShifted.size();
                printTimeSeriesToFile(fileNameWaitingMemRscs, resultCumulativeWaitingMemoryRscs, resultCumulativeStartedMemoryRscs, NodeConsciousScheduler.numNodes*NodeConsciousScheduler.memory);
                printTimeSeriesToFile(fileNameExecutingMemRscs, resultCumulativeStartedMemoryRscs, resultCumulativeFinishedMemoryRscs, NodeConsciousScheduler.numNodes*NodeConsciousScheduler.memory);
                printTimeSeriesToFile(fileNameArrivalMemRscs, resultCumulativeArrivalMemoryRscs, resultCumulativeArrivalMemoryRscsShifted, NodeConsciousScheduler.numNodes*NodeConsciousScheduler.memory);
            }
            
    }

    private PairIntegers calcWaitingAndNewArrivalJobAndStartJob(int THRESHOLD) {
        ArrayList<Integer> resultWaiting = new ArrayList<Integer>();
        ArrayList<Integer> resultArrival = new ArrayList<Integer>();
        ArrayList<Integer> resultStart = new ArrayList<Integer>();
        ArrayList<Integer> resultCumulativeStart = new ArrayList<Integer>();
        ArrayList<Integer> resultFinished = new ArrayList<Integer>();
        ArrayList<Integer> resultCumulativeFinished = new ArrayList<Integer>();
        
        
        ArrayList<Long> resultCumulativeStartRscs = new ArrayList<Long>();
        ArrayList<Long> resultCumulativeWaitingRscs = new ArrayList<Long>();
        ArrayList<Long> resultCumulativeFinishedRscs = new ArrayList<Long>();
        ArrayList<Long> resultCumulativeArrivalRscs = new ArrayList<Long>();

        ArrayList<Long> resultCumulativeStartMemoryRscs = new ArrayList<Long>();
        ArrayList<Long> resultCumulativeWaitingMemoryRscs = new ArrayList<Long>();
        ArrayList<Long> resultCumulativeFinishedMemoryRscs = new ArrayList<Long>();
        ArrayList<Long> resultCumulativeArrivalMemoryRscs = new ArrayList<Long>();
        
        PairIntegers resultPair = new PairIntegers();
        resultPair.setNumNewArrivalJobs(resultArrival);
        resultPair.setNumWaitingJobs(resultWaiting);
        resultPair.setNumNewStartJobs(resultStart);
        resultPair.setNumCumulativeStartJobs(resultCumulativeStart);
        resultPair.setNumNewFinishedJobs(resultFinished);
        resultPair.setNumCumulativeFinishedJobs(resultCumulativeFinished);       
        resultPair.setNumCumulativeStartedJobResources(resultCumulativeStartRscs);
        resultPair.setNumCumulativeWaitingJobResources(resultCumulativeWaitingRscs);
        resultPair.setNumCumulativeFinishedJobResources(resultCumulativeFinishedRscs);
        resultPair.setNumCumulativeArrivalJobResources(resultCumulativeArrivalRscs);
        resultPair.setNumCumulativeStartedJobMemoryResources(resultCumulativeStartMemoryRscs);
        resultPair.setNumCumulativeWaitingJobMemoryResources(resultCumulativeWaitingMemoryRscs);
        resultPair.setNumCumulativeFinishedJobMemoryResources(resultCumulativeFinishedMemoryRscs);;
        resultPair.setNumCumulativeArrivalJobMemoryResources(resultCumulativeArrivalMemoryRscs);
            
        ArrayList<Job> completedJobListStartOrder = new ArrayList<>(this.completedJobList);
        Collections.sort(completedJobListStartOrder);
        ArrayList<Job> completedJobListSubmitOrder = new ArrayList<>(this.completedJobList);
        Collections.sort(completedJobListSubmitOrder, new Comparator<Job>() {
            @Override
            public int compare(Job job1, Job job2) {
                return job1.getSubmitTime() - job2.getSubmitTime();
            }
        });
        ArrayList<Job> completedJobListFinishedOrder = new ArrayList<>(this.completedJobList);
        
        int idxWaiting = 0;
        int idxStart = 0;
        int idxFinished = 0;
        int threshold = 0;
        int numWaitingJob = 0;
        int numNewArrivalJobPerTHRESHOLD = 0;
        int numCumulativeStartedJobs = 0;
        int numCumulativeFinishedJobs = 0;
        long startedJobRscs = 0;
        long waitingJobRscs = 0;
        long finishedJobRscs = 0;
        long arrivalJobRscs = 0;
        long previousArrivalTime = 0;
        long startedJobMemoryRscs = 0;
        long waitingJobMemoryRscs = 0;
        long finishedJobMemoryRscs = 0;
        long arrivalJobMemoryRscs = 0;
        

        for (;;) {
            numNewArrivalJobPerTHRESHOLD = 0;            
            while (idxWaiting != completedJobListSubmitOrder.size() && completedJobListSubmitOrder.get(idxWaiting).getSubmitTime()<= threshold) {
                Job job = completedJobListSubmitOrder.get(idxWaiting);
                ++numNewArrivalJobPerTHRESHOLD;
                ++numWaitingJob;
                ++idxWaiting;
                assert previousArrivalTime <= job.getSubmitTime();
                previousArrivalTime = job.getSubmitTime();
                waitingJobRscs += job.getRequiredCoresPerNode() * job.getRequiredNodes();
                waitingJobMemoryRscs += job.getMaxMemory() * job.getRequiredNodes();
                arrivalJobMemoryRscs += job.getMaxMemory() * job.getRequiredNodes();
                arrivalJobRscs += job.getRequiredCoresPerNode() * job.getRequiredNodes();
            }
            
            int numStartedJob = 0;
            while (idxStart != completedJobListStartOrder.size() && completedJobListStartOrder.get(idxStart).getStartTime()<= threshold) {
                Job job = completedJobListStartOrder.get(idxStart);
                ++numStartedJob;
                ++idxStart;
                startedJobRscs += job.getRequiredCoresPerNode() * job.getRequiredNodes();
                startedJobMemoryRscs += job.getMaxMemory() * job.getRequiredNodes();
            }

            int numFinishedJob = 0;
            while (completedJobListFinishedOrder.get(idxFinished).getFinishedTime()<= threshold) {
                Job job = completedJobListFinishedOrder.get(idxFinished);
                ++numFinishedJob;
                ++idxFinished;
                finishedJobRscs += job.getRequiredCoresPerNode() * job.getRequiredNodes();
                finishedJobMemoryRscs += job.getMaxMemory() * job.getRequiredNodes();
                if (idxFinished == completedJobListStartOrder.size()) break;
            }
            
            numWaitingJob -= numStartedJob;
            numCumulativeStartedJobs += numStartedJob;
            numCumulativeFinishedJobs += numFinishedJob;
            
            resultWaiting.add(numWaitingJob);
            resultArrival.add(numNewArrivalJobPerTHRESHOLD);
            resultStart.add(numStartedJob);
            resultFinished.add(numFinishedJob);
            resultCumulativeStart.add(numCumulativeStartedJobs);
            resultCumulativeFinished.add(numCumulativeFinishedJobs);
            resultCumulativeWaitingRscs.add(waitingJobRscs);
            resultCumulativeStartRscs.add(startedJobRscs);
            resultCumulativeFinishedRscs.add(finishedJobRscs);   
            resultCumulativeArrivalRscs.add(arrivalJobRscs);         
            resultCumulativeWaitingMemoryRscs.add(waitingJobMemoryRscs);
            resultCumulativeStartMemoryRscs.add(startedJobMemoryRscs);
            resultCumulativeFinishedMemoryRscs.add(finishedJobMemoryRscs);            
            resultCumulativeArrivalMemoryRscs.add(arrivalJobMemoryRscs);
            
            threshold +=  THRESHOLD;

            if (idxFinished == completedJobListFinishedOrder.size()) break;
        }
        return resultPair;
    }

    private <T> void printTimeSeriesToFile(String fileName, ArrayList<T> resultTimeSeries) {
        try {
            PrintWriter pw;
            pw = new PrintWriter(this.p + "/" + fileName);
            
            if (resultTimeSeries.size() != 0) {
                for (int i = 0; i < resultTimeSeries.size(); ++i) {
                    pw.println(i + "\t" + resultTimeSeries.get(i));
                }
            }
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void outputResultEachUserAndGroup() {
        
        /* separete the list by each user(group) */
        Map<Integer, Boolean> foundUserId = new HashMap<Integer, Boolean>();
        Map<Integer, Boolean> foundGroupId = new HashMap<Integer, Boolean>();
        Map<Integer, ArrayList<Job>> jobByUser = new HashMap<Integer, ArrayList<Job>>();
        Map<Integer, ArrayList<Job>> jobByGroup = new HashMap<Integer, ArrayList<Job>>();
        for (Job job: completedJobList) {
            int userId = job.getUserId();
            int groupId = job.getGroupId();
            
            if (!foundUserId.containsKey(userId)) {
                foundUserId.put(userId, Boolean.TRUE);
                jobByUser.put(userId, new ArrayList<Job>());
            }
            jobByUser.get(userId).add(job);
            
            if (!foundGroupId.containsKey(groupId)) {
                foundGroupId.put(groupId, Boolean.TRUE); 
                jobByGroup.put(groupId, new ArrayList<Job>());
            }
            jobByGroup.get(groupId).add(job);            
        }

        assert foundUserId.size() == jobByUser.size();
        assert foundGroupId.size() == jobByGroup.size();

        ArrayList<UserResult> resultEachUser = new ArrayList<UserResult>();            
        ArrayList<GroupResult> resultEachGroup = new ArrayList<GroupResult>();
        
        calculateUserResult(jobByUser, resultEachUser);  
        printByUser(resultEachUser);
        
        calculateGroupResult(jobByGroup, resultEachGroup);
        printByGroup(resultEachGroup);
    }

    // TODO: Later decide conditions and implement
    private boolean checkLargeJob(int runningTime, int numNode, int numCorePerNode, int cpuTime) {
        return false;
    }

    private void printByUser(ArrayList<UserResult> resultEachUser) {
        String fileName = RESULT_EACH_USER;
        try {
            PrintWriter pw;
            pw = new PrintWriter(this.p + "/" + fileName);
            
            pw.print("userId\tnumJob\trunningTime\trunningTime(ave.)\trunningTime(max)\twaitTime\twaitTime(ave.)\twaitTime(max)\tturnAroundTime(max)\tnumNode\tnumNode(ave.)\tnumCore\tnumCore(ave.)\t"
                     + "CpuTime\tCpuTime(ave.)\tCpuTime(max)\tMemoryFootprint\tMemoryFootprint(ave)\tlargeJobRatio\tslowdown(max)\tslowdownOC(max)\t");

            int sdSize = thresholdForSlowdown.size();
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<=" + thresholdForSlowdown.get(i) + "\t");                
            }
            pw.print(">" + thresholdForSlowdown.get(sdSize-1) + "\t");
            
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<=" + thresholdForSlowdown.get(i) + "(OC)\t");                
            }
            pw.println(">" + thresholdForSlowdown.get(sdSize-1) + "(OC)");
            
            for (int i = 0; i < resultEachUser.size() ; ++i) {
                UserResult result = resultEachUser.get(i);
                
                int userId = result.getUserId();
                int numJob = result.getNumJobs();
                int runningTime = result.getAccumulatedTime();
                int waitTime = result.getAccumulatedWaitTime();
                int numNode = result.getAccumulatedNumNode();
                int numCore = result.getAccumulatedNumCore();
                long cpuTime = result.getAccumulatedCpuTime();
                long memoryFootprint = result.getAccumulatedMemoryFootprint();
                int cntSpecifiedMaxMemory = result.getNumJobsSetMemory();
                double averagedNumNode = result.getAccumulatedNumNode();
                double averagedNumCore = result.getAccumulatedNumCore();
                double averagedRunningTime = result.getAveragedTime();
                double averagedCpuTime = result.getAveragedCpuTime();
                double averagedMemoryFootprint = result.getAveragedMemoryFootprint();
                double averagedWaitTime = result.getAveragedWaitTime();
                double largeJobRatio = result.getLargeJobRatio();
                int maxRunningTime = result.getMaxRunningTime();
                int maxWaitTime = result.getMaxWaitTime();
                int maxCpuTime = result.getMaxCpuTime();
                int maxTurnAroundTime = result.getMaxTurnAroundTime();
                double maxSlowdown = result.getMaxSlowdown();
                double maxSlowdownOC = result.getMaxSlowdownOC();
                
                pw.print(userId + "\t" + numJob + "\t" + runningTime + "\t" + averagedRunningTime + "\t" + maxRunningTime + "\t" + waitTime + "\t" + averagedWaitTime + "\t" + maxWaitTime + "\t" + maxTurnAroundTime + "\t" +
                           numNode + "\t" + averagedNumNode + "\t" + numCore + "\t" + averagedNumCore + "\t" + cpuTime + "\t" + averagedCpuTime + "\t" + maxCpuTime + "\t" + 
                           memoryFootprint + "\t" + averagedMemoryFootprint + "\t" + largeJobRatio + "\t" + maxSlowdown + "\t" + maxSlowdownOC + "\t");
                
                ArrayList<Integer> slowdowns = result.getSlowdowns();
                int size = slowdowns.size();
                for (int j = 0; j < size - 1; ++j) {
                    slowdowns.set(j+1, slowdowns.get(j) + slowdowns.get(j+1));
                }
                for (int j = 0; j < size - 1; ++j) {
                    pw.print((double)slowdowns.get(j)/numJob + "\t");
                }
                pw.print((double)slowdowns.get(size - 1)/numJob + "\t");                

                ArrayList<Integer> slowdownsOC = result.getSlowdownsOC();
                int sizeOC = slowdownsOC.size();                
                for (int j = 0; j < sizeOC - 1; ++j) {
                    slowdownsOC.set(j+1, slowdownsOC.get(j) + slowdownsOC.get(j+1));
                }                
                for (int j = 0; j < sizeOC - 1; ++j) {
                    pw.print((double)slowdownsOC.get(j)/numJob + "\t");
                }
                pw.println((double)slowdownsOC.get(sizeOC - 1)/numJob);                            
            }            
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void calculateUserResult(Map<Integer, ArrayList<Job>> jobIdEachUser, ArrayList<UserResult> resultEach) {
        for (Integer userId: jobIdEachUser.keySet()) {
            ArrayList<Job> jobList = jobIdEachUser.get(userId);
            // each collumn value prepared here
            int numJob = jobList.size();
            int groupId = jobList.get(0).getGroupId();
            int accumulatedRunningTime = 0;
            int accumulatedWaitTime = 0;
            int accumulatedNumNode = 0;
            int accumulatedNumCore = 0;
            long accumulatedCpuTime = 0;
            long accumulatedMaxMemory = 0;
            int cntSpecifiedMaxMemory = 0;
            int cntLargeJob = 0;
            int maxRunningTime = 0;
            int maxWaitTime = 0;
            int maxCpuTime = 0;                    
            int maxTurnAroundTime = 0;
            double maxSlowdown = 0.0;
            double maxSlowdownOC = 0.0;
            
            ArrayList<Integer> slowdownHistgramEachUser = new ArrayList<Integer>();
            ArrayList<Integer> slowdownOCHistgramEachUser = new ArrayList<Integer>();            
            for (int i = 0; i <= thresholdForSlowdown.size(); ++i) {
                slowdownHistgramEachUser.add(0);
                slowdownOCHistgramEachUser.add(0);          
            }
                        
            for (Job job: jobList) {
                assert (int)userId == job.getUserId();
                assert (int)groupId == job.getGroupId();

                int runningTime = job.getRunningTimeDed() + job.getRunningTimeOC();
                int waitTime = job.getWaitTime();
                int numNode = job.getRequiredNodes();
                int numCorePerNode = job.getRequiredCoresPerNode();
                int cpuTime = numNode * numCorePerNode * runningTime;
                int turnAroundTime = runningTime + waitTime;
                
                accumulatedRunningTime += runningTime;
                accumulatedWaitTime += waitTime;
                accumulatedNumNode += numNode;
                accumulatedNumCore += numCorePerNode;
                accumulatedCpuTime += cpuTime;

                maxRunningTime = max(maxRunningTime, runningTime);
                maxWaitTime = max(maxWaitTime, waitTime);
                maxCpuTime = max(maxCpuTime, cpuTime);
                maxTurnAroundTime = max(maxTurnAroundTime, turnAroundTime);
                
                if (checkLargeJob(runningTime, numNode, numCorePerNode, cpuTime)) {
                    ++cntLargeJob;
                }
                
                long maxMemory = job.getMaxMemory();
                if (maxMemory > 0) {
                    accumulatedMaxMemory += maxMemory;
                    ++cntSpecifiedMaxMemory;
                }
                
                double slowdown = job.getSlowdown();
                boolean addedFlag = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdown <= thresholdForSlowdown.get(i)) {
                        slowdownHistgramEachUser.set((Integer) i, slowdownHistgramEachUser.get(i) + 1);
                        addedFlag = true;
                        break;
                    }                    
                }
                if (!addedFlag) {
                    int lastIndex = slowdownHistgramEachUser.size()-1;
                    slowdownHistgramEachUser.set((Integer) lastIndex, slowdownHistgramEachUser.get(lastIndex) + 1);
                }

                double slowdownOC = job.getSlowdownByOriginalRunningTime();
                boolean addedFlagOC = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdownOC <= thresholdForSlowdown.get(i)) {
                        slowdownOCHistgramEachUser.set((Integer) i, slowdownOCHistgramEachUser.get(i) + 1);
                        addedFlagOC = true;
                        break;
                    }                    
                }
                if (!addedFlagOC) {
                    int lastIndex = slowdownOCHistgramEachUser.size()-1;
                    slowdownOCHistgramEachUser.set((Integer) lastIndex, slowdownOCHistgramEachUser.get(lastIndex) + 1);
                }
                maxSlowdown = max(maxSlowdown, slowdown);                
                maxSlowdownOC = max(maxSlowdownOC, slowdownOC);
            }
            double averagedNumNode = (double)accumulatedNumNode/numJob;
            double averagedNumCore = (double)accumulatedNumCore/numJob;
            double averagedRunningTime = (double)accumulatedRunningTime/numJob;
            double averagedCpuTime = (double)accumulatedCpuTime/numJob;
            double averagedMemoryFootprint = (double)accumulatedMaxMemory/cntSpecifiedMaxMemory;
            double averagedWaitTime = (double)accumulatedWaitTime/numJob;
            double largeJobRatio = (double)cntLargeJob/numJob;
            
            
            resultEach.add(new UserResult(userId, numJob, accumulatedNumNode, averagedNumNode, accumulatedNumCore, averagedNumCore, 
                                              accumulatedRunningTime, averagedRunningTime, accumulatedCpuTime, averagedCpuTime, 
                                              accumulatedMaxMemory, cntSpecifiedMaxMemory, averagedMemoryFootprint, 
                                              accumulatedWaitTime, averagedWaitTime, largeJobRatio, slowdownHistgramEachUser, slowdownOCHistgramEachUser, maxRunningTime, maxWaitTime, maxCpuTime, maxTurnAroundTime, maxSlowdown, maxSlowdownOC));
        }        
    }

    private void calculateGroupResult(Map<Integer, ArrayList<Job>> jobIdEachGroup, ArrayList<GroupResult> resultEach) {
        for (Integer groupId: jobIdEachGroup.keySet()) {
            ArrayList<Job> jobList = jobIdEachGroup.get(groupId);
            // each collumn value prepared here
            int numJob = jobList.size();
            int accumulatedRunningTime = 0;
            int accumulatedWaitTime = 0;
            int accumulatedNumNode = 0;
            int accumulatedNumCore = 0;
            int accumulatedCpuTime = 0;
            long accumulatedMaxMemory = 0;
            int cntSpecifiedMaxMemory = 0;
            int cntLargeJob = 0;
            int maxRunningTime = 0;
            int maxWaitTime = 0;
            int maxCpuTime = 0;
            int maxTurnAroundTime = 0;            
            double maxSlowdown = 0.0;
            double maxSlowdownOC = 0.0;            
            
            ArrayList<Integer> slowdownHistgramEachUser = new ArrayList<Integer>();
            ArrayList<Integer> slowdownOCHistgramEachUser = new ArrayList<Integer>();            
            for (int i = 0; i <= thresholdForSlowdown.size(); ++i) {
                slowdownHistgramEachUser.add(0);
                slowdownOCHistgramEachUser.add(0);
            }
                        
            for (Job job: jobList) {
                assert (int)groupId == job.getGroupId();

                int runningTime = job.getRunningTimeDed() + job.getRunningTimeOC();
                int waitTime = job.getWaitTime();
                int numNode = job.getRequiredNodes();
                int numCorePerNode = job.getRequiredCoresPerNode();
                int cpuTime = numNode * numCorePerNode * runningTime;
                int turnAroundTime = runningTime + waitTime;
                
                accumulatedRunningTime += runningTime;
                accumulatedWaitTime += waitTime;
                accumulatedNumNode += numNode;
                accumulatedNumCore += numCorePerNode;
                accumulatedCpuTime += cpuTime;

                maxRunningTime = max(maxRunningTime, runningTime);
                maxWaitTime = max(maxWaitTime, waitTime);
                maxCpuTime = max(maxCpuTime, cpuTime);
                maxTurnAroundTime = max(maxTurnAroundTime, turnAroundTime);
                
                if (checkLargeJob(runningTime, numNode, numCorePerNode, cpuTime)) {
                    ++cntLargeJob;
                }
                
                long maxMemory = job.getMaxMemory();
                if (maxMemory > 0) {
                    accumulatedMaxMemory += maxMemory;
                    ++cntSpecifiedMaxMemory;
                }
                
                double slowdown = job.getSlowdown();
                boolean addedFlag = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdown <= thresholdForSlowdown.get(i)) {
                        slowdownHistgramEachUser.set((Integer) i, slowdownHistgramEachUser.get(i) + 1);
                        addedFlag = true;
                        break;
                    }                    
                }
                if (!addedFlag) {
                    int lastIndex = slowdownHistgramEachUser.size()-1;
                    slowdownHistgramEachUser.set((Integer) lastIndex, slowdownHistgramEachUser.get(lastIndex) + 1);
                }

                double slowdownOC = job.getSlowdownByOriginalRunningTime();
                boolean addedFlagOC = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdownOC <= thresholdForSlowdown.get(i)) {
                        slowdownOCHistgramEachUser.set((Integer) i, slowdownOCHistgramEachUser.get(i) + 1);
                        addedFlagOC = true;
                        break;
                    }                    
                }
                if (!addedFlagOC) {
                    int lastIndex = slowdownOCHistgramEachUser.size()-1;
                    slowdownOCHistgramEachUser.set((Integer) lastIndex, slowdownOCHistgramEachUser.get(lastIndex) + 1);
                }                
                maxSlowdown = max(maxSlowdown, slowdown);                
                maxSlowdownOC = max(maxSlowdownOC, slowdownOC);                
            }
            double averagedNumNode = (double)accumulatedNumNode/numJob;
            double averagedNumCore = (double)accumulatedNumCore/numJob;
            double averagedRunningTime = (double)accumulatedRunningTime/numJob;
            double averagedCpuTime = (double)accumulatedCpuTime/numJob;
            double averagedMemoryFootprint = (double)accumulatedMaxMemory/cntSpecifiedMaxMemory;
            double averagedWaitTime = (double)accumulatedWaitTime/numJob;
            double largeJobRatio = (double)cntLargeJob/numJob;
            
            
            resultEach.add(new GroupResult(groupId, numJob, accumulatedNumNode, averagedNumNode, accumulatedNumCore, averagedNumCore, 
                                              accumulatedRunningTime, averagedRunningTime, accumulatedCpuTime, averagedCpuTime, 
                                              accumulatedMaxMemory, cntSpecifiedMaxMemory, averagedMemoryFootprint, 
                                              accumulatedWaitTime, averagedWaitTime, largeJobRatio, slowdownHistgramEachUser, slowdownOCHistgramEachUser, maxRunningTime, maxWaitTime, maxCpuTime, maxTurnAroundTime, maxSlowdown, maxSlowdownOC));           
        }
        
    }    

    private void printByGroup(ArrayList<GroupResult> resultEachGroup) {
        String fileName = RESULT_EACH_GROUP;
        try {
            PrintWriter pw;
            pw = new PrintWriter(this.p + "/" + fileName);
            
            pw.print("groupId\tnumJob\trunningTime\trunningTime(ave.)\trunningTime(max)\twaitTime\twaitTime(ave.)\twaitTime(max)\tturnAroundTime(max)\tnumNode\tnumNode(ave.)\tnumCore\tnumCore(ave.)\t"
                     + "CpuTime\tCpuTime(ave.)\tCpuTime(max)\tMemoryFootprint\tMemoryFootprint(ave)\tlargeJobRatio\tslowdown(max)\tslowdownOC(max)\t");

            int sdSize = thresholdForSlowdown.size();
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<=" + thresholdForSlowdown.get(i) + "\t");                
            }
            pw.print(">" + thresholdForSlowdown.get(sdSize-1) + "\t");
            
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<=" + thresholdForSlowdown.get(i) + "(OC)\t");                
            }
            pw.println(">" + thresholdForSlowdown.get(sdSize-1) + "(OC)");            
            
            
            for (int i = 0; i < resultEachGroup.size() ; ++i) {
                GroupResult result = resultEachGroup.get(i);
                
                int groupId = result.getGroupId();
                int numJob = result.getNumJobs();
                int runningTime = result.getAccumulatedTime();
                int waitTime = result.getAccumulatedWaitTime();
                int numNode = result.getAccumulatedNumNode();
                int numCore = result.getAccumulatedNumCore();
                long cpuTime = result.getAccumulatedCpuTime();
                long memoryFootprint = result.getAccumulatedMemoryFootprint();
                int cntSpecifiedMaxMemory = result.getNumJobsSetMemory();
                double averagedNumNode = result.getAccumulatedNumNode();
                double averagedNumCore = result.getAccumulatedNumCore();
                double averagedRunningTime = result.getAveragedTime();
                double averagedCpuTime = result.getAveragedCpuTime();
                double averagedMemoryFootprint = result.getAveragedMemoryFootprint();
                double averagedWaitTime = result.getAveragedWaitTime();
                double largeJobRatio = result.getLargeJobRatio();
                int maxRunningTime = result.getMaxRunningTime();
                int maxWaitTime = result.getMaxWaitTime();
                int maxCpuTime = result.getMaxCpuTime();
                int maxTurnAroundTime = result.getMaxTurnAroundTime();
                double maxSlowdown = result.getMaxSlowdown();
                double maxSlowdownOC = result.getMaxSlowdownOC();

                pw.print(groupId + "\t" + numJob + "\t" + runningTime + "\t" + averagedRunningTime + "\t" + maxRunningTime + "\t" + waitTime + "\t" + averagedWaitTime + "\t" + maxWaitTime + "\t" + maxTurnAroundTime + "\t" +
                           numNode + "\t" + averagedNumNode + "\t" + numCore + "\t" + averagedNumCore + "\t" + cpuTime + "\t" + averagedCpuTime + "\t" + maxCpuTime + "\t" + 
                           memoryFootprint + "\t" + averagedMemoryFootprint + "\t" + largeJobRatio + "\t" + maxSlowdown + "\t" + maxSlowdownOC + "\t");

                ArrayList<Integer> slowdowns = result.getSlowdowns();
                int size = slowdowns.size();                
                for (int j = 0; j < size - 1; ++j) {
                    slowdowns.set(j+1, slowdowns.get(j) + slowdowns.get(j+1));
                }
                for (int j = 0; j < size - 1; ++j) {
                    pw.print((double)slowdowns.get(j)/numJob + "\t");
                }
                pw.print((double)slowdowns.get(size - 1)/numJob + "\t");                

                ArrayList<Integer> slowdownsOC = result.getSlowdownsOC();
                int sizeOC = slowdownsOC.size();                
                for (int j = 0; j < sizeOC - 1; ++j) {
                    slowdownsOC.set(j+1, slowdownsOC.get(j) + slowdownsOC.get(j+1));
                }
                for (int j = 0; j < sizeOC - 1; ++j) {
                    pw.print((double)slowdownsOC.get(j)/numJob + "\t");
                }
                pw.println((double)slowdownsOC.get(sizeOC - 1)/numJob);                            
            }
            
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    

    private void printTimeSeriesToFile(String fileNameRequireRscs, ArrayList<Long> resultRscs1, ArrayList<Long> resultRscs2, long systemRscs) {
        ArrayList<Double> resultReduced = new ArrayList<Double>();
        for (int i = 0; i < resultRscs1.size(); ++i) {
            Long value1 = resultRscs1.get(i);
            Long value2  = resultRscs2.get(i);
            long value = value1 - value2;
            double vadd = (double)value/systemRscs;
            resultReduced.add(vadd);
        }
        printTimeSeriesToFile(fileNameRequireRscs, resultReduced);
    }

    public boolean isScheduleUsingMemory() {
        return scheduleUsingMemory;
    }

    public boolean isCrammingMemoryScheduling() {
        return crammingMemoryScheduling;
    }
    
    
    private void outputInstatntMemoryUtilizationRatio(String fileName, int MODE) {
        try {
            PrintWriter pwUtilizationRatio;
            pwUtilizationRatio = new PrintWriter(this.p + "/" + fileName);

            ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
            result.addAll(calcInstantMemoryUtilizationRatio(MODE));
            
            for (int j = 0; j < NodeConsciousScheduler.numNodes; ++j) {
                pwUtilizationRatio.print("\t" + j);
            }
            pwUtilizationRatio.println("\tAve.");
            
            
            if (result.size() != 0) {
                for (int i = 0; i < result.size() - 1; ++i) {
                    //pwUtilizationRatio.println(idxWaiting + "\t" + resultWaiting.get(idxWaiting));
                    pwUtilizationRatio.print(i);
                    ArrayList<Double> ret = result.get(i);
                    double totalUtilizationRatio = 0.0;
                    for (int j = 0; j < ret.size(); ++j) {
                        pwUtilizationRatio.print("\t" + ret.get(j));
                        totalUtilizationRatio += ret.get(j);
                    }
                    pwUtilizationRatio.println("\t" + totalUtilizationRatio/NodeConsciousScheduler.numNodes);
                }
                pwUtilizationRatio.print(result.size() - 1);

                double totalUtilizationRatio = 0.0;
                ArrayList<Double> ret = result.get(result.size() - 1);
                for (int j = 0; j < ret.size(); ++j) {
                    pwUtilizationRatio.print("\t" + ret.get(j));
                    totalUtilizationRatio += ret.get(j);
                }
                pwUtilizationRatio.println("\t" + totalUtilizationRatio / NodeConsciousScheduler.numNodes);                
            }
            pwUtilizationRatio.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }        

    }

    private Collection<? extends ArrayList<Double>> calcInstantMemoryUtilizationRatio(int THRESHOLD) {
        ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>();
        LinkedList<TimeSlice> completedTimeSlices = this.sche.completedTimeSlices;
        int i = 0;
        int threshold = THRESHOLD;
        result.add(calcMemoryUtilizationAtTs(completedTimeSlices.get(i)));
        for (;;) {
            while (completedTimeSlices.get(i).getEndTime() <= threshold) {
                ++i;
                if (i == completedTimeSlices.size()) break;
            }
            int idx = i == completedTimeSlices.size() ? i-1:i;
            result.add(calcMemoryUtilizationAtTs(completedTimeSlices.get(idx)));
            threshold +=  THRESHOLD;
            if (i == completedTimeSlices.size()) break;
        }
        return result;        
    }

    private ArrayList<Double> calcMemoryUtilizationAtTs(TimeSlice ts) {
        ArrayList<Double> result = new ArrayList<Double>(); 
        ArrayList<Long> availableMemory = ts.getAvailableMemory();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            long memory = NodeConsciousScheduler.memory - ts.getAvailableMemory().get(i);
            result.add((double)memory/NodeConsciousScheduler.memory*100);
        }
        return result;
    }
    
    public boolean isConsiderJobMatching() {
        return considerJobMatching;
    }

    public Map<JobMatching, Double> getJobMatchingTable() {
        return jobMatchingTable;
    }

    public Map<Integer, Job> getJobMap() {
        return jobMap;
    }

    public boolean isUsingAffinityForSchedule() {
        return usingAffinityForSchedule;
    }

    public double getThresholdForAffinitySchedule() {
        return thresholdForAffinitySchedule;
    }
    
    public void freeResources(Job job) {
        freeResources(job, this.allNodesInfo);
    }
    

    public void freeResources(Job job, ArrayList<NodeInfo> allNodeInfo) {
        int jobId = job.getJobId();
        ArrayList<UsingNode> usingNodesList = job.getUsingNodesList();
        
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();

        for (int i = 0; i < usingNodesList.size(); ++i) {
            UsingNode usingNode = usingNodesList.get(i);
            int nodeNo = usingNode.getNodeNum();
            NodeInfo nodeInfo = allNodeInfo.get(nodeNo);
            int numFreeCores = nodeInfo.getNumFreeCores();
            int numOccupiedCores = nodeInfo.getNumOccupiedCores();
            assert nodeInfo.getExecutingJobIds().contains(jobId);

            int numUsingCores = usingNode.getNumUsingCores();
            long mpn = job.getMaxMemory();
            
            /* Number of free/occupied Cores */
            numFreeCores += numUsingCores;
            assert numFreeCores <= nodeInfo.getNumCores();
            assert numFreeCores >= -(NodeConsciousScheduler.M-1)*nodeInfo.getNumCores();
            nodeInfo.setNumFreeCores(numFreeCores);
            numOccupiedCores -= numUsingCores;
            nodeInfo.setNumOccupiedCores(numOccupiedCores);

            /* Number of free/occupied Memory */
            if (scheduleUsingMemory) {
                long freeMemory = nodeInfo.getFreeMemory();
                long occupiedMemory = nodeInfo.getOccupiedMemory();
                freeMemory += mpn;
                assert freeMemory <= nodeInfo.getMemorySize();
                assert freeMemory >= 0;
                occupiedMemory -= mpn;
                assert occupiedMemory <= nodeInfo.getMemorySize();
                assert occupiedMemory >= 0;
                nodeInfo.setFreeMemory(freeMemory);
                nodeInfo.setOccupiedMemory(occupiedMemory);
            }
            
            /* Each core */
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < nodeInfo.getNumCores(); ++j) {
                CoreInfo eachCore = occupiedCores.get(j);
                ArrayList<Integer> jobList = eachCore.getJobList();
                for (int k = 0; k < jobList.size(); ++k) {
                    if (jobList.get(k) == jobId) {
                        jobList.remove(k);
                    }
                }
            }
            
            nodeInfo.getExecutingJobIds().remove(jobId);            
            // TODO:
            // Want to free usingNode
        }

    }

    public boolean isOutputUtilizationRatio() {
        return outputUtilizationRatio;
    }

    public boolean isOutputMinuteBoolean() {
        return outputMinuteBoolean;
    }

    void calculateUtilRatio(int currentTime, TimeDesc timeDesc) {

        ArrayList<Double> utilizations = new ArrayList();
        double averageUtilizationAmongNodes = 0.0;
        for(NodeInfo nodeInfo: allNodesInfo) {
            int nodeNum = nodeInfo.getNodeNum();
            int coreNum = nodeInfo.getNumCores();
            double nodeUtilization = 0.0;
            for(CoreInfo coreInfo: nodeInfo.getOccupiedCores()) {
                double coreUtilzation = 0.0;
                for (Integer jobId: coreInfo.getJobList()) {
                    Job job = jobMap.get(jobId);
                    int netOCStateLevel = job.getNetOCStateLevel();
                    boolean interactiveJob = job.isInteracitveJob();
                    if (interactiveJob) {
                        boolean actStateFlag = job.isActivationState();
                        if (actStateFlag) {
                            coreUtilzation += (double) 1.0/netOCStateLevel;
                        }
                    } else {
                        coreUtilzation += (double) 1.0/netOCStateLevel;
                    }
                }                
                assert coreUtilzation <= 1;
                nodeUtilization += coreUtilzation;
            }
            assert nodeUtilization <= coreNum; 
            nodeUtilization /= (double)coreNum;
            averageUtilizationAmongNodes += nodeUtilization;
            utilizations.add(nodeNum, nodeUtilization*100);
        }
        int numNodes = allNodesInfo.size();
        averageUtilizationAmongNodes /= numNodes;
        utilizations.add(numNodes, averageUtilizationAmongNodes*100);

        PrintWriter pwWork = null;
        int threshold = UNUPDATED;
        if (timeDesc == TimeDesc.MINUTE) {
            pwWork = this.pwForUtilMinute;
            threshold = MINUTE_IN_SECOND;
        } else if (timeDesc == TimeDesc.HOUR) {
            pwWork = this.pwForUtilHour;
            threshold = HOUR_IN_SECOND;
        } else if (timeDesc == TimeDesc.DAY) {
            pwWork = this.pwForUtilDay;
            threshold = DAY_IN_SECOND;            
        }

        int count = currentTime/threshold;
        pwWork.print(count + "\t");
        for (int i = 0; i < utilizations.size(); ++i) {
            Double util = utilizations.get(i);
            pwWork.print(util);
            if (i != utilizations.size()-1) {
                pwWork.print("\t");
            } else {
                pwWork.print("\n");
            }
        }
        
        

    }

    void calculateWastedResource(int currentTime, TimeDesc timeDesc) {

        ArrayList<Double> wastedResourceUtilizations = new ArrayList();
        double averageWastedUtilizationAmongNodes = 0.0;
        for(NodeInfo nodeInfo: allNodesInfo) {
            int nodeNum = nodeInfo.getNodeNum();
            int coreNum = nodeInfo.getNumCores();
            double wastedNodeUtilization = 0.0;
            for(CoreInfo coreInfo: nodeInfo.getOccupiedCores()) {
                double coreUtilzation = 0.0;
                double wastedUtilization = 0.0;
                int numDeactiveInteractiveJob = 0;
                ArrayList<Integer> jobIds = coreInfo.getJobList();
                if (jobIds.isEmpty()) {
                    wastedUtilization = 0.0;
                } else {
                    for (Integer jobId: jobIds) {
                        Job job = jobMap.get(jobId);
                        int netOCStateLevel = job.getNetOCStateLevel();
                        boolean interactiveJob = job.isInteracitveJob();
                        if (interactiveJob) {
                            boolean actStateFlag = job.isActivationState();
                            if (actStateFlag) {
                                coreUtilzation += (double) 1.0/netOCStateLevel;
                            } else {
                                ++numDeactiveInteractiveJob;
                            }
                        } else {
                            coreUtilzation += (double) 1.0/netOCStateLevel;
                        }
                    }
                    assert coreUtilzation <= 1;
                    assert numDeactiveInteractiveJob <= jobIds.size();
                    if (coreUtilzation < 1.0) {
                        wastedUtilization += (double) (1.0 - coreUtilzation);
                    }
                    if (numDeactiveInteractiveJob == jobIds.size()) {
                        wastedUtilization = 0.0;
                    }                    
                }
                wastedNodeUtilization += wastedUtilization;
            }
            assert wastedNodeUtilization <= coreNum; 
            wastedNodeUtilization /= (double)coreNum;
            averageWastedUtilizationAmongNodes += wastedNodeUtilization;
            wastedResourceUtilizations.add(nodeNum, wastedNodeUtilization*100);
        }
        int numNodes = allNodesInfo.size();
        averageWastedUtilizationAmongNodes /= numNodes;
        wastedResourceUtilizations.add(numNodes, averageWastedUtilizationAmongNodes*100);

        PrintWriter pwWork = null;
        int threshold = UNUPDATED;
        if (timeDesc == TimeDesc.MINUTE) {
            pwWork = this.pwForWastedResourceMinute;
            threshold = MINUTE_IN_SECOND;
        } else if (timeDesc == TimeDesc.HOUR) {
            pwWork = this.pwForWastedResourceHour;
            threshold = HOUR_IN_SECOND;
        } else if (timeDesc == TimeDesc.DAY) {
            pwWork = this.pwForWastedResourceDay;
            threshold = DAY_IN_SECOND;            
        } else if (timeDesc == TimeDesc.SECOND) {    
            pwWork = this.pwForWastedResourceSecond;
            threshold = SECOND;
        }

        int count = currentTime/threshold;
        pwWork.print(count + "\t");
        for (int i = 0; i < wastedResourceUtilizations.size(); ++i) {
            Double util = wastedResourceUtilizations.get(i);
            pwWork.print(util);
            if (i != wastedResourceUtilizations.size()-1) {
                pwWork.print("\t");
            } else {
                pwWork.print("\n");
            }
        }
        
        

    }
    
    private void writeUtilFileHeader(PrintWriter pwWork) {
        for (int j = 0; j < NodeConsciousScheduler.numNodes; ++j) {
            pwWork.print("\t" + j);
        }
        pwWork.println("\tAve.");
    }

    void flushCurrentWastedResourceToFile(int currentTime, List<Double> wastedResources) {
        double sum = 0.0;
        int i = 0;
        int numNode = NodeConsciousScheduler.numNodes;
        pwForWastedResourceTimeseries.print(currentTime);
        pwForWastedResourceTimeseries.print("\t");
        for (Double wr: wastedResources) {
            pwForWastedResourceTimeseries.print(wr);
            sum += wr;
            pwForWastedResourceTimeseries.print("\t");
            if (i == numNode - 1) {
                wr /= numNode;
                pwForWastedResourceTimeseries.print(sum);
                pwForWastedResourceTimeseries.print("\n");
            }
            ++i;
        }
    }

    public ScheduleConsiderJobType getScheduleConsiderJobType() {
        return scheduleConsiderJobType;
    }

    public OsubOverheadModelType getOsubOverheadModelType() {
        return osubOverheadModelType;
    }

    public double getOsubOverheadConst() {
        return osubOverheadConst;
    }
}

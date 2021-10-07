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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.Constants.ARRIVAL_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.ARRIVAL_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_FINISHED_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_FINISHED_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_FINISHED_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_STARTED_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_STARTED_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_STARTED_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.DAY_IN_SECOND;
import static nodeconsciousscheduler.Constants.FINISHED_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISHED_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISHED_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.FINISH_ORDER_JOB_OUTPUT;
import static nodeconsciousscheduler.Constants.FOR_VISUALIZATION_OUTPUT;
import static nodeconsciousscheduler.Constants.HOUR_IN_SECOND;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_OC_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_OC_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.INSTANT_UTILIZATION_RATIO_OC_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.MINUTE_IN_SECOND;
import static nodeconsciousscheduler.Constants.RESULT_DIRECTORY;
import static nodeconsciousscheduler.Constants.RESULT_EACH_GROUP;
import static nodeconsciousscheduler.Constants.RESULT_EACH_USER;
import static nodeconsciousscheduler.Constants.SLOWDOWN_OC_OUTPUT;
import static nodeconsciousscheduler.Constants.SLOWDOWN_OUTPUT;
import static nodeconsciousscheduler.Constants.START_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.START_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.START_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.UNUPDATED;
import static nodeconsciousscheduler.Constants.UTILIZATION_RATIO_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.WAITING_JOB_PER_MINUTE_OUTPUT;

/**
 *
 * @author sminami
 */
public class Simulator {
    private ArrayList<Job> jobList;
    private ArrayList<NodeInfo> allNodesInfo;
    private ScheduleAlgorithm scheAlgo;
    private Scheduler sche;
    private EventQueue evq;
    private ArrayList<Job> executingJobList;
    private ArrayList<Job> completedJobList;
    private PrintWriter pw;
    private PrintWriter pwForVis;
    private Path p;
    ArrayList<Double> thresholdForSlowdown;
    private boolean outputMinuteBoolean;

    Simulator(ArrayList<Job> jobList, ArrayList<NodeInfo> allNodesInfo, ScheduleAlgorithm scheAlgo, SimulatorConfiguration simConf) {
        this.jobList = jobList;
        this.allNodesInfo = allNodesInfo;
        this.scheAlgo = scheAlgo;
        initScheduler(scheAlgo);
        makeEventQueue();
        this.executingJobList = new ArrayList<Job>();
        this.completedJobList = new ArrayList<Job>();
        this.thresholdForSlowdown = simConf.getThresholdForSlowdown();
        this.outputMinuteBoolean = simConf.isOutputMinuteTimeseries();
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
        
        dir += "/" + date + "_" + fname[0] + "_" + this.scheAlgo + "_n" + NodeConsciousScheduler.numNodes + "c" + NodeConsciousScheduler.numCores + "_M" + NodeConsciousScheduler.M;
        Path p = Paths.get(dir);
        return p;
    }

    void initOutputResult() throws IOException {
        Files.createDirectories(this.p);
        
        String fileName = FINISH_ORDER_JOB_OUTPUT;
        String fileNameForVis = FOR_VISUALIZATION_OUTPUT;
        try {
            this.pw = new PrintWriter(this.p + "/" + fileName);
            pw.println("JobID\tuserId\tgroupId\tarrivalTime\twaitTime\tstartTime\tfinishedTime\trunnningTime\tslowdown\tslowdownOC\tnum cores\tnum nodes\tnode num(tcore num)");
            this.pwForVis = new PrintWriter(this.p + "/" + fileNameForVis);

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
        double slowdown = max(1.0, (double) (waitTime + runningTime) /runningTime);
        job.setSlowdown(slowdown);
        int originalRunningTime = job.getActualExecuteTime();
        double slowdownOC = max(1.0, (double) (waitTime + runningTime) /originalRunningTime);
        job.setSlowdownByOriginalRunningTime(slowdownOC);
        int numCores = job.getRequiredCores();
        int numNodes = job.getRequiredNodes();
        
        pw.print(jobId + "\t" + userId + "\t" + groupId + "\t" + arrivalTime + "\t" + waitTime + "\t" + startTime + "\t" + finishedTime + "\t" + runningTime + "\t"
                + slowdown + "\t" + slowdownOC + "\t" + numCores + "\t" + numNodes + "\t");
        
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

        outputUtilizationRatio();
        outputInstatntUtilizationRatio(INSTANT_UTILIZATION_RATIO_DAY_OUTPUT, DAY_IN_SECOND, false);
        outputInstatntUtilizationRatio(INSTANT_UTILIZATION_RATIO_HOUR_OUTPUT, HOUR_IN_SECOND, false);
        outputInstatntUtilizationRatio(INSTANT_UTILIZATION_RATIO_OC_DAY_OUTPUT, DAY_IN_SECOND, true);
        outputInstatntUtilizationRatio(INSTANT_UTILIZATION_RATIO_OC_HOUR_OUTPUT, HOUR_IN_SECOND, true);

        outputSlowdown(false);
        outputSlowdown(true);
        outputResultEachUserAndGroup();

        outputFinishedAndCumulativeFinishedJob(FINISHED_JOB_PER_DAY_OUTPUT, CUMULATIVE_FINISHED_JOB_PER_DAY_OUTPUT, DAY_IN_SECOND);
        outputFinishedAndCumulativeFinishedJob(FINISHED_JOB_PER_HOUR_OUTPUT, CUMULATIVE_FINISHED_JOB_PER_HOUR_OUTPUT, HOUR_IN_SECOND);

        
        outputWaitingAndNewArrivalJob(WAITING_JOB_PER_DAY_OUTPUT, ARRIVAL_JOB_PER_DAY_OUTPUT, START_JOB_PER_DAY_OUTPUT, CUMULATIVE_STARTED_JOB_PER_DAY_OUTPUT, DAY_IN_SECOND);
        outputWaitingAndNewArrivalJob(WAITING_JOB_PER_HOUR_OUTPUT, ARRIVAL_JOB_PER_HOUR_OUTPUT, START_JOB_PER_HOUR_OUTPUT, CUMULATIVE_STARTED_JOB_PER_HOUR_OUTPUT, HOUR_IN_SECOND);

        
        if (outputMinuteBoolean) {
            outputInstatntUtilizationRatio(INSTANT_UTILIZATION_RATIO_MINUTE_OUTPUT, MINUTE_IN_SECOND, false);
            outputInstatntUtilizationRatio(INSTANT_UTILIZATION_RATIO_OC_MINUTE_OUTPUT, MINUTE_IN_SECOND, true);
            outputFinishedAndCumulativeFinishedJob(FINISHED_JOB_PER_MINUTE_OUTPUT, CUMULATIVE_FINISHED_JOB_PER_MINUTE_OUTPUT, MINUTE_IN_SECOND);
            outputWaitingAndNewArrivalJob(WAITING_JOB_PER_MINUTE_OUTPUT, ARRIVAL_JOB_PER_MINUTE_OUTPUT, START_JOB_PER_MINUTE_OUTPUT, CUMULATIVE_STARTED_JOB_PER_MINUTE_OUTPUT, MINUTE_IN_SECOND);
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
                if (slowdown < threshold.get(j)) {
                    result.set(j, result.get(j) + 1);
                    break;
                }
            }
            if (slowdown >= threshold.get(threshold.size()-1)) {
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
                pwSlowdown.println("<" + thresholdForSlowdown.get(i) + "\t" + (double)histgram.get(i)/jobnum);
            }
            pwSlowdown.println(">=" + thresholdForSlowdown.get(thresholdForSlowdown.size() - 1) + "\t" + (double)histgram.get(histgram.size() - 1)/jobnum);
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

    private void outputUtilizationRatio() {
        try {
            String fileName = UTILIZATION_RATIO_OUTPUT;        
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

    private void outputInstatntUtilizationRatio(String fileName, int MODE, boolean OCFlag) {
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
                    //pwUtilizationRatio.println(i + "\t" + resultWaiting.get(i));
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

    private ArrayList<Double> calcUtilizationAtTs(TimeSlice ts, boolean OCFlag) {
        ArrayList<Double> result = new ArrayList<Double>(); 
        ArrayList<Integer> availableCores = ts.getAvailableCores();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            int numRuuningCore = OCFlag ? NodeConsciousScheduler.numCores - availableCores.get(i) : min(NodeConsciousScheduler.numCores - availableCores.get(i), NodeConsciousScheduler.numCores);
            result.add((double)numRuuningCore/NodeConsciousScheduler.numCores*100);
        }
        return result;
    }

    private void outputWaitingAndNewArrivalJob(String fileNameWaiting, String fileNameArrival, String fileNameStart, String fileNameCumulativeStart, int MODE) {
            PairIntegers pairResult = new PairIntegers();
            pairResult = calcWaitingAndNewArrivalJob(MODE);

            ArrayList<Integer> resultWaiting = pairResult.getNumWaitingJobs();
            printTimeSeriesToFile(fileNameWaiting, resultWaiting);

            ArrayList<Integer> resultArrival = pairResult.getNumNewArrivalJobs();
            printTimeSeriesToFile(fileNameArrival, resultArrival);

            ArrayList<Integer> resultStart = pairResult.getNumNewStartJobs();
            printTimeSeriesToFile(fileNameStart, resultStart);

            ArrayList<Integer> resultCumulativeStart = pairResult.getNumCumulativeStartJobs();
            printTimeSeriesToFile(fileNameCumulativeStart, resultCumulativeStart);
            
    }

    private PairIntegers calcWaitingAndNewArrivalJob(int THRESHOLD) {
        ArrayList<Integer> resultWaiting = new ArrayList<Integer>();
        ArrayList<Integer> resultArrival = new ArrayList<Integer>();
        ArrayList<Integer> resultStart = new ArrayList<Integer>();
        ArrayList<Integer> resultCumulativeStart = new ArrayList<Integer>();
        PairIntegers resultPair = new PairIntegers();
        resultPair.setNumNewArrivalJobs(resultArrival);
        resultPair.setNumWaitingJobs(resultWaiting);
        resultPair.setNumNewStartJobs(resultStart);
        resultPair.setNumCumulativeStartJobs(resultCumulativeStart);
        ArrayList<Job> completedJobList = (ArrayList<Job>) this.completedJobList.clone();
        int i = 0;
        int previousI = 0;
        int threshold = 0;
        int numWaitingJob = 0;
        int numNewArrivalJobPerTHRESHOLD = 0;
        int numCumulativeStartedJobs = 0;
        

        Collections.sort(completedJobList);
        
        for (;;) {
            while (i != completedJobList.size() && completedJobList.get(i).getSubmitTime()<= threshold) {
                ++numNewArrivalJobPerTHRESHOLD;
                ++numWaitingJob;
                ++i;
            }
            
            int numStartedJob = 0;
            while (completedJobList.get(previousI).getStartTime()<= threshold) {
                ++numStartedJob;
                ++previousI;
                if (previousI == completedJobList.size()) break;
            }

            numWaitingJob -= numStartedJob;
            numCumulativeStartedJobs += numStartedJob;
            
            resultWaiting.add(numWaitingJob);
            resultArrival.add(numNewArrivalJobPerTHRESHOLD);
            resultStart.add(numStartedJob);
            resultCumulativeStart.add(numCumulativeStartedJobs);
            threshold +=  THRESHOLD;
            numNewArrivalJobPerTHRESHOLD = 0;
            if (previousI == completedJobList.size()) break;
        }
        return resultPair;
    }

    private void printTimeSeriesToFile(String fileName, ArrayList<Integer> resultTimeSeries) {
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
            
            pw.print("userId\tnumJob\trunningTime\trunningTime(ave.)\twaitTime\twaitTime(ave.)\tnumNode\tnumNode(ave.)\tnumCore\tnumCore(ave.)\t"
                     + "CpuTime\tCpuTime(ave.)\tMemoryFootprint\tMemoryFootprint(ave)\tlargeJobRatio\t");

            int sdSize = thresholdForSlowdown.size();
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<" + thresholdForSlowdown.get(i) + "\t");                
            }
            pw.print(">=" + thresholdForSlowdown.get(sdSize-1) + "\t");
            
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<" + thresholdForSlowdown.get(i) + "(OC)\t");                
            }
            pw.println(">=" + thresholdForSlowdown.get(sdSize-1) + "(OC)");
            
            for (int i = 0; i < resultEachUser.size() ; ++i) {
                UserResult result = resultEachUser.get(i);
                
                int userId = result.getUserId();
                int numJob = result.getNumJobs();
                int runningTime = result.getAccumulatedTime();
                int waitTime = result.getAccumulatedWaitTime();
                int numNode = result.getAccumulatedNumNode();
                int numCore = result.getAccumulatedNumCore();
                int cpuTime = result.getAccumulatedCpuTime();
                int memoryFootprint = result.getAccumulatedMemoryFootprint();
                int cntSpecifiedMaxMemory = result.getNumJobsSetMemory();
                double averagedNumNode = result.getAccumulatedNumNode();
                double averagedNumCore = result.getAccumulatedNumCore();
                double averagedRunningTime = result.getAveragedTime();
                double averagedCpuTime = result.getAveragedCpuTime();
                double averagedMemoryFootprint = result.getAveragedMemoryFootprint();
                double averagedWaitTime = result.getAveragedWaitTime();
                double largeJobRatio = result.getLargeJobRatio();
                
                pw.print(userId + "\t" + numJob + "\t" + runningTime + "\t" + averagedRunningTime + "\t" + waitTime + "\t" + averagedWaitTime + "\t" +
                           numNode + "\t" + averagedNumNode + "\t" + numCore + "\t" + averagedNumCore + "\t" + cpuTime + "\t" + averagedCpuTime + "\t" + 
                           memoryFootprint + "\t" + averagedMemoryFootprint + "\t" + largeJobRatio + "\t");
                
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
                    pw.print((double)slowdownsOC.get(j)/numJob + "\t");
                }

                for (int j = 0; j < size - 1; ++j) {
                    slowdownsOC.set(j+1, slowdownsOC.get(j) + slowdownsOC.get(j+1));
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
            int accumulatedCpuTime = 0;
            int accumulatedMaxMemory = 0;
            int cntSpecifiedMaxMemory = 0;
            int cntLargeJob = 0;
            
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
                int numNode = job.getRequiredNodes();
                int numCorePerNode = job.getRequiredCoresPerNode();
                int cpuTime = numNode * numCorePerNode * runningTime;
                
                accumulatedRunningTime += runningTime;
                accumulatedWaitTime += job.getWaitTime();
                accumulatedNumNode += numNode;
                accumulatedNumCore += numCorePerNode;
                accumulatedCpuTime += cpuTime;
                
                if (checkLargeJob(runningTime, numNode, numCorePerNode, cpuTime)) {
                    ++cntLargeJob;
                }
                
                int maxMemory = job.getMaxMemory();
                if (maxMemory > 0) {
                    accumulatedMaxMemory += maxMemory;
                    ++cntSpecifiedMaxMemory;
                }
                
                double slowdown = job.getSlowdown();
                boolean addedFlag = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdown < thresholdForSlowdown.get(i)) {
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
                    if (slowdownOC < thresholdForSlowdown.get(i)) {
                        slowdownOCHistgramEachUser.set((Integer) i, slowdownOCHistgramEachUser.get(i) + 1);
                        addedFlagOC = true;
                        break;
                    }                    
                }
                if (!addedFlagOC) {
                    int lastIndex = slowdownOCHistgramEachUser.size()-1;
                    slowdownOCHistgramEachUser.set((Integer) lastIndex, slowdownOCHistgramEachUser.get(lastIndex) + 1);
                }                
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
                                              accumulatedWaitTime, averagedWaitTime, largeJobRatio, slowdownHistgramEachUser, slowdownOCHistgramEachUser));           
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
            int accumulatedMaxMemory = 0;
            int cntSpecifiedMaxMemory = 0;
            int cntLargeJob = 0;
            
            ArrayList<Integer> slowdownHistgramEachUser = new ArrayList<Integer>();
            ArrayList<Integer> slowdownOCHistgramEachUser = new ArrayList<Integer>();            
            for (int i = 0; i <= thresholdForSlowdown.size(); ++i) {
                slowdownHistgramEachUser.add(0);
                slowdownOCHistgramEachUser.add(0);
            }
                        
            for (Job job: jobList) {
                assert (int)groupId == job.getGroupId();

                int runningTime = job.getRunningTimeDed() + job.getRunningTimeOC();
                int numNode = job.getRequiredNodes();
                int numCorePerNode = job.getRequiredCoresPerNode();
                int cpuTime = numNode * numCorePerNode * runningTime;
                
                accumulatedRunningTime += runningTime;
                accumulatedWaitTime += job.getWaitTime();
                accumulatedNumNode += numNode;
                accumulatedNumCore += numCorePerNode;
                accumulatedCpuTime += cpuTime;
                
                if (checkLargeJob(runningTime, numNode, numCorePerNode, cpuTime)) {
                    ++cntLargeJob;
                }
                
                int maxMemory = job.getMaxMemory();
                if (maxMemory > 0) {
                    accumulatedMaxMemory += maxMemory;
                    ++cntSpecifiedMaxMemory;
                }
                
                double slowdown = job.getSlowdown();
                boolean addedFlag = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdown < thresholdForSlowdown.get(i)) {
                        slowdownHistgramEachUser.set((Integer) i, slowdownHistgramEachUser.get(i) + 1);
                        addedFlag = true;
                        break;
                    }                    
                }
                if (!addedFlag) {
                    int lastIndex = slowdownHistgramEachUser.size()-1;
                    slowdownHistgramEachUser.set((Integer) lastIndex, slowdownHistgramEachUser.get(lastIndex) + 1);
                }

                double slowdownOC = job.getSlowdown();
                boolean addedFlagOC = false;
                for (int i = 0; i < thresholdForSlowdown.size(); ++i) {
                    if (slowdownOC < thresholdForSlowdown.get(i)) {
                        slowdownOCHistgramEachUser.set((Integer) i, slowdownOCHistgramEachUser.get(i) + 1);
                        addedFlagOC = true;
                        break;
                    }                    
                }
                if (!addedFlagOC) {
                    int lastIndex = slowdownOCHistgramEachUser.size()-1;
                    slowdownOCHistgramEachUser.set((Integer) lastIndex, slowdownOCHistgramEachUser.get(lastIndex) + 1);
                }                
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
                                              accumulatedWaitTime, averagedWaitTime, largeJobRatio, slowdownHistgramEachUser, slowdownOCHistgramEachUser));           
        }
        
    }    

    private void printByGroup(ArrayList<GroupResult> resultEachGroup) {
        String fileName = RESULT_EACH_GROUP;
        try {
            PrintWriter pw;
            pw = new PrintWriter(this.p + "/" + fileName);
            
            pw.print("groupId\tnumJob\trunningTime\trunningTime(ave.)\twaitTime\twaitTime(ave.)\tnumNode\tnumNode(ave.)\tnumCore\tnumCore(ave.)\t"
                     + "CpuTime\tCpuTime(ave.)\tMemoryFootprint\tMemoryFootprint(ave)\tlargeJobRatio\t");

            int sdSize = thresholdForSlowdown.size();
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<" + thresholdForSlowdown.get(i) + "\t");                
            }
            pw.print(">=" + thresholdForSlowdown.get(sdSize-1) + "\t");
            
            for (int i = 0; i < sdSize ; ++i) {
                pw.print("<" + thresholdForSlowdown.get(i) + "(OC)\t");                
            }
            pw.println(">=" + thresholdForSlowdown.get(sdSize-1) + "(OC)");            
            
            
            for (int i = 0; i < resultEachGroup.size() ; ++i) {
                GroupResult result = resultEachGroup.get(i);
                
                int groupId = result.getGroupId();
                int numJob = result.getNumJobs();
                int runningTime = result.getAccumulatedTime();
                int waitTime = result.getAccumulatedWaitTime();
                int numNode = result.getAccumulatedNumNode();
                int numCore = result.getAccumulatedNumCore();
                int cpuTime = result.getAccumulatedCpuTime();
                int memoryFootprint = result.getAccumulatedMemoryFootprint();
                int cntSpecifiedMaxMemory = result.getNumJobsSetMemory();
                double averagedNumNode = result.getAccumulatedNumNode();
                double averagedNumCore = result.getAccumulatedNumCore();
                double averagedRunningTime = result.getAveragedTime();
                double averagedCpuTime = result.getAveragedCpuTime();
                double averagedMemoryFootprint = result.getAveragedMemoryFootprint();
                double averagedWaitTime = result.getAveragedWaitTime();
                double largeJobRatio = result.getLargeJobRatio();


                pw.print(groupId + "\t" + numJob + "\t" + runningTime + "\t" + averagedRunningTime + "\t" + waitTime + "\t" + averagedWaitTime + "\t" +
                           numNode + "\t" + averagedNumNode + "\t" + numCore + "\t" + averagedNumCore + "\t" + cpuTime + "\t" + averagedCpuTime + "\t" + 
                           memoryFootprint + "\t" + averagedMemoryFootprint + "\t" + largeJobRatio + "\t");

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
                    pw.print((double)slowdownsOC.get(j)/numJob + "\t");
                }

                for (int j = 0; j < size - 1; ++j) {
                    slowdownsOC.set(j+1, slowdownsOC.get(j) + slowdownsOC.get(j+1));
                }
                
                pw.println((double)slowdownsOC.get(sizeOC - 1)/numJob);                            
            }
            
            pw.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    
}

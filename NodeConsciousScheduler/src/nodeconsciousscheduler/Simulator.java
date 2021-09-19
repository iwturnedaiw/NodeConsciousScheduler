/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.max;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.Constants.CUMULATIVE_JOB_PER_DAY_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_JOB_PER_HOUR_OUTPUT;
import static nodeconsciousscheduler.Constants.CUMULATIVE_JOB_PER_MINUTE_OUTPUT;
import static nodeconsciousscheduler.Constants.DAY_IN_SECOND;
import static nodeconsciousscheduler.Constants.FINISH_ORDER_JOB_OUTPUT;
import static nodeconsciousscheduler.Constants.FOR_VISUALIZATION_OUTPUT;
import static nodeconsciousscheduler.Constants.HOUR_IN_SECOND;
import static nodeconsciousscheduler.Constants.MINUTE_IN_SECOND;
import static nodeconsciousscheduler.Constants.RESULT_DIRECTORY;
import static nodeconsciousscheduler.Constants.SLOWDOWN_OUTPUT;

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

    Simulator(ArrayList<Job> jobList, ArrayList<NodeInfo> allNodesInfo, ScheduleAlgorithm scheAlgo) {
        this.jobList = jobList;
        this.allNodesInfo = allNodesInfo;
        this.scheAlgo = scheAlgo;
        initScheduler(scheAlgo);
        makeEventQueue();
        this.executingJobList = new ArrayList<Job>();
        this.completedJobList = new ArrayList<Job>();
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
        } else if (scheAlgo == ScheduleAlgorithm.EasyBackfilling) {
            this.sche = new EasyBackfilling();
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

        dir += "/" + date;
        Path p = Paths.get(dir);
        return p;
    }

    void initOutputResult() throws IOException {
        Files.createDirectories(this.p);
        
        String fileName = FINISH_ORDER_JOB_OUTPUT;
        String fileNameForVis = FOR_VISUALIZATION_OUTPUT;
        try {
            this.pw = new PrintWriter(this.p + "/" + fileName);
            pw.println("JobID\tarrivalTime\twaitTime\tstartTime\tfinishedTime\trunnningTime\tslowdown\tnum cores\tnum nodes\tnode num(tcore num)");
            this.pwForVis = new PrintWriter(this.p + "/" + fileNameForVis);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void outputResult(Job job) {
        
        int jobId = job.getJobId();
        int arrivalTime = job.getSubmitTime();
        int waitTime = job.getWaitTime();
        int startTime = job.getStartTime();
        int finishedTime = job.getFinishedTime();
        int runningTime = job.getRunningTimeDed() + job.getRunningTimeOC();
        double slowdown = max(1.0, (double) (waitTime + runningTime) /runningTime);
        job.setSlowdown(slowdown);
        int numCores = job.getRequiredCores();
        int numNodes = job.getRequiredNodes();
        
        pw.print(jobId + "\t" + arrivalTime + "\t" + waitTime + "\t" + startTime + "\t" + finishedTime + "\t" + runningTime + "\t"
                + slowdown + "\t" + numCores + "\t" + numNodes + "\t");
        
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

        outputSlowdown();

        outputCumulativeJob(CUMULATIVE_JOB_PER_DAY_OUTPUT, DAY_IN_SECOND);
        outputCumulativeJob(CUMULATIVE_JOB_PER_HOUR_OUTPUT, HOUR_IN_SECOND);
        outputCumulativeJob(CUMULATIVE_JOB_PER_MINUTE_OUTPUT, MINUTE_IN_SECOND);
        
        return;
    }

    private Collection<? extends Integer> countSlowdown(ArrayList<Double> threshold) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < threshold.size(); ++i) result.add((Integer) 0);
        
        for (int i = 0; i < completedJobList.size(); ++i) {
            double slowdown = completedJobList.get(i).getSlowdown();
            
            for (int j = 0; j < threshold.size(); ++j) {
                if (slowdown < threshold.get(j)) {
                    result.set(j, result.get(j) + 1);
                    break;
                }
            }
            if (slowdown >= threshold.get(threshold.size()-1)) {
                result.set(threshold.size()-1, result.get(threshold.size()-1) + 1);
            }
        }
        return result;
    }

    private void outputSlowdown() {
        try {
            String fileName = SLOWDOWN_OUTPUT;        
            PrintWriter pwSlowdown;
            pwSlowdown = new PrintWriter(this.p + "/" + fileName);
            ArrayList<Double> threshold = new ArrayList<Double>();
            threshold.add(1.01);
            threshold.add(2.00);
            threshold.add(5.00);
            threshold.add(20.00);
            threshold.add(50.00);
            threshold.add(100.0);
            threshold.add(1000.0);

            ArrayList<Integer> histgram = new ArrayList<Integer>();
            histgram.addAll(countSlowdown(threshold));

            for (int i = 0; i < histgram.size() - 1; ++i) {
                pwSlowdown.println("<" + threshold.get(i) + "\t" + histgram.get(i));
            }
            pwSlowdown.println(">=" + threshold.get(histgram.size() - 1) + "\t" + histgram.get(histgram.size() - 1));
            pwSlowdown.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void outputCumulativeJob(String fileName, int MODE) {
        try {
            PrintWriter pwCumulative;
            pwCumulative = new PrintWriter(this.p + "/" + fileName);

            ArrayList<Integer> result = new ArrayList<Integer>();
            result.addAll(countCumlativeJob(MODE));
            if (result.size() != 0) {
                for (int i = 0; i < result.size() - 1; ++i) {
                    pwCumulative.println(i + 1 + "\t" + result.get(i));
                }
                pwCumulative.println(result.size() - 1 + 1 + "\t" + result.get(result.size() - 1));
            }
            pwCumulative.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Simulator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    private Collection<? extends Integer> countCumlativeJob(int THRESHOLD) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        int i = 0;
        int threshold = THRESHOLD;
        int cnt = 0;
        for (; i < completedJobList.size();) {
            while (completedJobList.get(i).getFinishedTime() <= threshold) {
                ++cnt;
                ++i;
                if (i == completedJobList.size()) break;
            }
            result.add(cnt);
            threshold +=  THRESHOLD;
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
        
        pwForVis.print(jobId + "\t" + previousMigratingTime + "\t" + currentTime + "\t" + startFlag + "\t" + numCores + "\t" + numNodes + "\t");
        
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
}

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    Simulator(ArrayList<Job> jobList, ArrayList<NodeInfo> allNodesInfo, ScheduleAlgorithm scheAlgo) {
        this.jobList = jobList;
        this.allNodesInfo = allNodesInfo;
        this.scheAlgo = scheAlgo;
        initScheduler(scheAlgo);
        makeEventQueue();
        this.executingJobList = new ArrayList<Job>();
        this.completedJobList = new ArrayList<Job>();
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
        pw.close();
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
        }
        /*
        } else if (scheAlgo == ScheduleAlgorithm.EasyBackfilling) {
            this.sche = new EasyBackfilling();
        }
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


    void initOutputResult() throws IOException {
        String dir = "result";

        Calendar cl = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        String date = sdf.format(cl.getTime());

        dir += "/" + date;
        Path p = Paths.get(dir);
        Files.createDirectories(p);
        
        
        String fileName = "test.out";
        try {
            this.pw = new PrintWriter(p + "/" + fileName);
            pw.println("JobID\tarrivalTime\twaitTime\tstartTime\tfinishedTime\trunnningTime\tslowdown\tnum cores\tnum nodes\tnode num(tcore num)");                        
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
        int numCores = job.getRequiredCores();
        int numNodes = job.getRequiredNodes();
        
        pw.print(jobId + "\t" + arrivalTime + "\t" + waitTime + "\t" + startTime + "\t" + finishedTime + "\t" + runningTime + "\t"
                + slowdown + "\t" + numCores + "\t" + numNodes + "\t");
        
        ArrayList<UsingNodes> usingNodesList = job.getUsingNodesList();
        for (int i = 0; i < numNodes; ++i) {
            UsingNodes usingNode = usingNodesList.get(i);
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

    
    

}

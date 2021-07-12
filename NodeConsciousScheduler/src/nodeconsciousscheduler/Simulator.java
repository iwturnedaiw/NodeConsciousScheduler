/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.PriorityQueue;

/**
 *
 * @author sminami
 */
public class Simulator {
    private ArrayList<Job> jobList;
    private ArrayList<NodeInfo> allNodesInfo;
    private ScheduleAlgorithm scheAlgo;
    private Scheduler sche;
    EventQueue evq;

    Simulator(ArrayList<Job> jobList, ArrayList<NodeInfo> allNodesInfo, ScheduleAlgorithm scheAlgo) {
        this.jobList = jobList;
        this.allNodesInfo = allNodesInfo;
        this.scheAlgo = scheAlgo;
        initScheduler(scheAlgo);
        makeEventQueue();
    }
    
    public void run() {
        while (evq.size() > 0) {
//            Event ev = (Event) evq.poll();
//            System.out.println("Job Id: " + ev.getJob().getJobId() + ", Submission time " + ev.getJob().getSubmitTime());
            advance();
        }
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


}

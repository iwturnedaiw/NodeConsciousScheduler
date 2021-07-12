/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.PriorityQueue;
import nodeconsciousscheduler.ScheduleAlgorithm;
import static nodeconsciousscheduler.ScheduleAlgorithm.FCFS;

/**
 *
 * @author sminami
 */
public class NodeConsciousScheduler {

    static Simulator sim;
    static int numNode;
    static int ppn;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Resoures Setting
        NodeInfo node0 = new NodeInfo(0, 32);
        NodeInfo node1 = new NodeInfo(1, 32);
        NodeInfo node2 = new NodeInfo(2, 32);
        NodeInfo node3 = new NodeInfo(3, 32);
        ppn = node0.getNumCores();
        
        ArrayList<NodeInfo> allNodesInfo = new ArrayList<NodeInfo>();
        allNodesInfo.add(node0);
        allNodesInfo.add(node1);
        allNodesInfo.add(node2);
        allNodesInfo.add(node3);
        numNode = allNodesInfo.size();

        // Workload Trace Setting
        Job job0 = new Job(0, 1, 500, 1000, 40, 4);
        Job job1 = new Job(1, 10, 1500, 3000, 10, 1);
        Job job2 = new Job(2, 100, 100, 200, 12, 2);
        Job job3 = new Job(3, 10000, 100, 200, 12, 2);
        ArrayList<Job> jobList = new ArrayList<Job>();
        jobList.add(job3);
        jobList.add(job0);
        jobList.add(job2);
        jobList.add(job1);
        
        sim = new Simulator(jobList, allNodesInfo, FCFS);
        sim.run();
/*        
        PriorityQueue<Event> pq = new EventQueue();
        EventQueue evq = (EventQueue) pq;
        for (int i = 0; i < jobList.size(); ++i) {
            Job job = jobList.get(i);
            evq.enqueueJob(job);
        }

        
        while (evq.size() > 0) {
            Event ev = (Event) evq.poll();
            
            
        }
  */
    }
    
}

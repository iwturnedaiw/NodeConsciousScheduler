/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import nodeconsciousscheduler.ScheduleAlgorithm;
import static nodeconsciousscheduler.ScheduleAlgorithm.*;


/**
 *
 * @author sminami
 */
public class NodeConsciousScheduler {

    static Simulator sim;
    static int numNodes;
    static int numCores;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String fname = "gen02.swf";
        //String fname = "short.swf";
        
        // Resoures Setting
/*
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
*/
        ArrayList<NodeInfo> allNodesInfo = new ArrayList<NodeInfo>();
        allNodesInfo = readResourceSettings(fname);

        // Workload Trace Setting
        /*        
        Job job0 = new Job(0, 1, 500, 1000, 40, 4);
        Job job1 = new Job(1, 10, 1500, 3000, 10, 1);
        Job job2 = new Job(2, 100, 100, 200, 12, 2);
        Job job3 = new Job(3, 10000, 100, 200, 12, 2);
        ArrayList<Job> jobList = new ArrayList<Job>();
        jobList.add(job3);
        jobList.add(job0);
        jobList.add(job2);
        jobList.add(job1);
        */      
        ArrayList<Job> jobList = new ArrayList<Job>();
        try {
            jobList = readSWFFile(fname);
        } catch (IOException ex) {
            Logger.getLogger(NodeConsciousScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        ScheduleAlgorithm sche = EasyBackfilling;
        //sim = new Simulator(jobList, allNodesInfo, FCFS);
        sim = new Simulator(jobList, allNodesInfo, sche);
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

    private static ArrayList<NodeInfo> readResourceSettings(String data_set) {
        String fname = "configuration.properties";

        String dir = "./" + fname;
        Path p = Paths.get(dir);       
 
        LinkedList lines = new LinkedList();
        Input r = new Input();

        BufferedReader br = null;

        br = r.openFile(new File("data-set/" + data_set + ".machines"));
        System.out.println("Opening: " + "data-set/" + data_set + ".machines");
        r.getLines(lines, br);
        r.closeFile(br);

        if (lines.size() != 1) {
            System.out.println("Resource must be homogeneous");
            System.exit(1);
        }
        
        String[] values = ((String) lines.get(0)).split("\t");
        //System.out.println(lines.get(j));
        int id = Integer.parseInt(values[0]);
        long ram = 1024;
        if (values.length > 5) {
            //ram in KB
            ram = Long.parseLong(values[5]);
        }

        int numNodes = Integer.parseInt(values[2]);
        int numCores = Integer.parseInt(values[3]);
        NodeConsciousScheduler.numCores = numCores;
        NodeConsciousScheduler.numNodes = numNodes;
        int peRating = Integer.parseInt(values[4]);
        String name = values[1];

        ArrayList<NodeInfo> nodeInfoList = new ArrayList<NodeInfo>();
        for (int i = 0; i < numNodes; ++i){
            NodeInfo nodeInfo = new NodeInfo(i, numCores);
            nodeInfoList.add(nodeInfo);
        }            

        return nodeInfoList;
    }

    private static ArrayList<Job> readSWFFile(String fname) throws IOException {
        System.out.println("Opening job file at: " + "data-set/" + fname);
        BufferedReader br = null;
        Input in = new Input();
        br = in.openFile(new File("data-set/" + fname));
        
        String[] values = null;
        String line = "";



        ArrayList jobList = new ArrayList<Job>();
        while ((line = br.readLine()) != null) {
            if (line.charAt(0) == ' ') {
                line = line.substring(1);
            }
            if (line.charAt(0) == ' ') {
                line = line.substring(1);
            }
            if (line.charAt(0) == ' ') {
                line = line.substring(1);
            }
            if (line.charAt(0) == ' ') {
                line = line.substring(1);
            } 
            if (line.charAt(0) == ';') {
                continue;
            }
            values = line.split("\\s+");

            int jobId = Integer.parseInt(values[0]);
            int requiredCores = 0;
            try {
                requiredCores = Integer.parseInt(values[4]);
            } catch (NumberFormatException ex) {
                System.out.println(values[0] + ": Number parsing error: " + values[4]);
                ex.printStackTrace();
                //numCPU = 1;
            }
            
            int submitTime = Integer.parseInt(values[1]);
            int actualExecuteTime = Integer.parseInt(values[3]);
            int specifiedExecuteTime = Integer.parseInt(values[8]);

            int requiredNodes = 1;
            int ppn = -1;

            String properties = "";
            if (values.length > 19) {
                properties = values[20];

                String[] req_nodes = values[20].split(":");
                requiredNodes = Integer.parseInt(req_nodes[0]);
                
            }
            
            if (requiredNodes > NodeConsciousScheduler.numNodes) {
                continue;
            }
            
            if (requiredCores > requiredNodes * NodeConsciousScheduler.numCores) {
                continue;
            }            
            
            // TODO
            // Decide the accurate num of node for non-specified data
            
            Job job = new Job(jobId, submitTime, actualExecuteTime, specifiedExecuteTime, requiredCores, requiredNodes);
            jobList.add(job);
            
        }
        return jobList;
        
    }
}

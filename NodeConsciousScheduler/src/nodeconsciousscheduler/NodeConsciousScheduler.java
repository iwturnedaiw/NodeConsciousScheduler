/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import static java.lang.Math.min;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.Constants.CONFIGURATION_FILE;
import static nodeconsciousscheduler.Constants.DATASET_DIRECTORY;
import static nodeconsciousscheduler.Constants.UNSPECIFIED;
import static nodeconsciousscheduler.Constants.UNUPDATED;
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
    static long memory;
    static int M = 2;
    static String fname = "gen01.swf";
    static ScheduleAlgorithm sche = EasyBackfilling;
    static boolean ignoreIncompleteMemoryData = false;
    static boolean memoryDataPerCore = false;
    static boolean memoryDataPerNode = false;
    static boolean outputResultsInDetail = true;
    static boolean considerMemoryForNodeNum = false;
    static Map<Integer, Integer> matchingGroup;
    static int interactiveQueueNumber = -1;

    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if (args.length == 3){
            fname = args[0];
            sche = ScheduleAlgorithm.valueOf(args[1]);
            M = Integer.parseInt(args[2]);
        } else if (args.length == 2) {
            fname = args[0];
            sche = ScheduleAlgorithm.valueOf(args[1]);
        }
        //fname = "gen01.swf";
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
        SimulatorConfiguration simConf = readSimulatorConfiguration(CONFIGURATION_FILE);
        boolean scheduleUsingMemory = simConf.isScheduleUsingMemory();
        boolean considerJobMatching = simConf.isConsiderJobMatching();
        boolean usingAffinityForSchedule = simConf.isUsingAffinityForSchedule();
                
        if (memoryDataPerCore & memoryDataPerNode) {
            System.out.println("Configuration Error. Both MEMORY_DATA_PER_CORE and MEMORY_DATA_PER_NODE cannot be set true");
            System.exit(1);
        } 
        
        if (considerMemoryForNodeNum & scheduleUsingMemory) {
            System.out.println("Configuration Error. Both SCHEDULE_USING_MEMORY and CONSIDER_MEMORY_FOR_JOB_NODENUM cannot be set true");
            System.exit(1);            
        }
        
        if (usingAffinityForSchedule && !considerJobMatching) {
            System.out.println("Configuration Error. SCHEDULE_USING_AFFINITY must be set false when CONSIDER_JOB_MATCHING is false.");
            System.exit(1);
        }
        
        Map<JobMatching, Double> jobMatchingTable = new HashMap<>();
        if (considerJobMatching) {
            matchingGroup = readJobMatchingGroupTable(fname);
            jobMatchingTable = readJobMatchingTable(fname);
            simConf.setJobMatchingTable(jobMatchingTable);
        }
        
        boolean accurateInteractiveJobs = simConf.isAccurateInteractiveJobs();
        double interactiveCPURatio = simConf.getInteractiveCPURatio();
        if (accurateInteractiveJobs && interactiveQueueNumber == -1) {
            System.out.println("Configuration Error. INTERACTIVE_QUEUE_NUMBER must be set.");
            System.exit(1);            
        }
        if (accurateInteractiveJobs && ( (interactiveCPURatio <= 0) || (interactiveCPURatio >= 1))) {
            System.out.println("Configuration Error. INTERACTIVE_CPU_RATIO must be set between 0 < r < 1.");
            System.exit(1);            
        }
        
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
            jobList = readSWFFile(fname, scheduleUsingMemory, considerJobMatching, accurateInteractiveJobs, interactiveCPURatio);
        } catch (IOException ex) {
            Logger.getLogger(NodeConsciousScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //ScheduleAlgorithm sche = EasyBackfilling;
        //sim = new Simulator(jobList, allNodesInfo, FCFS);
        sim = new Simulator(jobList, allNodesInfo, sche, simConf);
        sim.run();
        if (outputResultsInDetail) sim.makeResults();
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
        LinkedList lines = new LinkedList();
        Input r = new Input();

        BufferedReader br = null;

        br = r.openFile(new File(DATASET_DIRECTORY + "/" + data_set + ".machines"));
        System.out.println("Opening: " + DATASET_DIRECTORY + "/" + data_set + ".machines");
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
        NodeConsciousScheduler.memory = ram;
        int peRating = Integer.parseInt(values[4]);
        String name = values[1];

        ArrayList<NodeInfo> nodeInfoList = new ArrayList<NodeInfo>();
        for (int i = 0; i < numNodes; ++i){
//            NodeInfo nodeInfo = new NodeInfo(i, numCores);
            NodeInfo nodeInfo = new NodeInfo(i, numCores, NodeConsciousScheduler.memory);
            nodeInfoList.add(nodeInfo);
        }            

        return nodeInfoList;
    }

    private static ArrayList<Job> readSWFFile(String fname, 
            boolean scheduleUsingMemory, 
            boolean considerJobMatching,
            boolean accurateInteractiveJobs,
            double interactiveCPURatio) throws IOException {
        boolean ignoreIncompleteMemoryData = NodeConsciousScheduler.ignoreIncompleteMemoryData;
        int interactiveQueueNumber = NodeConsciousScheduler.interactiveQueueNumber;

        System.out.println("Opening job file at: " + DATASET_DIRECTORY + "/" + fname);
        BufferedReader br = null;
        Input in = new Input();
        br = in.openFile(new File(DATASET_DIRECTORY + "/" + fname));
        
        String[] values = null;
        String line = "";
        
        
        int canNotExecuteDueCpuTime = 0;
        int notSpecifiedActural = 0;
        int zeroCpuCnt = 0;
        int canNotExecuteDueMemory = 0;
        int canNotExecuteDueMemoryUpperLimit = 0;
        int memoryUnspecifiedCnt = 0;
        int canNotExecuteCountDueCpuResources = 0;
        int coreZeroCnt = 0;
        int addCheckSkip = 0;


        int firstJobSubmitTime = UNUPDATED;
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
            int userId = Integer.parseInt(values[11]);
            int matchingGroup = UNSPECIFIED;
            if (considerJobMatching && NodeConsciousScheduler.matchingGroup.containsKey(userId)) {
                matchingGroup  = NodeConsciousScheduler.matchingGroup.get((Integer) userId);
            }
            int groupId = Integer.parseInt(values[12]);
            int requiredMemory = Integer.parseInt(values[9]);
            int actualMemory = Integer.parseInt(values[6]);
            // required memory is unspecified
            if (requiredMemory == UNSPECIFIED) {
                requiredMemory = actualMemory;
                memoryUnspecifiedCnt++;
                // TODO: set appropriate parameter?
            }


            // required and actual memory is unspecified
            if (requiredMemory == UNSPECIFIED) {
                ++canNotExecuteDueMemory;
                if (ignoreIncompleteMemoryData) {
                    continue;
                }
                requiredMemory = 0;
                // TODO: set appropriate parameter?
            }

            // required memory is zero
            if (requiredMemory == 0) {
                ++canNotExecuteDueMemory;
                if (ignoreIncompleteMemoryData) {
                    continue;
                }
            }
            
            
            int requiredCores = 0;
            try {
                requiredCores = Integer.parseInt(values[4]);
            } catch (NumberFormatException ex) {
                System.out.println(values[0] + ": Number parsing error: " + values[4]);
                ex.printStackTrace();
                //numCPU = 1;
            }
            
            if (requiredCores == 0 || requiredCores == UNSPECIFIED) {
                coreZeroCnt++;
                continue;
            }
 
            if (NodeConsciousScheduler.memoryDataPerCore) {
                requiredMemory = requiredMemory * requiredCores;
            }
            
            int submitTime = Integer.parseInt(values[1]);
            if (firstJobSubmitTime == UNUPDATED) {
                firstJobSubmitTime = submitTime;
            }
            submitTime = submitTime - firstJobSubmitTime;
            assert submitTime >= 0;
            int actualExecuteTime = Integer.parseInt(values[3]);
            // actual time is unspecified
            if (actualExecuteTime == Constants.NOTSPECIFIED) {
                actualExecuteTime = Integer.parseInt(values[5]);
                notSpecifiedActural++;
            }
 
            // actual and required time is unspecified            
            if (actualExecuteTime == Constants.NOTSPECIFIED) {
                canNotExecuteDueCpuTime++;
                continue;
            }

            // actual time is zero            
            if (actualExecuteTime == 0) {
                zeroCpuCnt++;
                actualExecuteTime = 1;
            }
            int specifiedExecuteTime = Integer.parseInt(values[8]);
            
            // actual time is greater than required time
            if (specifiedExecuteTime < actualExecuteTime) {
                specifiedExecuteTime = actualExecuteTime;
            }

            int requiredNodes = 1;

            String properties = "";
            boolean nodeSpecifiedFlag = false;
            if (values.length > 19) {
                properties = values[20];

                String[] req_nodes = values[20].split(":");
                requiredNodes = Integer.parseInt(req_nodes[0]);
                nodeSpecifiedFlag = true;
            }
            // if required node is specified
            if (nodeSpecifiedFlag && requiredNodes > NodeConsciousScheduler.numNodes) {
                ++canNotExecuteCountDueCpuResources;
                continue;
            }

            if (!nodeSpecifiedFlag) {
/*
                // Can execute single node
                if (requiredCores <= NodeConsciousScheduler.numCores) {
                    requiredNodes = 1;
                } else if (requiredCores > NodeConsciousScheduler.numCores) {
                    // Can execute multiple nodes using full core
                    if (requiredCores % NodeConsciousScheduler.numCores == 0) {
                        requiredNodes = requiredCores/NodeConsciousScheduler.numCores;
                    // Check can execute multiple nodes
                    } else {                    
                        if (requiredCores % 2 == 1)
                            ++requiredCores;
                        int nodeNum = checkCanExecuteMultipleNodes(requiredCores, requiredMemory, scheduleUsingMemory);

                        if (nodeNum >= 2) {
                            requiredNodes = nodeNum;
                        } else {
                            ++canNotExecuteCountDueCpuResources;
                        }
                    }
                }
*/
                int nodeNum = checkCanExecuteMultipleNodes(requiredCores, requiredMemory, scheduleUsingMemory);

                if (nodeNum >= 1) {
                    requiredNodes = nodeNum;
                } else {
                    ++canNotExecuteCountDueCpuResources;
                    checkCanExecuteMultipleNodes(requiredCores, requiredMemory, scheduleUsingMemory);
                }
            }
            /*
            if (!nodeSpecifiedFlag && requiredCores%NodeConsciousScheduler.numCores == 0) {
                requiredNodes = requiredCores/NodeConsciousScheduler.numCores;
            }
            */
            
            int ppn = requiredCores/requiredNodes;
            if (requiredCores%requiredNodes != 0) ppn++;

            if (requiredNodes > NodeConsciousScheduler.numNodes) {
                ++canNotExecuteCountDueCpuResources;
                continue;
            }
            
            if (requiredCores > requiredNodes * NodeConsciousScheduler.numCores) {
                ++canNotExecuteCountDueCpuResources;
                continue;
            }            
            
            // TODO
            // Decide the accurate num of node for non-specified data
                        
            if (!NodeConsciousScheduler.memoryDataPerNode) {
                requiredMemory /= requiredNodes;
            }
            
            if (requiredMemory > NodeConsciousScheduler.memory) {
                ++canNotExecuteDueMemoryUpperLimit;
                if (scheduleUsingMemory) {
                    continue;
                }
            }
            
            int queueNum = Integer.parseInt(values[14]);
            
            boolean interactiveJob = false;                
            int interactiveExecuteTime = -1;
            ArrayList<Integer> activationTimes = new ArrayList();
            ArrayList<Integer> idleTimes = new ArrayList();
            int prologTime = -1;
            int epilogTime = -1;
            if (accurateInteractiveJobs && (queueNum == NodeConsciousScheduler.interactiveQueueNumber) ) {
                 InteractiveJobInfoPack ij = simpleInteractiveModel(jobId, actualExecuteTime, interactiveCPURatio);
                 interactiveJob = ij.isInteractiveJob();
                 interactiveExecuteTime = ij.getInteractiveExecuteTime();
                 activationTimes = ij.getActivationTimes();
                 prologTime = ij.getPrologTime();
                 epilogTime = ij.getEpilogTime();
                 idleTimes = ij.getIdleTimes();
                /* next job initialization */
            }

            boolean addFlag = checkJobProperty(submitTime, actualExecuteTime, specifiedExecuteTime, requiredNodes, ppn, scheduleUsingMemory, requiredMemory);
            
            if (!addFlag) {
                addCheckSkip++;
                continue;
            }
            
            Job job = new Job(jobId, submitTime, actualExecuteTime, specifiedExecuteTime, 
                            requiredCores, requiredNodes, userId, groupId, requiredMemory, 
                            matchingGroup, queueNum,
                            accurateInteractiveJobs, 
                            interactiveJob, interactiveExecuteTime, activationTimes, 
                            prologTime, epilogTime, idleTimes);
            jobList.add(job);
            
        }
        return jobList;
    }

    private static SimulatorConfiguration readSimulatorConfiguration(String fname) throws IOException {
        String dir = "./" + fname;
        Path p = Paths.get(dir); 
        Properties configurations = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(dir);
            configurations.load(in);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(NodeConsciousScheduler.class.getName()).log(Level.SEVERE, null, ex);
        }

        String slowdownThreshold = configurations.getProperty("SLOWDOWN_THRESHOLD");
        String[] slowdownThresholds = slowdownThreshold.replace("\"","").split(",");
        boolean outputMinuteTimeseries = Boolean.parseBoolean(configurations.getProperty("OUTPUT_MINUTE_TIMESERIES"));
        boolean scheduleUsingMemory = Boolean.parseBoolean(configurations.getProperty("SCHEDULE_USING_MEMORY"));
        boolean crammingMemoryScheduling = Boolean.parseBoolean(configurations.getProperty("CRAMMING_MEMORY_SCHEDULING"));
        NodeConsciousScheduler.ignoreIncompleteMemoryData = Boolean.parseBoolean(configurations.getProperty("IGNORE_INCOMPLETE_MEMORY_DATA"));
        NodeConsciousScheduler.memoryDataPerCore = Boolean.parseBoolean(configurations.getProperty("MEMORY_DATA_PER_CORE"));
        NodeConsciousScheduler.outputResultsInDetail = Boolean.parseBoolean(configurations.getProperty("OUTPUT_RESULTS_IN_DETAIL"));
        NodeConsciousScheduler.memoryDataPerNode = Boolean.parseBoolean(configurations.getProperty("MEMORY_DATA_PER_NODE"));
        NodeConsciousScheduler.considerMemoryForNodeNum = Boolean.parseBoolean(configurations.getProperty("CONSIDER_MEMORY_FOR_JOB_NODENUM"));
        boolean considerJobMatching = Boolean.parseBoolean(configurations.getProperty("CONSIDER_JOB_MATCHING"));
        boolean usingAffinityForSchedule = Boolean.parseBoolean(configurations.getProperty("SCHEDULE_USING_AFFINITY"));
        double thresholdForAffinitySchedule = Double.parseDouble(configurations.getProperty("QUIT_SCHEDULE_THRESHOLD"));
        NodeConsciousScheduler.interactiveQueueNumber = Integer.parseInt(configurations.getProperty("INTERACTIVE_QUEUE_NUMBER"));
        boolean accurateInteractiveJobs = Boolean.parseBoolean(configurations.getProperty("ACCURATE_INTERACTIVE_JOBS"));
        double interacitiveCPURatio = Double.parseDouble(configurations.getProperty("INTERACTIVE_CPU_RATIO"));
        
        return new SimulatorConfiguration(slowdownThresholds, 
                outputMinuteTimeseries, 
                scheduleUsingMemory, 
                crammingMemoryScheduling, 
                considerJobMatching, 
                usingAffinityForSchedule, 
                thresholdForAffinitySchedule, 
                accurateInteractiveJobs, 
                interacitiveCPURatio);
    }

    private static int checkCanExecuteMultipleNodes(int requiredCores, int requiredMemeory, boolean scheduleUsingMemory) {
        int requiredNode = NodeConsciousScheduler.numNodes + 1;
        
        if ((!scheduleUsingMemory && !considerMemoryForNodeNum)){
            for (int i = 1; i <= NodeConsciousScheduler.numNodes; ++i) {
                int wRequiredCores = (requiredCores + i - 1) / i;
                if (wRequiredCores <= NodeConsciousScheduler.numCores) {
                    requiredNode = min(requiredNode, i);
                    if (requiredCores % i == 0) {
                        requiredNode = i;
                        break;
                    }
                }
            }
        } else {
            for (int i = 1; i <= NodeConsciousScheduler.numNodes; ++i) {
                int wRequiredCores = (requiredCores + i - 1) / i;
                int wRequiredMemoryPerNode = (requiredMemeory)/i;
                if (wRequiredMemoryPerNode > NodeConsciousScheduler.memory) {
                    continue;
                }
                if (wRequiredCores <= NodeConsciousScheduler.numCores) {
                    requiredNode = min(requiredNode, i);
                    if (requiredCores % i == 0) {
                        requiredNode = i;
                        break;
                    }
                }
            }            
        }
        return requiredNode;
    }

    private static boolean checkJobProperty(int submitTime, int actualExecuteTime, int specifiedExecuteTime, int requiredNodes, int ppn, boolean scheduleUsingMemory, int requiredMemory) {
        boolean submitTimeFlag = (submitTime >= 0) && (submitTime < (2 << 29));
        boolean actualFlag = (actualExecuteTime >= 0) && (specifiedExecuteTime >= 0) && (actualExecuteTime <= specifiedExecuteTime);
        boolean cpuRscFlag = (requiredNodes <= NodeConsciousScheduler.numNodes) && (ppn <= NodeConsciousScheduler.numCores) && (requiredNodes * ppn <= NodeConsciousScheduler.numCores * NodeConsciousScheduler.numNodes); 
        boolean memoryFlag = false;
        if (!scheduleUsingMemory) {
            memoryFlag = true;
        } else {
            memoryFlag = requiredMemory <= NodeConsciousScheduler.memory;
        }
                
        return submitTimeFlag && actualFlag && cpuRscFlag && memoryFlag;

    }

    private static Map<Integer, Integer> readJobMatchingGroupTable(String fname) throws IOException {
        String fnameMatchingGroupTable = fname + ".matching_group";
        System.out.println("Opening matching table file at: " + DATASET_DIRECTORY + "/" + fnameMatchingGroupTable);
        BufferedReader br = null;
        Input in = new Input();
        br = in.openFile(new File(DATASET_DIRECTORY + "/" + fnameMatchingGroupTable));
        
        String[] values = null;
        String line = "";
        
       Map<Integer, Integer> matchingGroupTable = new HashMap<Integer, Integer>();
        while ((line = br.readLine()) != null) {
            values = line.split("\\s+");
            for (int i = 1; i < values.length; ++i) {
                matchingGroupTable.put(Integer.parseInt(values[i]), Integer.parseInt(values[0]));
            }

        }
        return matchingGroupTable;
    }

    private static Map<JobMatching, Double> readJobMatchingTable(String fname) throws IOException {
        String fnameMatchingTable = fname + ".matching_table";
        System.out.println("Opening matching table file at: " + DATASET_DIRECTORY + "/" + fnameMatchingTable);
        BufferedReader br = null;
        Input in = new Input();
        br = in.openFile(new File(DATASET_DIRECTORY + "/" + fnameMatchingTable));
        
        String[] values = null;
        String line = "";
        
       Map<JobMatching, Double> matchingTable = new HashMap<>();
        while ((line = br.readLine()) != null) {
            values = line.split("\\s+");
            int victimJobId = Integer.parseInt(values[0]);
            int opponentJobId = Integer.parseInt(values[1]);
            double ratio = Double.parseDouble(values[2]);
            matchingTable.put(new JobMatching(victimJobId, opponentJobId), ratio);
        }
        return matchingTable;
        
    }
        
    private static InteractiveJobInfoPack simpleInteractiveModel(int jobId, int actualExecuteTime, double interactiveCPURatio) {
        int interactiveExecuteTime = (int) Math.ceil(actualExecuteTime * interactiveCPURatio);

        /* 0.2, 0.1, 0.1 are hard-coded following multiply. */
        int executionTimePerActivate = (int) Math.ceil(interactiveExecuteTime * 0.2);
        int prologTime = (int) Math.ceil(actualExecuteTime * 0.1);
        int epilogTime = (int) Math.ceil(actualExecuteTime * 0.1);

        if (actualExecuteTime <= prologTime + epilogTime) {
            prologTime = 0;
            epilogTime = 0;
        }

        int numOfTimesToActivate = (int) ((interactiveExecuteTime + executionTimePerActivate - 1) / executionTimePerActivate);
        int numOfTimesBetweenActivate = numOfTimesToActivate - 1;
        int idleTimeBetweenActivate = -1;
        if (numOfTimesBetweenActivate != 0) {
            idleTimeBetweenActivate = (actualExecuteTime - prologTime - epilogTime - interactiveExecuteTime + numOfTimesBetweenActivate - 1) / numOfTimesBetweenActivate;
        } else {
            idleTimeBetweenActivate = 0;
        }

        int sumTime = prologTime + epilogTime + interactiveExecuteTime + idleTimeBetweenActivate * numOfTimesBetweenActivate;

        /* If interactiveCPURatio >= about 0.5, a short-running job may be caught here. */
        if (sumTime > actualExecuteTime) {
            System.out.printf("JobId:%d\tacutualTime:%d\tintExecTime:%d\tExecTimePerAct:%d"
                    + "\tprologTime:%d\tepilogTime:%d\tidleTimeBtAct:%d\tsumTime:%d\n",
                    jobId, actualExecuteTime, interactiveExecuteTime, executionTimePerActivate,
                    prologTime, epilogTime, idleTimeBetweenActivate, sumTime);
        }

        System.out.printf("JobId:%d\tacutualTime:%d\tintExecTime:%d"
                + "\tExecTimePerAct:%d\tnumActivate:%d"
                + "\tprologTime:%d\tepilogTime:%d\tidleTimeBtAct:%d\tsumTime:%d\tDiff:%d\n",
                jobId, actualExecuteTime, interactiveExecuteTime,
                executionTimePerActivate, numOfTimesToActivate,
                prologTime, epilogTime, idleTimeBetweenActivate, sumTime,
                actualExecuteTime - sumTime);

        /* packing */
        ArrayList<Integer> activationTimes = new ArrayList();
        ArrayList<Integer> idleTimes = new ArrayList();
        
        int restTime = interactiveExecuteTime;
        int totalIdleTime = actualExecuteTime - (prologTime + epilogTime + interactiveExecuteTime);
        for(int i = 0; i < numOfTimesToActivate; ++i) {
            int wExecutionTimePerActivate = executionTimePerActivate;
            int wIdleTimeBetweenActivate = idleTimeBetweenActivate;
            if (interactiveExecuteTime % numOfTimesToActivate != 0 && i >= interactiveExecuteTime % numOfTimesToActivate) {
                --wExecutionTimePerActivate;
            }
            if (totalIdleTime % numOfTimesBetweenActivate != 0 && i >= totalIdleTime % numOfTimesBetweenActivate) {
                --wIdleTimeBetweenActivate;
            }
            
//            activationTimes.add((Integer) min(restTime, executionTimePerActivate));
            activationTimes.add((Integer) wExecutionTimePerActivate);
//            restTime -= executionTimePerActivate;
            if (i == numOfTimesToActivate-1) break;
//            idleTimes.add((Integer) min(restIdleTime, idleTimeBetweenActivate));
            idleTimes.add((Integer) wIdleTimeBetweenActivate);
//            restIdleTime -= idleTimeBetweenActivate;
        }
        assert activationTimes.size() == idleTimes.size() + 1;
        
        return new InteractiveJobInfoPack(true, interactiveExecuteTime, activationTimes, prologTime, epilogTime, idleTimes);
    }
}

class InteractiveJobInfoPack {

    boolean interactiveJob; 
    int interactiveExecuteTime;
    ArrayList<Integer> activationTimes;
    int prologTime;
    int epilogTime;
    ArrayList<Integer> idleTimes;

    public InteractiveJobInfoPack(boolean interactiveJob, int interactiveExecuteTime, ArrayList<Integer> activationTimes, int prologTime, int epilogTime, ArrayList<Integer> idleTimes) {
        this.interactiveJob = interactiveJob;
        this.interactiveExecuteTime = interactiveExecuteTime;
        this.activationTimes = activationTimes;
        this.prologTime = prologTime;
        this.epilogTime = epilogTime;
        this.idleTimes = idleTimes;
    }

    public boolean isInteractiveJob() {
        return interactiveJob;
    }

    public int getInteractiveExecuteTime() {
        return interactiveExecuteTime;
    }

    public ArrayList<Integer> getActivationTimes() {
        return activationTimes;
    }

    public int getPrologTime() {
        return prologTime;
    }

    public int getEpilogTime() {
        return epilogTime;
    }

    public ArrayList<Integer> getIdleTimes() {
        return idleTimes;
    }

    
    
}


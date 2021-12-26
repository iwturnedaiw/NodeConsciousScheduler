/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import static java.lang.StrictMath.min;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import static nodeconsciousscheduler.Constants.UNUPDATED;

/**
 *
 * @author sminami
 */
class FCFS extends Scheduler {
    FCFS() {
        init();
    }

    @Override
    protected ArrayList<Event> scheduleJobsStartAt(int currentTime) {
        /* 1. Obtain the head job in the queue */ 
        /* 2. Obtain the nodes the job can execute at */
        /* 3. Select nodes the job is assigned to */        
        /* 4. Modify the timeSlices */        
        /* 5. Modify the resource informaiton */        
        /* 6. Enqueue the START and END Events */                
        
        ArrayList<Event> result = new ArrayList<Event>();
        temporallyScheduledJobList.clear();
        while (!waitingQueue.isEmpty()) {
            /* 1. Obtain the head job in the queue */
            Job job = waitingQueue.peek();

            /* 2. Obtain the nodes the job can execute at */
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesImmediately(currentTime, job);
            TimeSlicesAndNodeInfoConsistency consistency = checkTimeSlicesAndAllNodeInfo(currentTime);
            assert consistency.isConsistency();
            if (consistency.isSameEndEventFlag()) return result;
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {
                /* 3. Select nodes the job is assigned to */
                Collections.sort(canExecuteNodes);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                for (int i = 0; i < job.getRequiredNodes(); ++i) {
                    assignNodesNo.add(canExecuteNodes.get(i).getNodeNo());
                }

                waitingQueue.poll();
                
                /* 4. Modify the timeSlices */
                int startTime = currentTime;
                job.setStartTime(startTime);
                makeTimeslices(startTime);
                
                int expectedEndTime = startTime + job.getRequiredTime();
                makeTimeslices(expectedEndTime);
                job.setOccupiedTimeInTimeSlices(expectedEndTime);

                /* 5. Modify the resource informaiton */
                assignJob(startTime, job, assignNodesNo);

                /* 6. Enqueue the START and END Events */                
                job.setPreviousMeasuredTime(startTime);
                int trueEndTime = startTime + job.getActualExecuteTime();
                result.add(new Event(EventType.START, startTime, job));
                result.add(new Event(EventType.END, trueEndTime, job));
                job.setEndEventOccuranceTimeNow(trueEndTime);
                temporallyScheduledJobList.add(job);
            } else break;
        }
        return result;
    }

    @Override
    protected ArrayList<Event> checkCoexistingJobsOCStateAndModifyENDEventAndTimeSlices(Event ev) {
        return new ArrayList<Event>();
    }
    
    protected ArrayList<VacantNode> canExecutableNodesImmediately(int currentTime, Job job) {
        /* Return variable
           This have the node no. with # of free core.
        */
        ArrayList<VacantNode> nodes = new ArrayList<VacantNode>();
        
        /* Working Variable */
        ArrayList<VacantNode> vacantNodes = new ArrayList<VacantNode>();
//        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodes.add(new VacantNode(i, NodeConsciousScheduler.numCores));
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodes.add(new VacantNode(i, NodeConsciousScheduler.numCores, NodeConsciousScheduler.memory));
        
        /* This is used for counting executable nodes */
        ArrayList<Integer> vacantNodeCount = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodeCount.add(0);

        /* Calculate ppn */
        /* TODO: The case requiredCores ist not dividable  */
        //int requiredCoresPerNode = job.getRequiredCores()/job.getRequiredNodes();
        //if (job.getRequiredCores()%job.getRequiredNodes() != 0) ++requiredCoresPerNode;
        int requiredCoresPerNode = job.getRequiredCoresPerNode();
        long requiredMemoryPerNode = job.getMaxMemory();

        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();
        
        int alongTimeSlices = 0;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            if (ts.getStartTime() <= currentTime && currentTime <= ts.getEndTime()) {
                ++alongTimeSlices;
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    int freeCores = ts.getAvailableCores().get(j);
                    long freeMemory = ts.getAvailableMemory().get(j);
                    VacantNode node = vacantNodes.get(j);
                    
                    assert node.getNodeNo() == j;

                    freeCores = min(freeCores, node.getFreeCores());
                    node.setFreeCores(freeCores);
                    
                    freeMemory = min(freeMemory, node.getFreeMemory());

                    boolean addFlag = false;
                    addFlag = (freeCores >= requiredCoresPerNode);                    
                    if (scheduleUsingMemory) {
                        addFlag &= (freeMemory >= requiredMemoryPerNode);
                    }
                    
                    if (addFlag) {
                        int cnt = vacantNodeCount.get(j);
                        vacantNodeCount.set(j, ++cnt);
                    }
                }
            }
        }

        /*
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            VacantNode node = vacantNodes.get(i);
            int freeCores = node.getFreeCores();
            node.setFreeCores(freeCores/alongTimeSlices);
        }
        */
        
        /* If cnt == alongTimeSlices, the job is executable on the nodes along the timeSlices */
        for (int i = 0; i < vacantNodeCount.size(); ++i) {
            int cnt = vacantNodeCount.get(i);
            if (cnt == alongTimeSlices) {
                VacantNode node = vacantNodes.get(i);
                assert node.getNodeNo() == i;
                nodes.add(node);
            }
        }
        
        return nodes;
    }
}

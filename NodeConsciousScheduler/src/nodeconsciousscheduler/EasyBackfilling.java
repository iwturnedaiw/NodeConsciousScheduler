/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import static nodeconsciousscheduler.Constants.CANNOT_START;
import static nodeconsciousscheduler.Constants.TS_ENDTIME;
import static nodeconsciousscheduler.Constants.UNSTARTED;
import static nodeconsciousscheduler.Constants.UNUPDATED;

/**
 *
 * @author sminami
 */
class EasyBackfilling extends Scheduler {
    EasyBackfilling() {
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
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesAt(currentTime, job);
            TimeSlicesAndNodeInfoConsistency consistency = checkTimeSlicesAndAllNodeInfo();
            assert consistency.isConsistency();
            if (consistency.isSameEndEventFlag()) return result;
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {
                // TODO: Erase below line
                System.out.println("size: " + canExecuteNodes.size() + ", FCFS job: " + job.getJobId());

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
        
        if (waitingQueue.size() <= 1) return result;
        
        /* Backfilling */

        /* 1. Assign the head job at the time it can start */ 
        /* 2. Obtain the 2nd or later job in the queue */ 
        /* 3. Select nodes the job is assigned to */        
        /* 4. Modify the timeSlices */        
        /* 5. Modify the resource informaiton */        
        /* 6. Enqueue the START and END Events */                

        Queue<Job> tailWaitingQueue = copyWaitingQueue();
        Job firstJob = waitingQueue.peek();

        /* 1. Assign the head job at the time it can start */ 
        /* TODO: break down the 1 in detail */
        int startTimeFirstJob = CANNOT_START;
        ArrayList<VacantNode> canExecuteTmpNodes = new ArrayList<VacantNode>();
        /* Find tempollary executable nodes */
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            //ts.printTsInfo();
            int tmpStartTime = ts.getStartTime();
            if (tmpStartTime < currentTime) {
                continue;
            }

            canExecuteTmpNodes = canExecutableTmpNodes(tmpStartTime, firstJob);

            if (canExecuteTmpNodes.size() >= firstJob.getRequiredNodes()) {
                startTimeFirstJob = tmpStartTime;
                break;
            }
        }

        // TODO: Erase below line
        System.out.println("size: " + canExecuteTmpNodes.size() + ", firstJob: " + firstJob.getJobId());

        assert canExecuteTmpNodes.size() >= firstJob.getRequiredNodes();
        assert startTimeFirstJob != CANNOT_START;

        /* We must handle the copy of original list */
        /* TODO?: Define the appropriate clonable setting */
        LinkedList<TimeSlice> tmpTimeSlices = cloneTimeSlices(timeSlices);
        ArrayList<NodeInfo> tmpAllNodesInfo = cloneAllNodesInfo(NodeConsciousScheduler.sim.getAllNodesInfo());

        makeTimeslices(startTimeFirstJob, tmpTimeSlices);
        int endTimeFirstJob = startTimeFirstJob + firstJob.getRequiredTime();
        makeTimeslices(endTimeFirstJob, tmpTimeSlices);
        assignFirstJobTemporally(tmpTimeSlices, tmpAllNodesInfo, startTimeFirstJob, firstJob, canExecuteTmpNodes);
        
        ArrayList<VacantNode> canExecuteNodesEasyBackfiling;
        while (tailWaitingQueue.size() > 0) {
            /* 2. Obtain the 2nd or later job in the queue */ 
            Job backfillJob = tailWaitingQueue.poll();

            /* 3. Obtain the nodes the job can execute at */            
            canExecuteNodesEasyBackfiling = canExecutableNodesOnBackfilling(currentTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, startTimeFirstJob);

            if (canExecuteNodesEasyBackfiling.size() >= backfillJob.getRequiredNodes()) {
                System.out.println("Succeed Backfill Job: " + backfillJob.getJobId() + ", at " + currentTime);

                /* 4. Select nodes the job is assigned to */        
                Collections.sort(canExecuteNodesEasyBackfiling);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                for (int i = 0; i < backfillJob.getRequiredNodes(); ++i) {
                    assignNodesNo.add(canExecuteNodesEasyBackfiling.get(i).getNodeNo());
                }

                /* Delete the job from original queue */
                Iterator itr = waitingQueue.iterator();
                while (itr.hasNext()) {
                    Job deleteJob = (Job) itr.next();
                    if (deleteJob.getJobId() == backfillJob.getJobId()) {
                        itr.remove();
                        break;
                    }
                }

                /* 4. Modify the timeSlices */        
                int startTime = currentTime;
                backfillJob.setStartTime(startTime);

                makeTimeslices(startTime);
                makeTimeslices(startTime, tmpTimeSlices);

                int expectedEndTime = startTime + backfillJob.getRequiredTime();
                makeTimeslices(expectedEndTime);
                makeTimeslices(expectedEndTime, tmpTimeSlices);
                backfillJob.setOccupiedTimeInTimeSlices(expectedEndTime);

                /* 5. Modify the resource informaiton */        
                assignJob(startTime, backfillJob, assignNodesNo);
                assignJobForTmp(startTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, assignNodesNo);

                /* 6. Enqueue the START and END Events */                                                
                backfillJob.setPreviousMeasuredTime(startTime);
                int trueEndTime = startTime + backfillJob.getActualExecuteTime();
                result.add(new Event(EventType.START, startTime, backfillJob));
                result.add(new Event(EventType.END, trueEndTime, backfillJob));
                backfillJob.setEndEventOccuranceTimeNow(trueEndTime);
                temporallyScheduledJobList.add(backfillJob);
            }
        }
        
        return result;
    }
 
    @Override
    protected ArrayList<Event> checkCoexistingJobsOCStateAndModifyENDEventAndTimeSlices(Event ev) {
        return new ArrayList<Event>();
    }
    
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, Job job) {
        return canExecutableNodesAt(currentTime, this.timeSlices, job, false, UNSTARTED);
    }
   
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, LinkedList<TimeSlice> timeSlices, Job job, boolean backfillFlag, int firstJobStartTime) {
        /* Return variable
           This have the node no. with # of free core.
        */
        ArrayList<VacantNode> nodes = new ArrayList<VacantNode>();
        
        /* Working Variable */
        ArrayList<VacantNode> vacantNodes = new ArrayList<VacantNode>();
        //for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodes.add(new VacantNode(i, NodeConsciousScheduler.numCores));
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
        
        int jobId = job.getJobId();
        int startTime = currentTime;
        int expectedEndTime = startTime + job.getRequiredTime();
        int alongTimeSlices = 0;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);

            if ( (ts.getStartTime() <= startTime && startTime < ts.getEndTime()) || 
                 (ts.getStartTime() < expectedEndTime && expectedEndTime <= ts.getEndTime()) ||
                 (startTime <= ts.getStartTime() && ts.getEndTime() <= expectedEndTime) ) {
                //ts.printTsInfo();
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
                    
                    if (addFlag ) {
                        int cnt = vacantNodeCount.get(j);
                        vacantNodeCount.set(j, ++cnt);
                    }
                }
            }
        }

        if (alongTimeSlices == 0) return nodes;

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
    
    protected Queue<Job> copyWaitingQueue() {
        Queue<Job> tailWaitingQueue = new LinkedList<Job>();
        
        Iterator<Job> it = waitingQueue.iterator();
        while(it.hasNext())  {
            tailWaitingQueue.add(it.next());
        }
        
        return tailWaitingQueue;
    }

    protected void assignFirstJobTemporally(LinkedList<TimeSlice> tmpTimeSlices, ArrayList<NodeInfo> tmpAllNodesInfo, int startTime, Job firstJob, ArrayList<VacantNode> canExecuteTmpNodes) {
        int addedPpn = firstJob.getRequiredCores()/firstJob.getRequiredNodes();
        int expectedEndTime = startTime + firstJob.getRequiredTime();
        long addedMpn = firstJob.getMaxMemory();
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();

        
        /* TODO: The case requiredCores ist not dividable  */
        if (firstJob.getRequiredCores()%firstJob.getRequiredNodes() != 0) {
            ++addedPpn;
            System.out.println("Not dividable, Job ID = " + firstJob.getJobId());
        }

        Collections.sort(canExecuteTmpNodes);
        ArrayList<Integer> tmpAssignNodesNo = new ArrayList<Integer>();
        for (int i = 0; i < firstJob.getRequiredNodes(); ++i) {
            tmpAssignNodesNo.add(canExecuteTmpNodes.get(i).getNodeNo());
        }
        
        for (int i = 0; i < tmpTimeSlices.size(); ++i) {
            TimeSlice ts = tmpTimeSlices.get(i);
//            ts.printTsInfo();
            if (startTime < ts.getEndTime() && ts.getStartTime() < expectedEndTime) {
                ArrayList<Integer> cores = ts.getAvailableCores();
                ArrayList<Long> memories = ts.getAvailableMemory();
                for (int j = 0; j < tmpAssignNodesNo.size(); ++j) {
                    int nodeNo = tmpAssignNodesNo.get(j);
                    int core = cores.get(nodeNo);
                    core -= addedPpn;
                    cores.set(nodeNo, core);
                    
                    if (scheduleUsingMemory) {
                        long memory = memories.get(nodeNo);
                        memory -= addedMpn;
                        assert memory >= 0;
                        memories.set(nodeNo, memory);
                    }
                }
            }
//            ts.printTsInfo();
        }
        
    }    
    
    protected ArrayList<VacantNode> canExecutableNodesOnBackfilling(int currentTime, LinkedList<TimeSlice> tmpTimeSlices, ArrayList<NodeInfo> tmpAllNodesInfo, Job backfillJob, int firstJobStartTime) {
        ArrayList<VacantNode> canExecuteNodesEasyBackfiling = new ArrayList<VacantNode>();
        
        canExecuteNodesEasyBackfiling = canExecutableNodesAt(currentTime, tmpTimeSlices, backfillJob, true, firstJobStartTime);
        
        return canExecuteNodesEasyBackfiling;

    }
    
    protected ArrayList<VacantNode> canExecutableTmpNodes(int startTimeFirstJob, Job firstJob) {
        return canExecutableNodesAt(startTimeFirstJob, firstJob);
    }

    protected LinkedList<TimeSlice> cloneTimeSlices(LinkedList<TimeSlice> timeSlices) {
        LinkedList<TimeSlice> tmpTimeSlices = new LinkedList<TimeSlice>();
        
        int size = timeSlices.size();
        for (int i = 0; i < size; ++i) {
            TimeSlice ts = timeSlices.get(i);
            TimeSlice copiedTs = ts.clone();
            tmpTimeSlices.add(copiedTs);
        }
        assert tmpTimeSlices.size() == timeSlices.size();
        return tmpTimeSlices;
    }

    protected ArrayList<NodeInfo> cloneAllNodesInfo(ArrayList<NodeInfo> allNodesInfo) {
        ArrayList<NodeInfo> tmpAllNodesInfo = new ArrayList<NodeInfo>();
        
        int size = allNodesInfo.size();
        for (int i = 0; i < size; ++i) {
            NodeInfo nodeInfo = allNodesInfo.get(i);
            NodeInfo copiedNodeInfo = cloneNodeInfo(nodeInfo);
            tmpAllNodesInfo.add(copiedNodeInfo);
        }
        assert tmpAllNodesInfo.size() == allNodesInfo.size();
        return tmpAllNodesInfo;
        
    }

    protected void assignJobForTmp(int startTime, LinkedList<TimeSlice> tmpTimeSlices, ArrayList<NodeInfo> tmpAllNodesInfo, Job backfillJob, ArrayList<Integer> assignNodesNo) {
        assignJob(startTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, assignNodesNo, true);
    }

    private NodeInfo cloneNodeInfo(NodeInfo nodeInfo) {
        NodeInfo copiedNodeInfo = nodeInfo.clone();

        /* Set the occupiedCores*/
        ArrayList<CoreInfo> orgOccupiedCores = nodeInfo.getOccupiedCores();
        ArrayList<CoreInfo> occupiedCores = (ArrayList<CoreInfo>) orgOccupiedCores.clone();
        occupiedCores.clear();
        copiedNodeInfo.setOccupiedCores(occupiedCores);
        
        /* Set the element of occupiedCores */
        int numCore = orgOccupiedCores.size();        
        assert occupiedCores.size() == 0;
        for (int j = 0; j < numCore; ++j) {
            CoreInfo eachCore = (CoreInfo) orgOccupiedCores.get(j).clone();
            occupiedCores.add(eachCore);
        }
        assert occupiedCores.size() == numCore;
        
        return copiedNodeInfo;
    }


}

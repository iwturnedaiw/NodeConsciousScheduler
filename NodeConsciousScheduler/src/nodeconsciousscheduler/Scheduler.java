/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import static java.lang.Integer.min;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.NodeChangeEvent;
import static nodeconsciousscheduler.Constants.BLANK_JOBID;
import static nodeconsciousscheduler.Constants.NOTACTIVATED;
import static nodeconsciousscheduler.Constants.NOT_FINISHED;
import static nodeconsciousscheduler.Constants.START_COUNT;
import nodeconsciousscheduler.Constants.ScheduleConsiderJobType;
import static nodeconsciousscheduler.Constants.UNSPECIFIED;
import static nodeconsciousscheduler.Constants.UNSTARTED;
import static nodeconsciousscheduler.Constants.UNUPDATED;
import static nodeconsciousscheduler.Constants.UNUSED;

/**
 *
 * @author sminami
 */
public abstract class Scheduler {
    protected Queue<Job> waitingQueue;
    protected LinkedList<TimeSlice> timeSlices;
    protected LinkedList<TimeSlice> completedTimeSlices;
    ArrayList<Job> temporallyScheduledJobList;
    protected List<Double> wastedResources;
    protected int previousCalcTime = 0;
    
    abstract protected ArrayList<Event> scheduleJobsStartAt(int currentTime);
    abstract protected ArrayList<Event> checkCoexistingJobsOCStateAndModifyENDEventAndTimeSlices(Event ev);
    
    protected void enqueue(Event ev) {
        waitingQueue.add(ev.getJob());
    }
    
    protected void init() {
        this.waitingQueue = new LinkedList<Job>();
        this.timeSlices = new LinkedList<TimeSlice>();
        this.timeSlices.add(new TimeSlice());
        this.completedTimeSlices = new LinkedList<TimeSlice>();    
        this.temporallyScheduledJobList = new ArrayList<Job>();
        this.wastedResources = new ArrayList<Double>();
        int numNodes = NodeConsciousScheduler.numNodes;
        for (int i = 0; i < numNodes; ++i) {
            this.wastedResources.add((double)0.0);
        }
    }
    
    protected boolean existSliceStartAt(int currentTime, LinkedList<TimeSlice> timeSlices) {
        for (int i = 0; i < timeSlices.size(); ++i) {
            if (timeSlices.get(i).getStartTime() == currentTime) {
                return true;
            }
        }
        return false;
    }
    
    protected int sliceIndexToSplit(int currentTime, LinkedList<TimeSlice> timeSlices) {
        int breakIndex = -1;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            int startTime = ts.getStartTime();
            int endTime = ts.getEndTime();
            if (startTime < currentTime && currentTime < endTime) {
                breakIndex = i;
                break;
            }
        }
        return breakIndex;
    }
    
    protected void makeTimeslices(int currentTime) {
        makeTimeslices(currentTime, this.timeSlices, true);
    }
    
    protected void makeTimeslices(int currentTime, LinkedList<TimeSlice> timeSlices, boolean uniteFlag) {        
        if (existSliceStartAt(currentTime, timeSlices))
            return;

        if (uniteFlag) {
            uniteTimeSlices(timeSlices);
        }
        
        int breakIndex = UNUPDATED;
        breakIndex = sliceIndexToSplit(currentTime, timeSlices);

        if (breakIndex != UNUPDATED) {
            TimeSlice ts = timeSlices.get(breakIndex);
            LinkedList<TimeSlice> brokenSlices = ts.split(currentTime);
            timeSlices.remove(breakIndex);
            timeSlices.add(breakIndex, brokenSlices.get(0));
            timeSlices.add(breakIndex + 1, brokenSlices.get(1));
            //System.out.println(brokenSlices.get(0).getAvailableCores());
            //System.out.println(brokenSlices.get(1).getAvailableCores());
        } else {
            System.out.println("Cannot break the timeslices at " + currentTime);
        }
        
        return;
    }
    
    protected void calcWastedResource(int currentTime) {
        int previousCalcTime = getPreviousCalcTime();
        
        assert previousCalcTime <= currentTime;
        if (currentTime == previousCalcTime) {
            return;
        }
        
        int numNodes = NodeConsciousScheduler.numNodes;
        for (TimeSlice ts: timeSlices) {
            double wastedRatioForDebug = 0.0;
            
            int startTime = ts.getStartTime();
            int endTime = ts.getEndTime();
            int duration = ts.getDuration();
            
            if (currentTime <= startTime) {
                break;
            }
            
            if (endTime < previousCalcTime ) {
                continue;
            }
            
            assert startTime <= currentTime;
            
            int leftEnd = max(startTime, previousCalcTime);
            int rightEnd = min(currentTime, endTime);
            
            int durationBtPreviousCalc = rightEnd - leftEnd;
            

            for (int nodeId = 0; nodeId < numNodes; ++nodeId) {
                double wastedResource = wastedResources.get(nodeId);
                double currentWasteResourceRatio = calcInstantWasteRatioOnNode(nodeId);
                double addedWastedResource = currentWasteResourceRatio * durationBtPreviousCalc;
                wastedResource += addedWastedResource;
                wastedResources.set(nodeId, wastedResource);
                wastedRatioForDebug += currentWasteResourceRatio;
            }
        }
        setPreviousCalcTime(currentTime);
        
        //NodeConsciousScheduler.sim.flushCurrentWastedResourceToFile(currentTime, wastedResources);
        
        return;
    }
    
    protected void completeOldSlices(int currentTime) {
        int size = timeSlices.size();
        
        while (size > 0) {
            TimeSlice ts = timeSlices.peek();
            if (ts.getEndTime() <= currentTime) {
                completedTimeSlices.add(ts);
                timeSlices.poll();
                --size;
            } else break;
        }
        return;
    }   
    
    protected void reduceTimeslices(int currentTime, Event ev) {

        Job job = ev.getJob();
        int expectedEndTime = job.getOccupiedTimeInTimeSlices();
        
        for (int i = 0; i < timeSlices.size(); ++i) {            
            TimeSlice ts = timeSlices.get(i);        
//            ts.printTsInfo();
            int endTime = ts.getEndTime();
            
            if (endTime <= currentTime) continue;
            
            if (endTime <= expectedEndTime) {                
                ts.refillResources(job);
            }
        }
    }
    
    private double calcInstantWasteRatioOnNode(int nodeId) {
        double wastedResourceRatioOnNode = 0.0; //retValue;
        NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeId);
        Map<Integer, Job> jobMap = NodeConsciousScheduler.sim.getJobMap();        
        int numCore = NodeConsciousScheduler.numCores;
        List<CoreInfo> coreInfos = nodeInfo.getOccupiedCores();        
        for (CoreInfo ci: coreInfos) {
            int coreId = ci.getCoreId();
            double wastedResourceRatioOnCore = 0.0;
            double utilizationRatioOnCore = 0.0;
            List<Integer> jobList = ci.getJobList();
            int numDeactiveJob = 0;
            for (Integer jobId: jobList) {
                Job job = jobMap.get(jobId);
                int netOCStateLevel = job.getNetOCStateLevel();
                boolean interactiveJob = job.isInteracitveJob();
                if (!interactiveJob) {
                    utilizationRatioOnCore += 1.0/netOCStateLevel;
                } else {
                    boolean actState = job.isActivationState();
                    if (actState) {
                        utilizationRatioOnCore += 1.0/netOCStateLevel;
                    } else {
                        ++numDeactiveJob;
                    }
                }
            }
            if (utilizationRatioOnCore < 1.0) {
                wastedResourceRatioOnCore = 1.0 - utilizationRatioOnCore;
            }
            if (numDeactiveJob == jobList.size()) {
                wastedResourceRatioOnCore = 0.0;                
            }
            
            wastedResourceRatioOnNode += wastedResourceRatioOnCore;
        }
        wastedResourceRatioOnNode /= numCore;
        return wastedResourceRatioOnNode;
    }

    protected Set<Integer> searchVictimJobs(int startTime, Job job, ArrayList<Integer> assignNodesNo) {
        return searchVictimJobs(startTime, NodeConsciousScheduler.sim.getAllNodesInfo(), job, assignNodesNo);
    }
    
    /* Search victim jobs for the job */
    protected Set<Integer> searchVictimJobs(int startTime, ArrayList<NodeInfo> allNodesInfo, Job job, ArrayList<Integer> assignNodesNo) {
        Set<Integer> victimJobs = new HashSet<Integer>();
        
        int jobId = job.getJobId();
        int addedPpn = job.getRequiredCores()/job.getRequiredNodes();
        /* TODO: The case requiredCores ist not dividable  */
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) {
            ++addedPpn;
            System.out.println("Not dividable, Job ID = " + job.getJobId());
        }
        
        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
           
            NodeInfo node = allNodesInfo.get(nodeNo);
            int numCores = node.getNumCores();
            
            int coreCnt = addedPpn;
            ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();
            if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {
                calculatePriorityForCores(occupiedCores, job);
            }
            ScheduleConsiderJobType scjt = NodeConsciousScheduler.sim.getScheduleConsiderJobType();
            if (scjt == ScheduleConsiderJobType.BATCH_INT) {
                boolean intJob = job.isInteracitveJob();
                calculatePriorityForCores(occupiedCores, job, scjt, intJob);
            }
            Collections.sort(occupiedCores);
            for (int j = 0; j < numCores; ++j) {
                CoreInfo eachCore = occupiedCores.get(j);
                ArrayList<Integer> jobList = eachCore.getJobList();
                /*
                if (jobList.size() == 0) {
                    jobList.add(jobId);                    
                    --coreCnt;                    
                }
                */

                /* Add the victim's jobId */
                for (int k = 0; k < jobList.size(); ++k) {
                    int victimJobId = jobList.get(k);
                    assert victimJobId != jobId;
                    victimJobs.add(victimJobId);
                }
                --coreCnt;
                if (coreCnt == 0) break;
            }
            assert coreCnt == 0;
        }
        return victimJobs;
    }
    
    
    protected TimeSlicesAndNodeInfoConsistency checkTimeSlicesAndAllNodeInfo(int currentTime) {
        /* For TimeSlices */
        ArrayList<Integer> freeCoreInTimeSlices = new ArrayList<Integer>();
        TimeSlice ts = timeSlices.get(0);
        assert currentTime == ts.getStartTime();
        for (int i = 0; i < ts.getNumNode(); ++i) {
            int freeCore = ts.getAvailableCores().get(i);
            freeCoreInTimeSlices.add(freeCore);
        }

        /* For AllNodeInfoList */
        ArrayList<Integer> freeCoreInAllNodeInfo = new ArrayList<Integer>();
        ArrayList<NodeInfo> allNodeInfoList = NodeConsciousScheduler.sim.getAllNodesInfo();

        for (int i = 0; i < allNodeInfoList.size(); ++i) {
            NodeInfo nodeInfo = allNodeInfoList.get(i);
            int freeCore = nodeInfo.getNumFreeCores();
            freeCoreInAllNodeInfo.add(freeCore);
        }

        assert freeCoreInTimeSlices.size() == freeCoreInAllNodeInfo.size();

        boolean ret1 = true;
        boolean ret2 = false;
        
        Set<Job> modifiedJobInTimeSlices = new HashSet<Job>();
        for (int i = 0; i < freeCoreInAllNodeInfo.size(); ++i) {
            int freeCoreInTimeSlice = freeCoreInTimeSlices.get(i);
            int freeCoreInNodeInfo = freeCoreInAllNodeInfo.get(i);
            if (freeCoreInTimeSlice == freeCoreInNodeInfo) continue;
            if (freeCoreInTimeSlice > freeCoreInNodeInfo) {
                System.out.println("Differ freecore value at node " + i);                
                System.out.println("Check wheter multiple END Event with the same event time exist.");
                int endEventCnt = 0;
/*
                ArrayList<Job> completedJobList = NodeConsciousScheduler.sim.getCompletedJobList();
                int sizeOfCompletedJobList = completedJobList.size();
                if (sizeOfCompletedJobList != 0) {
                    for (int k = sizeOfCompletedJobList - 1; k >= 0; --k) {
                        Job lastCompletedJob = completedJobList.get(k);
                        int endEventOccuranceTimeNowCompleted = lastCompletedJob.getEndEventOccuranceTimeNow();
                        if (endEventOccuranceTimeNowCompleted < currentTime) break;
                        if (lastCompletedJob.getUsingNodesList().contains((Integer)i) && endEventOccuranceTimeNowCompleted == currentTime) {
                            endEventCnt++;
                        }                    
                    }
                }
                */
                boolean existMultipleEventSameTime = false;
                ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
                for (int k = 0; k < executingJobList.size(); ++k) {
                    Job job = executingJobList.get(k);
                    
                    int endEventOccuranceTimeNow = job.getEndEventOccuranceTimeNow();
                    int occupiedTimeInTimeSeries = job.getOccupiedTimeInTimeSlices();
                    boolean containsNodeId = false;
                    for (UsingNode un: job.getUsingNodesList()) {
                        int nid = un.getNodeNum();
                        if (nid == i) {
                            containsNodeId = true;
                            break;
                        }
                    }
                    if (!containsNodeId) continue;
                    if (endEventOccuranceTimeNow == currentTime && occupiedTimeInTimeSeries >= currentTime) endEventCnt++;
                    if (endEventCnt >= 1) {
                        existMultipleEventSameTime = true;
                        break;
                    }
                }
                ret2 = existMultipleEventSameTime;
                if (ret2) {
                    System.out.println("Found multiple END Event with the same event time.");
                    continue;
                }
                
                System.out.println("Check whether job slow down more than requested time.");
                boolean existSlowsDownJob = false;
                for (int k = 0; k < executingJobList.size(); ++k) {
                    Job job = executingJobList.get(k);
                    
                    int occupiedTimeInTimeSeries = job.getOccupiedTimeInTimeSlices();
                    int endEventOccuranceTimeNow = job.getEndEventOccuranceTimeNow();
                    ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
                    ArrayList<Integer> usingNodeIds = new ArrayList<Integer>();
                    for (int j = 0; j < usingNodes.size(); ++j) {
                        usingNodeIds.add(usingNodes.get(j).getNodeNum());
                    }
                    if (occupiedTimeInTimeSeries <= currentTime && usingNodeIds.contains(i)) {
                        int currentDetectiveTime = job.getCurrentDeactiveTime();
                        if (occupiedTimeInTimeSeries < endEventOccuranceTimeNow) {
                            int jobId = job.getJobId();
                            System.out.println("Job " + jobId + ": slows down more than requested time... it needs to be modified timeslices information");
                            existSlowsDownJob = true;
                            int updateTimeSliceValue = endEventOccuranceTimeNow;
                            modifyTimeSlicesDueToSlowsDonwJob(currentTime, occupiedTimeInTimeSeries, updateTimeSliceValue, i, job);
                            modifiedJobInTimeSlices.add(job);                                
                        }
                    }
                }
                boolean existIntermittenceInteractiveJob = false;
                for (int k = 0; k < executingJobList.size(); ++k) {
                    Job job = executingJobList.get(k);
                    boolean intJob = job.isInteracitveJob();
                    if (!intJob) {
                        continue;
                    }
                    
                    int occupiedTimeInTimeSeries = job.getOccupiedTimeInTimeSlices();
                    ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
                    ArrayList<Integer> usingNodeIds = new ArrayList<Integer>();
                    for (int j = 0; j < usingNodes.size(); ++j) {
                        usingNodeIds.add(usingNodes.get(j).getNodeNum());
                    }

                    boolean actState = job.isActivationState();
                    int currentDetectiveTime = job.getCurrentDeactiveTime();
                    int nextActivationTime = job.getNextActivationTime();

                    
                    if ( ( occupiedTimeInTimeSeries <= currentTime && usingNodeIds.contains(i) ) &&
                         ( ( actState && occupiedTimeInTimeSeries < currentDetectiveTime) ||
                           ( !actState && occupiedTimeInTimeSeries <= nextActivationTime))
                       )
                    {                    
                        int jobId = job.getJobId();                     
                        System.out.println("Job " + jobId + ": interactive job, ... it needs to be modified timeslices information");
                        existIntermittenceInteractiveJob = true;
                        int endEventOccuranceTimeNow = job.getEndEventOccuranceTimeNow();
                        if (endEventOccuranceTimeNow == 0) {                            
                            int updateTimeSliceValue;
                            if (actState) {
                                updateTimeSliceValue = currentDetectiveTime;
                            } else {
                                updateTimeSliceValue = nextActivationTime;
                            }
                            modifyTimeSlicesDueToInteractiveJob(currentTime, occupiedTimeInTimeSeries, updateTimeSliceValue, i, job);
                            modifiedJobInTimeSlices.add(job);                            
                        }                                               
                    }
                }                
                assert existMultipleEventSameTime || existSlowsDownJob || existIntermittenceInteractiveJob;
            } else {
                ret1 = false;
            }
        }
        for (Job job: modifiedJobInTimeSlices) {
            int endEventOccuranceTimeNow = job.getEndEventOccuranceTimeNow();
            int updateTimeSliceValue = endEventOccuranceTimeNow;
            boolean intJob = job.isInteracitveJob();
            boolean actState = job.isActivationState();
            int currentDetectiveTime = job.getCurrentDeactiveTime();
            int nextActivationTime = job.getNextActivationTime();
            if (endEventOccuranceTimeNow == 0 && intJob) {
                if (actState) {
                    updateTimeSliceValue = currentDetectiveTime;
                } else {
                    updateTimeSliceValue = nextActivationTime;
                }
            }
            job.setOccupiedTimeInTimeSlices(updateTimeSliceValue);
        }
        
        // Executing Job List
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        int totalFreeCores;
        int totalOccupiedCores = 0;
        for (int i = 0; i < executingJobList.size(); ++i) {
            Job job = executingJobList.get(i);
            totalOccupiedCores += job.getRequiredCoresPerNode() * job.getRequiredNodes();
        }
        for (int i = 0; i < temporallyScheduledJobList.size(); ++i) {
            Job job = temporallyScheduledJobList.get(i);
            totalOccupiedCores += job.getRequiredCoresPerNode() * job.getRequiredNodes();
        }
        totalFreeCores = NodeConsciousScheduler.numCores*NodeConsciousScheduler.numNodes - totalOccupiedCores;
        
        int totalFreeCoresFromAllNodeInfo = 0;
        for (int i = 0; i < freeCoreInAllNodeInfo.size(); ++i) {
            totalFreeCoresFromAllNodeInfo += freeCoreInAllNodeInfo.get(i);
        }
        
        if (totalFreeCores != totalFreeCoresFromAllNodeInfo) ret1 = false;

        return new TimeSlicesAndNodeInfoConsistency(ret1, ret2);
    }
    
    protected ArrayList<Event> scheduleJobsOnEnd(Event ev) {
        int currentTime = ev.getJob().getFinishedTime();
        
        // TODO
        // unifyTimeSlices(currentTime);
        makeTimeslices(currentTime);
        reduceTimeslices(currentTime, ev);
        completeOldSlices(currentTime);
        
       TimeSlicesAndNodeInfoConsistency consistency = checkTimeSlicesAndAllNodeInfo(currentTime);
       assert consistency.isConsistency() || consistency.isSameEndEventFlag();

        try {
            EventQueue.debugExecuting(currentTime, ev);
        } catch (Exception ex) {
            Logger.getLogger(Scheduler.class.getName()).log(Level.SEVERE, null, ex);
        }

        /* We must consider the points below: */
        /*  1. Check the coexisting jobs' OCStateLevel */
        ArrayList<Event> newEvents = new ArrayList<Event>();
        
        ArrayList<Event> newEventsOCState = new ArrayList<Event>();
        newEventsOCState = checkCoexistingJobsOCStateAndModifyENDEventAndTimeSlices(ev);
        for (int i = 0; i < newEventsOCState.size(); ++i) {
            newEvents.add(newEventsOCState.get(i));
        }

        ArrayList<Event> newEventsLoadBalancing = new ArrayList<Event>();
//        newEventsLoadBalancing = loadBalancing(ev);
        newEventsLoadBalancing = loadBalancing(ev, -1);
        for (int i = 0; i < newEventsLoadBalancing.size(); ++i) {
            newEvents.add(newEventsLoadBalancing.get(i));
        }
                
        ArrayList<Event> newEventsStart = new ArrayList<Event>();
        newEventsStart = scheduleJobsStartAt(currentTime);
        for (int i = 0; i < newEventsStart.size(); ++i) {
            newEvents.add(newEventsStart.get(i));
        }
        
        return newEvents;
    }
    
    protected ArrayList<Event> scheduleJobsOnSubmission(Event ev) {
        int currentTime = ev.getOccurrenceTime();
        
        // TODO
        // unifyTimeSlices(currentTime);
        makeTimeslices(currentTime);
        calcWastedResource(currentTime);
        completeOldSlices(currentTime);
        
        enqueue(ev);

        ArrayList<Event> newEvents = new ArrayList<Event>();
        newEvents = scheduleJobsStartAt(currentTime);
        
        return newEvents;
    }

    protected void assignJob(int startTime, LinkedList<TimeSlice> timeSlices, ArrayList<NodeInfo> allNodesInfo, Job job, ArrayList<Integer> assignNodesNo, boolean tmpFlag) {
        int addedPpn = job.getRequiredCores()/job.getRequiredNodes();
        double expectedEndTimeDouble = startTime + (job.getRequiredTime()-job.getCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity())*job.getOCStateLevel();
        int expectedEndTime = (int) ceil(expectedEndTimeDouble);
        long addedMpn = job.getMaxMemory();
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();
        /* TODO: The case requiredCores ist not dividable  */
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) {
            ++addedPpn;
            System.out.println("Not dividable, Job ID = " + job.getJobId());
        }
        int jobId = job.getJobId();

        
        /* Timesleces' setting */
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
//            ts.printTsInfo();
            //if (startTime <= ts.getEndTime() && ts.getStartTime() <= expectedEndTime) {
            if (startTime <= ts.getEndTime() && ts.getStartTime() < expectedEndTime) {
                ArrayList<Integer> cores = ts.getAvailableCores();
                ArrayList<Long> memories = ts.getAvailableMemory();
                for (int j = 0; j < assignNodesNo.size(); ++j) {
                    int nodeNo = assignNodesNo.get(j);
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
        
        /* NodeInfo Setting */
        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
           
            NodeInfo node = allNodesInfo.get(nodeNo);
            int numCores = node.getNumCores();
            
            int coreCnt = addedPpn;
            int numOccupiedCores = node.getNumOccupiedCores() + addedPpn;
            int numFreeCores = node.getNumFreeCores() - addedPpn;
            
            ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();
            Collections.sort(occupiedCores);
            for (int j = 0; j < numCores; ++j) {
                CoreInfo eachCore = occupiedCores.get(j);
                ArrayList<Integer> jobList = eachCore.getJobList();
                /*
                if (jobList.size() == 0) {
                    jobList.add(migratingJobId);                    
                    --coreCnt;                    
                }
                */

                jobList.add(jobId);                    
                --coreCnt;
                assert jobList.size() <= NodeConsciousScheduler.M;
                if (coreCnt == 0) break;
            }
            assert coreCnt == 0;

            node.setNumOccupiedCores(numOccupiedCores);
            node.setNumFreeCores(numFreeCores);
            node.getExecutingJobIds().add(jobId);
            if (scheduleUsingMemory) {
                long memoryCnt = addedMpn;
                long occupiedMemory = node.getOccupiedMemory() + addedMpn;
                long freeMemory = node.getFreeMemory() - addedMpn;
                assert occupiedMemory <= node.getMemorySize();
                assert freeMemory >= 0;
                node.setOccupiedMemory(occupiedMemory);
                node.setFreeMemory(freeMemory);
            }
            
        }
        
        /* Job Setting */
        if (tmpFlag) return;
        ArrayList<UsingNode> nodes = job.getUsingNodesList();

        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
            ArrayList<Integer> coreNums = new ArrayList<Integer>();
            
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeNo);
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < occupiedCores.size(); ++j) {
                CoreInfo eachCore = occupiedCores.get(j);
                ArrayList<Integer> jobList = eachCore.getJobList();
                int coreId = eachCore.getCoreId();
                for (int k = 0; k < jobList.size(); ++k) {
                    int usingJobId = jobList.get(k);
                    if (usingJobId == jobId) {
                        coreNums.add(coreId);
                    }
                }
                // Collections.sort(coreNums);
            }
            
            UsingNode node = new UsingNode(nodeNo, addedPpn, coreNums);
            nodes.add(node);
        }
    }

    protected void assignJob(int startTime, Job job, ArrayList<Integer> assignNodesNo) {
        assignJob(startTime, this.timeSlices, NodeConsciousScheduler.sim.getAllNodesInfo(), job, assignNodesNo, false);       
    }

    protected void assignJobOC(int startTime, LinkedList<TimeSlice> timeSlices, ArrayList<NodeInfo> allNodesInfo, Job job, ArrayList<Integer> assignNodesNo, boolean tmpFlag) {
        int addedPpn = job.getRequiredCores()/job.getRequiredNodes();
        int expectedEndTime = startTime + job.getRequiredTime();

        /* TODO: The case requiredCores ist not dividable  */
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) {
            ++addedPpn;
            System.out.println("Not dividable, Job ID = " + job.getJobId());
        }

        
        /* Timesleces' setting */
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
//            ts.printTsInfo();
            //if (startTime <= ts.getEndTime() && ts.getStartTime() <= expectedEndTime) {
            if (startTime <= ts.getEndTime() && ts.getStartTime() < expectedEndTime) {
                ArrayList<Integer> cores = ts.getAvailableCores();
                for (int j = 0; j < assignNodesNo.size(); ++j) {
                    int nodeNo = assignNodesNo.get(j);
                    int core = cores.get(nodeNo);
                    core -= addedPpn;
                    cores.set(nodeNo, core);
                }
            }
//            ts.printTsInfo();
        }
        
        /* NodeInfo Setting */
        int jobId = job.getJobId();
        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
           
            NodeInfo node = allNodesInfo.get(nodeNo);
            int numCores = node.getNumCores();
            
            int coreCnt = addedPpn;
            int numOccupiedCores = node.getNumOccupiedCores() + addedPpn;
            int numFreeCores = node.getNumFreeCores() - addedPpn;

            /* ここがダメ。入れ子配列の要素数順にsortしたい。単なる要素じゃなくクラス化したほうがよい */
            ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();
            
            for (int j = 0; j < numCores; ++j) {
                CoreInfo eachCore = occupiedCores.get(j);
                ArrayList<Integer> jobList = eachCore.getJobList();
                // 下のifも不要
                if (jobList.size() == 0) {
                    jobList.add(jobId);                    
                    --coreCnt;                    
                }
                if (coreCnt == 0) break;
            }
            assert coreCnt == 0;

            node.setNumOccupiedCores(numOccupiedCores);
            node.setNumFreeCores(numFreeCores);
            
        }
        
        /* Job Setting */
        if (tmpFlag) return;
        ArrayList<UsingNode> nodes = job.getUsingNodesList();

        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
            ArrayList<Integer> coreNum = new ArrayList<Integer>();
            
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeNo);           
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < occupiedCores.size(); ++j) {
                CoreInfo eachCore = occupiedCores.get(j); 
                ArrayList jobList = eachCore.getJobList();
                for (int k = 0; k < jobList.size(); ++k) {
                    int usingJobId = (int) jobList.get(k);
                    if (usingJobId == jobId) {
                        coreNum.add(j);
                    }
                }
            }
            
            UsingNode node = new UsingNode(nodeNo, addedPpn, coreNum);
            nodes.add(node);
        }
    }

    private ArrayList<Event> loadBalancing(Event ev) {
        ArrayList<Event> result = new ArrayList<Event>();        
        int currentTime = ev.getOccurrenceTime();
        Job endingJob = ev.getJob();
        ArrayList<UsingNode> usingNodeList = endingJob.getUsingNodesList();
        int endingJobId = endingJob.getJobId();
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        /* Check the all executing jobs */
        for (int i = 0; i < executingJobList.size(); ++i) {
            Job migratingJob = executingJobList.get(i);
            int migratingJobId = migratingJob.getJobId();
            if(!checkUseSameNode(usingNodeList, migratingJob)) continue;
            System.out.println("\tdebug)jobId:" +migratingJob.getJobId());
            int OCStateLevel = migratingJob.getOCStateLevel();
            if (OCStateLevel == 1) {
                continue;
            }
            
            /* 1. Calculate candidate cores the migratingJob can migrate */
            /* 2. Do migration: return the new coexisting jobs and
                                       the deleted coexisting jobs(changed from coexisting one to NOT coexisting one due to migration)
                 2.1 modifying usingNode -> usingCoreNum
                 2.2 modifying allNodeInfo -> NodeInfo -> occupiedCores -> jobList
            */
            /* 3. Modify the END Event time */
            /* 4. Modify the TimeSlices */

            /* 1. Calculate candidate cores the migratingJob can migrate */
            ArrayList<MigrateTargetNode> migrateTargetNodes = calculateMigrateTargetCoresPerNode(migratingJob);
            /* 2. Do migration */
            NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobs = doMigrate(migratingJob, migrateTargetNodes);
            printNewAndDeletedCoexistingJobs(newAndDeletedCoexistingJobs, migratingJob);

            Set<Integer> deletedCoexistingJobs = newAndDeletedCoexistingJobs.getDeletedCoexistingJobsFromTheCore();
            for (int deletedCoexistingJobId: deletedCoexistingJobs) {
                
                Job coexistingJob = getJobByJobId(deletedCoexistingJobId);
                int coexistingJobId = coexistingJob.getJobId();
                ArrayList<UsingNode> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();
                int OCStateLevelCoexistingJob = checkMultiplicityAlongNodes(coexistingJobUsingNodeList, BLANK_JOBID, coexistingJobId);

                // 3.2 Modify the END Event time
                modifyTheENDEventTime(coexistingJob, coexistingJobId, currentTime, OCStateLevelCoexistingJob, result);

                /* 4. Modify the TimeSlices */
                Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
                modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, migratingJobId);
            }
            
            Set<Integer> newCoexistingJobs = newAndDeletedCoexistingJobs.getNewCoexistingJobsOnTheCore();
            for (int newCoexistingJobId: newCoexistingJobs) {
                // 3. Modify the END Event time 
                //  3.1 Check the OCStateLevel
                Job coexistingJob = getJobByJobId(newCoexistingJobId);
                int coexistingJobId = coexistingJob.getJobId();
                ArrayList<UsingNode> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();
                int OCStateLevelCoexistingJob = checkMultiplicityAlongNodes(coexistingJobUsingNodeList, BLANK_JOBID, coexistingJobId);

                // 3.2 Modify the END Event time
                // modifyTheENDEventTime(coexistingJob, coexistingJobId, currentTime, OCStateLevelCoexistingJob, result);
                result.addAll(modifyTheENDEventTimeForNewCoexistingJobByJobId(currentTime, coexistingJobId, OCStateLevelCoexistingJob));
                coexistingJob.setOCStateLevel(OCStateLevelCoexistingJob);
                /* 4. Modify the TimeSlices */
                Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
                modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, endingJobId);
                coexistingJobCoexistingJob.add(migratingJobId);
            }

            ArrayList<UsingNode> migratingJobUsingNodeList = migratingJob.getUsingNodesList();
            int newOCStateLevel = checkMultiplicityAlongNodes(migratingJobUsingNodeList, BLANK_JOBID, migratingJobId);
            modifyTheENDEventTime(migratingJob, migratingJobId, currentTime, newOCStateLevel, result);

            Set<Integer> migratingJobCoexistingJob = migratingJob.getCoexistingJobs();
            modifyTheTimeSlices(migratingJob, migratingJobCoexistingJob, currentTime, endingJobId);
            
            migratingJobCoexistingJob.addAll(newCoexistingJobs);
            migratingJobCoexistingJob.removeAll(deletedCoexistingJobs);
        }
        return result;
    }

    private ArrayList<Event> loadBalancing(Event ev, int x) {
        ArrayList<Event> result = new ArrayList<Event>();        
        int currentTime = ev.getOccurrenceTime();
        Job endingJob = ev.getJob();
        ArrayList<UsingNode> usingNodeList = endingJob.getUsingNodesList();
        int endingJobId = endingJob.getJobId();
        
        // 1. Check Ending jobs UsingNodes
        // 2. For each node, obtain the maximum/minimum multiplicity core
        // 3. Migrate the job

        System.out.println("\tdebug)Try to migrate, endingJobId:" +endingJobId);                    
        // 1. Check Ending jobs UsingNodes
        for (int i = 0; i < usingNodeList.size(); ++i) {
            UsingNode usingNode = usingNodeList.get(i);
            int nodeId = usingNode.getNodeNum();
            printUsingNode(usingNode, endingJobId);
            
            MaxAndMinMultiplicityCores maxAndMinMultiplicityCores = new MaxAndMinMultiplicityCores();
            while (true) {
                maxAndMinMultiplicityCores = obtainMaxMultiplicityCores(usingNode);
                CoreInfo maxCoreInfo = maxAndMinMultiplicityCores.getMaxCoreInfo();
                CoreInfo minCoreInfo = maxAndMinMultiplicityCores.getMinCoreInfo();

                if (maxCoreInfo.getJobList().size() - minCoreInfo.getJobList().size() <= 1)
                    break;
                
                result.addAll(doMigrate(maxAndMinMultiplicityCores, nodeId, currentTime, endingJobId));
            }
        }
        
        return result;
    }
    
    
    private ArrayList<MigrateTargetNode> calculateMigrateTargetCoresPerNode(Job job) {
        ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
        ArrayList<NodeInfo> allNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        int OCStateLevel = job.getOCStateLevel();
        int jobId = job.getJobId();
        ArrayList migrateTargetNodes = new ArrayList<MigrateTargetNode>();
        
        for (int i = 0; i < usingNodes.size(); ++i) {
            UsingNode usingNode = usingNodes.get(i);
            int nodeId = usingNode.getNodeNum();
            ArrayList<Integer> usingCores = usingNode.getUsingCoreNum();
            NodeInfo nodeInfo = allNodeInfo.get(nodeId);
            assert nodeId == nodeInfo.getNodeNum();
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();

            MigrateTargetNode targetNode = new MigrateTargetNode(nodeId);
            for (int j = 0; j < occupiedCores.size(); ++j) {
                CoreInfo coreInfo = occupiedCores.get(j);
                int coreId = coreInfo.getCoreId();
                ArrayList<Integer> jobList = coreInfo.getJobList();
                assert (jobList.contains(jobId) && usingCores.contains(coreId)) || (!jobList.contains(jobId) && !usingCores.contains(coreId));
                if (!jobList.contains(jobId) && jobList.size() < OCStateLevel-1) {
                    /* ADD CoreInfo to candidate of migrate target */
                    targetNode.getMigrateTargetCores().add(coreInfo);
                }                
            }
            migrateTargetNodes.add(targetNode);            
        }
        return migrateTargetNodes;
    }

    private NewAndDeletedCoexistingJobs doMigrate(Job job, ArrayList<MigrateTargetNode> migrateTargetNodes) {
        ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
        ArrayList<NodeInfo> allNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobs = new NewAndDeletedCoexistingJobs();

        /* For usingNode */
        for (int i = 0; i < migrateTargetNodes.size(); ++i) {            
            MigrateTargetNode migrateTargetNode = migrateTargetNodes.get(i);
            if (migrateTargetNode.getMigrateTargetCores().size() == 0) continue;
            int nodeId = migrateTargetNode.getNodeId();
                        
            UsingNode usingNode = usingNodes.get(i);
            assert usingNode.getNodeNum() == nodeId;
            ArrayList<Integer> usingCores = usingNode.getUsingCoreNum();
            
            ArrayList<CoreInfo> migrateTargetCores = migrateTargetNode.getMigrateTargetCores();
            for (int usingCore: usingCores) {
                assert !migrateTargetCores.contains(usingCore);
            }
            
            ArrayList<CoreInfo> usingCoresWithMultiplicity = getMultiplicityOnUsingCores(nodeId, usingCores);
//            Collections.sort(usingCoresWithMultiplicity);
            Collections.reverse(usingCoresWithMultiplicity);
            printUsingCoreInfo(job, nodeId, usingCoresWithMultiplicity, false);
            
            int migrationCnt = 0;
            
            /* Do migration */
            NewAndDeletedCoexistingJobs  newAndDeletedCoexistingJobsAlongCores = new NewAndDeletedCoexistingJobs();
            for (CoreInfo ci: usingCoresWithMultiplicity) {
                NewAndDeletedCoexistingJobs  newAndDeletedCoexistingJobsOnTheCore = new NewAndDeletedCoexistingJobs();                
                if (ci.getJobList().size() > 1) { // 1 is hard-coded?
                    newAndDeletedCoexistingJobsOnTheCore = migrate(job, ci, usingCores, migrateTargetCores, migrationCnt);
                    ++migrationCnt;
                }
                updateNewAndDeletedCoexistingJobs(newAndDeletedCoexistingJobsAlongCores, newAndDeletedCoexistingJobsOnTheCore);
                if (migrationCnt >= migrateTargetCores.size()) break;
            }
            
            usingCoresWithMultiplicity = getMultiplicityOnUsingCores(nodeId, usingCores);
            printUsingCoreInfo(job, nodeId, usingCoresWithMultiplicity, true);
            
            updateNewAndDeletedCoexistingJobs(newAndDeletedCoexistingJobs, newAndDeletedCoexistingJobsAlongCores);
        }
        return newAndDeletedCoexistingJobs;
    }

    private ArrayList<CoreInfo> getMultiplicityOnUsingCores(int nodeId, ArrayList<Integer> usingCores) {
        ArrayList<NodeInfo> allNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        NodeInfo node = allNodeInfo.get(nodeId);
        assert nodeId == node.getNodeNum();
        ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();
        
        ArrayList<CoreInfo> result = new ArrayList<CoreInfo>();
        for (int i = 0; i < usingCores.size(); ++i) {
            int coreId = usingCores.get(i);
            CoreInfo coreInfo = getOccupiedCoreInfoByCoreId(occupiedCores, coreId); // TODO: this method is O(N);
            assert coreId == coreInfo.getCoreId();
            result.add(coreInfo);
        }
        return result;
    }

    private void printUsingCoreInfo(Job job, int nodeId, ArrayList<CoreInfo> usingCoresWithMultiplicity, boolean afterFlag) {
        int jobId = job.getJobId();
        
        System.out.print("\tdebug) jobId " + jobId + " at node " + nodeId + ", ");

        if (!afterFlag) {
            System.out.print("Before migrating, ");
        } else {
            System.out.print("After  migrating, ");
        }
        System.out.print("now using cores: ");
        for (int i = 0; i < usingCoresWithMultiplicity.size(); ++i) {
            CoreInfo ci = usingCoresWithMultiplicity.get(i);
            assert ci.getJobList().contains(jobId);
            System.out.print(ci.getCoreId() + "(" + ci.getJobList().size() + ")");
            if (i != usingCoresWithMultiplicity.size() - 1) System.out.print(", ");            
        }
        System.out.println();
    }
    
    protected CoreInfo getOccupiedCoreInfoByCoreId(ArrayList<CoreInfo> occupiedCores, int usingCoreId) {
        CoreInfo ret = new CoreInfo();
        for (int i = 0; i < occupiedCores.size(); ++i) {
            CoreInfo coreInfo = occupiedCores.get(i);
            if (coreInfo.getCoreId() == usingCoreId) {
                ret = coreInfo;
                break;
            }
        }
        assert ret.getCoreId() != UNUSED;
        return ret;
    }

    private NewAndDeletedCoexistingJobs migrate(Job job, CoreInfo ci, ArrayList<Integer> usingCores, ArrayList<CoreInfo> migrateTargetCores, int migrationCnt) {
        NewAndDeletedCoexistingJobs result = new NewAndDeletedCoexistingJobs();
        Set<Integer> newCoexistingJobOnTheCore = new HashSet<Integer>();
        Set<Integer> deletedFromCoexistingJobOnTheCore = new HashSet<Integer>();
        result.setNewCoexistingJobsOnTheCore(newCoexistingJobOnTheCore);
        result.setDeletedCoexistingJobsFromTheCore(deletedFromCoexistingJobOnTheCore);
        
        int jobId = job.getJobId();
        int usingCoreId = ci.getCoreId();
        ArrayList<Integer> usingJobList = ci.getJobList();
        assert usingJobList.contains(jobId);
        
        CoreInfo migrateTargetCore = migrateTargetCores.get(migrationCnt);
        int targetCoreId = migrateTargetCore.getCoreId();
        assert targetCoreId != usingCoreId;
        
        ArrayList<Integer> migrateTargetJobList = migrateTargetCore.getJobList();
        assert !migrateTargetJobList.contains(jobId);
        
        usingJobList.remove((Integer) jobId);
        for (int deletedFromCoexistingJob: usingJobList) {
            deletedFromCoexistingJobOnTheCore.add(deletedFromCoexistingJob);
        }
        usingCores.remove((Integer) usingCoreId);
        
        for (int newCoexistingJob: migrateTargetJobList) {
            newCoexistingJobOnTheCore.add(newCoexistingJob);
        }
        migrateTargetJobList.add(jobId);
        usingCores.add(targetCoreId);
        
        deletedFromCoexistingJobOnTheCore.removeAll(newCoexistingJobOnTheCore);
        assert !deletedFromCoexistingJobOnTheCore.contains(jobId);
        assert !newCoexistingJobOnTheCore.contains(jobId);
        //newCoexistingJobOnTheCore.removeAll(deletedFromCoexistingJobOnTheCore);
        
        return result;
    }

    private boolean checkUseSameNode(ArrayList<UsingNode> usingNodeList, Job job) {
        ArrayList<UsingNode> migratingJobUsingNodeList = job.getUsingNodesList();
        boolean ret = false;
        for (int i = 0; i < migratingJobUsingNodeList.size(); ++i) {
            UsingNode migratingJobUsingNode = migratingJobUsingNodeList.get(i);
            int migratingJobNodeId = migratingJobUsingNode.getNodeNum();
            for (int j = 0; j < usingNodeList.size(); ++j) {
                UsingNode usingNode = usingNodeList.get(j);
                if (usingNode.getNodeNum() == migratingJobNodeId) {
                    ret = true;
                    break;
                }
            }
            if (ret) break;
        }
        return ret;
    }

    private void updateNewAndDeletedCoexistingJobs(NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobsSink, NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobsSource) {
        Set<Integer> updateSinkNew = newAndDeletedCoexistingJobsSink.getNewCoexistingJobsOnTheCore();
        Set<Integer> updateSinkDeleted = newAndDeletedCoexistingJobsSink.getDeletedCoexistingJobsFromTheCore();
        
        Set<Integer> updateSourceNew = newAndDeletedCoexistingJobsSource.getNewCoexistingJobsOnTheCore();
        Set<Integer> updateSourceDeleted = newAndDeletedCoexistingJobsSource.getDeletedCoexistingJobsFromTheCore();
  
        updateSinkNew.addAll(updateSourceNew);
        updateSinkDeleted.addAll(updateSourceDeleted);
        
        updateSinkDeleted.removeAll(updateSinkNew);
    }

    private void printNewAndDeletedCoexistingJobs(NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobs, Job job) {
        System.out.print("\tdebug)job id :" + job.getJobId());
        System.out.print(", newCoexistingJob"  + newAndDeletedCoexistingJobs.getNewCoexistingJobsOnTheCore());
        System.out.print(", deletedCoexistingJob" + newAndDeletedCoexistingJobs.getDeletedCoexistingJobsFromTheCore());
        System.out.println("");
    }

    static Job getJobByJobId(int coexistingJobId) {
/*
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        int i;
        Job job = new Job();
        for (i = 0; i < executingJobList.size(); ++i) {
            job = executingJobList.get(i);
            int executingJobId = job.getJobId();
            if(executingJobId == coexistingJobId) break;
        }
        if (job.getJobId() == coexistingJobId) return job;
        for (i = 0; i < temporallyScheduledJobList.size(); ++i) {
            job = temporallyScheduledJobList.get(i);
            int temporallyJobId = job.getJobId();
            if(temporallyJobId == coexistingJobId) break;            
        }
        assert job.getJobId() == coexistingJobId;
        return job;
*/
        return NodeConsciousScheduler.sim.getJobMap().get(coexistingJobId);
    }
     
     protected int checkMultiplicityAlongNodes(ArrayList<UsingNode> coexistingJobUsingNodeList, int endingJobId, int coexistingJobId) {        
        boolean noEndFlag = (endingJobId == BLANK_JOBID);
        int multiplicityAlongNodes = UNUPDATED;
        for (int i = 0; i < coexistingJobUsingNodeList.size(); ++i) {
            int multiplicityAlongCores = UNUPDATED;

            // node setting
            UsingNode usingNode = coexistingJobUsingNodeList.get(i);
            int usingNodeId = usingNode.getNodeNum();
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(usingNodeId);
            assert usingNodeId == nodeInfo.getNodeNum();

            ArrayList<Integer> usingCoreIds = usingNode.getUsingCoreNum();

                // Core loop
            // 1.2 Check all cores used by coexisting migratingJob                
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
            for (int usingCoreId : usingCoreIds) {
                CoreInfo usingCoreInfo = getOccupiedCoreInfoByCoreId(occupiedCores, usingCoreId); // O(N)
                assert usingCoreId == usingCoreInfo.getCoreId();
                ArrayList<Integer> jobListOnTheCore = usingCoreInfo.getJobList();
                assert jobListOnTheCore.contains(coexistingJobId);
                assert (!noEndFlag && !jobListOnTheCore.contains(endingJobId)) || noEndFlag;
                multiplicityAlongCores = max(multiplicityAlongCores, jobListOnTheCore.size());
            }
            assert multiplicityAlongCores != UNUPDATED;
            assert multiplicityAlongCores <= NodeConsciousScheduler.M;
            multiplicityAlongNodes = max(multiplicityAlongNodes, multiplicityAlongCores);
        }
        assert multiplicityAlongNodes != UNUPDATED;
        assert multiplicityAlongNodes <= NodeConsciousScheduler.M;
        return multiplicityAlongNodes;
    }    

    static void measureCurrentExecutingTime(int currentTime, Job executingJob) {
        int OCStateLevel = executingJob.getOCStateLevel();
        measureCurrentExecutingTime(currentTime, executingJob, OCStateLevel);
    }
     
    static void measureCurrentExecutingTime(int currentTime, Job victimJob, int OCStateLevel) {
        int currentNetOCStateLevel = victimJob.getNetOCStateLevel();
        int currentOCStateLevel = victimJob.getOCStateLevel();

        int startTime = victimJob.getStartTime();
        if (startTime == UNSTARTED) {
            return;
        }
        
        int jobId = victimJob.getJobId();
        // System.out.println("JobId: " + migratingJobId);
        /* measure current progress */
        int previousMeasuredTime = victimJob.getPreviousMeasuredTime();
        if (previousMeasuredTime == currentTime) {
            return;
        }
        // TODO: should be double, but now int.        
        double currentAccumulatedComputeQuantity = victimJob.getCurrentAccumulatedComputeQuantity();
        int realDeltaTime = currentTime - previousMeasuredTime;
        double jobAffinityRatio = victimJob.getJobAffinityRatio();
        double osubOvhRatio = victimJob.getOsubOverheadRatio();
        currentAccumulatedComputeQuantity += (double)realDeltaTime / currentNetOCStateLevel / jobAffinityRatio / osubOvhRatio;
        victimJob.setCurrentAccumulatedComputeQuantity(currentAccumulatedComputeQuantity);
        
        double currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = victimJob.getCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity();
        currentAccumulatedComputeQuantityOnlyConsiderMultiplicity += (double)realDeltaTime / currentOCStateLevel;
        victimJob.setCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity(currentAccumulatedComputeQuantityOnlyConsiderMultiplicity);

        double accumulatedCpuTime = victimJob.getAccumulatedCpuTime();
        accumulatedCpuTime += (double)(realDeltaTime) / currentNetOCStateLevel;
        victimJob.setAccumulatedCpuTime(accumulatedCpuTime);
        
        if (OCStateLevel == 1) {
            int runningTimeDed = victimJob.getRunningTimeDed();
            runningTimeDed += realDeltaTime;
            victimJob.setRunningTimeDed(runningTimeDed);
        } else {
            int runningTimeOC = victimJob.getRunningTimeOC();
            runningTimeOC += realDeltaTime;
            victimJob.setRunningTimeOC(runningTimeOC);
        }

        return;
    }

    static void measureCurrentExecutingTimeForActivation(int currentTime, Job job, int OCStateLevel) {
        boolean interactiveJob = job.isInteracitveJob();
        assert interactiveJob;
        
        boolean activateionState = job.isActivationState();
        if (!activateionState) {
            return;
        }
        
        int currentNetOCStateLevel = job.getNetOCStateLevel();

        int startTime = job.getStartTime();
        if (startTime == UNSTARTED) {
            return;
        }
        
        int jobId = job.getJobId();
        // System.out.println("JobId: " + migratingJobId);
        /* measure current progress */
        int previousMeasuredTime = job.getPreviousMeasuredTime();
        if (previousMeasuredTime == currentTime) {
            return;
        }




        int realDeltaTime = currentTime - previousMeasuredTime;
        double currentAccumulatedComputeQuantityForLatestActivation = job.getCurrentAccumulatedComputeQuantityForLatestActivation();

        double jobAffinityRatio = job.getJobAffinityRatio();
        double osubOvhRatio = job.getOsubOverheadRatio();
        currentAccumulatedComputeQuantityForLatestActivation += (double)realDeltaTime / currentNetOCStateLevel / jobAffinityRatio / osubOvhRatio;
        job.setCurrentAccumulatedComputeQuantityForLatestActivation(currentAccumulatedComputeQuantityForLatestActivation);
        
        int currentOCStateLevel = job.getOCStateLevel();
        double currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = job.getCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity();
        currentAccumulatedComputeQuantityOnlyConsiderMultiplicity += (double)realDeltaTime / currentOCStateLevel;
        job.setCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity(currentAccumulatedComputeQuantityOnlyConsiderMultiplicity);

        double accumulatedCpuTime = job.getAccumulatedCpuTime();
        accumulatedCpuTime += (double)(realDeltaTime) / currentNetOCStateLevel;
        job.setAccumulatedCpuTime(accumulatedCpuTime);
        
        if (OCStateLevel == 1) {
            int runningTimeDed = job.getRunningTimeDed();
            runningTimeDed += realDeltaTime;
            job.setRunningTimeDed(runningTimeDed);
        } else {
            int runningTimeOC = job.getRunningTimeOC();
            runningTimeOC += realDeltaTime;
            job.setRunningTimeOC(runningTimeOC);
        }

        return;
    }
    
    protected void printOCStateLevelTransition(int currentOCStateLevel, int newOCStateLevelForJob, int victimJobId) {
        if (currentOCStateLevel + 1 == newOCStateLevelForJob) {
            System.out.print("\t\tdebug) OC State is updated from " + currentOCStateLevel + " to " + newOCStateLevelForJob);
        } else if (currentOCStateLevel == newOCStateLevelForJob) {
            System.out.print("\t\tdebug) OC State is not updated, remains " + currentOCStateLevel);
        } else if (currentOCStateLevel - 1 == newOCStateLevelForJob) {
            System.out.print("\t\tdebug) OC State is updated from " + currentOCStateLevel + " to " + newOCStateLevelForJob);
        }
        System.out.println(", jobId: " + victimJobId);
    }

    protected int calculateNewActualEndTime(Job victimJob) {
        int startTime = victimJob.getStartTime();
        assert startTime >= 0;
        return calculateNewActualEndTime(startTime, victimJob);
    }

    static int calculateNewActualEndTime(int startTime, Job victimJob) {
        /* calculate new actual End Time */
        int currentNetOCStateLevel = victimJob.getNetOCStateLevel(); // This value is after-updated.
        double currentAccumulatedComputeQuantity = victimJob.getCurrentAccumulatedComputeQuantity();
        int actualExecuteTime = victimJob.getActualExecuteTime();
        double restActualExecuteTime = (actualExecuteTime - currentAccumulatedComputeQuantity) * currentNetOCStateLevel;      
        restActualExecuteTime = max(restActualExecuteTime, 0);
        //double ratio = 1.0;
        /*
        if (OCStateLevel >= 2 && NodeConsciousScheduler.sim.isConsiderJobMatching()) {
            Set<Integer> coexistingJobs = victimJob.getCoexistingJobs();
            ratio = calculateMaxDegradationRatioForVictim(victimJob, coexistingJobs);
        }
        */
        double jobAffinityRatio = victimJob.getJobAffinityRatio();
        double osubOvhRatio = victimJob.getOsubOverheadRatio();
        restActualExecuteTime *= (jobAffinityRatio * osubOvhRatio);
        double trueEndTime = startTime + restActualExecuteTime;

        return (int) ceil(trueEndTime);
    }

    
    static int calculateNewActualEndTimeForActivation(int startTime, Job job) {
        boolean interactiveJob = job.isInteracitveJob();
        assert interactiveJob;
        
        /* calculate new actual End Time */
        int currentNetOCStateLevel = job.getNetOCStateLevel(); // This value is after-updated.

        int currentActivationIndex = job.getCurrentActivationIndex();
        double currentAccumulatedComputeQuantityForLatestActivation = job.getCurrentAccumulatedComputeQuantityForLatestActivation();
        int actualExecuteTimeForLatestActivation = job.getActivationTimes().get(currentActivationIndex);
        double restActualExecuteTimeForLastestActivation = (actualExecuteTimeForLatestActivation - currentAccumulatedComputeQuantityForLatestActivation) * currentNetOCStateLevel;
        restActualExecuteTimeForLastestActivation = max(restActualExecuteTimeForLastestActivation, 0);
        // TODO: This is not accurate
        // Must calculate the accurate ratio when calculating netOCStateLevel
        double jobAffinityRatio = job.getJobAffinityRatio();
        double osubOvhRatio = job.getOsubOverheadRatio();
        restActualExecuteTimeForLastestActivation *= (jobAffinityRatio * osubOvhRatio);
        double deactivateTime = startTime + restActualExecuteTimeForLastestActivation;

        return (int) ceil(deactivateTime);
    }    
 
    static void printThrownEvent(int currentTime, int trueEndTime, Job job, EventType evt) {
        printThrownEvent(currentTime, trueEndTime, job, evt, 1);
    }
    
    static void printThrownEvent(int currentTime, int trueEndTime, Job job, EventType evt, int tabno) {
        String label = ", newArrivalTime: ";
        if (evt == EventType.END) {
            label = ", newTrueEndTime: ";
        } else if (evt == EventType.INT_ACTIVATE) {
            label = ", newNextActivateTime: ";
        } else if (evt == EventType.START) {
            label = ", startTime: ";
        } else if (evt == EventType.INT_DEACTIVATE) {
            label = ", newDeactivateTime: ";
        }
        
        for (int i = 0; i < tabno; ++i) System.out.print("\t");
        System.out.println("debug) Throw " + evt + " event: jobId " + job.getJobId() + label + trueEndTime + " at " + currentTime);
    }
    
    protected void modifyTheENDEventTime(Job coexistingJob, int coexistingJobId, int currentTime, int OCStateLevel, ArrayList<Event> result) {
        int coexistingStartTime = coexistingJob.getStartTime();
        assert coexistingStartTime >= 0;
        assert coexistingStartTime <= currentTime;

        //  1-1. Measure the executing time at current time for each victim jobs.
        boolean interactiveJob = coexistingJob.isInteracitveJob();
        if (!interactiveJob) {
            measureCurrentExecutingTime(currentTime, coexistingJob);
        } else {
            measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, coexistingJob.getNetOCStateLevel());
        }
        coexistingJob.setPreviousMeasuredTime(currentTime);

        //  1-2. Calculate new trueEndTime
        int currentOCStateLevel = coexistingJob.getOCStateLevel();
        assert (currentOCStateLevel - 1 == OCStateLevel) || (currentOCStateLevel == OCStateLevel) || (currentOCStateLevel > OCStateLevel);
        // debug
        printOCStateLevelTransition(currentOCStateLevel, OCStateLevel, coexistingJobId);
        int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
        coexistingJob.setOCStateLevel(OCStateLevel);
        int netOCStateLevel = calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
        coexistingJob.setNetOCStateLevel(netOCStateLevel);
        double ratio = calculateMaxDegradationRatioForVictim(coexistingJob, coexistingJob.getCoexistingJobs());
        coexistingJob.setJobAffinityRatio(ratio);
        coexistingJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(coexistingJob));
        int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
//        assert trueEndTime <= oldTrueEndTime;

        boolean actState = coexistingJob.isActivationState();
        //  1-3. Rethrow the END event set the time

        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && currentTime != oldTrueEndTime) {
        if (!interactiveJob && currentTime != trueEndTime && currentTime != oldTrueEndTime && trueEndTime != oldTrueEndTime ){
            printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
            result.add(new Event(EventType.END, trueEndTime, coexistingJob));
            coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
            printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
            result.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
        } else if (interactiveJob && actState) {
            int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
            int deactivateTime = Scheduler.calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
            if (oldDeactivateTime != deactivateTime) {
                printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                result.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                coexistingJob.setCurrentDeactiveTime(deactivateTime);
                printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                result.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
            }            
        }
    }
    /* This method return the exepeceted end time. */
    /* Using only OCStateLevel, not considering netOCStateLevel */
    protected int calculateNewExpectedEndTime(int currentTime, Job victimJob) {
        int currentOCStateLevel = victimJob.getOCStateLevel(); // This value is after-updated.
        double currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = victimJob.getCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity();
        int requiredTime = victimJob.getRequiredTime();
        double restRequiredTime = (requiredTime - currentAccumulatedComputeQuantityOnlyConsiderMultiplicity) * currentOCStateLevel;
        restRequiredTime = max(restRequiredTime, 1);
        double expectedEndTime = currentTime + restRequiredTime;

        return (int) ceil(expectedEndTime);
    }

    protected int calculateApproximateEndTime(int currentTime, Job victimJob, int OCStateLevel) {
        double currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = victimJob.getCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity();
        int requiredTime = victimJob.getRequiredTime();
        double restRequiredTime = (requiredTime - currentAccumulatedComputeQuantityOnlyConsiderMultiplicity) * OCStateLevel;
        double expectedEndTime = currentTime + restRequiredTime;

        return (int) ceil(expectedEndTime);
    }
    
    private void printDifferenceExpectedEndTime(int oldExpectedEndTime, int newExpectedEndTime, int jobId) {
        System.out.println("\t\tdebug) Modify the timeslices, oldExpectedEndTime: " + oldExpectedEndTime + ", newExpectedEndTime: " + newExpectedEndTime + ", jobId: " + jobId);
    }
    

    protected int getTimeSliceIndexEndTimeEquals(int oldExpectedEndTime) {
        return getTimeSliceIndexEndTimeEquals(oldExpectedEndTime, this.timeSlices);
    }
    
    protected int getTimeSliceIndexEndTimeEquals(int oldExpectedEndTime, LinkedList<TimeSlice> timeSlices) {
        int index = UNUPDATED;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            if (oldExpectedEndTime == ts.getEndTime()) {
                index = i;
                break;
            }
        }
        assert index != UNUPDATED;
        return index;
    }
    
    protected void refiilFreeCoresInTimeSlices(int currentTime, int timeSliceIndex, Job victimJob) {
        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob, this.timeSlices);
    }
    
    protected void refiilFreeCoresInTimeSlices(int currentTime, int timeSliceIndex, Job victimJob, LinkedList<TimeSlice> timeSlices) {
        ArrayList<UsingNode> usingNodes = victimJob.getUsingNodesList();
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();
        long addedMpn = victimJob.getMaxMemory();
        
        for (int i = 0; i <= timeSliceIndex; ++i) {
            TimeSlice ts = timeSlices.get(i);
            ArrayList<Long> memories = ts.getAvailableMemory();
            assert currentTime <= ts.getStartTime();
            ArrayList<Integer> availableCores = ts.getAvailableCores();
            for (int j = 0; j < usingNodes.size(); ++j) {
                UsingNode usingNode = usingNodes.get(j);
                int nodeId = usingNode.getNodeNum();
                int releaseCore = usingNode.getNumUsingCores();

                int freeCore = availableCores.get(nodeId);
                freeCore += releaseCore;
                assert freeCore <= NodeConsciousScheduler.numCores;
                availableCores.set(nodeId, freeCore);

                if (scheduleUsingMemory) {
                    long memory = memories.get(nodeId);
                    memory += addedMpn;
                    assert memory <= NodeConsciousScheduler.memory;
                    memories.set(nodeId, memory);
                }
                
                
            }
        }
    }
    
    protected void reallocateOccupiedCoresInTimeSlices(int currentTime, int newExpectedEndTime, Job victimJob) {
        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob, this.timeSlices);
    }

    protected void reallocateOccupiedCoresInTimeSlices(int currentTime, int newExpectedEndTime, Job victimJob, LinkedList<TimeSlice> timeSlices) {
        ArrayList<UsingNode> usingNodes = victimJob.getUsingNodesList();
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();
        long addedMpn = victimJob.getMaxMemory();
        
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            assert currentTime <= ts.getStartTime();
            assert ts.getStartTime() < newExpectedEndTime;

            ArrayList<Integer> availableCores = ts.getAvailableCores();
            ArrayList<Long> memories = ts.getAvailableMemory();
            for (int j = 0; j < usingNodes.size(); ++j) {
                UsingNode usingNode = usingNodes.get(j);
                int nodeId = usingNode.getNodeNum();
                int occupiedCore = usingNode.getNumUsingCores();

                int freeCore = availableCores.get(nodeId);
                freeCore -= occupiedCore;
                assert freeCore <= NodeConsciousScheduler.numCores;
                assert freeCore >= -(NodeConsciousScheduler.M - 1) * NodeConsciousScheduler.numCores;
                availableCores.set(nodeId, freeCore);

                if (scheduleUsingMemory) {
                    long memory = memories.get(nodeId);
                    memory -= addedMpn;
                    assert memory >= 0;
                    memories.set(nodeId, memory);
                }
            }
            if (ts.getEndTime() == newExpectedEndTime) {
                break;
            }
        }
    }
    
    protected void modifyTheTimeSlices(Job coexistingJob, Set<Integer> coexistingJobCoexistingJob, int currentTime, int endingJobId) {
        // 3. Modify the timeSlices
        boolean notEndFlag = (endingJobId == BLANK_JOBID);

        if (currentTime != coexistingJob.getOccupiedTimeInTimeSlices()) {
            //  2-1. Get old expectedEndTime            
            int oldExpectedEndTime = coexistingJob.getOccupiedTimeInTimeSlices();
            int newExpectedEndTime = calculateNewExpectedEndTime(currentTime, coexistingJob);
            int endEventOccuranceTime = coexistingJob.getEndEventOccuranceTimeNow();
            if (newExpectedEndTime < endEventOccuranceTime) { // This is not good implement            
                newExpectedEndTime = endEventOccuranceTime;                
            }
            coexistingJob.setOccupiedTimeInTimeSlices(newExpectedEndTime);
            // assert newExpectedEndTime <= oldExpectedEndTime;
            printDifferenceExpectedEndTime(oldExpectedEndTime, newExpectedEndTime, coexistingJob.getJobId());

            //  2-2. Update the timeslice between current and new expectedEndTime           
            int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
            refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, coexistingJob);                       
            makeTimeslices(currentTime);
            makeTimeslices(newExpectedEndTime);
            reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, coexistingJob);
        }
        if (!notEndFlag) {
            coexistingJobCoexistingJob.remove(endingJobId);
        }
    }     

    protected ArrayList<Event> modifyTheENDEventTimeForTheJobByJobId(int currentTime, int victimJobId, int OCStateLevel) {
        return modifyTheENDEventTimeForTheJobByJobId(currentTime, victimJobId, OCStateLevel, OCStateLevel);
    }
    
    protected ArrayList<Event> modifyTheENDEventTimeForTheJobByJobId(int currentTime, int victimJobId, int OCStateLevel, int netOCStateLevel) {
        ArrayList<Event> result = new ArrayList<Event>();

        /* 1. Modify the victim job's end time in event queue */
        Job victimJob = getJobByJobId(victimJobId); // O(N)
        int victimStartTime = victimJob.getStartTime();
        assert (victimStartTime >= 0 && victimStartTime <= currentTime) || victimStartTime == UNSTARTED;

        /*  1-1. Measure the executing time at current time for each victim jobs. */
        measureCurrentExecutingTime(currentTime, victimJob);
        victimJob.setPreviousMeasuredTime(currentTime);

        /*  1-2. Calculate new trueEndTime */
        int currentOCStateLevel = victimJob.getOCStateLevel();
        assert (currentOCStateLevel + 1 == OCStateLevel) || (currentOCStateLevel == OCStateLevel);
        /* debug */
        printOCStateLevelTransition(currentOCStateLevel, OCStateLevel, victimJobId);
        int oldTrueEndTime = victimJob.getEndEventOccuranceTimeNow();
        victimJob.setOCStateLevel(OCStateLevel);
        victimJob.setNetOCStateLevel(netOCStateLevel);
        victimJob.setJobAffinityRatio(calculateMaxDegradationRatio(victimJob, victimJob.getCoexistingJobs()));
        victimJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(victimJob));
        int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);
        assert oldTrueEndTime <= trueEndTime;
        victimJob.setOCStateLevel(currentOCStateLevel);
        
        /*  1-3. Rethrow the END event set the time */

        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
        if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime) {
            printThrownEvent(currentTime, trueEndTime, victimJob, EventType.END);
            result.add(new Event(EventType.END, trueEndTime, victimJob));
            victimJob.setEndEventOccuranceTimeNow(trueEndTime);
            printThrownEvent(currentTime, trueEndTime, victimJob, EventType.DELETE_FROM_BEGINNING);
            result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
        }
        return result;
    }

    
    protected ArrayList<Event> modifyTheENDEventTimeForTheJob(int currentTime, Job victimJob, int OCStateLevel) {
        return modifyTheENDEventTimeForTheJob(currentTime, victimJob, OCStateLevel, OCStateLevel, false);
    }
    
    protected ArrayList<Event> modifyTheENDEventTimeForTheJob(int currentTime, Job victimJob, int OCStateLevel, int netOCStateLevel, boolean opponentInteractiveJobFlag) {
        ArrayList<Event> result = new ArrayList<Event>();

        /* 1. Modify the victim job's end time in event queue */
        int victimJobId = victimJob.getJobId();
        int victimStartTime = victimJob.getStartTime();
        assert (victimStartTime >= 0 && victimStartTime <= currentTime) || victimStartTime == UNSTARTED;

        /*  1-1. Measure the executing time at current time for each victim jobs. */
        boolean interactiveJob = victimJob.isInteracitveJob();
        if (!interactiveJob) {
            measureCurrentExecutingTime(currentTime, victimJob);
        } else {
            measureCurrentExecutingTimeForActivation(currentTime, victimJob, victimJob.getNetOCStateLevel());
        }   
        victimJob.setPreviousMeasuredTime(currentTime);

        /*  1-2. Calculate new trueEndTime */
        int currentOCStateLevel = victimJob.getOCStateLevel();
        assert (currentOCStateLevel + 1 == OCStateLevel) || (currentOCStateLevel == OCStateLevel);
        /* debug */
        printOCStateLevelTransition(currentOCStateLevel, OCStateLevel, victimJobId);
        int oldTrueEndTime = victimJob.getEndEventOccuranceTimeNow();
        victimJob.setOCStateLevel(OCStateLevel);
        victimJob.setNetOCStateLevel(netOCStateLevel);                        

        /* TODO: must calculate ratio among activated jobs */
        double ratio = calculateMaxDegradationRatioForVictim(victimJob, victimJob.getCoexistingJobs());
        victimJob.setJobAffinityRatio(ratio);
        victimJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(victimJob));
        
        if (opponentInteractiveJobFlag) {
            ;;
        } else {
            /* TODO: must calculate trueEndTime only considering activated jobs */
            int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);
            //assert oldTrueEndTime <= trueEndTime+1;
            victimJob.setOCStateLevel(currentOCStateLevel);

            /*  1-3. Rethrow the END event set the time */
            //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
            //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && oldTrueEndTime < trueEndTime) {
            //if (currentTime != trueEndTime && oldTrueEndTime < trueEndTime) {
            boolean activationState = victimJob.isActivationState();
            if (!interactiveJob && currentTime < trueEndTime && oldTrueEndTime != trueEndTime && oldTrueEndTime != currentTime) {
                printThrownEvent(currentTime, trueEndTime, victimJob, EventType.END);
                result.add(new Event(EventType.END, trueEndTime, victimJob));
                victimJob.setEndEventOccuranceTimeNow(trueEndTime);
                printThrownEvent(currentTime, trueEndTime, victimJob, EventType.DELETE_FROM_BEGINNING);
                result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
            } else if (interactiveJob && activationState) {
                int oldDeactivateTime = victimJob.getCurrentDeactiveTime();
                int deactivateTime = calculateNewActualEndTimeForActivation(currentTime, victimJob);
                if (oldDeactivateTime != deactivateTime) {
                    printThrownEvent(currentTime, deactivateTime, victimJob, EventType.DELETE_DEACTIVE, 1);
                    result.add(new Event(EventType.DELETE_DEACTIVE, currentTime, victimJob, oldDeactivateTime));
                    victimJob.setCurrentDeactiveTime(deactivateTime);
                    printThrownEvent(currentTime, deactivateTime, victimJob, EventType.INT_DEACTIVATE, 1);
                    result.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, victimJob));
                }
            }
        }
        return result;
    } 
    
    protected void printDebugForCoexistingJob(Event ev, int coexistingJobId) {
        int currentTime = ev.getOccurrenceTime();
        Job endingJob = ev.getJob();
        int endingJobId = endingJob.getJobId();
        Set<Integer> coexistingJobs = endingJob.getCoexistingJobs();
        Job coexistingJob = getJobByJobId(coexistingJobId);
        /* Calculate new OCStateLevel for coexisting jobs */
        ArrayList<UsingNode> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();

        Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
        
        if (coexistingJobUsingNodeList == null) {
            System.out.println("debug) OCCURRED HERE, ending job Id: " + endingJobId + ", currentTime: " + currentTime);
            System.out.println("debug) usingNodeList: ");
            ArrayList<UsingNode> endingJobUsingNodeList = endingJob.getUsingNodesList();
            for (int i = 0; i < endingJobUsingNodeList.size(); ++i) {
                UsingNode node = endingJobUsingNodeList.get(i);
                System.out.print("\tNode" + node.getNodeNum() + ": ");
                ArrayList<Integer> cores = node.getUsingCoreNum();
                for (int j = 0; j < cores.size(); ++j) {
                    System.out.print(cores.get(j));
                    if (j != cores.size() - 1) {
                        System.out.print(", ");
                    }
                }
                System.out.println("");
            }
            System.out.println("");
            System.out.println("debug) coexisting jobs: " + coexistingJobs);
            System.out.println("debug) coexisting job Id: " + coexistingJobId);
            try {
                Thread.sleep(2000);
                throw new Exception();
            } catch (Exception ex) {
                Logger.getLogger(FCFSOC.class.getName()).log(Level.SEVERE, null, ex);
            }
            System.exit(1);
        }   
    }    

    protected ArrayList<Event> modifyTheENDEventTimeForNewCoexistingJobByJobId(int currentTime, int victimJobId, int OCStateLevel) {        
        int M = NodeConsciousScheduler.M;
        ArrayList<Event> result = new ArrayList<Event>();

        /* 1. Modify the victim job's end time in event queue */
        Job victimJob = getJobByJobId(victimJobId); // O(N)
        int victimStartTime = victimJob.getStartTime();
        assert (victimStartTime >= 0 && victimStartTime <= currentTime) || victimStartTime == UNSTARTED;

        /*  1-1. Measure the executing time at current time for each victim jobs. */
        boolean interactiveJob = victimJob.isInteracitveJob();
        if (!interactiveJob) {
            measureCurrentExecutingTime(currentTime, victimJob);
        } else {
            measureCurrentExecutingTimeForActivation(currentTime, victimJob, victimJob.getNetOCStateLevel());
        }
        victimJob.setPreviousMeasuredTime(currentTime);

        /*  1-2. Calculate new trueEndTime */
        int currentOCStateLevel = victimJob.getOCStateLevel();
        boolean OCStateLevelIncreasingflag = OCStateLevel >= currentOCStateLevel ? true : false;
        assert (M == 2 && OCStateLevelIncreasingflag && currentOCStateLevel + 1 == OCStateLevel) || (M == 2 && !OCStateLevelIncreasingflag && currentOCStateLevel - 1 == OCStateLevel) || (OCStateLevelIncreasingflag && currentOCStateLevel <= OCStateLevel) || (!OCStateLevelIncreasingflag && currentOCStateLevel > OCStateLevel);
        /* debug */
        printOCStateLevelTransition(currentOCStateLevel, OCStateLevel, victimJobId);
        int oldTrueEndTime = victimJob.getEndEventOccuranceTimeNow();
        victimJob.setOCStateLevel(OCStateLevel);
        int netOCStateLevel = calculateNewOCStateLevelForExecutingJob(victimJob, true);
        victimJob.setNetOCStateLevel(netOCStateLevel);
        victimJob.setJobAffinityRatio(calculateMaxDegradationRatio(victimJob, victimJob.getCoexistingJobs()));
        victimJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(victimJob));
        int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);
        //assert (OCStateLevelIncreasingflag && oldTrueEndTime <= trueEndTime+1) || (!OCStateLevelIncreasingflag && oldTrueEndTime >= trueEndTime);
        victimJob.setOCStateLevel(currentOCStateLevel);

        boolean actIntFlag = victimJob.isActivationState();
        /*  1-3. Rethrow the END event set the time */
        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
        if (!interactiveJob && currentOCStateLevel != OCStateLevel && currentTime != trueEndTime) {
            printThrownEvent(currentTime, trueEndTime, victimJob, EventType.END);
            result.add(new Event(EventType.END, trueEndTime, victimJob));
            victimJob.setEndEventOccuranceTimeNow(trueEndTime);
            printThrownEvent(currentTime, trueEndTime, victimJob, EventType.DELETE_FROM_BEGINNING);
            result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
        } else if (interactiveJob && actIntFlag) {
            int oldDeactivateTime = victimJob.getCurrentDeactiveTime();
            int deactivateTime = calculateNewActualEndTimeForActivation(currentTime, victimJob);
            if (oldDeactivateTime != deactivateTime) {
                printThrownEvent(currentTime, deactivateTime, victimJob, EventType.DELETE_DEACTIVE, 1);
                result.add(new Event(EventType.DELETE_DEACTIVE, currentTime, victimJob, oldDeactivateTime));
                victimJob.setCurrentDeactiveTime(deactivateTime);
                printThrownEvent(currentTime, deactivateTime, victimJob, EventType.INT_DEACTIVATE, 1);
                result.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, victimJob));
            }
        }
        return result;
    }

    private void printUsingNode(UsingNode usingNode, int endingJobId) {
            int nodeId = usingNode.getNodeNum();
            ArrayList<Integer> usingCores = usingNode.getUsingCoreNum();
            System.out.print("\t\tdebug)Job " +endingJobId + " used node " + nodeId + "(");
            for (int j = 0; j < usingCores.size(); ++j) {
                int coreId = usingCores.get(j);
                System.out.print(coreId);
                if (j != usingCores.size() - 1)
                    System.out.print(", ");
            }
            System.out.println(")");
        
    }

    private MaxAndMinMultiplicityCores obtainMaxMultiplicityCores(UsingNode usingNode) {
        int nodeId = usingNode.getNodeNum();
        CoreInfo maxCoreInfo = new CoreInfo();
        CoreInfo minCoreInfo = new CoreInfo();
        ArrayList<Integer> usingCores = usingNode.getUsingCoreNum();

        // nodeInfo managed by simulator
        NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeId);
        assert nodeId == nodeInfo.getNodeNum();
        ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
        
        int minCoreMultiplicity = UNUPDATED;
        int maxCoreMultiplicity = UNUPDATED;
        for (int i = 0; i < occupiedCores.size(); ++i) {
            CoreInfo coreInfo = occupiedCores.get(i);
            int multiplicity = coreInfo.getJobList().size();
            if (minCoreMultiplicity == UNUPDATED && maxCoreMultiplicity == UNUPDATED) {
                minCoreMultiplicity = multiplicity;
                maxCoreMultiplicity = multiplicity;
                maxCoreInfo = coreInfo;
                minCoreInfo = coreInfo;
            }
            
            if (multiplicity > maxCoreMultiplicity) {
                maxCoreMultiplicity = multiplicity;
                maxCoreInfo = coreInfo;
            }

            if (multiplicity < minCoreMultiplicity) {
                minCoreMultiplicity = multiplicity;
                minCoreInfo = coreInfo;                
            }
        }
        
        return new MaxAndMinMultiplicityCores(maxCoreInfo, minCoreInfo);
    }

    private Collection<? extends Event> doMigrate(MaxAndMinMultiplicityCores maxAndMinMultiplicityCores, int nodeId, int currentTime, int endingJobId) {
        ArrayList<Event> result = new ArrayList<Event>();
        
        CoreInfo maxCoreInfo = maxAndMinMultiplicityCores.getMaxCoreInfo();
        CoreInfo minCoreInfo = maxAndMinMultiplicityCores.getMinCoreInfo();
        
        ArrayList<Integer> candidateMigratingJobs = (ArrayList<Integer>) maxCoreInfo.getJobList().clone();
        candidateMigratingJobs.removeAll(minCoreInfo.getJobList());
        assert candidateMigratingJobs.size() >= 2;

        int migratingJobId = UNUPDATED;
        double currentLeastRatio = 1 << 30;
        int currentLeastCount = 1 << 30;
        for (int i = 0; i < candidateMigratingJobs.size(); ++i) {
            int tmpMigratingJobId = candidateMigratingJobs.get(i);
            assert maxCoreInfo.getJobList().contains(tmpMigratingJobId);
            Job tmpMigratingJob = NodeConsciousScheduler.sim.getJobMap().get(tmpMigratingJobId);
            int tmpMigratingJobGroup = tmpMigratingJob.getMatchingGroup();
            ScheduleConsiderJobType scjt = NodeConsciousScheduler.sim.getScheduleConsiderJobType();
            
            if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {
                double localRatio = UNSPECIFIED;
                for (Integer victimJobId: minCoreInfo.getJobList()) {
                    int victimJobGroup = NodeConsciousScheduler.sim.getJobMap().get(victimJobId).getMatchingGroup();
                    localRatio = NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(tmpMigratingJobGroup, victimJobGroup));
                    localRatio = max(localRatio, NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(victimJobGroup, tmpMigratingJobGroup)));
                }
                if (localRatio < currentLeastRatio) {
                    currentLeastRatio = localRatio;
                    migratingJobId = tmpMigratingJobId;
                }
            } else if (scjt != ScheduleConsiderJobType.NOTHING) {
                boolean preferBatchInt = scjt == ScheduleConsiderJobType.BATCH_INT;
                boolean migrateJobIsIntJob = tmpMigratingJob.isInteracitveJob();
                
                boolean targetJobIsIntJob = false;
                if (preferBatchInt && migrateJobIsIntJob) {
                    targetJobIsIntJob = false;
                } else if (preferBatchInt && !migrateJobIsIntJob) {
                    targetJobIsIntJob = true;
                } else if (!preferBatchInt && migrateJobIsIntJob) {
                    targetJobIsIntJob = true;
                } else if (!preferBatchInt && !migrateJobIsIntJob) {
                    targetJobIsIntJob = false;
                } 
                
                int localCount = START_COUNT;
                for (Integer victimJobId: minCoreInfo.getJobList()) {
                    Job victimJob = NodeConsciousScheduler.sim.getJobMap().get(victimJobId);
                    boolean victimIntJob = victimJob.isInteracitveJob();
                    if ( (targetJobIsIntJob && victimIntJob) || (!targetJobIsIntJob && !victimIntJob)) {
                        ++localCount;
                    }
                }
                if (localCount < currentLeastCount) {
                    currentLeastCount = localCount;
                    migratingJobId = tmpMigratingJobId;
                }                
            } else {
                    migratingJobId = tmpMigratingJobId;
            }

            /*            
            if (!minCoreInfo.getJobList().contains(tmpMigratingJobId)) {
                break;
            }            
            */
        }
        assert migratingJobId != UNUPDATED;
        Job migratingJob = getJobByJobId(migratingJobId);
        printMigrationInfo(migratingJobId, maxCoreInfo, minCoreInfo);
        outputResultForVis(migratingJob, currentTime);
        //printMigratingJobCore
        
        // Register the info.
        assert maxCoreInfo.getJobList().contains((Integer) migratingJobId);
        assert !minCoreInfo.getJobList().contains((Integer) migratingJobId);        
        maxCoreInfo.getJobList().remove((Integer) migratingJobId);
        minCoreInfo.getJobList().add((Integer) migratingJobId);
        UsingNode migratingJobUsingNode = new UsingNode();
        for (UsingNode tmpUsingNode: migratingJob.getUsingNodesList()) {
            int tmpNodeId = tmpUsingNode.getNodeNum();
            if (tmpNodeId == nodeId) {
                migratingJobUsingNode = tmpUsingNode;
                break;
            }
        }

        ArrayList<Integer> usingCores = migratingJobUsingNode.getUsingCoreNum();
        assert usingCores.contains((Integer) maxCoreInfo.getCoreId());
        assert !usingCores.contains((Integer) minCoreInfo.getCoreId());
        usingCores.remove((Integer) maxCoreInfo.getCoreId());
        usingCores.add((Integer) minCoreInfo.getCoreId());
        migratingJob.setPreviousMigratingTime(currentTime);
        
        // For migrating job, reobtain the coexisting job
        HashSet<Integer> newCoexistingJobs = obtainCoexistingJobsForJob(migratingJob);
        HashSet<Integer> deletedCoexistingJobs = new HashSet<Integer>();
        //deletedCoexistingJobs.addAll(minCoreInfo.getJobList());
        deletedCoexistingJobs.addAll(maxCoreInfo.getJobList());
        deletedCoexistingJobs.removeAll(newCoexistingJobs);
        deletedCoexistingJobs.remove((Integer) migratingJobId);
        
        // debug
        NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobs = new NewAndDeletedCoexistingJobs(newCoexistingJobs, deletedCoexistingJobs);
        printNewAndDeletedCoexistingJobs(newAndDeletedCoexistingJobs, migratingJob);
        
        for (int deletedCoexistingJobId : deletedCoexistingJobs) {

            Job coexistingJob = getJobByJobId(deletedCoexistingJobId);
            int coexistingJobId = coexistingJob.getJobId();
            ArrayList<UsingNode> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();
            int OCStateLevelCoexistingJob = checkMultiplicityAlongNodes(coexistingJobUsingNodeList, BLANK_JOBID, coexistingJobId);

            // 3.2 Modify the END Event time
            modifyTheENDEventTime(coexistingJob, coexistingJobId, currentTime, OCStateLevelCoexistingJob, result);

            /* 4. Modify the TimeSlices */
            Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
            modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, migratingJobId);
        }

        for (int newCoexistingJobId : newCoexistingJobs) {
                // 3. Modify the END Event time 
            //  3.1 Check the OCStateLevel
            Job coexistingJob = getJobByJobId(newCoexistingJobId);
            int coexistingJobId = coexistingJob.getJobId();
            ArrayList<UsingNode> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();
            int OCStateLevelCoexistingJob = checkMultiplicityAlongNodes(coexistingJobUsingNodeList, BLANK_JOBID, coexistingJobId);

                // 3.2 Modify the END Event time
            // modifyTheENDEventTime(coexistingJob, coexistingJobId, currentTime, OCStateLevelCoexistingJob, result);
            result.addAll(modifyTheENDEventTimeForNewCoexistingJobByJobId(currentTime, coexistingJobId, OCStateLevelCoexistingJob));
            coexistingJob.setOCStateLevel(OCStateLevelCoexistingJob);
            /* 4. Modify the TimeSlices */
            Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
            modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, endingJobId);

            coexistingJobCoexistingJob.add((Integer) migratingJobId);
        }

        ArrayList<UsingNode> migratingJobUsingNodeList = migratingJob.getUsingNodesList();
        int newOCStateLevel = checkMultiplicityAlongNodes(migratingJobUsingNodeList, BLANK_JOBID, migratingJobId);
        modifyTheENDEventTime(migratingJob, migratingJobId, currentTime, newOCStateLevel, result);

        Set<Integer> migratingJobCoexistingJob = migratingJob.getCoexistingJobs();
        modifyTheTimeSlices(migratingJob, migratingJobCoexistingJob, currentTime, endingJobId);

        migratingJobCoexistingJob.addAll(newCoexistingJobs);
        migratingJobCoexistingJob.removeAll(deletedCoexistingJobs);
        
        double ratio = calculateMaxDegradationRatioForVictim(migratingJob, migratingJobCoexistingJob);
        migratingJob.setJobAffinityRatio(ratio);
        migratingJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(migratingJob));
        
        return result;
    }

    private void printMigrationInfo(int migratingJobId, CoreInfo maxCoreInfo, CoreInfo minCoreInfo) {
        System.out.println("\t\t\tdebug)migrating job " + migratingJobId
                + " from core " + maxCoreInfo.getCoreId() + "(" + maxCoreInfo.getJobList().size() + ")"
                +   " to core " + minCoreInfo.getCoreId() + "(" + minCoreInfo.getJobList().size() + ")");
    }

    private HashSet<Integer> obtainCoexistingJobsForJob(Job job) {
        int jobId = job.getJobId();
        ArrayList<UsingNode> usingNodeList = job.getUsingNodesList();
        HashSet<Integer> result = new HashSet<Integer>();
        
        for (int i = 0; i < usingNodeList.size(); ++i) {
            UsingNode usingNode = usingNodeList.get(i);
            int nodeId = usingNode.getNodeNum();
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeId);
            assert nodeId == nodeInfo.getNodeNum();
            ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < occupiedCores.size(); ++j) {
                CoreInfo coreInfo = occupiedCores.get(j);
                ArrayList<Integer> jobList = coreInfo.getJobList();
                if (jobList.contains(jobId)) {
                    int coreId = coreInfo.getCoreId();
                    assert usingNode.getUsingCoreNum().contains(coreId);
                    result.addAll(jobList);
                }
            }
        }
        result.remove(jobId);
        return result;
    }

 
    protected int calculateVictimNewOCStateLevel(Job victimJob, int requiredCoresPerNode, ArrayList<Integer> assignNodesNo) {
        return calculateVictimNewOCStateLevel(victimJob, requiredCoresPerNode, assignNodesNo, false, false);
    }

    static int calculateVictimNewOCStateLevel(Job victimJob, int requiredCoresPerNode, ArrayList<Integer> assignNodesNo, boolean calculateNetOCStateLevel, boolean opponentInteractiveJobFlag) {
        int newOCStateLevel = UNUPDATED;
        
        int currentOCStateLevel = victimJob.getOCStateLevel();
        int currentNetOCStateLevel = victimJob.getNetOCStateLevel();
        
        ArrayList<NodeInfo> allNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        
        int victimJobId = victimJob.getJobId();
        
        // 1. Check the all assignNodes
        int OCStateLevelAlongNodes = UNUPDATED;
        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeId = assignNodesNo.get(i);
            NodeInfo node = allNodeInfo.get(nodeId);
            assert nodeId == node.getNodeNum();
             
            // if the victim job doesn't use the node, skip
            if (!node.getExecutingJobIds().contains(victimJobId))
                continue;

            // Check the least requiredCoresPerNode CoreInfos
            int OCStateLevelAlongCores = UNUPDATED;
            ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();

            // debug
            printOccupiedCores(nodeId, occupiedCores);

            
            for (int j = 0; j < requiredCoresPerNode; ++j) {
                CoreInfo coreInfo = occupiedCores.get(j);
                
                // if the victim job doesn't use the core, skip
                if (!coreInfo.getJobList().contains(victimJobId))
                    continue;
                
                ArrayList<Integer> jobList = coreInfo.getJobList();
                int OCStateLevelOnTheCore = jobList.size();
                if (calculateNetOCStateLevel) {
                    int numIntJob = 0;
                    for (int k = 0; k < jobList.size(); ++k) {
                        Job vjob = getJobByJobId(jobList.get(k));
                        boolean intFlag = vjob.isInteracitveJob();
                        boolean actFlag = vjob.isActivationState();
                        if (intFlag && !actFlag) {
                            ++numIntJob;
                        }
                    }
                    OCStateLevelOnTheCore -= numIntJob;
                }
                assert OCStateLevelOnTheCore <= currentOCStateLevel;
                if (calculateNetOCStateLevel) {
                    if (opponentInteractiveJobFlag) {
                        ;;;
                    } else {
                        ++OCStateLevelOnTheCore;                        
                    }
                } else {
                    ++OCStateLevelOnTheCore;
                } 
                OCStateLevelAlongCores = max(OCStateLevelAlongCores, OCStateLevelOnTheCore);
            }
            OCStateLevelAlongNodes = max(OCStateLevelAlongNodes, OCStateLevelAlongCores);
        }
        if (calculateNetOCStateLevel) {
            newOCStateLevel = max(currentNetOCStateLevel, OCStateLevelAlongNodes);
        } else {
            newOCStateLevel = max(currentOCStateLevel, OCStateLevelAlongNodes);
        }

        return newOCStateLevel;
    }

    static int calculateNewOCStateLevelForNewJob(Job job, int requiredCoresPerNode, ArrayList<Integer> assignNodesNo, boolean calculateNetOCStateLevel) {
        int newOCStateLevel = UNUPDATED;
        
        int currentOCStateLevel = job.getOCStateLevel();
        
        ArrayList<NodeInfo> allNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        
        int victimJobId = job.getJobId();
        boolean opponentInteractiveJobFlag = job.isInteracitveJob();
        
        // 1. Check the all assignNodes
        int OCStateLevelAlongNodes = UNUPDATED;
        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeId = assignNodesNo.get(i);
            NodeInfo node = allNodeInfo.get(nodeId);
            assert nodeId == node.getNodeNum();
             

            // Check the least requiredCoresPerNode CoreInfos
            int OCStateLevelAlongCores = UNUPDATED;
            ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();

            // debug
            printOccupiedCores(nodeId, occupiedCores);

            
            for (int j = 0; j < requiredCoresPerNode; ++j) {
                CoreInfo coreInfo = occupiedCores.get(j);
                
                
                ArrayList<Integer> jobList = coreInfo.getJobList();
                int OCStateLevelOnTheCore = jobList.size();
                if (calculateNetOCStateLevel) {
                    int numIntJob = 0;
                    for (int k = 0; k < jobList.size(); ++k) {
                        Job vjob = getJobByJobId(jobList.get(k));
                        boolean intFlag = vjob.isInteracitveJob();
                        boolean actFlag = vjob.isActivationState();
                        if (intFlag && !actFlag) {
                            ++numIntJob;
                        }
                    }
                    OCStateLevelOnTheCore -= numIntJob;
                }
                assert OCStateLevelOnTheCore <= currentOCStateLevel;
                if (calculateNetOCStateLevel) {
                    if (opponentInteractiveJobFlag) {
                        ;;
                    } else {
                        ++OCStateLevelOnTheCore;                        
                    }
                } else {
                    ++OCStateLevelOnTheCore;
                }
                OCStateLevelOnTheCore = max(1, OCStateLevelOnTheCore);
                OCStateLevelAlongCores = max(OCStateLevelAlongCores, OCStateLevelOnTheCore);
            }
            OCStateLevelAlongNodes = max(OCStateLevelAlongNodes, OCStateLevelAlongCores);
        }
        
        return OCStateLevelAlongNodes;
    }

    static int calculateNewOCStateLevelForExecutingJob(Job job, boolean calculateNetOCStateLevel) {
        int newOCStateLevel = UNUPDATED;
        
        int currentOCStateLevel = job.getOCStateLevel();
        
        ArrayList<NodeInfo> allNodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo();
        
        int victimJobId = job.getJobId();
        boolean opponentInteractiveJobFlag = job.isInteracitveJob();
        boolean activatedFlag = job.isActivationState();
        
        if (opponentInteractiveJobFlag && !activatedFlag) {
            return NOTACTIVATED;
        }
        
        ArrayList<UsingNode> usingNodesList = job.getUsingNodesList();
        int requiredCoresPerNode = job.getRequiredCoresPerNode();
        
        System.out.println("\tJobId: " + victimJobId);
        // 1. Check the all assignNodes
        int OCStateLevelAlongNodes = UNUPDATED;
        for (UsingNode usingNode: usingNodesList) {            
            int nodeId = usingNode.getNodeNum();
            NodeInfo node = allNodeInfo.get(nodeId);
            assert nodeId == node.getNodeNum();
             

            // Check the least requiredCoresPerNode CoreInfos
            int OCStateLevelAlongCores = UNUPDATED;
            ArrayList<CoreInfo> occupiedCores = node.getOccupiedCores();
            ArrayList<Integer> usingCoreNum = usingNode.getUsingCoreNum();
            ArrayList<CoreInfo> usingCoreInfo = new ArrayList();
            
            for (int idx = 0; idx < occupiedCores.size(); ++idx) {
                CoreInfo coreInfo = occupiedCores.get(idx);
                int coreId = coreInfo.getCoreId();
                if (usingCoreNum.contains(coreId)) {
                    usingCoreInfo.add(coreInfo);
                }
            }

            // debug
            printOccupiedCores(nodeId, usingCoreInfo, 2);
            
            assert usingCoreInfo.size() == requiredCoresPerNode;
            
            for (int j = 0; j < requiredCoresPerNode; ++j) {
                CoreInfo coreInfo = usingCoreInfo.get(j);
                
                
                ArrayList<Integer> jobList = coreInfo.getJobList();
                assert jobList.contains(victimJobId);
                int OCStateLevelOnTheCore = jobList.size();
                if (calculateNetOCStateLevel) {
                    int numIntJob = 0;
                    for (int k = 0; k < jobList.size(); ++k) {
                        int ji = jobList.get(k);
                        Job vjob = getJobByJobId(ji);
                        boolean intFlag = vjob.isInteracitveJob();
                        boolean actFlag = vjob.isActivationState();
                        if (intFlag && !actFlag) {
                            ++numIntJob;
                        }
                    }
                    OCStateLevelOnTheCore -= numIntJob;
                    OCStateLevelOnTheCore = max(1, OCStateLevelOnTheCore);
                }
                assert OCStateLevelOnTheCore <= currentOCStateLevel;
                /*
                if (calculateNetOCStateLevel) {
                    if (opponentInteractiveJobFlag) {
                        ;;
                    } else {
                        ++OCStateLevelOnTheCore;                        
                    }
                } else {
                    ++OCStateLevelOnTheCore;
                }
                */
                OCStateLevelAlongCores = max(OCStateLevelAlongCores, OCStateLevelOnTheCore);
            }
            OCStateLevelAlongNodes = max(OCStateLevelAlongNodes, OCStateLevelAlongCores);
        }
        
        return OCStateLevelAlongNodes;
    }
    
    static void printOccupiedCores(int nodeId, ArrayList<CoreInfo> occupiedCores) {
        System.out.print("\tdebug) nodeId: " + nodeId + ", m(coreId), ");
        for (int i = 0; i < occupiedCores.size(); ++i) {
            CoreInfo coreInfo = occupiedCores.get(i);
            int coreId = coreInfo.getCoreId();
            ArrayList<Integer> jobList = coreInfo.getJobList();
            System.out.print(jobList.size() + "(" + coreId + ")");
            if (i == occupiedCores.size()-1) break;
            System.out.print(", ");
        }
        System.out.println("");
    }

    static void printOccupiedCores(int nodeId, ArrayList<CoreInfo> occupiedCores, int tubnum) {
        for (int i = 0; i < tubnum; ++i) {
            System.out.print("\t");            
        }
        printOccupiedCores(nodeId, occupiedCores);

    }    
    
    private void outputResultForVis(Job migratingJob, int currentTime) {
        NodeConsciousScheduler.sim.outputResultForVis(migratingJob, currentTime);
    }

    protected double calculateMaxDegradationRatioForVictim(Job victimJob, Set<Integer> coexistingJobs) {    
        boolean considerJobMatching = NodeConsciousScheduler.sim.isConsiderJobMatching();
        if (!considerJobMatching) return 1.0;

        int victimJobGroup = victimJob.getMatchingGroup();
        double ratio = 0;
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            int coexistingJobGroup = coexistingJob.getMatchingGroup();
            double localRatio = NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(victimJobGroup, coexistingJobGroup));
            ratio = max(ratio, localRatio);
        }
        if (ratio == 0) ratio = 1.0;
        return ratio;
    }

    protected double calculateOsubOverheadRatioForVictim(Job victimJob) {    
        Constants.OsubOverheadModelType oomt = NodeConsciousScheduler.sim.getOsubOverheadModelType();
        boolean consideOsubOverhead = (oomt != Constants.OsubOverheadModelType.NOTHING);
        if (!consideOsubOverhead) return 1.0;

        double ratio = 0;
        
        int netOCStateLevel = victimJob.getNetOCStateLevel();
        if (netOCStateLevel == 1) {
            ratio = 1.0;
        } else if (oomt == Constants.OsubOverheadModelType.CONST) {         
            ratio = NodeConsciousScheduler.sim.getOsubOverheadConst();
        } else if (oomt == Constants.OsubOverheadModelType.CONSIDER_MULT) {
            // TODO
            ratio = NodeConsciousScheduler.sim.getOsubOverheadConst();
        }
        if (ratio == 0) ratio = 1.0;
        return ratio;
    }

    protected void modifyTheENDEventTime(Job coexistingJob, int coexistingJobId, int currentTime, int OCStateLevel, ArrayList<Event> result, boolean endFlag, int endingJobId) {
        int coexistingStartTime = coexistingJob.getStartTime();
        assert coexistingStartTime >= 0;
        assert coexistingStartTime <= currentTime;

        //  1-1. Measure the executing time at current time for each victim jobs.
        boolean interactiveJobs = coexistingJob.isInteracitveJob();
        if (interactiveJobs) {
            measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, OCStateLevel);
        } else {
            measureCurrentExecutingTime(currentTime, coexistingJob);
        }
        coexistingJob.setPreviousMeasuredTime(currentTime);

        //  1-2. Calculate new trueEndTime
        int currentOCStateLevel = coexistingJob.getOCStateLevel();
        assert (currentOCStateLevel - 1 == OCStateLevel) || (currentOCStateLevel == OCStateLevel) || (currentOCStateLevel > OCStateLevel);
        // debug
        printOCStateLevelTransition(currentOCStateLevel, OCStateLevel, coexistingJobId);
        int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
        Set<Integer> coexistingCoexistingJobs = coexistingJob.getCoexistingJobs();
        double ratio = coexistingJob.getJobAffinityRatio();
        if (endFlag) {
            coexistingCoexistingJobs.remove(endingJobId);
            ratio = calculateMaxDegradationRatioForVictim(coexistingJob, coexistingCoexistingJobs);
            ArrayList<Integer> nodeList = new ArrayList();
            for (UsingNode usingNode: coexistingJob.getUsingNodesList()) {
                nodeList.add(usingNode.getNodeNum());
            }
            int netOCStateLevel = calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
            coexistingJob.setNetOCStateLevel(netOCStateLevel);
        }
        coexistingJob.setOCStateLevel(OCStateLevel);
        coexistingJob.setJobAffinityRatio(ratio);
        coexistingJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(coexistingJob));
        int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
        coexistingCoexistingJobs.add(endingJobId);
//        assert trueEndTime <= oldTrueEndTime;

        //  1-3. Rethrow the END event set the time
        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && currentTime != oldTrueEndTime) {
        // TODO: treatment of int jobs
        boolean actStateFlag = coexistingJob.isActivationState();
        if (!interactiveJobs && (currentTime != trueEndTime && currentTime != oldTrueEndTime && trueEndTime != oldTrueEndTime)) {
            printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
            result.add(new Event(EventType.END, trueEndTime, coexistingJob));
            coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
            printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
            result.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
        } else if (interactiveJobs && actStateFlag) {
            int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
            int deactivateTime = Scheduler.calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
            if (oldDeactivateTime != deactivateTime) {
                printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                result.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                coexistingJob.setCurrentDeactiveTime(deactivateTime);
                printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                result.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
            }            
        }
    }

    private void modifyTimeSlicesDueToSlowsDonwJob(int currentTime, int occupiedTimeInTimeSeries, int endEventOccuranceTimeNow, int nodeNum, Job job) {
        makeTimeslices(currentTime);
        makeTimeslices(endEventOccuranceTimeNow);

        for (int i = completedTimeSlices.size()-1; i >= 0; --i) {
            TimeSlice ts = completedTimeSlices.get(i);
            int startTime = ts.getStartTime();
            int endTime = ts.getEndTime();
            if (endTime == occupiedTimeInTimeSeries) {
                break;
            } else if (endTime < occupiedTimeInTimeSeries) {
                break;  
            } else if (startTime >= endEventOccuranceTimeNow) {
                break; 
            } else {
                ts.assignResourcesAtNode(nodeNum, job);
            }
        }
        
        for (TimeSlice ts: timeSlices) {
            int startTime = ts.getStartTime();
            assert startTime >= currentTime;
            if (startTime < occupiedTimeInTimeSeries) continue;
            if (startTime >= endEventOccuranceTimeNow) {
                break;
            }
            ts.assignResourcesAtNode(nodeNum, job);
        }
    }
    protected Set<Integer> cloneCoexistingJobs(Set<Integer> coexistingJobs) {
        Set<Integer> clonedObject = new HashSet<Integer>();

        for(int jobId: coexistingJobs) {
            clonedObject.add(jobId);
        }
        return clonedObject;
    } 
    protected void calculatePriorityForNodes(ArrayList<VacantNode> canExecuteNodes, Job job) {
        assert NodeConsciousScheduler.sim.isUsingAffinityForSchedule();
        
        for (VacantNode vn: canExecuteNodes) {
            vn.setPriority(UNSPECIFIED);
        }
        
        int jobGroup = job.getMatchingGroup();
        
        for (VacantNode vn: canExecuteNodes) {
            int nodeId = vn.getNodeNo();
            NodeInfo node = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeId);
            Set<Integer> currentExecutingJobIds = node.getExecutingJobIds();
            double ratio = calculateMaxDegradationRatio(job, currentExecutingJobIds);
            /*
            for (Integer currentExecutingJobId: currentExecutingJobIds) {                            
                int currentExecutingJobGroup = getJobByJobId(currentExecutingJobId).getMatchingGroup();
                double localRatio = NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(jobGroup, currentExecutingJobGroup));
                localRatio = max(localRatio, NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(currentExecutingJobGroup, jobGroup)));
                ratio = max(ratio, localRatio);
            }
            if (ratio == UNSPECIFIED) {
                ratio = 1.0;
            }
            */            
            vn.setPriority(ratio);
        }
    }

    private void calculatePriorityForCores(ArrayList<CoreInfo> occupiedCores, Job job) {
        assert NodeConsciousScheduler.sim.isUsingAffinityForSchedule();
        
        for (CoreInfo ci: occupiedCores) {
            ci.setPriority(UNSPECIFIED);
        }
        
//        int jobGroup = job.getMatchingGroup();
        
        for (CoreInfo ci: occupiedCores) {
            int coreId = ci.getCoreId();
            ArrayList<Integer> currentExecutingJobIds = ci.getJobList();
/*
            double ratio = UNSPECIFIED;
            for (Integer currentExecutingJobId: currentExecutingJobIds) {                            
                int currentExecutingJobGroup = getJobByJobId(currentExecutingJobId).getMatchingGroup();
                double localRatio = NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(jobGroup, currentExecutingJobGroup));
                localRatio = max(localRatio, NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(currentExecutingJobGroup, jobGroup)));
                ratio = max(ratio, localRatio);
            }
            if (ratio == UNSPECIFIED) {
                ratio = 1.0;
            }
            */
            double ratio = calculateMaxDegradationRatio(job, currentExecutingJobIds);
            ci.setPriority(ratio);
        }
    }

    private void calculatePriorityForCores(ArrayList<CoreInfo> occupiedCores, Job job, ScheduleConsiderJobType scjt, Boolean intJob) {
        assert NodeConsciousScheduler.sim.getScheduleConsiderJobType() != ScheduleConsiderJobType.NOTHING;

        boolean targetJobIsIntJob = false;
        
        if (intJob && scjt == ScheduleConsiderJobType.INT_INT) {
            targetJobIsIntJob = true;
        } else if (intJob && scjt == ScheduleConsiderJobType.BATCH_INT){
            targetJobIsIntJob = false;            
        } else if (!intJob && scjt == ScheduleConsiderJobType.BATCH_INT) {
            targetJobIsIntJob = true;
        } else if (!intJob && scjt == ScheduleConsiderJobType.INT_INT) {
            targetJobIsIntJob = false;
        }
        
        
        for (CoreInfo ci: occupiedCores) {
            ci.setPriority(UNSPECIFIED);
        }
        
        
        for (CoreInfo ci: occupiedCores) {
            int coreId = ci.getCoreId();
            ArrayList<Integer> currentExecutingJobIds = ci.getJobList();
            
            int numTargetJobType = countTargetJobType(targetJobIsIntJob, currentExecutingJobIds);

            ci.setPriority((double)1/numTargetJobType);
        }
    }    
    
    /* This method calculates the worst ratio if a new job overcommits with n coexisting jobs. */
    /* The ratio is the worst one of 2*(n+1) values.  */
    protected double calculateMaxDegradationRatio(Job victimJob, Set<Integer> coexistingJobs) {    
        boolean considerJobMatching = NodeConsciousScheduler.sim.isConsiderJobMatching();
        if (!considerJobMatching) return 1.0;

        int victimJobGroup = victimJob.getMatchingGroup();
        double ratio = UNSPECIFIED;
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            int coexistingJobGroup = coexistingJob.getMatchingGroup();
            double localRatio = NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(victimJobGroup, coexistingJobGroup));
            localRatio =max(localRatio, NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(coexistingJobGroup, victimJobGroup)));
            ratio = max(ratio, localRatio);
        }
        if (ratio == UNSPECIFIED) ratio = 1.0;
        return ratio;
    }

    protected double calculateMaxDegradationRatio(Job victimJob, ArrayList<Integer> coexistingJobs) {    
        boolean considerJobMatching = NodeConsciousScheduler.sim.isConsiderJobMatching();
        if (!considerJobMatching) return 1.0;

        int victimJobGroup = victimJob.getMatchingGroup();
        double ratio = UNSPECIFIED;
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            int coexistingJobGroup = coexistingJob.getMatchingGroup();
            double localRatio = NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(victimJobGroup, coexistingJobGroup));
            localRatio =max(localRatio, NodeConsciousScheduler.sim.jobMatchingTable.get(new JobMatching(coexistingJobGroup, victimJobGroup)));
            ratio = max(ratio, localRatio);
        }
        if (ratio == UNSPECIFIED) ratio = 1.0;
        return ratio;
    }

    private void uniteTimeSlices(LinkedList<TimeSlice> timeSlices) {
        int size = timeSlices.size();
        ArrayList<Integer> deleteList = new ArrayList<Integer>();
        for (int i = 0; i < size - 1; ++i) {
            TimeSlice currentTs = timeSlices.get(i);
            TimeSlice nextTs = timeSlices.get(i+1);
            
            boolean flag = checkTimeSlicesAreSame(currentTs, nextTs);
            if (flag) {
                deleteList.add(i+1);
            }
        }
        
        int deleteListSize = deleteList.size();
        for (int i = deleteListSize - 1; i >= 0; --i) {
            int deleteIndex = deleteList.get(i);
            deleteAndModifyTimeSliceForUnite(timeSlices, deleteIndex);
        }
    }    

    private boolean checkTimeSlicesAreSame(TimeSlice currentTs, TimeSlice nextTs) {
        boolean ret = true;
        
        ArrayList<Integer> currentAvailableCores = currentTs.getAvailableCores();
        ArrayList<Integer> nextAvailableCores = nextTs.getAvailableCores();

        // freeCores
        for (int n = 0; n < NodeConsciousScheduler.numNodes; ++n) {            
            if (currentAvailableCores.get(n) != nextAvailableCores.get(n)) {
                ret = false;
                break;
            }
        }
        
        // memory
        if (NodeConsciousScheduler.sim.isScheduleUsingMemory()) {
            ArrayList<Long> currentAvailableMemory = currentTs.getAvailableMemory();
            ArrayList<Long> nextAvailableMemory = nextTs.getAvailableMemory();

            // node
            for (int n = 0; n < NodeConsciousScheduler.numNodes; ++n) {
                if (currentAvailableMemory.get(n) != nextAvailableMemory.get(n)) {
                    ret = false;
                    break;
                }
            }
        }
        return ret;
    }

    private void deleteAndModifyTimeSliceForUnite(LinkedList<TimeSlice> timeSlices, int deleteIndex) {
        TimeSlice deleteTimeSlice = timeSlices.get(deleteIndex);
        TimeSlice modifyTimeSlice = timeSlices.get(deleteIndex-1);
        
        assert deleteTimeSlice.getStartTime() == modifyTimeSlice.getEndTime();
        
        int startTime = modifyTimeSlice.getStartTime();
        int newEndTime = deleteTimeSlice.getEndTime();
        
        modifyTimeSlice.setEndTime(newEndTime);
        modifyTimeSlice.setDuration(newEndTime - startTime);
        
        timeSlices.remove(deleteIndex);

    }

    ArrayList<Event> activateInteractiveJob(Event ev) {
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // TODO
        // Get job Info
        // Timing coexisting jobs
        // Activation the job and coexisting jobs
        
        /* Fix the finish time */
        int currentTime = ev.getOccurrenceTime();
        Job job = ev.getJob();
        int jobId = job.getJobId();
        boolean interactiveJob = job.isInteracitveJob();
        boolean activationState = job.isActivationState();
        assert !activationState;
        
        calcWastedResource(currentTime);

        if (checkNoActivationJob(job)) {
            int epilogTime = job.getEpilogTIme();
            int endTime = epilogTime + currentTime;
            job.setEndEventOccuranceTimeNow(endTime);
            printThrownEvent(currentTime, endTime, job, EventType.END);
            evs.add(new Event(EventType.END, endTime, job));
            return evs;
        }
        
        int OCStateLevel = job.getOCStateLevel();
        Set<Integer> coexistingJobs = job.getCoexistingJobs();
        System.out.println("\tActivate jobId: " + jobId + ", victim jobId: " + coexistingJobs);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);           
            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            if (!intJobFlag) {
                measureCurrentExecutingTime(currentTime, coexistingJob);
            } else if (actStateFlag) {
                measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, coexistingJob.getNetOCStateLevel());
            }
            coexistingJob.setPreviousMeasuredTime(currentTime);
        }
        job.setActivationState(!activationState);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);           
            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();

            int coexistingNetOCStateLevel = coexistingJob.getNetOCStateLevel();
            int newCoexistingNetOCStateLevel = calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
            int coexistingOCStateLevel = coexistingJob.getOCStateLevel();
            assert coexistingNetOCStateLevel <= newCoexistingNetOCStateLevel;
            assert coexistingNetOCStateLevel <= coexistingOCStateLevel;
            coexistingJob.setNetOCStateLevel(newCoexistingNetOCStateLevel);
            coexistingJob.setJobAffinityRatio(calculateMaxDegradationRatio(coexistingJob, coexistingJob.getCoexistingJobs()));
            coexistingJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(coexistingJob));
            
            if (!intJobFlag) {
                int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
                int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
                if (oldTrueEndTime != trueEndTime) {
                    printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
                    evs.add(new Event(EventType.END, trueEndTime, coexistingJob));
                    coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
                    printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
                    evs.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
                }
            } else if (actStateFlag){
                int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
                int deactivateTime = calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
                if (oldDeactivateTime != deactivateTime) {
                    printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                    evs.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                    coexistingJob.setCurrentDeactiveTime(deactivateTime);
                    printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                    evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
                }
            }
        }        
        
        int netOCStateLevel = job.getNetOCStateLevel();
        int newNetOCStateLevel = calculateNewOCStateLevelForExecutingJob(job, true);
        // TODO: acc_int
        // calc coming int_deactivate
        // throw
        assert netOCStateLevel <= newNetOCStateLevel;
        assert newNetOCStateLevel <= OCStateLevel;
        job.setNetOCStateLevel(newNetOCStateLevel);
        job.setJobAffinityRatio(calculateMaxDegradationRatio(job, coexistingJobs));
        job.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(job));
        int deactivateTime = calculateNewActualEndTimeForActivation(currentTime, job);
        

        job.setPreviousMeasuredTime(currentTime);
        job.setCurrentDeactiveTime(deactivateTime);        
        printThrownEvent(currentTime, deactivateTime, job, EventType.INT_DEACTIVATE, 1);
        evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, job));
        
        return evs;
    }

    ArrayList<Event> deactivateInteractiveJob(Event ev) {
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // TODO
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int currentTime = ev.getOccurrenceTime();
        
        boolean interactiveJob = job.isInteracitveJob();
        assert interactiveJob;
        boolean activationState = job.isActivationState();
        assert activationState;

        calcWastedResource(currentTime);
        
        Set<Integer> coexistingJobs = job.getCoexistingJobs();
        System.out.println("\tDeactivate jobId: " + jobId + ", victim jobId: " + coexistingJobs);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);

            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            if (!intJobFlag) {            
                measureCurrentExecutingTime(currentTime, coexistingJob);
            } else if (actStateFlag) {
                measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, currentTime);
            }
            coexistingJob.setPreviousMeasuredTime(currentTime);
        }
        job.setActivationState(!activationState);
        for (int coexistingJobId: coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            int coexistingNetOCStateLevel = coexistingJob.getNetOCStateLevel();
            int coexistingOCStateLevel = coexistingJob.getOCStateLevel();

            int newCoexistingNetOCStateLevel = calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
            assert newCoexistingNetOCStateLevel <= coexistingNetOCStateLevel;
            assert coexistingNetOCStateLevel <= coexistingOCStateLevel;
            coexistingJob.setNetOCStateLevel(newCoexistingNetOCStateLevel);
            coexistingJob.setJobAffinityRatio(calculateMaxDegradationRatio(coexistingJob, coexistingJob.getCoexistingJobs()));
            coexistingJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(coexistingJob));
            
            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            
            if (!intJobFlag) {
                int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
                int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
                if (oldTrueEndTime != trueEndTime) {
                    printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
                    evs.add(new Event(EventType.END, trueEndTime, coexistingJob));
                    coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
                    printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
                    evs.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
                }
            } else if (actStateFlag) {
                int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
                int deactivateTime = calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
                if (oldDeactivateTime != currentTime && oldDeactivateTime != deactivateTime) {
                    printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                    evs.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                    coexistingJob.setCurrentDeactiveTime(deactivateTime);
                    printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                    evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
                }
            }
            // TODO: treatment of interactive jobs
        }
        job.setActivationState(activationState);

        measureCurrentExecutingTimeForActivation(currentTime, job, job.getOccupiedTimeInTimeSlices());
        double currentAccumulatedComputeQuantityForLatestActivation = job.getCurrentAccumulatedComputeQuantityForLatestActivation();
        // Avoiding Precision problem: + 1e-8
        assert job.getCurrentRequiredActivationTime() <= currentAccumulatedComputeQuantityForLatestActivation + 1e-8;
        currentAccumulatedComputeQuantityForLatestActivation = 0.0;
        job.setCurrentAccumulatedComputeQuantityForLatestActivation(currentAccumulatedComputeQuantityForLatestActivation);
        job.setPreviousMeasuredTime(currentTime);

        job.setActivationState(!activationState);

        int OCStateLevel = job.getOCStateLevel();
        int netOCStateLevel = job.getNetOCStateLevel();
        int newNetOCStateLevel = calculateNewOCStateLevelForExecutingJob(job, true);
        assert newNetOCStateLevel <= netOCStateLevel;
        assert newNetOCStateLevel <= OCStateLevel;
        job.setNetOCStateLevel(newNetOCStateLevel);
        job.setJobAffinityRatio(calculateMaxDegradationRatio(job, coexistingJobs));
        job.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(job));
        
        int currentActivationIndex = job.getCurrentActivationIndex();
        ArrayList<Integer> activationTimes = job.getActivationTimes();
        if (currentActivationIndex == activationTimes.size()-1) {
            int epilogTime = job.getEpilogTIme();
            int endTime = epilogTime + currentTime;
            job.setEndEventOccuranceTimeNow(endTime);
            printThrownEvent(currentTime, endTime, job, EventType.END);
            evs.add(new Event(EventType.END, endTime, job));
        } else {
            int idleTime = job.getIdleTimes().get(currentActivationIndex);
            int activateTime = currentTime + idleTime;

            printThrownEvent(currentTime,activateTime, job, EventType.INT_ACTIVATE, 1);
            evs.add(new Event(EventType.INT_ACTIVATE, activateTime, job));

            ++currentActivationIndex;
            job.setCurrentActivationIndex(currentActivationIndex);
            job.setNextActivationTime(activateTime);
        }
        return evs;
    }

    private boolean checkNoActivationJob(Job job) {
        return job.getActivationTimes().size() == 0 && job.getIdleTimes().size() == 0;
    }

    public int getPreviousCalcTime() {
        return previousCalcTime;
    }

    public void setPreviousCalcTime(int previousCalcTime) {
        this.previousCalcTime = previousCalcTime;
    }

    public List<Double> getWastedResources() {
        return wastedResources;
    }

    private int countTargetJobType(boolean targetJobIsIntJob, ArrayList<Integer> currentExecutingJobIds) {
        int count = START_COUNT;

        for (int coexistingJobId: currentExecutingJobIds) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            boolean intJob = coexistingJob.isInteracitveJob();
            if ( (targetJobIsIntJob && intJob) || (!targetJobIsIntJob && !intJob) ) {
                ++count;
            }
        }
        if (count == START_COUNT) count = -1;
        return count;
    }

    private void modifyTimeSlicesDueToInteractiveJob(int currentTime, int occupiedTimeInTimeSeries, int updateTimeSliceValue, int i, Job job) {
        modifyTimeSlicesDueToSlowsDonwJob(currentTime, occupiedTimeInTimeSeries, updateTimeSliceValue, i, job);
    }

    ArrayList<Event> updateVictimJobOCStateLevel(int currentTime, Job job) {
        Set<Integer> coexistingJobs = job.getCoexistingJobs();
        ArrayList<Event> evs = new ArrayList<Event>();

        for (int coexistingJobId : coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);

            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();
            if (!intJobFlag) {
                measureCurrentExecutingTime(currentTime, coexistingJob);
            } else if (actStateFlag) {
                measureCurrentExecutingTimeForActivation(currentTime, coexistingJob, currentTime);
            }
            coexistingJob.setPreviousMeasuredTime(currentTime);
        }

        for (int coexistingJobId : coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            int coexistingNetOCStateLevel = coexistingJob.getNetOCStateLevel();
            int coexistingOCStateLevel = coexistingJob.getOCStateLevel();

            int newCoexistingNetOCStateLevel = calculateNewOCStateLevelForExecutingJob(coexistingJob, true);
            assert newCoexistingNetOCStateLevel <= coexistingNetOCStateLevel;
            assert coexistingNetOCStateLevel <= coexistingOCStateLevel;
            coexistingJob.setNetOCStateLevel(newCoexistingNetOCStateLevel);
            coexistingJob.setJobAffinityRatio(calculateMaxDegradationRatio(coexistingJob, coexistingJob.getCoexistingJobs()));
            coexistingJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(coexistingJob));

            boolean intJobFlag = coexistingJob.isInteracitveJob();
            boolean actStateFlag = coexistingJob.isActivationState();

            if (!intJobFlag) {
                int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
                int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
                if (oldTrueEndTime != trueEndTime) {
                    printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
                    evs.add(new Event(EventType.END, trueEndTime, coexistingJob));
                    coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
                    printThrownEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
                    evs.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
                }
            } else if (actStateFlag) {
                int oldDeactivateTime = coexistingJob.getCurrentDeactiveTime();
                int deactivateTime = calculateNewActualEndTimeForActivation(currentTime, coexistingJob);
                if (oldDeactivateTime != currentTime && oldDeactivateTime != deactivateTime) {
                    printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.DELETE_DEACTIVE, 1);
                    evs.add(new Event(EventType.DELETE_DEACTIVE, currentTime, coexistingJob, oldDeactivateTime));
                    coexistingJob.setCurrentDeactiveTime(deactivateTime);
                    printThrownEvent(currentTime, deactivateTime, coexistingJob, EventType.INT_DEACTIVATE, 1);
                    evs.add(new Event(EventType.INT_DEACTIVATE, deactivateTime, coexistingJob));
                }
            }
        }
        return evs;
    }
}

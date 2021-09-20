/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import static java.lang.Math.max;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.Constants.BLANK_JOBID;
import static nodeconsciousscheduler.Constants.UNSTARTED;
import static nodeconsciousscheduler.Constants.UNUPDATED;
import static nodeconsciousscheduler.Constants.UNUSED;

/**
 *
 * @author sminami
 */
public class FCFSOC extends FCFS {
    @Override
    protected ArrayList<Event> scheduleJobsStartAt(int currentTime) {
        /* 1. Obtain the head job in the queue */ 
        /* 2. Obtain the nodes the job can execute at */
        /* 3. Select nodes the job is assigned to */        
        /* 4. Calculate opponent job (the head job) OCStateLevel */        
        /* 5. Modify the timeSlices */        
        /* 6. Modify the resource informaiton */        
        /* 7. Enqueue the START and END Events */                
        
        ArrayList<Event> result = new ArrayList<Event>();
        temporallyScheduledJobList.clear();
        
        /* 1. Obtain the head job in the queue */
        while (!waitingQueue.isEmpty()) {
            Job job = waitingQueue.peek();
            int jobId = job.getJobId();

            /* 2. Obtain the nodes the job can execute at */
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesImmediately(currentTime, job);
            TimeSlicesAndNodeInfoConsistency consistency = checkTimeSlicesAndAllNodeInfo();
            assert consistency.isConsistency();
            if (consistency.isSameEndEventFlag()) return result;            
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {

                /* 3. Select nodes the job is assigned to */        
                /* 4. Calculate opponent job (the head job) OCStateLevel */        
                Collections.sort(canExecuteNodes);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                int OCStateLevelForJob = 1;
                for (int i = 0; i < job.getRequiredNodes(); ++i) {
                    assignNodesNo.add(canExecuteNodes.get(i).getNodeNo());
                    OCStateLevelForJob = max(OCStateLevelForJob, canExecuteNodes.get(i).getOCStateLevel());
                }

                waitingQueue.poll();
                int startTime = currentTime;
                //job.setStartTime(startTime);
                makeTimeslices(startTime);


                /* 5. Modify the timeSlices */        
                /* 6. Modify the resource informaiton */        
                /* 7. Enqueue the START and END Events */                
                
                if (OCStateLevelForJob == 1) {
                    /* 5. Modify the timeSlices */        
                    int expectedEndTime = startTime + job.getRequiredTime();
                    makeTimeslices(expectedEndTime);
                    job.setSpecifiedExecuteTime(expectedEndTime);

                    job.setOCStateLevel(OCStateLevelForJob);
                    /* 6. Modify the resource informaiton */        
                    assignJob(startTime, job, assignNodesNo);

                    /* 7. Enqueue the START and END Events */                
                    int trueEndTime = startTime + job.getActualExecuteTime();
                    result.add(new Event(EventType.START, startTime, job));
                    printThrowENDEvent(currentTime, trueEndTime, job, EventType.END);
                    result.add(new Event(EventType.END, trueEndTime, job));
                    job.setEndEventOccuranceTimeNow(trueEndTime);
                } else {
                    /* If OCStateLevel is greater than 1, */
                    /* we must modify the three points below */
                    /*  0. Search victim jobs */
                    /*  1. Modify the victim job's end time in event queue */
                    /*  2. Modify the time slices (variable name: timeSlices defined in Class Scheduler) */
                    
                    /* 0. Search victim jobs */
                    /* 1. Modify the victim job's end time in event queue */
                    /*  1-1. Measure the executing time at current time for each victim jobs. */
                    /*  1-2. Calculate new trueEndTime */
                    /*  1-3. Rethrow the END event set the time */
                    /* 2. Modify the time slices (variable name: timeSlices defined in Class Scheduler) */
                    /*  2-1. Calculate new expectedEndTime */
                    /*  2-2. Update the timeslice between current and new expectedEndTime */

                    /* TODO: return job object list? */

                    /* 0. Search victim jobs */
                    Set<Integer> victimJobs = new HashSet<Integer>();
                    victimJobs = searchVictimJobs(startTime, job, assignNodesNo);
                    int opponentJobId = job.getJobId();
                    System.out.println("OC allocating, opponent jobId: " + opponentJobId + ", OCStateLevel: " + OCStateLevelForJob + ", victim jobId: " + victimJobs);
                    /* Set victim jobList for the opponent job */
                    job.setCoexistingJobs(victimJobs);
                    
                    /* Clear TimeSlece after currentTime */
//                    clearTimeSliceAfter(currentTime);

                    
                    /* 1. Modify the victim job's end time in event queue */
                    /*  1-1. Measure the executing time at current time for each victim jobs. */
                    /*  1-2. Calculate new trueEndTime */
                    /*  1-3. Rethrow the END event set the time */
                    for (int victimJobId: victimJobs) {
                        ArrayList<Event> resultForVictim = new ArrayList<Event>();           
                        Job victimJob = getJobByJobId(victimJobId);
                        int victimNewOCStateLevel = calculateVictimNewOCStateLevel(victimJob, job.getRequiredCoresPerNode(), assignNodesNo);
//                        resultForVictim = modifyTheENDEventTimeForTheJobByJobId(currentTime, victimJobId, OCStateLevelForJob);
                        resultForVictim = modifyTheENDEventTimeForTheJob(currentTime, victimJob, victimNewOCStateLevel);
                        for (Event ev: resultForVictim) {
                            result.add(ev);
                        }
                        /*

                        Job victimJob = getJobByJobId(victimJobId); // O(N)
                        int victimStartTime = victimJob.getStartTime();
                        assert victimStartTime >= 0 && victimStartTime <= currentTime;                        
                        

                        measureCurrentExecutingTime(currentTime, victimJob);
                        victimJob.setPreviousMeasuredTime(currentTime);


                        int currentOCStateLevel = victimJob.getOCStateLevel();
                        assert (currentOCStateLevel + 1 == OCStateLevelForJob) || (currentOCStateLevel == OCStateLevelForJob);

                        printOCStateLevelTransition(currentOCStateLevel, OCStateLevelForJob, victimJobId);
                        victimJob.setOCStateLevel(OCStateLevelForJob);
                        int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);                        

                        printThrowENDEvent(currentTime, trueEndTime, victimJob);
                        result.add(new Event(EventType.END, trueEndTime, victimJob));
                        result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob)); // This event delete the END event already exists in the event queue. 
                        */
                        victimJob.getCoexistingJobs().add(opponentJobId);         
                        victimJob.setOCStateLevel(victimNewOCStateLevel);
                    }
                    
                    /* 2. Modify the time slices (variable name: timeSlices defined in Class Scheduler) */
                    /*  2-1. Calculate new expectedEndTime */
                    /*  2-2. Update the timeslice between current and new expectedEndTime */

                    for (int victimJobId: victimJobs) {
                        Job victimJob = getJobByJobId(victimJobId); // O(N)

                        /*  2-1. Calculate new expectedEndTime */
                        int oldExpectedEndTime = victimJob.getSpecifiedExecuteTime(); // This field name is bad. Difficult to interpret.
                        int newExpectedEndTime = calculateNewExpectedEndTime(currentTime, victimJob);
                        victimJob.setSpecifiedExecuteTime(newExpectedEndTime);
                        assert oldExpectedEndTime <= newExpectedEndTime+1;
                        
                        /*  2-2. Update the timeslice between current and new expectedEndTime */
                        int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob);

                        makeTimeslices(currentTime);
                        makeTimeslices(newExpectedEndTime);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob);
                    }
                    
                    /* For opponent job */
                    job.setOCStateLevel(OCStateLevelForJob);
                    //int expectedEndTime = startTime + job.getRequiredTime() * OCStateLevelForJob;
                    int expectedEndTime = calculateNewExpectedEndTime(startTime, job);
                    makeTimeslices(expectedEndTime);
                    job.setSpecifiedExecuteTime(expectedEndTime);

                    /* Set previous time. */
                    /* This is opponent, so it is not "switched" now. But, this value is needed. */
                    job.setPreviousMeasuredTime(startTime);
                    
                    assignJob(startTime, job, assignNodesNo);

                    //int trueEndTime = startTime + job.getActualExecuteTime() * OCStateLevelForJob;
                    int trueEndTime = calculateNewActualEndTime(startTime, job);
                    result.add(new Event(EventType.START, startTime, job));
                    printThrowENDEvent(currentTime, trueEndTime, job, EventType.END);
                    result.add(new Event(EventType.END, trueEndTime, job));
                    job.setEndEventOccuranceTimeNow(trueEndTime);                   
                }
                temporallyScheduledJobList.add(job);
            } else break;
        }
        return result;
    }

    @Override
    protected ArrayList<Event>  checkCoexistingJobsOCStateAndModifyENDEventAndTimeSlices(Event ev) {
        ArrayList<Event> result = new ArrayList<Event>();

        /* Check the OCStateLevel for coexisting jobs */
        /*  1. Check the OCStateLevel */
        /*  2. Modify the END event time */
        /*  3. Modify the timeSlices */

        /* Setting */
        int currentTime = ev.getOccurrenceTime();
        Job endingJob = ev.getJob();
        int endingJobId = endingJob.getJobId();
        Set<Integer> coexistingJobs = endingJob.getCoexistingJobs();

        System.out.println("debug) ending Job Id: " + endingJobId);
        System.out.println("\tdebug) coexistingJobs: " + coexistingJobs);
        
        /* 1. Check the OCStateLevel */
        /*    Change appropriate OCStateLevel for coexisting jobs */
        for (int coexistingJobId : coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            /* Calculate new OCStateLevel for coexisting jobs */
            ArrayList<UsingNode> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();

            Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
            assert coexistingJobCoexistingJob.contains(endingJobId);
           
            printDebugForCoexistingJob(ev, coexistingJobId);
 
            /* 1.1 Check all nodes used by coexisting job */
            /*
            for (int i = 0; i < coexistingJobUsingNodeList.size(); ++i) {
                int multiplicityAlongCores = UNUPDATED;

                // node setting
                UsingNode usingNode = coexistingJobUsingNodeList.get(i);
                int usingNodeId = usingNode.getNodeNum();
                NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(usingNodeId);
                assert usingNodeId == nodeInfo.getNodeNum();
                
                ArrayList<Integer> usingCoreIds = usingNode.getUsingCoreNum();

                // Core loop
                // 1.2 Check all cores used by coexisting job                
                ArrayList<CoreInfo> occupiedCores = nodeInfo.getOccupiedCores();
                for (int usingCoreId: usingCoreIds) {
                    CoreInfo usingCoreInfo = getOccupiedCoreInfoByCoreId(occupiedCores, usingCoreId); // O(N)
                    assert usingCoreId == usingCoreInfo.getCoreId();
                    ArrayList<Integer> jobListOnTheCore = usingCoreInfo.getJobList();
                    assert jobListOnTheCore.contains(coexistingJobId);
                    assert !jobListOnTheCore.contains(endingJobId);
                    multiplicityAlongCores = max(multiplicityAlongCores, jobListOnTheCore.size());
                }
                assert multiplicityAlongCores != UNUPDATED;
                assert multiplicityAlongCores <= NodeConsciousScheduler.M;
                multiplicityAlongNodes = max(multiplicityAlongNodes, multiplicityAlongCores);
            }
            assert multiplicityAlongNodes != UNUPDATED; 
            assert multiplicityAlongNodes <= NodeConsciousScheduler.M;
            */
            
            //int OCStateLevel = multiplicityAlongNodes;
            int OCStateLevel = checkMultiplicityAlongNodes(coexistingJobUsingNodeList, endingJobId, coexistingJobId);

            // 2. Modify the END event time
            modifyTheENDEventTime(coexistingJob, coexistingJobId, currentTime, OCStateLevel, result);

            // 3. Modify the timeSlices
            modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, endingJobId);
        }
        
        return result;
    }
    
    @Override
    protected ArrayList<VacantNode> canExecutableNodesImmediately(int currentTime, Job job) {
        /* Return variable
           This have the node no. with # of free core.
        */
        ArrayList<VacantNode> nodes = new ArrayList<VacantNode>();
        
        /* Working Variable */
        ArrayList<VacantNode> vacantNodes = new ArrayList<VacantNode>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodes.add(new VacantNode(i, NodeConsciousScheduler.numCores));
        
        /* This is used for counting executable nodes */
        ArrayList<Integer> vacantNodeCount = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodeCount.add(0);

        /* Calculate ppn */
        /* TODO: The case requiredCores ist not dividable  */
        //int requiredCoresPerNode = job.getRequiredCores()/job.getRequiredNodes();
        //if (job.getRequiredCores()%job.getRequiredNodes() != 0) ++requiredCoresPerNode;
        int requiredCoresPerNode = job.getRequiredCoresPerNode();

        int jobId = job.getJobId();
        int M = NodeConsciousScheduler.M;
        int alongTimeSlices = 0;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            if (ts.getStartTime() <= currentTime && currentTime <= ts.getEndTime()) {
                ++alongTimeSlices;
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    int freeCores = ts.getAvailableCores().get(j);
                    int numCore = ts.getPpn();
                    
                    assert freeCores >= -(M-1)*numCore;
                    assert freeCores <= numCore;
                    
                    VacantNode node = vacantNodes.get(j);
                    
                    assert node.getNodeNo() == j;

                    freeCores = min(freeCores, node.getFreeCores());
                    node.setFreeCores(freeCores);

                    assert freeCores >= -(M-1)*numCore;
                    assert freeCores <= numCore;                    
//                    if (freeCores >= requiredCoresPerNode ) {
                    if (freeCores - requiredCoresPerNode >= -(M-1)*numCore) {
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

        /* Check OCStateLevel */
        int numCore = NodeConsciousScheduler.numCores;
        for (int i = 0; i < nodes.size(); ++i) {
            VacantNode node = nodes.get(i);
            int freeCores = node.getFreeCores();            

            for (int j = 1; j <= M; ++j) {
                /* if OCStateLevel == j */
                if ( (freeCores - requiredCoresPerNode >= -(j-1)*numCore) &&
                     (freeCores - requiredCoresPerNode <  -(j-2)*numCore) ) {
//                    System.out.println("jobId: " + jobId + ", on node: " + nodeNo);
                    int OCStateLevel = j;
                    node.setOCStateLevel(OCStateLevel);
                    break;
                }
            }
        }
        
        return nodes;
    }
    
    private void modifyTimeSlices(Job candidateJob) {
        return;
    }

    private void clearTimeSliceAfter(int currentTime) {
        FCFSOC.this.clearTimeSliceAfter(currentTime, this.timeSlices);
    }

    private void clearTimeSliceAfter(int currentTime, LinkedList<TimeSlice> timeSlices) {

        int clearIndex = UNUPDATED;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            int startTime = ts.getStartTime();
            if (currentTime == startTime) {
                clearIndex = i;
            }
            assert currentTime <= startTime;
        }
        
        if (clearIndex == UNUPDATED) return;

        for (int i = timeSlices.size() - 1; i >= clearIndex; --i) {
            timeSlices.remove(i);
        }
        
        int i = timeSlices.size() - 1;
        
        if (i >= 0) {
            TimeSlice lastTs = timeSlices.get(i);
            int lastTsEndTime = lastTs.getEndTime();
            timeSlices.add(new TimeSlice(lastTsEndTime));
        } else {
            timeSlices.add(new TimeSlice(currentTime));
        }
        

        
        return;
    }

    protected void assignJobForOnlyTimeSlices(int startTime, Job job, int expectedEndTime) {
        ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
        ArrayList<UsingNode> usingNodeList = job.getUsingNodesList();
        
        for (int i = 0; i < usingNodeList.size(); ++i) {
            UsingNode node = usingNodeList.get(i);
            int nodeId = node.getNodeNum();
            assignNodesNo.add(nodeId);
        }
        assert assignNodesNo.size() != 0;
        assert assignNodesNo.size() == usingNodeList.size();

        assignJobForOnlyTimeSlices(startTime, this.timeSlices, NodeConsciousScheduler.sim.getAllNodesInfo(), job, assignNodesNo, expectedEndTime);
        
    }
    
    protected void assignJobForOnlyTimeSlices(int startTime, LinkedList<TimeSlice> timeSlices, ArrayList<NodeInfo> allNodesInfo, Job job, ArrayList<Integer> assignNodesNo, int expectedEndTime) {
        int addedPpn = job.getRequiredCores()/job.getRequiredNodes();

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
    }



    private Set<Integer> cloneVictimJobs(Set<Integer> victimJobs) {
        Set<Integer> copiedVictimJobs = new HashSet<Integer>();
        for (int victimJobId: victimJobs) {
            copiedVictimJobs.add(victimJobId);
        }
        return copiedVictimJobs;
    }

    private Set<Integer> getAllJobIdsOnTheNode(int nodeId) {
        ArrayList<NodeInfo> allNodeInfoList = NodeConsciousScheduler.sim.getAllNodesInfo();
        
        int i;
        for (i = 0; i < allNodeInfoList.size(); ++i) {
            NodeInfo nodeInfo = allNodeInfoList.get(i);
            int nodeIdInList = nodeInfo.getNodeNum();
            if (nodeId == nodeIdInList) break;
        }
        assert i < allNodeInfoList.size();
        return allNodeInfoList.get(i).getExecutingJobIds();

    }

    private CoreOCState getMultiplicityOnTheCore(int nodeId, int coreId, int endingJobId) {
        /* Return the multiplicity on the core */

        /* Get nodes setting */
        ArrayList<NodeInfo> allNodeInfoList = NodeConsciousScheduler.sim.getAllNodesInfo();
        NodeInfo nodeInfo =allNodeInfoList.get(nodeId);
        int nodeIdFromList = nodeInfo.getNodeNum();
        assert nodeId == nodeIdFromList;

        /* Get cores setting */
        ArrayList<CoreInfo> jobIdListEachCore = nodeInfo.getOccupiedCores();
        CoreInfo coreInfo = new CoreInfo();

        int endCnt = -1;
        for (int i = 0; i < jobIdListEachCore.size(); ++i) {
            endCnt = i;
            int tmpCoreId = jobIdListEachCore.get(i).getCoreId();
            if (coreId == tmpCoreId) coreInfo = jobIdListEachCore.get(i);
        }
        assert endCnt != jobIdListEachCore.size();
        
        int coreIdFromList = coreInfo.getCoreId();
        assert coreId == coreIdFromList;
        ArrayList<Integer> jobListOnTheCore = coreInfo.getJobList();

        boolean isExistEndingJob = false;        
        for (int i = 0; i < jobListOnTheCore.size(); ++i) {
            int executingJobId = jobListOnTheCore.get(i);
            if (executingJobId == endingJobId) {
                isExistEndingJob = true;
                break;
            }
        }
        
        int multiplicity = jobListOnTheCore.size();
        
        return new CoreOCState(isExistEndingJob, multiplicity);
    }

    private void removeEndingJobFromJobList(int nodeId, int coreId, int endingJobId) {
        ArrayList<NodeInfo> allNodeInfoList = NodeConsciousScheduler.sim.getAllNodesInfo();
        NodeInfo nodeInfo =allNodeInfoList.get(nodeId);
        int nodeIdFromList = nodeInfo.getNodeNum();
        assert nodeId == nodeIdFromList;
        
        ArrayList<CoreInfo> jobIdListEachCore = nodeInfo.getOccupiedCores();
        
        CoreInfo coreInfo = new CoreInfo();
        int endCnt = -1;
        for (int i = 0; i < jobIdListEachCore.size(); ++i) {
            endCnt = i;
            int tmpCoreId = jobIdListEachCore.get(i).getCoreId();
            if (coreId == tmpCoreId) coreInfo = jobIdListEachCore.get(i);
        }
        assert endCnt != jobIdListEachCore.size();
        ArrayList<Integer> jobListOnTheCore = coreInfo.getJobList();

        for (int i = 0; i < jobListOnTheCore.size(); ++i) {
            int executingJobId = jobListOnTheCore.get(i);
            if (executingJobId == endingJobId) jobListOnTheCore.remove(i);
        }

    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;


import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.min;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import static nodeconsciousscheduler.Constants.CANNOT_START;
import static nodeconsciousscheduler.Constants.UNSTARTED;
import static nodeconsciousscheduler.Constants.UNUPDATED;

/**
 *
 * @author sminami
 */
public class EasyBackfillingOC extends EasyBackfilling {
    
    @Override
    protected ArrayList<Event> scheduleJobsStartAt(int currentTime) {
        ArrayList<Event> result = new ArrayList<Event>();
        temporallyScheduledJobList.clear();
        while (!waitingQueue.isEmpty()) {
            Job job = waitingQueue.peek();
            int jobId = job.getJobId();
            
            TimeSlicesAndNodeInfoConsistency consistency = checkTimeSlicesAndAllNodeInfo(currentTime);
            assert consistency.isConsistency();
            if (consistency.isSameEndEventFlag()) return result;
            
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesAt(currentTime, job);
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {
                if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {
                    calculatePriorityForNodes(canExecuteNodes, job);
                }
                Collections.sort(canExecuteNodes);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                int OCStateLevelForJob = 1;
                for (int i = 0; i < job.getRequiredNodes(); ++i) {
                    assignNodesNo.add(canExecuteNodes.get(i).getNodeNo());
                    OCStateLevelForJob = max(OCStateLevelForJob, canExecuteNodes.get(i).getOCStateLevel());
                }

                int startTime = currentTime;
                //job.setStartTime(startTime);
                makeTimeslices(startTime);

                boolean interJobFlag = job.isInteracitveJob();
                
                if (OCStateLevelForJob == 1) {
                    int expectedEndTime = startTime + job.getRequiredTime();
                    makeTimeslices(expectedEndTime);
                    job.setOccupiedTimeInTimeSlices(expectedEndTime);

                    job.setOCStateLevel(OCStateLevelForJob);
                    assignJob(startTime, job, assignNodesNo);

                    int trueEndTime = startTime + job.getActualExecuteTime();
                    result.add(new Event(EventType.START, startTime, job));

                    if (!interJobFlag) {
                        printThrownEvent(currentTime, trueEndTime, job, EventType.END);
                        result.add(new Event(EventType.END, trueEndTime, job));
                        job.setEndEventOccuranceTimeNow(trueEndTime);
                    }
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
                    int opponentJobId = job.getJobId();
                    victimJobs = searchVictimJobs(startTime, job, assignNodesNo);
                                         
                    if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {
                        double ratio = calculateMaxDegradationRatio(job, victimJobs);
                        if (ratio > NodeConsciousScheduler.sim.getThresholdForAffinitySchedule()) {
                            System.out.println("!!!!!! QUIT SCHEDULING DUE TO AFFINITY !!!!!!");
                            break;
                        }
                    }
                    
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
                        Job victimJob = getJobByJobId(victimJobId); // O(N)
                        int victimNewOCStateLevel = calculateVictimNewOCStateLevel(victimJob, job.getRequiredCoresPerNode(), assignNodesNo);

                        int victimNewNetOCStateLevel = calculateVictimNewOCStateLevel(victimJob, job.getRequiredCoresPerNode(), assignNodesNo, true, interJobFlag);
                        victimJob.getCoexistingJobs().add(opponentJobId);;
                        resultForVictim = modifyTheENDEventTimeForTheJob(currentTime, victimJob, victimNewOCStateLevel, victimNewNetOCStateLevel, interJobFlag);
                        for (Event ev : resultForVictim) {
                            result.add(ev);
                        }
                        
                        victimJob.getCoexistingJobs().add(opponentJobId);          
                        victimJob.setOCStateLevel(victimNewOCStateLevel);
                    }
                    
                    /* 2. Modify the time slices (variable name: timeSlices defined in Class Scheduler) */
                    /*  2-1. Calculate new expectedEndTime */
                    /*  2-2. Update the timeslice between current and new expectedEndTime */

                    for (int victimJobId: victimJobs) {
                        Job victimJob = getJobByJobId(victimJobId); // O(N)

                        /*  2-1. Calculate new expectedEndTime */
                        int oldExpectedEndTime = victimJob.getOccupiedTimeInTimeSlices(); // This field name is bad. Difficult to interpret.
                        int newExpectedEndTime = calculateNewExpectedEndTime(currentTime, victimJob);
                        int endEventOccuranceTime = victimJob.getEndEventOccuranceTimeNow();
                        if (newExpectedEndTime < endEventOccuranceTime) { // This is not good implement
                            newExpectedEndTime = endEventOccuranceTime;
                        }
                        victimJob.setOccupiedTimeInTimeSlices(newExpectedEndTime);
                        //assert oldExpectedEndTime <= newExpectedEndTime+1;
                        
                        /*  2-2. Update the timeslice between current and new expectedEndTime */
                        int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob);

                        makeTimeslices(currentTime);
                        makeTimeslices(newExpectedEndTime);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob);
                    }
                    
                    /* For opponent job */
                    job.setOCStateLevel(OCStateLevelForJob);
                    int netOCStateLevel = calculateNewOCStateLevelForNewJob(job, job.getRequiredCoresPerNode(), assignNodesNo, true);
                    job.setNetOCStateLevel(netOCStateLevel);
                    int expectedEndTime = calculateNewExpectedEndTime(startTime, job);
                    makeTimeslices(expectedEndTime);
                    job.setOccupiedTimeInTimeSlices(expectedEndTime);

                    /* Set previous time. */
                    /* This is opponent, so it is not "switched" now. But, this value is needed. */
                    job.setPreviousMeasuredTime(startTime);
                    
                    assignJob(startTime, job, assignNodesNo);

                    //int trueEndTime = startTime + job.getActualExecuteTime() * OCStateLevelForBackfillJob;
                    job.setJobAffinityRatio(calculateMaxDegradationRatioForVictim(job, victimJobs));
                    job.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(job));
                    int trueEndTime = calculateNewActualEndTime(startTime, job);
                    result.add(new Event(EventType.START, startTime, job));

                    if (!interJobFlag) {
                        printThrownEvent(currentTime, trueEndTime, job, EventType.END);
                        result.add(new Event(EventType.END, trueEndTime, job));
                        job.setEndEventOccuranceTimeNow(trueEndTime);                         
                    }
                }
                waitingQueue.poll();
                temporallyScheduledJobList.add(job);
            } else break;
        }
        
        if (waitingQueue.size() <= 1) {
            temporallyScheduledJobList.clear();
            return result;
        }
        
        /* Backfilling */
        Queue<Job> tailWaitingQueue = copyWaitingQueue();
        Job firstJob = waitingQueue.peek();

        int startTimeFirstJob = CANNOT_START;
        ArrayList<VacantNode> canExecuteTmpNodes = new ArrayList<VacantNode>();
        ArrayList<NodeInfo> tmpAllNodesInfo = cloneAllNodesInfo(NodeConsciousScheduler.sim.getAllNodesInfo());        
        /* Find tempollary executable nodes */
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            //ts.printTsInfo();
            int tmpStartTime = ts.getStartTime();
            updateAllNodesInfo(timeSlices, tmpAllNodesInfo, tmpStartTime);
            if (tmpStartTime <= currentTime) {
                continue;
            }

            canExecuteTmpNodes = canExecutableTmpNodes(tmpStartTime, firstJob);

            if (canExecuteTmpNodes.size() >= firstJob.getRequiredNodes()) {
                if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {
                    calculatePriorityForNodes(canExecuteTmpNodes, firstJob);
                }                
                Collections.sort(canExecuteTmpNodes);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                int OCStateLevelForJob = 1;
                for (int j = 0; j < firstJob.getRequiredNodes(); ++j) {
                    assignNodesNo.add(canExecuteTmpNodes.get(j).getNodeNo());
                    OCStateLevelForJob = max(OCStateLevelForJob, canExecuteTmpNodes.get(j).getOCStateLevel());
                }
                
                Set<Integer> victimJobs = new HashSet<Integer>(); 
                int opponentJobId = firstJob.getJobId();
                victimJobs = searchVictimJobs(tmpStartTime, tmpAllNodesInfo, firstJob, assignNodesNo);
                                         

                if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) { 
                    if (OCStateLevelForJob >= 2) {
                        double ratio = calculateMaxDegradationRatio(firstJob, victimJobs);
                        if (ratio > NodeConsciousScheduler.sim.getThresholdForAffinitySchedule()) {
                            System.out.println("!!!!!! QUIT SCHEDULING DUE TO AFFINITY FOR FIRST JOB !!!!!!");
                            continue;
                        }
                    }
                }
                
                
                startTimeFirstJob = tmpStartTime;
                break;
            }
        }
        
        tailWaitingQueue.poll();

        if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {        
            calculatePriorityForNodes(canExecuteTmpNodes, firstJob);            
        }
        Collections.sort(canExecuteTmpNodes);
        ArrayList<Integer> assignTmpNodesNo = new ArrayList<Integer>();
        int OCStateLevelForFirstJob = 1;
        for (int i = 0; i < firstJob.getRequiredNodes(); ++i) {
            assignTmpNodesNo.add(canExecuteTmpNodes.get(i).getNodeNo());
            OCStateLevelForFirstJob = max(OCStateLevelForFirstJob, canExecuteTmpNodes.get(i).getOCStateLevel());
        }

        // TODO: Erase below line
        int firstJobId = firstJob.getJobId();
        System.out.println("size: " + canExecuteTmpNodes.size() + ", firstJob: " + firstJobId + ", tentative start time: " + startTimeFirstJob);

        assert canExecuteTmpNodes.size() >= firstJob.getRequiredNodes();
        assert startTimeFirstJob != CANNOT_START;

        /* We must handle the copy of original list */
        /* TODO?: Define the appropriate clonable setting */
        LinkedList<TimeSlice> tmpTimeSlices = cloneTimeSlices(timeSlices);


        firstJob.setOCStateLevel(OCStateLevelForFirstJob);
        makeTimeslices(startTimeFirstJob, tmpTimeSlices, true);
        int endTimeFirstJob = calculateNewExpectedEndTime(startTimeFirstJob, firstJob);
        makeTimeslices(endTimeFirstJob, tmpTimeSlices, true);
        assignFirstJobTemporally(tmpTimeSlices, tmpAllNodesInfo, startTimeFirstJob, firstJob, canExecuteTmpNodes);

        ArrayList<VacantNode> canExecuteNodesEasyBackfiling;
        System.out.println("debug) Queue length: " + tailWaitingQueue.size());
        ArrayList<Integer> cancelJobExecutionTimeList = new ArrayList<Integer>();
        ArrayList<Integer> cancelJobRequiredCoresList = new ArrayList<Integer>();

        while (tailWaitingQueue.size() > 0) {
            TimeSlicesAndNodeInfoConsistency consistency = checkTimeSlicesAndAllNodeInfo(currentTime);
            assert consistency.isConsistency();
            if (consistency.isSameEndEventFlag()) return result;            
            Job backfillJob = tailWaitingQueue.poll();
            int backfillJobId = backfillJob.getJobId();
            int backfillRequiredTime = backfillJob.getRequiredTime();
            int backfillRequiredCores = backfillJob.getRequiredCores();
            
            boolean cancelFlag = false;
            for (int j = 0; j < cancelJobExecutionTimeList.size(); ++j) {
                int cancelJobExecutionTime = cancelJobExecutionTimeList.get(j);
                int cancelJobRequiredCores = cancelJobRequiredCoresList.get(j);
                if (cancelJobExecutionTime <= backfillRequiredTime &&
                    cancelJobRequiredCores <= backfillRequiredCores) {
                    cancelFlag = true;
                    break;
                }
            } 
            if (cancelFlag) {
                continue;
            }

            canExecuteNodesEasyBackfiling = canExecutableNodesOnBackfilling(currentTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, startTimeFirstJob, assignTmpNodesNo);

            if (canExecuteNodesEasyBackfiling.size() >= backfillJob.getRequiredNodes()) {
                // TO CONSIDER:
                // Is it appropriate way to select the nodes?                                
                if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {
                    calculatePriorityForNodes(canExecuteNodesEasyBackfiling, backfillJob);
                }
                Collections.sort(canExecuteNodesEasyBackfiling);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                int OCStateLevelForBackfillJob = 1;
                for (int i = 0; i < backfillJob.getRequiredNodes(); ++i) {
                    assignNodesNo.add(canExecuteNodesEasyBackfiling.get(i).getNodeNo());
                    OCStateLevelForBackfillJob = max(OCStateLevelForBackfillJob, canExecuteNodesEasyBackfiling.get(i).getOCStateLevel());                    
                }

                int startTime = currentTime;
                Set<Integer> victimJobs = new HashSet<Integer>();
                victimJobs = searchVictimJobs(startTime, backfillJob, assignNodesNo);
                
                System.out.println("\tdebug) candidate backfill jobId: " + backfillJobId);
                System.out.println("\tdebug) victimJobs: " + victimJobs);

                if (NodeConsciousScheduler.sim.isUsingAffinityForSchedule()) {                        
                    double ratio = calculateMaxDegradationRatio(backfillJob, victimJobs);  
                    if (ratio > NodeConsciousScheduler.sim.getThresholdForAffinitySchedule()) {                    
                        System.out.println("!!!!!! QUIT SCHEDULING DUE TO AFFINITY FOR BACKFILL JOB !!!!!!");                        
                        break;                        
                    }                    
                }
                
                boolean backfillFlag = true;
                // Check whether victim jobs will slow and delay the start time of first job.
                for (int victimJobId: victimJobs) {
                    Job victimJob = getJobByJobId(victimJobId);

                    int currentVictimExpectedEndTime = victimJob.getOccupiedTimeInTimeSlices();
                    /* "victim's occupied time in timeslices > first job's start time" means victim job does not use nodes in common with first job.*/
                    /* Thus we can skip it. */
                    if (currentVictimExpectedEndTime > startTimeFirstJob) continue;

                    /* if OC state level is up, it may delay the start time. */
                    /* Thus, we quit backfill */
                    int victimCurrentOCStateLevel = victimJob.getOCStateLevel();
                    int victimNewOCStateLevel = calculateVictimNewOCStateLevel(victimJob, backfillJob.getRequiredCoresPerNode(), assignNodesNo);

                    if (victimNewOCStateLevel > victimCurrentOCStateLevel) {
                        backfillFlag = false;
                        cancelJobExecutionTimeList.add(backfillRequiredTime);
                        cancelJobRequiredCoresList.add(backfillRequiredCores);

                        break;
                    }

                    /* if ratio is worse, it may delay the start time. */
                    /* Thus, we quit backfill */
                    Set<Integer> tmpCoexistingJobs = cloneCoexistingJobs(victimJob.getCoexistingJobs());
                    tmpCoexistingJobs.add(backfillJobId);
                    double tmpRatio = calculateMaxDegradationRatioForVictim(victimJob, tmpCoexistingJobs);
                    if (tmpRatio > victimJob.getJobAffinityRatio()) {
                        backfillFlag = false;
                        break;
                    }
                }
                
                if (!backfillFlag) continue;

                System.out.println("Succeed Backfill Job: " + backfillJobId + ", at " + currentTime + ", queueSize: " + tailWaitingQueue.size());
                
                Iterator itr = waitingQueue.iterator();
                while (itr.hasNext()) {
                    Job deleteJob = (Job) itr.next();
                    if (deleteJob.getJobId() == backfillJob.getJobId()) {
                        itr.remove();
                        break;
                    }
                }


                //backfillJob.setStartTime(startTime);

                makeTimeslices(startTime);
                makeTimeslices(startTime, tmpTimeSlices, false);
                
                if (OCStateLevelForBackfillJob == 1) {
                    int expectedEndTime = startTime + backfillJob.getRequiredTime();
                    makeTimeslices(expectedEndTime);
                    makeTimeslices(expectedEndTime, tmpTimeSlices, false);
                    backfillJob.setOccupiedTimeInTimeSlices(expectedEndTime);

                    backfillJob.setOCStateLevel(OCStateLevelForBackfillJob);
                    assignJob(startTime, backfillJob, assignNodesNo);
                    assignJobForTmp(startTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, assignNodesNo);
                    
                    int trueEndTime = startTime + backfillJob.getActualExecuteTime();

                    result.add(new Event(EventType.START, startTime, backfillJob));                    
                    boolean interactiveJob = backfillJob.isInteracitveJob();
                    if (!interactiveJob) {
                        printThrownEvent(currentTime, trueEndTime, backfillJob, EventType.END);
                        result.add(new Event(EventType.END, trueEndTime, backfillJob));
                        backfillJob.setEndEventOccuranceTimeNow(trueEndTime);
                    }
                } else {
                    System.out.println("OC allocating, opponent jobId: " + backfillJobId + ", OCStateLevel: " + OCStateLevelForBackfillJob + ", victim jobId: " + victimJobs);

                    Map<Integer, Boolean> neednessRealloc = new HashMap<Integer, Boolean>();
                    backfillJob.setCoexistingJobs(victimJobs);
                    /* 1. Modify the victim job's end time in event queue */
                    for (int victimJobId: victimJobs) {                                                 
                        neednessRealloc.put(victimJobId, true);
                        ArrayList<Event> resultForVictim = new ArrayList<Event>();
                        Job victimJob = getJobByJobId(victimJobId); // O(N)                        
                        int victimNewOCStateLevel = calculateVictimNewOCStateLevel(victimJob, backfillJob.getRequiredCoresPerNode(), assignNodesNo);
                        victimJob.getCoexistingJobs().add(backfillJobId);
                        resultForVictim = modifyTheENDEventTimeForTheJob(currentTime, victimJob, victimNewOCStateLevel);
                        for (Event ev: resultForVictim) {
                            result.add(ev);
                            neednessRealloc.put(victimJobId, false);
                        }
                        victimJob.getCoexistingJobs().add(backfillJobId);
                        victimJob.setOCStateLevel(victimNewOCStateLevel);
                    }

                    /* 2. Modify the time slices (variable name: timeSlices defined in Class Scheduler) */
                    for (int victimJobId: victimJobs) {                        
                        if (neednessRealloc.get(victimJobId)) continue;
                        Job victimJob = getJobByJobId(victimJobId); // O(N)

                        /*  2-1. Calculate new expectedEndTime */

                        int oldExpectedEndTime = victimJob.getOccupiedTimeInTimeSlices(); // This field name is bad. Difficult to interpret.
                        int newExpectedEndTime = calculateNewExpectedEndTime(currentTime, victimJob);                         
                        int endEventOccuranceTime = victimJob.getEndEventOccuranceTimeNow();
                        if (newExpectedEndTime < endEventOccuranceTime) { // This is not good implement
                            newExpectedEndTime = endEventOccuranceTime;
                        }
                        victimJob.setOccupiedTimeInTimeSlices(newExpectedEndTime);
                        //assert oldExpectedEndTime <= newExpectedEndTime + 1;

                        /*  2-2. Update the timeslice between current and new expectedEndTime */
                        int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob);
                        timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime, tmpTimeSlices);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob, tmpTimeSlices);

                        makeTimeslices(currentTime);
                        makeTimeslices(currentTime, tmpTimeSlices, false);
                        makeTimeslices(newExpectedEndTime);
                        makeTimeslices(newExpectedEndTime, tmpTimeSlices, false);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob, tmpTimeSlices);
                    }
                    
                    backfillJob.setOCStateLevel(OCStateLevelForBackfillJob);
                    int netOCStateLevel = calculateNewOCStateLevelForNewJob(backfillJob, backfillJob.getRequiredCoresPerNode(), assignNodesNo, true);
                    backfillJob.setNetOCStateLevel(netOCStateLevel);
                    int expectedEndTime = calculateNewExpectedEndTime(currentTime, backfillJob);
                    makeTimeslices(expectedEndTime);
                    makeTimeslices(expectedEndTime, tmpTimeSlices, false);
                    backfillJob.setOccupiedTimeInTimeSlices(expectedEndTime);

                    assignJob(startTime, backfillJob, assignNodesNo);
                    assignJobForTmp(startTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, assignNodesNo);

                    backfillJob.setPreviousMeasuredTime(startTime);
                    backfillJob.setJobAffinityRatio(calculateMaxDegradationRatioForVictim(backfillJob, victimJobs));
                    backfillJob.setOsubOverheadRatio(calculateOsubOverheadRatioForVictim(backfillJob));
                    int trueEndTime = calculateNewActualEndTime(startTime, backfillJob);
                    result.add(new Event(EventType.START, startTime, backfillJob));

                    boolean interactiveJob = backfillJob.isInteracitveJob();
                    if (!interactiveJob) {
                        printThrownEvent(currentTime, trueEndTime, backfillJob, EventType.END);
                        result.add(new Event(EventType.END, trueEndTime, backfillJob));
                        backfillJob.setEndEventOccuranceTimeNow(trueEndTime);
                    }
                }
                temporallyScheduledJobList.add(backfillJob);
            }
        }
        // TODO: firstJob clear
        firstJob.setOCStateLevel(1);

        temporallyScheduledJobList.clear();
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
            modifyTheENDEventTime(coexistingJob, coexistingJobId, currentTime, OCStateLevel, result, true, endingJobId);

            // 3. Modify the timeSlices
            modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, endingJobId);
        }
        
        return result;
    }
    
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, Job job) {
        return canExecutableNodesAt(currentTime, this.timeSlices, job, false, UNSTARTED);
    }
    
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, LinkedList<TimeSlice> timeSlices, Job job, boolean backfillFlag, int firstJobStartTime) {
       return canExecutableNodesAt(currentTime, timeSlices, job, backfillFlag, firstJobStartTime, new ArrayList<Integer>());
    }
    
    
    protected ArrayList<VacantNode> canExecutableNodesOnBackfilling(int currentTime, LinkedList<TimeSlice> tmpTimeSlices, ArrayList<NodeInfo> tmpAllNodesInfo, Job backfillJob, int firstJobStartTime, ArrayList<Integer> tmpAssignNodesNo) {
        ArrayList<VacantNode> canExecuteNodesEasyBackfiling = new ArrayList<VacantNode>();
        
        canExecuteNodesEasyBackfiling = canExecutableNodesAt(currentTime, tmpTimeSlices, backfillJob, true, firstJobStartTime, tmpAssignNodesNo);
        
        return canExecuteNodesEasyBackfiling;

    }
    
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, LinkedList<TimeSlice> timeSlices, Job job, boolean backfillFlag, int firstJobStartTime, ArrayList<Integer> tmpAssignNodesNo) {
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
        
        int requiredNodes = job.getRequiredNodes();

        int jobId = job.getJobId();
        int startTime = currentTime;
        int expectedEndTime = startTime + job.getRequiredTime();
//        int expectedEndTime = UNUPDATED;
        int alongTimeSlices = 0;
        ArrayList<Integer> jobRestTimeEachNode = new ArrayList<Integer>();
        int M = NodeConsciousScheduler.M;
        
        if (!backfillFlag) {
            for (int i = 0; i < timeSlices.size(); ++i) {
                TimeSlice ts = timeSlices.get(i);
                if ((ts.getStartTime() <= startTime && startTime < ts.getEndTime())
                        || (ts.getStartTime() < expectedEndTime && expectedEndTime <= ts.getEndTime())
                        || (startTime <= ts.getStartTime() && ts.getEndTime() <= expectedEndTime)) {
                    //ts.printTsInfo();
                    ++alongTimeSlices;
                    for (int j = 0; j < ts.getNumNode(); ++j) {
                        int freeCores = ts.getAvailableCores().get(j);
                        long freeMemory = ts.getAvailableMemory().get(j);
                        VacantNode node = vacantNodes.get(j);
                        int numCore = ts.getPpn();

                        assert node.getNodeNo() == j;

                        freeCores = min(freeCores, node.getFreeCores());
                        assert freeCores >= -(M-1)*numCore;
                        assert freeCores <= numCore;                    
                        node.setFreeCores(freeCores);

                        freeMemory = min(freeMemory, node.getFreeMemory());
                        node.setFreeMemory(freeMemory);

                        boolean addFlag = false;
                        addFlag = (freeCores - requiredCoresPerNode >= -(M-1)*numCore);
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
        } else {
            boolean checkFlag[];
            checkFlag = new boolean[NodeConsciousScheduler.numNodes];
            int jobRequiredTime = job.getRequiredTime();
            for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
                jobRestTimeEachNode.add(jobRequiredTime);
                checkFlag[i] = true;
            }            
            TimeSlice lastTs = timeSlices.get(timeSlices.size()-1);

            // 0. Check memory hard-limit
            // 1. Calculate estimated multiplicity for each node.
            // 2. S += duration/multiplicity
            // 3. If ts.endTime > firstJobEndTime before S reaches actualTime, it nodes is fails            
            for (int i = 0; i < timeSlices.size(); ++i) {                
                TimeSlice ts = timeSlices.get(i);
                boolean continueFlag = false;
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    continueFlag |= checkFlag[j];
                }
                
                if (!continueFlag) break;

                for (int j = 0; j < ts.getNumNode(); ++j) {
                    if(!checkFlag[j]) continue;

                    int freeCores = ts.getAvailableCores().get(j);
                    VacantNode node = vacantNodes.get(j);
                    int numCore = ts.getPpn();

                    assert node.getNodeNo() == j;

                    freeCores = min(freeCores, node.getFreeCores());
                    assert freeCores >= -(M-1)*numCore;
                    assert freeCores <= numCore;
                    node.setFreeCores(freeCores);

                    // 0. Check memory hard-limit
                    long freeMemory = ts.getAvailableMemory().get(j);
                    node.setFreeMemory(freeMemory);
                    if (scheduleUsingMemory && (freeMemory < requiredMemoryPerNode)) {
                        checkFlag[j] = false;
                        continue;
                    }
                    
                    // 1. Calculate estimated multiplicity for each node.
                    int tentativeMultiplicityForBackfillJob = caculateTentativeMultiplicity(freeCores, requiredCoresPerNode);
                    if (tentativeMultiplicityForBackfillJob > NodeConsciousScheduler.M) {
                        checkFlag[j] = false;
                        continue;
                    }


                    // 3. If ts.endTime > firstJobEndTime before S reaches actualTime, it nodes is fails
/*
                    if (ts.getEndTime() > firstJobStartTime) {
                        boolean mayChangeFirstJobOCState = (((M*numCore - freeCores)/M) == ((M*numCore - freeCores + requiredCoresPerNode)/M));
                        if (tmpAssignNodesNo.contains((Integer)j) && mayChangeFirstJobOCState) {
                            checkFlag[j] = false;                        
                            continue;
                        } else if (!tmpAssignNodesNo.contains((Integer)j)) {
                            for (int k = 0; k < ts.getNumNode(); ++k) {
                                if (k == j) continue;
                                int freeCoresOnNodeK = ts.getAvailableCores().get(k);
                                freeCoresOnNodeK = min(freeCoresOnNodeK, vacantNodes.get(k).getFreeCores());
                                boolean mayChangeFirstJobOCStateOnNodeK = (((M*numCore - freeCoresOnNodeK)/M) == ((M*numCore - freeCoresOnNodeK + requiredCoresPerNode)/M));
                                long freeMemoryOnNodeK = ts.getAvailableMemory().get(k);
                                freeMemoryOnNodeK = min(freeMemoryOnNodeK, vacantNodes.get(k).getFreeMemory());
                                boolean isMoreThanMemoryLimit = requiredMemoryPerNode > freeMemoryOnNodeK;                                
                                if (tmpAssignNodesNo.contains((Integer)k) && (mayChangeFirstJobOCStateOnNodeK || isMoreThanMemoryLimit) && nodes.contains(vacantNodes.get(k))) {
                                    nodes.remove(vacantNodes.get(k));
                                }
                            }
                        }
                    }
*/
                    // 3. If ts.endTime > firstJobEndTime before S reaches actualTime, it nodes is fails
                    if (ts.getEndTime() > firstJobStartTime) {
                        // If firstJob will use node j
                        if (tmpAssignNodesNo.contains((Integer)j)) {
                            int nowMultiplicityForFirstJob = caculateTentativeMultiplicity(freeCores, 0);
                            boolean mayChangeFirstJobOCState = (nowMultiplicityForFirstJob != tentativeMultiplicityForBackfillJob);

                            if (mayChangeFirstJobOCState) {
                                checkFlag[j] = false;                        
                                continue;
                            }
                        } else if (!tmpAssignNodesNo.contains((Integer)j)) {
                            for (int k: tmpAssignNodesNo) {
                                if (k == j) continue;
                                int freeCoresOnNodeK = ts.getAvailableCores().get(k);
                                freeCoresOnNodeK = min(freeCoresOnNodeK, vacantNodes.get(k).getFreeCores());
                                
                                int tentativeMultiplicityForBackfillJobOnNodeK = caculateTentativeMultiplicity(freeCoresOnNodeK, requiredCoresPerNode);
                                int nowMultiplicityForFirstJob = caculateTentativeMultiplicity(freeCoresOnNodeK, 0);
                                boolean mayChangeFirstJobOCStateOnNodeK = (tentativeMultiplicityForBackfillJobOnNodeK != nowMultiplicityForFirstJob);
                                long freeMemoryOnNodeK = ts.getAvailableMemory().get(k);
                                freeMemoryOnNodeK = min(freeMemoryOnNodeK, vacantNodes.get(k).getFreeMemory());
                                boolean isMoreThanMemoryLimit = requiredMemoryPerNode > freeMemoryOnNodeK;                                
                                if ((mayChangeFirstJobOCStateOnNodeK || isMoreThanMemoryLimit) && nodes.contains(vacantNodes.get(k))) {
                                    nodes.remove(vacantNodes.get(k));
                                }
                            }
                        }
                    }
                    
                    // 2. restTime -= duration/multiplicity
                    int restTime = jobRestTimeEachNode.get(j) - ts.getDuration()/tentativeMultiplicityForBackfillJob;
                    jobRestTimeEachNode.set(j, restTime);
                    if (restTime <= 0) {                   
                        nodes.add(node);
                        checkFlag[j] = false;
                    }
                }
            }    
        }
        if (!backfillFlag && (alongTimeSlices == 0)) return nodes;

        /*
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            VacantNode node = vacantNodes.get(i);
            int freeCores = node.getFreeCores();
            node.setFreeCores(freeCores/alongTimeSlices);
        }
        */

        /* If cnt == alongTimeSlices, the job is executable on the nodes along the timeSlices */        
        if (!backfillFlag) {
            for (int i = 0; i < vacantNodeCount.size(); ++i) {
                int cnt = vacantNodeCount.get(i);
                if (cnt == alongTimeSlices) {
                    VacantNode node = vacantNodes.get(i);
                    assert node.getNodeNo() == i;
                    nodes.add(node);
                }
            }
        }

        /* Check OCStateLevel */
        int numCore = NodeConsciousScheduler.numCores;
        for (int i = 0; i < nodes.size(); ++i) {
            VacantNode node = nodes.get(i);
            int freeCores = node.getFreeCores();            

            for (int j = 1; j <= NodeConsciousScheduler.M; ++j) {
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

    private int caculateTentativeMultiplicity(int freeCores, int requiredCoresPerNode) {
        int numUsingCore = NodeConsciousScheduler.numCores - freeCores + requiredCoresPerNode;
        double divide = (double)numUsingCore/NodeConsciousScheduler.numCores;
        return (int)ceil(divide);
    }

    private boolean checkEffectOnVictimJob(Job victimJob) {
        return false;
    }

    private void updateAllNodesInfo(LinkedList<TimeSlice> timeSlices, ArrayList<NodeInfo> tmpAllNodesInfo, int tmpStartTime) {
        Job endingJobInTimeSlices = new Job();
        
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        
        for (Job execJob : executingJobList) {
            int endTimeInTimeSlice = execJob.getOccupiedTimeInTimeSlices();
            if (endTimeInTimeSlice == tmpStartTime) {
                NodeConsciousScheduler.sim.freeResources(execJob, tmpAllNodesInfo);
            }
        }        

        for (Job tempJob : temporallyScheduledJobList) {
            int endTimeInTimeSlice = tempJob.getOccupiedTimeInTimeSlices();
            if (endTimeInTimeSlice == tmpStartTime) {
                NodeConsciousScheduler.sim.freeResources(tempJob, tmpAllNodesInfo);
            }
        }
    }
}

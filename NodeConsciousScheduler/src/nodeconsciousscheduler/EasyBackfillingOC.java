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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import static nodeconsciousscheduler.Constants.CANNOT_START;
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
            
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesAt(currentTime, job);
            assert checkTimeSlicesAndAllNodeInfo();
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {
                temporallyScheduledJobList.add(job);
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
                
                if (OCStateLevelForJob == 1) {
                    int expectedEndTime = startTime + job.getRequiredTime();
                    makeTimeslices(expectedEndTime);
                    job.setSpecifiedExecuteTime(expectedEndTime);

                    job.setOCStateLevel(OCStateLevelForJob);
                    assignJob(startTime, job, assignNodesNo);

                    int trueEndTime = startTime + job.getActualExecuteTime();
                    result.add(new Event(EventType.START, startTime, job));
                    printThrowENDEvent(currentTime, trueEndTime, job, EventType.END);
                    result.add(new Event(EventType.END, trueEndTime, job));
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
                        resultForVictim = modifyTheENDEventTimeForTheJobByJobId(currentTime, victimJobId, OCStateLevelForJob);
                        for (Event ev: resultForVictim) {
                            result.add(ev);
                        }
                        Job victimJob = getJobByJobId(victimJobId); // O(N)
                        /*

                        Job victimJob = getJobByJobId(victimJobId); // O(N)
                        int victimStartTime = victimJob.getStartTime();
                        assert victimStartTime >= 0 && victimStartTime <= currentTime;                        
                        

                        measureCurrentExecutingTime(currentTime, victimJob);
                        victimJob.setPreviousMeasuredTime(currentTime);


                        int currentOCStateLevel = victimJob.getOCStateLevel();
                        assert (currentOCStateLevel + 1 == OCStateLevelForBackfillJob) || (currentOCStateLevel == OCStateLevelForBackfillJob);

                        printOCStateLevelTransition(currentOCStateLevel, OCStateLevelForBackfillJob, victimJobId);
                        victimJob.setOCStateLevel(OCStateLevelForBackfillJob);
                        int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);                        

                        printThrowENDEvent(currentTime, trueEndTime, victimJob);
                        result.add(new Event(EventType.END, trueEndTime, victimJob));
                        result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob)); // This event delete the END event already exists in the event queue. 
                        */
                        victimJob.getCoexistingJobs().add(opponentJobId);                        
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
                        assert oldExpectedEndTime <= newExpectedEndTime;
                        
                        /*  2-2. Update the timeslice between current and new expectedEndTime */
                        int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob);

                        makeTimeslices(currentTime);
                        makeTimeslices(newExpectedEndTime);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob);
                    }
                    
                    /* For opponent job */
                    job.setOCStateLevel(OCStateLevelForJob);
                    //int expectedEndTime = startTime + job.getRequiredTime() * OCStateLevelForBackfillJob;
                    int expectedEndTime = calculateNewExpectedEndTime(startTime, job);
                    makeTimeslices(expectedEndTime);
                    job.setSpecifiedExecuteTime(expectedEndTime);

                    /* Set previous time. */
                    /* This is opponent, so it is not "switched" now. But, this value is needed. */
                    job.setPreviousMeasuredTime(startTime);
                    
                    assignJob(startTime, job, assignNodesNo);

                    //int trueEndTime = startTime + job.getActualExecuteTime() * OCStateLevelForBackfillJob;
                    int trueEndTime = calculateNewActualEndTime(startTime, job);
                    result.add(new Event(EventType.START, startTime, job));
                    printThrowENDEvent(currentTime, trueEndTime, job, EventType.END);
                    result.add(new Event(EventType.END, trueEndTime, job));                    
                }
            } else break;
        }
        
        if (waitingQueue.size() <= 1) return result;
        
        /* Backfilling */
        Queue<Job> tailWaitingQueue = copyWaitingQueue();
        Job firstJob = waitingQueue.peek();

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

        Collections.sort(canExecuteTmpNodes);
        ArrayList<Integer> assignTmpNodesNo = new ArrayList<Integer>();
        int OCStateLevelForFirstJob = 1;
        for (int i = 0; i < firstJob.getRequiredNodes(); ++i) {
            assignTmpNodesNo.add(canExecuteTmpNodes.get(i).getNodeNo());
            OCStateLevelForFirstJob = max(OCStateLevelForFirstJob, canExecuteTmpNodes.get(i).getOCStateLevel());
        }

        // TODO: Erase below line
        System.out.println("size: " + canExecuteTmpNodes.size() + ", firstJob: " + firstJob.getJobId() + ", tentative start time: " + startTimeFirstJob);

        assert canExecuteTmpNodes.size() >= firstJob.getRequiredNodes();
        assert startTimeFirstJob != CANNOT_START;

        /* We must handle the copy of original list */
        /* TODO?: Define the appropriate clonable setting */
        LinkedList<TimeSlice> tmpTimeSlices = cloneTimeSlices(timeSlices);
        ArrayList<NodeInfo> tmpAllNodesInfo = cloneAllNodesInfo(NodeConsciousScheduler.sim.getAllNodesInfo());

        firstJob.setOCStateLevel(OCStateLevelForFirstJob);
        makeTimeslices(startTimeFirstJob, tmpTimeSlices);
        int endTimeFirstJob = calculateNewExpectedEndTime(startTimeFirstJob, firstJob);
        makeTimeslices(endTimeFirstJob, tmpTimeSlices);
        assignFirstJobTemporally(tmpTimeSlices, tmpAllNodesInfo, startTimeFirstJob, firstJob, canExecuteTmpNodes);

        ArrayList<VacantNode> canExecuteNodesEasyBackfiling;
        while (tailWaitingQueue.size() > 0) {
            Job backfillJob = tailWaitingQueue.poll();

            canExecuteNodesEasyBackfiling = canExecutableNodesOnBackfilling(currentTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob);

            if (canExecuteNodesEasyBackfiling.size() >= backfillJob.getRequiredNodes()) {
                // TO CONSIDER:
                // Is it appropriate way to select the nodes?                
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

                boolean backfillFlag = true;
                for (int victimJobId: victimJobs) {
                    Job victimJob = getJobByJobId(victimJobId);
                    if (OCStateLevelForBackfillJob > victimJob.getOCStateLevel()) {
                        backfillFlag = false;
                        break;
                    }
                }
                
                if (backfillFlag) continue;

                System.out.println("Succeed Backfill Job: " + backfillJob.getJobId() + ", at " + currentTime);
                
                Iterator itr = waitingQueue.iterator();
                while (itr.hasNext()) {
                    Job deleteJob = (Job) itr.next();
                    if (deleteJob.getJobId() == backfillJob.getJobId()) {
                        itr.remove();
                        break;
                    }
                }


                backfillJob.setStartTime(startTime);

                makeTimeslices(startTime);
                makeTimeslices(startTime, tmpTimeSlices);
                
                if (OCStateLevelForBackfillJob == 1) {
                    int expectedEndTime = startTime + backfillJob.getRequiredTime();
                    makeTimeslices(expectedEndTime);
                    backfillJob.setSpecifiedExecuteTime(expectedEndTime);

                    backfillJob.setOCStateLevel(OCStateLevelForBackfillJob);
                    assignJob(startTime, backfillJob, assignNodesNo);
                    assignJobForTmp(startTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, assignNodesNo);
                    
                    int trueEndTime = startTime + backfillJob.getActualExecuteTime();
                    result.add(new Event(EventType.START, startTime, backfillJob));
                    printThrowENDEvent(currentTime, trueEndTime, backfillJob, EventType.END);
                    result.add(new Event(EventType.END, trueEndTime, backfillJob));
                } else {
                    int backfillJobId = backfillJob.getJobId();
                    System.out.println("OC allocating, opponent jobId: " + backfillJobId + ", OCStateLevel: " + OCStateLevelForBackfillJob + ", victim jobId: " + victimJobs);

                    backfillJob.setCoexistingJobs(victimJobs);
                    /* 1. Modify the victim job's end time in event queue */
                    for (int victimJobId: victimJobs) {                        
                        ArrayList<Event> resultForVictim = new ArrayList<Event>();
                        resultForVictim = modifyTheENDEventTimeForTheJobByJobId(currentTime, victimJobId, OCStateLevelForBackfillJob);
                        for (Event ev: resultForVictim) {
                            result.add(ev);
                        }
                        Job victimJob = getJobByJobId(victimJobId); // O(N)
                        victimJob.getCoexistingJobs().add(backfillJobId);                        
                    }

                    /* 2. Modify the time slices (variable name: timeSlices defined in Class Scheduler) */
                    for (int victimJobId: victimJobs) {
                        Job victimJob = getJobByJobId(victimJobId); // O(N)

                        /*  2-1. Calculate new expectedEndTime */
                        int oldExpectedEndTime = victimJob.getSpecifiedExecuteTime(); // This field name is bad. Difficult to interpret.
                        int newExpectedEndTime = calculateNewExpectedEndTime(currentTime, victimJob);
                        victimJob.setSpecifiedExecuteTime(newExpectedEndTime);
                        assert oldExpectedEndTime <= newExpectedEndTime;
                        
                        /*  2-2. Update the timeslice between current and new expectedEndTime */
                        int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob);
                        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, victimJob, tmpTimeSlices);

                        makeTimeslices(currentTime);
                        makeTimeslices(currentTime, tmpTimeSlices);
                        makeTimeslices(newExpectedEndTime);
                        makeTimeslices(newExpectedEndTime, tmpTimeSlices);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob);
                        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob, tmpTimeSlices);
                    }
                    
                    backfillJob.setOCStateLevel(OCStateLevelForBackfillJob);
                    int expectedEndTime = calculateNewExpectedEndTime(currentTime, backfillJob);
                    makeTimeslices(expectedEndTime);
                    makeTimeslices(expectedEndTime, tmpTimeSlices);
                    backfillJob.setSpecifiedExecuteTime(expectedEndTime);

                    assignJob(startTime, backfillJob, assignNodesNo);
                    assignJobForTmp(startTime, tmpTimeSlices, tmpAllNodesInfo, backfillJob, assignNodesNo);

                    backfillJob.setPreviousMeasuredTime(startTime);
                    int trueEndTime = calculateNewActualEndTime(startTime, backfillJob);
                    result.add(new Event(EventType.START, startTime, backfillJob));
                    printThrowENDEvent(currentTime, trueEndTime, backfillJob, EventType.END);
                    result.add(new Event(EventType.END, trueEndTime, backfillJob));
                }

            }
        }
        // TODO: firstJob clear
        firstJob.setOCStateLevel(1);
        
        return result;
    }
    
    @Override
    protected ArrayList<VacantNode> canExecutableNodesAt(int currentTime, LinkedList<TimeSlice> timeSlices, Job job) {
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
        int requiredCoresPerNode = job.getRequiredCores()/job.getRequiredNodes();
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) ++requiredCoresPerNode;
        
        int requiredNodes = job.getRequiredNodes();

        int jobId = job.getJobId();
        int startTime = currentTime;
        int expectedEndTime = startTime + job.getRequiredTime();
//        int expectedEndTime = UNUPDATED;
        int alongTimeSlices = 0;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);

/*
            if (i == 0) {
                ArrayList<Integer> multiplicityEachNodeAtNow = new ArrayList<Integer>();
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    int freeCores = ts.getAvailableCores().get(j);
                    multiplicityEachNodeAtNow.add(calculateEstimatedMultiplicityAtNow(ts, freeCores, requiredCoresPerNode));
                }
                Collections.sort(multiplicityEachNodeAtNow);
                int multiplicity = UNUPDATED;
                multiplicity = multiplicityEachNodeAtNow.get(requiredNodes-1);
                expectedEndTime = startTime + job.getRequiredTime()*multiplicity;
                assert expectedEndTime != UNUPDATED;
            }
*/            
            if ((ts.getStartTime() <= startTime && startTime < ts.getEndTime())
                    || (ts.getStartTime() < expectedEndTime && expectedEndTime <= ts.getEndTime())
                    || (startTime <= ts.getStartTime() && ts.getEndTime() <= expectedEndTime)) {
                //ts.printTsInfo();
                ++alongTimeSlices;
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    int freeCores = ts.getAvailableCores().get(j);
                    VacantNode node = vacantNodes.get(j);
                    
                    assert node.getNodeNo() == j;

                    freeCores = min(freeCores, node.getFreeCores());
                    node.setFreeCores(freeCores);

                    int numCore = ts.getPpn();
//                    if (freeCores >= requiredCoresPerNode ) {
                    if (freeCores - requiredCoresPerNode >= -(NodeConsciousScheduler.M-1)*numCore) {
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
 
}

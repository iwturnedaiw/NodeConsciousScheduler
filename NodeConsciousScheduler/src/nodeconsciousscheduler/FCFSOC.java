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
import static nodeconsciousscheduler.Constants.UNUPDATED;

/**
 *
 * @author sminami
 */
public class FCFSOC extends FCFS {
    @Override
    protected ArrayList<Event> scheduleJobsStartAt(int currentTime) {
        ArrayList<Event> result = new ArrayList<Event>();
        while (!waitingQueue.isEmpty()) {
            Job job = waitingQueue.peek();
            int jobId = job.getJobId();
            
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesImmediately(currentTime, job);
            assert checkTimeSlicesAndAllNodeInfo();
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {
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
                    System.out.println("OC allocating, opponent jobId: " + opponentJobId + ", victim jobId: " + victimJobs);
                    /* Set victim jobList for the opponent job */
                    job.setCoexistingJobs(victimJobs);
                    
                    /* Clear TimeSlece after currentTime */
//                    clearTimeSliceAfter(currentTime);

                    
                    /* 1. Modify the victim job's end time in event queue */
                    /*  1-1. Measure the executing time at current time for each victim jobs. */
                    /*  1-2. Calculate new trueEndTime */
                    /*  1-3. Rethrow the END event set the time */
                    for (int victimJobId: victimJobs) {
                        /* 1. Modify the victim job's end time in event queue */
                        Job victimJob = getJobByJobId(victimJobId); // O(N)
                        int victimStartTime = victimJob.getStartTime();
                        assert victimStartTime >= 0 && victimStartTime <= currentTime;                        
                        
                        /*  1-1. Measure the executing time at current time for each victim jobs. */
                        measureCurrentExecutingTime(currentTime, victimJob);
                        victimJob.setPreviousMeasuredTime(currentTime);

                        /*  1-2. Calculate new trueEndTime */
                        int currentOCStateLevel = victimJob.getOCStateLevel();
                        assert (currentOCStateLevel + 1 == OCStateLevelForJob) || (currentOCStateLevel == OCStateLevelForJob);
                        /* debug */
                        printOCStateLevelTransition(currentOCStateLevel, OCStateLevelForJob, victimJobId);
                        victimJob.setOCStateLevel(OCStateLevelForJob);
                        int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);                        

                        /*  1-3. Rethrow the END event set the time */
                        result.add(new Event(EventType.END, trueEndTime, victimJob));
                        result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob)); // This event delete the END event already exists in the event queue. 
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
                    //int expectedEndTime = startTime + job.getRequiredTime() * OCStateLevelForJob;
                    int expectedEndTime = calculateNewExpectedEndTime(startTime, job);
                    makeTimeslices(expectedEndTime);
                    job.setSpecifiedExecuteTime(expectedEndTime);

                    /* Set previous time. */
                    /* This is opponent, so it is not "switched" now. But, this value is needed. */
                    job.setPreviousMeasuredTime(startTime);
                    
                    assignJob(startTime, job, assignNodesNo);

                    //int trueEndTime = startTime + job.getActualExecuteTime() * OCStateLevelForJob;
                    int trueEndTime = calculateNewActualEndTime(startTime, job);;
                    result.add(new Event(EventType.START, startTime, job));
                    result.add(new Event(EventType.END, trueEndTime, job));                    
                }
            } else break;
        }
        return result;
    }

    @Override
    protected ArrayList<Event> scheduleJobsOCState(Event ev) {
        ArrayList<Event> result = new ArrayList<Event>();
        
        /* Setting */
        int currentTime = ev.getOccurrenceTime();
        Job endingJob = ev.getJob();
        int endingJobId = endingJob.getJobId();
        Set<Integer> coexistingJobs = endingJob.getCoexistingJobs();

        /* Change appropriate OCStateLevel for coexisting jobs */
        for (int coexistingJobId : coexistingJobs) {
            Job coexistingJob = getJobByJobId(coexistingJobId);
            /* Calculate new OCStateLevel for coexisting jobs */
            ArrayList<UsingNodes> coexistingJobUsingNodeList = coexistingJob.getUsingNodesList();

            Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
            assert coexistingJobCoexistingJob.contains(endingJobId);
            
            if (coexistingJobUsingNodeList == null) {
                System.out.println("debug) OCCURRED HERE, ending job Id: " + endingJobId + ", currentTime: " + currentTime);
                System.out.println("debug) usingNodeList: ");
                ArrayList<UsingNodes> endingJobUsingNodeList = endingJob.getUsingNodesList();
                for (int i = 0; i < endingJobUsingNodeList.size(); ++i) {
                    UsingNodes node = endingJobUsingNodeList.get(i);
                    System.out.print("\tNode" + node.getNodeNum() + ": ");
                    ArrayList<Integer> cores = node.getUsingCoreNum();
                    for (int j = 0; j < cores.size(); ++j) {
                        System.out.print(cores.get(j));
                        if (j != cores.size() -1 ) System.out.print(", ");                        
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
            int multiplicityAlongNodes = -1;
            for (int i = 0; i < coexistingJobUsingNodeList.size(); ++i) {
                int multiplicityAlongCores = -1;

                UsingNodes usingNode = coexistingJobUsingNodeList.get(i);
                int usingNodeId = usingNode.getNodeNum();
                ArrayList<Integer> usingCoreId = usingNode.getUsingCoreNum();

                CoreOCState coreOCState = new CoreOCState();
                for (int j = 0; j < usingCoreId.size(); ++j) {
                    int coreId = usingCoreId.get(j);
                    coreOCState = existEndingJobOnTheCoreAndReturnMultiplicity(usingNodeId, coreId, endingJobId);
                    boolean OCReleaseFlag = true;
                    OCReleaseFlag = coreOCState.isEndingJobFlag();
                    int multiplicity = coreOCState.getMultiplicity();
                    assert multiplicity >= 1;
                    assert multiplicity <= NodeConsciousScheduler.M;
                    if (OCReleaseFlag) {
                        removeEndingJobFromJobList(usingNodeId, coreId, endingJobId);
                        coexistingJob.getCoexistingJobs().remove(endingJobId);
                        --multiplicity;
                    }
                    multiplicityAlongCores = max(multiplicityAlongCores, multiplicity);
                }
                assert multiplicityAlongCores > 0;
                multiplicityAlongNodes = max(multiplicityAlongNodes, multiplicityAlongCores);
            }
            assert multiplicityAlongNodes > 0;
            int OCStateLevel = multiplicityAlongNodes;
            
            measureCurrentExecutingTime(currentTime, coexistingJob);

            // Calculate the new actual end time and throw new event
            coexistingJob.setOCStateLevel(OCStateLevel);
            int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
            coexistingJob.setPreviousMeasuredTime(currentTime);
            result.add(new Event(EventType.END, trueEndTime, coexistingJob));
            result.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob));
        }
        
        /* Clear TimeSlece after currentTime */
        /* TODO: this part is redundant because all executing job needs to be measured */
        /* In theory, I need to measure the victim jobs only, I think. */
        clearTimeSliceAfter(currentTime);
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        for (int i = 0; i < executingJobList.size(); ++i) {
            // ts処理
            Job executingJob = executingJobList.get(i);
            int previousMeasuredTime = executingJob.getPreviousMeasuredTime();
            if (previousMeasuredTime != currentTime) {
                measureCurrentExecutingTime(currentTime, executingJob);
                executingJob.setPreviousMeasuredTime(currentTime);
            }
            int expectedEndTime = calculateNewExpectedEndTime(currentTime, executingJob);
            executingJob.setSpecifiedExecuteTime(expectedEndTime);
            makeTimeslices(currentTime);
            makeTimeslices(expectedEndTime);
            assignJobForOnlyTimeSlices(currentTime, executingJob, expectedEndTime);
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
        int requiredCoresPerNode = job.getRequiredCores()/job.getRequiredNodes();
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) ++requiredCoresPerNode;

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
    
    private Set<Integer> searchVictimJobs(int startTime, Job job, ArrayList<Integer> assignNodesNo) {
        return searchVictimJobs(startTime, NodeConsciousScheduler.sim.getAllNodesInfo(), job, assignNodesNo);
    }
    
    /* Search victim jobs for the job */
    private Set<Integer> searchVictimJobs(int startTime, ArrayList<NodeInfo> allNodesInfo, Job job, ArrayList<Integer> assignNodesNo) {
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

    private void measureCurrentExecutingTime(int currentTime, Job victimJob, int OCStateLevel) {
        int currentOCStateLevel = victimJob.getOCStateLevel();
        
        int jobId = victimJob.getJobId();
        // System.out.println("JobId: " + jobId);
        /* measure current progress */
        int previousMeasuredTime = victimJob.getPreviousMeasuredTime();
        if (previousMeasuredTime == currentTime) return;
        // TODO: should be double, but now int.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int realDeltaTime = currentTime - previousMeasuredTime;
        cpuTimeForNow += realDeltaTime/currentOCStateLevel;
        victimJob.setCpuTimeForNow(cpuTimeForNow);
        
        if (OCStateLevel == 1) {
            int runningTimeDed = victimJob.getRunningTimeDed();
            runningTimeDed += realDeltaTime;
            victimJob.setRunningTimeDed(runningTimeDed);
        }
        else {
            int runningTimeOC = victimJob.getRunningTimeOC();
            runningTimeOC += realDeltaTime;
            victimJob.setRunningTimeOC(runningTimeOC);
        }
        
        return;
    }

    private int calculateNewActualEndTime(Job victimJob) {
        int startTime = victimJob.getStartTime();
        assert startTime >= 0;
        return calculateNewActualEndTime(startTime, victimJob);
    }

    private int calculateNewActualEndTime(int startTime, Job victimJob) {        
        /* calculate new actual End Time */
        int currentOCStateLevel = victimJob.getOCStateLevel(); // This value is after-updated.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int actualExecuteTime = victimJob.getActualExecuteTime();
        int restActualExecuteTime = (actualExecuteTime - cpuTimeForNow)*currentOCStateLevel;
        int trueEndTime = startTime + restActualExecuteTime;

        return trueEndTime;        
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

    /* This method return the exepeceted end time. */
    private int calculateNewExpectedEndTime(int currentTime, Job victimJob) {
        int currentOCStateLevel = victimJob.getOCStateLevel(); // This value is after-updated.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int requiredTime = victimJob.getRequiredTime();
        int restRequiredTime = (requiredTime - cpuTimeForNow) * currentOCStateLevel;
        int expectedEndTime = currentTime + restRequiredTime;

        return expectedEndTime;
    }

    protected void assignJobForOnlyTimeSlices(int startTime, Job job, int expectedEndTime) {
        ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
        ArrayList<UsingNodes> usingNodeList = job.getUsingNodesList();
        
        for (int i = 0; i < usingNodeList.size(); ++i) {
            UsingNodes node = usingNodeList.get(i);
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

    private void measureCurrentExecutingTime(int currentTime, Job executingJob) {
        int OCStateLevel = executingJob.getOCStateLevel();
        measureCurrentExecutingTime(currentTime, executingJob, OCStateLevel);
    }

    private Set<Integer> cloneVictimJobs(Set<Integer> victimJobs) {
        Set<Integer> copiedVictimJobs = new HashSet<Integer>();
        for (int victimJobId: victimJobs) {
            copiedVictimJobs.add(victimJobId);
        }
        return copiedVictimJobs;
    }

    private Job getJobByJobId(int coexistingJobId) {

        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        int i;
        Job job = new Job();
        for (i = 0; i < executingJobList.size(); ++i) {
            job = executingJobList.get(i);
            int executingJobId = job.getJobId();
            if(executingJobId == coexistingJobId) break;
        }
        assert job.getStartTime() >= 0;
        return job;
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

    private CoreOCState existEndingJobOnTheCoreAndReturnMultiplicity(int nodeId, int coreId, int endingJobId) {
        // コアをなめる
        // そのコアは、OCStateLevel個のジョブがいるか？　かつ、
        // endingJobIdがいるか？
        // この場合のみtrueを返す
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

    private boolean checkTimeSlicesAndAllNodeInfo() {
        /* For TimeSlices */
        ArrayList<Integer> freeCoreInTimeSlices = new ArrayList<Integer>();
        TimeSlice ts = timeSlices.get(0);
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

        boolean ret = true;
        for (int i = 0; i < freeCoreInAllNodeInfo.size(); ++i) {
            if (freeCoreInTimeSlices.get(i) != freeCoreInAllNodeInfo.get(i)) ret = false;
        }
        
        return ret;
    }

    private void printOCStateLevelTransition(int currentOCStateLevel, int newOCStateLevelForJob, int victimJobId) {
        if (currentOCStateLevel + 1 == newOCStateLevelForJob) {
            System.out.print("debug) OC State is updated from " + currentOCStateLevel + " to " + newOCStateLevelForJob);
        } else if (currentOCStateLevel == newOCStateLevelForJob) {
            System.out.print("debug) OC State is not updated, remains " + currentOCStateLevel);            
        }
        System.out.println(", jobId: " + victimJobId);
    }

    private int getTimeSliceIndexEndTimeEquals(int oldExpectedEndTime) {
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

    private void refiilFreeCoresInTimeSlices(int currentTime, int timeSliceIndex, Job victimJob) {
        ArrayList<UsingNodes> usingNodes = victimJob.getUsingNodesList();

        for (int i = 0; i <= timeSliceIndex; ++i) {
            TimeSlice ts = timeSlices.get(i);
            assert currentTime <= ts.getStartTime(); 
            ArrayList<Integer> availableCores = ts.getAvailableCores();
            for (int j = 0; j < usingNodes.size(); ++j) {
                UsingNodes usingNode = usingNodes.get(j);
                int nodeId = usingNode.getNodeNum();
                int releaseCore = usingNode.getNumUsingCores();
                
                int freeCore = availableCores.get(nodeId);
                freeCore += releaseCore;
                availableCores.set(nodeId, freeCore);
                assert freeCore <= NodeConsciousScheduler.numCores;
            }
        } 

    }

    private void reallocateOccupiedCoresInTimeSlices(int currentTime, int newExpectedEndTime, Job victimJob) {
        ArrayList<UsingNodes> usingNodes = victimJob.getUsingNodesList();
        
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            assert currentTime <= ts.getStartTime();
            assert ts.getStartTime() < newExpectedEndTime;

            ArrayList<Integer> availableCores = ts.getAvailableCores();
            for (int j = 0; j < usingNodes.size(); ++j) {
                UsingNodes usingNode = usingNodes.get(j);
                int nodeId = usingNode.getNodeNum();
                int occupiedCore = usingNode.getNumUsingCores();
                
                int freeCore = availableCores.get(nodeId);
                freeCore -= occupiedCore;
                availableCores.set(nodeId, freeCore);

                assert freeCore <= NodeConsciousScheduler.numCores;
                assert freeCore >= -(NodeConsciousScheduler.M-1) * NodeConsciousScheduler.numCores;                
            }
            if (ts.getEndTime() == newExpectedEndTime) break;
        }
    }
}

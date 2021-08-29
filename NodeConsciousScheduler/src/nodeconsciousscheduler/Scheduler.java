/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import static java.lang.Math.max;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
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
public abstract class Scheduler {
    protected Queue<Job> waitingQueue;
    protected LinkedList<TimeSlice> timeSlices;
    protected LinkedList<TimeSlice> completedTimeSlices;
    ArrayList<Job> temporallyScheduledJobList;
    
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
        makeTimeslices(currentTime, this.timeSlices);
    }
    
    protected void makeTimeslices(int currentTime, LinkedList<TimeSlice> timeSlices) {        
        if (existSliceStartAt(currentTime, timeSlices))
            return;

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
        int expectedEndTime = job.getSpecifiedExecuteTime();
        
        for (int i = 0; i < timeSlices.size(); ++i) {            
            TimeSlice ts = timeSlices.get(i);        
//            ts.printTsInfo();
            int endTime = ts.getEndTime();
            
            if (endTime <= expectedEndTime) {                
                ts.refillResources(job);
            }
        }
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
    
    
    protected boolean checkTimeSlicesAndAllNodeInfo() {
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
            int freeCoreInTimeSlice = freeCoreInTimeSlices.get(i);
            int freeCoreInNodeInfo = freeCoreInAllNodeInfo.get(i);
            if (freeCoreInTimeSlice == freeCoreInNodeInfo) continue;
            if (freeCoreInTimeSlice > freeCoreInNodeInfo) {
                System.out.println("Differ freecore value at node " + i);
                System.out.println("Check multiple END Event with the same event time are.");
            } else {
                ret = false;
            }
        }
        
        // Executing Job List
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        int totalFreeCores;
        int totalOccupiedCores = 0;
        for (int i = 0; i < executingJobList.size(); ++i) {
            Job job = executingJobList.get(i);
            totalOccupiedCores += job.getRequiredCores();
        }
        for (int i = 0; i < temporallyScheduledJobList.size(); ++i) {
            Job job = temporallyScheduledJobList.get(i);
            totalOccupiedCores += job.getRequiredCores();
        }
        totalFreeCores = NodeConsciousScheduler.numCores*NodeConsciousScheduler.numNodes - totalOccupiedCores;
        
        int totalFreeCoresFromAllNodeInfo = 0;
        for (int i = 0; i < freeCoreInAllNodeInfo.size(); ++i) {
            totalFreeCoresFromAllNodeInfo += freeCoreInAllNodeInfo.get(i);
        }
        
        if (totalFreeCores != totalFreeCoresFromAllNodeInfo) ret = false;

        return ret;
    }
    
    protected ArrayList<Event> scheduleJobsOnEnd(Event ev) {
        int currentTime = ev.getJob().getFinishedTime();
        
        // TODO
        // unifyTimeSlices(currentTime);
        makeTimeslices(currentTime);
        reduceTimeslices(currentTime, ev);
        completeOldSlices(currentTime);

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
        newEventsLoadBalancing = loadBalancing(ev);
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
        completeOldSlices(currentTime);
        
        enqueue(ev);

        ArrayList<Event> newEvents = new ArrayList<Event>();
        newEvents = scheduleJobsStartAt(currentTime);
        
        return newEvents;
    }

    protected void assignJob(int startTime, LinkedList<TimeSlice> timeSlices, ArrayList<NodeInfo> allNodesInfo, Job job, ArrayList<Integer> assignNodesNo, boolean tmpFlag) {
        int addedPpn = job.getRequiredCores()/job.getRequiredNodes();
        int expectedEndTime = startTime + (job.getRequiredTime()-job.getCpuTimeForNow())*job.getOCStateLevel();

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
                ArrayList<Integer> jobList = eachCore.getJobList();
                int coreId = eachCore.getCoreId();
                for (int k = 0; k < jobList.size(); ++k) {
                    int usingJobId = jobList.get(k);
                    if (usingJobId == jobId) {
                        coreNum.add(coreId);
                    }
                }
                // Collections.sort(coreNum);
            }
            
            UsingNode node = new UsingNode(nodeNo, addedPpn, coreNum);
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
                modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, endingJobId);
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
                result.addAll(modifyTheENDEventTimeForTheJobByJobId(currentTime, coexistingJobId, OCStateLevelCoexistingJob));
                coexistingJob.setOCStateLevel(OCStateLevelCoexistingJob);
                /* 4. Modify the TimeSlices */
                Set<Integer> coexistingJobCoexistingJob = coexistingJob.getCoexistingJobs();
                modifyTheTimeSlices(coexistingJob, coexistingJobCoexistingJob, currentTime, endingJobId);                
            }

            ArrayList<UsingNode> migratingJobUsingNodeList = migratingJob.getUsingNodesList();
            int newOCStateLevel = checkMultiplicityAlongNodes(migratingJobUsingNodeList, BLANK_JOBID, migratingJobId);
            modifyTheENDEventTime(migratingJob, migratingJobId, currentTime, newOCStateLevel, result);

            Set<Integer> migratingJobCoexistingJob = migratingJob.getCoexistingJobs();
            modifyTheTimeSlices(migratingJob, migratingJobCoexistingJob, currentTime, endingJobId);
            
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

     protected Job getJobByJobId(int coexistingJobId) {

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

    protected void measureCurrentExecutingTime(int currentTime, Job executingJob) {
        int OCStateLevel = executingJob.getOCStateLevel();
        measureCurrentExecutingTime(currentTime, executingJob, OCStateLevel);
    }
     
    protected void measureCurrentExecutingTime(int currentTime, Job victimJob, int OCStateLevel) {
        int currentOCStateLevel = victimJob.getOCStateLevel();

        int jobId = victimJob.getJobId();
        // System.out.println("JobId: " + migratingJobId);
        /* measure current progress */
        int previousMeasuredTime = victimJob.getPreviousMeasuredTime();
        if (previousMeasuredTime == currentTime) {
            return;
        }
        // TODO: should be double, but now int.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int realDeltaTime = currentTime - previousMeasuredTime;
        cpuTimeForNow += realDeltaTime / currentOCStateLevel;
        victimJob.setCpuTimeForNow(cpuTimeForNow);

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

    protected int calculateNewActualEndTime(int startTime, Job victimJob) {
        /* calculate new actual End Time */
        int currentOCStateLevel = victimJob.getOCStateLevel(); // This value is after-updated.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int actualExecuteTime = victimJob.getActualExecuteTime();
        int restActualExecuteTime = (actualExecuteTime - cpuTimeForNow) * currentOCStateLevel;
        int trueEndTime = startTime + restActualExecuteTime;

        return trueEndTime;
    }
    
 
    protected void printThrowENDEvent(int currentTime, int trueEndTime, Job job, EventType evt) {
        printThrowENDEvent(currentTime, trueEndTime, job, evt, 1);
    }
    
    protected void printThrowENDEvent(int currentTime, int trueEndTime, Job job, EventType evt, int tabno) {
        for (int i = 0; i < tabno; ++i) System.out.print("\t");
        System.out.println("debug) Throw " + evt + " event: jobId " + job.getJobId() + ", newTrueEndTime: " + trueEndTime + " at " + currentTime);
    }
    
    protected void modifyTheENDEventTime(Job coexistingJob, int coexistingJobId, int currentTime, int OCStateLevel, ArrayList<Event> result) {
        int coexistingStartTime = coexistingJob.getStartTime();
        assert coexistingStartTime >= 0;
        assert coexistingStartTime <= currentTime;

        //  1-1. Measure the executing time at current time for each victim jobs.
        measureCurrentExecutingTime(currentTime, coexistingJob);
        coexistingJob.setPreviousMeasuredTime(currentTime);

        //  1-2. Calculate new trueEndTime
        int currentOCStateLevel = coexistingJob.getOCStateLevel();
        assert (currentOCStateLevel - 1 == OCStateLevel) || (currentOCStateLevel == OCStateLevel);
        // debug
        printOCStateLevelTransition(currentOCStateLevel, OCStateLevel, coexistingJobId);
        int oldTrueEndTime = coexistingJob.getEndEventOccuranceTimeNow();
        coexistingJob.setOCStateLevel(OCStateLevel);
        int trueEndTime = calculateNewActualEndTime(currentTime, coexistingJob);
//        assert trueEndTime <= oldTrueEndTime;

        //  1-3. Rethrow the END event set the time
        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
        if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && currentTime != oldTrueEndTime) {
            printThrowENDEvent(currentTime, trueEndTime, coexistingJob, EventType.END);
            result.add(new Event(EventType.END, trueEndTime, coexistingJob));
            coexistingJob.setEndEventOccuranceTimeNow(trueEndTime);
            printThrowENDEvent(currentTime, trueEndTime, coexistingJob, EventType.DELETE_FROM_END);
            result.add(new Event(EventType.DELETE_FROM_END, currentTime, coexistingJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
        }
    }
    /* This method return the exepeceted end time. */
    protected int calculateNewExpectedEndTime(int currentTime, Job victimJob) {
        int currentOCStateLevel = victimJob.getOCStateLevel(); // This value is after-updated.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int requiredTime = victimJob.getRequiredTime();
        int restRequiredTime = (requiredTime - cpuTimeForNow) * currentOCStateLevel;
        int expectedEndTime = currentTime + restRequiredTime;

        return expectedEndTime;
    }

    protected int calculateApproximateEndTime(int currentTime, Job victimJob, int OCStateLevel) {
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int requiredTime = victimJob.getRequiredTime();
        int restRequiredTime = (requiredTime - cpuTimeForNow) * OCStateLevel;
        int expectedEndTime = currentTime + restRequiredTime;

        return expectedEndTime;
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

        for (int i = 0; i <= timeSliceIndex; ++i) {
            TimeSlice ts = timeSlices.get(i);
            assert currentTime <= ts.getStartTime();
            ArrayList<Integer> availableCores = ts.getAvailableCores();
            for (int j = 0; j < usingNodes.size(); ++j) {
                UsingNode usingNode = usingNodes.get(j);
                int nodeId = usingNode.getNodeNum();
                int releaseCore = usingNode.getNumUsingCores();

                int freeCore = availableCores.get(nodeId);
                freeCore += releaseCore;
                availableCores.set(nodeId, freeCore);
                assert freeCore <= NodeConsciousScheduler.numCores;
            }
        }
    }
    
    protected void reallocateOccupiedCoresInTimeSlices(int currentTime, int newExpectedEndTime, Job victimJob) {
        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, victimJob, this.timeSlices);
    }

    protected void reallocateOccupiedCoresInTimeSlices(int currentTime, int newExpectedEndTime, Job victimJob, LinkedList<TimeSlice> timeSlices) {
        ArrayList<UsingNode> usingNodes = victimJob.getUsingNodesList();

        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            assert currentTime <= ts.getStartTime();
            assert ts.getStartTime() < newExpectedEndTime;

            ArrayList<Integer> availableCores = ts.getAvailableCores();
            for (int j = 0; j < usingNodes.size(); ++j) {
                UsingNode usingNode = usingNodes.get(j);
                int nodeId = usingNode.getNodeNum();
                int occupiedCore = usingNode.getNumUsingCores();

                int freeCore = availableCores.get(nodeId);
                freeCore -= occupiedCore;
                availableCores.set(nodeId, freeCore);

                assert freeCore <= NodeConsciousScheduler.numCores;
                assert freeCore >= -(NodeConsciousScheduler.M - 1) * NodeConsciousScheduler.numCores;
            }
            if (ts.getEndTime() == newExpectedEndTime) {
                break;
            }
        }
    }
    
    protected void modifyTheTimeSlices(Job coexistingJob, Set<Integer> coexistingJobCoexistingJob, int currentTime, int endingJobId) {
        // 3. Modify the timeSlices
        boolean notEndFlag = (endingJobId == BLANK_JOBID);

        //  2-1. Get old expectedEndTime            
        int oldExpectedEndTime = coexistingJob.getSpecifiedExecuteTime(); // This field name is bad. Difficult to interpret.
        int newExpectedEndTime = calculateNewExpectedEndTime(currentTime, coexistingJob);
        coexistingJob.setSpecifiedExecuteTime(newExpectedEndTime);
        // assert newExpectedEndTime <= oldExpectedEndTime;
        printDifferenceExpectedEndTime(oldExpectedEndTime, newExpectedEndTime, coexistingJob.getJobId());

        //  2-2. Update the timeslice between current and new expectedEndTime           
        int timeSliceIndex = getTimeSliceIndexEndTimeEquals(oldExpectedEndTime);
        refiilFreeCoresInTimeSlices(currentTime, timeSliceIndex, coexistingJob);
        makeTimeslices(currentTime);
        makeTimeslices(newExpectedEndTime);
        reallocateOccupiedCoresInTimeSlices(currentTime, newExpectedEndTime, coexistingJob);

        if (!notEndFlag) {
            coexistingJobCoexistingJob.remove(endingJobId);
        }
    }     
    
    protected ArrayList<Event> modifyTheENDEventTimeForTheJobByJobId(int currentTime, int victimJobId, int OCStateLevel) {
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
        int trueEndTime = calculateNewActualEndTime(currentTime, victimJob);
        assert oldTrueEndTime <= trueEndTime;
        victimJob.setOCStateLevel(currentOCStateLevel);
        
        /*  1-3. Rethrow the END event set the time */
        //if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime && trueEndTime < oldTrueEndTime) {
        if (currentOCStateLevel != OCStateLevel && currentTime != trueEndTime) {
            printThrowENDEvent(currentTime, trueEndTime, victimJob, EventType.END);
            result.add(new Event(EventType.END, trueEndTime, victimJob));
            victimJob.setEndEventOccuranceTimeNow(trueEndTime);
            printThrowENDEvent(currentTime, trueEndTime, victimJob, EventType.DELETE_FROM_BEGINNING);
            result.add(new Event(EventType.DELETE_FROM_BEGINNING, currentTime, victimJob, oldTrueEndTime)); // This event delete the END event already exists in the event queue. 
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
}

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
    
    protected boolean existSliceStartAt(int currentTime) {
        for (int i = 0; i < timeSlices.size(); ++i) {
            if (timeSlices.get(i).getStartTime() == currentTime) {
                return true;
            }
        }
        return false;
    }
    
    protected int sliceIndexToSplit(int currentTime) {
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
        if (existSliceStartAt(currentTime))
            return;

        int breakIndex = UNUPDATED;
        breakIndex = sliceIndexToSplit(currentTime);

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
        
        ArrayList<Event> newEventsOCState = new ArrayList<Event>();
        newEventsOCState = checkCoexistingJobsOCStateAndModifyENDEventAndTimeSlices(ev);

        ArrayList<Event> newEvents = new ArrayList<Event>();

        for (int i = 0; i < newEventsOCState.size(); ++i) {
            newEvents.add(newEventsOCState.get(i));
        }

        loadBalancing(ev);
        
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
                    jobList.add(jobId);                    
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

    private void loadBalancing(Event ev) {
        Job endingJob = ev.getJob();
        ArrayList<UsingNode> usingNodeList = endingJob.getUsingNodesList();
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        /* Check the all executing jobs */
        for (int i = 0; i < executingJobList.size(); ++i) {
            Job job = executingJobList.get(i);
            if(!checkUseSameNode(usingNodeList, job)) continue;
            System.out.println("\tdebug)jobId:" +job.getJobId());
            int OCStateLevel = job.getOCStateLevel();
            if (OCStateLevel == 1) {
                continue;
            }
            /* Calculate candidate cores the job migrates */
            ArrayList<MigrateTargetNode> migrateTargetNodes = calculateMigrateTargetCoresPerNode(job);
            /* Do migrate */
            NewAndDeletedCoexistingJobs newAndDeletedCoexistingJobs = doMigrate(job, migrateTargetNodes);
            printNewAndDeletedCoexistingJobs(newAndDeletedCoexistingJobs, job);
        }
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
            Collections.reverse(usingCoresWithMultiplicity);
            printUsingCoreInfo(job, nodeId, usingCoresWithMultiplicity, false);
            
            int migrationCnt = 0;
            
            /* Do migration */
            NewAndDeletedCoexistingJobs  newAndDeletedCoexistingJobsAlongCores = new NewAndDeletedCoexistingJobs();
            for (CoreInfo ci: usingCoresWithMultiplicity) {
                NewAndDeletedCoexistingJobs  newAndDeletedCoexistingJobsOnTheCore = new NewAndDeletedCoexistingJobs();                
                if (ci.getJobList().size() > 1) {
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

}




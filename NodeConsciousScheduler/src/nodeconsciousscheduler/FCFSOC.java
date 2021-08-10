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
            
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesImmediately(currentTime, job);
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
                job.setStartTime(startTime);
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
                    /* Search victimt jobs */
                    Set<Integer> victimJobs = new HashSet<Integer>();
                    victimJobs = FCFSOC.this.searchVictimJobs(startTime, job, assignNodesNo);
                    System.out.println("OC allocating, opponent jobId: " + job.getJobId() + ", victim jobId: " + victimJobs);
                    
                    /* For victim jobs */                    
                    ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();

                    /* Clear TimeSlece after currentTime */
                    clearTimeSliceAfter(currentTime);
                    
                    /* TODO: reduce cost from O(N^2) to O(N) */
                    /*       Now, O(N^2) */
                    for (int victimJobId : victimJobs) {
                        int i;
                        for (i = 0; i < executingJobList.size(); ++i) {
                            Job candidateJob = executingJobList.get(i);
                            int candidateId = candidateJob.getJobId();
                            if (victimJobId == candidateId) {
                                // Measure the executing time at this time
                                measureCurrentExecutingTime(currentTime, candidateJob, OCStateLevelForJob);
                                candidateJob.setOCStateLevel(OCStateLevelForJob);
                                // Calculate the new actual end time and throw new event
                                int trueEndTime = calculateNewActualEndTime(currentTime, candidateJob);
                                result.add(new Event(EventType.END, trueEndTime, candidateJob));        
                                result.add(new Event(EventType.DELETE, currentTime, candidateJob));
                            }
                            // Refresh TimeSlices.
                            // Add the timeslices for all executing jobs
                            int expectedEndTime = calculateNewExpectedEndTime(currentTime, candidateJob);
                            makeTimeslices(currentTime);
                            makeTimeslices(expectedEndTime);
                            assignJobForOnlyTimeSlices(currentTime, candidateJob, expectedEndTime);

                            // Modify the usingNode? No, it's unneeded.
                            // Anything else?     
                        }
                        assert i != executingJobList.size() + 1;
                    }

                    /* For opponent job */
                    //int expectedEndTime = startTime + job.getRequiredTime() * OCStateLevelForJob;
                    int expectedEndTime = calculateNewExpectedEndTime(startTime, job);
                    makeTimeslices(expectedEndTime);
                    job.setSpecifiedExecuteTime(expectedEndTime);

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
                    VacantNode node = vacantNodes.get(j);
                    
                    assert node.getNodeNo() == j;

                    freeCores = min(freeCores, node.getFreeCores());
                    node.setFreeCores(freeCores);

                    int numCore = ts.getPpn();
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
        
        /* measure current progress */
        int previousSwitchedTime = victimJob.getPreviousSwitchedTime();
        // TODO: should be double, but now int.
        int cpuTimeForNow = victimJob.getCpuTimeForNow();
        int realDeltaTime = currentTime - previousSwitchedTime;
        cpuTimeForNow += realDeltaTime/currentOCStateLevel;
        victimJob.setCpuTimeForNow(cpuTimeForNow);
        
        if (OCStateLevel == 1) victimJob.setRunningTimeDed(realDeltaTime);
        else victimJob.setRunningTimeOC(realDeltaTime);
        
        return;
    }

    private int calculateNewActualEndTime(Job victimJob) {
        int startTime = victimJob.getStartTime();
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
        }
        
        assert clearIndex != UNUPDATED;

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
}

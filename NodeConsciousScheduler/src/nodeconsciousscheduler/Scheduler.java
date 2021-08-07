/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import static nodeconsciousscheduler.Constants.NOT_BREAK;

/**
 *
 * @author sminami
 */
public abstract class Scheduler {
    protected Queue<Job> waitingQueue;
    protected LinkedList<TimeSlice> timeSlices;
    protected LinkedList<TimeSlice> completedTimeSlices;

    
    abstract protected ArrayList<Event> scheduleJobsStartAt(int currentTime);
    
    protected void enqueue(Event ev) {
        waitingQueue.add(ev.getJob());
    }
    
    protected void init() {
        this.waitingQueue = new LinkedList<Job>();
        this.timeSlices = new LinkedList<TimeSlice>();
        this.timeSlices.add(new TimeSlice());
        this.completedTimeSlices = new LinkedList<TimeSlice>();    
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

        int breakIndex = NOT_BREAK;
        breakIndex = sliceIndexToSplit(currentTime);

        if (breakIndex != NOT_BREAK) {
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
        
        ArrayList<Event> newEvents = new ArrayList<Event>();
        newEvents = scheduleJobsStartAt(currentTime);
        
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
            ArrayList<ArrayList<Integer>> occupiedCores = node.getOccupiedCores();
            for (int j = 0; j < numCores; ++j) {
                ArrayList<Integer> eachCore = occupiedCores.get(j);
                if (eachCore.size() == 0) {
                    eachCore.add(jobId);                    
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
        ArrayList<UsingNodes> nodes = job.getUsingNodesList();

        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
            ArrayList<Integer> coreNum = new ArrayList<Integer>();
            
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeNo);
            ArrayList<ArrayList<Integer>> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < occupiedCores.size(); ++j) {
                ArrayList<Integer> eachCore = occupiedCores.get(j);
                for (int k = 0; k < eachCore.size(); ++k) {
                    int usingJobId = eachCore.get(k);
                    if (usingJobId == jobId) {
                        coreNum.add(j);
                    }
                }
            }
            
            UsingNodes node = new UsingNodes(nodeNo, addedPpn, coreNum);
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
            ArrayList<ArrayList<Integer>> occupiedCores = node.getOccupiedCores();
            
            for (int j = 0; j < numCores; ++j) {
                ArrayList<Integer> eachCore = occupiedCores.get(j);
                // 下のifも不要
                if (eachCore.size() == 0) {
                    eachCore.add(jobId);                    
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
        ArrayList<UsingNodes> nodes = job.getUsingNodesList();

        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
            ArrayList<Integer> coreNum = new ArrayList<Integer>();
            
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeNo);
            ArrayList<ArrayList<Integer>> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < occupiedCores.size(); ++j) {
                ArrayList<Integer> eachCore = occupiedCores.get(j);
                for (int k = 0; k < eachCore.size(); ++k) {
                    int usingJobId = eachCore.get(k);
                    if (usingJobId == jobId) {
                        coreNum.add(j);
                    }
                }
            }
            
            UsingNodes node = new UsingNodes(nodeNo, addedPpn, coreNum);
            nodes.add(node);
        }
    }
    
}


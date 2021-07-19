/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 *
 * @author sminami
 */
class FCFS implements Scheduler {
    Queue<Job> waitingQueue;
    LinkedList<TimeSlice> timeSlices;
    LinkedList<TimeSlice> completedTimeSlices;
    
    FCFS() {
        this.waitingQueue = new LinkedList<Job>();
        this.timeSlices = new LinkedList<TimeSlice>();
        this.timeSlices.add(new TimeSlice());
        this.completedTimeSlices = new LinkedList<TimeSlice>();
    }
    public void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    public ArrayList<Event> scheduleJobsOnSubmission(Event ev) {
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

    
    @Override
    public ArrayList<Event> scheduleJobsOnEnd(Event ev) {
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
    
    private void enqueue(Event ev) {
        waitingQueue.add(ev.getJob());
    }

    private ArrayList<Event> scheduleJobsStartAt(int currentTime) {
        ArrayList<Event> result = new ArrayList<Event>();
        while (!waitingQueue.isEmpty()) {
            Job job = waitingQueue.peek();
            
            ArrayList<VacantNode> canExecuteNodes = canExecutableNodesImmediately(currentTime, job);
            if (canExecuteNodes.size() >= job.getRequiredNodes()) {
                Collections.sort(canExecuteNodes);
                ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
                for (int i = 0; i < job.getRequiredNodes(); ++i) {
                    assignNodesNo.add(canExecuteNodes.get(i).getNodeNo());
                }

                waitingQueue.poll();
                int startTime = currentTime;
                job.setStartTime(startTime);
                makeTimeslices(startTime);
                
                int expectedEndTime = startTime + job.getRequiredTime();
                makeTimeslices(expectedEndTime);
                job.setSpecifiedExecuteTime(expectedEndTime);

                assignJob(startTime, job, assignNodesNo);

                int trueEndTime = startTime + job.getActualExecuteTime();                
                result.add(new Event(EventType.START, startTime, job));
                result.add(new Event(EventType.END, trueEndTime, job));
            } else break;
        }
        return result;
    }

    private void makeTimeslices(int currentTime) {
        
        if (exitSliceStartAt(currentTime))
            return;

        int breakIndex = -1;
        breakIndex = sliceIndexToSplit(currentTime);

        TimeSlice ts = timeSlices.get(breakIndex);
        LinkedList<TimeSlice> brokenSlices = ts.split(currentTime);
        timeSlices.remove(breakIndex);
        timeSlices.add(breakIndex, brokenSlices.get(0));
        timeSlices.add(breakIndex + 1, brokenSlices.get(1));
        //System.out.println(brokenSlices.get(0).getAvailableCores());
        //System.out.println(brokenSlices.get(1).getAvailableCores());
        
        
        return;
    }

    private int sliceIndexToSplit(int currentTime) {
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

    private boolean exitSliceStartAt(int currentTime) {
        for (int i = 0; i < timeSlices.size(); ++i) {
            if (timeSlices.get(i).getStartTime() == currentTime) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<VacantNode> canExecutableNodesImmediately(int currentTime, Job job) {
        /* Return variable
           This have the node no. with # of free core.
        */
        ArrayList<VacantNode> nodes = new ArrayList<VacantNode>();
        
        /* Working Variable */
        ArrayList<VacantNode> vacantNodes = new ArrayList<VacantNode>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodes.add(new VacantNode(i, 0));
        
        /* This is used for counting executable nodes */
        ArrayList<Integer> vacantNodeCount = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) vacantNodeCount.add(0);

        /* Calculate ppn */
        /* TODO: The case requiredCores ist not dividable  */
        int requiredCoresPerNode = job.getRequiredCores()/job.getRequiredNodes();
        if (job.getRequiredCores()%job.getRequiredNodes() != 0) ++requiredCoresPerNode;
        
        int alongTimeSlices = 0;
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            if (ts.getStartTime() <= currentTime && currentTime <= ts.getEndTime()) {
                ++alongTimeSlices;
                for (int j = 0; j < ts.getNumNode(); ++j) {
                    int freeCores = ts.getAvailableCores().get(j);
                    VacantNode node = vacantNodes.get(j);
                    
                    assert node.getNodeNo() == j;

                    int cores = node.getFreeCores();
                    node.setFreeCores(freeCores + cores);

                    if (freeCores >= requiredCoresPerNode ) {
                        int cnt = vacantNodeCount.get(j);
                        vacantNodeCount.set(j, ++cnt);
                    }
                }
            }
        }

        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            VacantNode node = vacantNodes.get(i);
            int freeCores = node.getFreeCores();
            node.setFreeCores(freeCores/alongTimeSlices);
        }
        
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

    private void completeOldSlices(int currentTime) {
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

    private void assignJob(int startTime, Job job, ArrayList<Integer> assignNodesNo) {

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
           
            NodeInfo node = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeNo);
            int numCores = node.getNumCores();
            
            int coreCnt = addedPpn;
            int numOccupiedCores = node.getNumOccupiedCores() + addedPpn;
            int numFreeCores = node.getNumFreeCores() - addedPpn;
            for (int j = 0; j < numCores; ++j) {
                if (node.getOccupiedCores().get(j) == Constants.UNUSED) {
                    node.getOccupiedCores().set(j, jobId);                    
                    --coreCnt;
                }            
                if (coreCnt == 0) break;
            }

            node.setNumOccupiedCores(numOccupiedCores);
            node.setNumFreeCores(numFreeCores);
            
        }
        
        /* Job Setting */
        ArrayList<UsingNodes> nodes = job.getUsingNodesList();

        for (int i = 0; i < assignNodesNo.size(); ++i) {
            int nodeNo = assignNodesNo.get(i);
            ArrayList<Integer> coreNum = new ArrayList<Integer>();
            
            NodeInfo nodeInfo = NodeConsciousScheduler.sim.getAllNodesInfo().get(nodeNo);
            ArrayList<Integer> occupiedCores = nodeInfo.getOccupiedCores();
            for (int j = 0; j < occupiedCores.size(); ++j) {
                int usingJobId = occupiedCores.get(j);
                if (usingJobId == jobId) {
                    coreNum.add(j);
                }
            }
            
            UsingNodes node = new UsingNodes(nodeNo, addedPpn, coreNum);
            nodes.add(node);
        }
    }

    private void reduceTimeslices(int currentTime, Event ev) {

        Job job = ev.getJob();
        int expectedEndTime = job.getSpecifiedExecuteTime();
        
        for (int i = 0; i < timeSlices.size(); ++i) {            
            TimeSlice ts = timeSlices.get(i);        
//            ts.printTsInfo();
            int startTime = ts.getStartTime();
            int endTime = ts.getEndTime();


//            if (currentTime == startTime || ( expectedEndTime != currentTime && (expectedEndTime == startTime || expectedEndTime == endTime)) ) {
//            if (currentTime == startTime || ( expectedEndTime != currentTime && expectedEndTime == endTime) ) {
            
            if (endTime <= expectedEndTime) {                
                ts.refillResources(job);
            }
            
//            ts.printTsInfo();
        }


/*
        LinkedList<TimeSlice> brokenSlices = ts.split(currentTime);
        timeSlices.remove(reduceIndex);
        timeSlices.add(reduceIndex, brokenSlices.get(0));
        timeSlices.add(reduceIndex + 1, brokenSlices.get(1));
        //System.out.println(brokenSlices.get(0).getAvailableCores());
        //System.out.println(brokenSlices.get(1).getAvailableCores());
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
*/
    }



}

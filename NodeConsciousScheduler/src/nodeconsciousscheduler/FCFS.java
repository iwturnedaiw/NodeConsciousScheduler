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
    public void schedule(Event ev) {
        System.out.println("Call schedule method");

        int currentTime = ev.getOccurrenceTime();
        
        // TODO
        // unifyTimeSlices(currentTime);
        makeTimeslices(currentTime);
        completeOldSlices(currentTime);
        
        enqueue(ev);

        scheduleJobs(currentTime);
        
        
        
        return;
    }

    private void enqueue(Event ev) {
        waitingQueue.add(ev.getJob());
    }

    private void scheduleJobs(int currentTime) {
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
                job.setStartTime(currentTime);
                makeTimeslices(currentTime);
                makeTimeslices(job.getStartTime() + job.getRequiredTime());
                assignJob(currentTime, job, assignNodesNo);
            } else break;
        }
        return;
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
        for (int i = 0; i < NodeConsciousScheduler.numNode; ++i) vacantNodes.add(new VacantNode(i, 0));
        
        /* This is used for counting executable nodes */
        ArrayList<Integer> vacantNodeCount = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNode; ++i) vacantNodeCount.add(0);

        /* Calculate ppn */
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

        for (int i = 0; i < NodeConsciousScheduler.numNode; ++i) {
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

    private void assignJob(int currentTime, Job job, ArrayList<Integer> assignNodesNo) {
        for (int i = 0; i < timeSlices.size(); ++i) {
            TimeSlice ts = timeSlices.get(i);
            if (ts.getStartTime() <= currentTime && currentTime <= ts.getEndTime()) {
                //ts.get
                // FROM THIS PART
                // 1. Reduce the free cores
                // 2. Register the job
            }
        }
    }
}

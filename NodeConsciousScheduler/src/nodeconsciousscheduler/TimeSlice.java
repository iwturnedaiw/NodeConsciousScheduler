/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.LinkedList;
import static nodeconsciousscheduler.Constants.TS_ENDTIME;

/**
 *
 * @author sminami
 */
class TimeSlice implements Cloneable {
    private int startTime;
    private int endTime;
    private int duration;
    private ArrayList<Integer> availableCores;
    private int ppn;
    private int numNode;
    private ArrayList<Long> availableMemory;
    
    TimeSlice() {
        this.startTime = 0;
        this.endTime = TS_ENDTIME;
        this.duration = this.endTime - this.startTime;
        this.numNode = NodeConsciousScheduler.numNodes;
        this.ppn = NodeConsciousScheduler.numCores;
        availableCores = new ArrayList<Integer>();
        availableMemory = new ArrayList<Long>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            availableCores.add(NodeConsciousScheduler.numCores);
            availableMemory.add(NodeConsciousScheduler.memory);
        }
    }

    TimeSlice(int startTime) {
        this.startTime = startTime;
        this.endTime = TS_ENDTIME;
        this.duration = this.endTime - this.startTime;
        this.numNode = NodeConsciousScheduler.numNodes;
        this.ppn = NodeConsciousScheduler.numCores;
        availableCores = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            availableCores.add(NodeConsciousScheduler.numCores);
            availableMemory.add(NodeConsciousScheduler.memory);            
        }
    }
    
    public int getStartTime() {
        return startTime;
    }

    public int getEndTime() {
        return endTime;
    }

    public int getDuration() {
        return duration;
    }

    public ArrayList<Integer> getAvailableCores() {
        return availableCores;
    }

    public int getPpn() {
        return ppn;
    }

    public int getNumNode() {
        return numNode;
    }

    public ArrayList<Long> getAvailableMemory() {
        return availableMemory;
    }

    public void setEndTime(int endTime) {
        this.endTime = endTime;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    LinkedList<TimeSlice> split(int currentTime) {
        TimeSlice first = this.clone();
        first.endTime = currentTime;
        first.duration = first.endTime - first.getStartTime();
        
        TimeSlice second = this.clone();
        second.startTime = currentTime;
        second.duration = second.endTime - currentTime;
//        second.availableCores = (ArrayList<Integer>) this.getAvailableCores().clone();
        
        LinkedList<TimeSlice> result = new LinkedList<TimeSlice>();
        result.add(first);
        result.add(second);
        return result;
    }
    
    
    @Override
    public TimeSlice clone() {
        TimeSlice clonedItem = null;
        try {
            // Object型で返ってくるのでキャストが必要
            clonedItem = (TimeSlice)super.clone();
            clonedItem.availableCores = (ArrayList<Integer>) this.getAvailableCores().clone();
            clonedItem.availableMemory = (ArrayList<Long>) this.getAvailableMemory().clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clonedItem;
    }

    void refillResources(Job job) {
        int endTime = this.endTime;
        int expectedEndTime = job.getOccupiedTimeInTimeSlices();
        
        ArrayList<UsingNode> usingNodesList = job.getUsingNodesList();
        
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();

        for (int i = 0; i < usingNodesList.size(); ++i) {
                UsingNode usingNode = usingNodesList.get(i);
                int nodeNo = usingNode.getNodeNum();
                
                int usingCores = usingNode.getNumUsingCores();
                ArrayList<Integer> nodes = this.getAvailableCores();
                int numFreeCores = nodes.get(nodeNo);
                numFreeCores += usingCores;
                assert numFreeCores <= NodeConsciousScheduler.numCores;
                nodes.set(nodeNo, numFreeCores);
                
                if (scheduleUsingMemory) {
                    long mpn = job.getMaxMemory();
                    ArrayList<Long> memories = this.getAvailableMemory();
                    long freeMemory = memories.get(nodeNo);
                    freeMemory += mpn;
                    assert freeMemory <= NodeConsciousScheduler.memory;
                    memories.set(nodeNo, freeMemory);
                }
        }

    }

    void printTsInfo() {
        System.out.println(this.startTime + "-" + this.endTime + ": " +  this.availableCores);

    }

    void assignResources(Job job) {
        int M = NodeConsciousScheduler.M;
        int numCore = NodeConsciousScheduler.numCores;
        
        int addedPpn = job.getRequiredCoresPerNode();
        long addedMpn = job.getMaxMemory();
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();
        ArrayList<Integer> cores = this.getAvailableCores();
        ArrayList<Long> memories = this.getAvailableMemory();
        ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
        ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
        for (int i = 0; i < usingNodes.size(); ++i) {
            assignNodesNo.add(usingNodes.get(i).getNodeNum());
        }
        for (int j = 0; j < assignNodesNo.size(); ++j) {
            int nodeNo = assignNodesNo.get(j);
            int core = cores.get(nodeNo);
            core -= addedPpn;
            assert core >= -(M-1) * numCore; 
            cores.set(nodeNo, core);

            if (scheduleUsingMemory) {
                long memory = memories.get(nodeNo);
                memory -= addedMpn;
                assert memory >= 0;
                memories.set(nodeNo, memory);
            }
        }
    }
    void assignResourcesAtNode(int n, Job job) {
        int M = NodeConsciousScheduler.M;
        int numCore = NodeConsciousScheduler.numCores;
        
        int addedPpn = job.getRequiredCoresPerNode();
        long addedMpn = job.getMaxMemory();
        boolean scheduleUsingMemory = NodeConsciousScheduler.sim.isScheduleUsingMemory();
        ArrayList<Integer> cores = this.getAvailableCores();
        ArrayList<Long> memories = this.getAvailableMemory();
        ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
        ArrayList<Integer> assignNodesNo = new ArrayList<Integer>();
        for (int i = 0; i < usingNodes.size(); ++i) {
            assignNodesNo.add(usingNodes.get(i).getNodeNum());
        }
        assert assignNodesNo.contains(n);

        int nodeNo = n;        
        int core = cores.get(nodeNo);
        core -= addedPpn;
        assert core >= -(M-1) * numCore; 
        cores.set(nodeNo, core);

        if (scheduleUsingMemory) {            
            long memory = memories.get(nodeNo);
            memory -= addedMpn;
            assert memory >= 0;
            memories.set(nodeNo, memory);
        }
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 *
 * @author sminami
 */
class TimeSlice implements Cloneable {
    int startTime;
    int endTime;
    int duration;
    ArrayList<Integer> availableCores;
    int ppn;
    int numNode;
    
    TimeSlice() {
        this.startTime = 0;
        this.endTime = 1 << 30;
        this.duration = this.endTime - this.startTime;
        this.numNode = NodeConsciousScheduler.numNodes;
        this.ppn = NodeConsciousScheduler.numCores;
        availableCores = new ArrayList<Integer>();
        for (int i = 0; i < NodeConsciousScheduler.numNodes; ++i) {
            availableCores.add(NodeConsciousScheduler.numCores);
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

    LinkedList<TimeSlice> split(int currentTime) {
        TimeSlice first = this.clone();
        first.endTime = currentTime;
        first.duration = first.endTime - first.getStartTime();
        
        TimeSlice second = this.clone();
        second.startTime = currentTime;
        second.duration = second.endTime - currentTime;
        second.availableCores = (ArrayList<Integer>) this.getAvailableCores().clone();
        
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
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clonedItem;
    }

    void refillResources(Job job) {
        int endTime = this.endTime;
        int expectedEndTime = job.getSpecifiedExecuteTime();
        
        ArrayList<UsingNodes> usingNodesList = job.getUsingNodesList();

        for (int i = 0; i < usingNodesList.size(); ++i) {
                UsingNodes usingNode = usingNodesList.get(i);
                int nodeNo = usingNode.getNodeNum();
                int usingCores = usingNode.getNumUsingCores();

                ArrayList<Integer> nodes = this.getAvailableCores();
                int numFreeCores = nodes.get(nodeNo);
                numFreeCores += usingCores;
                nodes.set(nodeNo, numFreeCores);
                
        }

    }

    void printTsInfo() {
        System.out.println(this.startTime + "-" + this.endTime + ": " +  this.availableCores);

    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;



/**
 *
 * @author sminami
 */
public class NodeInfo implements Cloneable{
    private int nodeNum;
    private int numCores;
    //private ArrayList<ArrayList<Integer>> occupiedCores;
    private ArrayList<CoreInfo> occupiedCores;
    private Set<Integer> executingJobIds;
    private int numOccupiedCores;
    private int numFreeCores;
    private int OCStateLevel;
    private long memorySize;
    private long freeMemory;
    private long occupiedMemory;
    
    
    NodeInfo(int nodeNum, int numCores) {
        this.nodeNum = nodeNum;
        this.numCores = numCores;
        this.occupiedCores = new ArrayList<CoreInfo>(numCores);
        this.executingJobIds = new HashSet<Integer>();
        init();
        this.numOccupiedCores = 0;
        this.numFreeCores = numCores;
        this.OCStateLevel = 0;
    }

    NodeInfo(int nodeNum, int numCores, long memory) {
        this.nodeNum = nodeNum;
        this.numCores = numCores;
        this.occupiedCores = new ArrayList<CoreInfo>(numCores);
        this.executingJobIds = new HashSet<Integer>();
        init();
        this.numOccupiedCores = 0;
        this.numFreeCores = numCores;
        this.OCStateLevel = 0;
        this.memorySize = memory;
        this.occupiedMemory = 0;
        this.freeMemory = memory;
    }
    
    public int getNodeNum() {
        return nodeNum;
    }

    public int getNumCores() {
        return numCores;
    }

    public ArrayList<CoreInfo> getOccupiedCores() {
        return occupiedCores;
    }

    public int getNumOccupiedCores() {
        return numOccupiedCores;
    }

    public int getNumFreeCores() {
        return numFreeCores;
    }

    public int getOCStateLevel() {
        return OCStateLevel;
    }

    public Set<Integer> getExecutingJobIds() {
        return executingJobIds;
    }
    
    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public void setNumCores(int numCores) {
        this.numCores = numCores;
    }

    public void setOccupiedCores(ArrayList<CoreInfo> occupiedCores) {
        this.occupiedCores = occupiedCores;
    }

    public void setNumOccupiedCores(int numOccupiedCores) {
        this.numOccupiedCores = numOccupiedCores;
    }

    public void setNumFreeCores(int numFreeCores) {
        this.numFreeCores = numFreeCores;
    }

    public void setOCStateLevel(int OCStateLevel) {
        this.OCStateLevel = OCStateLevel;
    }

    public void setExecutingJobIds(Set<Integer> executingJobIds) {
        this.executingJobIds = executingJobIds;
    }

    public long getMemorySize() {
        return memorySize;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public long getOccupiedMemory() {
        return occupiedMemory;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public void setOccupiedMemory(long occupiedMemory) {
        this.occupiedMemory = occupiedMemory;
    }
    
    private void init() {
        for (int i = 0; i < this.numCores; ++i) {
            CoreInfo eachCore = new CoreInfo(i);
            //jobIdList.add(Constants.UNUSED);
            this.occupiedCores.add(eachCore);            
        }
 
    }

    @Override
    public NodeInfo clone() {
        NodeInfo clonedItem = null;
        try {
            // Object型で返ってくるのでキャストが必要
            clonedItem = (NodeInfo)super.clone();
            ArrayList<CoreInfo> copiedOccupiedCores = new ArrayList<CoreInfo>();
            ArrayList<CoreInfo> orgOccupiedCores = this.getOccupiedCores();
            for (int i = 0; i < this.numCores; ++i) {
                CoreInfo coreInfo = orgOccupiedCores.get(i).clone();
                copiedOccupiedCores.add(coreInfo);
            }
            clonedItem.occupiedCores = copiedOccupiedCores;            
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clonedItem;
    }
    
    
}




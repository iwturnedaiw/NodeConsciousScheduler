/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;



/**
 *
 * @author sminami
 */
public class NodeInfo {
    private int nodeNum;
    private int numCores;
    private ArrayList<Integer> occupiedCores;
    private int numOccupiedCores;
    private int numFreeCores;
    private int OCStateLevel;
    
    
    NodeInfo(int nodeNum, int numCores) {
        this.nodeNum = nodeNum;
        this.numCores = numCores;
        this.occupiedCores = new ArrayList<Integer>();
        init();
        this.numOccupiedCores = 0;
        this.numFreeCores = numCores;
        this.OCStateLevel = 0;
    }

    public int getNodeNum() {
        return nodeNum;
    }

    public int getNumCores() {
        return numCores;
    }

    public ArrayList<Integer> getOccupiedCores() {
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

    public void setNodeNum(int nodeNum) {
        this.nodeNum = nodeNum;
    }

    public void setNumCores(int numCores) {
        this.numCores = numCores;
    }

    public void setOccupiedCores(ArrayList<Integer> occupiedCores) {
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

    
    
    
    private void init() {
        for (int i = 0; i < this.numCores; ++i) {
            this.occupiedCores.add(Constants.UNUSED);
        }
 
    }
    
    
}




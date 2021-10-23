/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

/**
 *
 * @author sminami
 */
class VacantNode implements Comparable<VacantNode>{
    private int nodeNo;
    private int freeCores;
    private int OCStateLevel;
    private long freeMemory;
    
    VacantNode() {
        this.nodeNo = -1;
        this.freeCores = 0;
        this.OCStateLevel = 1;
        this.freeMemory = 0;
    }
    
    VacantNode(int nodeNo, int freeCores) {
        this.nodeNo = nodeNo;
        this.freeCores = freeCores;
        this.OCStateLevel = 1;
    }
    
    VacantNode(int nodeNo, int freeCores, long freeMemory) {
        this.nodeNo = nodeNo;
        this.freeCores = freeCores;
        this.OCStateLevel = 1;
        this.freeMemory = freeMemory;
    }
    
    @Override
    public int compareTo(VacantNode o) {
        if (this.freeCores < o.freeCores) {
            return 1;
        }
        if (this.freeCores > o.freeCores) {
            return -1;
        }
        return 0;
    }

    public int getNodeNo() {
        return nodeNo;
    }

    public int getFreeCores() {
        return freeCores;
    }

    public int getOCStateLevel() {
        return OCStateLevel;
    }
    
    public void setNodeNo(int nodeNo) {
        this.nodeNo = nodeNo;
    }

    public void setFreeCores(int freeCores) {
        this.freeCores = freeCores;
    }

    public void setOCStateLevel(int OCStateLevel) {
        this.OCStateLevel = OCStateLevel;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }
}

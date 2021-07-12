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
    int nodeNo;
    int freeCores;
    
    VacantNode() {
        this.nodeNo = -1;
        this.freeCores = 0;
    }
    
    VacantNode(int nodeNo, int freeCores) {
        this.nodeNo = nodeNo;
        this.freeCores = freeCores;
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

    public void setNodeNo(int nodeNo) {
        this.nodeNo = nodeNo;
    }

    public void setFreeCores(int freeCores) {
        this.freeCores = freeCores;
    }
    
    
    
}

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
public class UsingNode implements Comparable<UsingNode>{
    int nodeNum;
    int numUsingCores;
    ArrayList<Integer> usingCoreNum;
    
    UsingNode() {}
    UsingNode(int nodeNum, int numUsingCores) {
        this.nodeNum = nodeNum;
        this.numUsingCores = numUsingCores;
    }

    UsingNode(int nodeNo, int addedPpn, ArrayList<Integer> coreNum) {
        this.nodeNum = nodeNo;
        this.numUsingCores = addedPpn;
        this.usingCoreNum = new ArrayList<Integer>();
        this.usingCoreNum = (ArrayList<Integer>) coreNum.clone();
    }

    @Override
    public int compareTo(UsingNode o) {
        if (this.nodeNum > o.nodeNum) {
            return 1;
        }
        if (this.nodeNum < o.nodeNum) {
            return -1;
        }
        return 0;
    }
    
    public int getNodeNum() {
        return nodeNum;
    }

    public int getNumUsingCores() {
        return numUsingCores;
    }

    public ArrayList<Integer> getUsingCoreNum() {
        return usingCoreNum;
    }
    
    
}

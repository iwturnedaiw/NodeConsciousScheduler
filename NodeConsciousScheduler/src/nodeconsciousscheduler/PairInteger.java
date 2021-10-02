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
class PairIntegers {
    private ArrayList<Integer> numWaitingJobs;
    private ArrayList<Integer> numNewArrivalJobs;
    private ArrayList<Integer> numNewStartJobs;
    private ArrayList<Integer> numCumulativeStartJobs;
    private ArrayList<Integer> numNewFinishedJobs;
    private ArrayList<Integer> numCumulativeFinishedJobs;
    
    PairIntegers(){}

    public ArrayList<Integer> getNumWaitingJobs() {
        return numWaitingJobs;
    }

    public ArrayList<Integer> getNumNewArrivalJobs() {
        return numNewArrivalJobs;
    }

    public void setNumWaitingJobs(ArrayList<Integer> numWaitingJobs) {
        this.numWaitingJobs = numWaitingJobs;
    }

    public void setNumNewArrivalJobs(ArrayList<Integer> numNewArrivalJobs) {
        this.numNewArrivalJobs = numNewArrivalJobs;
    }

    public ArrayList<Integer> getNumNewStartJobs() {
        return numNewStartJobs;
    }

    public void setNumNewStartJobs(ArrayList<Integer> numNewStartJobs) {
        this.numNewStartJobs = numNewStartJobs;
    }

    public ArrayList<Integer> getNumCumulativeStartJobs() {
        return numCumulativeStartJobs;
    }

    public void setNumCumulativeStartJobs(ArrayList<Integer> numCumulativeStartJobs) {
        this.numCumulativeStartJobs = numCumulativeStartJobs;
    }

    public ArrayList<Integer> getNumNewFinishedJobs() {
        return numNewFinishedJobs;
    }

    public ArrayList<Integer> getNumCumulativeFinishedJobs() {
        return numCumulativeFinishedJobs;
    }

    public void setNumNewFinishedJobs(ArrayList<Integer> numNewFinishedJobs) {
        this.numNewFinishedJobs = numNewFinishedJobs;
    }

    public void setNumCumulativeFinishedJobs(ArrayList<Integer> numCumulativeFinishedJobs) {
        this.numCumulativeFinishedJobs = numCumulativeFinishedJobs;
    }
    
    
    
    
    
}

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
    private ArrayList<Long> numCumulativeWaitingJobResources;
    private ArrayList<Long> numCumulativeStartedJobResources;
    private ArrayList<Long> numCumulativeFinishedJobResources;
    private ArrayList<Long> numCumulativeArrivalJobResources;
    
    private ArrayList<Long> numCumulativeWaitingJobMemoryResources;
    private ArrayList<Long> numCumulativeStartedJobMemoryResources;
    private ArrayList<Long> numCumulativeFinishedJobMemoryResources;
    private ArrayList<Long> numCumulativeArrivalJobMemoryResources;
    
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

    public ArrayList<Long> getNumCumulativeWaitingJobResources() {
        return numCumulativeWaitingJobResources;
    }

    public ArrayList<Long> getNumCumulativeStartedJobResources() {
        return numCumulativeStartedJobResources;
    }

    public ArrayList<Long> getNumCumulativeFinishedJobResources() {
        return numCumulativeFinishedJobResources;
    }

    public void setNumCumulativeWaitingJobResources(ArrayList<Long> numCumulativeWaitingJobResources) {
        this.numCumulativeWaitingJobResources = numCumulativeWaitingJobResources;
    }

    public void setNumCumulativeStartedJobResources(ArrayList<Long> numCumulativeStartedJobResources) {
        this.numCumulativeStartedJobResources = numCumulativeStartedJobResources;
    }

    public void setNumCumulativeFinishedJobResources(ArrayList<Long> numCumulativeFinishedJobResources) {
        this.numCumulativeFinishedJobResources = numCumulativeFinishedJobResources;
    }

    public ArrayList<Long> getNumCumulativeWaitingJobMemoryResources() {
        return numCumulativeWaitingJobMemoryResources;
    }

    public ArrayList<Long> getNumCumulativeStartedJobMemoryResources() {
        return numCumulativeStartedJobMemoryResources;
    }

    public ArrayList<Long> getNumCumulativeFinishedJobMemoryResources() {
        return numCumulativeFinishedJobMemoryResources;
    }

    public void setNumCumulativeWaitingJobMemoryResources(ArrayList<Long> numCumulativeWaitingJobMemoryResources) {
        this.numCumulativeWaitingJobMemoryResources = numCumulativeWaitingJobMemoryResources;
    }

    public void setNumCumulativeStartedJobMemoryResources(ArrayList<Long> numCumulativeStartedJobMemoryResources) {
        this.numCumulativeStartedJobMemoryResources = numCumulativeStartedJobMemoryResources;
    }

    public void setNumCumulativeFinishedJobMemoryResources(ArrayList<Long> numCumulativeFinishedJobMemoryResources) {
        this.numCumulativeFinishedJobMemoryResources = numCumulativeFinishedJobMemoryResources;
    }
    
    public ArrayList<Long> getNumCumulativeArrivalJobResources() {
        return numCumulativeArrivalJobResources;
    }

    public void setNumCumulativeArrivalJobResources(ArrayList<Long> numCumulativeArrivalJobResources) {
        this.numCumulativeArrivalJobResources = numCumulativeArrivalJobResources;
    }

    public ArrayList<Long> getNumCumulativeArrivalJobMemoryResources() {
        return numCumulativeArrivalJobMemoryResources;
    }

    public void setNumCumulativeArrivalJobMemoryResources(ArrayList<Long> numCumulativeArrivalJobMemoryResources) {
        this.numCumulativeArrivalJobMemoryResources = numCumulativeArrivalJobMemoryResources;
    }

}

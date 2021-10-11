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
class BasicResult {
    protected int groupId;
    protected int numJobs;
    protected int accumulatedNumNode;
    protected double averagedNumNode;
    protected int accumulatedNumCore;
    protected double averagedNumCore;
    protected int accumulatedTime;
    protected double averagedTime;
    protected long accumulatedCpuTime;
    protected double averagedCpuTime;
    protected int accumulatedMemoryFootprint;
    protected int numJobsSetMemory;
    protected double averagedMemoryFootprint;
    protected int accumulatedWaitTime;
    protected double averagedWaitTime;
    protected double largeJobRatio;
    protected ArrayList<Integer> slowdowns;
    protected ArrayList<Integer> slowdownsOC;
    protected int maxRunningTime;
    protected int maxWaitTime;
    protected int maxCpuTime;
    
    public int getGroupId() {
        return groupId;
    }

    public int getNumJobs() {
        return numJobs;
    }

    public int getAccumulatedNumNode() {
        return accumulatedNumNode;
    }

    public double getAveragedNumNode() {
        return averagedNumNode;
    }

    public int getAccumulatedNumCore() {
        return accumulatedNumCore;
    }

    public double getAveragedNumCore() {
        return averagedNumCore;
    }

    public int getAccumulatedTime() {
        return accumulatedTime;
    }

    public double getAveragedTime() {
        return averagedTime;
    }

    public long getAccumulatedCpuTime() {
        return accumulatedCpuTime;
    }

    public double getAveragedCpuTime() {
        return averagedCpuTime;
    }

    public int getAccumulatedMemoryFootprint() {
        return accumulatedMemoryFootprint;
    }

    public int getNumJobsSetMemory() {
        return numJobsSetMemory;
    }

    public double getAveragedMemoryFootprint() {
        return averagedMemoryFootprint;
    }

    public int getAccumulatedWaitTime() {
        return accumulatedWaitTime;
    }

    public double getAveragedWaitTime() {
        return averagedWaitTime;
    }

    public double getLargeJobRatio() {
        return largeJobRatio;
    }

    public ArrayList<Integer> getSlowdowns() {
        return slowdowns;
    }

    public ArrayList<Integer> getSlowdownsOC() {
        return slowdownsOC;
    }

    public int getMaxRunningTime() {
        return maxRunningTime;
    }

    public int getMaxWaitTime() {
        return maxWaitTime;
    }

    public int getMaxCpuTime() {
        return maxCpuTime;
    }

        
}

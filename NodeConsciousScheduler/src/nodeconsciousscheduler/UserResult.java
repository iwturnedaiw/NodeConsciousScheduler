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
class UserResult extends BasicResult{
    private int userId;
    
    UserResult(int userId, int numJobs, int accumulatedNumNode, double averagedNumNode, int accumulatedNumCore, double averagedNumCore, int accumulatedTime, 
                double averagedTime, long accumulatedCpuTime, double averagedCpuTime, long accumulatedMemoryFootprint, 
                int numJobsSetMemory, double averagedMemoryFootprint, int accumulatedWaitTime, double averagedWaitTime, 
                double largeJobRatio, ArrayList<Integer> slowdowns, ArrayList<Integer> slowdownsOC, int maxRunningTime, int maxWaitTime, int maxCpuTime, int maxTurnAroundTime, double maxSlowdown, double maxSlowdownOC) {
        this.userId = userId;
        this.numJobs = numJobs;
        this.accumulatedNumNode = accumulatedNumNode;
        this.averagedNumNode = averagedNumNode;
        this.accumulatedNumCore = accumulatedNumCore;
        this.averagedNumCore = averagedNumCore;
        this.accumulatedTime = accumulatedTime;
        this.averagedTime = averagedTime;
        this.accumulatedCpuTime = accumulatedCpuTime;
        this.averagedCpuTime = averagedCpuTime;
        this.accumulatedMemoryFootprint = accumulatedMemoryFootprint;
        this.numJobsSetMemory = numJobsSetMemory;
        this.averagedMemoryFootprint = averagedMemoryFootprint;
        this.accumulatedWaitTime = accumulatedWaitTime;
        this.averagedWaitTime = averagedWaitTime;
        this.largeJobRatio = largeJobRatio;
        this.slowdowns = slowdowns;
        this.slowdownsOC = slowdownsOC;        
        this.maxRunningTime = maxRunningTime;
        this.maxWaitTime = maxWaitTime;
        this.maxCpuTime = maxCpuTime;
        this.maxTurnAroundTime = maxTurnAroundTime;
        this.maxSlowdown = maxSlowdown;
        this.maxSlowdownOC = maxSlowdownOC;
    }

    public int getUserId() {
        return userId;
    }

    
}

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
public class Job {
    private int jobId;
    private int submitTime;
    private int actualExecuteTime;
    private int requiredTime;
    private int requiredCores;
    private int requiredNodes;
    private int runningTimeDed;
    private int runningTimeOC;
    private int startTime;
    private int finishedTime;
    private int waitTime;
    private int numNodes;
    private ArrayList<UsingNodes> usingNodesList;
    
    Job(int submitTime, int actualExecuteTime, int requiredTime, int requiredCores, int requiredNodes) {
        this.submitTime = submitTime;
        this.actualExecuteTime = actualExecuteTime;
        this.requiredTime = requiredTime;
        this.requiredCores = requiredCores;
        this.requiredNodes = requiredNodes;
        
        this.startTime = -1;
        this.finishedTime = 2 << 30;
        this.waitTime = -1;
    }

    Job(int jobId, int submitTime, int actualExecuteTime, int requiredTime, int requiredCores, int requiredNodes) {
        this.jobId = jobId;
        this.submitTime = submitTime;
        this.actualExecuteTime = actualExecuteTime;
        this.requiredTime = requiredTime;
        this.requiredCores = requiredCores;
        this.requiredNodes = requiredNodes;
        
        this.startTime = -1;
        this.finishedTime = 2 << 30;
        this.waitTime = -1;
    }
    
    
    public int getSubmitTime() {
        return submitTime;
    }

    public int getActualExecuteTime() {
        return actualExecuteTime;
    }

    public int getRequiredTime() {
        return requiredTime;
    }

    public int getRequiredCores() {
        return requiredCores;
    }

    public int getRunningTimeDed() {
        return runningTimeDed;
    }

    public int getRunningTimeOC() {
        return runningTimeOC;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getFinishedTime() {
        return finishedTime;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public int getNumNodes() {
        return numNodes;
    }

    public ArrayList<UsingNodes> getUsingNodesList() {
        return usingNodesList;
    }

    public int getJobId() {
        return jobId;
    }

    public int getRequiredNodes() {
        return requiredNodes;
    }

    public void setJobId(int jobId) {
        this.jobId = jobId;
    }

    public void setSubmitTime(int submitTime) {
        this.submitTime = submitTime;
    }

    public void setActualExecuteTime(int actualExecuteTime) {
        this.actualExecuteTime = actualExecuteTime;
    }

    public void setRequiredTime(int requiredTime) {
        this.requiredTime = requiredTime;
    }

    public void setRequiredCores(int requiredCores) {
        this.requiredCores = requiredCores;
    }

    public void setRequiredNodes(int requiredNodes) {
        this.requiredNodes = requiredNodes;
    }

    public void setRunningTimeDed(int runningTimeDed) {
        this.runningTimeDed = runningTimeDed;
    }

    public void setRunningTimeOC(int runningTimeOC) {
        this.runningTimeOC = runningTimeOC;
    }

    public void setStartTime(int startTime) {
        this.startTime = startTime;
    }

    public void setFinishedTime(int finishedTime) {
        this.finishedTime = finishedTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public void setNumNodes(int numNodes) {
        this.numNodes = numNodes;
    }

    public void setUsingNodesList(ArrayList<UsingNodes> usingNodesList) {
        this.usingNodesList = usingNodesList;
    }
    
    
    
    
    
    
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import static nodeconsciousscheduler.Constants.UNSTARTED;

/**
 *
 * @author sminami
 */
public class Job {
    private int jobId;
    private int submitTime;
    private int actualExecuteTime;
    private int specifiedExecuteTime;
    private int endEventOccuranceTimeNow;
    private int requiredTime;
    private int requiredCores;
    private int requiredNodes;
    private int requiredCoresPerNode;
    private int runningTimeDed;
    private int runningTimeOC;
    private double cpuTimeForNow;
    private int OCStateLevel;
    private int startTime;
    private int previousMeasuredTime;
    private int finishedTime;
    private int waitTime;
    private int numNodes;
    private double slowdown;
    private ArrayList<UsingNode> usingNodesList;
    private Set<Integer> coexistingJobs;
    

    Job() {}
    
    Job(int submitTime, int actualExecuteTime, int requiredTime, int requiredCores, int requiredNodes) {
        this.submitTime = submitTime;
        this.actualExecuteTime = actualExecuteTime;
        this.requiredTime = requiredTime;
        this.requiredCores = requiredCores;
        this.requiredNodes = requiredNodes;
        
        this.startTime = UNSTARTED;
        this.finishedTime = 2 << 30;
        this.waitTime = -1;
        this.cpuTimeForNow = 0.0;
        this.usingNodesList = new ArrayList<UsingNode>();
        this.coexistingJobs = new HashSet<Integer>();
        this.OCStateLevel = 1;
    }

    Job(int jobId, int submitTime, int actualExecuteTime, int requiredTime, int requiredCores, int requiredNodes) {
        this.jobId = jobId;
        this.submitTime = submitTime;
        this.actualExecuteTime = actualExecuteTime;
        this.requiredTime = requiredTime;
        this.requiredCores = requiredCores;
        this.requiredNodes = requiredNodes;
        this.requiredCoresPerNode = requiredCores/requiredNodes;
        if (requiredCores%requiredNodes != 0) ++this.requiredCoresPerNode;
        
        this.startTime = -1;
        this.finishedTime = 2 << 30;
        this.waitTime = -1;
        this.cpuTimeForNow = 0.0;
        this.usingNodesList = new ArrayList<UsingNode>();
        this.coexistingJobs = new HashSet<Integer>();
        this.OCStateLevel = 1;
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

    public int getPreviousMeasuredTime() {
        return previousMeasuredTime;
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

    public ArrayList<UsingNode> getUsingNodesList() {
        return usingNodesList;
    }

    public int getJobId() {
        return jobId;
    }

    public int getRequiredNodes() {
        return requiredNodes;
    }

    public double getCpuTimeForNow() {
        return cpuTimeForNow;
    }
    
    public int getOCStateLevel() {
        return OCStateLevel;
    }

    public Set<Integer> getCoexistingJobs() {
        return coexistingJobs;
    }

    public int getEndEventOccuranceTimeNow() {
        return endEventOccuranceTimeNow;
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

    public void setPreviousMeasuredTime(int previousSwitchedTime) {
        this.previousMeasuredTime = previousSwitchedTime;
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

    public void setUsingNodesList(ArrayList<UsingNode> usingNodesList) {
        this.usingNodesList = usingNodesList;
    }

    public int getSpecifiedExecuteTime() {
        return specifiedExecuteTime;
    }

    public void setSpecifiedExecuteTime(int specifiedExecuteTime) {
        this.specifiedExecuteTime = specifiedExecuteTime;
    }

    public void setCpuTimeForNow(double cpuTimeForNow) {
        this.cpuTimeForNow = cpuTimeForNow;
    }
    
    public void setOCStateLevel(int OCStateLevel) {
        this.OCStateLevel = OCStateLevel;
    }    

    public void setCoexistingJobs(Set<Integer> coexistingJobs) {
        this.coexistingJobs = coexistingJobs;
    }

    public void setEndEventOccuranceTimeNow(int endEventOccuranceTimeNow) {
        this.endEventOccuranceTimeNow = endEventOccuranceTimeNow;
    }

    public double getSlowdown() {
        return slowdown;
    }

    public void setSlowdown(double slowdown) {
        this.slowdown = slowdown;
    }

    public int getRequiredCoresPerNode() {
        return requiredCoresPerNode;
    }

    public void setRequiredCoresPerNode(int requiredCoresPerNode) {
        this.requiredCoresPerNode = requiredCoresPerNode;
    }
    
    
}

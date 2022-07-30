/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import static nodeconsciousscheduler.Constants.NOT_FINISHED;
import static nodeconsciousscheduler.Constants.UNSTARTED;

/**
 *
 * @author sminami
 */
public class Job implements Comparable<Job> {
    private int jobId;
    private int submitTime;
    private int actualExecuteTime;
    private int occupiedTimeInTimeSlices;
    private int endEventOccuranceTimeNow;
    private int requiredTime;
    private int requiredCores;
    private int requiredNodes;
    private int requiredCoresPerNode;
    private int runningTimeDed;
    private int runningTimeOC;
    private double currentAccumulatedComputeQuantity;
    private double currentAccumulatedComputeQuantityOnlyConsiderMultiplicity;
    private int OCStateLevel;
    private int startTime;
    private int previousMeasuredTime;
    private int previousMigratingTime;
    private int finishedTime;
    private int waitTime;
    private int numNodes;
    private double slowdown;
    private double slowdownByOriginalRunningTime;
    private ArrayList<UsingNode> usingNodesList;
    private Set<Integer> coexistingJobs;
    private int userId;
    private int groupId;
    private long maxMemory;
    private int matchingGroup;
    private double currentRatio;
    private double accumulatedCpuTime;
    private int queueNum;
    private int apparentOCStateLevel;
    private boolean interacitveJob;
    private int interactiveExecuteTime;
    private int prologTime;
    private int epilogTIme;
    private ArrayList<Integer> activationTimes;
    private ArrayList<Integer> idleTimes;
    private boolean activationState;
    private int currentActivationIndex;
    private double currentAccumulatedComputeQuantityForLatestActivation;
    

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
        this.currentAccumulatedComputeQuantity = 0.0;
        this.currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = 0.0;
        this.accumulatedCpuTime = 0.0;
        this.usingNodesList = new ArrayList<UsingNode>();
        this.coexistingJobs = new HashSet<Integer>();
        this.OCStateLevel = 1;
    }

    Job(int jobId, int submitTime, int actualExecuteTime, int requiredTime, 
            int requiredCores, int requiredNodes, int userId, int groupId, 
            int maxMemory, int matchingGroup, int queueNum,
            boolean accurateIntteractiveJobs,   
            boolean interactiveJob, 
            int interactiveExecuteTime, ArrayList<Integer> activationTimes, 
            int prologTime, int epilogTime, ArrayList<Integer> idleTimes) {
        this.jobId = jobId;
        this.submitTime = submitTime;
        this.actualExecuteTime = actualExecuteTime;
        this.requiredTime = requiredTime;
        this.requiredCores = requiredCores;
        this.requiredNodes = requiredNodes;
        this.requiredCoresPerNode = requiredCores/requiredNodes;
        if (requiredCores%requiredNodes != 0) ++this.requiredCoresPerNode;
        this.userId = userId;
        this.groupId = groupId;
        this.maxMemory = maxMemory;
        this.matchingGroup = matchingGroup;
        this.currentRatio = 1.0;
        
        this.startTime = -1;
        this.finishedTime = NOT_FINISHED;
        this.waitTime = -1;
        this.currentAccumulatedComputeQuantity = 0.0;
        this.currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = 0.0;
        this.accumulatedCpuTime = 0.0;
        this.usingNodesList = new ArrayList<UsingNode>();
        this.coexistingJobs = new HashSet<Integer>();
        this.OCStateLevel = 1;
        this.queueNum = queueNum;
        this.apparentOCStateLevel = 1;

        if (accurateIntteractiveJobs) {
            this.interacitveJob = interactiveJob;
            this.interactiveExecuteTime = interactiveExecuteTime;
            this.prologTime = prologTime;
            this.epilogTIme = epilogTime;
            this.idleTimes = idleTimes;
            this.activationTimes = activationTimes;
            this.activationState = false;
            this.currentActivationIndex = 0;
            this.currentAccumulatedComputeQuantityForLatestActivation = 0.0;
        } else {            
            this.interacitveJob = interacitveJob;
        }
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

    public double getCurrentAccumulatedComputeQuantity() {
        return currentAccumulatedComputeQuantity;
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

    public void setPreviousMeasuredTime(int previousMeasuredTime) {
        this.previousMeasuredTime = previousMeasuredTime;
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

    public int getOccupiedTimeInTimeSlices() {
        return occupiedTimeInTimeSlices;
    }

    public void setOccupiedTimeInTimeSlices(int occupiedTimeInTimeSlices) {
        this.occupiedTimeInTimeSlices = occupiedTimeInTimeSlices;
    }

    public void setCurrentAccumulatedComputeQuantity(double cpuTimeForNow) {
        this.currentAccumulatedComputeQuantity = cpuTimeForNow;
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

    public int getPreviousMigratingTime() {
        return previousMigratingTime;
    }

    public void setPreviousMigratingTime(int previousMigratingTime) {
        this.previousMigratingTime = previousMigratingTime;
    }
    
    
    @Override
    public int compareTo(Job o) {
        if (this.startTime < o.startTime) {
            return -1;
        }
        if (this.startTime > o.startTime) {
            return 1;
        }
        
        return 0;
    }

    public int getUserId() {
        return userId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }   

    public long getMaxMemory() {
        return maxMemory;
    }

    public void setMaxMemory(long maxMemory) {
        this.maxMemory = maxMemory;
    }

    public double getSlowdownByOriginalRunningTime() {
        return slowdownByOriginalRunningTime;
    }

    public void setSlowdownByOriginalRunningTime(double slowdownByOriginalRunningTime) {
        this.slowdownByOriginalRunningTime = slowdownByOriginalRunningTime;
    }

    public int getMatchingGroup() {
        return matchingGroup;
    }

    public void setMatchingGroup(int matchingGroup) {
        this.matchingGroup = matchingGroup;
    }

    public double getCurrentRatio() {
        return currentRatio;
    }

    public void setCurrentRatio(double currentRatio) {
        this.currentRatio = currentRatio;
    }

    public double getCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity() {
        return currentAccumulatedComputeQuantityOnlyConsiderMultiplicity;
    }

    public void setCurrentAccumulatedComputeQuantityOnlyConsiderMultiplicity(double cpuTimeOnlyConsiderMultiplicity) {
        this.currentAccumulatedComputeQuantityOnlyConsiderMultiplicity = cpuTimeOnlyConsiderMultiplicity;
    }

    public double getAccumulatedCpuTime() {
        return accumulatedCpuTime;
    }

    public void setAccumulatedCpuTime(double accumulatedCpuTime) {
        this.accumulatedCpuTime = accumulatedCpuTime;
    }

    public int getQueueNum() {
        return queueNum;
    }

    public int getApparentOCStateLevel() {
        return apparentOCStateLevel;
    }

    public boolean isInteracitveJob() {
        return interacitveJob;
    }

    public int getPrologTime() {
        return prologTime;
    }

    public int getEpilogTIme() {
        return epilogTIme;
    }

    public boolean isActivationState() {
        return activationState;
    }

    public void setApparentOCStateLevel(int apparentOCStateLevel) {
        this.apparentOCStateLevel = apparentOCStateLevel;
    }

    public void setActivationState(boolean activationState) {
        this.activationState = activationState;
    }

    public int getInteractiveExecuteTime() {
        return interactiveExecuteTime;
    }

    public ArrayList<Integer> getActivationTimes() {
        return activationTimes;
    }

    public int getCurrentActivationIndex() {
        return currentActivationIndex;
    }

    public double getCurrentAccumulatedComputeQuantityForLatestActivation() {
        return currentAccumulatedComputeQuantityForLatestActivation;
    }

    public void setCurrentAccumulatedComputeQuantityForLatestActivation(double currentAccumulatedComputeQuantityForLatestActivation) {
        this.currentAccumulatedComputeQuantityForLatestActivation = currentAccumulatedComputeQuantityForLatestActivation;
    }
    
    public int getCurrentRequiredActivationTime() {
        return getActivationTimes().get(getCurrentActivationIndex());
    }

    public void setCurrentActivationIndex(int currentActivationIndex) {
        this.currentActivationIndex = currentActivationIndex;
    }

    public ArrayList<Integer> getIdleTimes() {
        return idleTimes;
    }
}

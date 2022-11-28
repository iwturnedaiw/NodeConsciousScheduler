/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author sminami
 */
class SimulatorConfiguration {
    private ArrayList<Double> thresholdForSlowdown = new ArrayList<Double>();
    private boolean outputMinuteTimeseries;
    private boolean outputUtilizationRatio;
    private boolean scheduleUsingMemory;
    private boolean crammingMemoryScheduling;
    private boolean considerJobMatching;
    Map<JobMatching, Double> jobMatchingTable = new HashMap<>();
    private boolean usingAffinityForSchedule;
    private double thresholdForAffinitySchedule;
    private boolean accurateInteractiveJobs;
    private int interactiveJobsRecordsType = Constants.UNSPECIFIED;
    private double interactiveCPURatio;
    private double prologTimeRatio;
    private double epilogTimeRatio;
    
    SimulatorConfiguration(String[] thresholdForSlowdown, boolean outputMinuteTimeseries, boolean scheduleUsingMemory) {
        this.scheduleUsingMemory = scheduleUsingMemory;
        double previousValue = -1;
        for (int i = 0; i < thresholdForSlowdown.length; ++i) {
            double value = Double.parseDouble(thresholdForSlowdown[i]);
            assert previousValue < value;
            this.thresholdForSlowdown.add(value);
            previousValue = value;
        }
        this.outputMinuteTimeseries = outputMinuteTimeseries;
    }

    SimulatorConfiguration(String[] thresholdForSlowdown, 
            boolean outputMinuteTimeseries, 
            boolean outputUtilizationRatio,
            boolean scheduleUsingMemory, 
            boolean crammingMemoryScheduling, 
            boolean considerJobMatching, 
            boolean usingAffinityForSchedule, 
            double thresholdForAffinitySchedule,
            boolean accurateInteractiveJobs, 
            int interactiveJobsRecordsType,
            double interacitiveCPURatio,
            double prologTimeRatio,
            double epilogTimeRatio) {
        this.scheduleUsingMemory = scheduleUsingMemory;
        this.considerJobMatching = considerJobMatching;
        this.crammingMemoryScheduling = crammingMemoryScheduling;
        this.usingAffinityForSchedule = usingAffinityForSchedule;
        this.thresholdForAffinitySchedule = thresholdForAffinitySchedule;
        double previousValue = -1;
        for (int i = 0; i < thresholdForSlowdown.length; ++i) {
            double value = Double.parseDouble(thresholdForSlowdown[i]);
            assert previousValue < value;
            this.thresholdForSlowdown.add(value);
            previousValue = value;
        }
        this.outputMinuteTimeseries = outputMinuteTimeseries;
        this.outputUtilizationRatio = outputUtilizationRatio;
        this.accurateInteractiveJobs = accurateInteractiveJobs;     
        this.interactiveJobsRecordsType = interactiveJobsRecordsType;     
        this.interactiveCPURatio = interacitiveCPURatio;
        this.prologTimeRatio = prologTimeRatio;
        this.epilogTimeRatio = epilogTimeRatio;
    }
    
    public ArrayList<Double> getThresholdForSlowdown() {
        return thresholdForSlowdown;
    }

    public boolean isOutputMinuteTimeseries() {
        return outputMinuteTimeseries;
    }

    public boolean isScheduleUsingMemory() {
        return scheduleUsingMemory;
    }

    public boolean isConsiderJobMatching() {
        return considerJobMatching;
    }

    public void setJobMatchingTable(Map<JobMatching, Double> jobMatchingTable) {
        this.jobMatchingTable = jobMatchingTable;
    }

    public Map<JobMatching, Double> getJobMatchingTable() {
        return jobMatchingTable;
    }

    public boolean isCrammingMemoryScheduling() {
        return crammingMemoryScheduling;
    }

    public boolean isUsingAffinityForSchedule() {
        return usingAffinityForSchedule;
    }

    public double getThresholdForAffinitySchedule() {
        return thresholdForAffinitySchedule;
    }

    public boolean isAccurateInteractiveJobs() {
        return accurateInteractiveJobs;
    }

    public double getInteractiveCPURatio() {
        return interactiveCPURatio;
    }

    public boolean isOutputUtilizationRatio() {
        return outputUtilizationRatio;
    }

    public double getPrologTimeRatio() {
        return prologTimeRatio;
    }

    public double getEpilogTimeRatio() {
        return epilogTimeRatio;
    }

    public int getInteractiveJobsRecordsType() {
        return interactiveJobsRecordsType;
    }
    
}

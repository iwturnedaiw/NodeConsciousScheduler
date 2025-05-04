/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import nodeconsciousscheduler.Constants.ScheduleConsiderJobType;
import nodeconsciousscheduler.Constants.OsubOverheadModelType;

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
    private boolean outputSecondWastedResources;
    private Constants.ScheduleConsiderJobType scheduleConsiderJobType;
    private Constants.OsubOverheadModelType osubOverheadModelType;
    private double osubOverheadConst;
    
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
            double epilogTimeRatio,
            boolean outputSecondWastedResources,
            int scheduleConsiderJobType,
            int osubOverheadModelType,
            double osubOverheadConst) {
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
        this.outputSecondWastedResources = outputSecondWastedResources;
        if (scheduleConsiderJobType == 0) {
            this.scheduleConsiderJobType = ScheduleConsiderJobType.NOTHING;
        } else if (scheduleConsiderJobType == 1) {
            this.scheduleConsiderJobType = ScheduleConsiderJobType.BATCH_INT;
        } else if (scheduleConsiderJobType == 2) {
            this.scheduleConsiderJobType = ScheduleConsiderJobType.INT_INT;
        }
        if (osubOverheadModelType == 0) {
            this.osubOverheadModelType = OsubOverheadModelType.NOTHING;
        } else if (osubOverheadModelType == 1) {
            this.osubOverheadModelType = OsubOverheadModelType.CONST;
            this.osubOverheadConst = osubOverheadConst;
        } else if (osubOverheadModelType == 2) {
            this.osubOverheadModelType = OsubOverheadModelType.CONSIDER_MULT;
        }
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

    public boolean isOutputSecondWastedResources() {
        return outputSecondWastedResources;
    }

    public Constants.ScheduleConsiderJobType getScheduleConsiderJobType() {
        return scheduleConsiderJobType;
    }

    public OsubOverheadModelType getOsubOverheadModelType() {
        return osubOverheadModelType;
    }

    public double getOsubOverheadConst() {
        return osubOverheadConst;
    }
}

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
class SimulatorConfiguration {
    private ArrayList<Double> thresholdForSlowdown = new ArrayList<Double>();
    private boolean outputMinuteTimeseries;
    private boolean scheduleUsingMemory;
    private boolean ignoreIncompleteMemoryData;

    SimulatorConfiguration(String[] thresholdForSlowdown, boolean outputMinuteTimeseries) {
        double previousValue = -1;
        for (int i = 0; i < thresholdForSlowdown.length; ++i) {
            double value = Double.parseDouble(thresholdForSlowdown[i]);
            assert previousValue < value;
            this.thresholdForSlowdown.add(value);
            previousValue = value;
        }
        this.outputMinuteTimeseries = outputMinuteTimeseries;
    }

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
    

    SimulatorConfiguration(String[] thresholdForSlowdown, boolean outputMinuteTimeseries, boolean scheduleUsingMemory, boolean ignoreIncompleteMemoryData) {
        this.scheduleUsingMemory = scheduleUsingMemory;
        this.ignoreIncompleteMemoryData = ignoreIncompleteMemoryData;
        double previousValue = -1;
        for (int i = 0; i < thresholdForSlowdown.length; ++i) {
            double value = Double.parseDouble(thresholdForSlowdown[i]);
            assert previousValue < value;
            this.thresholdForSlowdown.add(value);
            previousValue = value;
        }
        this.outputMinuteTimeseries = outputMinuteTimeseries;
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

    public boolean isIgnoreIncompleteMemoryData() {
        return ignoreIncompleteMemoryData;
    }
    
    
    
}

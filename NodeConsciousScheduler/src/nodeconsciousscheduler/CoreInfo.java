/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import static nodeconsciousscheduler.Constants.UNUSED;

/**
 *
 * @author sminami
 */
public class CoreInfo implements Cloneable, Comparable<CoreInfo> {
    private int coreId;
    private ArrayList<Integer> jobList;
    private double priority;

    CoreInfo() {
        this.coreId = UNUSED;
    }
    CoreInfo(int coreId) {
        this.coreId = coreId;
        this.jobList = new ArrayList<Integer>();
    }
    
    @Override
    public CoreInfo clone() {
        CoreInfo clonedItem = null;
        try {
            // Object型で返ってくるのでキャストが必要
            clonedItem = (CoreInfo)super.clone();
            
            ArrayList<Integer> orgJobList = this.getJobList();
            clonedItem.jobList = new ArrayList<Integer>();
            for (int i = 0; i < orgJobList.size(); ++i){
                int jobId = orgJobList.get(i);
                clonedItem.jobList.add(jobId);
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return clonedItem;
    }
     
    @Override
    public int compareTo(CoreInfo o) {
        if (this.jobList.size() > o.jobList.size()) {
            return 1;
        }
        if (this.jobList.size() < o.jobList.size()) {
            return -1;
        }

        if (this.priority > o.priority) {
            return 1;
        }
        if (this.priority < o.priority) {
            return -1;
        }
        
        if (this.coreId > o.coreId) {
            return 1;            
        }
         
        if (this.coreId < o.coreId) {
            return -1;            
        }
        
        return 0;        
    }
    

    public int getCoreId() {
        return coreId;
    }

    public ArrayList<Integer> getJobList() {
        return jobList;
    }

    public void setCoreId(int coreId) {
        this.coreId = coreId;
    }

    public void setJobList(ArrayList<Integer> jobList) {
        this.jobList = jobList;
    }

    public double getPriority() {
        return priority;
    }

    public void setPriority(double priority) {
        this.priority = priority;
    }
}

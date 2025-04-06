/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import nodeconsciousscheduler.Constants.TimeDesc;
import static nodeconsciousscheduler.Constants.UNUSED;



/**
 *
 * @author sminami
 */
public class Event implements Comparable<Event> {
    private EventType eventType;
    private int occurrenceTime;
    private Job job;
    private int deleteTargetTime = UNUSED;
    private TimeDesc timeDesc;
    
    
    Event() {
        ;
    }
    Event(EventType eventType, int occurrenceTime, Job job) {
        this.eventType = eventType;
        this.occurrenceTime = occurrenceTime;
        this.job = job;
        
    }

    Event(EventType eventType, int occurrenceTime, Job job, int deleteTargetTime) {
        this.eventType = eventType;
        this.occurrenceTime = occurrenceTime;
        this.job = job;
        this.deleteTargetTime = deleteTargetTime;        
    }
    
    Event(int occurrenceTime, Job job) {
        this.occurrenceTime = occurrenceTime;
        this.job = job;
    }

    Event(int START_TIME, TimeDesc timeDesc, EventType evt) {
        this.occurrenceTime = START_TIME;
        this.timeDesc = timeDesc;
        this.eventType = evt;
        this.job = null;
    }
    
    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public void setOccurrenceTime(int occurrenceTime) {
        this.occurrenceTime = occurrenceTime;
    }

    public void setJob(Job job) {
        this.job = job;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getOccurrenceTime() {
        return occurrenceTime;
    }

    public Job getJob() {
        return job;
    }

    public int getDeleteTargetTime() {
        return deleteTargetTime;
    }

    public void setDeleteTargetTime(int deleteTargetTime) {
        this.deleteTargetTime = deleteTargetTime;
    }
    
    @Override
    public int compareTo(Event o) {
        if (this.occurrenceTime < o.occurrenceTime) {
            return -1;
        }
        if (this.occurrenceTime > o.occurrenceTime) {
            return 1;
        } 
        if (this.eventType == EventType.MEASURING_UTIL_RATIO) {
            return -1;
        }
        if (o.eventType == EventType.MEASURING_UTIL_RATIO) {
            return 1;
        }         
        if (this.eventType == EventType.MEASURING_WASTED_RESOURCE) {
            return -1;
        }
        if (o.eventType == EventType.MEASURING_WASTED_RESOURCE) {
            return 1;
        }
        //if (this.eventType != EventType.END && this.eventType == o.eventType) {
        if (this.eventType == o.eventType) {
            Job job1 = this.getJob();
            Job job2 = o.getJob();
            if (job1 != null && job2 != null) {
                return Integer.compare(this.getJob().getJobId(), o.getJob().getJobId());            
            }
        }
        if (this.eventType == EventType.START) {
            return -1;
        }
        if (o.eventType == EventType.START) {
            return 1;
        }        
        if (this.eventType == EventType.INT_ACTIVATE) {
            return -1;
        }
        if (o.eventType == EventType.INT_ACTIVATE) {
            return 1;
        } 
        if (this.eventType == EventType.INT_DEACTIVATE) {
            return -1;
        }
        if (o.eventType == EventType.INT_DEACTIVATE) {
            return 1;
        }        
        if (this.eventType == EventType.END) {
            return -1;
        }
        if (o.eventType == EventType.END) {
            return 1;
        }        
        if (this.eventType == EventType.DELETE_FROM_END) {
            return -1;
        }
        if (o.eventType == EventType.DELETE_FROM_END) {
            return 1;
        }
        if (this.eventType == EventType.DELETE_FROM_BEGINNING) {
            return -1;
        }
        if (o.eventType == EventType.DELETE_FROM_BEGINNING) {
            return 1;
        }
        if (this.eventType == EventType.DELETE_DEACTIVE) {
            return -1;
        }
        if (o.eventType == EventType.DELETE_DEACTIVE) {
            return 1;
        }
        
        
        return Integer.compare(this.getJob().getJobId(), o.getJob().getJobId());
    }

    public TimeDesc getTimeDesc() {
        return timeDesc;
    }
    
    
}

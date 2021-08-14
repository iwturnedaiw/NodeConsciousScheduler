/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;



/**
 *
 * @author sminami
 */
public class Event implements Comparable<Event> {
    private EventType eventType;
    private int occurrenceTime;
    private Job job;
    
    
    Event(EventType eventType, int occurrenceTime, Job job) {
        this.eventType = eventType;
        this.occurrenceTime = occurrenceTime;
        this.job = job;
        
    }

    Event(int occurrenceTime, Job job) {
        this.occurrenceTime = occurrenceTime;
        this.job = job;
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

    @Override
    public int compareTo(Event o) {
        if (this.occurrenceTime < o.occurrenceTime) {
            return -1;
        }
        if (this.occurrenceTime > o.occurrenceTime) {
            return 1;
        }
        if (this.eventType == o.eventType) {
            return Integer.compare(this.getJob().getJobId(), o.getJob().getJobId());            
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
        
        
        return Integer.compare(this.getJob().getJobId(), o.getJob().getJobId());
    }
}

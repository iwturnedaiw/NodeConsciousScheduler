/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author sminami
 */
public class EventQueue extends PriorityQueue {
    
    
    void enqueueJob(Job job) {
        Event ev = jobToEvent(job);
        ev.setEventType(EventType.SUBMIT);
        this.add(ev);        
    }

    private Event jobToEvent(Job job) {
        Event ev = new Event(job.getSubmitTime(), job);
        return ev;
    }
    
    Object dequeue() {
        return this.poll();
    }

    void handle() {
        Event ev = (Event) this.dequeue();
        EventType evt = ev.getEventType();
        
        EventHandler evh = null;
        if (evt == EventType.SUBMIT) {
            evh = new Submission();
//            System.out.println("size: " + this.size());
//            System.out.println("jobId: " + ev.getJob().getJobId());            
        } else if (evt == EventType.START) {
            evh = new Start();
//            System.out.println("size: " + this.size());
//            System.out.println("jobId: " + ev.getJob().getJobId());            
        } else if (evt == EventType.END) {
            evh = new End();
//            System.out.println("size: " + this.size());
//            System.out.println("jobId: " + ev.getJob().getJobId());
        } else if (evt == EventType.DELETE_FROM_BEGINNING) {
            evh = new DeleteFromBeginning();
        } else if (evt == EventType.DELETE_FROM_END) {
            evh = new DeleteFromEnd();
        }
        
        assert evh != null;
        
        ArrayList<Event> newEvents = new ArrayList<Event>();
        newEvents = evh.handle(ev);
        for (Event e : newEvents) {
            this.add(e);            
        }

    }

    void deleteEventFromBeginning(Event ev) {
        Iterator itr = this.iterator();
        
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int updatedTime = ev.getUpdatedTime();        
        int deleteCnt = 0;
        while(itr.hasNext()) {
            Event candidateEvent = (Event) itr.next();
            EventType evt = candidateEvent.getEventType();            
            int occuranceTime = candidateEvent.getOccurrenceTime();            
            Job candidateJob = candidateEvent.getJob();
            int candidateJobId = candidateJob.getJobId();
            if (evt == EventType.END && jobId == candidateJobId && occuranceTime != updatedTime) {
                // TODO: if originalEndTime == currentTime, delete the newcomer event
                // int originalEndTime = candidateEvent.getOccurrenceTime();                               
                itr.remove();
                ++deleteCnt;
                break;
            }
        }
        assert deleteCnt == 1;
        return;
    }

    void deleteEventFromEnd(Event ev) {
        Iterator itr = this.iterator();
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int updatedTime = ev.getUpdatedTime();
        int deleteCnt = 0;
        while(itr.hasNext()) {
            Event candidateEvent = (Event) itr.next();
            EventType evt = candidateEvent.getEventType();
            int occuranceTime = candidateEvent.getOccurrenceTime();
            Job candidateJob = candidateEvent.getJob();
            int candidateJobId = candidateJob.getJobId();
            if (evt == EventType.END && jobId == candidateJobId && occuranceTime != updatedTime) {                
                // TODO: if originalEndTime == currentTime, delete the newcomer event
                // int originalEndTime = candidateEvent.getOccurrenceTime();                                               
                itr.remove();
                ++deleteCnt;
                break;
            }
        }
        assert deleteCnt == 1;
        return;
    }

    private void debug() {
        System.out.println("Debug:");
        while(this.size() > 0) {
            Event ev = (Event) this.poll();
            System.out.println("\tEvent Type: " + ev.getEventType()+ ", JobId: " + ev.getJob().getJobId() + " at " + ev.getOccurrenceTime());
        }
        System.exit(1);
    }

    static void debugExecuting(int currentTime, Event ev) throws Exception {
        ArrayList<Job> executingJobList = NodeConsciousScheduler.sim.getExecutingJobList();
        for (int i = 0; i < executingJobList.size(); ++i) {
            Job job = executingJobList.get(i);
            ArrayList<UsingNode> usingNodes = job.getUsingNodesList();
            if (usingNodes == null) {
                System.out.println("debug) FOUND null pointer HERE: " + job.getJobId() + " at " + currentTime); 
                System.out.println("\tEventType: " + ev.getEventType() + ", jobId: " + ev.getJob().getJobId()); 
                throw new Exception();
            }
        }
    }
}



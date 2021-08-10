/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.PriorityQueue;

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
        } else if (evt == EventType.DELETE) {
            evh = new Delete();
        }
        
        assert evh != null;

        ArrayList<Event> newEvents = new ArrayList<Event>();
        newEvents = evh.handle(ev);
        for (Event e : newEvents) {
            this.add(e);            
        }

    }

    void deleteEvent(Event ev) {
        Iterator itr = this.iterator();
        
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int deleteCnt = 0;
        while(itr.hasNext()) {
            Event candidateEvent = (Event) itr.next();
            Job candidateJob = candidateEvent.getJob();
            int candidateJobId = candidateJob.getJobId();
            if (jobId == candidateJobId) {
                itr.remove();
                ++deleteCnt;
                break;
            }
        }
        assert deleteCnt == 1;
        return;
    }
}



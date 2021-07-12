/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

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
        }
        
        assert evh != null;

        Event[] newEvents = null;
        newEvents = evh.handle(ev);
    }
}



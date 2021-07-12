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
public interface EventHandler {
    Event[] handle(Event ev);

}

class Submission implements EventHandler {
    public Event[] handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType());
//        newEvents = 
        
        Event[] evs = null;
        
        NodeConsciousScheduler.sim.getSche().schedule(ev);
        
        return evs;
    }
}


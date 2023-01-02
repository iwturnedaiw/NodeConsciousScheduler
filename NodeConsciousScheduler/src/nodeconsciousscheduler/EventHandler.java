/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package nodeconsciousscheduler;

import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static nodeconsciousscheduler.Constants.DAY_IN_SECOND;
import static nodeconsciousscheduler.Constants.HOUR_IN_SECOND;
import static nodeconsciousscheduler.Constants.MINUTE_IN_SECOND;
import static nodeconsciousscheduler.Constants.SECOND;
import static nodeconsciousscheduler.Constants.START_TIME;
import nodeconsciousscheduler.Constants.TimeDesc;
import static nodeconsciousscheduler.Constants.UNUPDATED;

/**
 *
 * @author sminami
 */
public interface EventHandler {

    ArrayList<Event> handle(Event ev);

}

class Submission implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnSubmission(ev);
        
        return evs;
    }
}

class Start implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        // Add the Job to Executing job List
        // Executing job list is declared in Simulator
//        evs = NodeConsciousScheduler.sim.getSche().schedule(ev);
        
        Job job = ev.getJob();
        job.setStartTime(ev.getOccurrenceTime());
        job.setPreviousMeasuredTime(ev.getOccurrenceTime());
        job.setWaitTime(job.getStartTime() - job.getSubmitTime());
        job.setPreviousMigratingTime(ev.getOccurrenceTime());
        
        NodeConsciousScheduler.sim.getExecutingJobList().add(job);
        
        // TODO
        // Update activated cores table if it is not int.
        boolean interactiveJob = job.isInteracitveJob();
        if (interactiveJob) {
            int currentTime = ev.getOccurrenceTime();
            int prologTime = job.getPrologTime();
            int activationTime = currentTime + prologTime;
            Scheduler.printThrownEvent(currentTime, activationTime, job, EventType.INT_ACTIVATE, 1);
            evs.add(new Event(EventType.INT_ACTIVATE, activationTime, job));
        }
        
        return evs;
    }
}

class End implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        /* Fix the finish time */
        Job job = ev.getJob();
        int jobId = job.getJobId();
        int currentTime = ev.getOccurrenceTime();
        assert currentTime == job.getEndEventOccuranceTimeNow();
        boolean interactiveJob = job.isInteracitveJob();

        NodeConsciousScheduler.sim.getSche().calcWastedResource(currentTime);
        
        if (!interactiveJob) {
            int previousMeasuredTime = job.getPreviousMeasuredTime();
            int mostRecentRunningTime = currentTime - previousMeasuredTime;
            int netOCStateLevel = job.getNetOCStateLevel();
            double accumulatedCpuTime = job.getAccumulatedCpuTime();
            accumulatedCpuTime += (double) mostRecentRunningTime / netOCStateLevel;
            job.setAccumulatedCpuTime(accumulatedCpuTime);

            if (netOCStateLevel == 1) {
                int runningTimeDed = job.getRunningTimeDed();
                job.setRunningTimeDed(runningTimeDed + mostRecentRunningTime);
            } else {
                int runningTimeOC = job.getRunningTimeOC();
                job.setRunningTimeOC(runningTimeOC + mostRecentRunningTime);
            }
        }
        int runningTimeDed = job.getRunningTimeDed();
        int runningTimeOC = job.getRunningTimeOC();
        int runningTime = runningTimeDed + runningTimeOC;
        int finishedTime = runningTime + job.getStartTime();
        if (interactiveJob)
            finishedTime = job.getStartTime() + runningTime + job.getPrologTime() + job.getSumIdleTime() + job.getEpilogTIme();
        job.setFinishedTime(finishedTime);

            
        assert currentTime == finishedTime;
        
        // Output the result
        NodeConsciousScheduler.sim.outputResult(job);
        NodeConsciousScheduler.sim.outputResultForVis(job);

        // Erase the job from executing job list
        NodeConsciousScheduler.sim.getExecutingJobList().remove(job);
        // Add the job completed List
        NodeConsciousScheduler.sim.getCompletedJobList().add(job);

        // Resource refill        
        NodeConsciousScheduler.sim.freeResources(job);

        // Again call scheduling
        evs = NodeConsciousScheduler.sim.getSche().scheduleJobsOnEnd(ev);

        
        return evs;
    }


}

class IntActivate implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        evs = NodeConsciousScheduler.sim.getSche().activateInteractiveJob(ev);
        return evs;
    }
}

class IntDeactivate implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        evs = NodeConsciousScheduler.sim.getSche().deactivateInteractiveJob(ev);
        return evs;
    }
}

class DeleteDeactivate implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );

        NodeConsciousScheduler.sim.getEvq().deleteDeactiveEvent(ev);

        return new ArrayList<Event>();
    }
}

class DeleteFromBeginning implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        
        
        NodeConsciousScheduler.sim.getEvq().deleteEventFromBeginning(ev);
        
        return new ArrayList<Event>();
    }
}

class DeleteFromEnd implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime() + ", jobId " + ev.getJob().getJobId() );
//        newEvents = 
        
        
        
        NodeConsciousScheduler.sim.getEvq().deleteEventFromEnd(ev);
        
        return new ArrayList<Event>();
    }
}

class MeasuringUtilRatio implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime());
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        int currentTime = ev.getOccurrenceTime();
        TimeDesc timeDesc = ev.getTimeDesc();
        NodeConsciousScheduler.sim.calculateUtilRatio(currentTime, timeDesc);
        
        int threshold = UNUPDATED;
        if (timeDesc == TimeDesc.MINUTE) {
            threshold = MINUTE_IN_SECOND;
        } else if (timeDesc == TimeDesc.HOUR) {
            threshold = HOUR_IN_SECOND;
        } else if (timeDesc == TimeDesc.DAY) {
            threshold = DAY_IN_SECOND;
        }
        
        if ((currentTime == START_TIME) || (currentTime != START_TIME && NodeConsciousScheduler.sim.getCompletedJobList().size() != NodeConsciousScheduler.sim.getJobList().size())) {
            int nextArrivalTime = currentTime + threshold;
            NodeConsciousScheduler.sim.getEvq().enqueueUtilizationMeasuringEvent(nextArrivalTime, timeDesc);
        }
        
        return new ArrayList<Event>();
    }
}

class MeasuringWastedResource implements EventHandler {
    public ArrayList<Event> handle(Event ev) {
        System.out.println("Event type: " + ev.getEventType() + ", at " + ev.getOccurrenceTime());
        
        ArrayList<Event> evs = new ArrayList<Event>();
        
        int currentTime = ev.getOccurrenceTime();
        TimeDesc timeDesc = ev.getTimeDesc();
        NodeConsciousScheduler.sim.calculateWastedResource(currentTime, timeDesc);
        
        int threshold = UNUPDATED;
        if (timeDesc == TimeDesc.MINUTE) {
            threshold = MINUTE_IN_SECOND;
        } else if (timeDesc == TimeDesc.HOUR) {
            threshold = HOUR_IN_SECOND;
        } else if (timeDesc == TimeDesc.DAY) {
            threshold = DAY_IN_SECOND;
        } else if (timeDesc == TimeDesc.SECOND) {
            threshold = SECOND;
        }
        
        if ((currentTime == START_TIME) || (currentTime != START_TIME && NodeConsciousScheduler.sim.getCompletedJobList().size() != NodeConsciousScheduler.sim.getJobList().size())) {
            int nextArrivalTime = currentTime + threshold;
            NodeConsciousScheduler.sim.getEvq().enqueueWastedResourceMeasuringEvent(nextArrivalTime, timeDesc);
        }
        
        return new ArrayList<Event>();
    }
}